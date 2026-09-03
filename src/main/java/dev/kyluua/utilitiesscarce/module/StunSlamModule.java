package dev.kyluua.utilitiesscarce.module;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.SwapMethod;
import dev.kyluua.utilitiesscarce.util.ActionScheduler;
import dev.kyluua.utilitiesscarce.util.ClientActions;
import dev.kyluua.utilitiesscarce.util.InventoryHelper;
import dev.kyluua.utilitiesscarce.util.ItemHelper;
import dev.kyluua.utilitiesscarce.util.Sequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Axe into mace: breaks the target's shield, then swaps to the mace and lands
 * the follow-up hit.
 *
 * <p>By default the mace hit waits for the attack cooldown to recharge, because
 * a mace swung on a cold cooldown does a fraction of its damage. Turn
 * {@code waitForCooldown} off for a faster, weaker follow-up.
 */
public final class StunSlamModule extends Module {
	public StunSlamModule(ActionScheduler scheduler) {
		super("stun_slam", scheduler);
	}

	@Override
	public boolean isEnabled() {
		return ConfigManager.get().stunSlam.enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		ConfigManager.get().stunSlam.enabled = enabled;
	}

	@Override
	public boolean onAttackEntity(Minecraft minecraft, Entity target) {
		UtilitiesScarceConfig.StunSlam config = ConfigManager.get().stunSlam;
		LocalPlayer player = minecraft.player;

		if (player == null || scheduler.isRunning(HAND_LANE)) {
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
		int maceSlot = CombatSupport.hotbarSlotFor(minecraft, ItemHelper::isMace, config.moveToHotbar,
				SwapMethod.SWAP);

		if (axeSlot == -1 || maceSlot == -1) {
			return false;
		}

		int originalSlot = player.getInventory().getSelectedSlot();

		Sequence sequence = new Sequence()
				.require(() -> minecraft.player != null
						&& ClientActions.isValidTarget(minecraft.player, target, config.maxRange));

		// The axe hit. Skipped when the attack that triggered this was already
		// an axe hit, so the shield is not struck twice for nothing.
		if (axeSlot != originalSlot) {
			sequence.run(config.axeDelayTicks,
					() -> InventoryHelper.selectHotbarSlot(player, axeSlot));
			sequence.run(0, () -> ClientActions.attack(minecraft, target));
		}

		sequence.run(config.maceDelayTicks,
				() -> InventoryHelper.selectHotbarSlot(player, maceSlot));

		if (config.waitForCooldown) {
			sequence.waitUntil(() -> ClientActions.attackReady(player), config.cooldownTimeoutTicks);
		}

		sequence.run(0, () -> {
			ClientActions.attack(minecraft, target);
			announce(displayName());
		});

		if (config.restoreSlot) {
			sequence.run(config.restoreDelayTicks,
					() -> InventoryHelper.selectHotbarSlot(player, originalSlot));
			sequence.onAbort(() -> InventoryHelper.selectHotbarSlot(player, originalSlot));
		}

		scheduler.submit(HAND_LANE, sequence);
		return true;
	}
}
