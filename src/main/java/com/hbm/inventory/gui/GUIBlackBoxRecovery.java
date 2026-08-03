package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerBlackBoxRecovery;
import com.hbm.tileentity.machine.TileEntityBlackBoxRecovery;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public final class GUIBlackBoxRecovery extends GuiContainer {

    private static final ResourceLocation INVENTORY_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/inventory.png");

    public GUIBlackBoxRecovery(InventoryPlayer playerInventory, TileEntityBlackBoxRecovery recovery) {
        super(new ContainerBlackBoxRecovery(playerInventory, recovery));
        xSize = 176;
        ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("container.hbm_black_box_recovery");
        fontRenderer.drawString(title, xSize / 2 - fontRenderer.getStringWidth(title) / 2, 7, 0xE0E0E0);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 72, 0xC0C0C0);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();
        drawPanel(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(INVENTORY_TEXTURE);

        drawSlot(guiLeft + 80, guiTop + 35);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(guiLeft + 8 + column * 18, guiTop + 84 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(guiLeft + 8 + column * 18, guiTop + 142);
        }
    }

    private void drawPanel(int left, int top, int right, int bottom) {
        drawRect(left, top, right, bottom, 0xF0101010);
        drawRect(left + 3, top + 3, right - 3, bottom - 3, 0xF02E2E2E);
    }

    private void drawSlot(int x, int y) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(INVENTORY_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(x - 1, y - 1, 7, 83, 18, 18, 256, 256);
        drawRect(x - 1, y - 1, x + 17, y + 17, 0x44000000);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
    }
}
