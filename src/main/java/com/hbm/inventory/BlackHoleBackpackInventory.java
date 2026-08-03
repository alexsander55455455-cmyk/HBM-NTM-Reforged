package com.hbm.inventory;

import com.hbm.config.BackpackConfig;
import com.hbm.items.tool.ItemBackpack;
import com.hbm.saveddata.BlackHoleBackpackSavedData;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Bridges the black-hole item to its world-saved storage.  The client keeps
 * only the currently synchronized container stacks; it never receives the
 * full storage NBT.
 */
public abstract class BlackHoleBackpackInventory implements IBackpackInventory {
    protected final ItemBackpack backpackItem;
    protected ItemStack backpack;

    private BlackHoleBackpackInventory(ItemBackpack backpackItem, ItemStack backpack) {
        this.backpackItem = backpackItem;
        this.backpack = backpack;
    }

    public static IBackpackInventory create(ItemBackpack backpackItem, ItemStack backpack, World world) {
        if (world != null && !world.isRemote) {
            return new Server(backpackItem, backpack, BackpackVirtualStorage.getStorage(world, backpack));
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
    public boolean isInfiniteStorage() {
        return BackpackConfig.hasInfiniteSlots(backpack);
    }

    @Override
    public boolean usesVirtualLongCounts() {
        return true;
    }

    private static final class Server extends BlackHoleBackpackInventory {
        private final BlackHoleBackpackSavedData.BackpackStorage storage;

        private Server(ItemBackpack backpackItem, ItemStack backpack, BlackHoleBackpackSavedData.BackpackStorage storage) {
            super(backpackItem, backpack);
            this.storage = storage;
            if (isAutoSortEnabled()) {
                storage.ensureCompactInvariant(BackpackConfig.allowsOverstack(backpack));
            }
            syncFilledCount();
        }

        @Override
        public int getCapacity() {
            return backpackItem.getStorageSlots(backpack);
        }

        @Override
        public int getFilledSlotCount() {
            return storage.getFilledSlotCount();
        }

        @Override
        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            ItemStack remaining = storage.insertStackAnywhere(
                    stack, simulate, isAutoSortEnabled(), getCapacity(),
                    BackpackConfig.allowsOverstack(backpack));
            if (!simulate && remaining.getCount() != stack.getCount()) syncFilledCount();
            return remaining;
        }

        @Override
        public int getSlots() {
            return getCapacity();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot >= 0 && slot < getCapacity() ? storage.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public long getTrueSlotCount(int slot) {
            return slot >= 0 && slot < getCapacity() ? storage.getSlotCount(slot) : 0L;
        }

        @Override
        public void forEachStoredStack(BiConsumer<ItemStack, Long> visitor) {
            storage.forEachStoredStack(visitor);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            ItemStack remaining = storage.insertItem(
                    slot, stack, simulate, isAutoSortEnabled(), getCapacity(),
                    BackpackConfig.allowsOverstack(backpack));
            if (!simulate && remaining.getCount() != stack.getCount()) syncFilledCount();
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= getCapacity()) return ItemStack.EMPTY;
            ItemStack extracted = storage.extractItem(slot, amount, simulate, isAutoSortEnabled(),
                    BackpackConfig.allowsOverstack(backpack));
            if (!simulate && !extracted.isEmpty()) syncFilledCount();
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < 0 || slot >= getCapacity()) return 0;
            return BackpackConfig.allowsOverstack(backpack)
                    ? Integer.MAX_VALUE : Math.max(1, getStackInSlot(slot).isEmpty()
                    ? 64 : getStackInSlot(slot).getMaxStackSize());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !ItemBackpack.isForbiddenBackpackContent(stack)
                    && storage.isItemValid(slot, stack, isAutoSortEnabled(), getCapacity(),
                    BackpackConfig.allowsOverstack(backpack));
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            // Virtual long-count storage is mutated only through atomic
            // insertItem/extractItem operations. Treating a display proxy as
            // the complete slot value can duplicate or truncate its true count.
        }

        @Override
        public void sortContents() {
            storage.compactAndMergeStable(BackpackConfig.allowsOverstack(backpack));
            syncFilledCount();
        }

        @Override
        public void setAutoSortEnabled(boolean enabled) {
            boolean wasEnabled = isAutoSortEnabled();
            super.setAutoSortEnabled(enabled);
            if (enabled && !wasEnabled) {
                storage.compactAndMergeStable(BackpackConfig.allowsOverstack(backpack));
                syncFilledCount();
            }
        }

        private void syncFilledCount() {
            BackpackVirtualStorage.updateSummary(backpack, storage);
        }
    }

