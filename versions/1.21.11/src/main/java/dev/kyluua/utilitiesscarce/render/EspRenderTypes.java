package dev.kyluua.utilitiesscarce.render;

import java.util.Optional;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Line render types for the highlight modules.
 *
 * <p>Both are built from vanilla's own line snippet, so they reuse the stock
 * line shaders and no shader assets ship with the mod. The only difference
 * between them is the depth state: dropping it entirely is what draws a box
 * through terrain.
 */
public final class EspRenderTypes {
	private static final RenderPipeline DEPTH_TESTED_LINES = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
					.withLocation(Identifier.parse("utilitiesscarce:pipeline/lines"))
					.withDepthStencilState(DepthStencilState.DEFAULT)
					.build());

	private static final RenderPipeline THROUGH_WALL_LINES = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
					.withLocation(Identifier.parse("utilitiesscarce:pipeline/lines_through_walls"))
					.withDepthStencilState(Optional.empty())
					.build());

	private static final RenderType LINES =
			lineType("utilitiesscarce:lines", DEPTH_TESTED_LINES);
	private static final RenderType LINES_THROUGH_WALLS =
			lineType("utilitiesscarce:lines_through_walls", THROUGH_WALL_LINES);

	private EspRenderTypes() {
	}

	/**
	 * Forces the pipelines to register during client init rather than partway
	 * through the first frame that needs them.
	 */
	public static void bootstrap() {
		// Touching the class is the whole job; the static initialisers do the work.
	}

	public static RenderType lines(boolean throughWalls) {
		return throughWalls ? LINES_THROUGH_WALLS : LINES;
	}

	private static RenderType lineType(String name, RenderPipeline pipeline) {
		return RenderType.create(name, RenderSetup.builder(pipeline)
				.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
				.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
				.createRenderSetup());
	}
}
