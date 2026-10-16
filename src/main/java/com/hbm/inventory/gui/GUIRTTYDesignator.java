package com.hbm.inventory.gui;

import com.hbm.Tags;
import com.hbm.items.tool.ItemRTTYDesignator;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTItemControlPacket;
import com.hbm.util.I18nUtil;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GUIRTTYDesignator extends GuiScreen {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/machine/gui_rtty_designator.png");
    private final String startingChannel;
    private final EnumHand hand;
    protected int xSize = 184;
    protected int ySize = 42;
    protected int guiLeft;
    protected int guiTop;
    protected GuiTextField chan;

    public GUIRTTYDesignator(ItemStack designator, EnumHand hand) {
        this.startingChannel = designator.hasTagCompound() ? designator.getTagCompound().getString(ItemRTTYDesignator.KEY_CHANNEL) : "";
        this.hand = hand;
    }

    @Override
    public void initGui() {
        super.initGui();
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
        Keyboard.enableRepeatEvents(true);
        chan = new GuiTextField(0, fontRenderer, guiLeft + 31, guiTop + 23, 82, 14);
        GUIScreenRBMKKeyPad.setupTextFieldStandard(chan, 15, startingChannel);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        drawDefaultBackground();
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        chan.drawTextBox();
        String title = I18nUtil.resolveKey("container.rttyDesignator");
        fontRenderer.drawString(title, guiLeft + (xSize - fontRenderer.getStringWidth(title)) / 2, guiTop + 6, 4210752);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        if (button == 0 && x >= guiLeft + 137 && x < guiLeft + 155 && y >= guiTop + 17 && y < guiTop + 35) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1));
            NBTTagCompound data = new NBTTagCompound();
            data.setString(ItemRTTYDesignator.KEY_CHANNEL, chan.getText());
            PacketDispatcher.wrapper.sendToServer(new NBTItemControlPacket(data, hand));
            return;
        }
        chan.mouseClicked(x, y, button);
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (chan.textboxKeyTyped(character, key)) return;
        if (key == Keyboard.KEY_ESCAPE || key == mc.gameSettings.keyBindInventory.getKeyCode()) {
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
        }
    }

    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
