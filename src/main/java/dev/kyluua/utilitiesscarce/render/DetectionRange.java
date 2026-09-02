package dev.kyluua.utilitiesscarce.render;

import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Turns the render distance into the ranges the highlight modules search. */
public final class DetectionRange {
	private static final int CHUNK_BLOCKS = 16;

	private DetectionRange() {
	}

	/** Render distance in blocks, or 0 when it cannot be read yet. */
	public static double renderDistanceBlocks(Minecraft minecraft) {
		if (minecraft.options == null) {
			return 0.0D;
		}

		return (double) minecraft.options.getEffectiveRenderDistance() * CHUNK_BLOCKS;
	}

	/**
	 * Entities are cheap to filter and the client only knows about ones nearby
	 * anyway, so this follows the render distance exactly.
	 */
	public static double entities(Minecraft minecraft, UtilitiesScarceConfig.Targets targets) {
		if (!targets.useRenderDistance) {
			return Math.max(0.0D, targets.entityRange);
		}

		double distance = renderDistanceBlocks(minecraft);
		return distance > 0.0D ? distance : Math.max(0.0D, targets.entityRange);
	}

	/**
	 * Blocks follow the render distance too, but stop at the configured limit.
	 * A sweep covers the cube of the radius, so letting it run to a 32-chunk
	 * view would mean tens of millions of positions per pass and a refresh
	 * measured in minutes -- slower than useless for finding ore.
	 */
	public static int blocks(Minecraft minecraft, UtilitiesScarceConfig.Targets targets) {
		if (!targets.useRenderDistance) {
			return Math.max(0, targets.blockRange);
		}

		int limit = Math.max(1, targets.blockRangeLimit);
		int fromRenderDistance = (int) renderDistanceBlocks(minecraft);

		if (fromRenderDistance <= 0) {
			return Math.min(limit, Math.max(0, targets.blockRange));
		}

		return Mth.clamp(fromRenderDistance, 1, limit);
	}
}
