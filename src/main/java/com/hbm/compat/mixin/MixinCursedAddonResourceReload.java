package com.hbm.compat.mixin;

import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.leafia.init.ResourceInit", remap = false)
public abstract class MixinCursedAddonResourceReload {

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/SplashProgress;pause()V"), remap = false)
    private static void hbm$pauseStartupSplash() {
        if (!Loader.instance().hasReachedState(LoaderState.AVAILABLE)) {
            SplashProgress.pause();
        }
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/SplashProgress;resume()V"), remap = false)
    private static void hbm$resumeStartupSplash() {
        if (!Loader.instance().hasReachedState(LoaderState.AVAILABLE)) {
            SplashProgress.resume();
        }
    }
}
