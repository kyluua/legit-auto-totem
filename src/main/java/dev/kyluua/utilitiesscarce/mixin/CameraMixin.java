package dev.kyluua.utilitiesscarce.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.kyluua.utilitiesscarce.util.FreeCamState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/** Puts the camera where Free Cam says, leaving the player entity alone. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setPosition(Vec3 position);

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	/**
	 * Runs right after the camera has been aligned to the player, so the
	 * override wins but everything else vanilla does still happens.
	 *
	 * <p>Marking the camera detached is what makes the game draw the player's
	 * body and skip the held-item overlay.
	 */
	@Inject(
			method = "update(Lnet/minecraft/client/DeltaTracker;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V",
					shift = At.Shift.AFTER))
	private void utilitiesscarce$applyFreeCam(DeltaTracker deltaTracker, CallbackInfo callback) {
		if (!FreeCamState.isActive()) {
			return;
		}

		detached = true;
		setPosition(FreeCamState.cameraPos(deltaTracker.getGameTimeDeltaPartialTick(true)));
		setRotation(FreeCamState.yaw(), FreeCamState.pitch());
	}

	/**
	 * Occlusion culling is built from the player's position. With the camera
	 * somewhere else that hides most of what you flew out to look at, so turn
	 * it off while Free Cam is running.
	 */
	@Inject(
			method = "extractRenderState(Lnet/minecraft/client/renderer/state/level/CameraRenderState;F)V",
			at = @At("RETURN"))
	private void utilitiesscarce$disableSmartCull(CameraRenderState cameraState, float partialTick,
			CallbackInfo callback) {
		if (FreeCamState.isActive()) {
			cameraState.smartCull = false;
		}
	}
}
