package com.hbm.render.tileentity;

import com.hbm.Tags;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.network.TileEntityNetworkSeparator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Uses the model and texture packaged with HbmMods X5846. */
@AutoRegister
public class RenderNetworkSeparator extends TileEntitySpecialRenderer<TileEntityNetworkSeparator> implements IItemRendererProvider {
    private static final HFRWavefrontObject MODEL = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/network/network_separator.obj"));
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/models/network/network_separator.png");
    @Override public void render(TileEntityNetworkSeparator tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        switch (tile.getBlockMetadata() - 10) {
            case 2: GlStateManager.rotate(90, 0, 1, 0); break;
            case 4: GlStateManager.rotate(180, 0, 1, 0); break;
            case 3: GlStateManager.rotate(270, 0, 1, 0); break;
            default: break;
        }
        renderModel();
        GlStateManager.popMatrix();
    }
    private void renderModel() {
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        bindTexture(TEXTURE);
        MODEL.renderAll();
        GlStateManager.shadeModel(GL11.GL_FLAT);
    }
    @Override public Item getItemForRenderer() { return Item.getItemFromBlock(ModBlocks.network_separator); }
    @Override public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, -3, 0); GlStateManager.scale(5, 5, 5); }
            @Override public void renderCommon() { renderModel(); }
        };
    }
}
