package com.hbm.render.tileentity;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;
import com.hbm.render.NTMRenderHelper;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityAMSEmitter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Random;

@AutoRegister
public class RenderAMSEmitter extends TileEntitySpecialRenderer<TileEntityAMSEmitter> implements IItemRendererProvider {

	private final Random rand = new Random();

	@Override
	public boolean isGlobalRenderer(TileEntityAMSEmitter te) {
		return true;
	}

	@Override
	public void render(TileEntityAMSEmitter te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();
		GlStateManager.disableCull();
		GlStateManager.rotate(180, 0F, 1F, 0F);

		if(te.locked) {
			bindTexture(ResourceManager.ams_destroyed_tex);
			ResourceManager.ams_emitter_destroyed.renderAll();
		} else {
			bindTexture(ResourceManager.ams_emitter_tex);
			ResourceManager.ams_emitter.renderAll();
		}

		GlStateManager.popMatrix();
		renderBeamEffect(te, x, y, z, partialTicks);
		GlStateManager.enableCull();
	}

	private void renderBeamEffect(TileEntityAMSEmitter emitter, double x, double y, double z, float partialTicks) {
		float radius = 0.04F;
		int distance = 1;
		int layers = 3;

		GlStateManager.pushMatrix();
		GlStateManager.disableTexture2D();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.enableLighting();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GlStateManager.translate((float) x + 0.5F, (float) y - 7, (float) z + 0.5F);

		if(!emitter.locked && emitter.efficiency > 0) {
			NTMRenderHelper.startDrawingColored(GL11.GL_QUADS);

			double lastPosX = 0;
			double lastPosZ = 0;

			for(int i = 7; i > 0; i -= distance) {
				double posX = rand.nextDouble() - 0.5;
				double posZ = rand.nextDouble() - 0.5;

				for(int j = 1; j <= layers; j++) {
					NTMRenderHelper.addVertexColor((float) (lastPosX + (radius * j)), i, (float) (lastPosZ + (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (lastPosX + (radius * j)), i, (float) (lastPosZ - (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (posX + (radius * j)), i - distance, (float) (posZ - (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (posX + (radius * j)), i - distance, (float) (posZ + (radius * j)), 1, 0.5F, 0, 1f);

					NTMRenderHelper.addVertexColor((float) (lastPosX - (radius * j)), i, (float) (lastPosZ + (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (lastPosX - (radius * j)), i, (float) (lastPosZ - (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (posX - (radius * j)), i - distance, (float) (posZ - (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (posX - (radius * j)), i - distance, (float) (posZ + (radius * j)), 1, 0.5F, 0, 1f);

					NTMRenderHelper.addVertexColor((float) (lastPosX + (radius * j)), i, (float) (lastPosZ + (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (lastPosX - (radius * j)), i, (float) (lastPosZ + (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (posX - (radius * j)), i - distance, (float) (posZ + (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (posX + (radius * j)), i - distance, (float) (posZ + (radius * j)), 1, 0.5F, 0, 1f);

					NTMRenderHelper.addVertexColor((float) (lastPosX + (radius * j)), i, (float) (lastPosZ - (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (lastPosX - (radius * j)), i, (float) (lastPosZ - (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (posX - (radius * j)), i - distance, (float) (posZ - (radius * j)), 1, 0.5F, 0, 1f);
					NTMRenderHelper.addVertexColor((float) (posX + (radius * j)), i - distance, (float) (posZ - (radius * j)), 1, 0.5F, 0, 1f);
				}

				lastPosX = posX;
				lastPosZ = posZ;
			}

			for(int j = 1; j <= 2; j++) {
				NTMRenderHelper.addVertexColor(0 + (radius * j), 7, 0 + (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 + (radius * j), 7, 0 - (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 + (radius * j), 0, 0 - (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 + (radius * j), 0, 0 + (radius * j), 1, 1, 0, 1f);

				NTMRenderHelper.addVertexColor(0 - (radius * j), 7, 0 + (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 - (radius * j), 7, 0 - (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 - (radius * j), 0, 0 - (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 - (radius * j), 0, 0 + (radius * j), 1, 1, 0, 1f);

				NTMRenderHelper.addVertexColor(0 + (radius * j), 7, 0 + (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 - (radius * j), 7, 0 + (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 - (radius * j), 0, 0 + (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 + (radius * j), 0, 0 + (radius * j), 1, 1, 0, 1f);

				NTMRenderHelper.addVertexColor(0 + (radius * j), 7, 0 - (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 - (radius * j), 7, 0 - (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 - (radius * j), 0, 0 - (radius * j), 1, 1, 0, 1f);
				NTMRenderHelper.addVertexColor(0 + (radius * j), 0, 0 - (radius * j), 1, 1, 0, 1f);
			}
			NTMRenderHelper.draw();
		}

		GlStateManager.disableBlend();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.ams_emitter);
	}

	@Override
	public ItemRenderBase getRenderer(Item item) {
		return new ItemRenderBase() {
			public void renderInventory() {
				GlStateManager.translate(0, -2, 0);
				GlStateManager.scale(1.5, 1.5, 1.5);
			}

			public void renderCommon(ItemStack itemStack) {
				GlStateManager.shadeModel(GL11.GL_SMOOTH);
				bindTexture(ResourceManager.ams_emitter_tex);
				ResourceManager.ams_emitter.renderAll();
				GlStateManager.shadeModel(GL11.GL_FLAT);
			}
		};
	}
}