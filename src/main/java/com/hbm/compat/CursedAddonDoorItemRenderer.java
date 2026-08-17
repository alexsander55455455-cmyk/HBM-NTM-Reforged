package com.hbm.compat;

import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.tileentity.door.IRenderDoors;
import com.hbm.tileentity.TileEntityDoorGeneric;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.nio.DoubleBuffer;

/** Renders Leafia's crimson doors as centered inventory/held items. */
public final class CursedAddonDoorItemRenderer extends ItemRenderBase {

    private final IRenderDoors renderer;
    private final TileEntityDoorGeneric dummyDoor = new TileEntityDoorGeneric();
    private final DoubleBuffer clipBuffer = GLAllocation.createDirectByteBuffer(8 * 4).asDoubleBuffer();
    private final double modelScale;

    public CursedAddonDoorItemRenderer(BlockDoorGeneric block) {
        this.renderer = block.type.getSEDNARenderer();
        this.dummyDoor.setDoorType(block.type);

        int[] dimensions = block.type.getDimensions();
        int width = dimensions[4] + dimensions[5] + 1;
        int height = dimensions[0] + dimensions[1] + 1;
        this.modelScale = 12D / Math.max(width, height);
    }

    @Override
    public void renderNonInv(ItemStack stack) {
        GlStateManager.scale(0.25F, 0.25F, 0.25F);
    }

    @Override
    public void renderGround() {
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
    }

    @Override
    public void renderCommon(ItemStack stack) {
        if (renderer == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.scale(modelScale, modelScale, modelScale);
        GlStateManager.rotate(90F, 0F, 1F, 0F);
        GlStateManager.translate(0F, -1.5F, 0F);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        renderer.render(dummyDoor, clipBuffer);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }
}
