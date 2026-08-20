package com.hbm.inventory.slot;

import com.hbm.inventory.AssemblyMachineInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** A machine-only input slot that exposes the assembly inventory's real stack limit to the GUI. */
public class SlotAssemblyInput extends SlotNonRetarded {

    private final int handlerIndex;

    public SlotAssemblyInput(IItemHandler inventory, int id, int x, int y) {
        super(inventory, id, x, y);
        this.handlerIndex = id;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return !stack.isEmpty() && getItemHandler().isItemValid(handlerIndex, stack);
    }

    @Override
    public int getSlotStackLimit() {
        return AssemblyMachineInventory.INPUT_STACK_LIMIT;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return AssemblyMachineInventory.INPUT_STACK_LIMIT;
    }
}
