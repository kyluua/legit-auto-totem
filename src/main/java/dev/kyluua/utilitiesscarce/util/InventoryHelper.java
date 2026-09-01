package dev.kyluua.utilitiesscarce.util;

import java.util.function.Predicate;

import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.SearchOrder;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.SwapMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Slot arithmetic and the container clicks used to move items around without
 * opening the inventory screen.
 *
 * <p>Two numbering schemes are in play. {@code Inventory} indices run 0-8 for
 * the hotbar and 9-35 for the three storage rows. The container protocol used
 * by {@code handleContainerInput} numbers the player's own menu differently:
 * 9-35 storage, 36-44 hotbar, 45 offhand. {@link #toNetworkSlot(int)} converts.
 */
public final class InventoryHelper {
	public static final int HOTBAR_SIZE = 9;
	/** Number of {@code Inventory} indices covering hotbar plus storage. */
	public static final int MAIN_SIZE = 36;
	/** Container slot id of the offhand in the player's own menu. */
	public static final int OFFHAND_NETWORK_SLOT = 45;
	/** Button value that makes a {@code SWAP} click target the offhand. */
	public static final int OFFHAND_SWAP_BUTTON = 40;
	/** Sentinel used by the modules to mean "the offhand" instead of a hotbar index. */
	public static final int OFFHAND_TARGET = -2;

	private InventoryHelper() {
	}

	/** Maps an {@code Inventory} index to its slot id in the player's own menu. */
	public static int toNetworkSlot(int inventoryIndex) {
		if (inventoryIndex >= 0 && inventoryIndex < HOTBAR_SIZE) {
			return inventoryIndex + 36;
		}

		return inventoryIndex;
	}

	public static ItemStack stackAt(LocalPlayer player, int inventoryIndex) {
		if (inventoryIndex < 0 || inventoryIndex >= MAIN_SIZE) {
			return ItemStack.EMPTY;
		}

		return player.getInventory().getItem(inventoryIndex);
	}

	/** First hotbar index holding a matching stack, or {@code -1}. */
	public static int findInHotbar(LocalPlayer player, Predicate<ItemStack> predicate) {
		for (int index = 0; index < HOTBAR_SIZE; index++) {
			if (predicate.test(stackAt(player, index))) {
				return index;
			}
		}

		return -1;
	}

	/** First storage index (9-35) holding a matching stack, or {@code -1}. */
	public static int findInStorage(LocalPlayer player, Predicate<ItemStack> predicate) {
		for (int index = HOTBAR_SIZE; index < MAIN_SIZE; index++) {
			if (predicate.test(stackAt(player, index))) {
				return index;
			}
		}

		return -1;
	}

	/** First matching index anywhere in hotbar or storage, or {@code -1}. */
	public static int find(LocalPlayer player, Predicate<ItemStack> predicate, SearchOrder order) {
		if (order == SearchOrder.HOTBAR_FIRST) {
			int hotbar = findInHotbar(player, predicate);
			return hotbar != -1 ? hotbar : findInStorage(player, predicate);
		}

		int storage = findInStorage(player, predicate);
		return storage != -1 ? storage : findInHotbar(player, predicate);
	}

	/** Total matching items in hotbar, storage and the offhand. */
	public static int count(LocalPlayer player, Predicate<ItemStack> predicate) {
		int total = 0;

		for (int index = 0; index < MAIN_SIZE; index++) {
			ItemStack stack = stackAt(player, index);

			if (predicate.test(stack)) {
				total += stack.getCount();
			}
		}

		ItemStack offhand = player.getOffhandItem();

		if (predicate.test(offhand)) {
			total += offhand.getCount();
		}

		return total;
	}

	/**
	 * Container clicks address the player's own menu, so they are only valid
	 * while no other screen owns the cursor.
	 */
	public static boolean canClickInventory(Minecraft minecraft) {
		return minecraft.player != null && minecraft.gameMode != null && minecraft.gui.screen() == null;
	}

	private static void click(Minecraft minecraft, int networkSlot, int button, ContainerInput input) {
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.gameMode == null) {
			return;
		}

		minecraft.gameMode.handleContainerInput(player.inventoryMenu.containerId,
				networkSlot, button, input, player);
	}

	/**
	 * Moves the stack at {@code sourceIndex} into the given hotbar slot.
	 *
	 * @return {@code true} if clicks were sent
	 */
	public static boolean moveToHotbarSlot(Minecraft minecraft, int sourceIndex, int hotbarIndex,
			SwapMethod method) {
		LocalPlayer player = minecraft.player;

		if (player == null || !canClickInventory(minecraft)) {
			return false;
		}

		if (sourceIndex < 0 || sourceIndex >= MAIN_SIZE || hotbarIndex < 0 || hotbarIndex >= HOTBAR_SIZE) {
			return false;
		}

		if (sourceIndex == hotbarIndex) {
			return false;
		}

		if (method == SwapMethod.SWAP) {
			// One packet: swap the source slot with the target hotbar slot.
			click(minecraft, toNetworkSlot(sourceIndex), hotbarIndex, ContainerInput.SWAP);
			return true;
		}

		click(minecraft, toNetworkSlot(sourceIndex), 0, ContainerInput.PICKUP);
		click(minecraft, toNetworkSlot(hotbarIndex), 0, ContainerInput.PICKUP);

		if (!player.inventoryMenu.getCarried().isEmpty()) {
			click(minecraft, toNetworkSlot(sourceIndex), 0, ContainerInput.PICKUP);
		}

		return true;
	}

	/**
	 * Moves the stack at {@code sourceIndex} into the offhand.
	 *
	 * @return {@code true} if clicks were sent
	 */
	public static boolean moveToOffhand(Minecraft minecraft, int sourceIndex, SwapMethod method) {
		LocalPlayer player = minecraft.player;

		if (player == null || !canClickInventory(minecraft)) {
			return false;
		}

		if (sourceIndex < 0 || sourceIndex >= MAIN_SIZE) {
			return false;
		}

		if (method == SwapMethod.SWAP) {
			// Button 40 is the offhand's swap button in the player's own menu.
			click(minecraft, toNetworkSlot(sourceIndex), OFFHAND_SWAP_BUTTON, ContainerInput.SWAP);
			return true;
		}

		click(minecraft, toNetworkSlot(sourceIndex), 0, ContainerInput.PICKUP);
		click(minecraft, OFFHAND_NETWORK_SLOT, 0, ContainerInput.PICKUP);

		if (!player.inventoryMenu.getCarried().isEmpty()) {
			click(minecraft, toNetworkSlot(sourceIndex), 0, ContainerInput.PICKUP);
		}

		return true;
	}

	/**
	 * Selects a hotbar slot. The held-item packet is flushed by the game before
	 * the next attack or use, so a swap costs nothing extra on its own.
	 */
	public static void selectHotbarSlot(LocalPlayer player, int hotbarIndex) {
		if (hotbarIndex < 0 || hotbarIndex >= HOTBAR_SIZE) {
			return;
		}

		if (player.getInventory().getSelectedSlot() == hotbarIndex) {
			return;
		}

		player.getInventory().setSelectedSlot(hotbarIndex);
	}

	/**
	 * Finds an item and makes sure it ends up in the hotbar.
	 *
	 * @param moveFromStorage whether an item found outside the hotbar may be
	 *                        pulled into the currently selected slot
	 * @return the hotbar index holding the item, or {@code -1} if none is
	 *         reachable. When a move was needed the item only arrives on the
	 *         following tick, so the caller must re-check.
	 */
	public static int ensureInHotbar(Minecraft minecraft, Predicate<ItemStack> predicate,
			boolean moveFromStorage, SwapMethod method) {
		LocalPlayer player = minecraft.player;

		if (player == null) {
			return -1;
		}

		int hotbar = findInHotbar(player, predicate);

		if (hotbar != -1) {
			return hotbar;
		}

		if (!moveFromStorage) {
			return -1;
		}

		int storage = findInStorage(player, predicate);

		if (storage == -1) {
			return -1;
		}

		// Prefer an empty hotbar slot so the weapon currently in hand survives.
		int free = player.getInventory().getFreeSlot();
		int target = free > -1 && free < HOTBAR_SIZE ? free : player.getInventory().getSelectedSlot();

		moveToHotbarSlot(minecraft, storage, target, method);
		return -1;
	}
}
