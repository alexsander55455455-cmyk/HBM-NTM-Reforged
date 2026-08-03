package com.hbm.render.tileentity;

import com.hbm.Tags;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.lib.ForgeDirection;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderSatLink extends TileEntitySpecialRenderer<TileEntityMachineSatLink> implements IItemRendererProvider {

    private static final HFRWavefrontObject MODEL =
            new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/machines/satlink.obj"));
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Tags.MODID, "textures/models/machines/satlink.png");

    @Override
    public void render(TileEntityMachineSatLink link, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y, z + 0.5D);
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.rotate(180F, 0F, 1F, 0F);

        ForgeDirection direction = ForgeDirection.getOrientation(link.getBlockMetadata() - BlockDummyable.offset);
        ForgeDirection right = direction.getRotation(ForgeDirection.DOWN);
        GlStateManager.translate((direction.offsetX + right.offsetX) * 0.5D, 0D,
                (direction.offsetZ + right.offsetZ) * 0.5D);

        float rotation = link.prevRot + (link.rot - link.prevRot) * partialTicks;
        float lift = link.prevLift + (link.lift - link.prevLift) * partialTicks;
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        bindTexture(TEXTURE);
        MODEL.renderPart("Base");
        GlStateManager.rotate(rotation, 0F, 1F, 0F);
        MODEL.renderPart("Rotor");
        GlStateManager.translate(0D, 7.375D, 0D);
        GlStateManager.rotate(lift, 0F, 0F, 1F);
        GlStateManager.translate(0D, -7.375D, 0D);
        MODEL.renderPart("Dish");
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }

    @Override public Item getItemForRenderer() { return Item.getItemFromBlock(ModBlocks.machine_satlink); }

    @Override
    public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override public void renderInventory() {
                GlStateManager.translate(0D, -5D, 0D);
                GlStateManager.scale(3.5D, 3.5D, 3.5D);
            }

            @Override public void renderCommon(ItemStack stack) {
                GlStateManager.scale(0.5D, 0.5D, 0.5D);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(TEXTURE);
                MODEL.renderPart("Base");
                GlStateManager.rotate(15F, 0F, 1F, 0F);
                MODEL.renderPart("Rotor");
                GlStateManager.translate(0D, 7.375D, 0D);
                GlStateManager.rotate(-45F, 0F, 0F, 1F);
                GlStateManager.translate(0D, -7.375D, 0D);
                MODEL.renderPart("Dish");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }
}
