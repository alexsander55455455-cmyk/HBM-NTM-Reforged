package com.hbm.compat.mixin;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leafia's VintageFix correction targets VintageFix's replaced vanilla model
 * perspective. Reforged gives Leafia TEISR items an explicit stable
 * perspective instead, so applying both corrections shifts and rotates every
 * 3D item a second time.
 */
@Pseudo
@Mixin(targets = "com.leafia.dev.items.LeafiaGripOffsetHelper", remap = false)
public abstract class MixinCursedAddonGripOffset {

    @Inject(method = "fixGrip", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hbm$skipObsoleteVintageFixOffset(TransformType type, CallbackInfo ci) {
        ci.cancel();
    }
}
