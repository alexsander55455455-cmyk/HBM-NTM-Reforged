package com.hbm.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.function.BiConsumer;

/**
 * The storage behind one backpack item.  Normal backpacks keep it in item NBT;
 * special backpacks may use a different backend while the container stays the same.
 */
public interface IBackpackInventory extends IItemHandlerModifiable {

    int getCapacity();

    int getFilledSlotCount();

    ItemStack insertItemAnywhere(ItemStack stack, boolean simulate);

    boolean supportsAutoPickup();

    boolean isAutoPickupEnabled();

    void setAutoPickupEnabled(boolean enabled);

    default boolean supportsAutoSorting() {
        return false;
    }

    default boolean isAutoSortEnabled() {
        return false;
    }

    default void setAutoSortEnabled(boolean enabled) {
    }

    /**
     * Ordinary backpacks expose a one-shot sort action. Persistent automatic
     * sorting is a separate capability used only by automated backpacks.
     */
    default boolean supportsManualSorting() {
        return false;
    }

    void sortContents();

    /**
     * Sorts only one logical compartment. Implementations that cannot safely
     * sort a sub-range leave it unchanged.
     */
    default void sortContents(int fromInclusive, int toExclusive) {
        if (fromInclusive <= 0 && toExclusive >= getSlots()) {
            sortContents();
        }
    }

    default boolean isInfiniteStorage() {
        return false;
    }

    /**
     * True amount represented by one displayed slot. Ordinary backpacks simply
     * expose their vanilla stack count; virtual-count backends may return a long.
     */
    default long getTrueSlotCount(int slot) {
        ItemStack stack = getStackInSlot(slot);
        return stack.isEmpty() ? 0L : stack.getCount();
    }

    /** Visits occupied logical stacks without exposing their storage positions. */
    default void forEachStoredStack(BiConsumer<ItemStack, Long> visitor) {
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty()) visitor.accept(stack, Math.max((long) stack.getCount(), getTrueSlotCount(slot)));
        }
    }

    /**
     * Applies a server-synchronized true count to a client-side mirror.
     * Server and ordinary NBT-backed inventories do not need to implement it.
     */
    default void setClientTrueSlotCount(int slot, long count) {
    }

    /**
     * Replaces the client-side mirror of a remotely stored inventory. Ordinary
     * NBT-backed inventories do not keep such a mirror.
     */
    default void resetClientStorageMirror(int capacity) {
    }

    /**
     * Applies one trusted server-synchronized virtual slot to a client mirror.
     * This is intentionally separate from insertion validation: legacy storage
     * may contain items that are no longer legal to insert but must remain
     * visible and extractable.
     */
    default void applyClientSyncedVirtualSlot(int slot, ItemStack prototype, long count) {
        setStackInSlot(slot, prototype);
        setClientTrueSlotCount(slot, count);
    }

    /**
     * Virtual-count slots require atomic container clicks instead of vanilla's
     * mutable ItemStack arithmetic.
     */
    default boolean usesVirtualLongCounts() {
        return false;
    }

    /**
     * Keeps a client-side storage mirror when vanilla replaces the backpack
     * ItemStack with a freshly synchronized copy of the same logical item.
     */
    default boolean tryRebindClientBackpack(ItemStack backpack) {
        return false;
    }
}
