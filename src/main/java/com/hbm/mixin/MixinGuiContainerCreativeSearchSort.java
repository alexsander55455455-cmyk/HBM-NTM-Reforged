package com.hbm.mixin;

import com.hbm.creativetabs.CreativeTabSearchSortHelper;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge 1.12.2 builds the compass list in {@code updateCreativeSearch} via
 * {@code Item.getSubItems(SEARCH)} — not {@link CreativeTabs#SEARCH#displayAllRelevantItems}.
 */
@Mixin(GuiContainerCreative.class)
public class MixinGuiContainerCreativeSearchSort {

	@Shadow
	private static int selectedTabIndex;

	@Inject(method = "updateCreativeSearch", at = @At("TAIL"))
	private void hbm$sortCreativeSearchItems(CallbackInfo ci) {
		if (CreativeTabs.CREATIVE_TAB_ARRAY[selectedTabIndex] != CreativeTabs.SEARCH) {
			return;
		}
		GuiContainer gui = (GuiContainer) (Object) this;
		if (!(gui.inventorySlots instanceof GuiContainerCreative.ContainerCreative)) {
			return;
		}
		CreativeTabSearchSortHelper.sortSearchList(
				((GuiContainerCreative.ContainerCreative) gui.inventorySlots).itemList);
	}
}