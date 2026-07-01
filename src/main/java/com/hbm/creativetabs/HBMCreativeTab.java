package com.hbm.creativetabs;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class HBMCreativeTab extends CreativeTabs {

	private final String tabKey;

	protected HBMCreativeTab(int index, String label, String tabKey) {
		super(index, label);
		this.tabKey = tabKey;
	}

	public String getTabKey() {
		return tabKey;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void displayAllRelevantItems(NonNullList<ItemStack> list) {
		NonNullList<ItemStack> tabItems = NonNullList.create();
		collectItems(tabItems);
		sortStacks(tabItems);
		appendTabExtras(tabItems);
		list.addAll(tabItems);
	}

	@SideOnly(Side.CLIENT)
	protected void collectItems(NonNullList<ItemStack> list) {
		NonNullList<ItemStack> raw = NonNullList.create();
		super.displayAllRelevantItems(raw);
		for (ItemStack stack : raw) {
			if (!CreativeTabExclusions.isExcluded(stack)) {
				list.add(stack);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	protected void appendTabExtras(NonNullList<ItemStack> list) {
	}

	@SideOnly(Side.CLIENT)
	protected void sortStacks(NonNullList<ItemStack> list) {
		CreativeTabSortHelper.sortStacks(list, tabKey);
	}
}