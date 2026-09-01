package dev.kyluua.utilitiesscarce.keybind;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import dev.kyluua.utilitiesscarce.UtilitiesScarce;
import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.gui.ConfigScreens;
import dev.kyluua.utilitiesscarce.module.Module;
import dev.kyluua.utilitiesscarce.module.ModuleManager;
import dev.kyluua.utilitiesscarce.util.Notifier;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * One toggle key per module plus a key that opens the settings screen.
 *
 * <p>Defaults sit on the numeric keypad, which vanilla leaves unbound, so
 * nothing is stomped on a fresh install. Rebind them in Options -> Controls.
 */
public final class Keybinds {
	private static final int[] DEFAULT_TOGGLE_KEYS = {
			GLFW.GLFW_KEY_KP_1,
			GLFW.GLFW_KEY_KP_2,
			GLFW.GLFW_KEY_KP_3,
			GLFW.GLFW_KEY_KP_4,
			GLFW.GLFW_KEY_KP_5,
			GLFW.GLFW_KEY_KP_6
	};

	private static final Map<String, KeyMapping> TOGGLES = new LinkedHashMap<>();
	private static KeyMapping openConfig;

	private Keybinds() {
	}

	/**
	 * Registers every key mapping. Must run during client init: the game
	 * refuses new mappings once the options have been built.
	 */
	public static void register(ModuleManager manager) {
		KeyMapping.Category category = KeyMapping.Category.register(UtilitiesScarce.id("main"));

		openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.utilitiesscarce.open_config",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_KP_0,
				category));

		int index = 0;

		for (Module module : manager.modules()) {
			int defaultKey = index < DEFAULT_TOGGLE_KEYS.length
					? DEFAULT_TOGGLE_KEYS[index]
					: InputConstants.UNKNOWN.getValue();

			TOGGLES.put(module.id(), KeyMappingHelper.registerKeyMapping(new KeyMapping(
					"key.utilitiesscarce.toggle_" + module.id(),
					InputConstants.Type.KEYSYM,
					defaultKey,
					category)));

			index++;
		}
	}

	/** Drains queued key presses. Called once per client tick. */
	public static void handle(Minecraft minecraft, ModuleManager manager) {
		if (openConfig != null) {
			while (openConfig.consumeClick()) {
				ConfigScreens.open(minecraft);
			}
		}

		for (Module module : manager.modules()) {
			KeyMapping mapping = TOGGLES.get(module.id());

			if (mapping == null) {
				continue;
			}

			while (mapping.consumeClick()) {
				toggle(module);
			}
		}
	}

	private static void toggle(Module module) {
		boolean enabled = !module.isEnabled();
		module.setEnabled(enabled);

		if (!enabled) {
			module.onStop();
		}

		ConfigManager.save();

		if (ConfigManager.get().general.notifyOnToggle) {
			Notifier.toggled(module.displayName(), enabled);
		}
	}
}
