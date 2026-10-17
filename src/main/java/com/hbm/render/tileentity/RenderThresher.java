package com.hbm.render.tileentity;

import com.hbm.Tags;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.TileEntityMachineThresher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderThresher extends TileEntitySpecialRenderer<TileEntityMachineThresher> implements IItemRendererProvider {
    private static final HFRWavefrontObject MODEL = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/machines/thresher.obj"));
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/models/machines/thresher.png");

    @Override public void render(TileEntityMachineThresher thresher, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        switch(thresher.getBlockMetadata()) {
            case 3: GlStateManager.rotate(180, 0, 1, 0); break;
            case 5: GlStateManager.rotate(270, 0, 1, 0); break;
            case 4: GlStateManager.rotate(90, 0, 1, 0); break;
            default: break;
        }
        double angle = thresher.prevAngle + (thresher.angle - thresher.prevAngle) * partialTicks;
        double spin = thresher.lastSpin + (thresher.spin - thresher.lastSpin) * partialTicks;
        double engine = thresher.isOn ? Math.sin(thresher.getWorld().getTotalWorldTime() * 2 % (Math.PI * 2) + partialTicks) : 0;
        renderCommon(82.5 - angle, spin, engine);
        GlStateManager.popMatrix();
    }

    private void renderCommon(double angle, double spin, double engine) {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        bindTexture(TEXTURE);
        MODEL.renderPart("Base");
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, engine * 0.01, 0);
        MODEL.renderPart("Engine");
        GlStateManager.popMatrix();
        GlStateManager.translate(0, 0.5, -1);
        GlStateManager.rotate((float) angle, 1, 0, 0);
        GlStateManager.translate(0, -0.5, 1);
        MODEL.renderPart("ArmUpper");
        GlStateManager.translate(0, 0.5, -5);
        GlStateManager.rotate((float) (angle * -2), 1, 0, 0);
        GlStateManager.translate(0, -0.5, 5);
        GlStateManager.translate(-0.01, 0, 0);
        MODEL.renderPart("ArmLower");
        GlStateManager.translate(0.01, 0, 0);
        GlStateManager.translate(0, 0.5, -9);
        GlStateManager.rotate((float) angle, 1, 0, 0);
        GlStateManager.translate(0, -0.5, 9);
        GlStateManager.translate(0.01, 0, 0);
        MODEL.renderPart("Front");
        GlStateManager.translate(0, 0.5, -11);
        GlStateManager.rotate((float) -spin, 1, 0, 0);
        GlStateManager.translate(0, -0.5, 11);
        MODEL.renderPart("Wheel");
        GlStateManager.shadeModel(GL11.GL_FLAT);
    }

    @Override public Item getItemForRenderer() { return Item.getItemFromBlock(ModBlocks.machine_thresher); }
    @Override public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, 4, -8);GlStateManager.scale(4.5, 4.5, 4.5); }
            @Override public void renderCommon(ItemStack stack) {
                GlStateManager.scale(0.5, 0.5, 0.5);
                GlStateManager.rotate(90, 0, -1, 0);
                RenderThresher.this.renderCommon(80, System.currentTimeMillis() % 3600 * 0.25, 0);
            }
        };
    }
}
