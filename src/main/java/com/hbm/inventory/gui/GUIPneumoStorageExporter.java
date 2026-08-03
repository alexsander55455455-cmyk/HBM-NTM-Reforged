package com.hbm.inventory.gui;

import com.hbm.Tags;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.container.ContainerPneumoStorageExporter;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.network.TileEntityPneumoStorageExporter;
import com.hbm.util.I18nUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class GUIPneumoStorageExporter extends GuiInfoContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/storage/gui_pneumatic_exporter.png");
    private final TileEntityPneumoStorageExporter exporter;

    public GUIPneumoStorageExporter(InventoryPlayer player, TileEntityPneumoStorageExporter exporter) {
        super(new ContainerPneumoStorageExporter(player, exporter));
        this.exporter = exporter;
        this.xSize = 176;
        this.ySize = 185;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 142, guiTop + 16, 18, 18, mouseX, mouseY,
                new String[] { "Request mode: " + TextFormatting.YELLOW + (exporter.continuousRequest ? "Continuous" : "By request") });
        String requestType = exporter.requestMode == TileEntityPneumoStorageExporter.MODE_AS_MUCH_AS_POSSIBLE ? "As much as possible"
                : exporter.requestMode == TileEntityPneumoStorageExporter.MODE_FULL_STACK ? "Only full stacks" : "Only full requests";
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 142, guiTop + 34, 18, 18, mouseX, mouseY,
                new String[] { "Request type: " + TextFormatting.YELLOW + requestType });

        if (exporter.rorConfiguredMode) {
            String[] label = new String[10];
            label[0] = "Filter type: " + TextFormatting.YELLOW + "RoR configured";
            for (int i = 0; i < 9; i++) {
                int id = Short.toUnsignedInt(exporter.rorFilters[i][0]);
                int meta = Short.toUnsignedInt(exporter.rorFilters[i][1]);
                int amount = Short.toUnsignedInt(exporter.rorFilters[i][2]);
                label[i + 1] = "Slot " + (i + 1) + ": " + (id == 0 || amount == 0 ? "None" : "Item #" + id + " with Meta " + meta + " x" + amount);
            }
            drawCustomInfoStat(mouseX, mouseY, guiLeft + 142, guiTop + 52, 18, 18, mouseX, mouseY, label);
        } else {
            drawCustomInfoStat(mouseX, mouseY, guiLeft + 142, guiTop + 52, 18, 18, mouseX, mouseY,
                    new String[] { "Filter type: " + TextFormatting.YELLOW + "Manually configured" });
        }
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        click(mouseX, mouseY, 142, 16, "continuous");
        click(mouseX, mouseY, 142, 34, "request");
        click(mouseX, mouseY, 142, 52, "ror");
    }

    private void click(int mouseX, int mouseY, int left, int top, String flag) {
        if (!checkClick(mouseX, mouseY, left, top, 18, 18)) return;
        mc.getSoundHandler().playSound(PositionedSoundRecord.getRecord(SoundEvents.UI_BUTTON_CLICK, 1F, 1F));
        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean(flag, true);
        PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, exporter.getPos()));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = I18nUtil.resolveKey(exporter.getName());
        fontRenderer.drawString(name, xSize / 2 - fontRenderer.getStringWidth(name) / 2, 5, 4210752);
        fontRenderer.drawString(I18nUtil.resolveKey("container.inventory"), 8, ySize - 94, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        if (exporter.rorConfiguredMode) {
            drawTexturedModalRect(guiLeft + 142, guiTop + 52, xSize, 18, 18, 18);
            drawTexturedModalRect(guiLeft + 14, guiTop + 14, 77, 14, 58, 58);
        }
        if (!exporter.continuousRequest) drawTexturedModalRect(guiLeft + 142, guiTop + 16, xSize, 0, 18, 18);
        if (exporter.requestMode == TileEntityPneumoStorageExporter.MODE_FULL_STACK)
            drawTexturedModalRect(guiLeft + 142, guiTop + 34, xSize + 18, 0, 18, 18);
        if (exporter.requestMode == TileEntityPneumoStorageExporter.MODE_FULL_REQUEST)
            drawTexturedModalRect(guiLeft + 142, guiTop + 34, xSize + 18, 18, 18, 18);
    }
}
