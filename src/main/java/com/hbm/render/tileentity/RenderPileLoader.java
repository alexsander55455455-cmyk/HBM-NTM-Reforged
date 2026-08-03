package com.hbm.render.tileentity;

import com.hbm.Tags;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.interfaces.AutoRegister;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderPileLoader extends TileEntitySpecialRenderer<TileEntityPileLoader> implements IItemRendererProvider {

    static final HFRWavefrontObject LOADER =
            new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/pile/pile_loader.obj"));
    static final HFRWavefrontObject VENT =
            new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/pile/pile_vent.obj"));
    static final HFRWavefrontObject CONTROL =
            new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/pile/pile_control.obj"));
    static final ResourceLocation LOADER_TEXTURE =
            new ResourceLocation(Tags.MODID, "textures/models/pile/pile_loader.png");
    static final ResourceLocation VENT_TEXTURE =
            new ResourceLocation(Tags.MODID, "textures/models/pile/pile_vent.png");
    static final ResourceLocation CONTROL_TEXTURE =
            new ResourceLocation(Tags.MODID, "textures/models/pile/pile_control.png");

    @Override
    public void render(TileEntityPileLoader loader, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y, z + 0.5D);
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        rotate(loader.getBlockMetadata() % 4);

        double position = loader.lastLevel + (loader.level - loader.lastLevel) * partialTicks;
        bindTexture(LOADER_TEXTURE);
        LOADER.renderPart("Loader");
        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.1875D, 0.5D, 0D);
        GlStateManager.rotate((float) (position * 90D), 0F, 0F, 1F);
        GlStateManager.translate(0.1875D, -0.5D, 0D);
        LOADER.renderPart("Lever");
        GlStateManager.popMatrix();

        GlStateManager.translate(position * -0.5D, 0D, 0D);
        LOADER.renderPart("Slider");
        if (!loader.syncStack.isEmpty()) LOADER.renderPart("Rod");
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }

    static void rotate(int orientation) {
        if (orientation == 0) GlStateManager.rotate(90F, 0F, 1F, 0F);
        if (orientation == 1) GlStateManager.rotate(270F, 0F, 1F, 0F);
        if (orientation == 2) GlStateManager.rotate(180F, 0F, 1F, 0F);
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.pile_device);
    }

    @Override
    public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override
            public void renderInventory() {
                GlStateManager.translate(0D, -3.5D, 0D);
                GlStateManager.scale(5D, 5D, 5D);
            }

            @Override
            public void renderCommon(ItemStack stack) {
                GlStateManager.scale(2D, 2D, 2D);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                int meta = stack.isEmpty() ? 0 : stack.getMetadata();
                if (meta == BlockPileDevice.ITEM_META_VENT) {
                    bindTexture(VENT_TEXTURE);
                    VENT.renderAll();
                } else if (meta == BlockPileDevice.ITEM_META_CONTROL) {
                    bindTexture(CONTROL_TEXTURE);
                    CONTROL.renderAll();
                } else {
                    bindTexture(LOADER_TEXTURE);
                    LOADER.renderAll();
                }
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }
}
