package com.hbm.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.function.IntPredicate;

/** Allows assembly-machine input slots to hold recipe-sized stacks of normally non-stackable items. */
public class AssemblyMachineInventory extends ItemStackHandler {

    public static final int INPUT_STACK_LIMIT = 64;

    private final IntPredicate inputSlot;

    public AssemblyMachineInventory(int size, IntPredicate inputSlot) {
        super(size);
        this.inputSlot = inputSlot;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        if(inputSlot.test(slot)) return Math.min(INPUT_STACK_LIMIT, getSlotLimit(slot));
        return super.getStackLimit(slot, stack);
    }
}
