package com.hbm.render.tileentity;

import com.hbm.interfaces.AutoRegister;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderPileControl extends TileEntitySpecialRenderer<TileEntityPileControl> {

    @Override
    public void render(TileEntityPileControl control, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y, z + 0.5D);
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        RenderPileLoader.rotate(control.getBlockMetadata() % 4);

        double level = control.lastLevel + (control.level - control.lastLevel) * partialTicks;
        bindTexture(RenderPileLoader.CONTROL_TEXTURE);
        RenderPileLoader.CONTROL.renderPart("Base");
        GlStateManager.translate(0D, level * 0.75D, 0D);
        RenderPileLoader.CONTROL.renderPart("Rod");
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }
}
