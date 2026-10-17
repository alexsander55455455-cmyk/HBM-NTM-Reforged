package com.hbm.inventory.gui;

import com.hbm.Tags;
import com.hbm.inventory.container.ContainerTapeDrive;
import com.hbm.tileentity.machine.TileEntityMachineTapeDrive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUITapeDrive extends GuiInfoContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/machine/gui_tape_drive.png");
    private final TileEntityMachineTapeDrive tapeDrive;
    public GUITapeDrive(InventoryPlayer player, TileEntityMachineTapeDrive drive) {
        super(new ContainerTapeDrive(player, drive.getCheckedInventory()));
        tapeDrive = drive;
        xSize = 176;
        ySize = 186;
    }
    @Override protected void drawGuiContainerForegroundLayer(int x, int y) {
        String name = tapeDrive.hasCustomName() ? tapeDrive.getName() : I18n.format(tapeDrive.getName());
        fontRenderer.drawString(name, xSize / 2 - fontRenderer.getStringWidth(name) / 2, 6, 0xffffff);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float ticks, int x, int y) {
        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
