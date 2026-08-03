package com.hbm.saveddata;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackHoleBackpackSavedDataTest {

    @BeforeAll
    static void registerVanillaItems() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void legacyStacksMigrateToEarliestCanonicalLongEntryAndRoundTripAsV2() {
        UUID id = UUID.randomUUID();
        ItemStack orangePaper = variant(Items.PAPER, 1, "orange");
        ItemStack purplePaper = variant(Items.PAPER, 1, "purple");

        NBTTagList legacyItems = new NBTTagList();
        appendLegacyStack(legacyItems, 7, withCount(orangePaper, 64));
        appendLegacyStack(legacyItems, 1, withCount(orangePaper, 32));
        appendLegacyStack(legacyItems, 4, withCount(purplePaper, 11));
        appendLegacyStack(legacyItems, 100, withCount(orangePaper, 5));

        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        data.readFromNBT(rootWithStorage(legacyStorage(id, legacyItems)));

        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(id);
        assertEquals(2, storage.getFilledSlotCount());
        assertEquals(101L, storage.getSlotCount(1));
        assertEquals(11L, storage.getSlotCount(4));
        assertEquals(0L, storage.getSlotCount(7));
        assertEquals(0L, storage.getSlotCount(100));
        assertSameType(orangePaper, storage.getStackInSlot(1));
        assertSameType(purplePaper, storage.getStackInSlot(4));
        assertEquals(1, storage.getStackInSlot(1).getCount());

        NBTTagCompound written = data.writeToNBT(new NBTTagCompound());
        NBTTagCompound serializedStorage = findStorage(written, id);
        assertEquals(3, serializedStorage.getInteger("format"));

        NBTTagList entries = serializedStorage.getTagList("entries", Constants.NBT.TAG_COMPOUND);
        assertEquals(2, entries.tagCount());
        NBTTagCompound orangeEntry = findEntry(entries, 1);
        assertEquals(101L, orangeEntry.getLong("count"));
        assertEquals(1, new ItemStack(orangeEntry.getCompoundTag("prototype")).getCount());

        BlackHoleBackpackSavedData reloaded = new BlackHoleBackpackSavedData();
        reloaded.readFromNBT(written);
        BlackHoleBackpackSavedData.BackpackStorage reloadedStorage = reloaded.getStorage(id);
        assertEquals(2, reloadedStorage.getFilledSlotCount());
        assertEquals(101L, reloadedStorage.getSlotCount(1));
        assertEquals(11L, reloadedStorage.getSlotCount(4));
        assertSameType(orangePaper, reloadedStorage.getStackInSlot(1));
        assertSameType(purplePaper, reloadedStorage.getStackInSlot(4));
    }

    @Test
    void supportsMoreThanOnePageOfDistinctTypesAndCanonicalMerging() {
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(UUID.randomUUID());

        for (int variant = 0; variant < 200; variant++) {
            ItemStack stack = numberedVariant(Items.PAPER, variant, variant % 64 + 1);
            assertTrue(storage.insertStackAnywhere(stack, false, false).isEmpty());
        }

        assertEquals(200, storage.getFilledSlotCount());
        assertEquals(273, storage.getSlots());
        assertEquals(0, storage.getSlots() % BlackHoleBackpackSavedData.BackpackStorage.ROW_WIDTH);
        for (int variant = 0; variant < 200; variant++) {
            assertEquals(variant % 64 + 1L, storage.getSlotCount(variant));
            assertSameType(numberedVariant(Items.PAPER, variant, 1), storage.getStackInSlot(variant));
        }

        ItemStack additional = numberedVariant(Items.PAPER, 42, 13);
        assertTrue(storage.insertStackAnywhere(additional, false, true).isEmpty());
        int canonicalSlot = findSlot(storage, numberedVariant(Items.PAPER, 42, 1));
        assertTrue(canonicalSlot >= 0);
        assertEquals(42 % 64 + 1L + 13L, storage.getSlotCount(canonicalSlot));
        assertEquals(200, storage.getFilledSlotCount());
    }

    @Test
    void saturatesAtLongMaxAndReturnsTheUnstoredRemainder() {
        UUID id = UUID.randomUUID();
        ItemStack prototype = variant(Items.PAPER, 1, "capacity");
        NBTTagList entries = new NBTTagList();
        appendV2Entry(entries, 3, prototype, Long.MAX_VALUE - 10L);

        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        data.readFromNBT(rootWithStorage(v2Storage(id, 7L, entries)));
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(id);

        ItemStack remainder = storage.insertItem(40, withCount(prototype, 64), false, true);
        assertFalse(remainder.isEmpty());
        assertEquals(54, remainder.getCount());
        assertEquals(Long.MAX_VALUE, storage.getSlotCount(0));
        assertEquals(0L, storage.getSlotCount(3));
        assertEquals(0L, storage.getSlotCount(40));

        ItemStack extracted = storage.extractItem(0, 64, false);
        assertSameType(prototype, extracted);
        assertEquals(64, extracted.getCount());
        assertEquals(Long.MAX_VALUE - 64L, storage.getSlotCount(0));

        assertTrue(storage.insertItem(100, withCount(prototype, 64), false, true).isEmpty());
        assertEquals(Long.MAX_VALUE, storage.getSlotCount(0));
    }

    @Test
    void explicitSortIsDeterministicAndConservesCounts() {
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(UUID.randomUUID());
        ItemStack apple = new ItemStack(Items.APPLE);
        ItemStack bone = new ItemStack(Items.BONE);
        ItemStack paper = new ItemStack(Items.PAPER);

        assertTrue(storage.insertItem(50, withCount(bone, 5), false, false).isEmpty());
        assertTrue(storage.insertItem(5, withCount(apple, 7), false, false).isEmpty());
        assertTrue(storage.insertItem(90, withCount(paper, 9), false, false).isEmpty());
        assertTrue(storage.insertItem(120, withCount(apple, 3), false, false).isEmpty());
        assertEquals(4, storage.getFilledSlotCount());
        assertEquals(24L, totalCount(storage));

        storage.sort();

        assertEquals(3, storage.getFilledSlotCount());
        assertEquals(24L, totalCount(storage));
        assertSameType(apple, storage.getStackInSlot(0));
        assertEquals(10L, storage.getSlotCount(0));
        assertSameType(bone, storage.getStackInSlot(1));
        assertEquals(5L, storage.getSlotCount(1));
        assertSameType(paper, storage.getStackInSlot(2));
        assertEquals(9L, storage.getSlotCount(2));

        NBTTagCompound firstSort = data.writeToNBT(new NBTTagCompound());
        storage.sort();
        NBTTagCompound secondSort = data.writeToNBT(new NBTTagCompound());
        assertEquals(
                findStorage(firstSort, findOnlyStorageId(firstSort)).getTagList(
                        "entries", Constants.NBT.TAG_COMPOUND).toString(),
                findStorage(secondSort, findOnlyStorageId(secondSort)).getTagList(
                        "entries", Constants.NBT.TAG_COMPOUND).toString());
    }

    @Test
    void writingDoesNotMutateLiveSparseStorage() {
        UUID id = UUID.randomUUID();
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(id);
        ItemStack paper = new ItemStack(Items.PAPER);
        ItemStack bone = new ItemStack(Items.BONE);

        assertTrue(storage.insertItem(5, withCount(paper, 7), false, false).isEmpty());
        assertTrue(storage.insertItem(120, withCount(bone, 9), false, false).isEmpty());
        assertEquals(9, storage.extractItem(120, 64, false).getCount());

        int allocatedBefore = storage.getAllocatedSlotCount();
        long revisionBefore = storage.getRevision();
        long paperBefore = storage.getSlotCount(5);

        NBTTagCompound written = data.writeToNBT(new NBTTagCompound());

        assertEquals(121, allocatedBefore);
        assertEquals(allocatedBefore, storage.getAllocatedSlotCount());
        assertEquals(revisionBefore, storage.getRevision());
        assertEquals(paperBefore, storage.getSlotCount(5));
        assertEquals(1, findStorage(written, id)
                .getTagList("entries", Constants.NBT.TAG_COMPOUND).tagCount());
    }

    @Test
    void outOfRangeSlotsAreRemappedWhileHugeValidSlotsStaySparse() {
        UUID legacyId = UUID.randomUUID();
        UUID v2Id = UUID.randomUUID();

        NBTTagList legacyItems = new NBTTagList();
        appendLegacyStack(legacyItems, Integer.MAX_VALUE, withCount(new ItemStack(Items.APPLE), 17));

        NBTTagList entries = new NBTTagList();
        appendV2Entry(entries, Integer.MAX_VALUE, new ItemStack(Items.PAPER), 23L);
        appendV2Entry(entries, 999_999, new ItemStack(Items.FEATHER), 19L);
        appendV2Entry(entries,
                BlackHoleBackpackSavedData.BackpackStorage.MAX_LOGICAL_SLOTS - 1,
                new ItemStack(Items.BONE), 29L);

        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        data.readFromNBT(rootWithStorages(
                legacyStorage(legacyId, legacyItems),
                v2Storage(v2Id, 4L, entries)));

        BlackHoleBackpackSavedData.BackpackStorage legacy = data.getStorage(legacyId);
        assertEquals(1, legacy.getAllocatedSlotCount());
        assertEquals(17L, legacy.getSlotCount(0));
        assertSameType(new ItemStack(Items.APPLE), legacy.getStackInSlot(0));

        BlackHoleBackpackSavedData.BackpackStorage v2 = data.getStorage(v2Id);
        assertEquals(3, v2.getFilledSlotCount());
        assertEquals(BlackHoleBackpackSavedData.BackpackStorage.MAX_LOGICAL_SLOTS,
                v2.getAllocatedSlotCount());
        assertEquals(23L, v2.getSlotCount(0));
        assertEquals(19L, v2.getSlotCount(999_999));
        assertEquals(29L, v2.getSlotCount(
                BlackHoleBackpackSavedData.BackpackStorage.MAX_LOGICAL_SLOTS - 1));
        assertSameType(new ItemStack(Items.PAPER), v2.getStackInSlot(0));
        assertSameType(new ItemStack(Items.FEATHER), v2.getStackInSlot(999_999));
        assertSameType(new ItemStack(Items.BONE), v2.getStackInSlot(
                BlackHoleBackpackSavedData.BackpackStorage.MAX_LOGICAL_SLOTS - 1));
    }

    @Test
    void sparseCanonicalSlotSurvivesAnchorRemovalAndRoundTrip() {
        UUID id = UUID.randomUUID();
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(id);

        assertTrue(storage.insertItem(142, new ItemStack(Items.APPLE), false, false).isEmpty());
        assertEquals(208, storage.getSlots());
        assertTrue(storage.insertItem(207, new ItemStack(Items.BONE), false, false).isEmpty());
        assertEquals(273, storage.getSlots());
        assertEquals(1, storage.extractItem(142, 64, false, false).getCount());
        assertEquals(208, storage.getAllocatedSlotCount());

        NBTTagCompound written = data.writeToNBT(new NBTTagCompound());
        BlackHoleBackpackSavedData reloaded = new BlackHoleBackpackSavedData();
        reloaded.readFromNBT(written);
        BlackHoleBackpackSavedData.BackpackStorage restored = reloaded.getStorage(id);

        assertEquals(208, restored.getAllocatedSlotCount());
        assertEquals(273, restored.getSlots());
        assertEquals(1L, restored.getSlotCount(207));
        assertSameType(new ItemStack(Items.BONE), restored.getStackInSlot(207));
        assertTrue(restored.getStackInSlot(0).isEmpty());
    }

    @Test
    void manualModePreservesDuplicateVariantsAndExactSlotsAcrossV3Reload() {
        UUID id = UUID.randomUUID();
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(id);
        ItemStack paper = variant(Items.PAPER, 1, "manual");

        assertTrue(storage.insertItem(4, withCount(paper, 20), false, false).isEmpty());
        assertTrue(storage.insertItem(31, withCount(paper, 30), false, false).isEmpty());
        assertTrue(storage.insertStackAnywhere(withCount(paper, 40), false, false).isEmpty());
        assertEquals(0L, storage.getSlotCount(0));
        assertEquals(20L, storage.getSlotCount(4));
        assertEquals(70L, storage.getSlotCount(31));
        assertEquals(2, storage.getFilledSlotCount());

        NBTTagCompound written = data.writeToNBT(new NBTTagCompound());
        assertEquals(3, findStorage(written, id).getInteger("format"));
        BlackHoleBackpackSavedData reloaded = new BlackHoleBackpackSavedData();
        reloaded.readFromNBT(written);
        BlackHoleBackpackSavedData.BackpackStorage restored = reloaded.getStorage(id);

        assertEquals(0L, restored.getSlotCount(0));
        assertEquals(20L, restored.getSlotCount(4));
        assertEquals(70L, restored.getSlotCount(31));
        assertEquals(2, restored.getFilledSlotCount());
        assertEquals(90L, totalCount(restored));
    }

    @Test
    void automaticInsertionUsesLargestMatchingStackAndLowestSlotForTies() {
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(UUID.randomUUID());
        ItemStack paper = variant(Items.PAPER, 1, "largest");
        ItemStack tie = variant(Items.BONE, 1, "tie");
        ItemStack other = variant(Items.PAPER, 1, "other-nbt");

        assertTrue(storage.insertItem(2, withCount(paper, 16), false, false).isEmpty());
        assertTrue(storage.insertItem(7, withCount(paper, 53), false, false).isEmpty());
        assertTrue(storage.insertItem(20, withCount(paper, 78), false, false).isEmpty());
        assertTrue(storage.insertStackAnywhere(withCount(paper, 64), false, false).isEmpty());
        assertEquals(142L, storage.getSlotCount(20));
        assertEquals(16L, storage.getSlotCount(2));
        assertEquals(53L, storage.getSlotCount(7));

        assertTrue(storage.insertItem(1, withCount(tie, 16), false, false).isEmpty());
        assertTrue(storage.insertItem(6, withCount(tie, 16), false, false).isEmpty());
        assertTrue(storage.insertStackAnywhere(withCount(tie, 8), false, false).isEmpty());
        assertEquals(24L, storage.getSlotCount(1));
        assertEquals(16L, storage.getSlotCount(6));

        assertTrue(storage.insertStackAnywhere(withCount(other, 5), false, false).isEmpty());
        assertEquals(5L, storage.getSlotCount(0));
        assertSameType(other, storage.getSlotPrototype(0));
    }

    @Test
    void enablingSortUsesDeterministicRegistryOrderAndDisablingLeavesPositionsAlone() {
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(UUID.randomUUID());
        ItemStack paper = new ItemStack(Items.PAPER);
        ItemStack bone = new ItemStack(Items.BONE);
        ItemStack apple = new ItemStack(Items.APPLE);

        assertTrue(storage.insertItem(2, withCount(bone, 5), false, false).isEmpty());
        assertTrue(storage.insertItem(7, withCount(paper, 11), false, false).isEmpty());
        assertTrue(storage.insertItem(20, withCount(paper, 13), false, false).isEmpty());
        assertTrue(storage.insertItem(80, withCount(apple, 17), false, false).isEmpty());

        storage.compactAndMergeStable();
        assertSameType(apple, storage.getStackInSlot(0));
        assertEquals(17L, storage.getSlotCount(0));
        assertSameType(bone, storage.getStackInSlot(1));
        assertEquals(5L, storage.getSlotCount(1));
        assertSameType(paper, storage.getStackInSlot(2));
        assertEquals(24L, storage.getSlotCount(2));
        assertEquals(3, storage.getFilledSlotCount());

        long revisionAfterFirstSort = storage.getRevision();
        storage.compactAndMergeStable();
        assertEquals(revisionAfterFirstSort, storage.getRevision());

        assertEquals(24, storage.extractItem(2, 64, false, false).getCount());
        assertTrue(storage.getStackInSlot(2).isEmpty());
        assertSameType(apple, storage.getStackInSlot(0));
        assertSameType(bone, storage.getStackInSlot(1));
    }

    @Test
    void sortedHandleRepairsManualChangesMadeThroughAnotherHandleOfTheSameStorage() {
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(UUID.randomUUID());
        ItemStack paper = new ItemStack(Items.PAPER);
        ItemStack bone = new ItemStack(Items.BONE);
        ItemStack apple = new ItemStack(Items.APPLE);

        assertTrue(storage.insertItem(9, withCount(paper, 20), false, false).isEmpty());
        assertTrue(storage.insertItem(30, withCount(paper, 30), false, false).isEmpty());
        assertTrue(storage.insertItem(80, withCount(bone, 7), false, false).isEmpty());

        // A sorted operation through another physical copy sharing this UUID
        // must first restore the complete sorted invariant.
        assertTrue(storage.insertStackAnywhere(withCount(apple, 5), false, true).isEmpty());
        assertEquals(3, storage.getFilledSlotCount());
        assertSameType(apple, storage.getStackInSlot(0));
        assertEquals(5L, storage.getSlotCount(0));
        assertSameType(bone, storage.getStackInSlot(1));
        assertEquals(7L, storage.getSlotCount(1));
        assertSameType(paper, storage.getStackInSlot(2));
        assertEquals(50L, storage.getSlotCount(2));
        assertEquals(62L, totalCount(storage));
    }

    @Test
    void deterministicSortKeepsExactVariantsSeparateMergesTheirDuplicatesAndIsIdempotent() {
        UUID id = UUID.randomUUID();
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(id);
        ItemStack alphaPaper = variant(Items.PAPER, 1, "alpha");
        ItemStack betaPaper = variant(Items.PAPER, 1, "beta");
        ItemStack apple = new ItemStack(Items.APPLE);

        assertTrue(storage.insertItem(90, withCount(betaPaper, 7), false, false).isEmpty());
        assertTrue(storage.insertItem(4, withCount(alphaPaper, 11), false, false).isEmpty());
        assertTrue(storage.insertItem(70, withCount(betaPaper, 13), false, false).isEmpty());
        assertTrue(storage.insertItem(25, withCount(alphaPaper, 17), false, false).isEmpty());
        assertTrue(storage.insertItem(120, withCount(apple, 5), false, false).isEmpty());
        assertEquals(53L, totalCount(storage));

        storage.compactAndMergeStable();

        assertEquals(3, storage.getFilledSlotCount());
        assertEquals(53L, totalCount(storage));
        assertSameType(apple, storage.getStackInSlot(0));
        assertEquals(5L, storage.getSlotCount(0));
        assertSameType(alphaPaper, storage.getStackInSlot(1));
        assertEquals(28L, storage.getSlotCount(1));
        assertSameType(betaPaper, storage.getStackInSlot(2));
        assertEquals(20L, storage.getSlotCount(2));

        long revisionAfterFirstSort = storage.getRevision();
        NBTTagCompound firstSort = data.writeToNBT(new NBTTagCompound());
        storage.compactAndMergeStable();
        NBTTagCompound secondSort = data.writeToNBT(new NBTTagCompound());

        assertEquals(revisionAfterFirstSort, storage.getRevision());
        assertEquals(
                findStorage(firstSort, id).getTagList("entries", Constants.NBT.TAG_COMPOUND).toString(),
                findStorage(secondSort, id).getTagList("entries", Constants.NBT.TAG_COMPOUND).toString());
        assertEquals(53L, totalCount(storage));
    }

    @Test
    void sortedExtractionCompactsTheGapAndAllCapacitiesHaveFullThirteenSlotRows() {
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(UUID.randomUUID());

        assertEquals(143, storage.getSlots());
        assertEquals(0, storage.getSlots() % 13);
        assertTrue(storage.insertItem(142, new ItemStack(Items.PAPER), false, false).isEmpty());
        assertEquals(208, storage.getSlots());
        assertEquals(0, storage.getSlots() % 13);
        assertTrue(storage.insertItem(207, new ItemStack(Items.BONE), false, false).isEmpty());
        assertEquals(273, storage.getSlots());
        assertEquals(0, storage.getSlots() % 13);

        storage.compactAndMergeStable();
        assertSameType(new ItemStack(Items.BONE), storage.getStackInSlot(0));
        assertSameType(new ItemStack(Items.PAPER), storage.getStackInSlot(1));
        assertEquals(1, storage.extractItem(0, 64, false, true).getCount());
        assertSameType(new ItemStack(Items.PAPER), storage.getStackInSlot(0));
        assertTrue(storage.getStackInSlot(1).isEmpty());
        assertEquals(0, storage.getSlots() % 13);
        assertEquals(0, BlackHoleBackpackSavedData.BackpackStorage.MAX_LOGICAL_SLOTS % 13);
    }

    @Test
    void simulatedManualAndSortedOperationsNeverMutateStorage() {
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(UUID.randomUUID());
        ItemStack paper = new ItemStack(Items.PAPER, 32);

        long revision = storage.getRevision();
        assertTrue(storage.insertItem(30, paper, true, false).isEmpty());
        assertEquals(revision, storage.getRevision());
        assertEquals(0, storage.getFilledSlotCount());

        assertTrue(storage.insertItem(30, paper, false, false).isEmpty());
        long storedRevision = storage.getRevision();
        assertEquals(32, storage.extractItem(30, 64, true, true).getCount());
        assertEquals(storedRevision, storage.getRevision());
        assertEquals(32L, storage.getSlotCount(30));
    }

    @Test
    void boundedInsertionNeverUsesSlotsOutsideInstalledCapacity() {
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(UUID.randomUUID());
        for (int slot = 0; slot < 3; slot++) {
            assertTrue(storage.insertItem(slot, numberedVariant(Items.PAPER, slot, 1),
                    false, false, 3).isEmpty());
        }

        ItemStack rejected = numberedVariant(Items.PAPER, 99, 1);
        assertSameType(rejected, storage.insertStackAnywhere(rejected, false, false, 3));
        assertSameType(rejected, storage.insertItem(3, rejected, false, false, 3));
        assertFalse(storage.isItemValid(3, rejected, false, 3));
        assertEquals(3, storage.getFilledSlotCount());
    }

    @Test
    void shrinkTransactionSplitsOneThousandItemsIntoLegalStacksAndCanRollback() {
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(UUID.randomUUID());
        ItemStack prototype = variant(Items.PAPER, 1, "kept-nbt");
        for (int remaining = 1000; remaining > 0; remaining -= 64) {
            ItemStack batch = prototype.copy();
            batch.setCount(Math.min(64, remaining));
            assertTrue(storage.insertItem(150, batch, false, false).isEmpty());
        }

        BlackHoleBackpackSavedData.BackpackStorage.ShrinkTransaction transaction = storage.prepareShrink(143);
        assertTrue(transaction.isPrepared());
        assertEquals(1000L, transaction.getDrops().stream().mapToLong(ItemStack::getCount).sum());
        assertEquals(16, transaction.getDrops().size());
        assertTrue(transaction.getDrops().stream().allMatch(stack -> stack.getCount() <= 64));
        assertTrue(transaction.getDrops().stream().allMatch(stack ->
                BlackHoleBackpackSavedData.BackpackStorage.areSameType(prototype, stack)));
        assertTrue(transaction.commit());
        assertEquals(0L, storage.getSlotCount(150));
        assertTrue(transaction.rollback());
        assertEquals(1000L, storage.getSlotCount(150));
        assertSameType(prototype, storage.getSlotPrototype(150));
    }

    @Test
    void shrinkTransactionHonorsNonStackableItemLimit() {
        BlackHoleBackpackSavedData data = new BlackHoleBackpackSavedData();
        BlackHoleBackpackSavedData.BackpackStorage storage = data.getStorage(UUID.randomUUID());
        ItemStack sword = new ItemStack(Items.IRON_SWORD, 4);
        assertTrue(storage.insertItem(200, sword, false, false).isEmpty());

        BlackHoleBackpackSavedData.BackpackStorage.ShrinkTransaction transaction = storage.prepareShrink(143);
        assertTrue(transaction.isPrepared());
        assertEquals(4, transaction.getDrops().size());
        assertTrue(transaction.getDrops().stream().allMatch(stack -> stack.getCount() == 1));
    }

    @Test
    void disabledOverstackFillsLargestMatchingStackThenUsesFirstEmptySlot() {
        BlackHoleBackpackSavedData.BackpackStorage storage =
                new BlackHoleBackpackSavedData().getStorage(UUID.randomUUID());
        ItemStack paper = new ItemStack(Items.PAPER);
        assertTrue(storage.insertItem(0, withCount(paper, 16), false, false, 9, false).isEmpty());
        assertTrue(storage.insertItem(1, withCount(paper, 53), false, false, 9, false).isEmpty());
        assertTrue(storage.insertItem(2, withCount(paper, 64), false, false, 9, false).isEmpty());

        assertTrue(storage.insertStackAnywhere(withCount(paper, 20), false, false, 9, false).isEmpty());
        assertEquals(16L, storage.getSlotCount(0));
        assertEquals(64L, storage.getSlotCount(1));
        assertEquals(64L, storage.getSlotCount(2));
        assertEquals(9L, storage.getSlotCount(3));
    }

    @Test
    void disabledOverstackManualInsertionOnlyChangesSelectedSlot() {
        BlackHoleBackpackSavedData.BackpackStorage storage =
                new BlackHoleBackpackSavedData().getStorage(UUID.randomUUID());
        ItemStack paper = new ItemStack(Items.PAPER);
        assertTrue(storage.insertItem(4, withCount(paper, 60), false, false, 9, false).isEmpty());

        ItemStack remainder = storage.insertItem(4, withCount(paper, 16), false, false, 9, false);
        assertEquals(12, remainder.getCount());
        assertEquals(64L, storage.getSlotCount(4));
        assertEquals(1, storage.getFilledSlotCount());
    }

    @Test
    void disabledOverstackSimulationAccountsForMultipleEmptySlotsWithoutMutation() {
        BlackHoleBackpackSavedData.BackpackStorage storage =
                new BlackHoleBackpackSavedData().getStorage(UUID.randomUUID());
        ItemStack swords = new ItemStack(Items.IRON_SWORD, 4);

        ItemStack remainder = storage.insertStackAnywhere(swords, true, false, 3, false);
        assertEquals(1, remainder.getCount());
        assertEquals(0, storage.getFilledSlotCount());
        assertEquals(0L, storage.getRevision());
    }

    @Test
    void disablingOverstackSplitsCountsWithoutLosingItems() {
        BlackHoleBackpackSavedData.BackpackStorage storage =
                new BlackHoleBackpackSavedData().getStorage(UUID.randomUUID());
        ItemStack paper = new ItemStack(Items.PAPER);
        for (int remaining = 150; remaining > 0; remaining -= 50) {
            assertTrue(storage.insertItem(0, withCount(paper, Math.min(50, remaining)),
                    false, false).isEmpty());
        }

        assertTrue(storage.applySlotLimitPolicy(false));
        assertEquals(150L, totalCount(storage));
        assertEquals(64L, storage.getSlotCount(0));
        assertEquals(64L, storage.getSlotCount(1));
        assertEquals(22L, storage.getSlotCount(2));
    }

    @Test
    void slotLimitPolicyPreservesSeparateManualDuplicatesThatAlreadyFit() {
        BlackHoleBackpackSavedData.BackpackStorage storage =
                new BlackHoleBackpackSavedData().getStorage(UUID.randomUUID());
        ItemStack paper = new ItemStack(Items.PAPER);
        assertTrue(storage.insertItem(3, withCount(paper, 16), false, false).isEmpty());
        assertTrue(storage.insertItem(8, withCount(paper, 53), false, false).isEmpty());

        assertTrue(storage.applySlotLimitPolicy(false));
        assertEquals(16L, storage.getSlotCount(3));
        assertEquals(53L, storage.getSlotCount(8));
        assertEquals(2, storage.getFilledSlotCount());
    }

    @Test
    void unsafeOverstackExpansionIsRefusedWithoutChangingStoredCount() {
        BlackHoleBackpackSavedData.BackpackStorage storage =
                new BlackHoleBackpackSavedData().getStorage(UUID.randomUUID());
        ItemStack huge = new ItemStack(Items.PAPER);
        huge.setCount(Integer.MAX_VALUE);
        assertTrue(storage.insertItem(0, huge, false, false).isEmpty());

        long revision = storage.getRevision();
        assertFalse(storage.applySlotLimitPolicy(false));
        assertEquals((long) Integer.MAX_VALUE, storage.getSlotCount(0));
        assertEquals(revision, storage.getRevision());
    }

    @Test
    void hugeCapacityOverflowIsReturnedInBoundedRollbackSafeBatches() {
        BlackHoleBackpackSavedData.BackpackStorage storage =
                new BlackHoleBackpackSavedData().getStorage(UUID.randomUUID());
        ItemStack huge = new ItemStack(Items.PAPER);
        huge.setCount(Integer.MAX_VALUE);
        assertTrue(storage.insertItem(200, huge, false, false).isEmpty());

        BlackHoleBackpackSavedData.BackpackStorage.ShrinkTransaction transaction = storage.prepareShrink(143);
        assertTrue(transaction.isPrepared());
        assertEquals(256, transaction.getDrops().size());
        long removed = transaction.getDrops().stream().mapToLong(ItemStack::getCount).sum();
        assertTrue(transaction.commit());
        assertEquals((long) Integer.MAX_VALUE - removed, storage.getSlotCount(200));
        assertTrue(transaction.rollback());
        assertEquals((long) Integer.MAX_VALUE, storage.getSlotCount(200));
    }

    private static long totalCount(BlackHoleBackpackSavedData.BackpackStorage storage) {
        long total = 0L;
        for (int slot = 0; slot < storage.getAllocatedSlotCount(); slot++) {
            total += storage.getSlotCount(slot);
        }
        return total;
    }

    private static int findSlot(BlackHoleBackpackSavedData.BackpackStorage storage, ItemStack prototype) {
        for (int slot = 0; slot < storage.getAllocatedSlotCount(); slot++) {
            if (BlackHoleBackpackSavedData.BackpackStorage.areSameType(
                    prototype, storage.getSlotPrototype(slot))) return slot;
        }
        return -1;
    }

    private static ItemStack numberedVariant(Item item, int variant, int count) {
        ItemStack stack = new ItemStack(item, count);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("variant", variant);
        stack.setTagCompound(tag);
        return stack;
    }

    private static ItemStack variant(Item item, int count, String variant) {
        ItemStack stack = new ItemStack(item, count);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("variant", variant);
        stack.setTagCompound(tag);
        return stack;
    }

    private static ItemStack withCount(ItemStack prototype, int count) {
        ItemStack stack = prototype.copy();
        stack.setCount(count);
        return stack;
    }

    private static void assertSameType(ItemStack expected, ItemStack actual) {
        assertTrue(
                BlackHoleBackpackSavedData.BackpackStorage.areSameType(expected, actual),
                () -> "Expected exact type " + expected.serializeNBT() + " but got " + actual.serializeNBT());
    }

    private static void appendLegacyStack(NBTTagList items, int slot, ItemStack stack) {
        NBTTagCompound serialized = new NBTTagCompound();
        serialized.setInteger("slot", slot);
        serialized.setTag("stack", stack.serializeNBT());
        items.appendTag(serialized);
    }

    private static void appendV2Entry(NBTTagList entries, int slot, ItemStack prototype, long count) {
        ItemStack normalized = prototype.copy();
        normalized.setCount(1);
        NBTTagCompound serialized = new NBTTagCompound();
        serialized.setInteger("slot", slot);
        serialized.setLong("count", count);
        serialized.setTag("prototype", normalized.serializeNBT());
        entries.appendTag(serialized);
    }

    private static NBTTagCompound legacyStorage(UUID id, NBTTagList items) {
        NBTTagCompound storage = new NBTTagCompound();
        storage.setUniqueId("id", id);
        storage.setTag("items", items);
        return storage;
    }

    private static NBTTagCompound v2Storage(UUID id, long revision, NBTTagList entries) {
        NBTTagCompound storage = new NBTTagCompound();
        storage.setUniqueId("id", id);
        storage.setInteger("format", 2);
        storage.setLong("revision", revision);
        storage.setTag("entries", entries);
        return storage;
    }

    private static NBTTagCompound rootWithStorage(NBTTagCompound storage) {
        return rootWithStorages(storage);
    }

    private static NBTTagCompound rootWithStorages(NBTTagCompound... storageCompounds) {
        NBTTagList storages = new NBTTagList();
        for (NBTTagCompound storage : storageCompounds) {
            storages.appendTag(storage);
        }
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("storages", storages);
        return root;
    }

    private static NBTTagCompound findStorage(NBTTagCompound root, UUID id) {
        NBTTagList storages = root.getTagList("storages", Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < storages.tagCount(); index++) {
            NBTTagCompound storage = storages.getCompoundTagAt(index);
            if (storage.hasUniqueId("id") && id.equals(storage.getUniqueId("id"))) {
                return storage;
            }
        }
        throw new AssertionError("Missing serialized storage " + id);
    }

    private static UUID findOnlyStorageId(NBTTagCompound root) {
        NBTTagList storages = root.getTagList("storages", Constants.NBT.TAG_COMPOUND);
        assertEquals(1, storages.tagCount());
        return storages.getCompoundTagAt(0).getUniqueId("id");
    }

    private static NBTTagCompound findEntry(NBTTagList entries, int slot) {
        for (int index = 0; index < entries.tagCount(); index++) {
            NBTTagCompound entry = entries.getCompoundTagAt(index);
            if (entry.getInteger("slot") == slot) {
                return entry;
            }
        }
        throw new AssertionError("Missing serialized slot " + slot);
    }
}