    private static final class Client extends BlackHoleBackpackInventory {
        private final Map<Integer, ItemStack> prototypes = new TreeMap<>();
        private final Map<Integer, Long> counts = new TreeMap<>();
        private int capacity;

        private Client(ItemBackpack backpackItem, ItemStack backpack) {
            super(backpackItem, backpack);
            capacity = backpackItem.getStorageSlots(backpack);
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        public int getFilledSlotCount() {
            return backpackItem.getFilledSlotCount(backpack);
        }

        @Override
        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            int destination = findMatchingSlotWithRoom(stack);
            if (destination < 0) destination = findFirstEmptySlot();
            return insertItem(destination, stack, simulate);
        }

        @Override
        public int getSlots() {
            return capacity;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (!isOccupied(slot)) return ItemStack.EMPTY;
            return copyWithCount(prototypes.get(slot), 1);
        }

        @Override
        public long getTrueSlotCount(int slot) {
            if (slot < 0) return 0L;
            return Math.max(0L, counts.getOrDefault(slot, 0L));
        }

        @Override
        public void setClientTrueSlotCount(int slot, long count) {
            if (slot < 0 || slot >= capacity) return;
            if (count <= 0L) {
                clearSlot(slot);
            } else {
                counts.put(slot, count);
            }
        }

        @Override
        public void resetClientStorageMirror(int synchronizedCapacity) {
            prototypes.clear();
            counts.clear();
            capacity = Math.max(1, Math.min(BlackHoleBackpackSavedData.BackpackStorage.MAX_LOGICAL_SLOTS,
                    synchronizedCapacity));
        }

        @Override
        public void applyClientSyncedVirtualSlot(int slot, ItemStack prototype, long count) {
            if (slot < 0 || slot >= capacity) return;
            if (prototype == null || prototype.isEmpty() || count <= 0L) {
                clearSlot(slot);
                return;
            }

            ItemStack normalized = prototype.copy();
            normalized.setCount(1);
            prototypes.put(slot, normalized);
            counts.put(slot, count);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= capacity
                    || stack.isEmpty() || ItemBackpack.isForbiddenBackpackContent(stack)) return stack;

            int destination = slot;
            if (isAutoSortEnabled()) {
                destination = findMatchingSlotWithRoom(stack);
                if (destination < 0) destination = findFirstEmptySlot();
            } else if (isOccupied(destination)
                    && !BlackHoleBackpackSavedData.BackpackStorage.areSameType(prototypes.get(destination), stack)) {
                return stack;
            }
            if (destination < 0 || destination >= capacity) {
                return stack;
            }

