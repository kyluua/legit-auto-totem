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
import net.minecraft.world.item.ItemStack;

/**
 * Hit something with a sword or axe and the mace finishes it.
 *
 * <p>Swaps to a Breach-enchanted mace, lands one hit, and returns to the weapon
 * that started the trade.
 */
public final class BreachSwapModule extends Module {
	public BreachSwapModule(ActionScheduler scheduler) {
		super("breach_swap", scheduler);
	}

	@Override
	public boolean isEnabled() {
		return ConfigManager.get().breachSwap.enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		ConfigManager.get().breachSwap.enabled = enabled;
	}

	@Override
	public boolean onAttackEntity(Minecraft minecraft, Entity target) {
		UtilitiesScarceConfig.BreachSwap config = ConfigManager.get().breachSwap;
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

		ItemStack held = player.getMainHandItem();
		boolean fromSword = config.triggerWithSword && ItemHelper.isSword(held);
		boolean fromAxe = config.triggerWithAxe && ItemHelper.isAxe(held);

		if (!fromSword && !fromAxe) {
			return false;
		}

		int maceSlot = CombatSupport.hotbarSlotFor(minecraft,
				stack -> ItemHelper.isBreachMace(stack, config.requireBreach, config.minBreachLevel),
				config.moveToHotbar, SwapMethod.SWAP);

		if (maceSlot == -1) {
			return false;
		}

		int originalSlot = player.getInventory().getSelectedSlot();

		if (maceSlot == originalSlot) {
			return false;
		}

		Sequence sequence = new Sequence()
				.require(() -> minecraft.player != null
						&& ClientActions.isValidTarget(minecraft.player, target, config.maxRange))
				.run(config.maceDelayTicks,
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
