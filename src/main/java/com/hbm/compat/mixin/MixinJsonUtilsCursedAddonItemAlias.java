package com.hbm.compat.mixin;

import com.google.gson.JsonElement;
import net.minecraft.item.Item;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JsonUtils.class)
public abstract class MixinJsonUtilsCursedAddonItemAlias {

    @Inject(
            method = "getItem(Lcom/google/gson/JsonElement;Ljava/lang/String;)Lnet/minecraft/item/Item;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void hbm$resolveMovedHardDrive(JsonElement element, String memberName,
                                                   CallbackInfoReturnable<Item> cir) {
        if (!Loader.isModLoaded("leafia") || !element.isJsonPrimitive()
                || !"hbm:hard_drive_full".equals(element.getAsString())) {
            return;
        }
        Item item = Item.getByNameOrId("hbmspace:hard_drive_full");
        if (item != null) {
            cir.setReturnValue(item);
        }
    }
}
