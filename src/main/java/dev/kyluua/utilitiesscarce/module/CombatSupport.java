package dev.kyluua.utilitiesscarce.module;

import java.util.function.Predicate;

import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.SwapMethod;
import dev.kyluua.utilitiesscarce.util.InventoryHelper;
import dev.kyluua.utilitiesscarce.util.ItemHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Checks shared by the three weapon-swapping modules. */
final class CombatSupport {
	/** Minimum gap between two pulls out of storage, in milliseconds. */
	private static final long PULL_INTERVAL_MS = 500L;

	private static long lastPullAt;

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
		LocalPlayer player = minecraft.player;

		if (player == null) {
			return -1;
		}

		int hotbar = InventoryHelper.findInHotbar(player, predicate);

		if (hotbar != -1 || !moveToHotbar) {
			return hotbar;
		}

		// A pull sends a container click that only pays off on the following
		// tick, and the caller bails out meanwhile. Without this guard an
		// auto-triggering module would send one every tick for as long as the
		// item stayed out of the hotbar -- twenty packets a second going
		// nowhere.
		long now = System.currentTimeMillis();

		if (now - lastPullAt < PULL_INTERVAL_MS) {
			return -1;
		}

		lastPullAt = now;
		return InventoryHelper.ensureInHotbar(minecraft, predicate, true, swapMethod);
	}
}
