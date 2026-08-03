package com.hbm.api.ntl;

import com.hbm.uninos.networkproviders.PneumaticNetwork;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class StackCache {

    public final int x;
    public final int y;
    public final int z;
    public boolean hasExpired;

    public final LinkedHashMap<Long, CacheSlot> cacheSlots = new LinkedHashMap<>();
    private PneumaticNetwork network;

    public StackCache(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PneumaticNetwork getNetwork() {
        return network;
    }

    public void attachTo(PneumaticNetwork newNetwork) {
        if (network == newNetwork) return;
        clearSlots();
        if (network != null) network.accessors.remove(this);
        network = newNetwork;
        hasExpired = false;
    }

    public void addToCache(SlotMonitor monitor) {
        long identity = getStackIdentity(monitor.item, monitor.meta, monitor.nbt);
        CacheSlot slot = cacheSlots.computeIfAbsent(identity, _ -> new CacheSlot(monitor.toZeroStack()));
        slot.addMonitor(monitor);
    }

    public CacheSlot getSlotFromStack(ItemStack stack) {
        return cacheSlots.get(getStackIdentity(stack));
    }

    public CacheSlot getSlotFromStack(Item item, int meta, NBTTagCompound nbt) {
        return cacheSlots.get(getStackIdentity(item, meta, nbt));
    }

    public long consumeItemsAndReturnQuantity(ItemStack stack, long amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) return 0;
        CacheSlot slot = cacheSlots.get(getStackIdentity(stack));
        if (slot == null) return 0;

        long remaining = amount;
        for (SlotMonitor monitor : new LinkedHashSet<>(slot.monitors)) {
            ItemStack original = monitor.parent.getSlotAt(monitor.index);
            if (getStackIdentity(original) != getStackIdentity(stack)) continue;
            remaining = monitor.parent.useUpItem(monitor.index, remaining);
            if (remaining <= 0) break;
        }
        return amount - remaining;
    }

    public long addItemsAndReturnQuantity(ItemStack stack, long amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) return amount;
        long identity = getStackIdentity(stack);
        CacheSlot slot = cacheSlots.get(identity);

        if (slot != null) {
            for (SlotMonitor monitor : new LinkedHashSet<>(slot.monitors)) {
                if (getStackIdentity(monitor.parent.getSlotAt(monitor.index)) != identity) continue;
                amount = monitor.parent.addItem(monitor.index, amount);
                if (amount <= 0) return 0;
            }
        }

        CacheSlot emptySlots = cacheSlots.get(getNullIdentity());
        if (emptySlots != null) {
            for (SlotMonitor monitor : new LinkedHashSet<>(emptySlots.monitors)) {
                if (!monitor.parent.allowTypeSetting()) continue;
                ItemStack current = monitor.parent.getSlotAt(monitor.index);
                if (current != null && !current.isEmpty()) continue;
                amount = monitor.parent.setupType(monitor.index, stack, amount);
                if (amount <= 0) return 0;
            }
        }
        return amount;
    }

    public void dissolveCache() {
        clearSlots();
        if (network != null) network.accessors.remove(this);
        network = null;
        hasExpired = true;
    }

    private void clearSlots() {
        for (CacheSlot slot : cacheSlots.values()) slot.destroy();
        cacheSlots.clear();
    }

    public final class CacheSlot {
        @Nullable public final ItemStack displayStack;
        public final int itemId;
        public final int meta;
        public final NBTTagCompound nbt;
        public long stacksize;
        public final LinkedHashSet<SlotMonitor> monitors = new LinkedHashSet<>();

        private CacheSlot(ItemStack stack) {
            if (stack != null && !stack.isEmpty()) {
                displayStack = stack.copy();
                displayStack.setCount(1);
                itemId = Item.getIdFromItem(stack.getItem());
                meta = stack.getMetadata();
                nbt = stack.hasTagCompound() ? stack.getTagCompound().copy() : null;
            } else {
                displayStack = null;
                itemId = 0;
                meta = 0;
                nbt = null;
            }
        }

        public void addMonitor(SlotMonitor monitor) {
            if (monitors.add(monitor)) {
                monitor.viewedBy.add(this);
                changeAmounts(monitor.stacksize);
            }
        }

        public void removeMonitor(SlotMonitor monitor) {
            if (monitors.remove(monitor)) changeAmounts(-monitor.stacksize);
            if (monitors.isEmpty()) cacheSlots.remove(getStackIdentity(itemId, meta, nbt), this);
        }

        public void destroy() {
            for (SlotMonitor monitor : monitors) monitor.viewedBy.remove(this);
            monitors.clear();
            stacksize = 0;
        }

        public void changeAmounts(long delta) {
            stacksize = Math.max(0, stacksize + delta);
        }

        public StackCache getStackCache() {
            return StackCache.this;
        }
    }

    public static long getNullIdentity() {
        return 0;
    }

    public static long getStackIdentity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return getNullIdentity();
        return getStackIdentity(stack.getItem(), stack.getMetadata(), stack.getTagCompound());
    }

    public static long getStackIdentity(Item item, int meta, NBTTagCompound nbt) {
        if (item == null) return getNullIdentity();
        return getStackIdentity(Item.getIdFromItem(item), meta, nbt);
    }

    public static long getStackIdentity(int itemId, int meta, NBTTagCompound nbt) {
        long identity = itemId * 27_644_437L;
        identity = (identity + meta) * 27_644_437L;
        if (nbt != null) identity += nbt.hashCode();
        return identity;
    }
}
