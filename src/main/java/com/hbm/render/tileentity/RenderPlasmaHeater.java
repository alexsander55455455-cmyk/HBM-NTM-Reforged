package com.hbm.render.tileentity;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityMachinePlasmaHeater;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderPlasmaHeater extends TileEntitySpecialRenderer<TileEntityMachinePlasmaHeater> implements IItemRendererProvider {

    @Override
    public void render(TileEntityMachinePlasmaHeater te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        // World rendering is handled by StaticTesrBakedModels for plasma_heater.
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.plasma_heater);
    }

    @Override
    public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override
            public void renderInventory() {
                GlStateManager.translate(0, -1, 0);
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.scale(2.5F, 2.5F, 2.5F);
            }

            @Override
            public void renderCommon() {
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
                GlStateManager.translate(0, 0, 14);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(ResourceManager.iter_microwave);
                ResourceManager.iter.renderPart("Microwave");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }
}