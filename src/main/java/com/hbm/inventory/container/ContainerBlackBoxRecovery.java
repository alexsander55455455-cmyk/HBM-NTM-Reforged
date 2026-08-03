package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotTakeOnly;
import com.hbm.tileentity.machine.TileEntityBlackBoxRecovery;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerBlackBoxRecovery extends Container {

    private static final int RECOVERY_SLOT_COUNT = 1;
    private final TileEntityBlackBoxRecovery recovery;

    public ContainerBlackBoxRecovery(InventoryPlayer playerInventory, TileEntityBlackBoxRecovery recovery) {
        this.recovery = recovery;

        addSlotToContainer(new SlotTakeOnly(recovery.getInventoryForContainer(), 0, 80, 35));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return recovery.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size() || index >= RECOVERY_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stored = slot.getStack();
        ItemStack original = stored.copy();
        if (!mergeItemStack(stored, RECOVERY_SLOT_COUNT, inventorySlots.size(), true)) {
            return ItemStack.EMPTY;
        }

        if (stored.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        slot.onTake(player, stored);
        return original;
    }

    @Override
    public boolean canDragIntoSlot(Slot slot) {
        return slot.slotNumber != 0 && super.canDragIntoSlot(slot);
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId == 0 && clickType == ClickType.CLONE) {
            return ItemStack.EMPTY;
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        recovery.removeIfEmpty();
    }
}