            long current = getTrueSlotCount(destination);
            long maximum = BackpackConfig.allowsOverstack(backpack)
                    ? Long.MAX_VALUE : Math.max(1, Math.min(64, stack.getMaxStackSize()));
            long room = maximum - current;
            int moved = (int) Math.min((long) stack.getCount(), room);
            if (moved <= 0) return stack;
            if (!simulate) {
                if (!isOccupied(destination)) {
                    ItemStack prototype = stack.copy();
                    prototype.setCount(1);
                    prototypes.put(destination, prototype);
                }
                counts.put(destination, current + moved);
            }
            return copyWithCount(stack, stack.getCount() - moved);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || !isOccupied(slot)) return ItemStack.EMPTY;
            ItemStack prototype = prototypes.get(slot);
            int legalStack = Math.max(1, Math.min(64, prototype.getMaxStackSize()));
            int extractedCount = (int) Math.min((long) Math.min(amount, legalStack), getTrueSlotCount(slot));
            ItemStack extracted = copyWithCount(prototype, extractedCount);
            if (!simulate) {
                long remaining = getTrueSlotCount(slot) - extractedCount;
                if (remaining <= 0L) {
                    clearSlot(slot);
                }
                else counts.put(slot, remaining);
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < 0 || slot >= capacity) return 0;
            if (BackpackConfig.allowsOverstack(backpack)) return Integer.MAX_VALUE;
            ItemStack prototype = prototypes.get(slot);
            return prototype == null || prototype.isEmpty()
                    ? 64 : Math.max(1, Math.min(64, prototype.getMaxStackSize()));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < 0 || slot >= capacity
                    || stack.isEmpty() || ItemBackpack.isForbiddenBackpackContent(stack)) return false;
            if (isAutoSortEnabled()) {
                return findMatchingSlotWithRoom(stack) >= 0
                        || findFirstEmptySlot() < capacity;
            }
            return !isOccupied(slot)
                    || BlackHoleBackpackSavedData.BackpackStorage.areSameType(prototypes.get(slot), stack)
                    && getTrueSlotCount(slot) < (BackpackConfig.allowsOverstack(backpack)
                    ? Long.MAX_VALUE : Math.max(1, Math.min(64, stack.getMaxStackSize())));
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 0 || slot >= capacity) return;
            if (!stack.isEmpty() && ItemBackpack.isForbiddenBackpackContent(stack)) return;
            if (stack.isEmpty()) {
                clearSlot(slot);
                return;
            }

            ItemStack prototype = stack.copy();
            prototype.setCount(1);
            long synchronizedCount = getTrueSlotCount(slot);
            prototypes.put(slot, prototype);
            // FULL/DELTA page sync may deliver the long before the prototype.
            // Preserve an already synchronized count so packet order cannot
            // transiently truncate it back to the count-one display proxy.
            if (synchronizedCount <= 0L) counts.put(slot, 1L);
        }

        @Override
        public void sortContents() {
            // This mirror contains only the currently synchronized page.
            // Sorting it would move page-local entries to global slots 0..N.
            // The server is the sole authority for the complete layout.
        }

        @Override
        public void setAutoSortEnabled(boolean enabled) {
            super.setAutoSortEnabled(enabled);
        }

        @Override
        public boolean tryRebindClientBackpack(ItemStack current) {
            if (current.isEmpty() || current.getItem() != backpackItem) return false;

            UUID previousId = BackpackVirtualStorage.getStorageId(backpack);
            UUID currentId = BackpackVirtualStorage.getStorageId(current);
            if (previousId == null || !previousId.equals(currentId)) return false;

            backpack = current;
            return true;
        }

        private boolean isOccupied(int slot) {
            ItemStack prototype = prototypes.get(slot);
            return slot >= 0 && prototype != null && !prototype.isEmpty() && getTrueSlotCount(slot) > 0L;
        }

        private int findMatchingSlotWithRoom(ItemStack stack) {
            int bestSlot = -1;
            long bestCount = -1L;
            long maximum = BackpackConfig.allowsOverstack(backpack)
                    ? Long.MAX_VALUE : Math.max(1, Math.min(64, stack.getMaxStackSize()));
            for (Map.Entry<Integer, ItemStack> entry : prototypes.entrySet()) {
                int slot = entry.getKey();
                long count = getTrueSlotCount(slot);
                if (isOccupied(slot) && count < maximum && count > bestCount
                        && BlackHoleBackpackSavedData.BackpackStorage.areSameType(entry.getValue(), stack)) {
                    bestSlot = slot;
                    bestCount = count;
                }
            }
            return bestSlot;
        }

        private int findFirstEmptySlot() {
            for (int slot = 0; slot < capacity; slot++) {
                if (!isOccupied(slot)) return slot;
            }
            return capacity;
        }

        private void clearSlot(int slot) {
            prototypes.remove(slot);
            counts.remove(slot);
        }

    }

    private static ItemStack copyWithCount(ItemStack stack, int count) {
        if (count <= 0 || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }
}
