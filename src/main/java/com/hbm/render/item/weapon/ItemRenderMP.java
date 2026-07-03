package com.hbm.render.item.weapon;

import com.hbm.Tags;
import com.hbm.render.item.TEISRBase;
import com.hbm.render.model.BakedModelTransforms;
import com.hbm.render.model.ModelMP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ItemRenderMP extends TEISRBase {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/models/weapons/modelmp.png");

	private final ModelMP model = new ModelMP();

	@Override
	public ModelBinding createModelBinding(Item item) {
		return ModelBinding.inventoryWithGuiModel(item, BakedModelTransforms.defaultItemTransforms(),
				new ResourceLocation(Tags.MODID, "items/gun_pm"));
	}

	@Override
	public void renderByItem(ItemStack stack) {
		GlStateManager.enableCull();
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

		switch(type) {
		case FIRST_PERSON_LEFT_HAND:
			GL11.glTranslated(0.0D, 0.0D, 0.0D);
		case FIRST_PERSON_RIGHT_HAND:
			GL11.glScaled(0.5D, 0.5D, 0.5D);
			GL11.glTranslated(1.0D, 1.1D, 1.3D);
			if(type == TransformType.FIRST_PERSON_RIGHT_HAND) {
				GL11.glRotated(10.0D, 0.0D, 1.0D, 0.0D);
				GL11.glRotated(-50.0D, 0.0D, 0.0D, 1.0D);
				GL11.glRotated(190.0D, 1.0D, 0.0D, 0.0D);
			} else {
				GL11.glRotated(180.0D, 0.0D, 1.0D, 0.0D);
				GL11.glRotated(180.0D, 1.0D, 0.0D, 0.0D);
				GL11.glRotated(50.0D, 0.0D, 0.0D, 1.0D);
			}
			model.render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
			break;
		case THIRD_PERSON_LEFT_HAND:
		case THIRD_PERSON_RIGHT_HAND:
		case HEAD:
		case FIXED:
		case GROUND:
			GL11.glScaled(0.75D, 0.75D, 0.75D);
			if(type == TransformType.THIRD_PERSON_LEFT_HAND) {
				GL11.glTranslated(0.1D, 0.0D, 0.0D);
			}
			GL11.glTranslated(0.6D, 0.7D, 0.67D);
			GL11.glRotated(-90.0D, 0.0D, 1.0D, 0.0D);
			GL11.glRotated(180.0D, 1.0D, 0.0D, 0.0D);
			model.render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
			break;
		default:
			break;
		}
	}
}
