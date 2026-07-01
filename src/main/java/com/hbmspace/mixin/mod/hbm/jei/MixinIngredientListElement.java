package com.hbmspace.mixin.mod.hbm.jei;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mezz.jei.ingredients.IngredientListElement", remap = false)
public abstract class MixinIngredientListElement {

	@Shadow(remap = false)
	public abstract Object getIngredient();

	private static String hbmModSortName;

	@Inject(method = "getModNameForSorting", at = @At("HEAD"), cancellable = true, remap = false)
	private void hbm$unifyHbmspaceModName(CallbackInfoReturnable<String> cir) {
		Object ingredient = getIngredient();
		if (!(ingredient instanceof ItemStack stack) || stack.isEmpty()) {
			return;
		}
		Item item = stack.getItem();
		if (item == null) {
			return;
		}
		ResourceLocation key = item.getRegistryName();
		if (key == null || !"hbmspace".equals(key.getNamespace())) {
			return;
		}
		String sortName = resolveHbmModSortName();
		if (sortName != null) {
			cir.setReturnValue(sortName);
		}
	}

	private static String resolveHbmModSortName() {
		if (hbmModSortName != null) {
			return hbmModSortName;
		}
		ModContainer hbm = Loader.instance().getIndexedModList().get("hbm");
		if (hbm != null) {
			hbmModSortName = hbm.getName();
		}
		return hbmModSortName;
	}
}