package com.hbm.mixin;

import com.hbm.compat.NtmdopolnenieCompat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TemplateManager.class)
public abstract class MixinTemplateManagerNtmdopolnenie {

    @Inject(
            method = "getTemplate(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/world/gen/structure/template/Template;",
            at = @At("HEAD")
    )
    private void hbm$beginAddonTemplateRemapGetTemplate(MinecraftServer server, ResourceLocation location, CallbackInfoReturnable<Template> ci) {
        hbm$beginAddonTemplateRemap(location);
    }

    @Inject(
            method = "getTemplate(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/world/gen/structure/template/Template;",
            at = @At("RETURN")
    )
    private void hbm$endAddonTemplateRemapGetTemplate(MinecraftServer server, ResourceLocation location, CallbackInfoReturnable<Template> ci) {
        hbm$endAddonTemplateRemap(location);
    }

    @Inject(
            method = "get(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/world/gen/structure/template/Template;",
            at = @At("HEAD")
    )
    private void hbm$beginAddonTemplateRemapGet(MinecraftServer server, ResourceLocation location, CallbackInfoReturnable<Template> ci) {
        hbm$beginAddonTemplateRemap(location);
    }

    @Inject(
            method = "get(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/world/gen/structure/template/Template;",
            at = @At("RETURN")
    )
    private void hbm$endAddonTemplateRemapGet(MinecraftServer server, ResourceLocation location, CallbackInfoReturnable<Template> ci) {
        hbm$endAddonTemplateRemap(location);
    }

    @Inject(
            method = "readTemplate(Lnet/minecraft/util/ResourceLocation;)Z",
            at = @At("HEAD")
    )
    private void hbm$beginAddonTemplateRemapRead(ResourceLocation location, CallbackInfoReturnable<Boolean> ci) {
        hbm$beginAddonTemplateRemap(location);
    }

    @Inject(
            method = "readTemplate(Lnet/minecraft/util/ResourceLocation;)Z",
            at = @At("RETURN")
    )
    private void hbm$endAddonTemplateRemapRead(ResourceLocation location, CallbackInfoReturnable<Boolean> ci) {
        hbm$endAddonTemplateRemap(location);
    }

    private static void hbm$beginAddonTemplateRemap(ResourceLocation location) {
        if (NtmdopolnenieCompat.isActive() && "ntmdopolnenie".equals(location.getNamespace())) {
            NtmdopolnenieCompat.beginAddonTemplateRemap();
        }
    }

    private static void hbm$endAddonTemplateRemap(ResourceLocation location) {
        if (NtmdopolnenieCompat.isActive() && "ntmdopolnenie".equals(location.getNamespace())) {
            NtmdopolnenieCompat.endAddonTemplateRemap();
        }
    }
}