package com.hbm.inventory;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealityErrorBackpackInventoryTest {

    @BeforeAll
    static void registerVanillaItems() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void insertedStackBecomesLockedGlitchUntilManualSortRollsReward() {
        Item glitch = Item.getItemFromBlock(Blocks.BARRIER);
        ItemStack backpack = new ItemStack(Items.PAPER);
        IBackpackInventory inventory = RealityErrorBackpackInventory.createForTest(
                backpack,
                glitch,
                Collections.singletonList(Items.APPLE)
        );

        ItemStack remainder = inventory.insertItem(0, new ItemStack(Blocks.DIRT, 32), false);

        assertTrue(remainder.isEmpty());
        assertEquals(glitch, inventory.getStackInSlot(0).getItem());
        assertEquals(1L, inventory.getTrueSlotCount(0));
        assertTrue(inventory.extractItem(0, 64, false).isEmpty());

        inventory.sortContents();

        assertEquals(Items.APPLE, inventory.getStackInSlot(0).getItem());
        long rewardCount = inventory.getTrueSlotCount(0);
        assertTrue(rewardCount >= 1L && rewardCount <= 64L);

        ItemStack reward = inventory.extractItem(0, 64, false);
        assertEquals(Items.APPLE, reward.getItem());
        assertEquals(rewardCount, reward.getCount());
        assertEquals(0L, inventory.getTrueSlotCount(0));
    }

    @Test
    void ammoDoesNotExistForFeedersUntilManualSortMaterializesIt() {
        Item glitch = Item.getItemFromBlock(Blocks.BARRIER);
        ItemStack backpack = new ItemStack(Items.PAPER);
        IBackpackInventory inventory = RealityErrorBackpackInventory.createForTest(
                backpack,
                glitch,
                Collections.singletonList(Items.ARROW)
        );

        assertTrue(inventory.insertItemAnywhere(new ItemStack(Blocks.DIRT, 48), false).isEmpty());
        long[] arrows = {0L};
        inventory.forEachStoredStack((stack, count) -> {
            if (stack.getItem() == Items.ARROW) arrows[0] += count;
        });
        assertEquals(0L, arrows[0]);
        assertFalse(inventory.supportsAutoSorting());

        inventory.sortContents();
        inventory.forEachStoredStack((stack, count) -> {
            if (stack.getItem() == Items.ARROW) arrows[0] += count;
        });
        assertTrue(arrows[0] > 0L);
    }

    @Test
    void lockedOrRolledSlotCannotConsumeAnotherInput() {
        Item glitch = Item.getItemFromBlock(Blocks.BARRIER);
        ItemStack backpack = new ItemStack(Items.PAPER);
        IBackpackInventory inventory = RealityErrorBackpackInventory.createForTest(
                backpack,
                glitch,
                Collections.singletonList(Items.APPLE)
        );
        inventory.insertItem(0, new ItemStack(Blocks.DIRT), false);

        ItemStack lockedRemainder = inventory.insertItem(0, new ItemStack(Items.BONE, 5), false);
        assertEquals(5, lockedRemainder.getCount());

        inventory.sortContents();
        ItemStack rolledRemainder = inventory.insertItem(0, new ItemStack(Items.BONE, 5), false);
        assertEquals(5, rolledRemainder.getCount());
    }

    @Test
    void everyExpandedSlotLocksAndRollsIndependently() {
        Item glitch = Item.getItemFromBlock(Blocks.BARRIER);
        ItemStack backpack = new ItemStack(Items.PAPER);
        IBackpackInventory inventory = RealityErrorBackpackInventory.createForTest(
                backpack, 7, glitch, Collections.singletonList(Items.APPLE));

        for (int slot = 0; slot < 7; slot++) {
            assertTrue(inventory.insertItem(slot, new ItemStack(Blocks.DIRT, slot + 1), false).isEmpty());
        }
        assertEquals(7, inventory.getCapacity());
        assertEquals(7, inventory.getFilledSlotCount());

        inventory.sortContents();
        for (int slot = 0; slot < 7; slot++) {
            assertEquals(Items.APPLE, inventory.getStackInSlot(slot).getItem());
            assertTrue(inventory.getTrueSlotCount(slot) >= 1L);
        }
    }

    @Test
    void shrinkingExpandedStorageReturnsEveryDisappearingReward() {
        Item glitch = Item.getItemFromBlock(Blocks.BARRIER);
        ItemStack backpack = new ItemStack(Items.PAPER);
        IBackpackInventory inventory = RealityErrorBackpackInventory.createForTest(
                backpack, 7, glitch, Collections.singletonList(Items.APPLE));
        for (int slot = 0; slot < 7; slot++) {
            inventory.insertItem(slot, new ItemStack(Blocks.DIRT), false);
        }
        inventory.sortContents();

        List<ItemStack> overflow = RealityErrorBackpackInventory.takeOverflowItems(backpack, 2);

        assertEquals(5, overflow.size());
        assertEquals(2, RealityErrorBackpackInventory.countStoredValues(backpack));
        assertTrue(overflow.stream().allMatch(stack -> stack.getItem() == Items.APPLE));
    }
}
