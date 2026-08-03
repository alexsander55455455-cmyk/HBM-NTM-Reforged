package com.hbm.saveddata;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PocketHoleBackpackSavedDataTest {

    @BeforeAll
    static void registerVanillaItems() {
        if (!Bootstrap.isRegistered()) Bootstrap.register();
    }

    @Test
    void activeCapacityGatesExpandedCells() {
        PocketHoleBackpackSavedData data = new PocketHoleBackpackSavedData();
        PocketHoleBackpackSavedData.PocketHoleStorage storage = data.getStorage(UUID.randomUUID());

        assertTrue(storage.insertItem(8, variant("base", 8), false, 9).isEmpty());
        ItemStack rejected = storage.insertItem(9, variant("expanded", 9), false, 9);
        assertFalse(rejected.isEmpty());
        assertTrue(storage.insertItem(9, variant("expanded", 9), false, 36).isEmpty());
        assertEquals(9L, storage.getSlotCount(9));
    }

    @Test
    void shrinkTransactionSplitsExactNbtAndCanRollback() {
        PocketHoleBackpackSavedData data = new PocketHoleBackpackSavedData();
        PocketHoleBackpackSavedData.PocketHoleStorage storage = data.getStorage(UUID.randomUUID());
        ItemStack stored = variant("tail", 130);
        assertTrue(storage.insertItem(9, stored, false, 36).isEmpty());

        PocketHoleBackpackSavedData.PocketHoleStorage.ShrinkTransaction transaction = storage.prepareShrink(9);
        assertTrue(transaction.isPrepared());
        List<ItemStack> drops = transaction.getDrops();
        assertEquals(3, drops.size());
        assertEquals(130L, drops.stream().mapToLong(ItemStack::getCount).sum());
        assertTrue(drops.stream().allMatch(stack ->
                PocketHoleBackpackSavedData.PocketHoleStorage.areSameType(stored, stack)));

        assertTrue(transaction.commit());
        assertEquals(0L, storage.getSlotCount(9));
        assertTrue(transaction.rollback());
        assertEquals(130L, storage.getSlotCount(9));
        assertTrue(PocketHoleBackpackSavedData.PocketHoleStorage.areSameType(
                stored, storage.getSlotPrototype(9)));
    }

    @Test
    void highestExpandedSlotSurvivesSerialization() {
        UUID id = UUID.randomUUID();
        PocketHoleBackpackSavedData original = new PocketHoleBackpackSavedData();
        PocketHoleBackpackSavedData.PocketHoleStorage storage = original.getStorage(id);
        ItemStack stored = variant("highest", 17);
        int highestSlot = PocketHoleBackpackSavedData.PocketHoleStorage.MAX_SLOTS - 1;
        assertTrue(storage.insertItem(highestSlot, stored, false,
                PocketHoleBackpackSavedData.PocketHoleStorage.MAX_SLOTS).isEmpty());

        NBTTagCompound serialized = original.writeToNBT(new NBTTagCompound());
        PocketHoleBackpackSavedData reloaded = new PocketHoleBackpackSavedData();
        reloaded.readFromNBT(serialized);
        PocketHoleBackpackSavedData.PocketHoleStorage restored = reloaded.getStorage(id);

        assertEquals(17L, restored.getSlotCount(highestSlot));
        assertTrue(PocketHoleBackpackSavedData.PocketHoleStorage.areSameType(
                stored, restored.getSlotPrototype(highestSlot)));
    }

    @Test
    void manualDuplicatesKeepTheirExactSlotsAcrossReload() {
        UUID id = UUID.randomUUID();
        PocketHoleBackpackSavedData data = new PocketHoleBackpackSavedData();
        PocketHoleBackpackSavedData.PocketHoleStorage storage = data.getStorage(id);
        ItemStack paper = variant("manual", 1);

        assertTrue(storage.insertItem(2, withCount(paper, 16), false, 36).isEmpty());
        assertTrue(storage.insertItem(7, withCount(paper, 53), false, 36).isEmpty());
        assertTrue(storage.insertItem(20, withCount(paper, 78), false, 36).isEmpty());

        PocketHoleBackpackSavedData reloaded = new PocketHoleBackpackSavedData();
        reloaded.readFromNBT(data.writeToNBT(new NBTTagCompound()));
        PocketHoleBackpackSavedData.PocketHoleStorage restored = reloaded.getStorage(id);
        assertEquals(16L, restored.getSlotCount(2));
        assertEquals(53L, restored.getSlotCount(7));
        assertEquals(78L, restored.getSlotCount(20));
        assertEquals(3, restored.getOccupiedSlots(36));
    }

    @Test
    void automaticInsertionUsesLargestMatchingStackAndKeepsOtherNbtSeparate() {
        PocketHoleBackpackSavedData data = new PocketHoleBackpackSavedData();
        PocketHoleBackpackSavedData.PocketHoleStorage storage = data.getStorage(UUID.randomUUID());
        ItemStack paper = variant("largest", 1);
        ItemStack tie = new ItemStack(Items.BONE);
        tie.setTagCompound(paper.getTagCompound().copy());
        ItemStack other = variant("other-nbt", 5);

        assertTrue(storage.insertItem(2, withCount(paper, 16), false, 36).isEmpty());
        assertTrue(storage.insertItem(7, withCount(paper, 53), false, 36).isEmpty());
        assertTrue(storage.insertItem(20, withCount(paper, 78), false, 36).isEmpty());
        assertTrue(storage.insertItemAnywhere(withCount(paper, 64), false, 36).isEmpty());
        assertEquals(142L, storage.getSlotCount(20));
        assertEquals(16L, storage.getSlotCount(2));
        assertEquals(53L, storage.getSlotCount(7));

        assertTrue(storage.insertItem(1, withCount(tie, 16), false, 36).isEmpty());
        assertTrue(storage.insertItem(6, withCount(tie, 16), false, 36).isEmpty());
        assertTrue(storage.insertItemAnywhere(withCount(tie, 8), false, 36).isEmpty());
        assertEquals(24L, storage.getSlotCount(1));
        assertEquals(16L, storage.getSlotCount(6));

        assertTrue(storage.insertItemAnywhere(other, false, 36).isEmpty());
        assertEquals(5L, storage.getSlotCount(0));
        assertTrue(PocketHoleBackpackSavedData.PocketHoleStorage.areSameType(
                other, storage.getSlotPrototype(0)));
    }

    @Test
    void simulationDoesNotMutateAndSortMergesDuplicatesWithoutLosingCounts() {
        PocketHoleBackpackSavedData data = new PocketHoleBackpackSavedData();
        PocketHoleBackpackSavedData.PocketHoleStorage storage = data.getStorage(UUID.randomUUID());
        ItemStack paper = variant("sort", 1);

        assertTrue(storage.insertItem(3, withCount(paper, 20), true, 36).isEmpty());
        assertEquals(0L, storage.getSlotCount(3));
        assertTrue(storage.insertItem(3, withCount(paper, 20), false, 36).isEmpty());
        assertTrue(storage.insertItem(15, withCount(paper, 30), false, 36).isEmpty());
        storage.sortContents(36);

        assertEquals(50L, storage.getSlotCount(0));
        assertEquals(1, storage.getOccupiedSlots(36));
        assertEquals(50L, storage.getTotalCount());
    }

    @Test
    void fullLargestStackFallsBackWithoutOverflowing() {
        UUID id = UUID.randomUUID();
        ItemStack paper = variant("full", 1);
        PocketHoleBackpackSavedData data = new PocketHoleBackpackSavedData();
        data.readFromNBT(serializedStorage(id, paper, Long.MAX_VALUE));
        PocketHoleBackpackSavedData.PocketHoleStorage storage = data.getStorage(id);

        assertTrue(storage.insertItemAnywhere(withCount(paper, 64), false, 9).isEmpty());
        assertEquals(Long.MAX_VALUE, storage.getSlotCount(0));
        assertEquals(64L, storage.getSlotCount(1));
    }

    @Test
    void backendMigrationClearRemovesEveryLongCountOnlyAfterItCanBeCopied() {
        PocketHoleBackpackSavedData.PocketHoleStorage oldStorage =
                new PocketHoleBackpackSavedData().getStorage(UUID.randomUUID());
        ItemStack first = variant("first", 1);
        ItemStack second = variant("second", 1);
        assertTrue(oldStorage.insertItem(4, withCount(first, 64), false, 9).isEmpty());
        assertTrue(oldStorage.insertItem(7, withCount(second, 53), false, 9).isEmpty());

        BlackHoleBackpackSavedData.BackpackStorage migrated =
                new BlackHoleBackpackSavedData().getStorage(UUID.randomUUID());
        for (int slot = 0; slot < PocketHoleBackpackSavedData.PocketHoleStorage.MAX_SLOTS; slot++) {
            migrated.importStoredCount(slot, oldStorage.getSlotPrototype(slot), oldStorage.getSlotCount(slot));
        }
        oldStorage.clearForBackendMigration();

        assertTrue(oldStorage.isEmpty());
        assertEquals(64L, migrated.getSlotCount(4));
        assertEquals(53L, migrated.getSlotCount(7));
    }

    private static NBTTagCompound serializedStorage(UUID id, ItemStack prototype, long count) {
        ItemStack normalized = prototype.copy();
        normalized.setCount(1);
        NBTTagCompound slot = new NBTTagCompound();
        slot.setInteger("slot", 0);
        slot.setLong("count", count);
        slot.setTag("prototype", normalized.serializeNBT());
        NBTTagList slots = new NBTTagList();
        slots.appendTag(slot);
        NBTTagCompound storage = new NBTTagCompound();
        storage.setUniqueId("id", id);
        storage.setTag("slots", slots);
        NBTTagList storages = new NBTTagList();
        storages.appendTag(storage);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("storages", storages);
        return root;
    }

    private static ItemStack withCount(ItemStack prototype, int count) {
        ItemStack stack = prototype.copy();
        stack.setCount(count);
        return stack;
    }

    private static ItemStack variant(String marker, int count) {
        ItemStack stack = new ItemStack(Items.PAPER, count);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("marker", marker);
        stack.setTagCompound(tag);
        return stack;
    }
}
