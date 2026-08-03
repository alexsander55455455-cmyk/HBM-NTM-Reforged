package com.hbm.saveddata;

import com.hbm.items.tool.ItemBackpack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Persistent, server-side storage for black hole backpacks. The item stack
 * carries only the UUID used as a key here, so its NBT remains small enough for
 * player and container synchronization.
 */
public class BlackHoleBackpackSavedData extends WorldSavedData {

    public static final String DATA_NAME = "hbm_black_hole_backpacks";
    private static final int STORAGE_FORMAT = 3;
    private final Map<UUID, BackpackStorage> storages = new HashMap<>();

    public BlackHoleBackpackSavedData() {
        super(DATA_NAME);
    }

    public BlackHoleBackpackSavedData(String name) {
        super(name);
    }

    public static BlackHoleBackpackSavedData get(World world) {
        if (world.isRemote || world.getMinecraftServer() == null) {
            throw new IllegalStateException("Black hole backpack storage requires a server world");
        }

        WorldServer overworld = world.getMinecraftServer().getWorld(0);
        if (overworld == null || overworld.getMapStorage() == null) {
            throw new IllegalStateException("Overworld map storage is unavailable");
        }

        MapStorage mapStorage = overworld.getMapStorage();
        BlackHoleBackpackSavedData data = (BlackHoleBackpackSavedData) mapStorage.getOrLoadData(
                BlackHoleBackpackSavedData.class, DATA_NAME);
        if (data == null) {
            data = new BlackHoleBackpackSavedData();
            mapStorage.setData(DATA_NAME, data);
        }
        return data;
    }

    public BackpackStorage getStorage(UUID id) {
        BackpackStorage storage = storages.get(id);
        if (storage == null) {
            storage = new BackpackStorage(this::markDirty);
            storages.put(id, storage);
            markDirty();
        }
        return storage;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        storages.clear();
        boolean migrated = false;
        NBTTagList serializedStorages = nbt.getTagList("storages", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < serializedStorages.tagCount(); i++) {
            NBTTagCompound serializedStorage = serializedStorages.getCompoundTagAt(i);
            if (!serializedStorage.hasUniqueId("id")) continue;

            UUID id = serializedStorage.getUniqueId("id");
            BackpackStorage storage = new BackpackStorage(this::markDirty);
            int format = serializedStorage.getInteger("format");
            if (format >= 2) {
                migrated |= storage.deserializeEntries(
                        serializedStorage.getTagList("entries", Constants.NBT.TAG_COMPOUND),
                        serializedStorage.getLong("revision"));
                migrated |= format < STORAGE_FORMAT;
            } else {
                storage.deserializeLegacy(
                        serializedStorage.getTagList("items", Constants.NBT.TAG_COMPOUND));
                migrated = true;
            }
            BackpackStorage existing = storages.get(id);
            if (existing == null) {
                storages.put(id, storage);
            } else {
                existing.mergeLoadedStorage(storage);
                migrated = true;
            }
        }
        if (migrated) markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList serializedStorages = new NBTTagList();
        for (Map.Entry<UUID, BackpackStorage> entry : storages.entrySet()) {
            BackpackStorage storage = entry.getValue();
            if (storage.getFilledSlotCount() == 0) continue;

            NBTTagCompound serializedStorage = new NBTTagCompound();
            serializedStorage.setUniqueId("id", entry.getKey());
            serializedStorage.setInteger("format", STORAGE_FORMAT);
            serializedStorage.setLong("revision", storage.getRevision());
            serializedStorage.setTag("entries", storage.serializeEntries());
            serializedStorages.appendTag(serializedStorage);
        }
        nbt.setTag("storages", serializedStorages);
        return nbt;
    }

    /**
     * Dynamic virtual-count storage. Each occupied logical slot owns one exact
     * count-one item prototype and a long count. Storage policy is explicit:
     * sorted insertion merges equal variants and keeps a compact row-major
     * list, while manual insertion preserves the exact slot chosen by the
     * player, including separate entries of the same variant.
     */
    public static class BackpackStorage implements IItemHandlerModifiable {
        public static final int ROW_WIDTH = 13;
        public static final int MINIMUM_OPEN_SLOTS = 143;
        private static final int EXTRA_OPEN_SLOTS = 65;
        public static final int MAX_LOGICAL_SLOTS = 1_000_012;
        private static final int MAX_SAFE_POLICY_SEGMENTS = 100_000;

        private final TreeMap<Integer, StoredEntry> entries = new TreeMap<>();
        private final Runnable changed;
        private int allocatedSlots;
        private int filledSlots;
        private double storedRadiation;
        private long revision;
        private boolean compactInvariantDirty;

