package dev.kyluua.utilitiesscarce.module;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.render.BlockScanner;
import dev.kyluua.utilitiesscarce.render.EspRenderTypes;
import dev.kyluua.utilitiesscarce.render.HighlightTargets;
import dev.kyluua.utilitiesscarce.render.RenderHelper;
import dev.kyluua.utilitiesscarce.util.ActionScheduler;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a box around every configured entity and block.
 *
 * <p>What counts as a target lives in the shared Targets section, so ESP and
 * Tracer agree on it and the block sweep only runs once. This module owns just
 * the presentation: which of the two kinds to draw, the colours, the line
 * weight, and whether the boxes show through terrain.
 */
public final class EspModule extends Module {
	private final BlockScanner blockScanner;

	public EspModule(ActionScheduler scheduler, BlockScanner blockScanner) {
		super("esp", scheduler);
		this.blockScanner = blockScanner;
	}

	@Override
	public boolean isEnabled() {
		return ConfigManager.get().esp.enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		ConfigManager.get().esp.enabled = enabled;
	}

	@Override
	public void onRender(LevelRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.level == null) {
			return;
		}

		UtilitiesScarceConfig config = ConfigManager.get();
		UtilitiesScarceConfig.Esp esp = config.esp;

		List<AABB> entityBoxes = esp.showEntities
				? entityBoxes(minecraft, config.targets)
				: List.of();
		List<AABB> blockBoxes = esp.showBlocks
				? blockBoxes(blockScanner.results())
				: List.of();

		if (entityBoxes.isEmpty() && blockBoxes.isEmpty()) {
			return;
		}

		Vec3 camera = context.levelState().cameraRenderState.pos;
		PoseStack poseStack = context.poseStack();
		float width = (float) Math.max(0.5D, esp.lineWidth);

		poseStack.pushPose();
		// Geometry below is in world space; shift it into camera space.
		poseStack.translate(-camera.x, -camera.y, -camera.z);

		context.submitNodeCollector().submitCustomGeometry(poseStack,
				EspRenderTypes.lines(esp.throughWalls), (pose, buffer) -> {
					for (AABB box : entityBoxes) {
						RenderHelper.box(pose, buffer, box, esp.entityColor, width);
					}

					for (AABB box : blockBoxes) {
						RenderHelper.box(pose, buffer, box, esp.blockColor, width);
					}
				});

		poseStack.popPose();
	}

	private static List<AABB> entityBoxes(Minecraft minecraft, UtilitiesScarceConfig.Targets targets) {
		List<Entity> entities = HighlightTargets.entities(minecraft, targets);
		List<AABB> boxes = new ArrayList<>(entities.size());

		for (Entity entity : entities) {
			// Nudge outwards so the box does not z-fight with the model.
			boxes.add(entity.getBoundingBox().inflate(0.02D));
		}

		return boxes;
	}

	private static List<AABB> blockBoxes(List<BlockPos> positions) {
		List<AABB> boxes = new ArrayList<>(positions.size());

		for (BlockPos pos : positions) {
			boxes.add(new AABB(pos).inflate(0.002D));
		}

		return boxes;
	}
}
