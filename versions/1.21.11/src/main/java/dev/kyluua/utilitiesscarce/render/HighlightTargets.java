package dev.kyluua.utilitiesscarce.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

/** Turns the shared target settings into concrete blocks and entities. */
public final class HighlightTargets {
	private static List<String> cachedIds = List.of();
	private static Set<Block> cachedBlocks = Set.of();

	private HighlightTargets() {
	}

	/**
	 * Resolves block ids to blocks, skipping anything unparseable or unknown so
	 * one typo in the list does not break the rest. Cached, because this runs
	 * every tick and the list rarely changes.
	 */
	public static Set<Block> blocks(List<String> ids) {
		if (ids == null) {
			return Set.of();
		}

		if (ids.equals(cachedIds)) {
			return cachedBlocks;
		}

		Set<Block> resolved = new HashSet<>();

		for (String raw : ids) {
			if (raw == null || raw.isBlank()) {
				continue;
			}

			Identifier id = Identifier.tryParse(raw.trim());

			if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
				continue;
			}

			resolved.add(BuiltInRegistries.BLOCK.getValue(id));
		}

		cachedIds = List.copyOf(ids);
		cachedBlocks = Set.copyOf(resolved);
		return cachedBlocks;
	}

	/**
	 * @param range how far to look, already resolved from the render distance
	 *              or the manual setting
	 */
	public static List<Entity> entities(Minecraft minecraft, UtilitiesScarceConfig.Targets config,
			double range) {
		ClientLevel level = minecraft.level;
		LocalPlayer player = minecraft.player;

		if (level == null || player == null) {
			return List.of();
		}

		double rangeSq = range * range;
		List<Entity> found = new ArrayList<>();

		for (Entity entity : level.entitiesForRendering()) {
			if (entity == player || entity.isSpectator()) {
				continue;
			}

			if (config.ignoreInvisible && entity.isInvisible()) {
				continue;
			}

			if (player.distanceToSqr(entity) > rangeSq) {
				continue;
			}

			if (matches(entity, config)) {
				found.add(entity);
			}
		}

		return found;
	}

	private static boolean matches(Entity entity, UtilitiesScarceConfig.Targets config) {
		if (entity instanceof Player) {
			return config.players;
		}

		if (entity instanceof ItemEntity) {
			return config.items;
		}

		if (entity instanceof Enemy) {
			return config.hostiles;
		}

		if (entity instanceof Mob) {
			return config.passives;
		}

		return config.others;
	}
}
