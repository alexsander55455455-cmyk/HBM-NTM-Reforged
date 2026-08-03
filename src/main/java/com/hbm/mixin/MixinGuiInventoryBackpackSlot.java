package com.hbm.mixin;

import com.hbm.inventory.BackpackEquipmentSlot;
import com.hbm.inventory.BackpackEquipmentInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiInventory.class)
public abstract class MixinGuiInventoryBackpackSlot {

    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/inventory.png");
    private static final ResourceLocation BACKPACK_SLOT_ICON = new ResourceLocation("hbm", "textures/gui/backpack_slot_icon.png");

    @Inject(method = "drawGuiContainerBackgroundLayer", at = @At("TAIL"))
    private void hbm$drawBackpackSlot(float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        MixinGuiContainerAccessor gui = (MixinGuiContainerAccessor) (Object) this;
        int x = gui.hbm$getGuiLeft() + BackpackEquipmentSlot.PLAYER_INVENTORY_X - 1;
        int y = gui.hbm$getGuiTop() + BackpackEquipmentSlot.PLAYER_INVENTORY_Y - 1;
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(INVENTORY_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 7, 83, 18, 18, 256, 256);
        if (hbm$isBackpackSlotEmpty(gui)) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            Minecraft.getMinecraft().getTextureManager().bindTexture(BACKPACK_SLOT_ICON);
            Gui.drawModalRectWithCustomSizedTexture(x + 1, y + 1, 0, 0, 16, 16, 16, 16);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
    }

    private boolean hbm$isBackpackSlotEmpty(MixinGuiContainerAccessor gui) {
        Container container = gui.hbm$getInventorySlots();
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory instanceof BackpackEquipmentInventory) {
                return !slot.getHasStack();
            }
        }
        return false;
    }
}
