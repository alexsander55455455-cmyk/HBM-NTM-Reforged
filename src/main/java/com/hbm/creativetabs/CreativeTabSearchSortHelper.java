package com.hbm.creativetabs;

import com.hbm.handler.jei.HbmJeiIngredientSort;
import com.hbm.main.MainRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Global HBM ordering for creative search (compass), matching JEI packed keys. */
public final class CreativeTabSearchSortHelper {

	private static final Set<String> SEARCH_EXCLUDED_PATHS = new HashSet<>(Arrays.asList(
			"ammo_debug",
			"gun_debug",
			"pellet_rtg_depleted"
	));

	private static boolean loggedApply;

	private CreativeTabSearchSortHelper() {
	}

	public static void sortSearchList(NonNullList<ItemStack> list) {
		if (list == null || list.isEmpty()) {
			return;
		}

		removeExcludedStacks(list);

		int size = list.size();
		List<Integer> hbmSlots = new ArrayList<>();
		List<ItemStack> hbmStacks = new ArrayList<>();

		for (int i = 0; i < size; i++) {
			ItemStack stack = list.get(i);
			if (HbmJeiIngredientSort.isHbmSortedNamespace(stack)) {
				hbmSlots.add(i);
				hbmStacks.add(stack);
			}
		}

		if (hbmStacks.size() < 2) {
			return;
		}

		hbmStacks.sort(HbmJeiIngredientSort::compare);
		for (int j = 0; j < hbmStacks.size(); j++) {
			list.set(hbmSlots.get(j), hbmStacks.get(j));
		}

		if (!loggedApply && MainRegistry.logger != null) {
			loggedApply = true;
			MainRegistry.logger.log(Level.INFO, "[HBM] search HBM sort applied slots={}", hbmStacks.size());
		}
	}

	private static void removeExcludedStacks(NonNullList<ItemStack> list) {
		for (int i = list.size() - 1; i >= 0; i--) {
			if (isSearchExcluded(list.get(i))) {
				list.remove(i);
			}
		}
	}

	static boolean isSearchExcluded(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		Item item = stack.getItem();
		if (item == null) {
			return false;
		}
		ResourceLocation key = item.getRegistryName();
		return key != null && SEARCH_EXCLUDED_PATHS.contains(key.getPath());
	}
}