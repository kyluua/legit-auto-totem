package dev.kyluua.utilitiesscarce.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/**
 * Finds configured blocks near the player, a fixed number of positions per tick.
 *
 * <p>A sweep covers the cube of its radius, which at render-distance scale is
 * tens of millions of positions. Rather than let that land on one tick, the
 * sweep walks a linear cursor through the volume and stops when it has spent
 * its budget, resuming there next tick. Cost per tick is therefore flat no
 * matter how far the search reaches; only the time to complete a pass grows.
 *
 * <p>Results are swapped in only when a pass finishes, so what gets drawn is
 * always a complete sweep rather than a half-built one.
 */
public final class BlockScanner {
	private List<BlockPos> results = List.of();
	private List<BlockPos> pending;
	private BlockPos origin;
	private Set<Block> passTargets = Set.of();
	private int passRadius;
	private long cursor;

	/** Positions from the most recently completed sweep. */
	public List<BlockPos> results() {
		return results;
	}

	public void clear() {
		results = List.of();
		pending = null;
		cursor = 0L;
	}

	public void tick(Minecraft minecraft, Set<Block> targets, int radius, int maxResults, int budget) {
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;

		if (player == null || level == null || targets.isEmpty() || radius <= 0 || maxResults <= 0) {
			clear();
			return;
		}

		if (pending == null) {
			// Start a fresh sweep, pinned to where the player is right now.
			origin = player.blockPosition();
			passTargets = targets;
			passRadius = radius;
			cursor = 0L;
			pending = new ArrayList<>();
		}

		int span = passRadius * 2 + 1;
		long total = (long) span * span * span;
		int remaining = Math.max(1, budget);
		int minY = level.getMinY();
		int maxY = level.getMaxY();
		BlockPos.MutableBlockPos cursorPos = new BlockPos.MutableBlockPos();

		while (remaining > 0 && cursor < total) {
			remaining--;
			long index = cursor++;

			int dz = (int) (index % span);
			long rest = index / span;
			int dx = (int) (rest % span);
			int dy = (int) (rest / span);

			int y = origin.getY() - passRadius + dy;

			if (y < minY || y > maxY) {
				continue;
			}

			int x = origin.getX() - passRadius + dx;
			int z = origin.getZ() - passRadius + dz;
			cursorPos.set(x, y, z);

			if (!passTargets.contains(level.getBlockState(cursorPos).getBlock())) {
				continue;
			}

			pending.add(new BlockPos(x, y, z));

			if (pending.size() >= maxResults) {
				// Hit the cap; end the pass here rather than keep looking.
				cursor = total;
			}
		}

		if (cursor >= total) {
			results = List.copyOf(pending);
			pending = null;
		}
	}
}
