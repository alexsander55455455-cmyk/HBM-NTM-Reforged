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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent, server-side storage for pocket-hole backpacks. Each backpack has
 * nine base slots plus capacity-module expansion; every slot binds to one exact
 * item prototype and holds a long count. Vanilla only receives a count-one
 * display proxy. All mutations are performed atomically against the true
 * server-side value by the backpack container, so mutable vanilla ItemStack
 * arithmetic can never truncate it.
 */
public class PocketHoleBackpackSavedData extends WorldSavedData {

    public static final String DATA_NAME = "hbm_pocket_hole_backpacks";
    private final Map<UUID, PocketHoleStorage> storages = new HashMap<>();

    public PocketHoleBackpackSavedData() {
        super(DATA_NAME);
    }

    public PocketHoleBackpackSavedData(String name) {
        super(name);
    }

    public static PocketHoleBackpackSavedData get(World world) {
        if (world.isRemote || world.getMinecraftServer() == null) {
            throw new IllegalStateException("Pocket-hole backpack storage requires a server world");
        }

        WorldServer overworld = world.getMinecraftServer().getWorld(0);
        if (overworld == null || overworld.getMapStorage() == null) {
            throw new IllegalStateException("Overworld map storage is unavailable");
        }

        MapStorage mapStorage = overworld.getMapStorage();
        PocketHoleBackpackSavedData data = (PocketHoleBackpackSavedData) mapStorage.getOrLoadData(
                PocketHoleBackpackSavedData.class, DATA_NAME);
        if (data == null) {
            data = new PocketHoleBackpackSavedData();
            mapStorage.setData(DATA_NAME, data);
        }
        return data;
    }

