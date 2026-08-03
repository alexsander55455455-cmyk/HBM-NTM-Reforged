package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotPattern;
import com.hbm.tileentity.network.TileEntityPneumoStorageExporter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ContainerPneumoStorageExporter extends ContainerBase {

    private final TileEntityPneumoStorageExporter exporter;

    public ContainerPneumoStorageExporter(InventoryPlayer player, TileEntityPneumoStorageExporter exporter) {
        super(player, exporter.getCheckedInventory());
        this.exporter = exporter;
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new SlotPattern(exporter.getCheckedInventory(), i, 17 + i % 3 * 18, 17 + i / 3 * 18, true));
        }
        addTakeOnlySlots(exporter.getCheckedInventory(), 9, 80, 17, 3, 3);
        playerInv(player, 8, 103);
    }

    @Override
    public @NotNull ItemStack slotClick(int index, int button, @NotNull ClickType type, @NotNull EntityPlayer player) {
        if (index < 0 || index >= 9) return super.slotClick(index, button, type, player);
        Slot slot = getSlot(index);
        ItemStack previous = slot.getHasStack() ? slot.getStack().copy() : ItemStack.EMPTY;
        ItemStack held = player.inventory.getItemStack();
        slot.putStack(held.isEmpty() ? ItemStack.EMPTY : held.copy());
        return previous;
    }

    @Override public boolean canInteractWith(EntityPlayer player) { return exporter.isUseableByPlayer(player); }
}
