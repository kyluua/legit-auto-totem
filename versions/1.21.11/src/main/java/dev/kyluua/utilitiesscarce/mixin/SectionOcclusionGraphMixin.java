package dev.kyluua.utilitiesscarce.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.util.FreeCamState;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.core.Direction;

/**
 * Stops chunks being culled by occlusion while something needs to see through
 * them.
 *
 * <p>The graph is built from where the player is standing, so a highlight drawn
 * through terrain, or a camera flown away from the body, would otherwise be
 * hidden along with the chunk it sits in. Culling is left alone whenever no
 * module needs it, because switching it off costs frames.
 */
@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {
	@Redirect(
			method = "runUpdates",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;"
							+ "facesCanSeeEachother(Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;)Z"))
	private boolean utilitiesscarce$seeThroughChunks(SectionMesh mesh, Direction from, Direction to) {
		if (utilitiesscarce$needsUnculledChunks()) {
			return true;
		}

		return mesh.facesCanSeeEachother(from, to);
	}

	private static boolean utilitiesscarce$needsUnculledChunks() {
		UtilitiesScarceConfig config = ConfigManager.get();

		return (config.esp.enabled && config.esp.throughWalls)
				|| (config.tracer.enabled && config.tracer.throughWalls)
				|| FreeCamState.isActive();
	}
}
