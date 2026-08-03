package com.hbm.mixin;

import com.hbm.inventory.BackpackEquipmentInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the capability-backed player slot out of Creative's hotbar remapping. */
@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreativeBackpackSlot {

    private static final ResourceLocation INVENTORY_TAB_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/creative_inventory/tab_inventory.png");
    private static final ResourceLocation BACKPACK_SLOT_ICON = new ResourceLocation("hbm", "textures/gui/backpack_slot_icon.png");

    @Shadow
    private static int selectedTabIndex;

    @Inject(method = "setCurrentCreativeTab", at = @At("TAIL"))
    private void hbm$positionBackpackSlot(CreativeTabs tab, CallbackInfo ci) {
        if (tab != CreativeTabs.INVENTORY) return;

        Container container = ((MixinGuiContainerAccessor) (Object) this).hbm$getInventorySlots();
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory instanceof BackpackEquipmentInventory) {
                slot.xPos = 127;
                slot.yPos = 20;
                return;
            }
        }
    }

    @Inject(method = "drawGuiContainerBackgroundLayer", at = @At("TAIL"))
    private void hbm$drawBackpackSlot(float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        if (CreativeTabs.CREATIVE_TAB_ARRAY[selectedTabIndex] != CreativeTabs.INVENTORY) return;

        Container container = ((MixinGuiContainerAccessor) (Object) this).hbm$getInventorySlots();
        Slot backpackSlot = null;
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory instanceof BackpackEquipmentInventory) {
                backpackSlot = slot;
                break;
            }
        }
        if (backpackSlot == null) return;

        MixinGuiContainerAccessor gui = (MixinGuiContainerAccessor) (Object) this;
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(INVENTORY_TAB_TEXTURE);
        int x = gui.hbm$getGuiLeft() + 126;
        int y = gui.hbm$getGuiTop() + 19;
        Gui.drawModalRectWithCustomSizedTexture(x, y, 8, 53, 18, 18, 256, 256);
        if (!backpackSlot.getHasStack()) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            Minecraft.getMinecraft().getTextureManager().bindTexture(BACKPACK_SLOT_ICON);
            Gui.drawModalRectWithCustomSizedTexture(x + 1, y + 1, 0, 0, 16, 16, 16, 16);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
    }

}
