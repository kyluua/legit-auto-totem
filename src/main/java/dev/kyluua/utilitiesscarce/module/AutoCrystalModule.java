package dev.kyluua.utilitiesscarce.module;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.util.ActionScheduler;
import dev.kyluua.utilitiesscarce.util.ClientActions;
import dev.kyluua.utilitiesscarce.util.InventoryHelper;
import dev.kyluua.utilitiesscarce.util.ItemHelper;
import dev.kyluua.utilitiesscarce.util.Sequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Places an end crystal on whatever obsidian the crosshair is resting on,
 * whatever is in your hand.
 *
 * <p>The point is not having to hold a crystal out. Look at obsidian with a
 * sword in hand and crystals still go down: the offhand is used when a crystal
 * is there, which costs no hotbar change at all, and otherwise the hotbar slot
 * is borrowed for the placement and the sword comes straight back.
 *
 * <p>Whether a crystal actually fits is worked out here rather than found out
 * by asking -- the same three tests vanilla makes, so a refused placement is
 * never sent.
 */
public final class AutoCrystalModule extends Module {
	private static final String OWNER = "auto_crystal";

	private int cooldown;

	public AutoCrystalModule(ActionScheduler scheduler) {
		super("auto_crystal", scheduler);
	}

	@Override
	public boolean isEnabled() {
		return ConfigManager.get().autoCrystal.enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		ConfigManager.get().autoCrystal.enabled = enabled;
	}

	@Override
	public void onStop() {
		cooldown = 0;
	}

	@Override
	public void onTick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.level == null) {
			return;
		}

		if (cooldown > 0) {
			cooldown--;
			return;
		}

		UtilitiesScarceConfig.AutoCrystal config = ConfigManager.get().autoCrystal;

		if (config.requireUseKey && !minecraft.options.keyUse.isDown()) {
			return;
		}

		// Never fight another module over the hotbar.
		if (scheduler.isRunning(OWNER) || scheduler.isRunning(HAND_LANE)) {
			return;
		}

		if (!(minecraft.hitResult instanceof BlockHitResult blockHit)) {
			return;
		}

		BlockPos base = blockHit.getBlockPos();

		if (!inRange(player, base, config.maxRange) || !canPlaceOn(minecraft.level, base, config)) {
			return;
		}

		if (config.useOffhand && ItemHelper.isEndCrystal(player.getOffhandItem())) {
			placeFromOffhand(minecraft, config, blockHit);
			return;
		}

		placeFromHotbar(minecraft, config, blockHit);
	}

	/**
	 * One use packet and nothing else: the main hand is not touched, so a sword
	 * stays a sword throughout.
	 */
	private void placeFromOffhand(Minecraft minecraft, UtilitiesScarceConfig.AutoCrystal config,
			BlockHitResult blockHit) {
		Sequence sequence = new Sequence()
				.require(() -> minecraft.player != null && minecraft.level != null)
				.run(0, () -> ClientActions.useItemOn(minecraft, InteractionHand.OFF_HAND, blockHit));

		start(sequence, config);
	}

	/** Borrows a hotbar slot, places, and gives the slot straight back. */
	private void placeFromHotbar(Minecraft minecraft, UtilitiesScarceConfig.AutoCrystal config,
			BlockHitResult blockHit) {
		LocalPlayer player = minecraft.player;

		if (player == null) {
			return;
		}

		int crystalSlot = CombatSupport.hotbarSlotFor(minecraft, ItemHelper::isEndCrystal,
				config.moveToHotbar, config.swapMethod);

		if (crystalSlot == -1) {
			return;
		}

		int originalSlot = player.getInventory().getSelectedSlot();

		Sequence sequence = new Sequence()
				.require(() -> minecraft.player != null && minecraft.level != null);

		if (crystalSlot != originalSlot) {
			sequence.run(0, () -> InventoryHelper.selectHotbarSlot(player, crystalSlot));
		}

		sequence.run(config.placeDelayTicks,
				() -> ClientActions.useItemOn(minecraft, InteractionHand.MAIN_HAND, blockHit));

		if (config.restoreSlot && crystalSlot != originalSlot) {
			sequence.run(config.restoreDelayTicks,
					() -> InventoryHelper.selectHotbarSlot(player, originalSlot));
			// A cancelled sequence must not leave a crystal in hand.
			sequence.onAbort(() -> InventoryHelper.selectHotbarSlot(player, originalSlot));
		}

		start(sequence, config);
	}

	private void start(Sequence sequence, UtilitiesScarceConfig.AutoCrystal config) {
		cooldown = Math.max(1, config.cooldownTicks);
		scheduler.submit(OWNER, sequence);
		announce(displayName());
	}

	private static boolean inRange(LocalPlayer player, BlockPos pos, double maxRange) {
		double limit = Math.max(0.0D, maxRange);
		return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= limit * limit;
	}

	/**
	 * Vanilla's own rules for an end crystal: obsidian or bedrock underneath,
	 * air directly above, and nothing standing in the two blocks the crystal
	 * occupies.
	 */
	private static boolean canPlaceOn(Level level, BlockPos base, UtilitiesScarceConfig.AutoCrystal config) {
		BlockState state = level.getBlockState(base);

		if (!state.is(Blocks.OBSIDIAN) && !(config.allowBedrock && state.is(Blocks.BEDROCK))) {
			return false;
		}

		BlockPos above = base.above();

		if (!level.isEmptyBlock(above)) {
			return false;
		}

		AABB occupied = new AABB(above.getX(), above.getY(), above.getZ(),
				above.getX() + 1.0D, above.getY() + 2.0D, above.getZ() + 1.0D);

		return level.getEntitiesOfClass(Entity.class, occupied).isEmpty();
	}
}
