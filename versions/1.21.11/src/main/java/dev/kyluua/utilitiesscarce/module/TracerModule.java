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
import dev.kyluua.utilitiesscarce.util.FreeCamState;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a line from the viewer to every configured entity and block.
 *
 * <p>Shares the Targets section with ESP, so the two never disagree about what
 * is worth pointing at. Lines start just in front of the camera by default,
 * which both keeps them clear of the near clip plane and makes them read as
 * fanning out from the crosshair.
 */
public final class TracerModule extends Module {
	private final BlockScanner blockScanner;

	public TracerModule(ActionScheduler scheduler, BlockScanner blockScanner) {
		super("tracer", scheduler);
		this.blockScanner = blockScanner;
	}

	@Override
	public boolean isEnabled() {
		return ConfigManager.get().tracer.enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		ConfigManager.get().tracer.enabled = enabled;
	}

	@Override
	public void onRender(WorldRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.level == null) {
			return;
		}

		UtilitiesScarceConfig config = ConfigManager.get();
		UtilitiesScarceConfig.Tracer tracer = config.tracer;

		List<Vec3> entityTargets = tracer.showEntities
				? entityPoints(minecraft, config.targets,
						DetectionRange.entities(minecraft, config.targets))
				: List.of();
		List<Vec3> blockTargets = tracer.showBlocks
				? blockPoints(blockScanner.results())
				: List.of();

		if (entityTargets.isEmpty() && blockTargets.isEmpty()) {
			return;
		}

		Vec3 camera = context.worldState().cameraRenderState.pos;
		Vec3 start = lineOrigin(player, camera, tracer);
		PoseStack poseStack = context.matrices();
		float width = (float) Math.max(0.5D, tracer.lineWidth);

		poseStack.pushPose();
		poseStack.translate(-camera.x, -camera.y, -camera.z);

		VertexConsumer buffer = context.consumers().getBuffer(EspRenderTypes.lines(tracer.throughWalls));
		PoseStack.Pose pose = poseStack.last();

		for (int index = 0; index < entityTargets.size(); index++) {
			RenderHelper.line(pose, buffer, start, entityTargets.get(index),
					color(tracer, tracer.entityColor, index), width);
		}

		for (int index = 0; index < blockTargets.size(); index++) {
			RenderHelper.line(pose, buffer, start, blockTargets.get(index),
					color(tracer, tracer.blockColor, index), width);
		}

		poseStack.popPose();
	}

	private static Vec3 lineOrigin(LocalPlayer player, Vec3 camera,
			UtilitiesScarceConfig.Tracer tracer) {
		return switch (tracer.origin) {
			case EYES -> camera;
			case FEET -> new Vec3(player.getX(), player.getY(), player.getZ());
			case CROSSHAIR -> {
				// Follow the free camera's own aim when it has taken over the view.
				float yaw = FreeCamState.isActive() ? FreeCamState.yaw() : player.getYRot();
				float pitch = FreeCamState.isActive() ? FreeCamState.pitch() : player.getXRot();
				yield camera.add(RenderHelper.direction(yaw, pitch)
						.scale(Math.max(0.1D, tracer.originDistance)));
			}
		};
	}

	private static int color(UtilitiesScarceConfig.Tracer tracer, int staticColor, int index) {
		return Palette.resolve(tracer.colorMode, staticColor, tracer.rainbowSpeed,
				tracer.rainbowSpread, tracer.rainbowAlpha, index);
	}

	private static List<Vec3> entityPoints(Minecraft minecraft,
			UtilitiesScarceConfig.Targets targets, double range) {
		List<Entity> entities = HighlightTargets.entities(minecraft, targets, range);
		List<Vec3> points = new ArrayList<>(entities.size());

		for (Entity entity : entities) {
			points.add(entity.getBoundingBox().getCenter());
		}

		return points;
	}

	private static List<Vec3> blockPoints(List<BlockPos> positions) {
		List<Vec3> points = new ArrayList<>(positions.size());

		for (BlockPos pos : positions) {
			points.add(new AABB(pos).getCenter());
		}

		return points;
	}
}
