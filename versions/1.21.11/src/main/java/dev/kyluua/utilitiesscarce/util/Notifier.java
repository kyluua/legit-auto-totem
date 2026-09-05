package dev.kyluua.utilitiesscarce.util;

import dev.kyluua.utilitiesscarce.UtilitiesScarce;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Short client-side chat messages, prefixed with the mod name. */
public final class Notifier {
	private Notifier() {
	}

	public static void send(Component message) {
		Minecraft minecraft = Minecraft.getInstance();

		// Only in-world, and only once the HUD exists: 26.2 goes through the
		// player, which on this version has no client-side system message.
		if (minecraft.player == null || minecraft.gui == null) {
			return;
		}

		minecraft.gui.getChat().addMessage(Component.literal("[" + UtilitiesScarce.MOD_NAME + "] ")
				.withStyle(ChatFormatting.GRAY)
				.append(message));
	}

	public static void toggled(Component moduleName, boolean enabled) {
		Component state = Component.translatable(
				enabled ? "text.utilitiesscarce.toggled_on" : "text.utilitiesscarce.toggled_off",
				moduleName.copy().withStyle(ChatFormatting.WHITE));

		send(state.copy().withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
	}
}
