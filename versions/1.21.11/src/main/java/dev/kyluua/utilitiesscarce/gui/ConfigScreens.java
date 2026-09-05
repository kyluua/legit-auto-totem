package dev.kyluua.utilitiesscarce.gui;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.util.Notifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings entry point for the 1.21.11 build, which has no settings screen.
 *
 * <p>Cloth Config's 1.21.11 release declares its access widener in the
 * intermediary namespace. 1.21.11 ships non-obfuscated, so Loom refuses to
 * apply it and the build cannot depend on Cloth at all. Rather than hand-roll a
 * screen against widget APIs that differ again between these versions, this
 * build configures from the JSON file and the hotkeys, both of which work
 * exactly as they do on 26.2.
 */
public final class ConfigScreens {
	private ConfigScreens() {
	}

	public static boolean available() {
		return false;
	}

	/** Always {@code null} here; see the class note. */
	public static Screen create(Screen parent) {
		return null;
	}

	/** Points at the config file, since there is no screen to open. */
	public static void open(Minecraft minecraft) {
		Notifier.send(Component.literal("Settings for this build live in " + ConfigManager.path()
				+ " -- the hotkeys work as normal.").withStyle(ChatFormatting.YELLOW));
	}
}
