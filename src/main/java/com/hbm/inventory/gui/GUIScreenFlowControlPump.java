package com.hbm.inventory.gui;

import com.hbm.api.energymk2.IEnergyReceiverMK2.ConnectionPriority;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.network.TileEntityFlowControlPump;
import com.hbm.util.I18nUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GUIScreenFlowControlPump extends GuiScreen {
    private final TileEntityFlowControlPump pump;
    private GuiTextField throughput;
    private int pressure, priority, left, top;
    public GUIScreenFlowControlPump(TileEntityFlowControlPump pump) {
        this.pump = pump;
        pressure = pump.tank[0].getPressure();
        priority = pump.priority.ordinal();
    }
    @Override public void initGui() {
        Keyboard.enableRepeatEvents(true);
        left = (width - 304) / 2;
        top = (height - 110) / 2;
        String value = throughput == null ? Integer.toString(pump.bufferSize) : throughput.getText();
        throughput = new GuiTextField(0, fontRenderer, left + 8, top + 47, 80, 20);
        throughput.setMaxStringLength(5);
        throughput.setValidator(text -> text.matches("[0-9]*"));
        throughput.setText(value);
        buttonList.clear();
        buttonList.add(new GuiButton(0, left + 96, top + 47, 80, 20, pressure + " PU"));
        buttonList.add(new GuiButton(1, left + 184, top + 47, 112, 20, priorityName()));
        buttonList.add(new GuiButton(2, left + 104, top + 84, 96, 20, I18nUtil.resolveKey("gui.done")));
    }
    private String priorityName() { return I18nUtil.resolveKey("separator.priority." + priority); }
    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) { pressure = (pressure + 1) % 6; button.displayString = pressure + " PU"; }
        else if (button.id == 1) { priority = (priority + 1) % ConnectionPriority.VALUES.length; button.displayString = priorityName(); }
        else if (button.id == 2) mc.player.closeScreen();
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18nUtil.resolveKey("tile.flow_control_pump.name"), width / 2, top + 8, 0xffffff);
        drawString(fontRenderer, I18nUtil.resolveKey("pump.throughput"), left + 8, top + 34, 0xa0a0a0);
        drawString(fontRenderer, I18nUtil.resolveKey("pump.pressure"), left + 96, top + 34, 0xa0a0a0);
        drawString(fontRenderer, I18nUtil.resolveKey("separator.priority"), left + 184, top + 34, 0xa0a0a0);
        drawString(fontRenderer, I18nUtil.resolveKey("pump.maximum"), left + 8, top + 71, 0xa0a0a0);
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
        data.setByte("pressure", (byte) pressure);
        data.setByte("priority", (byte) priority);
        if (throughput != null && !throughput.getText().isEmpty()) data.setInteger("capacity", Integer.parseInt(throughput.getText()));
        PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, pump.getPos()));
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
