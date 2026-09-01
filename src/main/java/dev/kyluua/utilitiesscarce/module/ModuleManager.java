package dev.kyluua.utilitiesscarce.module;

import java.util.ArrayList;
import java.util.List;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.util.ActionScheduler;
import dev.kyluua.utilitiesscarce.util.ClientActions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Owns the module instances and forwards client events to them.
 *
 * <p>Attack events are offered to the modules in priority order and stop at the
 * first one that takes the held item, so Stun Slam (which already disables the
 * shield itself) wins over Shield Disable, and both win over Breach Swap.
 */
public final class ModuleManager {
	private final ActionScheduler scheduler = new ActionScheduler();

	private final AutoTotemModule autoTotem;
	private final StunSlamModule stunSlam;
	private final ShieldDisableModule shieldDisable;
	private final BreachSwapModule breachSwap;
	private final FastAnchorModule fastAnchor;

	private final List<Module> modules = new ArrayList<>();
	private final List<Module> attackPriority = new ArrayList<>();

	private boolean inWorld;

	public ModuleManager() {
		autoTotem = new AutoTotemModule(scheduler);
		stunSlam = new StunSlamModule(scheduler);
		shieldDisable = new ShieldDisableModule(scheduler);
		breachSwap = new BreachSwapModule(scheduler);
		fastAnchor = new FastAnchorModule(scheduler);

		modules.add(autoTotem);
		modules.add(stunSlam);
		modules.add(shieldDisable);
		modules.add(breachSwap);
		modules.add(fastAnchor);

		attackPriority.add(stunSlam);
		attackPriority.add(shieldDisable);
		attackPriority.add(breachSwap);
	}

	public List<Module> modules() {
		return modules;
	}

	public ActionScheduler scheduler() {
		return scheduler;
	}

	public void onClientTick(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null) {
			if (inWorld) {
				// Left the world: drop anything half-executed.
				inWorld = false;
				stopAll();
			}

			return;
		}

		inWorld = true;

		if (!ConfigManager.get().general.pauseWhenScreenOpen || minecraft.gui.screen() == null) {
			for (Module module : modules) {
				if (module.isEnabled()) {
					module.onTick(minecraft);
				}
			}
		}

		scheduler.tick(ConfigManager.get().general.maxActionsPerTick);
	}

	public void onAttackEntity(Minecraft minecraft, Entity target) {
		// Our own scheduled attacks come back through this event; ignore them or
		// a swap sequence would keep restarting itself.
		if (ClientActions.isSynthetic() || minecraft.player == null || target == null) {
			return;
		}

		for (Module module : attackPriority) {
			if (module.isEnabled() && module.onAttackEntity(minecraft, target)) {
				return;
			}
		}
	}

	public void onUseBlock(Minecraft minecraft, InteractionHand hand, BlockHitResult hitResult) {
		if (ClientActions.isSynthetic() || minecraft.player == null) {
			return;
		}

		for (Module module : modules) {
			if (module.isEnabled()) {
				module.onUseBlock(minecraft, hand, hitResult);
			}
		}
	}

	/** Drops any in-flight sequence, e.g. on disconnect. */
	public void stopAll() {
		scheduler.clear();

		for (Module module : modules) {
			module.onStop();
		}
	}
}
