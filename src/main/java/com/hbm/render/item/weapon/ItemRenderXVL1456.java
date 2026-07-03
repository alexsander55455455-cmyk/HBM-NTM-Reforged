package com.hbm.render.item.weapon;

import com.hbm.Tags;
import com.hbm.items.weapon.ItemGunGauss;
import com.hbm.main.ClientProxy;
import com.hbm.main.MainRegistry;
import com.hbm.main.ModEventHandlerClient;
import com.hbm.particle.ParticleFirstPerson;
import com.hbm.particle.ParticleFirstPerson.ParticleType;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.item.TEISRBase;
import com.hbm.render.model.BakedModelTransforms;
import com.hbm.render.model.ModelXVL1456;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ItemRenderXVL1456 extends TEISRBase {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/models/weapons/modelxvl1456.png");

	private final ModelXVL1456 model = new ModelXVL1456();

	@Override
	public ModelBinding createModelBinding(Item item) {
		return ModelBinding.inventoryWithGuiModel(item, BakedModelTransforms.defaultItemTransforms());
	}

	@Override
	public void renderByItem(ItemStack stack) {
		GL11.glPopMatrix();
		GlStateManager.enableCull();
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

		float drawSpin = 0.0F;
		if(entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) entity;
			drawSpin = player.getActiveItemStack().getItemUseAction() == EnumAction.BOW ? 0.05F : 0.0F;
			if(drawSpin == 0.05F && player.getHeldItemMainhand().getItem() == stack.getItem() && player.getHeldItemOffhand().getItem() == stack.getItem()) {
				drawSpin = 0.025F;
			}
		}

		switch(type) {
		case FIRST_PERSON_LEFT_HAND:
			GL11.glTranslated(50.5D, 0.0D, 8.4D);
		case FIRST_PERSON_RIGHT_HAND:
			EnumHand hand = type == TransformType.FIRST_PERSON_RIGHT_HAND ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", hand);
			double[] spinAnim = ItemGunGauss.getCharge(stack) > 0 ? HbmAnimations.getRelevantTransformation("SPIN", hand) : new double[] { 0.0D, 0.0D, 0.0D };

			GL11.glScaled(10.0D, 10.0D, 10.0D);
			GL11.glTranslated(-1.7D - recoil[2] * 0.3D - spinAnim[1], 0.4D, -0.8D);
			GL11.glRotated(182.0D, 1.0D, 0.0D, 0.0D);
			GL11.glRotated(-5.0D, 0.0D, 1.0D, 0.0D);
			GL11.glRotated(30.0D + recoil[1] * 0.2D, 0.0D, 0.0D, 1.0D);

			if(type == TransformType.FIRST_PERSON_LEFT_HAND) {
				GL11.glRotated(100.0D, 0.0D, 0.0D, 1.0D);
				GL11.glRotated(180.0D, 1.0D, 0.0D, 0.0D);
			}

			model.render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F,
					(float) spinAnim[0] * 0.000006F * (ItemGunGauss.getCharge(stack) + MainRegistry.proxy.partialTicks()));

			if(type == TransformType.FIRST_PERSON_RIGHT_HAND) {
				GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, ClientProxy.AUX_GL_BUFFER2);
				GL11.glLoadIdentity();
				GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, ClientProxy.AUX_GL_BUFFER);
				ClientProxy.AUX_GL_BUFFER.put(12, ClientProxy.AUX_GL_BUFFER2.get(12));
				ClientProxy.AUX_GL_BUFFER.put(13, ClientProxy.AUX_GL_BUFFER2.get(13));
				ClientProxy.AUX_GL_BUFFER.put(14, ClientProxy.AUX_GL_BUFFER2.get(14));
				GL11.glLoadMatrix(ClientProxy.AUX_GL_BUFFER2);

				for(ParticleFirstPerson particle : ModEventHandlerClient.firstPersonAuxParticles) {
					if(particle.getType() == ParticleType.TAU) {
						particle.renderParticle(Tessellator.getInstance().getBuffer(), entity, MainRegistry.proxy.partialTicks(), 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
					}
				}
			}
			break;
		case THIRD_PERSON_RIGHT_HAND:
		case THIRD_PERSON_LEFT_HAND:
		case HEAD:
		case FIXED:
		case GROUND:
			GL11.glTranslated(-0.25D, 0.1D, -0.4D);
			GL11.glRotated(90.0D, 0.0D, 1.0D, 0.0D);
			GL11.glRotated(180.0D, 0.0D, 0.0D, 1.0D);
			GL11.glScaled(0.75D, 0.75D, 0.75D);
			model.render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, drawSpin);
			break;
		case GUI:
			GL11.glTranslated(0.55D, 0.25D, 0.55D);
			GL11.glRotated(35.0D, 0.0D, 1.0D, 0.0D);
			GL11.glRotated(25.0D, 1.0D, 0.0D, 0.0D);
			GL11.glScaled(0.42D, 0.42D, 0.42D);
			model.render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, drawSpin);
			break;
		default:
			break;
		}
		GL11.glPushMatrix();
	}
}
