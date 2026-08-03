package com.hbm.inventory;

import com.hbm.config.BackpackConfig;
import com.hbm.items.tool.ItemBackpack;
import com.hbm.items.tool.ItemPocketHoleBackpack;
import com.hbm.saveddata.PocketHoleBackpackSavedData;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Bridges the pocket-hole item to its expandable world-saved storage. The server
 * backend writes straight to the shared storage (no deferred "pending" state, so
 * every open container observes one authoritative value); the client backend only
 * mirrors the synchronized display stacks for prediction and rendering.
 */
public abstract class PocketHoleBackpackInventory implements IBackpackInventory {
    protected final ItemPocketHoleBackpack backpackItem;
    protected ItemStack backpack;

    private PocketHoleBackpackInventory(ItemPocketHoleBackpack backpackItem, ItemStack backpack) {
        this.backpackItem = backpackItem;
        this.backpack = backpack;
    }

    public static IBackpackInventory create(ItemPocketHoleBackpack backpackItem, ItemStack backpack, World world) {
        if (world != null && !world.isRemote) {
            return new Server(backpackItem, backpack, backpackItem.getServerStorage(world, backpack));
        }
        return new Client(backpackItem, backpack);
    }

    @Override
    public boolean supportsAutoPickup() {
        return BackpackUpgradeManager.supportsAutoPickup(backpack);
    }

    @Override
    public boolean isAutoPickupEnabled() {
        return BackpackUpgradeManager.isAutoPickupEnabled(backpack);
    }

    @Override
    public void setAutoPickupEnabled(boolean enabled) {
        BackpackUpgradeManager.setAutoPickupEnabled(backpack, enabled);
    }

    @Override
    public boolean supportsAutoSorting() {
        return BackpackUpgradeManager.supportsAutoSorting(backpack);
    }

    @Override
    public boolean isAutoSortEnabled() {
        return BackpackUpgradeManager.isAutoSortEnabled(backpack);
    }

    @Override
    public void setAutoSortEnabled(boolean enabled) {
        BackpackUpgradeManager.setAutoSortEnabled(backpack, enabled);
    }

    @Override
    public boolean supportsManualSorting() {
        return true;
    }

    @Override
    public void sortContents() {
    }

    @Override
    public boolean isInfiniteStorage() {
        return BackpackConfig.hasInfiniteSlots(backpack);
    }

    @Override
    public boolean usesVirtualLongCounts() {
        return true;
    }

    protected int getActiveCapacity() {
        return backpackItem.getStorageSlots(backpack);
    }

    private static final class Server extends PocketHoleBackpackInventory {
        private final PocketHoleBackpackSavedData.PocketHoleStorage storage;

        private Server(ItemPocketHoleBackpack backpackItem, ItemStack backpack,
                       PocketHoleBackpackSavedData.PocketHoleStorage storage) {
            super(backpackItem, backpack);
            this.storage = storage;
            syncSummary();
        }

        @Override
        public int getCapacity() {
            return getActiveCapacity();
        }

        @Override
        public int getFilledSlotCount() {
            return storage.getOccupiedSlots(getActiveCapacity());
        }

        @Override
        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            ItemStack remaining = storage.insertItemAnywhere(stack, simulate, getActiveCapacity());
            if (!simulate && remaining.getCount() != stack.getCount()) {
                if (isAutoSortEnabled()) storage.sortContents(getActiveCapacity());
                syncSummary();
            }
            return remaining;
        }

