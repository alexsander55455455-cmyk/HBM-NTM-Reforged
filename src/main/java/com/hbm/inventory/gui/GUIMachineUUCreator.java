package com.hbm.inventory.gui;

import com.hbm.Tags;
import com.hbm.inventory.container.ContainerMachineUUCreator;
import com.hbm.lib.Library;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.AuxButtonPacket;
import com.hbm.tileentity.machine.TileEntityMachineUUCreator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import java.io.IOException;

import static com.hbm.util.SoundUtil.playClickSound;

public class GUIMachineUUCreator extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(Tags.MODID + ":textures/gui/machine/gui_uu_creator.png");
	private final TileEntityMachineUUCreator uuCreator;

	public GUIMachineUUCreator(InventoryPlayer invPlayer, TileEntityMachineUUCreator tedf) {
		super(new ContainerMachineUUCreator(invPlayer, tedf));
		uuCreator = tedf;
		this.xSize = 176;
		this.ySize = 186;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		uuCreator.tank.renderTankInfo(this, mouseX, mouseY, guiLeft + 142, guiTop + 22, 16, 60);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 19, guiTop + 22, 16, 60, uuCreator.power, TileEntityMachineUUCreator.maxPower);
		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void mouseClicked(int x, int y, int i) throws IOException {
		super.mouseClicked(x, y, i);

		if(guiLeft + 79 <= x && guiLeft + 79 + 18 > x && guiTop + 60 < y && guiTop + 60 + 18 >= y) {
			playClickSound();
			PacketDispatcher.wrapper.sendToServer(new AuxButtonPacket(uuCreator.getPos(), 0, 0));
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = uuCreator.hasCustomName() ? uuCreator.getName() : I18n.format(uuCreator.getName());
		this.fontRenderer.drawString("Produced UU", 56, 26, 0xE700FF);

		String producedmb;
		if(uuCreator.producedmb * 20 > 1000)
			producedmb = Library.getShortNumber((long)(uuCreator.producedmb * 20)) + " mB/s";
		else
			producedmb = (long)(uuCreator.producedmb * 20) + " mB/s";

		this.fontRenderer.drawString(producedmb, 123 - this.fontRenderer.getStringWidth(producedmb), 40, 0xE700FF);
		this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawDefaultBackground();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(uuCreator.isOn)
			drawTexturedModalRect(guiLeft + 79, guiTop + 60, 176, 60, 18, 18);

		int powerGauge = (int) uuCreator.getPowerScaled(60);
		drawTexturedModalRect(guiLeft + 19, guiTop + 83 - powerGauge, 176, 60 - powerGauge, 16, powerGauge);
		uuCreator.tank.renderTank(guiLeft + 141, guiTop + 81, this.zLevel, 16, 60);
	}
}