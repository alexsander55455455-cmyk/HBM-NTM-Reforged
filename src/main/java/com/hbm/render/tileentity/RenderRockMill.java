package com.hbm.render.tileentity;

import com.hbm.Tags;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.TileEntityMachineRockMill;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderRockMill extends TileEntitySpecialRenderer<TileEntityMachineRockMill> implements IItemRendererProvider {
    private static final HFRWavefrontObject MODEL = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/machines/rockmill.obj"));
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/models/machines/rockmill.png");

    @Override public void render(TileEntityMachineRockMill mill, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.enableLighting();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        switch (mill.getBlockMetadata() - BlockDummyable.offset) {
            case 4: GlStateManager.rotate(90, 0, 1, 0); break;
            case 3: GlStateManager.rotate(180, 0, 1, 0); break;
            case 5: GlStateManager.rotate(270, 0, 1, 0); break;
            default: break;
        }
        bindTexture(TEXTURE);
        MODEL.renderPart("Base");
        if (mill.frame) MODEL.renderPart("Frame");
        GlStateManager.rotate(mill.prevRotation + (mill.rotation - mill.prevRotation) * partialTicks, 0, -1, 0);
        MODEL.renderPart("Wheel");
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }

    @Override public Item getItemForRenderer() { return Item.getItemFromBlock(ModBlocks.machine_rockmill); }

    @Override public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override public void renderInventory() {
                GlStateManager.translate(0, -1.5, 0);
                GlStateManager.scale(3, 3, 3);
            }
            @Override public void renderCommon(ItemStack stack) {
                GlStateManager.scale(0.75, 0.75, 0.75);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(TEXTURE);
                MODEL.renderAll();
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }
}
