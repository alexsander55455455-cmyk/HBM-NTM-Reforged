package com.hbm.compat.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies sane break/hit particles for Leafia TESR blocks that have no baked block model. */
@Mixin(BlockModelShapes.class)
public abstract class MixinBlockModelShapesCursedAddonParticles {

    @Inject(method = "getTexture", at = @At("RETURN"), cancellable = true)
    private void hbm$replaceMissingCursedAddonParticle(IBlockState state,
                                                        CallbackInfoReturnable<TextureAtlasSprite> cir) {
        if (state == null) return;

        Block block = state.getBlock();
        ResourceLocation name = block.getRegistryName();
        if (name == null || !"leafia".equals(name.getNamespace())) return;

        TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite current = cir.getReturnValue();
        if (current != null && current != textureMap.getMissingSprite()
                && !"missingno".equals(current.getIconName())) return;

        cir.setReturnValue(textureMap.getAtlasSprite(hbm$fallbackTexture(state.getMaterial())));
    }

    private static String hbm$fallbackTexture(Material material) {
        if (material == Material.GLASS || material == Material.ICE || material == Material.PACKED_ICE) {
            return "minecraft:blocks/glass";
        }
        if (material == Material.WOOD) return "minecraft:blocks/planks_oak";
        if (material == Material.GROUND || material == Material.GRASS) return "minecraft:blocks/dirt";
        if (material == Material.SAND) return "minecraft:blocks/sand";
        if (material == Material.SNOW || material == Material.CRAFTED_SNOW) return "minecraft:blocks/snow";
        if (material == Material.CLAY) return "minecraft:blocks/clay";
        if (material == Material.ROCK) return "minecraft:blocks/stone";
        return "minecraft:blocks/iron_block";
    }
}
