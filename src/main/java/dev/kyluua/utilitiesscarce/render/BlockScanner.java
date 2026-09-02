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
 * Finds configured blocks near the player, a few horizontal slabs per tick.
 *
 * <p>Sweeping a 32-block radius in one go is a quarter of a million block
 * lookups; doing it in slabs keeps each tick cheap and refreshes the whole
 * volume roughly once a second. Results are swapped in only when a pass
 * finishes, so what gets drawn is always a complete sweep rather than a
 * half-built one.
 */
public final class BlockScanner {
	private List<BlockPos> results = List.of();
	private List<BlockPos> pending;
	private BlockPos origin;
	private Set<Block> passTargets = Set.of();
	private int passRadius;
	private int cursor;

	/** Positions from the most recently completed sweep. */
	public List<BlockPos> results() {
		return results;
	}

	public void clear() {
		results = List.of();
		pending = null;
		cursor = 0;
	}

	public void tick(Minecraft minecraft, Set<Block> targets, int radius, int maxResults,
			int slabsPerTick) {
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
			cursor = 0;
			pending = new ArrayList<>();
		}

		int span = passRadius * 2 + 1;
		int minY = level.getMinY();
		int maxY = level.getMaxY();
		BlockPos.MutableBlockPos cursorPos = new BlockPos.MutableBlockPos();
		int slabs = Math.max(1, slabsPerTick);

		for (int slab = 0; slab < slabs && cursor < span; slab++, cursor++) {
			int y = origin.getY() - passRadius + cursor;

			if (y < minY || y > maxY) {
				continue;
			}

			if (scanSlab(level, cursorPos, y, maxResults)) {
				// Hit the cap; finish the sweep here rather than keep looking.
				cursor = span;
				break;
			}
		}

		if (cursor >= span) {
			results = List.copyOf(pending);
			pending = null;
		}
	}

	/** @return true once the result cap has been reached */
	private boolean scanSlab(ClientLevel level, BlockPos.MutableBlockPos cursorPos, int y,
			int maxResults) {
		int originX = origin.getX();
		int originZ = origin.getZ();

		for (int dx = -passRadius; dx <= passRadius; dx++) {
			for (int dz = -passRadius; dz <= passRadius; dz++) {
				cursorPos.set(originX + dx, y, originZ + dz);

				if (!passTargets.contains(level.getBlockState(cursorPos).getBlock())) {
					continue;
				}

				pending.add(new BlockPos(originX + dx, y, originZ + dz));

				if (pending.size() >= maxResults) {
					return true;
				}
			}
		}

		return false;
	}
}
