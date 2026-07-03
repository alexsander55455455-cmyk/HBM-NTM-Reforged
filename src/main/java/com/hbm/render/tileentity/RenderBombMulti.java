package com.hbm.render.tileentity;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.bomb.TileEntityBombMulti;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderBombMulti extends TileEntitySpecialRenderer<TileEntityBombMulti> implements IItemRendererProvider {

    @Override
    public void render(TileEntityBombMulti te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        // World rendering is handled by StaticTesrBakedModels for bomb_multi.
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.bomb_multi);
    }

    @Override
    public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override
            public void renderInventory() {
                GlStateManager.translate(0, -1, 0);
                GlStateManager.scale(4, 4, 4);
            }

            @Override
            public void renderCommon() {
                GlStateManager.translate(0.75F, 0, 0);
                GlStateManager.scale(3, 3, 3);
                GlStateManager.translate(0, 0.5F, 0);
                GlStateManager.rotate(180, 1, 0, 0);
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.disableCull();
                bindTexture(ResourceManager.bomb_multi_tex);
                ResourceManager.bomb_multi.renderAll();
                GlStateManager.enableCull();
            }
        };
    }
}