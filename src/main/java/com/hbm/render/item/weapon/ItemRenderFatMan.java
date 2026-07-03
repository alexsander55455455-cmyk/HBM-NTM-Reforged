package com.hbm.render.item.weapon;

import com.hbm.Tags;
import com.hbm.render.item.TEISRBase;
import com.hbm.render.model.BakedModelTransforms;
import com.hbm.render.model.ModelFatman;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ItemRenderFatMan extends TEISRBase {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/models/weapons/fatmanlauncher.png");

	private final ModelFatman model = new ModelFatman();

	@Override
	public ModelBinding createModelBinding(Item item) {
		return ModelBinding.inventoryWithGuiModel(item, BakedModelTransforms.defaultItemTransforms(),
				new ResourceLocation(Tags.MODID, "items/gun_fatman"));
	}

	@Override
	public void renderByItem(ItemStack stack) {
		GL11.glPopMatrix();
		GlStateManager.enableCull();
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

		switch(type) {
		case FIRST_PERSON_LEFT_HAND:
			GL11.glTranslated(0.0D, 0.0D, -0.2D);
		case FIRST_PERSON_RIGHT_HAND:
			GL11.glTranslated(0.0D, -0.2D, 0.2D);
			GL11.glRotated(180.0D, 1.0D, 0.0D, 0.0D);
			GL11.glRotated(40.0D, 0.0D, 0.0D, 1.0D);
			if(type == TransformType.FIRST_PERSON_LEFT_HAND) {
				GL11.glRotated(100.0D, 0.0D, 0.0D, 1.0D);
				GL11.glRotated(180.0D, 1.0D, 0.0D, 0.0D);
			}
			model.render((Entity) entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, stack);
			break;
		case GUI:
		case GROUND:
		case FIXED:
		case HEAD:
		case THIRD_PERSON_RIGHT_HAND:
		case THIRD_PERSON_LEFT_HAND:
			GL11.glTranslated(-0.1D, -0.1D, 0.6D);
			GL11.glRotated(90.0D, 0.0D, 1.0D, 0.0D);
			GL11.glRotated(180.0D, 0.0D, 0.0D, 1.0D);
			GL11.glScaled(1.5D, 1.5D, 1.5D);
			model.render((Entity) entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, stack);
			break;
		default:
			break;
		}
		GL11.glPushMatrix();
	}
}
