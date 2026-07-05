package com.hbm.render.item;

import com.hbm.Tags;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.render.NTMRenderHelper;
import com.hbm.render.model.BakedModelTransforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

public class ItemRenderFFFluidDuct extends TEISRBase {

	private static final double HALF_A_PIXEL = 0.03125;
	private static final double PIX = 0.0625;

	@Override
	public ModelBinding createModelBinding(Item item) {
		return ModelBinding.inventoryWithGuiModel(item, BakedModelTransforms.defaultItemTransforms(), new ResourceLocation(Tags.MODID, "items/duct"));
	}

	@Override
	public boolean useRegistryPerspective(Item item) {
		return true;
	}

	@Override
	public void renderByItem(ItemStack stack) {
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
		NTMRenderHelper.bindBlockTexture();

		Tessellator tes = Tessellator.getInstance();
		BufferBuilder buf = tes.getBuffer();
		GL11.glPushMatrix();
		GL11.glTranslated(0.5, 0.5, 0.5);
		if (itemModel != null) {
			Minecraft.getMinecraft().getRenderItem().renderItem(stack, itemModel);
		}
		GL11.glPopMatrix();

		Fluid fluid = getFluid(stack);
		if (fluid != null) {
			TextureAtlasSprite sprite = getFluidSprite(fluid);
			if (sprite != null) {
				NTMRenderHelper.setColor(fluid.getColor(new FluidStack(fluid, 1000)));
				GlStateManager.disableLighting();

				float scroll = sprite.getFrameCount() > 1 ? 0.0F : getFlowScroll();
				float minU = sprite.getInterpolatedU(3) + scroll;
				float maxU = sprite.getInterpolatedU(13) + scroll;
				float minV = sprite.getInterpolatedV(7);
				float maxV = sprite.getInterpolatedV(9);

				GL11.glTranslated(0, 0, 0.5 + HALF_A_PIXEL);
				buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
				buf.pos(3 * PIX, 7 * PIX, 0).tex(minU, minV).endVertex();
				buf.pos(13 * PIX, 7 * PIX, 0).tex(maxU, minV).endVertex();
				buf.pos(13 * PIX, 9 * PIX, 0).tex(maxU, maxV).endVertex();
				buf.pos(3 * PIX, 9 * PIX, 0).tex(minU, maxV).endVertex();

				buf.pos(13 * PIX, 7 * PIX, -PIX).tex(maxU, minV).endVertex();
				buf.pos(3 * PIX, 7 * PIX, -PIX).tex(minU, minV).endVertex();
				buf.pos(3 * PIX, 9 * PIX, -PIX).tex(minU, maxV).endVertex();
				buf.pos(13 * PIX, 9 * PIX, -PIX).tex(maxU, maxV).endVertex();
				tes.draw();
				GlStateManager.enableLighting();
			}
		}
		GL11.glPopAttrib();
		GL11.glPopMatrix();
		super.renderByItem(stack);
	}

	private static float getFlowScroll() {
		Minecraft mc = Minecraft.getMinecraft();
		long tick = mc.world != null ? mc.world.getTotalWorldTime() : System.currentTimeMillis() / 50L;
		return (tick % 40L) / 640.0F;
	}

	private static TextureAtlasSprite getFluidSprite(Fluid fluid) {
		TextureAtlasSprite flowing = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluid.getFlowing().toString());
		if (flowing.getFrameCount() > 1) {
			return flowing;
		}
		return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluid.getStill().toString());
	}

	private static Fluid getFluid(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		FluidType type = Fluids.fromID(stack.getMetadata());
		if (type == null || type.hasNoID()) {
			return null;
		}
		return type.getFF();
	}
}