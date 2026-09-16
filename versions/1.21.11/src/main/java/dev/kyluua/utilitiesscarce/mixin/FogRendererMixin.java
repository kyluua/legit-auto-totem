package dev.kyluua.utilitiesscarce.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.kyluua.utilitiesscarce.util.FreeCamState;
import net.minecraft.client.renderer.fog.FogRenderer;

/**
 * Pushes the distance fog past the render distance while Free Cam is flying.
 *
 * <p>Vanilla fades the world out well before the edge of the render distance,
 * which is the one thing left stopping you from seeing everything the chosen
 * distance actually holds. Rather than rewrite the fog values afterwards, the
 * render distance handed to the fog maths is inflated, so every start and end
 * derived from it lands far beyond anything drawn.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
	/** Chunks. Far enough that no fog band falls inside the real distance. */
	private static final int UNLIMITED_CHUNKS = 1024;

	@ModifyVariable(method = "setupFog", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int utilitiesscarce$extendFogDistance(int renderDistanceInChunks) {
		if (!FreeCamState.unlimitedView()) {
			return renderDistanceInChunks;
		}

		return Math.max(renderDistanceInChunks, UNLIMITED_CHUNKS);
	}
}
