package com.hbm.render.tileentity;

import com.hbm.interfaces.AutoRegister;
import com.hbm.tileentity.machine.pile.TileEntityPileVent;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderPileVent extends TileEntitySpecialRenderer<TileEntityPileVent> {

    @Override
    public void render(TileEntityPileVent vent, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y, z + 0.5D);
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        RenderPileLoader.rotate(vent.getBlockMetadata() % 4);

        float rotation = vent.lastFan + (vent.fan - vent.lastFan) * partialTicks;
        bindTexture(RenderPileLoader.VENT_TEXTURE);
        RenderPileLoader.VENT.renderPart("Pipe");
        GlStateManager.rotate(rotation, 0F, 1F, 0F);
        RenderPileLoader.VENT.renderPart("Fan");
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }
}
