package dev.kyluua.utilitiesscarce.gui;

import dev.kyluua.utilitiesscarce.util.Notifier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Entry point for the settings screen.
 *
 * <p>Cloth Config is optional, so its classes are only reached through
 * {@link ClothConfigScreenFactory}; that class is not loaded at all until the
 * mod has been confirmed present, which keeps the mod working without it.
 */
public final class ConfigScreens {
	private static final String CLOTH_CONFIG = "cloth-config";

	private ConfigScreens() {
	}

	public static boolean available() {
		return FabricLoader.getInstance().isModLoaded(CLOTH_CONFIG);
	}

	/** The settings screen, or {@code null} when Cloth Config is missing. */
	public static Screen create(Screen parent) {
		if (!available()) {
			return null;
		}

		return ClothConfigScreenFactory.create(parent);
	}

	/** Opens the settings screen, explaining in chat if it cannot be built. */
	public static void open(Minecraft minecraft) {
		Screen screen = create(minecraft.screen);

		if (screen == null) {
			Notifier.send(Component.literal(
							"Install Cloth Config for the settings screen. Until then, edit config/utilitiesscarce.json.")
					.withStyle(ChatFormatting.YELLOW));
			return;
		}

		minecraft.setScreen(screen);
	}
}
