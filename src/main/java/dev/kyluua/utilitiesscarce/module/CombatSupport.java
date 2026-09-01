package dev.kyluua.utilitiesscarce.module;

import java.util.function.Predicate;

import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.SwapMethod;
import dev.kyluua.utilitiesscarce.util.InventoryHelper;
import dev.kyluua.utilitiesscarce.util.ItemHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Checks shared by the three weapon-swapping modules. */
final class CombatSupport {
	private CombatSupport() {
	}

	/**
	 * Whether the target counts as shielded.
	 *
	 * @param requireBlocking when true the shield has to actually be raised,
	 *                        otherwise merely carrying one is enough
	 */
	static boolean isShielded(Entity target, boolean requireBlocking) {
		if (!(target instanceof LivingEntity living)) {
			return false;
		}

		if (requireBlocking) {
			return living.isBlocking();
		}

		return ItemHelper.isShield(living.getMainHandItem())
				|| ItemHelper.isShield(living.getOffhandItem());
	}

	static boolean passesTargetFilter(Entity target, boolean playersOnly) {
		if (playersOnly) {
			return target instanceof Player;
		}

		return target instanceof LivingEntity;
	}

	/**
	 * Locates a matching item in the hotbar. When it is only in the inventory
	 * and {@code moveToHotbar} is on, a move into the selected slot is started
	 * and {@code -1} is returned -- the item is reachable from the next tick, so
	 * this attempt is skipped rather than acting on the wrong slot.
	 */
	static int hotbarSlotFor(Minecraft minecraft, Predicate<ItemStack> predicate, boolean moveToHotbar,
			SwapMethod swapMethod) {
		return InventoryHelper.ensureInHotbar(minecraft, predicate, moveToHotbar, swapMethod);
	}
}
