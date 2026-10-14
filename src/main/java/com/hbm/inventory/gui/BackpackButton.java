package com.hbm.inventory.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

class BackpackButton extends GuiButton {
    BackpackButton(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        minecraft.getTextureManager().bindTexture(BUTTON_TEXTURES);
        GlStateManager.color(1F, 1F, 1F, 1F);
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        drawBackground(getHoverState(hovered));
        mouseDragged(minecraft, mouseX, mouseY);
        int color = packedFGColour != 0 ? packedFGColour : !enabled ? 0xA0A0A0 : hovered ? 0xFFFFA0 : 0xE0E0E0;
        drawCenteredString(minecraft.fontRenderer, displayString, x + width / 2, y + (height - 8) / 2, color);
    }

    protected void drawBackground(int state) {
        int leftWidth = width / 2;
        int rightWidth = width - leftWidth;
        int topHeight = height / 2;
        int bottomHeight = height - topHeight;
        int textureY = 46 + state * 20;
        drawTexturedModalRect(x, y, 0, textureY, leftWidth, topHeight);
        drawTexturedModalRect(x + leftWidth, y, 200 - rightWidth, textureY, rightWidth, topHeight);
        drawTexturedModalRect(x, y + topHeight, 0, textureY + 20 - bottomHeight, leftWidth, bottomHeight);
        drawTexturedModalRect(x + leftWidth, y + topHeight, 200 - rightWidth,
                textureY + 20 - bottomHeight, rightWidth, bottomHeight);
    }

    @Override
    public void drawCenteredString(FontRenderer font, String text, int x, int y, int color) {
        int textWidth = font.getStringWidth(text);
        float scale = Math.min(1F, (width - 6) / (float) Math.max(1, textWidth));
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0F);
        GlStateManager.scale(scale, scale, 1F);
        font.drawStringWithShadow(text, -textWidth / 2F, 0F, color);
        GlStateManager.popMatrix();
    }
}