    public PocketHoleStorage getStorage(UUID id) {
        PocketHoleStorage storage = storages.get(id);
        if (storage == null) {
            storage = new PocketHoleStorage(this::markDirty);
            storages.put(id, storage);
            markDirty();
        }
        return storage;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        storages.clear();
        NBTTagList serializedStorages = nbt.getTagList("storages", Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < serializedStorages.tagCount(); index++) {
            NBTTagCompound serializedStorage = serializedStorages.getCompoundTagAt(index);
            if (!serializedStorage.hasUniqueId("id")) continue;

            PocketHoleStorage storage = new PocketHoleStorage(this::markDirty);
            if (storage.deserialize(serializedStorage)) {
                storages.put(serializedStorage.getUniqueId("id"), storage);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList serializedStorages = new NBTTagList();
        for (Map.Entry<UUID, PocketHoleStorage> entry : storages.entrySet()) {
            PocketHoleStorage storage = entry.getValue();
            if (storage.isEmpty()) continue;

            NBTTagCompound serializedStorage = storage.serialize();
            serializedStorage.setUniqueId("id", entry.getKey());
            serializedStorages.appendTag(serializedStorage);
        }
        nbt.setTag("storages", serializedStorages);
        return nbt;
    }

    public static final class PocketHoleStorage implements IItemHandlerModifiable {
        public static final int BASE_SLOTS = 9;
        public static final int MAX_SLOTS = BASE_SLOTS + 5 * 27;
        /** Legacy name retained for code that means the base nine-cell view. */
        public static final int SLOTS = BASE_SLOTS;
        private static final int MAX_DROP_STACKS = 256;

        private final Runnable changed;
        private final ItemStack[] prototypes = new ItemStack[MAX_SLOTS];
        private final long[] counts = new long[MAX_SLOTS];
        private long revision;

        private PocketHoleStorage(Runnable changed) {
            this.changed = changed;
            Arrays.fill(prototypes, ItemStack.EMPTY);
        }

        // ---- queries -------------------------------------------------------

        public boolean isEmpty() {
            for (int slot = 0; slot < MAX_SLOTS; slot++) {
                if (isOccupied(slot)) return false;
            }
            return true;
        }

        public long getTotalCount() {
            long total = 0L;
            for (long count : counts) {
                if (count > Long.MAX_VALUE - total) return Long.MAX_VALUE;
                total += count;
            }
            return total;
        }

        /** The true, uncapped count of a slot (0 if empty). */
        public long getSlotCount(int slot) {
            return slot >= 0 && slot < MAX_SLOTS ? Math.max(0L, counts[slot]) : 0L;
        }

        public ItemStack getSlotPrototype(int slot) {
            return slot >= 0 && slot < MAX_SLOTS && !prototypes[slot].isEmpty()
                    ? prototypes[slot].copy() : ItemStack.EMPTY;
        }

        /** Clears the old backend after every prototype and long count was copied to sparse storage. */
        public void clearForBackendMigration() {
            boolean changedContents = false;
            for (int slot = 0; slot < MAX_SLOTS; slot++) {
                if (!isOccupied(slot)) continue;
                clearSlot(slot);
                changedContents = true;
            }
            if (changedContents) markChanged();
        }

        public int getOccupiedSlots() {
            return getOccupiedSlots(MAX_SLOTS);
        }

        public int getOccupiedSlots(int activeSlots) {
            int occupied = 0;
            int capacity = clampCapacity(activeSlots);
            for (int slot = 0; slot < capacity; slot++) {
                if (isOccupied(slot)) occupied++;
            }
            return occupied;
        }

        public double getStoredRadiation() {
            double radiation = 0D;
            for (int slot = 0; slot < MAX_SLOTS; slot++) {
                if (!isOccupied(slot)) continue;
                double perItem = ItemBackpack.getStackRadiation(prototypes[slot]); // prototype count is 1
                if (perItem <= 0D) continue;
                radiation += perItem * counts[slot];
                if (!Double.isFinite(radiation)) return Double.MAX_VALUE;
            }
            return radiation;
        }

        public void sortContents() {
            sortContents(MAX_SLOTS);
        }

        public void sortContents(int activeSlots) {
            int capacity = clampCapacity(activeSlots);
            List<SortEntry> compacted = new ArrayList<>();
            for (int slot = 0; slot < capacity; slot++) {
                if (!isOccupied(slot)) continue;
                long remaining = counts[slot];
                for (SortEntry target : compacted) {
                    if (!areSameType(target.prototype, prototypes[slot]) || target.count == Long.MAX_VALUE) continue;
                    long moved = Math.min(remaining, Long.MAX_VALUE - target.count);
                    target.count += moved;
                    remaining -= moved;
                    if (remaining == 0L) break;
                }
                if (remaining > 0L) compacted.add(new SortEntry(prototypes[slot], remaining));
            }
            compacted.sort((left, right) ->
                    ItemBackpack.compareForBackpackSort(left.prototype, right.prototype));

            boolean changedLayout = false;
            for (int slot = 0; slot < capacity; slot++) {
                if (slot >= compacted.size()) {
                    if (isOccupied(slot)) changedLayout = true;
                } else {
                    SortEntry target = compacted.get(slot);
                    if (!isOccupied(slot) || counts[slot] != target.count
                            || !areSameType(prototypes[slot], target.prototype)) {
                        changedLayout = true;
                    }
                }
                if (changedLayout) break;
            }
            if (!changedLayout) return;

            for (int slot = 0; slot < capacity; slot++) clearSlot(slot);
            for (int slot = 0; slot < compacted.size(); slot++) {
                SortEntry entry = compacted.get(slot);
                prototypes[slot] = entry.prototype;
                counts[slot] = entry.count;
            }
            markChanged();
        }

        private boolean isOccupied(int slot) {
            return counts[slot] > 0L && !prototypes[slot].isEmpty();
        }

        // ---- IItemHandlerModifiable ---------------------------------------

        @Override
        public int getSlots() {
            return MAX_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= MAX_SLOTS || !isOccupied(slot)) return ItemStack.EMPTY;
            return copyWithCount(prototypes[slot], 1);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return insertItem(slot, stack, simulate, false, MAX_SLOTS);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate, int activeSlots) {
            return insertItem(slot, stack, simulate, false, activeSlots);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate,
                                    boolean autoSort, int activeSlots) {
            int capacity = clampCapacity(activeSlots);
            if (slot < 0 || slot >= capacity || !isNewContentAllowed(stack)) return stack;

            int destination = slot;
            if (autoSort) {
                destination = findLargestMatchingSlotWithRoom(stack, capacity);
                if (destination < 0) destination = findFirstEmptySlot(capacity);
            } else if (isOccupied(destination) && !areSameType(prototypes[destination], stack)) {
                return stack;
            }
            if (destination < 0) return stack;

            long room = Long.MAX_VALUE - counts[destination];
            int moved = (int) Math.min((long) stack.getCount(), room);
            if (moved <= 0) return stack;
            if (!simulate) {
                bind(destination, stack);
                counts[destination] += moved;
                markChanged();
            }
            return copyWithCount(stack, stack.getCount() - moved);
        }

        /** Merge into the largest matching slot, otherwise bind the first empty slot. */
        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            return insertItemAnywhere(stack, simulate, MAX_SLOTS);
        }

        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate, int activeSlots) {
            if (!isNewContentAllowed(stack)) return stack;
            int capacity = clampCapacity(activeSlots);
            int destination = findLargestMatchingSlotWithRoom(stack, capacity);
            if (destination < 0) destination = findFirstEmptySlot(capacity);
            return destination < 0 ? stack : insertItem(destination, stack, simulate, false, capacity);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= MAX_SLOTS || amount <= 0 || !isOccupied(slot)) return ItemStack.EMPTY;
            int perExtract = Math.max(1, prototypes[slot].getMaxStackSize());
            int taken = (int) Math.min((long) Math.min(amount, perExtract), counts[slot]);
            if (taken <= 0) return ItemStack.EMPTY;
            ItemStack result = copyWithCount(prototypes[slot], taken);
            if (!simulate) {
                counts[slot] -= taken;
                if (counts[slot] <= 0L) clearSlot(slot);
                markChanged();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot >= 0 && slot < MAX_SLOTS ? 64 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isItemValid(slot, stack, false, MAX_SLOTS);
        }

