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
 * Swaps to a mace and lands the follow-up hit on whatever you just attacked.
 *
 * <p>The mace hit always happens. The axe is only a preamble: when the target
 * has a shield up, it breaks that first so the mace is not blocked. No shield,
 * or no axe to hand, and the sequence goes straight to the mace.
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

		// The mace hit is the point of the module and always happens. The axe
		// is only for getting a shield out of the way first.
		int maceSlot = CombatSupport.hotbarSlotFor(minecraft, ItemHelper::isMace, config.moveToHotbar,
				SwapMethod.SWAP);

		if (maceSlot == -1) {
			return false;
		}

		int originalSlot = player.getInventory().getSelectedSlot();
		boolean shielded = CombatSupport.isShielded(target, config.requireBlocking);

		int axeSlot = shielded
				? CombatSupport.hotbarSlotFor(minecraft, ItemHelper::isAxe, config.moveToHotbar,
						SwapMethod.SWAP)
				: -1;

		// Already holding the mace against an unshielded target: the hit that
		// triggered this was the mace hit, so adding another would just be a
		// free double.
		if (!shielded && maceSlot == originalSlot) {
			return false;
		}

		Sequence sequence = new Sequence()
				.require(() -> minecraft.player != null
						&& ClientActions.isValidTarget(minecraft.player, target, config.maxRange));

		// Break the shield first, when there is one and an axe to do it with.
		// Skipped when the triggering hit was already an axe hit, so the shield
		// is not struck twice for nothing.
		if (axeSlot != -1 && axeSlot != originalSlot) {
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
