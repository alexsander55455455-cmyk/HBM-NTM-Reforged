package com.hbm.creativetabs;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Items that must not appear in creative tabs (still craftable / obtainable). */
public final class CreativeTabExclusions {

	private static final Set<String> EXCLUDED_PATHS = new HashSet<>(Arrays.asList(
			"ammo_debug",
			"board_copper",
			"gun_debug",
			"magnet_circular",
			"pellet_rtg_depleted",
			"sliding_blast_door_keypad",
			"wand_air",
			"wand_loot",
			"wand_jigsaw",
			"wand_logic",
			"wand_tandem"
	));

	private CreativeTabExclusions() {
	}

	public static boolean isExcluded(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		Item item = stack.getItem();
		if (item == null) {
			return false;
		}
		ResourceLocation key = item.getRegistryName();
		return key != null && EXCLUDED_PATHS.contains(key.getPath());
	}
}