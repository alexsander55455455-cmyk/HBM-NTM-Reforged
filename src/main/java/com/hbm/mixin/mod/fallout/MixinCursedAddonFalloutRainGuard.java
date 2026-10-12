package com.hbm.mixin.mod.fallout;

import com.hbm.entity.effect.EntityFalloutRain;
import org.spongepowered.asm.mixin.Mixin;

// ModMixinConfigPlugin.postApply guards the addon's hook after all mixins have been merged.
@Mixin(value = EntityFalloutRain.class, priority = 900, remap = false)
public abstract class MixinCursedAddonFalloutRainGuard {
}
