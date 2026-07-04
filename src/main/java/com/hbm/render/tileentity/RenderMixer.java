package com.hbm.render.tileentity;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityMachineMixer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import org.lwjgl.opengl.GL11;

import java.awt.*;
@AutoRegister
public class RenderMixer extends TileEntitySpecialRenderer<TileEntityMachineMixer>
    implements IItemRendererProvider {

  @Override
  public void render(TileEntityMachineMixer mixer, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
    GlStateManager.enableAlpha();
    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5D, y, z + 0.5D);
    GlStateManager.enableLighting();
    GlStateManager.disableCull();

    GlStateManager.shadeModel(GL11.GL_SMOOTH);
    bindTexture(mixer.uuMixer ? ResourceManager.mixer_uu_tex : ResourceManager.mixer_tex);
    ResourceManager.mixer.renderPart("Main");

    GlStateManager.pushMatrix();
    GlStateManager.rotate(
        mixer.prevRotation + (mixer.rotation - mixer.prevRotation) * partialTicks, 0, -1, 0);
    ResourceManager.mixer.renderPart("Mixer");
    GlStateManager.popMatrix();

    int totalFill = 0;
    int totalMax = 0;

    for (FluidTankNTM tank : mixer.tanks) {
      if (tank.getTankType() != Fluids.NONE) {
        totalFill += tank.getFill();
        totalMax += tank.getMaxFill();
      }
    }

    if (totalFill > 0) {
      GlStateManager.depthMask(false);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.alphaFunc(GL11.GL_GREATER, 0);
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

      Color color = new Color(mixer.tanks[2].getTankType().getColor());
      GlStateManager.color(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, 0.75F);
      GlStateManager.translate(0, 1, 0);
      if(mixer.uuMixer) {
        GlStateManager.scale(1, 0.99, 1);
      } else {
        GlStateManager.scale(1, (double) totalFill / (double) totalMax * 0.99, 1);
      }
      GlStateManager.translate(0, -1, 0);
      ResourceManager.mixer.renderPart("Fluid");

      GlStateManager.color(1F, 1F, 1F, 1F);
      GlStateManager.depthMask(true);
      GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
    }
    GlStateManager.shadeModel(GL11.GL_FLAT);

    GlStateManager.enableCull();

    GlStateManager.popMatrix();
  }

  @Override
  public Item getItemForRenderer() {
    return Item.getItemFromBlock(ModBlocks.machine_mixer);
  }

  @Override
  public ItemRenderBase getRenderer(Item item) {
    return new ItemRenderBase() {
      public void renderInventory() {
        GlStateManager.translate(0, -5, 0);
        GlStateManager.scale(5, 5, 5);
      }

      public void renderCommon() {
        GlStateManager.rotate(180, 0, 1, 0);
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        bindTexture(ResourceManager.mixer_tex);
        ResourceManager.mixer.renderPart("Main");
        ResourceManager.mixer.renderPart("Mixer");
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();
      }
    };
  }
}
