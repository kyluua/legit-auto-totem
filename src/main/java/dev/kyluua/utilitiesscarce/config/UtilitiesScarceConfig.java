package dev.kyluua.utilitiesscarce.config;

/**
 * Plain data holder for every setting in the mod. Serialised to
 * {@code config/utilitiesscarce.json} by {@link ConfigManager}.
 *
 * <p>Fields are public and mutable so that both the Cloth Config screen and the
 * hotkey toggles can write to the same instance.
 */
public final class UtilitiesScarceConfig {
	public General general = new General();
	public AutoTotem autoTotem = new AutoTotem();
	public StunSlam stunSlam = new StunSlam();
	public ShieldDisable shieldDisable = new ShieldDisable();
	public BreachSwap breachSwap = new BreachSwap();
	public FastAnchor fastAnchor = new FastAnchor();

	/** How an item is pulled out of the inventory into a hand slot. */
	public enum SwapMethod {
		/**
		 * One {@code SWAP} container click. Cheapest option: a single packet
		 * moves the item straight into the target slot.
		 */
		SWAP,
		/**
		 * Pick the stack up, drop it in the target slot, put the displaced
		 * stack back. Two or three packets, but it behaves like a real mouse
		 * click, which some anticheats are happier with.
		 */
		PICKUP
	}

	/** Which part of the inventory is searched first when looking for an item. */
	public enum SearchOrder {
		INVENTORY_FIRST,
		HOTBAR_FIRST
	}

	/** When a module is allowed to start its sequence. */
	public enum TriggerMode {
		/** Only when you actually attack the target yourself. */
		ON_ATTACK,
		/** As soon as the target under your crosshair meets the conditions. */
		AUTO
	}

	/** What Fast Anchor selects once the anchor is charged. */
	public enum AnchorSwapTarget {
		TOTEM,
		ANCHOR,
		GLOWSTONE,
		NONE
	}

	public static final class General {
		/** Print a chat line when a hotkey toggles a module. */
		public boolean notifyOnToggle = true;
		/** Print a chat line every time a module fires. Noisy; off by default. */
		public boolean notifyOnAction = false;
		/**
		 * Upper bound on packet-producing steps executed per client tick,
		 * across all modules. Keeps bursts small.
		 */
		public int maxActionsPerTick = 2;
		/** Do nothing while an inventory or other screen is open. */
		public boolean pauseWhenScreenOpen = true;
	}

	public static final class AutoTotem {
		public boolean enabled = true;
		/** Refill the offhand when the totem there is gone. */
		public boolean refillOffhand = true;
		/** Refill a hotbar slot that lost its totem, into that same slot. */
		public boolean refillHotbar = true;
		/**
		 * Require the player to have taken damage in the last few ticks, so the
		 * module only reacts to an actual pop and not to you moving a totem
		 * around by hand.
		 */
		public boolean requireRecentDamage = false;
		/** Ticks between spotting the empty slot and refilling it. */
		public int delayTicks = 0;
		/** Minimum ticks between two refills. */
		public int cooldownTicks = 2;
		/** Leave this many totems untouched in the inventory. */
		public int keepInReserve = 0;
		public SwapMethod swapMethod = SwapMethod.SWAP;
		public SearchOrder searchOrder = SearchOrder.INVENTORY_FIRST;
	}

	public static final class StunSlam {
		public boolean enabled = false;
		/** Only run when the target is actually holding its shield up. */
		public boolean requireBlocking = true;
		public boolean playersOnly = true;
		public double maxRange = 4.0D;
		/** Ticks to wait before the axe hit. */
		public int axeDelayTicks = 0;
		/** Ticks to wait between the axe hit and the mace hit. */
		public int maceDelayTicks = 0;
		/** Hold the mace hit until the attack cooldown has recharged. */
		public boolean waitForCooldown = true;
		/** Give up waiting for the cooldown after this many ticks. */
		public int cooldownTimeoutTicks = 40;
		/** Return to the slot that was selected before the sequence. */
		public boolean restoreSlot = true;
		public int restoreDelayTicks = 2;
		/** Allow pulling the axe or mace out of the inventory into the hotbar. */
		public boolean moveToHotbar = false;
	}

	public static final class ShieldDisable {
		public boolean enabled = false;
		public TriggerMode triggerMode = TriggerMode.ON_ATTACK;
		/** Only run when the target is actually holding its shield up. */
		public boolean requireBlocking = true;
		public boolean playersOnly = true;
		public double maxRange = 4.0D;
		public boolean waitForCooldown = false;
		public int cooldownTimeoutTicks = 40;
		public boolean restoreSlot = true;
		public int restoreDelayTicks = 2;
		public boolean moveToHotbar = false;
		/** Minimum ticks between two disable attempts on the same target. */
		public int cooldownTicks = 10;
	}

	public static final class BreachSwap {
		public boolean enabled = false;
		public boolean triggerWithSword = true;
		public boolean triggerWithAxe = true;
		/** Only swap to a mace that actually carries Breach. */
		public boolean requireBreach = true;
		public int minBreachLevel = 1;
		public boolean playersOnly = false;
		public double maxRange = 4.0D;
		public int maceDelayTicks = 0;
		public boolean waitForCooldown = true;
		public int cooldownTimeoutTicks = 40;
		public boolean restoreSlot = true;
		public int restoreDelayTicks = 2;
		public boolean moveToHotbar = false;
	}

	public static final class FastAnchor {
		public boolean enabled = false;
		/** How many glowstone charges to put in. One is enough to detonate. */
		public int charges = 1;
		public int chargeDelayTicks = 1;
		public int swapDelayTicks = 0;
		public AnchorSwapTarget swapTarget = AnchorSwapTarget.TOTEM;
		/**
		 * Skip dimensions where a respawn anchor sets your spawn instead of
		 * exploding (the Nether), so charging does not waste glowstone.
		 */
		public boolean onlyWhereExplosive = true;
		public boolean moveToHotbar = false;
	}
}
