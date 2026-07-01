package com.hbm.handler.jei;

import com.hbm.creativetabs.CreativeTabSortOrder;
import com.hbm.main.MainRegistry;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.VanillaTypes;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * JEI browse-pane ordering for HBM items.
 * <p>
 * Seeds JEI {@code ORDER_TRACKER} at registration time so {@code getOrderIndex()} aligns with
 * {@code creative_tab_order.txt}. Final list order is applied by
 * {@link com.hbmspace.mixin.mod.hbm.jei.MixinIngredientListElement} unifying hbmspace mod sort name with hbm.
 * <p>
 * Do not reorder {@code IngredientFilter.elementList} at runtime: JEI suffix-tree search indices
 * are tied to list positions; mutating the list without rebuilding the trees makes search return wrong items.
 */
public final class HbmJeiIngredientOrder {

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

	private HbmJeiIngredientOrder() {
	}

	public static void seedRegistrationOrder(IModRegistry registry) {
		try {
			Class<?> factoryClass = Class.forName("mezz.jei.ingredients.IngredientListElementFactory");
			Field trackerField = factoryClass.getDeclaredField("ORDER_TRACKER");
			trackerField.setAccessible(true);
			Object tracker = trackerField.get(null);
			Method getOrderIndex = tracker.getClass().getMethod("getOrderIndex", Object.class, IIngredientHelper.class);

			@SuppressWarnings("unchecked")
			IIngredientHelper<ItemStack> helper = registry.getIngredientRegistry().getIngredientHelper(VanillaTypes.ITEM);

			int seeded = 0;
			for (String tabKey : TAB_KEYS) {
				for (String path : CreativeTabSortOrder.getTabRegistryOrder(tabKey)) {
					seeded += seedCreativeStacks(tracker, getOrderIndex, helper, stackFromPath(path));
				}
			}
			MainRegistry.logger.log(Level.INFO, "[HBM] JEI ingredient order seeded entries={}", seeded);
		} catch (Exception e) {
			MainRegistry.logger.log(Level.ERROR, "[HBM] JEI ingredient order seed failed", e);
		}
	}

	private static int seedCreativeStacks(
			Object tracker,
			Method getOrderIndex,
			IIngredientHelper<ItemStack> helper,
			ItemStack stack) throws ReflectiveOperationException {
		if (stack.isEmpty()) {
			return 0;
		}
		int seeded = 0;
		getOrderIndex.invoke(tracker, stack, helper);
		seeded++;
		Item item = stack.getItem();
		if (item == null) {
			return seeded;
		}
		CreativeTabs tab = item.getCreativeTab();
		if (tab == null) {
			return seeded;
		}
		NonNullList<ItemStack> subItems = NonNullList.create();
		item.getSubItems(tab, subItems);
		for (ItemStack sub : subItems) {
			if (sub.isEmpty()) {
				continue;
			}
			getOrderIndex.invoke(tracker, sub, helper);
			seeded++;
		}
		return seeded;
	}

	private static ItemStack stackFromPath(String path) {
		ResourceLocation key = CreativeTabSortOrder.resolveRegistryKey(path);
		if (key == null) {
			return ItemStack.EMPTY;
		}
		Item item = Item.REGISTRY.getObject(key);
		return item != null ? new ItemStack(item) : ItemStack.EMPTY;
	}

	public static void logSortOrderHealth() {
		CreativeTabSortOrder.logLoadHealth();
	}
}