package dev.kyluua.utilitiesscarce.config;

import java.util.ArrayList;
import java.util.List;

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
	public FreeCam freeCam = new FreeCam();
	public Targets targets = new Targets();
	public Esp esp = new Esp();
	public Tracer tracer = new Tracer();

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
		public int cooldownTicks = 5;
		/** Leave this many totems untouched in the inventory. */
		public int keepInReserve = 0;
		public SwapMethod swapMethod = SwapMethod.SWAP;
		public SearchOrder searchOrder = SearchOrder.INVENTORY_FIRST;
	}

	public static final class StunSlam {
		public boolean enabled = false;
		/**
		 * Whether the axe step needs the shield actually raised, or merely
		 * carried. Either way the mace hit still happens without a shield.
		 */
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
		public int cooldownTicks = 20;
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

	/** How a highlight picks its colour. */
	public enum ColorMode {
		/** The configured colour, as-is. */
		STATIC,
		/** Cycles through the hue wheel. */
		RAINBOW
	}

	/** Where a tracer line starts from. */
	public enum TracerOrigin {
		/** Just in front of the camera, so lines fan out from the crosshair. */
		CROSSHAIR,
		/** The camera itself. */
		EYES,
		/** The player's feet. */
		FEET
	}

	/**
	 * What ESP and Tracer highlight. Shared by both so the block sweep only has
	 * to run once, and so "what to show" is decided in one place while each
	 * module decides how to draw it.
	 */
	public static final class Targets {
		public boolean players = true;
		public boolean hostiles = true;
		public boolean passives = false;
		public boolean items = true;
		/** Anything that is not a player, a mob or a dropped item. */
		public boolean others = false;
		public boolean ignoreInvisible = false;
		/**
		 * Detect as far as the client can see. Entities use the render distance
		 * directly; the block sweep uses it too but stops at
		 * {@link #blockRangeLimit}, because sweep cost grows with the cube of
		 * the radius.
		 */
		public boolean useRenderDistance = true;
		/** Entity range when not following the render distance. */
		public double entityRange = 96.0D;
		/** Block ids to look for. Unknown or malformed entries are ignored. */
		public List<String> blocks = new ArrayList<>(List.of(
				"minecraft:ancient_debris",
				"minecraft:diamond_ore",
				"minecraft:deepslate_diamond_ore"));
		/** Block search radius when not following the render distance. */
		public int blockRange = 32;
		/** Ceiling on the block radius when following the render distance. */
		public int blockRangeLimit = 48;
		/** Cap on highlighted blocks, so a vein-rich area cannot stall a frame. */
		public int maxBlocks = 512;
		/**
		 * Block positions checked per tick. The sweep resumes where it stopped,
		 * so this bounds the per-tick cost no matter how large the radius is.
		 */
		public int scanBudget = 24000;
	}

	public static final class Esp {
		public boolean enabled = false;
		public boolean showEntities = true;
		public boolean showBlocks = true;
		/** Draw through terrain. Off makes it an outline you only see in the open. */
		public boolean throughWalls = true;
		public ColorMode colorMode = ColorMode.STATIC;
		public int entityColor = 0xFFFF5555;
		public int blockColor = 0xFF55FF55;
		/** Full hue cycles per second in RAINBOW mode. */
		public double rainbowSpeed = 0.5D;
		/** Hue offset between consecutive targets, 0 to 1. */
		public double rainbowSpread = 0.05D;
		/** Opacity used in RAINBOW mode, 0 to 255. */
		public int rainbowAlpha = 255;
		public double lineWidth = 2.0D;
	}

	public static final class Tracer {
		public boolean enabled = false;
		public boolean showEntities = true;
		public boolean showBlocks = false;
		public boolean throughWalls = true;
		public ColorMode colorMode = ColorMode.STATIC;
		public int entityColor = 0xFFFF5555;
		public int blockColor = 0xFF55FF55;
		/** Full hue cycles per second in RAINBOW mode. */
		public double rainbowSpeed = 0.5D;
		/** Hue offset between consecutive targets, 0 to 1. */
		public double rainbowSpread = 0.05D;
		/** Opacity used in RAINBOW mode, 0 to 255. */
		public int rainbowAlpha = 255;
		public double lineWidth = 1.5D;
		public TracerOrigin origin = TracerOrigin.CROSSHAIR;
		/** How far in front of the camera CROSSHAIR lines begin. */
		public double originDistance = 1.0D;
	}

	public static final class FreeCam {
		public boolean enabled = false;
		/** Camera speed in blocks per tick. 0.8 is roughly 16 blocks a second. */
		public double moveSpeed = 0.8D;
		/** Speed multiplier while the sprint key is held. */
		public double sprintMultiplier = 3.0D;
		/**
		 * Fly along the look direction instead of level. Off keeps movement
		 * flat so you can look down at your body while flying sideways.
		 */
		public boolean followPitch = false;
		/** Maximum distance the camera may stray from the body; 0 is unlimited. */
		public double maxDistance = 0.0D;
		/** Snap back to the body when something hurts you. */
		public boolean disableOnDamage = true;
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
