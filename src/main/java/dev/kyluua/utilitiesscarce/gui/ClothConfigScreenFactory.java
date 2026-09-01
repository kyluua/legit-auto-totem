package dev.kyluua.utilitiesscarce.gui;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.AnchorSwapTarget;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.SearchOrder;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.SwapMethod;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.TriggerMode;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Builds the settings screen with Cloth Config.
 *
 * <p>This class touches Cloth Config types directly, so it must only be loaded
 * once {@link ConfigScreens} has confirmed the mod is present.
 */
public final class ClothConfigScreenFactory {
	private ClothConfigScreenFactory() {
	}

	public static Screen create(Screen parent) {
		UtilitiesScarceConfig config = ConfigManager.get();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("text.utilitiesscarce.title"))
				.setSavingRunnable(ConfigManager::save);

		ConfigEntryBuilder entries = builder.entryBuilder();

		addGeneral(builder, entries, config);
		addAutoTotem(builder, entries, config);
		addStunSlam(builder, entries, config);
		addShieldDisable(builder, entries, config);
		addBreachSwap(builder, entries, config);
		addFastAnchor(builder, entries, config);
		addFreeCam(builder, entries, config);

		return builder.build();
	}

	private static Component option(String key) {
		return Component.translatable("text.utilitiesscarce.option." + key);
	}

	private static ConfigCategory category(ConfigBuilder builder, String key) {
		return builder.getOrCreateCategory(Component.translatable(key));
	}

	private static void addGeneral(ConfigBuilder builder, ConfigEntryBuilder entries,
			UtilitiesScarceConfig config) {
		ConfigCategory category = category(builder, "text.utilitiesscarce.category.general");
		UtilitiesScarceConfig.General general = config.general;

		category.addEntry(entries.startBooleanToggle(option("notify_on_toggle"), general.notifyOnToggle)
				.setDefaultValue(true)
				.setSaveConsumer(value -> general.notifyOnToggle = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("notify_on_action"), general.notifyOnAction)
				.setDefaultValue(false)
				.setSaveConsumer(value -> general.notifyOnAction = value)
				.build());

		category.addEntry(entries.startIntSlider(option("max_actions_per_tick"),
						general.maxActionsPerTick, 1, 8)
				.setDefaultValue(2)
				.setTooltip(Component.literal(
						"Upper bound on packet-producing steps per tick, shared by all modules."))
				.setSaveConsumer(value -> general.maxActionsPerTick = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("pause_when_screen_open"),
						general.pauseWhenScreenOpen)
				.setDefaultValue(true)
				.setSaveConsumer(value -> general.pauseWhenScreenOpen = value)
				.build());
	}

	private static void addAutoTotem(ConfigBuilder builder, ConfigEntryBuilder entries,
			UtilitiesScarceConfig config) {
		ConfigCategory category = category(builder, "text.utilitiesscarce.module.auto_totem");
		UtilitiesScarceConfig.AutoTotem module = config.autoTotem;

		category.addEntry(entries.startBooleanToggle(option("enabled"), module.enabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.enabled = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("refill_offhand"), module.refillOffhand)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.refillOffhand = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("refill_hotbar"), module.refillHotbar)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.refillHotbar = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("require_recent_damage"),
						module.requireRecentDamage)
				.setDefaultValue(false)
				.setTooltip(Component.literal(
						"Only refill when you were hit recently, so moving totems by hand is ignored."))
				.setSaveConsumer(value -> module.requireRecentDamage = value)
				.build());

		category.addEntry(entries.startIntSlider(option("delay_ticks"), module.delayTicks, 0, 20)
				.setDefaultValue(0)
				.setSaveConsumer(value -> module.delayTicks = value)
				.build());

		category.addEntry(entries.startIntSlider(option("cooldown_ticks"), module.cooldownTicks, 0, 40)
				.setDefaultValue(2)
				.setSaveConsumer(value -> module.cooldownTicks = value)
				.build());

		category.addEntry(entries.startIntSlider(option("keep_in_reserve"), module.keepInReserve, 0, 16)
				.setDefaultValue(0)
				.setSaveConsumer(value -> module.keepInReserve = value)
				.build());

		category.addEntry(entries.startEnumSelector(option("swap_method"), SwapMethod.class,
						module.swapMethod)
				.setDefaultValue(SwapMethod.SWAP)
				.setTooltip(Component.literal("SWAP is one packet. PICKUP mimics real mouse clicks."))
				.setSaveConsumer(value -> module.swapMethod = value)
				.build());

		category.addEntry(entries.startEnumSelector(option("search_order"), SearchOrder.class,
						module.searchOrder)
				.setDefaultValue(SearchOrder.INVENTORY_FIRST)
				.setSaveConsumer(value -> module.searchOrder = value)
				.build());
	}

	private static void addStunSlam(ConfigBuilder builder, ConfigEntryBuilder entries,
			UtilitiesScarceConfig config) {
		ConfigCategory category = category(builder, "text.utilitiesscarce.module.stun_slam");
		UtilitiesScarceConfig.StunSlam module = config.stunSlam;

		category.addEntry(entries.startBooleanToggle(option("enabled"), module.enabled)
				.setDefaultValue(false)
				.setSaveConsumer(value -> module.enabled = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("require_blocking"), module.requireBlocking)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.requireBlocking = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("players_only"), module.playersOnly)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.playersOnly = value)
				.build());

		category.addEntry(entries.startDoubleField(option("max_range"), module.maxRange)
				.setDefaultValue(4.0D)
				.setSaveConsumer(value -> module.maxRange = value)
				.build());

		category.addEntry(entries.startIntSlider(option("axe_delay_ticks"), module.axeDelayTicks, 0, 20)
				.setDefaultValue(0)
				.setSaveConsumer(value -> module.axeDelayTicks = value)
				.build());

		category.addEntry(entries.startIntSlider(option("mace_delay_ticks"), module.maceDelayTicks, 0, 20)
				.setDefaultValue(0)
				.setSaveConsumer(value -> module.maceDelayTicks = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("wait_for_cooldown"), module.waitForCooldown)
				.setDefaultValue(true)
				.setTooltip(Component.literal(
						"A mace swung before the cooldown recharges does a fraction of its damage."))
				.setSaveConsumer(value -> module.waitForCooldown = value)
				.build());

		category.addEntry(entries.startIntSlider(option("cooldown_timeout_ticks"),
						module.cooldownTimeoutTicks, 1, 100)
				.setDefaultValue(40)
				.setSaveConsumer(value -> module.cooldownTimeoutTicks = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("restore_slot"), module.restoreSlot)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.restoreSlot = value)
				.build());

		category.addEntry(entries.startIntSlider(option("restore_delay_ticks"),
						module.restoreDelayTicks, 0, 40)
				.setDefaultValue(2)
				.setSaveConsumer(value -> module.restoreDelayTicks = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("move_to_hotbar"), module.moveToHotbar)
				.setDefaultValue(false)
				.setSaveConsumer(value -> module.moveToHotbar = value)
				.build());
	}

	private static void addShieldDisable(ConfigBuilder builder, ConfigEntryBuilder entries,
			UtilitiesScarceConfig config) {
		ConfigCategory category = category(builder, "text.utilitiesscarce.module.shield_disable");
		UtilitiesScarceConfig.ShieldDisable module = config.shieldDisable;

		category.addEntry(entries.startBooleanToggle(option("enabled"), module.enabled)
				.setDefaultValue(false)
				.setSaveConsumer(value -> module.enabled = value)
				.build());

		category.addEntry(entries.startEnumSelector(option("trigger_mode"), TriggerMode.class,
						module.triggerMode)
				.setDefaultValue(TriggerMode.ON_ATTACK)
				.setTooltip(Component.literal(
						"ON_ATTACK reacts to your own hit. AUTO fires at whatever is under the crosshair."))
				.setSaveConsumer(value -> module.triggerMode = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("require_blocking"), module.requireBlocking)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.requireBlocking = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("players_only"), module.playersOnly)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.playersOnly = value)
				.build());

		category.addEntry(entries.startDoubleField(option("max_range"), module.maxRange)
				.setDefaultValue(4.0D)
				.setSaveConsumer(value -> module.maxRange = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("wait_for_cooldown"), module.waitForCooldown)
				.setDefaultValue(false)
				.setSaveConsumer(value -> module.waitForCooldown = value)
				.build());

		category.addEntry(entries.startIntSlider(option("cooldown_timeout_ticks"),
						module.cooldownTimeoutTicks, 1, 100)
				.setDefaultValue(40)
				.setSaveConsumer(value -> module.cooldownTimeoutTicks = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("restore_slot"), module.restoreSlot)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.restoreSlot = value)
				.build());

		category.addEntry(entries.startIntSlider(option("restore_delay_ticks"),
						module.restoreDelayTicks, 0, 40)
				.setDefaultValue(2)
				.setSaveConsumer(value -> module.restoreDelayTicks = value)
				.build());

		category.addEntry(entries.startIntSlider(option("cooldown_ticks"), module.cooldownTicks, 0, 60)
				.setDefaultValue(10)
				.setSaveConsumer(value -> module.cooldownTicks = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("move_to_hotbar"), module.moveToHotbar)
				.setDefaultValue(false)
				.setSaveConsumer(value -> module.moveToHotbar = value)
				.build());
	}

	private static void addBreachSwap(ConfigBuilder builder, ConfigEntryBuilder entries,
			UtilitiesScarceConfig config) {
		ConfigCategory category = category(builder, "text.utilitiesscarce.module.breach_swap");
		UtilitiesScarceConfig.BreachSwap module = config.breachSwap;

		category.addEntry(entries.startBooleanToggle(option("enabled"), module.enabled)
				.setDefaultValue(false)
				.setSaveConsumer(value -> module.enabled = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("trigger_with_sword"), module.triggerWithSword)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.triggerWithSword = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("trigger_with_axe"), module.triggerWithAxe)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.triggerWithAxe = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("require_breach"), module.requireBreach)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.requireBreach = value)
				.build());

		category.addEntry(entries.startIntSlider(option("min_breach_level"), module.minBreachLevel, 1, 4)
				.setDefaultValue(1)
				.setSaveConsumer(value -> module.minBreachLevel = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("players_only"), module.playersOnly)
				.setDefaultValue(false)
				.setSaveConsumer(value -> module.playersOnly = value)
				.build());

		category.addEntry(entries.startDoubleField(option("max_range"), module.maxRange)
				.setDefaultValue(4.0D)
				.setSaveConsumer(value -> module.maxRange = value)
				.build());

		category.addEntry(entries.startIntSlider(option("mace_delay_ticks"), module.maceDelayTicks, 0, 20)
				.setDefaultValue(0)
				.setSaveConsumer(value -> module.maceDelayTicks = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("wait_for_cooldown"), module.waitForCooldown)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.waitForCooldown = value)
				.build());

		category.addEntry(entries.startIntSlider(option("cooldown_timeout_ticks"),
						module.cooldownTimeoutTicks, 1, 100)
				.setDefaultValue(40)
				.setSaveConsumer(value -> module.cooldownTimeoutTicks = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("restore_slot"), module.restoreSlot)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.restoreSlot = value)
				.build());

		category.addEntry(entries.startIntSlider(option("restore_delay_ticks"),
						module.restoreDelayTicks, 0, 40)
				.setDefaultValue(2)
				.setSaveConsumer(value -> module.restoreDelayTicks = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("move_to_hotbar"), module.moveToHotbar)
				.setDefaultValue(false)
				.setSaveConsumer(value -> module.moveToHotbar = value)
				.build());
	}

	private static void addFreeCam(ConfigBuilder builder, ConfigEntryBuilder entries,
			UtilitiesScarceConfig config) {
		ConfigCategory category = category(builder, "text.utilitiesscarce.module.free_cam");
		UtilitiesScarceConfig.FreeCam module = config.freeCam;

		category.addEntry(entries.startBooleanToggle(option("enabled"), module.enabled)
				.setDefaultValue(false)
				.setTooltip(Component.literal(
						"Your body stays put and keeps aiming where it was, so it carries on mining."))
				.setSaveConsumer(value -> module.enabled = value)
				.build());

		category.addEntry(entries.startDoubleField(option("move_speed"), module.moveSpeed)
				.setDefaultValue(0.8D)
				.setSaveConsumer(value -> module.moveSpeed = value)
				.build());

		category.addEntry(entries.startDoubleField(option("sprint_multiplier"), module.sprintMultiplier)
				.setDefaultValue(3.0D)
				.setSaveConsumer(value -> module.sprintMultiplier = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("follow_pitch"), module.followPitch)
				.setDefaultValue(false)
				.setTooltip(Component.literal(
						"Off keeps movement level so you can look down while flying sideways."))
				.setSaveConsumer(value -> module.followPitch = value)
				.build());

		category.addEntry(entries.startDoubleField(option("max_distance"), module.maxDistance)
				.setDefaultValue(0.0D)
				.setTooltip(Component.literal("0 is unlimited. Beyond loaded chunks there is nothing to see."))
				.setSaveConsumer(value -> module.maxDistance = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("disable_on_damage"), module.disableOnDamage)
				.setDefaultValue(true)
				.setSaveConsumer(value -> module.disableOnDamage = value)
				.build());
	}

	private static void addFastAnchor(ConfigBuilder builder, ConfigEntryBuilder entries,
			UtilitiesScarceConfig config) {
		ConfigCategory category = category(builder, "text.utilitiesscarce.module.fast_anchor");
		UtilitiesScarceConfig.FastAnchor module = config.fastAnchor;

		category.addEntry(entries.startBooleanToggle(option("enabled"), module.enabled)
				.setDefaultValue(false)
				.setSaveConsumer(value -> module.enabled = value)
				.build());

		category.addEntry(entries.startIntSlider(option("charges"), module.charges, 1, 4)
				.setDefaultValue(1)
				.setTooltip(Component.literal("One charge is enough to make the anchor explode."))
				.setSaveConsumer(value -> module.charges = value)
				.build());

		category.addEntry(entries.startIntSlider(option("charge_delay_ticks"),
						module.chargeDelayTicks, 0, 20)
				.setDefaultValue(1)
				.setSaveConsumer(value -> module.chargeDelayTicks = value)
				.build());

		category.addEntry(entries.startIntSlider(option("swap_delay_ticks"), module.swapDelayTicks, 0, 20)
				.setDefaultValue(0)
				.setSaveConsumer(value -> module.swapDelayTicks = value)
				.build());

		category.addEntry(entries.startEnumSelector(option("swap_target"), AnchorSwapTarget.class,
						module.swapTarget)
				.setDefaultValue(AnchorSwapTarget.TOTEM)
				.setSaveConsumer(value -> module.swapTarget = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("only_where_explosive"),
						module.onlyWhereExplosive)
				.setDefaultValue(true)
				.setTooltip(Component.literal(
						"Skips the Nether, where an anchor sets your spawn instead of exploding."))
				.setSaveConsumer(value -> module.onlyWhereExplosive = value)
				.build());

		category.addEntry(entries.startBooleanToggle(option("move_to_hotbar"), module.moveToHotbar)
				.setDefaultValue(false)
				.setSaveConsumer(value -> module.moveToHotbar = value)
				.build());
	}
}
