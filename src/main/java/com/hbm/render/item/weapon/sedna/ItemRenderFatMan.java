package com.hbm.render.item.weapon.sedna;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.sedna.HbmAnimationsSedna;
import com.hbm.render.util.RenderMiscEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
public class ItemRenderFatMan extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) {
		return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
	}

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress +
				(ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GlStateManager.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.5F * offset, -1.25F * offset, 0.5F * offset,
				-1F * offset, -1.25F * offset, 0F * offset);
	}

	protected static String label = "AUTO";

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.fatman_tex);
		double scale = 0.5D;
		GlStateManager.scale(scale, scale, scale);

		boolean isLoaded = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0;

		double[] equip = HbmAnimationsSedna.getRelevantTransformation("EQUIP");
		double[] lid = HbmAnimationsSedna.getRelevantTransformation("LID");
		double[] nuke = HbmAnimationsSedna.getRelevantTransformation("NUKE");
		double[] piston = HbmAnimationsSedna.getRelevantTransformation("PISTON");
		double[] handle = HbmAnimationsSedna.getRelevantTransformation("HANDLE");
		double[] gauge = HbmAnimationsSedna.getRelevantTransformation("GAUGE");

		GlStateManager.translate(0, 1, -2);
		GlStateManager.rotate((float) equip[0], 1, 0, 0);
		GlStateManager.translate(0, -1, 2);

		GlStateManager.shadeModel(GL11.GL_SMOOTH);

		ResourceManager.fatman.renderPart("Launcher");

		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 0, handle[2]);
		ResourceManager.fatman.renderPart("Handle");

		GlStateManager.translate(0.4375, -0.875, 0);
		GlStateManager.rotate((float) gauge[2], 0, 0, 1);
		GlStateManager.translate(-0.4375, 0.875, 0);
		ResourceManager.fatman.renderPart("Gauge");
		GlStateManager.popMatrix();

		GlStateManager.pushMatrix();
		GlStateManager.translate(0.25, 0.125, 0);
		GlStateManager.rotate((float) lid[2], 0, 0, 1);
		GlStateManager.translate(-0.25, -0.125, 0);
		ResourceManager.fatman.renderPart("Lid");
		GlStateManager.popMatrix();

		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 0, piston[2]);
		if(!isLoaded && piston[2] == 0) GlStateManager.translate(0, 0, 3);
		ResourceManager.fatman.renderPart("Piston");
		GlStateManager.popMatrix();

		if(isLoaded || nuke[0] != 0 || nuke[1] != 0 || nuke[2] != 0) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(nuke[0], nuke[1], nuke[2]);
			renderNuke(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getType(stack, null));
			GlStateManager.popMatrix();
		}

		GlStateManager.shadeModel(GL11.GL_FLAT);
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.5D;
		GlStateManager.scale(scale, scale, scale);
		GlStateManager.translate(-0.5, 0.5, -3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.375D;
		GlStateManager.scale(scale, scale, scale);
		GlStateManager.rotate(25F, 1, 0, 0);
		GlStateManager.rotate(45F, 0, 1, 0);
		GlStateManager.translate(0, -0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, Object type) {
		GlStateManager.enableLighting();

		Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.fatman_tex);

		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		ResourceManager.fatman.renderPart("Launcher");
		ResourceManager.fatman.renderPart("Handle");
		ResourceManager.fatman.renderPart("Gauge");
		ResourceManager.fatman.renderPart("Lid");
		ResourceManager.fatman.renderPart("Piston");
		Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.fatman_mininuke_tex);
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		if(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0) {
			ResourceManager.fatman.renderPart("MiniNuke");
		}
		GlStateManager.shadeModel(GL11.GL_FLAT);
	}

	public void renderNuke(Object type) {
		if(type == XFactoryCatapult.nuke_balefire) {
			renderBalefire(interp);
		} else {
			Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.fatman_mininuke_tex);
			ResourceManager.fatman.renderPart("MiniNuke");
		}
	}

	public static void renderBalefire(float interp) {

		Minecraft mc = Minecraft.getMinecraft();
		mc.renderEngine.bindTexture(ResourceManager.fatman_balefire_tex);
		ResourceManager.fatman.renderPart("MiniNuke");
		mc.renderEngine.bindTexture(RenderMiscEffects.glintBF);
		mc.entityRenderer.disableLightmap();

		float scale = 2F;
		float r = 0F;
		float g = 0.8F;
		float b = 0.15F;
		float speed = -6;
		float glintColor = 0.76F;
		int layers = 3;

		GlStateManager.pushMatrix();
		float offset = mc.player.ticksExisted + interp;
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.color(1F, 1F, 1F, 1F);
		GlStateManager.depthFunc(GL11.GL_EQUAL);
		GlStateManager.depthMask(false);
		GlStateManager.blendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);

		for (int k = 0; k < layers; ++k) {
			GlStateManager.color(r * glintColor, g * glintColor, b * glintColor, 1.0F);
			GlStateManager.matrixMode(GL11.GL_TEXTURE);
			GlStateManager.loadIdentity();

			float movement = offset * (0.001F + (float) k * 0.003F) * speed;

			GlStateManager.scale(scale, scale, scale);
			GlStateManager.rotate(30.0F - k * 60.0F, 0.0F, 0.0F, 1.0F);
			GlStateManager.translate(0F, movement, 0F);

			GlStateManager.matrixMode(GL11.GL_MODELVIEW);

			ResourceManager.fatman.renderPart("MiniNuke");
		}

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.matrixMode(GL11.GL_TEXTURE);
		GlStateManager.depthMask(true);
		GlStateManager.loadIdentity();
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.depthFunc(GL11.GL_LEQUAL);
		GlStateManager.popMatrix();

		mc.entityRenderer.enableLightmap();
	}
}