        private BackpackStorage(Runnable changed) {
            this.changed = changed;
        }

        public int getFilledSlotCount() {
            return filledSlots;
        }

        public double getStoredRadiation() {
            return storedRadiation;
        }

        public int getAllocatedSlotCount() {
            return allocatedSlots;
        }

        public long getRevision() {
            return revision;
        }

        public long getSlotCount(int slot) {
            StoredEntry entry = getEntry(slot);
            return entry == null ? 0L : entry.count;
        }

        public ItemStack getSlotPrototype(int slot) {
            StoredEntry entry = getEntry(slot);
            return entry == null ? ItemStack.EMPTY : entry.prototype.copy();
        }

        public void forEachStoredStack(BiConsumer<ItemStack, Long> visitor) {
            for (StoredEntry entry : entries.values()) {
                if (entry != null && entry.count > 0L && !entry.prototype.isEmpty()) {
                    visitor.accept(entry.prototype.copy(), entry.count);
                }
            }
        }

        /** Imports one authoritative virtual slot during a backend migration without narrowing its long count. */
        public void importStoredCount(int requestedSlot, ItemStack prototype, long count) {
            if (prototype.isEmpty() || count <= 0L) return;
            int destination = requestedSlot >= 0 && requestedSlot < MAX_LOGICAL_SLOTS
                    && !isOccupied(requestedSlot) ? requestedSlot : findFirstEmptySlot();
            if (destination < 0) return;
            entries.put(destination, new StoredEntry(prototype, count));
            allocatedSlots = Math.max(allocatedSlots, destination + 1);
            recomputeDerivedState();
            compactInvariantDirty = true;
            markChanged();
        }

        @Override
        public int getSlots() {
            long requested = Math.max((long) MINIMUM_OPEN_SLOTS,
                    Math.min((long) MAX_LOGICAL_SLOTS, (long) allocatedSlots + EXTRA_OPEN_SLOTS));
            long rounded = (requested + ROW_WIDTH - 1L) / ROW_WIDTH * ROW_WIDTH;
            return (int) Math.min((long) MAX_LOGICAL_SLOTS, rounded);
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            StoredEntry entry = getEntry(slot);
            return entry == null ? ItemStack.EMPTY : copyWithCount(entry.prototype, 1);
        }

        /**
         * Merge into the largest slot for this exact variant, otherwise bind
         * the first sparse empty slot. A full Long.MAX_VALUE entry returns the
         * input unchanged instead of overflowing or deleting it.
         */
        public ItemStack insertStackAnywhere(ItemStack stack, boolean simulate) {
            return insertStackAnywhere(stack, simulate, true);
        }

        public ItemStack insertStackAnywhere(ItemStack stack, boolean simulate, boolean autoSort) {
            return insertStackAnywhere(stack, simulate, autoSort, MAX_LOGICAL_SLOTS);
        }

        public ItemStack insertStackAnywhere(ItemStack stack, boolean simulate, boolean autoSort, int maxSlots) {
            return insertStackAnywhere(stack, simulate, autoSort, maxSlots, true);
        }

        public ItemStack insertStackAnywhere(ItemStack stack, boolean simulate, boolean autoSort,
                                             int maxSlots, boolean allowOverstack) {
            if (!isNewContentAllowed(stack)) return stack;
            int limit = clampCapacity(maxSlots);
            if (simulate) return simulateInsertAnywhere(stack, limit, allowOverstack);
            if (autoSort && !simulate) ensureCompactInvariant(allowOverstack);
            ItemStack remaining = stack;
            while (!remaining.isEmpty()) {
                int destination = findMatchingSlotWithRoom(remaining, limit, allowOverstack);
                if (destination < 0) destination = findFirstEmptySlot(limit);
                if (destination < 0) break;
                ItemStack next = insertAtDestination(destination, remaining, simulate, allowOverstack);
                if (next.getCount() == remaining.getCount()) break;
                remaining = next;
            }
            if (autoSort && !simulate && remaining.getCount() != stack.getCount()) {
                compactAndMergeStable(allowOverstack);
            }
            if (!autoSort && !simulate && remaining.getCount() != stack.getCount()) {
                compactInvariantDirty = true;
            }
            return remaining;
        }

