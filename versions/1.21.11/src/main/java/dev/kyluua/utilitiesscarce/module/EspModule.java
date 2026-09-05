package dev.kyluua.utilitiesscarce.module;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.render.BlockScanner;
import dev.kyluua.utilitiesscarce.render.DetectionRange;
import dev.kyluua.utilitiesscarce.render.EspRenderTypes;
import dev.kyluua.utilitiesscarce.render.HighlightTargets;
import dev.kyluua.utilitiesscarce.render.Palette;
import dev.kyluua.utilitiesscarce.render.RenderHelper;
import dev.kyluua.utilitiesscarce.util.ActionScheduler;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
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
	public void onRender(WorldRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.level == null) {
			return;
		}

		UtilitiesScarceConfig config = ConfigManager.get();
		UtilitiesScarceConfig.Esp esp = config.esp;

		List<AABB> entityBoxes = esp.showEntities
				? entityBoxes(minecraft, config.targets,
						DetectionRange.entities(minecraft, config.targets))
				: List.of();
		List<AABB> blockBoxes = esp.showBlocks
				? blockBoxes(blockScanner.results())
				: List.of();

		if (entityBoxes.isEmpty() && blockBoxes.isEmpty()) {
			return;
		}

		// 26.2 reads this off the level render state; here it comes straight
		// from the camera, which Free Cam has already moved.
		Vec3 camera = context.gameRenderer().getMainCamera().position();
		PoseStack poseStack = context.matrices();
		float width = (float) Math.max(0.5D, esp.lineWidth);

		poseStack.pushPose();
		// Geometry below is in world space; shift it into camera space.
		poseStack.translate(-camera.x, -camera.y, -camera.z);

		VertexConsumer buffer = context.consumers().getBuffer(EspRenderTypes.lines(esp.throughWalls));
		PoseStack.Pose pose = poseStack.last();

		for (int index = 0; index < entityBoxes.size(); index++) {
			RenderHelper.box(pose, buffer, entityBoxes.get(index),
					color(esp, esp.entityColor, index), width);
		}

		for (int index = 0; index < blockBoxes.size(); index++) {
			RenderHelper.box(pose, buffer, blockBoxes.get(index),
					color(esp, esp.blockColor, index), width);
		}

		poseStack.popPose();
	}

	private static int color(UtilitiesScarceConfig.Esp esp, int staticColor, int index) {
		return Palette.resolve(esp.colorMode, staticColor, esp.rainbowSpeed, esp.rainbowSpread,
				esp.rainbowAlpha, index);
	}

	private static List<AABB> entityBoxes(Minecraft minecraft, UtilitiesScarceConfig.Targets targets,
			double range) {
		List<Entity> entities = HighlightTargets.entities(minecraft, targets, range);
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
