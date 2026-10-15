package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.AutoRegister;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;

@AutoRegister
public class RenderAUTOCAL extends TileEntitySpecialRenderer<TileEntityRadioAUTOCAL> implements IItemRendererProvider {

	@Override
	public void render(TileEntityRadioAUTOCAL tile, double x, double y, double z, float interp, int destroyStage, float alpha) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y, z + 0.5);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_CULL_FACE);

		switch(tile.getBlockMetadata() - BlockDummyable.offset) {
		case 2: GL11.glRotatef(90, 0F, 1F, 0F); break;
		case 4: GL11.glRotatef(180, 0F, 1F, 0F); break;
		case 3: GL11.glRotatef(270, 0F, 1F, 0F); break;
		case 5: GL11.glRotatef(0, 0F, 1F, 0F); break;
		}

		bindTexture(ResourceManager.autocal_tex);
		ResourceManager.autocal.renderAll();

		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glPopMatrix();
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.radio_autocal);
	}

	@Override
	public ItemRenderBase getRenderer(Item item) {
		return new ItemRenderBase( ) {
			public void renderInventory() {
				GL11.glTranslated(0, -4, 0);
				GL11.glScaled(6.25, 6.25, 6.25);
			}
			public void renderCommon() {
				bindTexture(ResourceManager.autocal_tex);
				ResourceManager.autocal.renderAll();
			}};
	}
}
