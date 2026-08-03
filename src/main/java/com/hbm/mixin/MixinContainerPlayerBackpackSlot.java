package com.hbm.mixin;

import com.hbm.inventory.BackpackEquipmentSlot;
import com.hbm.handler.BackpackHandler;
import com.hbm.items.tool.ItemBackpack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerPlayer.class)
public abstract class MixinContainerPlayerBackpackSlot extends Container {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hbm$addBackpackSlot(InventoryPlayer inventory, boolean localWorld, EntityPlayer player, CallbackInfo ci) {
        ((MixinContainerSlotAdder) (Object) this).hbm$addSlotToContainer(new BackpackEquipmentSlot(player, BackpackEquipmentSlot.PLAYER_INVENTORY_X, BackpackEquipmentSlot.PLAYER_INVENTORY_Y));
    }

    @Inject(method = "transferStackInSlot", at = @At("HEAD"), cancellable = true)
    private void hbm$shiftBackpack(EntityPlayer player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < 0 || index >= inventorySlots.size()) return;
        int equipmentIndex = hbm$findBackpackEquipmentSlot();
        if (equipmentIndex < 0) return;

        Slot sourceSlot = inventorySlots.get(index);
        if (sourceSlot == null || !sourceSlot.getHasStack()) return;
        ItemStack source = sourceSlot.getStack();
        boolean sourceIsEquipment = index == equipmentIndex;
        boolean sourceIsPlayerBackpack = sourceSlot.inventory == player.inventory
                && sourceSlot.getSlotIndex() >= 0 && sourceSlot.getSlotIndex() < 36
                && source.getItem() instanceof ItemBackpack;
        if (!sourceIsEquipment && !sourceIsPlayerBackpack) return;

        ItemStack original = source.copy();
        boolean moved;
        if (sourceIsEquipment) {
            int[] playerRange = hbm$findPlayerMainRange(player.inventory);
            moved = playerRange[0] >= 0
                    && mergeItemStack(source, playerRange[0], playerRange[1], false);
        } else {
            Slot target = inventorySlots.get(equipmentIndex);
            moved = !target.getHasStack()
                    && mergeItemStack(source, equipmentIndex, equipmentIndex + 1, false);
        }

        if (!moved) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (source.isEmpty()) sourceSlot.putStack(ItemStack.EMPTY);
        else sourceSlot.onSlotChanged();
        sourceSlot.onTake(player, source);
        if (!player.world.isRemote) BackpackHandler.syncEquipmentState(player);
        cir.setReturnValue(original);
    }

    private int hbm$findBackpackEquipmentSlot() {
        for (int index = 0; index < inventorySlots.size(); index++) {
            if (inventorySlots.get(index) instanceof BackpackEquipmentSlot) return index;
        }
        return -1;
    }

    private int[] hbm$findPlayerMainRange(InventoryPlayer playerInventory) {
        int first = -1;
        int last = -1;
        for (int index = 0; index < inventorySlots.size(); index++) {
            Slot slot = inventorySlots.get(index);
            if (slot.inventory == playerInventory && slot.getSlotIndex() >= 0 && slot.getSlotIndex() < 36) {
                if (first < 0) first = index;
                last = index + 1;
            }
        }
        return new int[] {first, last};
    }
}
