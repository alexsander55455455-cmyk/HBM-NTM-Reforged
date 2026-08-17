package com.hbm.compat;

import com.hbm.render.NTMRenderHelper;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Restores the addon core's TESR model as a centered inventory/held item. */
public final class CursedAddonModularTurbineCoreItemRenderer extends ItemRenderBase {

    private static final ResourceLocation MODEL = new ResourceLocation(
            "leafia", "textures/_integrated/machines/modular_turbines/export.obj");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "leafia", "textures/_integrated/machines/modular_turbines/texture1.png");

    private final HFRWavefrontObject model = new HFRWavefrontObject(MODEL);

    @Override
    public void renderNonInv(ItemStack stack) {
        GlStateManager.scale(0.25F, 0.25F, 0.25F);
    }

    @Override
    public void renderGround() {
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
    }

    @Override
    public void renderCommon(ItemStack stack) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(12F, 12F, 12F);
        GlStateManager.translate(0F, -0.5F, 0F);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        NTMRenderHelper.bindTexture(TEXTURE);
        model.renderPart("Core");
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }
}
