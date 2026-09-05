package dev.kyluua.utilitiesscarce.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.kyluua.utilitiesscarce.util.FreeCamState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Puts the camera where Free Cam says, leaving the player entity alone.
 *
 * <p>Marking the camera detached is what makes the game draw the player's body
 * and skip the held-item overlay.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setPosition(Vec3 position);

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(
			method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V",
			at = @At("RETURN"))
	private void utilitiesscarce$applyFreeCam(Level level, Entity entity, boolean detachedCamera,
			boolean thirdPersonReverse, float partialTick, CallbackInfo callback) {
		if (!FreeCamState.isActive()) {
			return;
		}

		detached = true;
		setPosition(FreeCamState.cameraPos(partialTick));
		setRotation(FreeCamState.yaw(), FreeCamState.pitch());
	}
}
