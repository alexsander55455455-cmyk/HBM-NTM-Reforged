package com.hbm.render.tileentity;

import com.hbm.Tags;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.TileEntityMachineSuperComputer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderSuperComputer extends TileEntitySpecialRenderer<TileEntityMachineSuperComputer> implements IItemRendererProvider {
    private static final HFRWavefrontObject MODEL = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/machines/supercomputer.obj"));
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/models/machines/supercomputer.png");
    private static final ResourceLocation SCAN = new ResourceLocation(Tags.MODID, "textures/models/machines/supercomputer_scan.png");

    @Override public void render(TileEntityMachineSuperComputer computer, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + .5, y, z + .5);
        GlStateManager.enableLighting();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1, 1, 1, 1);
        switch(computer.getBlockMetadata() - BlockDummyable.offset) {
            case 2: GlStateManager.rotate(180, 0, 1, 0); break;
            case 4: GlStateManager.rotate(270, 0, 1, 0); break;
            case 5: GlStateManager.rotate(90, 0, 1, 0); break;
            default: break;
        }
        bindTexture(TEXTURE);
        MODEL.renderPart("Computer");
        if(!computer.didProcess) GlStateManager.color(0, 0, 0, 1);
        float scroll = (computer.getWorld().getTotalWorldTime() % 20 + partialTicks) / 20F;
        float brightnessX = OpenGlHelper.lastBrightnessX, brightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        GlStateManager.disableLighting();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        bindTexture(SCAN);
        GL11.glTranslatef(-scroll, 0, 0);
        MODEL.renderPart("Lights");
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableLighting();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }
    @Override public Item getItemForRenderer() { return Item.getItemFromBlock(ModBlocks.machine_supercomputer); }
    @Override public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, -2.5, 0);GlStateManager.scale(2.5, 2.5, 2.5); }
            @Override public void renderCommon(ItemStack stack) {
                GlStateManager.scale(.5, .5, .5);
                GlStateManager.translate(-2, 0, 0);
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.color(1, 1, 1, 1);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(TEXTURE);MODEL.renderPart("Computer");
                bindTexture(SCAN);MODEL.renderPart("Lights");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }
}
