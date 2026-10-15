package com.hbm.render.tileentity;

import com.hbm.Tags;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.network.TileEntityFlowControlPump;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Uses the model, texture, model groups and transforms packaged with HbmMods X5846. */
@AutoRegister
public class RenderFlowControlPump extends TileEntitySpecialRenderer<TileEntityFlowControlPump> implements IItemRendererProvider {
    private static final HFRWavefrontObject MODEL = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/network/flow_control_pump.obj"));
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/models/network/flow_control_pump.png");
    @Override public void render(TileEntityFlowControlPump tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        switch (tile.getBlockMetadata() - BlockDummyable.offset) {
            case 2: GlStateManager.rotate(90, 0, 1, 0); break;
            case 4: GlStateManager.rotate(180, 0, 1, 0); break;
            case 3: GlStateManager.rotate(270, 0, 1, 0); break;
            default: break;
        }
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        bindTexture(TEXTURE);
        MODEL.renderPart("Pump");
        if (tile.topRail) MODEL.renderPart("RailTop");
        if (tile.bottomRail) MODEL.renderPart("RailBottom");
        GlStateManager.translate(0, 0.5, 0);
        GlStateManager.rotate(tile.prevRotor + (tile.rotor - tile.prevRotor) * partialTicks, 0, 0, -1);
        GlStateManager.translate(0, -0.5, 0);
        MODEL.renderPart("Rotor");
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }
    @Override public Item getItemForRenderer() { return Item.getItemFromBlock(ModBlocks.flow_control_pump); }
    @Override public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.scale(5, 5, 5); }
            @Override public void renderCommon() {
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(TEXTURE);
                MODEL.renderPart("Pump");
                MODEL.renderPart("Rotor");
                MODEL.renderPart("RailBottom");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }
}
