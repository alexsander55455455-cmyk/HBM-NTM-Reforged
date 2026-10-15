package com.hbm.inventory.gui;

import com.hbm.api.energymk2.IEnergyReceiverMK2.ConnectionPriority;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.network.TileEntityNetworkSeparator;
import com.hbm.util.I18nUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GUIScreenNetworkSeparator extends GuiScreen {
    private final TileEntityNetworkSeparator separator;
    private GuiTextField throughput;
    private int priority;
    private int left;
    private int top;

    public GUIScreenNetworkSeparator(TileEntityNetworkSeparator separator) {
        this.separator = separator;
        priority = separator.priority.ordinal();
    }
    @Override public void initGui() {
        Keyboard.enableRepeatEvents(true);
        left = (width - 304) / 2;
        top = (height - 110) / 2;
        String value = throughput == null ? Long.toString(separator.configuredLimit) : throughput.getText();
        throughput = new GuiTextField(0, fontRenderer, left + 8, top + 47, 168, 20);
        throughput.setMaxStringLength(11);
        throughput.setValidator(text -> text.matches("[0-9]*"));
        throughput.setText(value);
        buttonList.clear();
        buttonList.add(new GuiButton(0, left + 184, top + 47, 112, 20, priorityName()));
        buttonList.add(new GuiButton(1, left + 104, top + 84, 96, 20, I18nUtil.resolveKey("gui.done")));
    }
    private String priorityName() { return I18nUtil.resolveKey("separator.priority." + priority); }
    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            priority = (priority + 1) % ConnectionPriority.VALUES.length;
            button.displayString = priorityName();
        } else if (button.id == 1) mc.player.closeScreen();
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18nUtil.resolveKey("tile.network_separator.name"), width / 2, top + 8, 0xffffff);
        drawString(fontRenderer, I18nUtil.resolveKey("separator.limit"), left + 8, top + 34, 0xa0a0a0);
        drawString(fontRenderer, I18nUtil.resolveKey("separator.priority"), left + 184, top + 34, 0xa0a0a0);
        drawString(fontRenderer, I18nUtil.resolveKey("separator.maximum"), left + 8, top + 71, 0xa0a0a0);
        throughput.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    @Override public void updateScreen() { throughput.updateCursorCounter(); }
    @Override protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (throughput.textboxKeyTyped(typedChar, keyCode)) return;
        if (keyCode == Keyboard.KEY_RETURN || keyCode == mc.gameSettings.keyBindInventory.getKeyCode()) mc.player.closeScreen();
        else super.keyTyped(typedChar, keyCode);
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        throughput.mouseClicked(x, y, button);
    }
    @Override public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        NBTTagCompound data = new NBTTagCompound();
        data.setByte("priority", (byte) priority);
        if (throughput != null && !throughput.getText().isEmpty()) data.setLong("limit", Long.parseLong(throughput.getText()));
        PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, separator.getPos()));
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
