package com.hbm.compat.mixin;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FMLModContainer.class, remap = false)
public abstract class MixinFMLModContainerCursedAddonCompatibility {

    private static final ArtifactVersion CURSED_ADDON_API_VERSION =
            new DefaultArtifactVersion("hbm", "2.5.0.5-reforged.1.0.5");

    @Shadow
    public abstract String getModId();

    @Inject(method = "getProcessedVersion", at = @At("HEAD"), cancellable = true)
    private void hbm$provideCursedAddonApiVersion(CallbackInfoReturnable<ArtifactVersion> cir) {
        if ("hbm".equals(getModId())
                && Launch.classLoader.getResource("com/leafia/AddonBase.class") != null) {
            cir.setReturnValue(CURSED_ADDON_API_VERSION);
        }
    }
}
