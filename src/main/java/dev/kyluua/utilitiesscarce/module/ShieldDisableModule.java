package dev.kyluua.utilitiesscarce.module;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.SwapMethod;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.TriggerMode;
import dev.kyluua.utilitiesscarce.util.ActionScheduler;
import dev.kyluua.utilitiesscarce.util.ClientActions;
import dev.kyluua.utilitiesscarce.util.InventoryHelper;
import dev.kyluua.utilitiesscarce.util.ItemHelper;
import dev.kyluua.utilitiesscarce.util.Sequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Breaks a raised shield with an axe and goes straight back to whatever was
 * held before.
 *
 * <p>In {@code ON_ATTACK} mode it reacts to your own hit; in {@code AUTO} mode
 * it fires as soon as a shielded target is under the crosshair.
 */
public final class ShieldDisableModule extends Module {
	private int cooldown;

	public ShieldDisableModule(ActionScheduler scheduler) {
		super("shield_disable", scheduler);
	}

	@Override
	public boolean isEnabled() {
		return ConfigManager.get().shieldDisable.enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		ConfigManager.get().shieldDisable.enabled = enabled;
	}

	@Override
	public void onStop() {
		cooldown = 0;
	}

	@Override
	public void onTick(Minecraft minecraft) {
		if (cooldown > 0) {
			cooldown--;
		}

		UtilitiesScarceConfig.ShieldDisable config = ConfigManager.get().shieldDisable;

		if (config.triggerMode != TriggerMode.AUTO) {
			return;
		}

		if (!(minecraft.hitResult instanceof EntityHitResult entityHit)
				|| minecraft.hitResult.getType() != HitResult.Type.ENTITY) {
			return;
		}

		// Stun Slam disables the shield itself; let it own the swap instead.
		if (ConfigManager.get().stunSlam.enabled) {
			return;
		}

		start(minecraft, entityHit.getEntity(), config);
	}

	@Override
	public boolean onAttackEntity(Minecraft minecraft, Entity target) {
		UtilitiesScarceConfig.ShieldDisable config = ConfigManager.get().shieldDisable;

		if (config.triggerMode != TriggerMode.ON_ATTACK) {
			return false;
		}

		return start(minecraft, target, config);
	}

	private boolean start(Minecraft minecraft, Entity target, UtilitiesScarceConfig.ShieldDisable config) {
		LocalPlayer player = minecraft.player;

		if (player == null || cooldown > 0 || scheduler.isRunning(HAND_LANE)) {
			return false;
		}

		if (!CombatSupport.passesTargetFilter(target, config.playersOnly)) {
			return false;
		}

		if (!ClientActions.isValidTarget(player, target, config.maxRange)) {
			return false;
		}

		if (!CombatSupport.isShielded(target, config.requireBlocking)) {
			return false;
		}

		int axeSlot = CombatSupport.hotbarSlotFor(minecraft, ItemHelper::isAxe, config.moveToHotbar,
				SwapMethod.SWAP);

		if (axeSlot == -1) {
			return false;
		}

		int originalSlot = player.getInventory().getSelectedSlot();

		if (config.triggerMode == TriggerMode.ON_ATTACK && axeSlot == originalSlot) {
			// The hit that triggered this was already an axe hit.
			return false;
		}

		cooldown = Math.max(0, config.cooldownTicks);

		Sequence sequence = new Sequence()
				.require(() -> minecraft.player != null
						&& ClientActions.isValidTarget(minecraft.player, target, config.maxRange))
				.runLocal(0, () -> InventoryHelper.selectHotbarSlot(player, axeSlot));

		if (config.waitForCooldown) {
			sequence.waitUntil(() -> ClientActions.attackReady(player), config.cooldownTimeoutTicks);
		}

		sequence.run(0, () -> {
			ClientActions.attack(minecraft, target);
			announce(displayName());
		});

		if (config.restoreSlot) {
			sequence.runLocal(config.restoreDelayTicks,
					() -> InventoryHelper.selectHotbarSlot(player, originalSlot));
			sequence.onAbort(() -> InventoryHelper.selectHotbarSlot(player, originalSlot));
		}

		scheduler.submit(HAND_LANE, sequence);
		return true;
	}
}
