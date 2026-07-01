package com.hbm.creativetabs;

import com.hbm.handler.jei.HbmJeiIngredientSort;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CreativeTabSortHelper {

	private CreativeTabSortHelper() {
	}

	public static int compareStacks(ItemStack a, ItemStack b, String tabKey) {
		if (HbmJeiIngredientSort.isHbmSortedNamespace(a) && HbmJeiIngredientSort.isHbmSortedNamespace(b)) {
			return HbmJeiIngredientSort.compareForTab(a, b, tabKey);
		}
		return compareSortKeys(
				CreativeTabSortOrder.getSortIndex(a, tabKey),
				variantSortKey(a),
				registrySortKey(a),
				CreativeTabSortOrder.getSortIndex(b, tabKey),
				variantSortKey(b),
				registrySortKey(b));
	}

	public static int compareSortKeys(
			int idxA,
			long variantA,
			String registryA,
			int idxB,
			long variantB,
			String registryB) {
		int cmp = Integer.compare(idxA, idxB);
		if (cmp != 0) {
			return cmp;
		}
		cmp = Long.compare(variantA, variantB);
		if (cmp != 0) {
			return cmp;
		}
		cmp = registryA.compareTo(registryB);
		if (cmp != 0) {
			return cmp;
		}
		return 0;
	}

	public static void sortRegistryKeys(List<String> keys, String tabKey) {
		keys.sort((a, b) -> {
			ResourceLocation regA = parseRegistryKey(a);
			ResourceLocation regB = parseRegistryKey(b);
			return compareSortKeys(
					CreativeTabSortOrder.getSortIndex(regA, tabKey),
					0L,
					registryKeyString(regA),
					CreativeTabSortOrder.getSortIndex(regB, tabKey),
					0L,
					registryKeyString(regB));
		});
	}

	private static ResourceLocation parseRegistryKey(String key) {
		if (key.contains(":")) {
			String[] parts = key.split(":", 2);
			return new ResourceLocation(parts[0], parts[1]);
		}
		return new ResourceLocation("hbm", key);
	}

	private static String registryKeyString(ResourceLocation reg) {
		if (reg == null) {
			return "";
		}
		return "hbm".equals(reg.getNamespace()) ? reg.getPath() : reg.toString();
	}

	public static void sortStacks(NonNullList<ItemStack> list, String tabKey) {
		List<ItemStack> sorted = new ArrayList<ItemStack>(list);
		Collections.sort(sorted, (a, b) -> compareStacks(a, b, tabKey));
		list.clear();
		list.addAll(sorted);
	}

	private static long variantSortKey(ItemStack stack) {
		if (HbmJeiIngredientSort.isHbmSortedNamespace(stack)) {
			return HbmJeiIngredientSort.variantSortOffset(stack);
		}
		return stack.getMetadata() & 0xFFFFL;
	}

	static String registrySortKey(ItemStack stack) {
		Item item = stack.getItem();
		if (item == null) {
			return "";
		}
		ResourceLocation key = item.getRegistryName();
		return key != null ? key.toString() : "";
	}
}