        public boolean isItemValid(int slot, ItemStack stack, int activeSlots) {
            return isItemValid(slot, stack, false, activeSlots);
        }

        public boolean isItemValid(int slot, ItemStack stack, boolean autoSort, int activeSlots) {
            int capacity = clampCapacity(activeSlots);
            if (slot < 0 || slot >= capacity || !isNewContentAllowed(stack)) return false;
            if (autoSort) {
                return findLargestMatchingSlotWithRoom(stack, capacity) >= 0
                        || findFirstEmptySlot(capacity) >= 0;
            }
            return !isOccupied(slot)
                    || areSameType(prototypes[slot], stack) && counts[slot] < Long.MAX_VALUE;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 0 || slot >= MAX_SLOTS) return;
            if (stack.isEmpty()) {
                if (!isOccupied(slot)) return;
                clearSlot(slot);
                markChanged();
                return;
            }
            if (!isNewContentAllowed(stack)) return;

            if (isOccupied(slot) && !areSameType(prototypes[slot], stack)) return;

            long desired = Math.max(0, stack.getCount());
            if (desired == counts[slot] && isOccupied(slot)) return;
            bind(slot, stack);
            counts[slot] = desired;
            markChanged();
        }

        /**
         * Snapshots one bounded overflow batch before capacity is reduced.
         * Repeated ticks drain larger long counts without allocating an unsafe
         * number of ItemStacks. Returned stacks retain exact item NBT.
         */
        public ShrinkTransaction prepareShrink(int activeSlots) {
            int capacity = clampCapacity(activeSlots);
            List<ShrinkEntry> removed = new ArrayList<>();
            List<ItemStack> drops = new ArrayList<>();
            for (int slot = capacity; slot < MAX_SLOTS; slot++) {
                if (drops.size() >= MAX_DROP_STACKS) break;
                if (!isOccupied(slot)) continue;
                ItemStack prototype = prototypes[slot];
                long count = counts[slot];
                int legalStack = Math.max(1, Math.min(64, prototype.getMaxStackSize()));
                long stackBudget = MAX_DROP_STACKS - drops.size();
                long removable = Math.min(count, stackBudget * legalStack);
                if (removable <= 0L) break;
                removed.add(new ShrinkEntry(slot, prototype, count, removable));
                long remaining = removable;
                while (remaining > 0L) {
                    int amount = (int) Math.min((long) legalStack, remaining);
                    drops.add(copyWithCount(prototype, amount));
                    remaining -= amount;
                }
            }
            return new ShrinkTransaction(this, capacity, revision, removed, drops, true);
        }

        // ---- helpers -------------------------------------------------------

        private static boolean isNewContentAllowed(ItemStack stack) {
            return !stack.isEmpty() && !ItemBackpack.isForbiddenBackpackContent(stack);
        }

        private int findLargestMatchingSlotWithRoom(ItemStack stack, int activeSlots) {
            if (stack.isEmpty()) return -1;
            int capacity = clampCapacity(activeSlots);
            int bestSlot = -1;
            long bestCount = -1L;
            for (int slot = 0; slot < capacity; slot++) {
                if (isOccupied(slot) && counts[slot] < Long.MAX_VALUE && counts[slot] > bestCount
                        && areSameType(prototypes[slot], stack)) {
                    bestSlot = slot;
                    bestCount = counts[slot];
                }
            }
            return bestSlot;
        }

        private int findFirstEmptySlot(int activeSlots) {
            int capacity = clampCapacity(activeSlots);
            for (int slot = 0; slot < capacity; slot++) {
                if (!isOccupied(slot)) return slot;
            }
            return -1;
        }

        private void bind(int slot, ItemStack stack) {
            if (isOccupied(slot) || stack.isEmpty()) return;
            ItemStack proto = stack.copy();
            proto.setCount(1);
            prototypes[slot] = proto;
        }

        private void clearSlot(int slot) {
            counts[slot] = 0L;
            prototypes[slot] = ItemStack.EMPTY;
        }

        private static final class SortEntry {
            private final ItemStack prototype;
            private long count;

            private SortEntry(ItemStack prototype, long count) {
                this.prototype = prototype;
                this.count = count;
            }
        }

        private static int clampCapacity(int slots) {
            return Math.max(0, Math.min(MAX_SLOTS, slots));
        }

        private void markChanged() {
            revision++;
            changed.run();
        }

        // ---- serialization -------------------------------------------------

        private NBTTagCompound serialize() {
            NBTTagCompound serialized = new NBTTagCompound();
            NBTTagList slotList = new NBTTagList();
            for (int slot = 0; slot < MAX_SLOTS; slot++) {
                if (!isOccupied(slot)) continue;
                NBTTagCompound slotTag = new NBTTagCompound();
                slotTag.setInteger("slot", slot);
                slotTag.setLong("count", counts[slot]);
                slotTag.setTag("prototype", prototypes[slot].serializeNBT());
                slotList.appendTag(slotTag);
            }
            serialized.setTag("slots", slotList);
            return serialized;
        }

        private boolean deserialize(NBTTagCompound serialized) {
            Arrays.fill(prototypes, ItemStack.EMPTY);
            Arrays.fill(counts, 0L);
            boolean any = false;

            // Legacy layout: the whole backpack shared one prototype and count.
            // Load it into the first slot so existing worlds keep their contents.
            if (!serialized.hasKey("slots", Constants.NBT.TAG_LIST)
                    && serialized.hasKey("prototype", Constants.NBT.TAG_COMPOUND)) {
                ItemStack legacyPrototype = new ItemStack(serialized.getCompoundTag("prototype"));
                long legacyCount = serialized.getLong("count");
                if (legacyCount > 0L && !legacyPrototype.isEmpty()) {
                    legacyPrototype.setCount(1);
                    prototypes[0] = legacyPrototype;
                    counts[0] = legacyCount;
                    markChanged();
                    return true;
                }
                return false;
            }

            NBTTagList slotList = serialized.getTagList("slots", Constants.NBT.TAG_COMPOUND);
            boolean migrated = false;
            // Process physical slots in numeric order and preserve their exact
            // layout. Duplicate variants in different cells are intentional in
            // manual mode and must survive a save/reload cycle.
            for (int requestedSlot = 0; requestedSlot < MAX_SLOTS; requestedSlot++) {
                for (int index = 0; index < slotList.tagCount(); index++) {
                    NBTTagCompound slotTag = slotList.getCompoundTagAt(index);
                    if (slotTag.getInteger("slot") != requestedSlot) continue;

                    long count = slotTag.getLong("count");
                    ItemStack proto = new ItemStack(slotTag.getCompoundTag("prototype"));
                    if (count <= 0L || proto.isEmpty()) continue;
                    proto.setCount(1);

                    int destination = !isOccupied(requestedSlot)
                            || areSameType(prototypes[requestedSlot], proto)
                            ? requestedSlot : findFirstEmptySlot(MAX_SLOTS);
                    if (destination < 0) continue;
                    if (destination != requestedSlot) migrated = true;

                    if (!isOccupied(destination)) {
                        prototypes[destination] = proto;
                        counts[destination] = count;
                    } else {
                        counts[destination] = saturatingAdd(counts[destination], count);
                    }
                    any = true;
                }
            }
            if (migrated) markChanged();
            return any;
        }

        public static boolean areSameType(ItemStack first, ItemStack second) {
            if (first.isEmpty() || second.isEmpty()) return false;
            ItemStack a = first.copy();
            ItemStack b = second.copy();
            a.setCount(1);
            b.setCount(1);
            return a.serializeNBT().equals(b.serializeNBT());
        }

        private static long saturatingAdd(long first, long second) {
            if (second <= 0L) return first;
            return second > Long.MAX_VALUE - first ? Long.MAX_VALUE : first + second;
        }

        private static ItemStack copyWithCount(ItemStack stack, int count) {
            if (count <= 0 || stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack copy = stack.copy();
            copy.setCount(count);
            return copy;
        }

        private static final class ShrinkEntry {
            private final int slot;
            private final ItemStack prototype;
            private final long count;
            private final long removedCount;

            private ShrinkEntry(int slot, ItemStack prototype, long count, long removedCount) {
                this.slot = slot;
                this.prototype = prototype.copy();
                this.prototype.setCount(1);
                this.count = count;
                this.removedCount = Math.max(0L, Math.min(count, removedCount));
            }
        }

        public static final class ShrinkTransaction {
            private final PocketHoleStorage storage;
            private final int capacity;
            private final long preparedRevision;
            private final List<ShrinkEntry> removed;
            private final List<ItemStack> drops;
            private final boolean prepared;
            private boolean committed;
            private long committedRevision;

            private ShrinkTransaction(PocketHoleStorage storage, int capacity, long preparedRevision,
                                      List<ShrinkEntry> removed, List<ItemStack> drops, boolean prepared) {
                this.storage = storage;
                this.capacity = capacity;
                this.preparedRevision = preparedRevision;
                this.removed = removed;
                this.drops = drops;
                this.prepared = prepared;
            }

            private static ShrinkTransaction unavailable(PocketHoleStorage storage, int capacity, long revision) {
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
                for (ShrinkEntry entry : removed) {
                    if (entry.slot < capacity || entry.slot >= MAX_SLOTS
                            || !storage.isOccupied(entry.slot)
                            || storage.counts[entry.slot] != entry.count
                            || !areSameType(storage.prototypes[entry.slot], entry.prototype)) {
                        return false;
                    }
                }
                for (ShrinkEntry entry : removed) {
                    if (entry.removedCount >= storage.counts[entry.slot]) {
                        storage.clearSlot(entry.slot);
                    } else {
                        storage.counts[entry.slot] -= entry.removedCount;
                    }
                }
                if (!removed.isEmpty()) storage.markChanged();
                committed = true;
                committedRevision = storage.revision;
                return true;
            }

            public boolean rollback() {
                if (!committed || storage.revision != committedRevision) return false;
                for (ShrinkEntry entry : removed) {
                    storage.prototypes[entry.slot] = entry.prototype.copy();
                    storage.counts[entry.slot] = entry.count;
                }
                if (!removed.isEmpty()) storage.markChanged();
                committed = false;
                return true;
            }
        }
    }
}
