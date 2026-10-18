package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerLaunchpadLambda;
import com.hbm.Tags;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.machine.TileEntityLaunchpadLambda;
import com.hbm.util.I18nUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GUILaunchpadLambda extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(Tags.MODID + ":textures/gui/machine/gui_launchpad_lambda.png");
	private TileEntityLaunchpadLambda launcher;
	
	public GUILaunchpadLambda(InventoryPlayer invPlayer, TileEntityLaunchpadLambda tedf) {
		super(new ContainerLaunchpadLambda(invPlayer, tedf));
		launcher = tedf;
		
		this.xSize = 176;
		this.ySize = 226;
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		launcher.tanks[0].renderTankInfo(this, mouseX, mouseY, guiLeft + 107, guiTop + 26, 16, 52);
		launcher.tanks[1].renderTankInfo(this, mouseX, mouseY, guiLeft + 125, guiTop + 26, 16, 52);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 89, guiTop + 26, 16, 52, launcher.power, launcher.maxPower);

		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 34, guiTop + 43, 18, 18, mouseX, mouseY, I18nUtil.resolveKeyArray("desc.gui.lambda.manual"));
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 52, guiTop + 43, 18, 18, mouseX, mouseY, I18nUtil.resolveKeyArray("desc.gui.lambda.auto"));
	}

	@Override
	protected void mouseClicked(int x, int y, int i) throws java.io.IOException {
		super.mouseClicked(x, y, i);
		
		NBTTagCompound data = null;

		if(this.checkClick(x, y, 34, 43, 18, 18)) {
			data = new NBTTagCompound();
			data.setBoolean("auto", false);
		}

		if(this.checkClick(x, y, 52, 43, 18, 18)) {
			data = new NBTTagCompound();
			data.setBoolean("auto", true);
		}

		if(this.checkClick(x, y, 43, 70, 18, 18) && launcher.erected) {
			data = new NBTTagCompound();
			data.setBoolean("launch", true);
		}
		
		if(data != null) {
			this.mc.player.playSound(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1F, 1F);
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, launcher.getPos()));
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer( int i, int j) {
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
		
		if(launcher.erected && launcher.countdown > 0) {
			
			int countdown = launcher.countdown;
			
			String secs = "" + countdown / 20;
			String cents = "" + (countdown % 20) * 5;
			if(secs.length() == 1) secs = "0" + secs;
			if(cents.length() == 1) cents += "0";
			
			float scale = 1;
			GL11.glScalef(scale, scale, 1);
			this.fontRenderer.drawString(secs + ":" + cents, (int)(40 / scale), (int)(103 / scale), 0xff0000);
			GL11.glScalef(1 / scale, 1 / scale, 1);
			
		} else if(!launcher.hasRocketLoaded()) {
			drawStatus("desc.gui.soyuz.idle", 0xff0000);
		} else if(!launcher.erected) {
			drawStatus("desc.gui.soyuz.loading", 0xff8000);
		} else {
			drawStatus("desc.gui.soyuz.ready", 0x00ff00);
		}
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float interp, int x, int y) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		
		int power = (int) (launcher.power * 52 / launcher.maxPower);
		drawTexturedModalRect(guiLeft + 89, guiTop + 78 - power, 194, 52 - power, 16, power);
		
		drawTexturedModalRect(guiLeft + 34 + (launcher.autolaunch ? 18 : 0), guiTop + 43, 210 + (launcher.autolaunch ? 18 : 0), 26, 18, 18);
		
		drawTexturedModalRect(guiLeft + 112, guiTop + 13, launcher.hasJetFuel() ? 210 : 216, 0, 6, 8);
		drawTexturedModalRect(guiLeft + 130, guiTop + 13, launcher.hasOxidizer() ? 210 : 216, 0, 6, 8);
		drawTexturedModalRect(guiLeft + 94, guiTop + 13, launcher.power >= launcher.CONSUMPTION ? 210 : 216, 0, 6, 8);
		
		if(launcher.countdown > 0)
			drawTexturedModalRect(guiLeft + 43, guiTop + 70, 210, 44, 18, 18);
		
		if(!launcher.hasRocketLoaded())
			drawTexturedModalRect(guiLeft + 34, guiTop + 16, 210, 8, 18, 18);
		else
			drawTexturedModalRect(guiLeft + 34, guiTop + 16, 228, 8, 18, 18);
		
		if(!TileEntityLaunchpadLambda.acceptsPayload(launcher.inventory.getStackInSlot(1)))
			drawTexturedModalRect(guiLeft + 52, guiTop + 16, 210, 8, 18, 18);
		else
			drawTexturedModalRect(guiLeft + 52, guiTop + 16, 228, 8, 18, 18);
		
		launcher.tanks[0].renderTank(guiLeft + 107, guiTop + 78, this.zLevel, 16, 52);
		launcher.tanks[1].renderTank(guiLeft + 125, guiTop + 78, this.zLevel, 16, 52);
	}
    private void drawStatus(String key, int color) {
        String label = I18nUtil.resolveKey(key);
        float scale = Math.min(1F, 22F / fontRenderer.getStringWidth(label));
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1);
        fontRenderer.drawString(label, (int)(53 / scale - fontRenderer.getStringWidth(label) / 2F), (int)(107 / scale - fontRenderer.FONT_HEIGHT / 2F), color);
        GL11.glPopMatrix();
    }
}
