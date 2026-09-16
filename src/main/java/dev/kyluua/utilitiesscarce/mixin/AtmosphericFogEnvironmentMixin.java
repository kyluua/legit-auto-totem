package dev.kyluua.utilitiesscarce.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.kyluua.utilitiesscarce.util.FreeCamState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;

/**
 * Clears the overworld haze while Free Cam is flying.
 *
 * <p>Separate from the distance fog: this is the environmental layer, the one
 * that thickens in rain, and it is applied on top of whatever the render
 * distance produced.
 */
@Mixin(AtmosphericFogEnvironment.class)
public class AtmosphericFogEnvironmentMixin {
	private static final float FAR = 1.0E6F;

	@Inject(method = "setupFog", at = @At("RETURN"))
	private void utilitiesscarce$clearHaze(FogData data, Camera camera, ClientLevel level,
			float viewDistance, DeltaTracker deltaTracker, CallbackInfo callback) {
		if (!FreeCamState.unlimitedView()) {
			return;
		}

		data.environmentalStart = FAR;
		data.environmentalEnd = FAR;
	}
}
