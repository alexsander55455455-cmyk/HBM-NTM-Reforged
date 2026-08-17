package com.hbm.compat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "com.leafia.dev.blocks.legacy.LegacyBlockHazardMeta", remap = false)
public abstract class MixinCursedAddonLegacyHazardTextures {

    @ModifyVariable(
            method = "generateBlockFrames",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private String hbm$resolvePackagedSnowTextureName(String registryName) {
        return "waste_snow_block".equals(registryName) ? "waste_snow" : registryName;
    }
}
