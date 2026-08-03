package com.hbm.inventory.gui;

import com.hbm.Tags;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.container.ContainerPneumoStorageMono;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.render.util.GaugeUtil;
import com.hbm.tileentity.network.TileEntityPneumoStorageMono;
import com.hbm.tileentity.network.TileEntityPneumoTube;
import com.hbm.util.I18nUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.Locale;

public class GUIPneumoStorageMono extends GuiInfoContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/storage/gui_pneumatic_mono.png");
    private final TileEntityPneumoStorageMono storage;

    public GUIPneumoStorageMono(InventoryPlayer player, TileEntityPneumoStorageMono storage) {
        super(new ContainerPneumoStorageMono(player, storage));
        this.storage = storage;
        this.xSize = 200;
        this.ySize = 181;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 174, guiTop + 36, 20, 8, mouseX, mouseY,
                new String[] { "Compressor: " + storage.compair.getPressure() + " PU",
                        "Max range: " + TileEntityPneumoTube.getRangeFromPressure(storage.compair.getPressure()) + "m" });
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (checkClick(mouseX, mouseY, 174, 36, 20, 8)) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getRecord(SoundEvents.UI_BUTTON_CLICK, 1F, 1F));
            NBTTagCompound data = new NBTTagCompound();
            data.setBoolean("pressure", true);
            PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, storage.getPos()));
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = I18nUtil.resolveKey(storage.getName());
        fontRenderer.drawString(name, 88 - fontRenderer.getStringWidth(name) / 2, 5, 4210752);
        fontRenderer.drawString(I18nUtil.resolveKey("container.inventory"), 8, ySize - 94, 4210752);
        for (int i = 0; i < 3; i++) {
            if (!storage.inventory.getStackInSlot(i).isEmpty()) {
                int amount = storage.amounts[i];
                String percent = " (" + ((int) (amount * 1000D / TileEntityPneumoStorageMono.CAPACITY) / 10D) + "%)";
                fontRenderer.drawString(String.format(Locale.US, "%,d", amount) + percent, 50, 22 + i * 18, 0);
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        for (int i = 0; i < 3; i++) {
            if (!storage.inventory.getStackInSlot(i).isEmpty()) {
                int bar = storage.amounts[i] * 124 / TileEntityPneumoStorageMono.CAPACITY;
                drawTexturedModalRect(guiLeft + 44, guiTop + 17 + i * 18, 0, 181, bar, 16);
            }
        }
        drawTexturedModalRect(guiLeft + 174 + 4 * (storage.compair.getPressure() - 1), guiTop + 36, 200, 0, 4, 8);
        GaugeUtil.drawSmoothGauge(guiLeft + 184, guiTop + 25, zLevel,
                (double) storage.compair.getFill() / storage.compair.getMaxFill(), 5, 2, 1, 0xCA6C43, 0xAB4223);
    }
}