        private ItemStack simulateInsertAnywhere(ItemStack stack, int maxSlots, boolean allowOverstack) {
            long remaining = stack.getCount();
            long maximum = getMaximumSlotCount(stack, allowOverstack);
            int occupied = 0;
            for (Map.Entry<Integer, StoredEntry> stored : entries.entrySet()) {
                if (stored.getKey() >= maxSlots) break;
                StoredEntry entry = stored.getValue();
                if (entry == null || entry.count <= 0L) continue;
                occupied++;
                if (areSameType(entry.prototype, stack) && entry.count < maximum) {
                    remaining -= Math.min(remaining, maximum - entry.count);
                    if (remaining <= 0L) return ItemStack.EMPTY;
                }
            }
            long emptySlots = Math.max(0L, (long) maxSlots - occupied);
            if (maximum == Long.MAX_VALUE && emptySlots > 0L) return ItemStack.EMPTY;
            long emptyRoom = emptySlots > Long.MAX_VALUE / maximum
                    ? Long.MAX_VALUE : emptySlots * maximum;
            remaining -= Math.min(remaining, emptyRoom);
            return copyWithCount(stack, (int) remaining);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return insertItem(slot, stack, simulate, true);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate, boolean autoSort) {
            return insertItem(slot, stack, simulate, autoSort, MAX_LOGICAL_SLOTS);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate,
                                    boolean autoSort, int maxSlots) {
            return insertItem(slot, stack, simulate, autoSort, maxSlots, true);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate,
                                    boolean autoSort, int maxSlots, boolean allowOverstack) {
            int limit = clampCapacity(maxSlots);
            if (slot < 0 || slot >= limit || !isNewContentAllowed(stack)) return stack;

            if (autoSort) {
                return insertStackAnywhere(stack, simulate, true, limit, allowOverstack);
            }

            StoredEntry entry = getEntry(slot);
            if (entry != null && !areSameType(entry.prototype, stack)) return stack;
            ItemStack remaining = insertAtDestination(slot, stack, simulate, allowOverstack);
            if (!simulate && remaining.getCount() != stack.getCount()) {
                compactInvariantDirty = true;
            }
            return remaining;
        }