        @Override
        public int getSlots() {
            return getActiveCapacity();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot >= 0 && slot < getActiveCapacity() ? storage.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public long getTrueSlotCount(int slot) {
            return slot >= 0 && slot < getActiveCapacity() ? storage.getSlotCount(slot) : 0L;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            ItemStack remaining = storage.insertItem(
                    slot, stack, simulate, isAutoSortEnabled(), getActiveCapacity());
            if (!simulate && remaining.getCount() != stack.getCount()) {
                if (isAutoSortEnabled()) storage.sortContents(getActiveCapacity());
                syncSummary();
            }
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= getActiveCapacity()) return ItemStack.EMPTY;
            ItemStack extracted = storage.extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) {
                if (isAutoSortEnabled()) storage.sortContents(getActiveCapacity());
                syncSummary();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot >= 0 && slot < getActiveCapacity() ? storage.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !ItemBackpack.isForbiddenBackpackContent(stack)
                    && storage.isItemValid(slot, stack, isAutoSortEnabled(), getActiveCapacity());
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 0 || slot >= getActiveCapacity()) return;
            if (!stack.isEmpty() && ItemBackpack.isForbiddenBackpackContent(stack)) return;
            storage.setStackInSlot(slot, stack);
            if (isAutoSortEnabled()) storage.sortContents(getActiveCapacity());
            syncSummary();
        }

        @Override
        public void sortContents() {
            storage.sortContents(getActiveCapacity());
            syncSummary();
        }

        @Override
        public void setAutoSortEnabled(boolean enabled) {
            boolean wasEnabled = isAutoSortEnabled();
            super.setAutoSortEnabled(enabled);
            if (enabled && !wasEnabled) {
                storage.sortContents(getActiveCapacity());
                syncSummary();
            }
        }

        private void syncSummary() {
            backpackItem.setCachedSummary(backpack, storage);
        }
    }

    private static final class Client extends PocketHoleBackpackInventory {
        private final ItemStack[] prototypes = new ItemStack[PocketHoleBackpackSavedData.PocketHoleStorage.MAX_SLOTS];
        private final long[] counts = new long[PocketHoleBackpackSavedData.PocketHoleStorage.MAX_SLOTS];

        private Client(ItemPocketHoleBackpack backpackItem, ItemStack backpack) {
            super(backpackItem, backpack);
            java.util.Arrays.fill(prototypes, ItemStack.EMPTY);
            loadCachedSummary();
        }

        private void loadCachedSummary() {
            java.util.Arrays.fill(prototypes, ItemStack.EMPTY);
            java.util.Arrays.fill(counts, 0L);
            for (int slot = 0; slot < getActiveCapacity(); slot++) {
                ItemStack prototype = backpackItem.getCachedSlotPrototype(backpack, slot);
                long count = backpackItem.getCachedSlotCount(backpack, slot);
                if (prototype.isEmpty() || count <= 0L) continue;
                prototype.setCount(1);
                prototypes[slot] = prototype;
                counts[slot] = count;
            }
        }

        @Override
        public int getCapacity() {
            return getActiveCapacity();
        }

        @Override
        public int getFilledSlotCount() {
            int occupied = 0;
            for (int slot = 0; slot < getActiveCapacity(); slot++) {
                if (isOccupied(slot)) occupied++;
            }
            return occupied;
        }

        @Override
        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            int destination = findLargestMatchingSlotWithRoom(stack);
            if (destination < 0) destination = findFirstEmptySlot();
            return destination < 0 ? stack : insertItem(destination, stack, simulate);
        }

        @Override
        public int getSlots() {
            return getActiveCapacity();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return isOccupied(slot) ? copyWithCount(prototypes[slot], 1) : ItemStack.EMPTY;
        }

        @Override
        public long getTrueSlotCount(int slot) {
            return slot >= 0 && slot < getActiveCapacity() ? Math.max(0L, counts[slot]) : 0L;
        }

        @Override
        public void setClientTrueSlotCount(int slot, long count) {
            if (slot < 0 || slot >= getActiveCapacity()) return;
            counts[slot] = Math.max(0L, count);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= getActiveCapacity() || stack.isEmpty()
                    || ItemBackpack.isForbiddenBackpackContent(stack)) {
                return stack;
            }

            int destination = slot;
            if (isAutoSortEnabled()) {
                destination = findLargestMatchingSlotWithRoom(stack);
                if (destination < 0) destination = findFirstEmptySlot();
            } else if (isOccupied(destination)
                    && !PocketHoleBackpackSavedData.PocketHoleStorage.areSameType(prototypes[destination], stack)) {
                return stack;
            }
            if (destination < 0) return stack;

            long room = Long.MAX_VALUE - counts[destination];
            int moved = (int) Math.min((long) stack.getCount(), room);
            if (moved <= 0) return stack;
            if (!simulate) {
                if (!isOccupied(destination)) {
                    ItemStack prototype = stack.copy();
                    prototype.setCount(1);
                    prototypes[destination] = prototype;
                }
                counts[destination] += moved;
            }
            return copyWithCount(stack, stack.getCount() - moved);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isOccupied(slot) || amount <= 0) return ItemStack.EMPTY;
            int limit = Math.max(1, prototypes[slot].getMaxStackSize());
            int extractedCount = (int) Math.min((long) Math.min(amount, limit), counts[slot]);
            ItemStack extracted = copyWithCount(prototypes[slot], extractedCount);
            if (!simulate) {
                counts[slot] -= extractedCount;
                if (counts[slot] <= 0L) clearSlot(slot);
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot >= 0 && slot < getActiveCapacity() ? 64 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < 0 || slot >= getActiveCapacity() || stack.isEmpty()
                    || ItemBackpack.isForbiddenBackpackContent(stack)) {
                return false;
            }
            if (isAutoSortEnabled()) {
                return findLargestMatchingSlotWithRoom(stack) >= 0 || findFirstEmptySlot() >= 0;
            }
            return !isOccupied(slot)
                    || PocketHoleBackpackSavedData.PocketHoleStorage.areSameType(prototypes[slot], stack)
                    && counts[slot] < Long.MAX_VALUE;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 0 || slot >= getActiveCapacity()) return;
            if (stack.isEmpty()) {
                clearSlot(slot);
                return;
            }

            ItemStack prototype = stack.copy();
            prototype.setCount(1);
            boolean samePrototype = !prototypes[slot].isEmpty()
                    && PocketHoleBackpackSavedData.PocketHoleStorage.areSameType(prototypes[slot], prototype);
            prototypes[slot] = prototype;
            if (!samePrototype || counts[slot] <= 0L) counts[slot] = 1L;
        }

        private boolean isOccupied(int slot) {
            return slot >= 0 && slot < getActiveCapacity()
                    && counts[slot] > 0L && !prototypes[slot].isEmpty();
        }

        private int findLargestMatchingSlotWithRoom(ItemStack stack) {
            int bestSlot = -1;
            long bestCount = -1L;
            for (int slot = 0; slot < getActiveCapacity(); slot++) {
                if (isOccupied(slot) && counts[slot] < Long.MAX_VALUE && counts[slot] > bestCount
                        && PocketHoleBackpackSavedData.PocketHoleStorage.areSameType(prototypes[slot], stack)) {
                    bestSlot = slot;
                    bestCount = counts[slot];
                }
            }
            return bestSlot;
        }

        private int findFirstEmptySlot() {
            for (int slot = 0; slot < getActiveCapacity(); slot++) {
                if (!isOccupied(slot)) return slot;
            }
            return -1;
        }

        private void clearSlot(int slot) {
            prototypes[slot] = ItemStack.EMPTY;
            counts[slot] = 0L;
        }

        @Override
        public boolean tryRebindClientBackpack(ItemStack current) {
            if (current.isEmpty() || current.getItem() != backpackItem) return false;
            UUID previousId = backpackItem.getStorageId(backpack);
            UUID currentId = backpackItem.getStorageId(current);
            if (previousId == null || !previousId.equals(currentId)) return false;
            backpack = current;
            loadCachedSummary();
            return true;
        }
    }

    private static ItemStack copyWithCount(ItemStack stack, int count) {
        if (count <= 0 || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }
}
