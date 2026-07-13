package com.hbm.render.tileentity;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityMachineBlastFurnace;
import com.hbm.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderBlastFurnace extends TileEntitySpecialRenderer<TileEntityMachineBlastFurnace> implements IItemRendererProvider {

    @Override
    public void render(TileEntityMachineBlastFurnace furnace, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        boolean prevLighting = RenderUtil.isLightingEnabled();
        boolean prevCull = RenderUtil.isCullEnabled();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y, z + 0.5D);
        GlStateManager.enableLighting();
        GlStateManager.disableCull();

        switch(furnace.getBlockMetadata() - BlockDummyable.offset) {
            case 2 -> GlStateManager.rotate(90, 0F, 1F, 0F);
            case 4 -> GlStateManager.rotate(180, 0F, 1F, 0F);
            case 3 -> GlStateManager.rotate(270, 0F, 1F, 0F);
            case 5 -> GlStateManager.rotate(0, 0F, 1F, 0F);
        }

        bindTexture(ResourceManager.blast_furnace_tex);
        ResourceManager.blast_furnace.renderAll();

        if(prevCull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        if(prevLighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        GlStateManager.popMatrix();
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.machine_blast_furnace);
    }

    @Override
    public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            public void renderInventory() {
                GlStateManager.translate(0, -4.5, 0);
                GlStateManager.scale(4, 4, 4);
            }

            public void renderCommon() {
                int prevShade = RenderUtil.getShadeModel();
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                GlStateManager.scale(0.5, 0.5, 0.5);
                bindTexture(ResourceManager.blast_furnace_tex);
                ResourceManager.blast_furnace.renderAll();
                GlStateManager.shadeModel(prevShade);
            }
        };
    }
}