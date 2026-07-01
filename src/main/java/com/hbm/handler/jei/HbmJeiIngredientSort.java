package com.hbm.handler.jei;

import com.hbm.creativetabs.CreativeTabSortOrder;
import com.hbm.creativetabs.HBMCreativeTab;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.main.MainRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Aligns JEI browse-pane order for HBM item stacks with creative-tab sort indices
 * from {@code assets/hbm/creative_tab_order.txt}.
 */
public final class HbmJeiIngredientSort {

	/** Per-sort-index variant span; must exceed max EnumMulti / grenade_universal variant count. */
	private static final int VARIANT_MULTIPLIER = 10_000;

	/** Variant span reserved per registry bucket inside UNKNOWN_SORT_INDEX. */
	private static final int MAX_VARIANTS_PER_ITEM = 256;

	/** BedrockOreGrade.VALUES.length — type-major variant bucket per ore type. */
	private static final int BEDROCK_ORE_GRADE_COUNT = 26;

	/** Matches hbmspace MixinItemBedrockOreNew: grade << 8 | type. */
	private static final int BEDROCK_ORE_META_GRADE_SHIFT = 8;
	private static final int BEDROCK_ORE_META_TYPE_MASK = 0xFF;

	private static final String[] TAB_KEYS = {
			"partsTab",
			"controlTab",
			"templateTab",
			"resourceTab",
			"blockTab",
			"machineTab",
			"nukeTab",
			"missileTab",
			"weaponTab",
			"consumableTab",
	};

	private HbmJeiIngredientSort() {
	}

	public static boolean isHbmSortedNamespace(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		Item item = stack.getItem();
		if (item == null) {
			return false;
		}
		ResourceLocation key = item.getRegistryName();
		if (key == null) {
			return false;
		}
		String ns = key.getNamespace();
		return "hbm".equals(ns) || "hbmspace".equals(ns);
	}

	public static int compare(ItemStack a, ItemStack b) {
		int cmp = Long.compare(packedSortKey(a), packedSortKey(b));
		if (cmp != 0) {
			return cmp;
		}
		return compareTieBreak(a, b);
	}

	/** Creative-tab sort within one tab key; matches JEI variant ordering without tab ordinal. */
	public static int compareForTab(ItemStack a, ItemStack b, String tabKey) {
		int cmp = Long.compare(packedSortKeyForTab(a, tabKey), packedSortKeyForTab(b, tabKey));
		if (cmp != 0) {
			return cmp;
		}
		return compareTieBreak(a, b);
	}

	private static int compareTieBreak(ItemStack a, ItemStack b) {
		int cmp = Long.compare(variantSortOffset(a), variantSortOffset(b));
		return cmp != 0 ? cmp : 0;
	}

	/** Total-order key for JEI TimSort (tab ordinal + creative index + variant offset). */
	public static long packedSortKey(ItemStack stack) {
		String tab = resolveTabKey(stack);
		long key = (long) tabOrdinal(tab) * 2_000_000_000L;
		key += packedSortKeyForTab(stack, tab);
		return key;
	}

	public static long packedSortKeyForTab(ItemStack stack, String tabKey) {
		int sortIndex = CreativeTabSortOrder.getSortIndex(stack, tabKey);
		long variant = variantSortOffset(stack);
		if (sortIndex == CreativeTabSortOrder.UNKNOWN_SORT_INDEX) {
			ResourceLocation reg = stack.getItem() != null ? stack.getItem().getRegistryName() : null;
			long bucket = CreativeTabSortOrder.getRegistryBucket(reg);
			return (long) sortIndex * (long) VARIANT_MULTIPLIER + bucket * (long) MAX_VARIANTS_PER_ITEM + variant;
		}
		return (long) sortIndex * (long) VARIANT_MULTIPLIER + variant;
	}

