package com.hbm.api.ntl;

import com.hbm.uninos.networkproviders.PneumaticNetwork;
import net.minecraft.item.ItemStack;

public interface ISlotMonitorProvider {

    SlotMonitor[] getMonitors();

    ItemStack getSlotAt(int index);

    long getAmountAt(int index);

    long useUpItem(int index, long amount);

    long addItem(int index, long amount);

    long setupType(int index, ItemStack zeroStack, long amount);

    boolean allowTypeSetting();

    boolean isAvailableToCache(StackCache cache);

    PneumaticNetwork getRelevantNetwork();

    default void onNewCacheHasJoined(StackCache stackCache, PneumaticNetwork network) {
        for (SlotMonitor monitor : getMonitors()) {
            if (!stackCache.hasExpired && isAvailableToCache(stackCache)) {
                stackCache.addToCache(monitor);
            }
        }
    }

    default void updateMonitors() {
        for (SlotMonitor monitor : getMonitors()) {
            monitor.checkUpdate();
        }
    }
}
