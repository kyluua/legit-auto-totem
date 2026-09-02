package dev.kyluua.utilitiesscarce.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import dev.kyluua.utilitiesscarce.UtilitiesScarce;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Loads and saves {@link UtilitiesScarceConfig} as pretty-printed JSON.
 *
 * <p>The config is read once at start-up and kept in memory; hotkey toggles and
 * the settings screen mutate that instance and call {@link #save()}.
 */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private static Path configPath;
	private static UtilitiesScarceConfig config = new UtilitiesScarceConfig();

	private ConfigManager() {
	}

	public static UtilitiesScarceConfig get() {
		return config;
	}

	public static Path path() {
		if (configPath == null) {
			configPath = FabricLoader.getInstance().getConfigDir()
					.resolve(UtilitiesScarce.MOD_ID + ".json");
		}

		return configPath;
	}

	/**
	 * Reads the config from disk, falling back to defaults if the file is
	 * missing or unreadable. Missing keys keep their default value, so the file
	 * survives mod updates that add settings.
	 */
	public static void load() {
		Path file = path();

		if (!Files.exists(file)) {
			config = new UtilitiesScarceConfig();
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			UtilitiesScarceConfig loaded = GSON.fromJson(reader, UtilitiesScarceConfig.class);
			config = loaded == null ? new UtilitiesScarceConfig() : loaded;
			fillGaps(config);
		} catch (IOException | JsonParseException e) {
			UtilitiesScarce.LOGGER.warn("Could not read {}, using defaults", file, e);
			config = new UtilitiesScarceConfig();
		}
	}

	public static void save() {
		Path file = path();

		try {
			Path parent = file.getParent();

			if (parent != null) {
				Files.createDirectories(parent);
			}

			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			UtilitiesScarce.LOGGER.error("Could not write {}", file, e);
		}
	}

	/**
	 * Gson leaves whole sections null when the JSON on disk predates them, so
	 * replace any missing section with its defaults.
	 */
	private static void fillGaps(UtilitiesScarceConfig target) {
		if (target.general == null) {
			target.general = new UtilitiesScarceConfig.General();
		}

		if (target.autoTotem == null) {
			target.autoTotem = new UtilitiesScarceConfig.AutoTotem();
		}

		if (target.stunSlam == null) {
			target.stunSlam = new UtilitiesScarceConfig.StunSlam();
		}

		if (target.shieldDisable == null) {
			target.shieldDisable = new UtilitiesScarceConfig.ShieldDisable();
		}

		if (target.breachSwap == null) {
			target.breachSwap = new UtilitiesScarceConfig.BreachSwap();
		}

		if (target.fastAnchor == null) {
			target.fastAnchor = new UtilitiesScarceConfig.FastAnchor();
		}

		if (target.freeCam == null) {
			target.freeCam = new UtilitiesScarceConfig.FreeCam();
		}

		if (target.targets == null) {
			target.targets = new UtilitiesScarceConfig.Targets();
		}

		if (target.esp == null) {
			target.esp = new UtilitiesScarceConfig.Esp();
		}

		if (target.tracer == null) {
			target.tracer = new UtilitiesScarceConfig.Tracer();
		}

		if (target.targets.blocks == null) {
			target.targets.blocks = new UtilitiesScarceConfig.Targets().blocks;
		}

		// Gson maps an unrecognised enum name to null rather than failing.
		if (target.autoTotem.swapMethod == null) {
			target.autoTotem.swapMethod = UtilitiesScarceConfig.SwapMethod.SWAP;
		}

		if (target.autoTotem.searchOrder == null) {
			target.autoTotem.searchOrder = UtilitiesScarceConfig.SearchOrder.INVENTORY_FIRST;
		}

		if (target.shieldDisable.triggerMode == null) {
			target.shieldDisable.triggerMode = UtilitiesScarceConfig.TriggerMode.ON_ATTACK;
		}

		if (target.tracer.origin == null) {
			target.tracer.origin = UtilitiesScarceConfig.TracerOrigin.CROSSHAIR;
		}

		if (target.fastAnchor.swapTarget == null) {
			target.fastAnchor.swapTarget = UtilitiesScarceConfig.AnchorSwapTarget.TOTEM;
		}
	}
}
