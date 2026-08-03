package com.hbm.api.ntl;

import com.hbm.uninos.networkproviders.PneumaticNetwork;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackCacheTest {

    static {
        Bootstrap.register();
    }

    @Test
    void cacheTracksConsumptionInsertionAndNetworkDestruction() {
        PneumaticNetwork network = new PneumaticNetwork();
        TestStorage storage = new TestStorage(network);
        storage.stacks[0] = new ItemStack(Items.STICK, 1);
        storage.amounts[0] = 10;
        StackCache cache = new StackCache(0, 0, 0);

        network.addStorage(storage);
        network.addStackCache(cache);
        storage.updateMonitors();

        assertEquals(10, cache.getSlotFromStack(new ItemStack(Items.STICK)).stacksize);
        assertEquals(4, cache.consumeItemsAndReturnQuantity(new ItemStack(Items.STICK), 4));
        storage.updateMonitors();
        assertEquals(6, cache.getSlotFromStack(new ItemStack(Items.STICK)).stacksize);

        assertEquals(0, cache.addItemsAndReturnQuantity(new ItemStack(Items.STICK), 60));
        storage.updateMonitors();
        assertEquals(66, cache.getSlotFromStack(new ItemStack(Items.STICK)).stacksize);

        network.destroy();
        assertTrue(cache.hasExpired);
        assertTrue(cache.cacheSlots.isEmpty());
    }

    private static final class TestStorage implements ISlotMonitorProvider {
        private final PneumaticNetwork network;
        private final SlotMonitor[] monitors = new SlotMonitor[2];
        private final ItemStack[] stacks = { ItemStack.EMPTY, ItemStack.EMPTY };
        private final long[] amounts = new long[2];

        private TestStorage(PneumaticNetwork network) {
            this.network = network;
            for (int i = 0; i < monitors.length; i++) monitors[i] = new SlotMonitor(i, this);
        }

        @Override public SlotMonitor[] getMonitors() { return monitors; }
        @Override public ItemStack getSlotAt(int index) { return stacks[index]; }
        @Override public long getAmountAt(int index) { return amounts[index]; }

        @Override
        public long useUpItem(int index, long amount) {
            long removed = Math.min(amounts[index], amount);
            amounts[index] -= removed;
            if (amounts[index] == 0) stacks[index] = ItemStack.EMPTY;
            return amount - removed;
        }

        @Override
        public long addItem(int index, long amount) {
            long added = Math.min(64 - amounts[index], amount);
            amounts[index] += added;
            return amount - added;
        }

        @Override
        public long setupType(int index, ItemStack zeroStack, long amount) {
            long added = Math.min(64, amount);
            stacks[index] = zeroStack.copy();
            stacks[index].setCount(1);
            amounts[index] = added;
            return amount - added;
        }

        @Override public boolean allowTypeSetting() { return true; }
        @Override public boolean isAvailableToCache(StackCache cache) { return true; }
        @Override public PneumaticNetwork getRelevantNetwork() { return network; }
    }
}
