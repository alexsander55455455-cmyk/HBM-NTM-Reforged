package com.hbm.mixin;

import com.hbm.compat.NtmdopolnenieCompat;
import com.hbm.compat.StructureLegacyRemap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.gen.structure.template.Template;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Template.class)
public abstract class MixinTemplateNtmdopolnenie {

    @Inject(
            method = "read(Lnet/minecraft/nbt/NBTTagCompound;)V",
            at = @At("HEAD")
    )
    private void hbm$remapAddonStructureNbt(NBTTagCompound compound, CallbackInfo ci) {
        if (NtmdopolnenieCompat.isRemappingAddonTemplate()) {
            StructureLegacyRemap.remapVanillaStructureNbt(compound);
        }
    }
}