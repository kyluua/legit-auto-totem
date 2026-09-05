package dev.kyluua.utilitiesscarce.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Item tests used by the modules. Tools are matched by tag rather than by item
 * class so that modded swords and axes count too.
 */
public final class ItemHelper {
	private ItemHelper() {
	}

	public static boolean isTotem(ItemStack stack) {
		return stack.is(Items.TOTEM_OF_UNDYING);
	}

	public static boolean isMace(ItemStack stack) {
		return stack.is(Items.MACE);
	}

	public static boolean isAxe(ItemStack stack) {
		return stack.is(ItemTags.AXES);
	}

	public static boolean isSword(ItemStack stack) {
		return stack.is(ItemTags.SWORDS);
	}

	public static boolean isShield(ItemStack stack) {
		return stack.is(Items.SHIELD);
	}

	public static boolean isGlowstone(ItemStack stack) {
		return stack.is(Items.GLOWSTONE);
	}

	public static boolean isRespawnAnchor(ItemStack stack) {
		return stack.is(Items.RESPAWN_ANCHOR);
	}

	/**
	 * Level of an enchantment on a stack, or {@code 0} when it is absent. Needs
	 * a world because enchantments live in a dynamic registry.
	 */
	public static int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> key) {
		Minecraft minecraft = Minecraft.getInstance();

		if (stack.isEmpty() || minecraft.level == null) {
			return 0;
		}

		return minecraft.level.registryAccess().lookup(Registries.ENCHANTMENT)
				.flatMap(registry -> registry.get(key))
				.map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
				.orElse(0);
	}

	public static int breachLevel(ItemStack stack) {
		return enchantmentLevel(stack, Enchantments.BREACH);
	}

	/**
	 * A mace, optionally required to carry Breach at {@code minLevel} or above.
	 */
	public static boolean isBreachMace(ItemStack stack, boolean requireBreach, int minLevel) {
		if (!isMace(stack)) {
			return false;
		}

		if (!requireBreach) {
			return true;
		}

		return breachLevel(stack) >= Math.max(1, minLevel);
	}
}
