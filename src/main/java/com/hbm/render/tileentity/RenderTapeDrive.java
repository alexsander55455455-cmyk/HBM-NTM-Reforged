package com.hbm.render.tileentity;

import com.hbm.Tags;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.TileEntityMachineTapeDrive;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderTapeDrive extends TileEntitySpecialRenderer<TileEntityMachineTapeDrive> implements IItemRendererProvider {
    private static final HFRWavefrontObject MODEL = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/machines/tape_drive.obj"));
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/models/machines/tape_drive.png");

    @Override public void render(TileEntityMachineTapeDrive drive, double x, double y, double z, float ticks, int stage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + .5, y, z + .5);
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        switch(drive.getBlockMetadata()) {
            case 3: GlStateManager.rotate(270, 0, 1, 0); break;
            case 2: GlStateManager.rotate(90, 0, 1, 0); break;
            case 4: GlStateManager.rotate(180, 0, 1, 0); break;
            default: break;
        }
        bindTexture(TEXTURE);
        MODEL.renderPart("Frame");
        for(int i = 0; i < drive.tapes.length; i++) if(drive.tapes[i] != TileEntityMachineTapeDrive.SLOT_EMPTY) renderSlot(i, "Drive");
        float brightnessX = OpenGlHelper.lastBrightnessX, brightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        for(int i = 0; i < drive.tapes.length; i++) {
            byte tape = drive.tapes[i];
            if(tape == TileEntityMachineTapeDrive.SLOT_EMPTY) continue;
            if(tape == TileEntityMachineTapeDrive.SLOT_ANY) GlStateManager.color(1, 0, 0, 1);
            else if(tape == TileEntityMachineTapeDrive.SLOT_EMPTY_TAPE) GlStateManager.color(1, .75F, 0, 1);
            else GlStateManager.color(0, 1, 0, 1);
            renderSlot(i, "Light");
        }
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableTexture2D();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
    private void renderSlot(int slot, String part) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, .25 - .5 * (slot / 6), .3125 - (slot % 6) * .125);
        MODEL.renderPart(part);
        GlStateManager.popMatrix();
    }
    @Override public Item getItemForRenderer() { return Item.getItemFromBlock(ModBlocks.machine_tape_drive); }
    @Override public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, -3, 0); GlStateManager.scale(5, 5, 5); }
            @Override public void renderCommon(ItemStack stack) {
                GlStateManager.scale(2, 2, 2);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(TEXTURE);
                MODEL.renderPart("Frame");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }
}
