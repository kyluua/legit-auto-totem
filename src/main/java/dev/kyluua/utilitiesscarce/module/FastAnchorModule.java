package dev.kyluua.utilitiesscarce.module;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.SwapMethod;
import dev.kyluua.utilitiesscarce.util.ActionScheduler;
import dev.kyluua.utilitiesscarce.util.ClientActions;
import dev.kyluua.utilitiesscarce.util.InventoryHelper;
import dev.kyluua.utilitiesscarce.util.ItemHelper;
import dev.kyluua.utilitiesscarce.util.Sequence;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Charges a respawn anchor the moment it is placed, then puts a totem back in
 * hand.
 *
 * <p>Placement is confirmed by looking for the block rather than assuming it
 * landed, and the charging clicks go through the shared per-tick action budget,
 * so the whole thing costs one glowstone use plus the hotbar changes.
 */
public final class FastAnchorModule extends Module {
	/** Ticks to keep looking for the freshly placed anchor. */
	private static final int PLACEMENT_WINDOW_TICKS = 4;
	private static final int MAX_CHARGE = 4;

	private BlockPos primaryCandidate;
	private BlockPos secondaryCandidate;
	private int placementTimer;

	public FastAnchorModule(ActionScheduler scheduler) {
		super("fast_anchor", scheduler);
	}

	@Override
	public boolean isEnabled() {
		return ConfigManager.get().fastAnchor.enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		ConfigManager.get().fastAnchor.enabled = enabled;
	}

	@Override
	public void onStop() {
		clearPending();
	}

	@Override
	public void onUseBlock(Minecraft minecraft, InteractionHand hand, BlockHitResult hitResult) {
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.level == null || hand != InteractionHand.MAIN_HAND) {
			return;
		}

		if (!ItemHelper.isRespawnAnchor(player.getMainHandItem())) {
			return;
		}

		UtilitiesScarceConfig.FastAnchor config = ConfigManager.get().fastAnchor;

		if (config.onlyWhereExplosive && minecraft.level.dimension() == Level.NETHER) {
			// There an anchor sets your spawn instead of exploding.
			return;
		}

		// The anchor lands either in the clicked block, if that block was
		// replaceable, or against the face that was clicked.
		primaryCandidate = hitResult.getBlockPos().relative(hitResult.getDirection());
		secondaryCandidate = hitResult.getBlockPos();
		placementTimer = PLACEMENT_WINDOW_TICKS;
	}

	@Override
	public void onTick(Minecraft minecraft) {
		if (placementTimer <= 0) {
			return;
		}

		placementTimer--;

		if (minecraft.level == null || minecraft.player == null) {
			clearPending();
			return;
		}

		BlockPos anchor = resolveAnchor(minecraft, primaryCandidate);

		if (anchor == null) {
			anchor = resolveAnchor(minecraft, secondaryCandidate);
		}

		if (anchor == null) {
			if (placementTimer <= 0) {
				clearPending();
			}

			return;
		}

		clearPending();

		if (!scheduler.isRunning(HAND_LANE)) {
			charge(minecraft, anchor, ConfigManager.get().fastAnchor);
		}
	}

	/** Returns the position if an anchor with room for more charge is there. */
	private BlockPos resolveAnchor(Minecraft minecraft, BlockPos pos) {
		if (pos == null || minecraft.level == null) {
			return null;
		}

		BlockState state = minecraft.level.getBlockState(pos);

		if (!(state.getBlock() instanceof RespawnAnchorBlock)) {
			return null;
		}

		int charge = state.getOptionalValue(RespawnAnchorBlock.CHARGE).orElse(0);
		return charge < MAX_CHARGE ? pos : null;
	}

	private void clearPending() {
		primaryCandidate = null;
		secondaryCandidate = null;
		placementTimer = 0;
	}

	private void charge(Minecraft minecraft, BlockPos anchor, UtilitiesScarceConfig.FastAnchor config) {
		LocalPlayer player = minecraft.player;

		if (player == null) {
			return;
		}

		int glowstoneSlot = CombatSupport.hotbarSlotFor(minecraft, ItemHelper::isGlowstone,
				config.moveToHotbar, SwapMethod.SWAP);

		if (glowstoneSlot == -1) {
			announce(Component.literal("Fast Anchor: no glowstone available"));
			return;
		}

		int originalSlot = player.getInventory().getSelectedSlot();
		int charges = Math.max(1, Math.min(MAX_CHARGE, config.charges));

		Sequence sequence = new Sequence()
				.require(() -> minecraft.player != null && minecraft.level != null)
				.run(0, () -> InventoryHelper.selectHotbarSlot(player, glowstoneSlot));

		for (int index = 0; index < charges; index++) {
			// Keep repeat charges at least a tick apart so the server sees them
			// as separate interactions.
			int delay = index == 0 ? Math.max(0, config.chargeDelayTicks)
					: Math.max(1, config.chargeDelayTicks);

			sequence.run(delay, () -> ClientActions.useItemOn(minecraft, chargeHit(minecraft, anchor)));
		}

		sequence.run(config.swapDelayTicks, () -> swapAfterCharge(minecraft, config, originalSlot));
		sequence.onAbort(() -> swapAfterCharge(minecraft, config, originalSlot));

		scheduler.submit(HAND_LANE, sequence);
		announce(displayName());
	}

	/** Aims at the anchor: reuse the live crosshair hit when it is on the block. */
	private static BlockHitResult chargeHit(Minecraft minecraft, BlockPos anchor) {
		if (minecraft.hitResult instanceof BlockHitResult blockHit
				&& blockHit.getBlockPos().equals(anchor)) {
			return blockHit;
		}

		Vec3 topFace = new Vec3(anchor.getX() + 0.5D, anchor.getY() + 1.0D, anchor.getZ() + 0.5D);
		return new BlockHitResult(topFace, Direction.UP, anchor, false);
	}

	private static void swapAfterCharge(Minecraft minecraft, UtilitiesScarceConfig.FastAnchor config,
			int originalSlot) {
		LocalPlayer player = minecraft.player;

		if (player == null) {
			return;
		}

		int slot = switch (config.swapTarget) {
			case TOTEM -> InventoryHelper.findInHotbar(player, ItemHelper::isTotem);
			case ANCHOR -> InventoryHelper.findInHotbar(player, ItemHelper::isRespawnAnchor);
			case GLOWSTONE -> player.getInventory().getSelectedSlot();
			case NONE -> originalSlot;
		};

		if (slot != -1) {
			InventoryHelper.selectHotbarSlot(player, slot);
		}
	}
}
