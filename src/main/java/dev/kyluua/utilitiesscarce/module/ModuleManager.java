package dev.kyluua.utilitiesscarce.module;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.render.BlockScanner;
import dev.kyluua.utilitiesscarce.render.DetectionRange;
import dev.kyluua.utilitiesscarce.render.HighlightTargets;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
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
	private final FreeCamModule freeCam;
	private final EspModule esp;
	private final TracerModule tracer;

	/** Shared by ESP and Tracer so the block sweep only runs once. */
	private final BlockScanner blockScanner = new BlockScanner();

	private final List<Module> modules = new ArrayList<>();
	private final List<Module> attackPriority = new ArrayList<>();
	/** Last seen enabled state, so a module switched off anywhere gets cleaned up. */
	private final Map<Module, Boolean> wasEnabled = new IdentityHashMap<>();

	private boolean inWorld;

	public ModuleManager() {
		autoTotem = new AutoTotemModule(scheduler);
		stunSlam = new StunSlamModule(scheduler);
		shieldDisable = new ShieldDisableModule(scheduler);
		breachSwap = new BreachSwapModule(scheduler);
		fastAnchor = new FastAnchorModule(scheduler);
		freeCam = new FreeCamModule(scheduler);
		esp = new EspModule(scheduler, blockScanner);
		tracer = new TracerModule(scheduler, blockScanner);

		modules.add(autoTotem);
		modules.add(stunSlam);
		modules.add(shieldDisable);
		modules.add(breachSwap);
		modules.add(fastAnchor);
		modules.add(freeCam);
		modules.add(esp);
		modules.add(tracer);

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

		boolean paused = ConfigManager.get().general.pauseWhenScreenOpen && minecraft.gui.screen() != null;

		for (Module module : modules) {
			boolean enabled = module.isEnabled();
			boolean previously = Boolean.TRUE.equals(wasEnabled.put(module, enabled));

			if (!enabled) {
				// Covers being switched off from the settings screen, where no
				// hotkey ran to tidy up after the module.
				if (previously) {
					module.onStop();
				}

				continue;
			}

			if (paused && !module.runsWhileScreenOpen()) {
				continue;
			}

			module.onTick(minecraft);
		}

		tickBlockScanner(minecraft);
		scheduler.tick(ConfigManager.get().general.maxActionsPerTick);
	}

	/**
	 * Sweeps for highlighted blocks only while something actually draws them.
	 */
	private void tickBlockScanner(Minecraft minecraft) {
		UtilitiesScarceConfig config = ConfigManager.get();
		boolean wanted = (config.esp.enabled && config.esp.showBlocks)
				|| (config.tracer.enabled && config.tracer.showBlocks);

		if (!wanted) {
			blockScanner.clear();
			return;
		}

		UtilitiesScarceConfig.Targets targets = config.targets;
		blockScanner.tick(minecraft, HighlightTargets.blocks(targets.blocks),
				DetectionRange.blocks(minecraft, targets), targets.maxBlocks, targets.scanBudget);
	}

	/** Forwards the level render pass to whichever modules draw. */
	public void onLevelRender(LevelRenderContext context) {
		if (Minecraft.getInstance().player == null) {
			return;
		}

		for (Module module : modules) {
			if (module.isEnabled()) {
				module.onRender(context);
			}
		}
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
		wasEnabled.clear();
		blockScanner.clear();

		for (Module module : modules) {
			module.onStop();
		}
	}
}
