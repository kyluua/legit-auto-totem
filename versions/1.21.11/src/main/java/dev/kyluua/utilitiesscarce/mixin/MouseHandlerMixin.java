package dev.kyluua.utilitiesscarce.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.kyluua.utilitiesscarce.util.FreeCamState;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sends mouse look to the free camera instead of the player.
 *
 * <p>Without this the body would turn with the mouse, and it would stop mining
 * the block it was aimed at the moment Free Cam came on.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Redirect(
			method = "turnPlayer(D)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void utilitiesscarce$turnFreeCam(LocalPlayer player, double deltaYaw, double deltaPitch) {
		if (FreeCamState.isActive()) {
			FreeCamState.turn(deltaYaw, deltaPitch);
			return;
		}

		player.turn(deltaYaw, deltaPitch);
	}
}
