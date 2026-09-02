package dev.kyluua.utilitiesscarce;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.keybind.Keybinds;
import dev.kyluua.utilitiesscarce.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import dev.kyluua.utilitiesscarce.render.EspRenderTypes;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

/**
 * Client entry point. Wires the modules to the three events they need: the
 * client tick, attacking an entity and right-clicking a block.
 *
 * <p>Neither callback ever changes the outcome of the interaction -- both return
 * {@code PASS} so vanilla behaviour is untouched.
 */
public final class UtilitiesScarceClient implements ClientModInitializer {
	private static final ModuleManager MODULES = new ModuleManager();

	public static ModuleManager modules() {
		return MODULES;
	}

	@Override
	public void onInitializeClient() {
		ConfigManager.load();
		Keybinds.register(MODULES);
		// Register the line pipelines now rather than partway through a frame.
		EspRenderTypes.bootstrap();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Keybinds.handle(client, MODULES);
			MODULES.onClientTick(client);
		});

		// Late in the pass, so highlights sit on top of the world.
		LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(MODULES::onLevelRender);

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			Minecraft minecraft = Minecraft.getInstance();

			if (level.isClientSide() && player == minecraft.player) {
				MODULES.onAttackEntity(minecraft, entity);
			}

			return InteractionResult.PASS;
		});

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			Minecraft minecraft = Minecraft.getInstance();

			if (level.isClientSide() && player == minecraft.player) {
				MODULES.onUseBlock(minecraft, hand, hitResult);
			}

			return InteractionResult.PASS;
		});

		UtilitiesScarce.LOGGER.info("{} ready with {} modules", UtilitiesScarce.MOD_NAME,
				MODULES.modules().size());
	}
}
