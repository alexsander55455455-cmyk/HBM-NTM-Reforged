package com.hbm.render.item.weapon;

import org.lwjgl.opengl.GL11;

import com.hbm.items.weapon.ItemGunBase;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.item.TEISRBase;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public class ItemRenderWeaponSauer extends TEISRBase {

	@Override
	public ModelBinding createModelBinding(Item item) {
		return bindingFullTeisr(item);
	}

	@Override
	public boolean useRegistryPerspective(Item item) {
		return true;
	}

	@Override
	public void renderByItem(ItemStack itemStackIn) {
		GL11.glPushMatrix();
		GlStateManager.enableCull();
		GlStateManager.shadeModel(GL11.GL_SMOOTH);

		Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.sauer_tex);

		boolean aiming = ItemGunBase.isAimingForRender(itemStackIn);

		switch(type) {

		case FIRST_PERSON_LEFT_HAND:
		case FIRST_PERSON_RIGHT_HAND:
			EnumHand hand = type == TransformType.FIRST_PERSON_LEFT_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;

			double[] recoil = HbmAnimations.getRelevantTransformation("SAUER_RECOIL", hand);
			double[] tilt = HbmAnimations.getRelevantTransformation("SAUER_TILT", hand);
			double[] cock = HbmAnimations.getRelevantTransformation("SAUER_COCK", hand);
			double[] eject = HbmAnimations.getRelevantTransformation("SAUER_SHELL_EJECT", hand);

			if(type == TransformType.FIRST_PERSON_RIGHT_HAND) {
				GL11.glTranslated(0.8, -1, 0);
				if(aiming) {
					GL11.glTranslated(-0.5, 0.1, -0.53);
					GL11.glRotated(7, 0, 1, 0);
					GL11.glRotated(4, 1, 0, 0);
				}
				GL11.glRotated(90, 0, 1, 0);
				GL11.glRotated(20, 1, 0, 0);
				GL11.glRotated(-10, 0, 1, 0);
			} else {
				GL11.glTranslated(0.2, -1, 0);
				GL11.glRotated(-75, 0, 1, 0);
				GL11.glRotated(22, 1, 0, 0);
				GL11.glRotated(-10, 0, 1, 0);
			}

			if(recoil != null)
				GL11.glTranslated(0, 0, recoil[0]);

			if(aiming && type == TransformType.FIRST_PERSON_RIGHT_HAND) {
				GL11.glRotatef(-3F, 0.0F, 0.0F, 1.0F);
				GL11.glRotatef(2F, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(3F, 1.0F, 0.0F, 0.0F);
				GL11.glTranslatef(-2.0F, 0.5F, 0.3F);
			}

			if(tilt != null) {
				GL11.glTranslated(0, -5, 0);
				GL11.glRotated(tilt[2] * -0.5, 1, 0, 0);
				GL11.glTranslated(0, 5, 0);
				GL11.glRotated(tilt[0], 0, 0, 1);

				GL11.glTranslated(0, 0, cock[0] * 2);
				ResourceManager.sauer.renderPart("Lever");
				GL11.glTranslated(0, 0, -cock[0] * 2);

				GL11.glTranslated(eject[2] * 10, -eject[2], 0);
				GL11.glRotated(eject[2] * 90, -1, 0, 0);
				ResourceManager.sauer.renderPart("Shell");
				GL11.glRotated(eject[2] * 90, 1, 0, 0);
				GL11.glTranslated(-eject[2] * 10, eject[2], 0);

			} else {
				ResourceManager.sauer.renderPart("Lever");
			}

			break;
		case FIXED:
		case THIRD_PERSON_LEFT_HAND:
		case THIRD_PERSON_RIGHT_HAND:
		case HEAD:
		case GROUND:
			GL11.glTranslated(0.5, 0, 0.7);
			if(type == TransformType.FIXED) {
				GL11.glRotated(90, 0, 1, 0);
			}
			GL11.glScaled(0.4, 0.4, 0.4);
			ResourceManager.sauer.renderPart("Lever");
			break;

		case GUI:
			GlStateManager.enableLighting();
			GL11.glScaled(0.16, 0.16, 0.16);
			GL11.glTranslatef(3.6F, 2.1F, 0.0F);
			GL11.glRotatef(90F, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(40F, 1.0F, 0.0F, 0.0F);
			ResourceManager.sauer.renderPart("Lever");
			break;

		default:
			break;
		}

		ResourceManager.sauer.renderPart("Gun");

		GlStateManager.shadeModel(GL11.GL_FLAT);
		GL11.glPopMatrix();
	}
}