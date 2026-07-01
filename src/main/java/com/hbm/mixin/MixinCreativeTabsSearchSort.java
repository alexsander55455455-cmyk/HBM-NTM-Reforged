package com.hbm.mixin;

import com.hbm.creativetabs.CreativeTabSearchSortHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeTabs.class)
public class MixinCreativeTabsSearchSort {

	@Inject(method = "displayAllRelevantItems", at = @At("TAIL"))
	private void hbm$sortSearchTabStacks(NonNullList<ItemStack> list, CallbackInfo ci) {
		if ((Object) this != CreativeTabs.SEARCH) {
			return;
		}
		CreativeTabSearchSortHelper.sortSearchList(list);
	}
}