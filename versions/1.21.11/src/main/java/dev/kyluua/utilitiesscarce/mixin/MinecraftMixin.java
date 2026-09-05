package dev.kyluua.utilitiesscarce.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.kyluua.utilitiesscarce.UtilitiesScarceClient;
import net.minecraft.client.Minecraft;

/**
 * Drives the per-tick module work.
 *
 * <p>This is what Fabric's {@code ClientTickEvents} would do, done directly.
 * That API lives in fabric-lifecycle-events-v1, which declares an access
 * widener in the intermediary namespace; 1.21.11 ships non-obfuscated, so Loom
 * refuses to apply it and the module cannot be depended on here at all. One
 * injection is a cheaper price than losing the build.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "tick", at = @At("RETURN"))
	private void utilitiesscarce$endClientTick(CallbackInfo callback) {
		UtilitiesScarceClient.onEndClientTick((Minecraft) (Object) this);
	}
}
