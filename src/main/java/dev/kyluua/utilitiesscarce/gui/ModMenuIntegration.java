package dev.kyluua.utilitiesscarce.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Puts the settings screen behind the Config button in Mod Menu's mod list. */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (!ConfigScreens.available()) {
			// No Cloth Config, so no screen to show; leave the button out.
			return ModMenuApi.super.getModConfigScreenFactory();
		}

		return ConfigScreens::create;
	}
}