	/** Variant offset aligned with creative-tab getSubItems emission order. */
	public static long variantSortOffset(ItemStack stack) {
		ResourceLocation reg = stack.getItem() != null ? stack.getItem().getRegistryName() : null;
		if (reg != null && "grenade_universal".equals(reg.getPath())) {
			return ItemGrenadeUniversal.getSubItemSortIndex(stack);
		}
		if (isBedrockOreItem(reg)) {
			int meta = stack.getMetadata() & 0xFFFF;
			int grade = meta >> BEDROCK_ORE_META_GRADE_SHIFT;
			int type = meta & BEDROCK_ORE_META_TYPE_MASK;
			return (long) type * BEDROCK_ORE_GRADE_COUNT + grade;
		}
		Item item = stack.getItem();
		if (item != null && item.getHasSubtypes()) {
			return stack.getMetadata() & 0xFFFFL;
		}
		if (item != null && item.isDamageable() && stack.getMetadata() == 0) {
			return stack.getItemDamage() & 0xFFFFL;
		}
		return stack.getMetadata() & 0xFFFFL;
	}

	/** @deprecated use {@link #variantSortOffset(ItemStack)} */
	public static long variantSortOffsetForTab(ItemStack stack) {
		return variantSortOffset(stack);
	}

	private static boolean isBedrockOreItem(ResourceLocation reg) {
		if (reg == null || !"hbm".equals(reg.getNamespace())) {
			return false;
		}
		String path = reg.getPath();
		return "bedrock_ore".equals(path)
				|| "bedrock_ore_new".equals(path)
				|| "bedrock_ore_base".equals(path)
				|| "bedrock_ore_fragment".equals(path);
	}

	private static int tabOrdinal(String tabKey) {
		for (int i = 0; i < TAB_KEYS.length; i++) {
			if (TAB_KEYS[i].equals(tabKey)) {
				return i;
			}
		}
		return TAB_KEYS.length;
	}

	private static String resolveTabKey(ItemStack stack) {
		Item item = stack.getItem();
		if (item == null) {
			return "partsTab";
		}
		CreativeTabs creativeTab = item.getCreativeTab();
		if (creativeTab instanceof HBMCreativeTab) {
			return ((HBMCreativeTab) creativeTab).getTabKey();
		}
		String fromTab = tabKeyFromCreativeTab(creativeTab);
		if (fromTab != null) {
			return fromTab;
		}
		ResourceLocation reg = item.getRegistryName();
		if (reg != null) {
			String explicitTab = resolveExplicitTabKey(reg);
			if (explicitTab != null) {
				return explicitTab;
			}
		}
		return "partsTab";
	}

	/** Prefer the tab that owns an explicit order-file entry (ignore GLOBAL_FALLBACK hits). */
	private static String resolveExplicitTabKey(ResourceLocation reg) {
		String bestTab = null;
		int bestIdx = Integer.MAX_VALUE;
		for (String tabKey : TAB_KEYS) {
			Integer idx = CreativeTabSortOrder.getExplicitSortIndex(reg, tabKey);
			if (idx == null) {
				continue;
			}
			if (idx < bestIdx) {
				bestIdx = idx;
				bestTab = tabKey;
			}
		}
		return bestTab;
	}

	private static String tabKeyFromCreativeTab(CreativeTabs tab) {
		if (tab == null) {
			return null;
		}
		if (tab == MainRegistry.partsTab) {
			return "partsTab";
		}
		if (tab == MainRegistry.controlTab) {
			return "controlTab";
		}
		if (tab == MainRegistry.templateTab) {
			return "templateTab";
		}
		if (tab == MainRegistry.resourceTab) {
			return "resourceTab";
		}
		if (tab == MainRegistry.blockTab) {
			return "blockTab";
		}
		if (tab == MainRegistry.machineTab) {
			return "machineTab";
		}
		if (tab == MainRegistry.nukeTab) {
			return "nukeTab";
		}
		if (tab == MainRegistry.missileTab) {
			return "missileTab";
		}
		if (tab == MainRegistry.weaponTab) {
			return "weaponTab";
		}
		if (tab == MainRegistry.consumableTab) {
			return "consumableTab";
		}
		return null;
	}
}