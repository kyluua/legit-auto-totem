package dev.kyluua.utilitiesscarce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

/**
 * Shared constants for the mod. Everything here is client-side only.
 */
public final class UtilitiesScarce {
	public static final String MOD_ID = "utilitiesscarce";
	public static final String MOD_NAME = "Utilities Scarce";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	private UtilitiesScarce() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