        private ItemStack insertAtDestination(int destination, ItemStack stack, boolean simulate,
                                              boolean allowOverstack) {
            StoredEntry entry = getEntry(destination);
            long current = entry == null ? 0L : entry.count;
            long room = getMaximumSlotCount(stack, allowOverstack) - current;
            int moved = (int) Math.min((long) stack.getCount(), room);
            if (moved <= 0) return stack;

            if (!simulate) {
                if (entry == null) {
                    entry = new StoredEntry(stack, 0L);
                    entries.put(destination, entry);
                    allocatedSlots = Math.max(allocatedSlots, destination + 1);
                    filledSlots++;
                }
                entry.count += moved;
                addStoredRadiation(entry.prototype, moved);
                markChanged();
            }
            return copyWithCount(stack, stack.getCount() - moved);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return extractItem(slot, amount, simulate, false);
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate, boolean autoSort) {
            return extractItem(slot, amount, simulate, autoSort, true);
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate,
                                     boolean autoSort, boolean allowOverstack) {
            if (autoSort && !simulate) ensureCompactInvariant(allowOverstack);
            StoredEntry entry = getEntry(slot);
            if (entry == null || amount <= 0) return ItemStack.EMPTY;

            int legalStack = Math.max(1, Math.min(64, entry.prototype.getMaxStackSize()));
            int extracted = (int) Math.min((long) Math.min(amount, legalStack), entry.count);
            if (extracted <= 0) return ItemStack.EMPTY;
            ItemStack result = copyWithCount(entry.prototype, extracted);

            if (!simulate) {
                entry.count -= extracted;
                if (entry.count <= 0L) {
                    entries.remove(slot);
                    filledSlots--;
                }
                removeStoredRadiation(entry.prototype, extracted);
                if (autoSort && entry.count <= 0L) {
                    compactAndMergeStableInternal(allowOverstack);
                } else if (!autoSort) {
                    compactInvariantDirty = true;
                }
                markChanged();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot >= 0 && slot < MAX_LOGICAL_SLOTS ? Integer.MAX_VALUE : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isItemValid(slot, stack, true);
        }

        public boolean isItemValid(int slot, ItemStack stack, boolean autoSort) {
            return isItemValid(slot, stack, autoSort, MAX_LOGICAL_SLOTS);
        }

        public boolean isItemValid(int slot, ItemStack stack, boolean autoSort, int maxSlots) {
            return isItemValid(slot, stack, autoSort, maxSlots, true);
        }

        public boolean isItemValid(int slot, ItemStack stack, boolean autoSort,
                                   int maxSlots, boolean allowOverstack) {
            int limit = clampCapacity(maxSlots);
            if (slot < 0 || slot >= limit || !isNewContentAllowed(stack)) return false;
            if (autoSort) {
                return findMatchingSlotWithRoom(stack, limit, allowOverstack) >= 0
                        || findFirstEmptySlot(limit) >= 0;
            }
            StoredEntry entry = getEntry(slot);
            return entry == null || areSameType(entry.prototype, stack)
                    && entry.count < getMaximumSlotCount(stack, allowOverstack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            // A count-one ItemStack is only a display proxy for this backend.
            // Letting generic Slot/IItemHandler code replace it would truncate
            // an arbitrary long count or delete the entire logical entry.
            // All supported server mutations are atomic insert/extract calls.
        }

        /**
         * Deterministic row-major compaction used when auto-sort is enabled.
         * Equal variants merge without silently losing overflow, then entries
         * are ordered by their stable registry identity.
         */
        public void compactAndMergeStable() {
            compactAndMergeStable(true);
        }

        public void compactAndMergeStable(boolean allowOverstack) {
            if (!allowOverstack && !canApplySlotLimitPolicy(MAX_SAFE_POLICY_SEGMENTS)) return;
            boolean changedLayout = compactAndMergeStableInternal(allowOverstack);
            compactInvariantDirty = false;
            if (changedLayout) {
                recomputeDerivedState();
                markChanged();
            }
        }

        /** Prevents a config change from expanding one huge long count into an unsafe number of objects. */
        public boolean applySlotLimitPolicy(boolean allowOverstack) {
            if (allowOverstack) return true;
            boolean needsSplit = false;
            for (StoredEntry entry : entries.values()) {
                if (entry != null && entry.count > getMaximumSlotCount(entry.prototype, false)) {
                    needsSplit = true;
                    break;
                }
            }
            if (!needsSplit) return true;
            if (!canApplySlotLimitPolicy(MAX_SAFE_POLICY_SEGMENTS)) return false;

            TreeMap<Integer, StoredEntry> normalized = new TreeMap<>();
            List<StoredEntry> overflow = new ArrayList<>();
            for (Map.Entry<Integer, StoredEntry> stored : entries.entrySet()) {
                StoredEntry source = stored.getValue();
                if (source == null || source.count <= 0L || source.prototype.isEmpty()) continue;
                long maximum = getMaximumSlotCount(source.prototype, false);
                normalized.put(stored.getKey(), new StoredEntry(source.prototype, Math.min(source.count, maximum)));
                long remaining = source.count - Math.min(source.count, maximum);
                while (remaining > 0L) {
                    long segment = Math.min(remaining, maximum);
                    overflow.add(new StoredEntry(source.prototype, segment));
                    remaining -= segment;
                }
            }
            int destination = 0;
            for (StoredEntry segment : overflow) {
                while (normalized.containsKey(destination) && destination < MAX_LOGICAL_SLOTS) destination++;
                if (destination >= MAX_LOGICAL_SLOTS) return false;
                normalized.put(destination++, segment);
            }
            entries.clear();
            entries.putAll(normalized);
            allocatedSlots = entries.isEmpty() ? 0 : entries.lastKey() + 1;
            recomputeDerivedState();
            compactInvariantDirty = true;
            markChanged();
            return true;
        }

        private boolean canApplySlotLimitPolicy(int maximumSegments) {
            long segments = 0L;
            for (StoredEntry entry : entries.values()) {
                if (entry == null || entry.count <= 0L || entry.prototype.isEmpty()) continue;
                long maximum = getMaximumSlotCount(entry.prototype, false);
                long required = (entry.count - 1L) / maximum + 1L;
                if (required > maximumSegments - segments) return false;
                segments += required;
            }
            return true;
        }

        /**
         * Restores the sorted invariant only when a manual view of the same
         * UUID has changed its layout. This keeps multiple handles to one
         * storage deterministic without re-sorting on every operation.
         */
        public void ensureCompactInvariant() {
            ensureCompactInvariant(true);
        }

        public void ensureCompactInvariant(boolean allowOverstack) {
            if (!compactInvariantDirty) return;
            compactAndMergeStable(allowOverstack);
        }

        /** Backwards-compatible name used by the common inventory interface. */
        public void sort() {
            compactAndMergeStable();
        }

        /**
         * Builds a bounded, exact, revision-checked removal batch before
         * capacity is reduced. Repeated ticks drain arbitrarily large overflow
         * without constructing an unsafe number of ItemStacks at once.
         */
        public ShrinkTransaction prepareShrink(int newCapacity) {
            int capacity = clampCapacity(newCapacity);
            List<ShrinkEntry> removed = new ArrayList<>();
            List<ItemStack> drops = new ArrayList<>();
            for (Map.Entry<Integer, StoredEntry> stored : entries.tailMap(capacity, true).entrySet()) {
                if (drops.size() >= ShrinkTransaction.MAX_DROP_STACKS) break;
                StoredEntry entry = stored.getValue();
                if (entry == null || entry.count <= 0L || entry.prototype.isEmpty()) continue;
                int legalStack = Math.max(1, Math.min(64, entry.prototype.getMaxStackSize()));
                long stackBudget = ShrinkTransaction.MAX_DROP_STACKS - drops.size();
                long removable = Math.min(entry.count, stackBudget * legalStack);
                if (removable <= 0L) break;
                removed.add(new ShrinkEntry(stored.getKey(), entry, removable));
                long remaining = removable;
                while (remaining > 0L) {
                    int amount = (int) Math.min((long) legalStack, remaining);
                    drops.add(copyWithCount(entry.prototype, amount));
                    remaining -= amount;
                }
            }
            return new ShrinkTransaction(this, capacity, revision, removed, drops, true);
        }

        private boolean compactAndMergeStableInternal() {
            return compactAndMergeStableInternal(true);
        }

        private boolean compactAndMergeStableInternal(boolean allowOverstack) {
            List<StoredEntry> compacted = new ArrayList<>();
            Map<NBTTagCompound, StoredEntry> writableByVariant = new LinkedHashMap<>();

            for (StoredEntry source : entries.values()) {
                if (source == null || source.count <= 0L || source.prototype.isEmpty()) continue;

                long remaining = source.count;
                NBTTagCompound identity = normalizedIdentity(source.prototype);
                StoredEntry target = writableByVariant.get(identity);
                long slotMaximum = getMaximumSlotCount(source.prototype, allowOverstack);
                if (target != null && target.count < slotMaximum) {
                    long moved = Math.min(remaining, slotMaximum - target.count);
                    target.count += moved;
                    remaining -= moved;
                }
                while (remaining > 0L) {
                    long segment = Math.min(remaining, slotMaximum);
                    target = new StoredEntry(source.prototype, segment);
                    compacted.add(target);
                    writableByVariant.put(identity, target);
                    remaining -= segment;
                }
            }
            compacted.sort((left, right) ->
                    ItemBackpack.compareForBackpackSort(left.prototype, right.prototype));

            boolean changedLayout = compacted.size() != entries.size();
            if (!changedLayout) {
                int slot = 0;
                for (Map.Entry<Integer, StoredEntry> current : entries.entrySet()) {
                    StoredEntry compact = compacted.get(slot);
                    if (current.getKey() != slot
                            || current.getValue().count != compact.count
                            || !areSameType(current.getValue().prototype, compact.prototype)) {
                        changedLayout = true;
                        break;
                    }
                    slot++;
                }
            }
            if (!changedLayout) return false;

            entries.clear();
            for (int slot = 0; slot < compacted.size(); slot++) {
                entries.put(slot, compacted.get(slot));
            }
            allocatedSlots = compacted.size();
            filledSlots = compacted.size();
            return true;
        }

        private NBTTagList serializeEntries() {
            NBTTagList serializedEntries = new NBTTagList();
            for (Map.Entry<Integer, StoredEntry> stored : entries.entrySet()) {
                int slot = stored.getKey();
                StoredEntry entry = stored.getValue();
                if (entry == null || entry.count <= 0L) continue;

                NBTTagCompound serializedEntry = new NBTTagCompound();
                serializedEntry.setInteger("slot", slot);
                serializedEntry.setLong("count", entry.count);
                serializedEntry.setTag("prototype", entry.prototype.serializeNBT());
                serializedEntries.appendTag(serializedEntry);
            }
            return serializedEntries;
        }

        /**
         * @return true when malformed/duplicate v2 data was normalized and must
         * be written back.
         */
        private boolean deserializeEntries(NBTTagList serializedEntries, long serializedRevision) {
            clearForLoad();
            revision = Math.max(0L, serializedRevision);
            List<LoadedEntry> loaded = new ArrayList<>();
            boolean normalized = false;

            for (int index = 0; index < serializedEntries.tagCount(); index++) {
                NBTTagCompound serializedEntry = serializedEntries.getCompoundTagAt(index);
                int slot = serializedEntry.getInteger("slot");
                long count = serializedEntry.getLong("count");
                ItemStack prototype = new ItemStack(serializedEntry.getCompoundTag("prototype"));
                if (slot < 0 || count <= 0L || prototype.isEmpty()) {
                    normalized = true;
                    continue;
                }
                if (prototype.getCount() != 1) normalized = true;
                loaded.add(new LoadedEntry(slot, prototype, count));
            }
            loaded.sort((first, second) -> Integer.compare(first.slot, second.slot));
            for (LoadedEntry entry : loaded) {
                normalized |= placeLoadedEntryPreservingPosition(entry.slot, entry.prototype, entry.count);
            }
            recomputeDerivedState();
            compactInvariantDirty = !entries.isEmpty();
            if (normalized) revision = nextRevision(revision);
            return normalized;
        }

        /** Reads the original physical-stack layout and canonically merges it. */
        private void deserializeLegacy(NBTTagList serializedItems) {
            clearForLoad();
            List<LoadedEntry> loaded = new ArrayList<>();
            for (int index = 0; index < serializedItems.tagCount(); index++) {
                NBTTagCompound serializedStack = serializedItems.getCompoundTagAt(index);
                int slot = serializedStack.getInteger("slot");
                ItemStack stack = new ItemStack(serializedStack.getCompoundTag("stack"));
                if (slot < 0 || stack.isEmpty()) continue;
                loaded.add(new LoadedEntry(slot, stack, stack.getCount()));
            }
            loaded.sort((first, second) -> Integer.compare(first.slot, second.slot));
            for (LoadedEntry entry : loaded) {
                placeLoadedEntry(entry.slot, entry.prototype, entry.count);
            }
            recomputeDerivedState();
            compactInvariantDirty = !entries.isEmpty();
            revision = filledSlots == 0 ? 0L : 1L;
        }

        private void mergeLoadedStorage(BackpackStorage other) {
            for (Map.Entry<Integer, StoredEntry> stored : other.entries.entrySet()) {
                int slot = stored.getKey();
                StoredEntry entry = stored.getValue();
                if (entry != null) {
                    placeLoadedEntryPreservingPosition(slot, entry.prototype, entry.count);
                }
            }
            recomputeDerivedState();
            compactInvariantDirty = !entries.isEmpty();
            revision = nextRevision(Math.max(revision, other.revision));
        }

        /**
         * Places one serialized record while preserving its requested position
         * where possible. Exact duplicates merge into the earliest position.
         */
        private boolean placeLoadedEntry(int requestedSlot, ItemStack prototype, long count) {
            int canonicalSlot = findMatchingSlot(prototype);
            if (canonicalSlot >= 0) {
                StoredEntry canonical = entries.get(canonicalSlot);
                long moved = Math.min(count, Long.MAX_VALUE - canonical.count);
                canonical.count += moved;
                long remaining = count - moved;
                if (remaining > 0L) {
                    int overflowSlot = findFirstEmptySlot();
                    if (overflowSlot >= 0) {
                        entries.put(overflowSlot, new StoredEntry(prototype, remaining));
                        allocatedSlots = Math.max(allocatedSlots, overflowSlot + 1);
                    }
                }
                return true;
            }

            boolean normalized = requestedSlot >= MAX_LOGICAL_SLOTS;
            int destination = !normalized && !isOccupied(requestedSlot)
                    ? requestedSlot : findFirstEmptySlot();
            if (destination < 0) return true;
            entries.put(destination, new StoredEntry(prototype, count));
            allocatedSlots = Math.max(allocatedSlots, destination + 1);
            return normalized || destination != requestedSlot;
        }

        /**
         * V2/V3 entries already represent virtual slots. Preserve their exact
         * positions and same-variant duplicates; only a corrupt slot collision
         * is remapped to the first free position.
         */
        private boolean placeLoadedEntryPreservingPosition(int requestedSlot, ItemStack prototype, long count) {
            boolean normalized = requestedSlot < 0 || requestedSlot >= MAX_LOGICAL_SLOTS;
            int destination = !normalized && !isOccupied(requestedSlot)
                    ? requestedSlot : findFirstEmptySlot();
            if (destination < 0) return true;

            entries.put(destination, new StoredEntry(prototype, count));
            allocatedSlots = Math.max(allocatedSlots, destination + 1);
            return normalized || destination != requestedSlot;
        }

        private void clearForLoad() {
            entries.clear();
            allocatedSlots = 0;
            filledSlots = 0;
            storedRadiation = 0D;
            revision = 0L;
            compactInvariantDirty = false;
        }

        private void recomputeDerivedState() {
            filledSlots = 0;
            storedRadiation = 0D;
            for (StoredEntry entry : entries.values()) {
                if (entry == null || entry.count <= 0L) continue;
                filledSlots++;
                storedRadiation = safeRadiationAdd(
                        storedRadiation, radiationFor(entry.prototype, entry.count));
            }
        }

        private void addStoredRadiation(ItemStack prototype, long amount) {
            storedRadiation = safeRadiationAdd(storedRadiation, radiationFor(prototype, amount));
        }

        private void removeStoredRadiation(ItemStack prototype, long amount) {
            if (storedRadiation == Double.MAX_VALUE) {
                recomputeDerivedState();
                return;
            }
            double removed = radiationFor(prototype, amount);
            storedRadiation = !Double.isFinite(removed) ? 0D : Math.max(0D, storedRadiation - removed);
        }

        private static double radiationFor(ItemStack prototype, long count) {
            if (prototype.isEmpty() || count <= 0L) return 0D;
            ItemStack single = prototype.copy();
            single.setCount(1);
            double perItem = Math.max(0D, ItemBackpack.getStackRadiation(single));
            if (perItem <= 0D) return 0D;
            double total = perItem * (double) count;
            return Double.isFinite(total) ? Math.max(0D, total) : Double.MAX_VALUE;
        }

        private static double safeRadiationAdd(double first, double second) {
            if (first >= Double.MAX_VALUE || second >= Double.MAX_VALUE) return Double.MAX_VALUE;
            double result = Math.max(0D, first) + Math.max(0D, second);
            return Double.isFinite(result) ? result : Double.MAX_VALUE;
        }

        private StoredEntry getEntry(int slot) {
            if (slot < 0 || slot >= allocatedSlots) return null;
            StoredEntry entry = entries.get(slot);
            return entry == null || entry.count <= 0L || entry.prototype.isEmpty() ? null : entry;
        }

        private boolean isOccupied(int slot) {
            return getEntry(slot) != null;
        }

        private int findMatchingSlot(ItemStack stack) {
            if (stack.isEmpty()) return -1;
            for (Map.Entry<Integer, StoredEntry> stored : entries.entrySet()) {
                StoredEntry entry = stored.getValue();
                if (entry != null && entry.count > 0L
                        && areSameType(entry.prototype, stack)) {
                    return stored.getKey();
                }
            }
            return -1;
        }

        private int findMatchingSlotWithRoom(ItemStack stack) {
            return findMatchingSlotWithRoom(stack, MAX_LOGICAL_SLOTS);
        }

        private int findMatchingSlotWithRoom(ItemStack stack, int maxSlots) {
            return findMatchingSlotWithRoom(stack, maxSlots, true);
        }

        private int findMatchingSlotWithRoom(ItemStack stack, int maxSlots, boolean allowOverstack) {
            if (stack.isEmpty()) return -1;
            int bestSlot = -1;
            long bestCount = -1L;
            long maximum = getMaximumSlotCount(stack, allowOverstack);
            for (Map.Entry<Integer, StoredEntry> stored : entries.entrySet()) {
                if (stored.getKey() >= maxSlots) break;
                StoredEntry entry = stored.getValue();
                if (entry != null && entry.count > 0L && entry.count < maximum
                        && areSameType(entry.prototype, stack) && entry.count > bestCount) {
                    bestSlot = stored.getKey();
                    bestCount = entry.count;
                }
            }
            return bestSlot;
        }

        private int findFirstEmptySlot() {
            return findFirstEmptySlot(MAX_LOGICAL_SLOTS);
        }

        private int findFirstEmptySlot(int maxSlots) {
            int limit = clampCapacity(maxSlots);
            int candidate = 0;
            for (Integer occupiedSlot : entries.keySet()) {
                if (occupiedSlot < candidate) continue;
                if (occupiedSlot > candidate) break;
                candidate++;
                if (candidate >= limit) return -1;
            }
            return candidate < limit ? candidate : -1;
        }

        private static int clampCapacity(int capacity) {
            return Math.max(0, Math.min(MAX_LOGICAL_SLOTS, capacity));
        }

        private static long getMaximumSlotCount(ItemStack prototype, boolean allowOverstack) {
            return allowOverstack ? Long.MAX_VALUE
                    : Math.max(1, Math.min(64, prototype.getMaxStackSize()));
        }

        private void restoreShrinkEntries(List<ShrinkEntry> removed) {
            for (ShrinkEntry snapshot : removed) {
                entries.put(snapshot.slot, snapshot.entry.copy());
            }
            allocatedSlots = entries.isEmpty() ? 0 : entries.lastKey() + 1;
            recomputeDerivedState();
            compactInvariantDirty = !entries.isEmpty();
            markChanged();
        }

        private void markChanged() {
            revision = nextRevision(revision);
            changed.run();
        }

        private static long nextRevision(long current) {
            return current >= Long.MAX_VALUE ? 1L : Math.max(0L, current) + 1L;
        }

        private static boolean isNewContentAllowed(ItemStack stack) {
            return !stack.isEmpty() && !ItemBackpack.isForbiddenBackpackContent(stack);
        }

        /**
         * Exact stored variant identity. Count is deliberately excluded; all
         * other serialized item, metadata, NBT and Forge capability data remain
         * part of the key.
         */
        public static boolean areSameType(ItemStack first, ItemStack second) {
            if (first.isEmpty() || second.isEmpty()) return false;
            ItemStack a = first.copy();
            ItemStack b = second.copy();
            a.setCount(1);
            b.setCount(1);
            return a.serializeNBT().equals(b.serializeNBT());
        }

        private static NBTTagCompound normalizedIdentity(ItemStack stack) {
            ItemStack normalized = stack.copy();
            normalized.setCount(1);
            return normalized.serializeNBT();
        }

        private static ItemStack copyWithCount(ItemStack stack, int count) {
            if (count <= 0 || stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack copy = stack.copy();
            copy.setCount(count);
            return copy;
        }

        private static final class StoredEntry {
            private final ItemStack prototype;
            private long count;

            private StoredEntry(ItemStack prototype, long count) {
                this.prototype = prototype.copy();
                this.prototype.setCount(1);
                this.count = Math.max(0L, count);
            }

            private StoredEntry copy() {
                return new StoredEntry(prototype, count);
            }
        }

        private static final class ShrinkEntry {
            private final int slot;
            private final StoredEntry entry;
            private final long removedCount;

            private ShrinkEntry(int slot, StoredEntry entry, long removedCount) {
                this.slot = slot;
                this.entry = entry.copy();
                this.removedCount = Math.max(0L, Math.min(entry.count, removedCount));
            }
        }

        public static final class ShrinkTransaction {
            private static final int MAX_DROP_STACKS = 256;

            private final BackpackStorage storage;
            private final int capacity;
            private final long preparedRevision;
            private final List<ShrinkEntry> removed;
            private final List<ItemStack> drops;
            private final boolean prepared;
            private boolean committed;

            private ShrinkTransaction(BackpackStorage storage, int capacity, long preparedRevision,
                                      List<ShrinkEntry> removed, List<ItemStack> drops, boolean prepared) {
                this.storage = storage;
                this.capacity = capacity;
                this.preparedRevision = preparedRevision;
                this.removed = removed;
                this.drops = drops;
                this.prepared = prepared;
            }

            private static ShrinkTransaction unavailable(BackpackStorage storage, int capacity, long revision) {
                return new ShrinkTransaction(storage, capacity, revision,
                        new ArrayList<>(), new ArrayList<>(), false);
            }

            public boolean isPrepared() {
                return prepared;
            }

            public List<ItemStack> getDrops() {
                List<ItemStack> copy = new ArrayList<>(drops.size());
                for (ItemStack stack : drops) copy.add(stack.copy());
                return copy;
            }

            public boolean commit() {
                if (!prepared || committed || storage.revision != preparedRevision) return false;
                for (ShrinkEntry snapshot : removed) {
                    StoredEntry current = storage.entries.get(snapshot.slot);
                    if (current == null || current.count != snapshot.entry.count
                            || !areSameType(current.prototype, snapshot.entry.prototype)) return false;
                }
                for (ShrinkEntry snapshot : removed) {
                    StoredEntry current = storage.entries.get(snapshot.slot);
                    if (current == null || snapshot.removedCount >= current.count) {
                        storage.entries.remove(snapshot.slot);
                    } else {
                        current.count -= snapshot.removedCount;
                    }
                }
                storage.allocatedSlots = storage.entries.isEmpty() ? 0 : storage.entries.lastKey() + 1;
                storage.recomputeDerivedState();
                storage.compactInvariantDirty = !storage.entries.isEmpty();
                storage.markChanged();
                committed = true;
                return true;
            }

            public boolean rollback() {
                if (!committed) return false;
                storage.restoreShrinkEntries(removed);
                committed = false;
                return true;
            }
        }

        private static final class LoadedEntry {
            private final int slot;
            private final ItemStack prototype;
            private final long count;

            private LoadedEntry(int slot, ItemStack prototype, long count) {
                this.slot = slot;
                this.prototype = prototype.copy();
                this.prototype.setCount(1);
                this.count = Math.max(0L, count);
            }
        }
    }
}
