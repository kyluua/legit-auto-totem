package dev.kyluua.utilitiesscarce.module;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.util.ActionScheduler;
import dev.kyluua.utilitiesscarce.util.InventoryHelper;
import dev.kyluua.utilitiesscarce.util.ItemHelper;
import dev.kyluua.utilitiesscarce.util.Sequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Puts a fresh totem back into the exact slot the last one left.
 *
 * <p>Every tick the module records which hand slots hold a totem. When a slot
 * that held one is suddenly empty -- which is what a pop looks like from the
 * client's side -- a replacement is moved in from the rest of the inventory,
 * into that same hotbar slot or back into the offhand.
 */
public final class AutoTotemModule extends Module {
	private static final String OWNER = "auto_totem";
	/** How long after taking damage a vacancy still counts as a pop. */
	private static final int DAMAGE_WINDOW_TICKS = 10;
	/** Nothing is pending. */
	private static final int NO_SLOT = Integer.MIN_VALUE;
	/** How long to wait for a refill click to show up before giving up on it. */
	private static final int PENDING_TIMEOUT_TICKS = 20;

	private final boolean[] hotbarHadTotem = new boolean[InventoryHelper.HOTBAR_SIZE];
	private boolean offhandHadTotem;
	private boolean primed;
	private int cooldown;
	private int damageTimer;
	private int pendingSlot = NO_SLOT;
	private int pendingTicks;

	public AutoTotemModule(ActionScheduler scheduler) {
		super("auto_totem", scheduler);
	}

	@Override
	public boolean isEnabled() {
		return ConfigManager.get().autoTotem.enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		ConfigManager.get().autoTotem.enabled = enabled;
	}

	@Override
	public void onStop() {
		primed = false;
		cooldown = 0;
		damageTimer = 0;
		pendingSlot = NO_SLOT;
		pendingTicks = 0;
	}

	@Override
	public void onTick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;

		if (player == null) {
			return;
		}

		UtilitiesScarceConfig.AutoTotem config = ConfigManager.get().autoTotem;

		if (player.hurtTime > 0) {
			damageTimer = DAMAGE_WINDOW_TICKS;
		} else if (damageTimer > 0) {
			damageTimer--;
		}

		if (cooldown > 0) {
			cooldown--;
		}

		boolean offhandHasTotem = ItemHelper.isTotem(player.getOffhandItem());
		boolean[] hotbarHasTotem = new boolean[InventoryHelper.HOTBAR_SIZE];

		for (int index = 0; index < InventoryHelper.HOTBAR_SIZE; index++) {
			hotbarHasTotem[index] = ItemHelper.isTotem(InventoryHelper.stackAt(player, index));
		}

		int target = findVacancy(config, offhandHasTotem, hotbarHasTotem);

		// Record the new state before bailing out, so a slot is only reported
		// as vacated once.
		offhandHadTotem = offhandHasTotem;
		System.arraycopy(hotbarHasTotem, 0, hotbarHadTotem, 0, hotbarHasTotem.length);

		if (!primed) {
			// First tick in a world: the snapshot is meaningless until now.
			primed = true;
			return;
		}

		// A refill click is optimistic: the client shows the totem arrive at
		// once, but a server that refuses the click leaves the slot empty
		// again. Wait for it to settle instead of firing another click every
		// couple of ticks, which is what turned one pop into a burst.
		if (pendingSlot != NO_SLOT) {
			if (isFilled(pendingSlot, offhandHasTotem, hotbarHasTotem) || --pendingTicks <= 0) {
				pendingSlot = NO_SLOT;
			} else {
				return;
			}
		}

		if (target == Integer.MIN_VALUE || cooldown > 0 || scheduler.isRunning(OWNER)) {
			return;
		}

		if (config.requireRecentDamage && damageTimer <= 0) {
			return;
		}

		if (!InventoryHelper.canClickInventory(minecraft)) {
			return;
		}

		if (InventoryHelper.count(player, ItemHelper::isTotem) <= Math.max(0, config.keepInReserve)) {
			return;
		}

		refill(minecraft, config, target);
	}

	private static boolean isFilled(int slot, boolean offhandHasTotem, boolean[] hotbarHasTotem) {
		if (slot == InventoryHelper.OFFHAND_TARGET) {
			return offhandHasTotem;
		}

		return slot >= 0 && slot < hotbarHasTotem.length && hotbarHasTotem[slot];
	}

	/**
	 * @return the hotbar index that lost its totem,
	 *         {@link InventoryHelper#OFFHAND_TARGET} for the offhand, or
	 *         {@link Integer#MIN_VALUE} when nothing needs refilling
	 */
	private int findVacancy(UtilitiesScarceConfig.AutoTotem config, boolean offhandHasTotem,
			boolean[] hotbarHasTotem) {
		if (config.refillOffhand && offhandHadTotem && !offhandHasTotem) {
			return InventoryHelper.OFFHAND_TARGET;
		}

		if (config.refillHotbar) {
			for (int index = 0; index < InventoryHelper.HOTBAR_SIZE; index++) {
				if (hotbarHadTotem[index] && !hotbarHasTotem[index]) {
					return index;
				}
			}
		}

		return Integer.MIN_VALUE;
	}

	private void refill(Minecraft minecraft, UtilitiesScarceConfig.AutoTotem config, int target) {
		LocalPlayer player = minecraft.player;

		if (player == null) {
			return;
		}

		int source = InventoryHelper.find(player, ItemHelper::isTotem, config.searchOrder);

		if (source == -1 || source == target) {
			return;
		}

		cooldown = Math.max(0, config.cooldownTicks);
		pendingSlot = target;
		pendingTicks = PENDING_TIMEOUT_TICKS;

		Sequence sequence = new Sequence()
				.require(() -> minecraft.player != null && InventoryHelper.canClickInventory(minecraft))
				.run(config.delayTicks, () -> {
					// Re-resolve the source: the inventory may have shifted
					// during the delay.
					LocalPlayer current = minecraft.player;

					if (current == null) {
						return;
					}

					int from = InventoryHelper.find(current, ItemHelper::isTotem, config.searchOrder);

					if (from == -1 || from == target) {
						return;
					}

					boolean moved = target == InventoryHelper.OFFHAND_TARGET
							? InventoryHelper.moveToOffhand(minecraft, from, config.swapMethod)
							: InventoryHelper.moveToHotbarSlot(minecraft, from, target, config.swapMethod);

					if (moved) {
						announce(displayName());
					}
				});

		scheduler.submit(OWNER, sequence);
	}
}
