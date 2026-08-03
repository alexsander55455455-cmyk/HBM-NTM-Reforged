package com.hbm.mixin;

import com.hbm.items.tool.ItemBackpack;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies only to damage already aimed at a dropped backpack item entity. */
@Mixin(EntityItem.class)
public abstract class MixinEntityItemBackpackProtection {

    @Unique
    private static final String HBM_LAVA_EXPOSURE_TICKS = "hbmBackpackLavaExposureTicks";

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void hbm$trackBackpackLavaExposure(CallbackInfo ci) {
        EntityItem itemEntity = (EntityItem) (Object) this;
        if (itemEntity.world.isRemote) return;

        ItemStack stack = itemEntity.getItem();
        if (!(stack.getItem() instanceof ItemBackpack) || !itemEntity.isInLava()) {
            itemEntity.getEntityData().removeTag(HBM_LAVA_EXPOSURE_TICKS);
            return;
        }

        int exposure = itemEntity.getEntityData().getInteger(HBM_LAVA_EXPOSURE_TICKS);
        if (exposure < Integer.MAX_VALUE) {
            itemEntity.getEntityData().setInteger(HBM_LAVA_EXPOSURE_TICKS, exposure + 1);
        }
    }

    @Inject(method = "attackEntityFrom", at = @At("HEAD"), cancellable = true)
    private void hbm$protectMaterialBackpacks(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        EntityItem itemEntity = (EntityItem) (Object) this;
        ItemStack stack = itemEntity.getItem();
        if (!(stack.getItem() instanceof ItemBackpack backpack)) return;

        boolean lavaDamage = source != null && ("lava".equals(source.getDamageType())
                || source.isFireDamage() && itemEntity.isInLava());
        if (lavaDamage) {
            int survivalTicks = backpack.getDroppedLavaSurvivalTicks();
            int exposureTicks = itemEntity.getEntityData().getInteger(HBM_LAVA_EXPOSURE_TICKS);
            if (survivalTicks < 0 || survivalTicks > 0 && exposureTicks < survivalTicks) {
                cir.setReturnValue(false);
            }
            return;
        }

        if (backpack.protectsDroppedItemDamage(source, amount)) {
            cir.setReturnValue(false);
        }
    }
}
