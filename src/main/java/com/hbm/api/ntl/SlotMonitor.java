package com.hbm.api.ntl;

import com.hbm.api.ntl.StackCache.CacheSlot;
import com.hbm.uninos.networkproviders.PneumaticNetwork;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class SlotMonitor {

    public final int index;
    public final ISlotMonitorProvider parent;

    public final LinkedHashSet<CacheSlot> viewedBy = new LinkedHashSet<>();

    @Nullable public Item item;
    public long stacksize;
    public int meta;
    public NBTTagCompound nbt;

    private boolean hasAvailabilityChanged = true;
    private boolean forceTypeUpdate = true;

    public SlotMonitor(int index, ISlotMonitorProvider parent) {
        this.index = index;
        this.parent = parent;
    }

    public ItemStack toZeroStack() {
        if (item == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item, 1, meta);
        if (nbt != null) stack.setTagCompound(nbt.copy());
        return stack;
    }

    public void availabilityHasChanged() {
        this.hasAvailabilityChanged = true;
    }

    public void detachFromAllCaches() {
        Iterator<CacheSlot> iterator = viewedBy.iterator();
        while (iterator.hasNext()) {
            CacheSlot slot = iterator.next();
            slot.removeMonitor(this);
            iterator.remove();
        }
    }

    public void detachFromNetwork(PneumaticNetwork network) {
        Iterator<CacheSlot> iterator = viewedBy.iterator();
        while (iterator.hasNext()) {
            CacheSlot slot = iterator.next();
            if (slot.getStackCache().getNetwork() == network) {
                slot.removeMonitor(this);
                iterator.remove();
            }
        }
    }

    public void checkUpdate() {
        PneumaticNetwork network = parent.getRelevantNetwork();

        if (hasAvailabilityChanged) {
            if (network != null) {
                for (StackCache cache : network.accessors) {
                    if (!cache.hasExpired && parent.isAvailableToCache(cache)) {
                        cache.addToCache(this);
                    }
                }
            }

            Iterator<CacheSlot> iterator = viewedBy.iterator();
            while (iterator.hasNext()) {
                CacheSlot slot = iterator.next();
                StackCache cache = slot.getStackCache();
                if (cache.hasExpired || cache.getNetwork() != network || !parent.isAvailableToCache(cache)) {
                    slot.removeMonitor(this);
                    iterator.remove();
                }
            }
            hasAvailabilityChanged = false;
        }

        ItemStack stack = parent.getSlotAt(index);
        long amount = parent.getAmountAt(index);
        if (stack == null || stack.isEmpty()) stack = ItemStack.EMPTY;

        boolean typeChanged;
        if (stack.isEmpty() || item == null) {
            typeChanged = stack.isEmpty() != (item == null);
        } else {
            typeChanged = item != stack.getItem() || meta != stack.getMetadata();
            if (!typeChanged) {
                NBTTagCompound tag = stack.getTagCompound();
                typeChanged = nbt == null ? tag != null : tag == null || !nbt.equals(tag);
            }
        }

        if (typeChanged || forceTypeUpdate) {
            detachFromAllCaches();

            if (stack.isEmpty()) {
                item = null;
                stacksize = 0;
                meta = 0;
                nbt = null;
            } else {
                item = stack.getItem();
                stacksize = amount;
                meta = stack.getMetadata();
                nbt = stack.hasTagCompound() ? stack.getTagCompound().copy() : null;
            }

            if (network != null) {
                for (StackCache cache : network.accessors) {
                    if (!cache.hasExpired && parent.isAvailableToCache(cache)) {
                        cache.addToCache(this);
                    }
                }
            }
            forceTypeUpdate = false;
            return;
        }

        if (stacksize != amount) {
            long delta = amount - stacksize;
            for (CacheSlot slot : viewedBy) slot.changeAmounts(delta);
            stacksize = amount;
        }
    }
}
