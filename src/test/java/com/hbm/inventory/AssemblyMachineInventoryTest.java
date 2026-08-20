package com.hbm.inventory;

import com.hbm.inventory.slot.SlotAssemblyInput;
import com.hbm.util.InventoryUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssemblyMachineInventoryTest {

    @Test
    void storesNormallyNonStackableItemsInAssemblyInputsOnly() {
        Item nonStackable = new Item().setMaxStackSize(1);
        AssemblyMachineInventory inventory = new AssemblyMachineInventory(2, slot -> slot == 0);

        ItemStack remainder = inventory.insertItem(0, new ItemStack(nonStackable, 20), false);

        assertTrue(remainder.isEmpty());
        assertEquals(20, inventory.getStackInSlot(0).getCount());

        remainder = inventory.insertItem(1, new ItemStack(nonStackable, 20), false);
        assertEquals(1, inventory.getStackInSlot(1).getCount());
        assertEquals(19, remainder.getCount());
    }

    @Test
    void shiftTransferCombinesNonStackableItemsInAssemblyInput() {
        Item nonStackable = new Item().setMaxStackSize(1);
        AssemblyMachineInventory inventory = new AssemblyMachineInventory(1, slot -> true);
        inventory.setStackInSlot(0, new ItemStack(nonStackable));
        SlotAssemblyInput slot = new SlotAssemblyInput(inventory, 0, 0, 0);
        ItemStack moving = new ItemStack(nonStackable);

        assertTrue(InventoryUtil.mergeItemStack(Collections.singletonList(slot), moving, 0, 1, false));
        assertTrue(moving.isEmpty());
        assertEquals(2, inventory.getStackInSlot(0).getCount());
    }
}
