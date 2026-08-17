package com.hbm.compat.mixin;

import com.hbm.compat.CursedAddonItemVisibility;
import com.hbm.items.IDynamicModels;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.leafia.eventbuses.LeafiaClientListener$HandlerClient", remap = false)
public abstract class MixinCursedAddonModelRegistration {

    @Inject(method = "registerModel", at = @At("HEAD"), cancellable = true, remap = false)
    private void hbm$skipInternalAndDynamicModels(Item item, int meta, CallbackInfo ci) {
        if (item == null || item == Items.AIR
                || hbm$isReforgedTeisr(item, "modular_turbine_core")
                || item instanceof IDynamicModels && IDynamicModels.INSTANCES.contains(item)
                || item instanceof ItemBlock && ((ItemBlock) item).getBlock() instanceof IDynamicModels dynamic
                && IDynamicModels.INSTANCES.contains(dynamic)
                || CursedAddonItemVisibility.shouldHide(item)) {
            ci.cancel();
        }
    }

    private static boolean hbm$isReforgedTeisr(Item item, String path) {
        return item.getRegistryName() != null
                && "leafia".equals(item.getRegistryName().getNamespace())
                && path.equals(item.getRegistryName().getPath());
    }
}
