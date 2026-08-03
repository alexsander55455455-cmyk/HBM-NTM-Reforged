package com.hbm.inventory.container;

import com.hbm.items.tool.ItemBlackHoleBackpack;
import com.hbm.items.tool.ItemBackpack;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.IRegistryDelegate;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerBackpackBlackHolePageSyncTest {

    @BeforeAll
    static void registerVanillaItems() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void fullPagesRemainAtomicAcrossRowsAndRejectUnrelatedOrStalePackets() {
        TestClientWorld world = new TestClientWorld();
        TestPlayer player = new TestPlayer(world);
        ItemBlackHoleBackpack backpackItem = newUninitializedBlackHoleBackpack();
        UUID storageId = UUID.randomUUID();
        ItemStack backpack = new ItemStack(backpackItem);
        NBTTagCompound backpackTag = new NBTTagCompound();
        backpackTag.setUniqueId(ItemBlackHoleBackpack.STORAGE_ID_TAG, storageId);
        backpack.setTagCompound(backpackTag);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, backpack);

        ContainerBackpack container = new ContainerBackpack(player, EnumHand.MAIN_HAND, 260);
        assertTrue(container.usesServerAuthoritativeBlackHolePages());

        applyFull(container, storageId, 1, 0, 260, 1,
                new int[]{0}, new ItemStack[]{new ItemStack(Items.PAPER)}, new long[]{128L});
        assertPageCell(container, 0, 0, Items.PAPER, 128L);

        applyFull(container, storageId, 2, 7, 260, 1,
                new int[]{0}, new ItemStack[]{new ItemStack(Items.PAPER)}, new long[]{1_200_000L});
        assertPageCell(container, 7, 0, Items.PAPER, 1_200_000L);

        // An older page must not move the view or replace its current count.
        applyFull(container, storageId, 1, 1, 999, 99,
                new int[]{0}, new ItemStack[]{new ItemStack(Items.BONE)}, new long[]{7L});
        assertPageCell(container, 7, 0, Items.PAPER, 1_200_000L);
        assertEquals(260, container.getCapacity());
        assertEquals(1, container.getFilledSlotCount());

        // A packet for another backpack must be ignored without consuming its
        // sequence number. The same sequence from the open backpack is valid.
        applyFull(container, UUID.randomUUID(), 3, 2, 999, 99,
                new int[]{0}, new ItemStack[]{new ItemStack(Items.BONE)}, new long[]{9L});
        assertPageCell(container, 7, 0, Items.PAPER, 1_200_000L);

        applyFull(container, storageId, 3, 3, 260, 1,
                new int[]{0}, new ItemStack[]{new ItemStack(Items.PAPER)}, new long[]{4_000_000_000L});
        assertPageCell(container, 3, 0, Items.PAPER, 4_000_000_000L);
    }

    @Test
    void fullPageClearsEveryOmittedCellBeforeApplyingItsEntries() {
        TestClientWorld world = new TestClientWorld();
        TestPlayer player = new TestPlayer(world);
        ItemBlackHoleBackpack backpackItem = newUninitializedBlackHoleBackpack();
        UUID storageId = UUID.randomUUID();
        ItemStack backpack = new ItemStack(backpackItem);
        NBTTagCompound backpackTag = new NBTTagCompound();
        backpackTag.setUniqueId(ItemBlackHoleBackpack.STORAGE_ID_TAG, storageId);
        backpack.setTagCompound(backpackTag);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, backpack);

        ContainerBackpack container = new ContainerBackpack(player, EnumHand.MAIN_HAND, 260);
        applyFull(container, storageId, 10, 4, 260, 2,
                new int[]{0, 1},
                new ItemStack[]{new ItemStack(Items.PAPER), new ItemStack(Items.BONE)},
                new long[]{300L, 600L});
        assertPageCell(container, 4, 0, Items.PAPER, 300L);
        assertPageCell(container, 4, 1, Items.BONE, 600L);

        applyFull(container, storageId, 11, 4, 260, 1,
                new int[]{1}, new ItemStack[]{new ItemStack(Items.PAPER)}, new long[]{900L});

        assertEquals(4, container.getScrollRow());
        assertTrue(contentStack(container, 0).isEmpty(), "omitted full-page slot retained stale contents");
        assertEquals(-1L, container.getTrueSlotCount(0));
        assertSame(Items.PAPER, contentStack(container, 1).getItem());
        assertEquals(900L, container.getTrueSlotCount(1));
        assertEquals(1, container.getFilledSlotCount());
    }

    @Test
    void smallerAuthoritativeCapacityDoesNotShrinkTheSessionViewOrClampItsCurrentRow() {
        TestClientWorld world = new TestClientWorld();
        TestPlayer player = new TestPlayer(world);
        ItemBlackHoleBackpack backpackItem = newUninitializedBlackHoleBackpack();
        UUID storageId = UUID.randomUUID();
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, backpackWithStorage(backpackItem, storageId));

        ContainerBackpack container = new ContainerBackpack(player, EnumHand.MAIN_HAND, 143);
        applyFull(container, storageId, 1, 7, 260, 1,
                new int[]{0}, new ItemStack[]{new ItemStack(Items.PAPER)}, new long[]{500L});
        assertEquals(260, container.getCapacity());
        assertPageCell(container, 7, 0, Items.PAPER, 500L);

        applyFull(container, storageId, 2, 7, 143, 1,
                new int[]{0}, new ItemStack[]{new ItemStack(Items.BONE)}, new long[]{900L});

        assertEquals(260, container.getCapacity(), "authoritative shrink reduced the open session view");
        assertPageCell(container, 7, 0, Items.BONE, 900L);
    }

    @Test
    void bottomPageAlwaysContainsFiveCompleteRowsOfThirteenWorkingCells() {
        TestClientWorld world = new TestClientWorld();
        TestPlayer player = new TestPlayer(world);
        ItemBlackHoleBackpack backpackItem = newUninitializedBlackHoleBackpack();
        UUID storageId = UUID.randomUUID();
        ItemStack backpack = backpackWithStorage(backpackItem, storageId);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, backpack);

        ContainerBackpack container = new ContainerBackpack(player, EnumHand.MAIN_HAND, 143);
        int[] indices = new int[ContainerBackpack.VISIBLE_SLOTS];
        ItemStack[] prototypes = new ItemStack[ContainerBackpack.VISIBLE_SLOTS];
        long[] counts = new long[ContainerBackpack.VISIBLE_SLOTS];
        for (int slot = 0; slot < ContainerBackpack.VISIBLE_SLOTS; slot++) {
            indices[slot] = slot;
            prototypes[slot] = numberedVariant(slot);
            counts[slot] = 1_000L + slot;
        }

        applyFull(container, storageId, 1, 6, 143, 65, indices, prototypes, counts);
        assertEquals(65, container.getVisibleSlotCount());
        assertEquals(13, container.getColumns());
        for (int slot = 52; slot < 65; slot++) {
            assertEquals(1_000L + slot, container.getTrueSlotCount(slot));
            assertEquals(slot, contentStack(container, slot).getTagCompound().getInteger("variant"));
        }
    }

    @Test
    void matchingAcknowledgementKeepsOldPageUntilAtomicReplacementAndSequenceWraps() {
        TestClientWorld world = new TestClientWorld();
        TestPlayer player = new TestPlayer(world);
        ItemBlackHoleBackpack backpackItem = newUninitializedBlackHoleBackpack();
        UUID storageId = UUID.randomUUID();
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, backpackWithStorage(backpackItem, storageId));

        ContainerBackpack container = new ContainerBackpack(player, EnumHand.MAIN_HAND, 143);
        applyFull(container, storageId, Integer.MAX_VALUE, 0, 143, 1,
                new int[]{0}, new ItemStack[]{new ItemStack(Items.PAPER)}, new long[]{10L});
        assertTrue(container.beginBlackHolePageRequest());
        assertTrue(container.isBlackHolePagePending());
        assertFalse(container.beginBlackHolePageRequest());

        container.applyBlackHolePage(storageId, 1, 6, 143, 1,
                true, true, new int[]{64}, new ItemStack[]{new ItemStack(Items.BONE)}, new long[]{20L});
        assertFalse(container.isBlackHolePagePending());
        assertEquals(6, container.getScrollRow());
        assertTrue(contentStack(container, 0).isEmpty());
        assertSame(Items.BONE, contentStack(container, 64).getItem());
        assertEquals(20L, container.getTrueSlotCount(64));

        // Delayed pre-wrap data cannot restore the old row.
        applyFull(container, storageId, Integer.MAX_VALUE, 0, 143, 1,
                new int[]{0}, new ItemStack[]{new ItemStack(Items.PAPER)}, new long[]{99L});
        assertEquals(6, container.getScrollRow());
        assertSame(Items.BONE, contentStack(container, 64).getItem());
    }

    @Test
    void clientClicksRemainReadOnlyUntilTheServerConfirmsThem() {
        TestClientWorld world = new TestClientWorld();
        TestPlayer player = new TestPlayer(world);
        ItemBlackHoleBackpack backpackItem = newUninitializedBlackHoleBackpack();
        UUID storageId = UUID.randomUUID();
        ItemStack backpack = backpackWithStorage(backpackItem, storageId);
        backpack.getTagCompound().setBoolean("BackpackAutoSort", false);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, backpack);

        ContainerBackpack container = new ContainerBackpack(player, EnumHand.MAIN_HAND, 260);
        container.updateProgressBar(24, 0);
        assertTrue(container.beginVirtualClickRequest());
        assertFalse(container.beginVirtualClickRequest());
        container.updateProgressBar(24, 0);
        assertTrue(container.isVirtualClickPending(), "a repeated acknowledgement cleared a newer click");
        container.updateProgressBar(24, 1);
        assertFalse(container.isVirtualClickPending());
        assertTrue(container.beginVirtualClickRequest());
        container.updateProgressBar(24, 2);
        assertFalse(container.isVirtualClickPending());

        applyFull(container, storageId, 1, 4, 260, 2,
                new int[]{0, 1},
                new ItemStack[]{new ItemStack(Items.PAPER), new ItemStack(Items.BONE)},
                new long[]{1L, 600L});

        // PROPERTY_AUTO_SORT arrives after the authoritative page. Applying it
        // must not move this partial page to backend slots 0..N.
        container.updateProgressBar(21, 1);
        assertTrue(container.isAutoSortEnabled());
        assertPageCell(container, 4, 0, Items.PAPER, 1L);
        assertSame(Items.BONE, contentStack(container, 1).getItem());
        assertEquals(600L, container.getTrueSlotCount(1));

        ItemStack transactionSnapshot =
                container.slotClick(container.getContentStart(), 0, ClickType.PICKUP, player);
        assertSame(Items.PAPER, transactionSnapshot.getItem());
        assertSame(Items.PAPER, contentStack(container, 0).getItem());
        assertEquals(1L, container.getTrueSlotCount(0));
        assertSame(Items.BONE, contentStack(container, 1).getItem());
        assertEquals(600L, container.getTrueSlotCount(1));
        assertTrue(player.inventory.getItemStack().isEmpty());

        ItemStack largeTransactionSnapshot =
                container.slotClick(container.getContentStart() + 1, 0, ClickType.PICKUP, player);
        assertEquals(64, largeTransactionSnapshot.getCount());
        assertSame(Items.BONE, contentStack(container, 1).getItem());
        assertEquals(600L, container.getTrueSlotCount(1));
        assertTrue(player.inventory.getItemStack().isEmpty());

        assertFalse(container.enchantItem(player, ContainerBackpack.SCROLL_DOWN));
        assertEquals(4, container.getScrollRow());
    }

    @Test
    void virtualModeCannotFallBackToVanillaWhileTheOpenBackpackStackIsBeingResynchronized() {
        TestClientWorld world = new TestClientWorld();
        TestPlayer player = new TestPlayer(world);
        ItemBlackHoleBackpack backpackItem = newUninitializedBlackHoleBackpack();
        UUID storageId = UUID.randomUUID();
        ItemStack backpack = backpackWithStorage(backpackItem, storageId);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, backpack);

        ContainerBackpack container = new ContainerBackpack(player, EnumHand.MAIN_HAND, 143);
        assertTrue(container.usesVirtualLongCounts());

        ItemStack transientCopy = backpack.copy();
        transientCopy.getTagCompound().removeTag(ItemBackpack.INSTANCE_ID_TAG);
        player.inventory.setInventorySlotContents(0, transientCopy);

        assertTrue(container.usesVirtualLongCounts(),
                "an open virtual container switched to vanilla slot arithmetic during stack synchronization");
    }

    @Test
    void quickCraftIntoAnOccupiedVirtualSlotAddsAllSixtyFourItems() throws ReflectiveOperationException {
        TestClientWorld world = new TestClientWorld();
        TestPlayer player = new TestPlayer(world);
        VirtualCountHandler handler = new VirtualCountHandler(new ItemStack(Blocks.GRASS), 64L);
        TestDragContainer container = new TestDragContainer();

        Class<?> slotType = Class.forName(
                "com.hbm.inventory.container.ContainerBackpack$BackpackContentSlot");
        Constructor<?> constructor = slotType.getDeclaredConstructor(
                IItemHandlerModifiable.class, int.class, int.class, int.class,
                BooleanSupplier.class, BooleanSupplier.class, BooleanSupplier.class);
        constructor.setAccessible(true);
        Slot slot = (Slot) constructor.newInstance(
                handler, 0, 0, 0,
                (BooleanSupplier) () -> true,
                (BooleanSupplier) () -> true,
                (BooleanSupplier) () -> false);
        container.add(slot);

        player.inventory.setItemStack(new ItemStack(Blocks.GRASS, 64));
        container.slotClick(-999, 0, ClickType.QUICK_CRAFT, player);
        container.slotClick(0, 1, ClickType.QUICK_CRAFT, player);
        container.slotClick(-999, 2, ClickType.QUICK_CRAFT, player);

        assertEquals(128L, handler.count);
        assertTrue(player.inventory.getItemStack().isEmpty());
    }

    private static void applyFull(ContainerBackpack container, UUID storageId, int sequence,
                                  int row, int capacity, int filled, int[] indices,
                                  ItemStack[] prototypes, long[] counts) {
        container.applyBlackHolePage(storageId, sequence, row, capacity, filled,
                true, true, indices, prototypes, counts);
    }

    private static void assertPageCell(ContainerBackpack container, int expectedRow, int displaySlot,
                                       net.minecraft.item.Item expectedItem, long expectedCount) {
        assertEquals(expectedRow, container.getScrollRow());
        assertSame(expectedItem, contentStack(container, displaySlot).getItem());
        assertEquals(expectedCount, container.getTrueSlotCount(displaySlot));
    }

    private static ItemStack contentStack(ContainerBackpack container, int displaySlot) {
        return container.inventorySlots.get(container.getContentStart() + displaySlot).getStack();
    }

    private static ItemStack backpackWithStorage(ItemBlackHoleBackpack item, UUID storageId) {
        ItemStack backpack = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId(ItemBlackHoleBackpack.STORAGE_ID_TAG, storageId);
        tag.setUniqueId(ItemBackpack.INSTANCE_ID_TAG, UUID.randomUUID());
        backpack.setTagCompound(tag);
        return backpack;
    }

    private static ItemStack numberedVariant(int variant) {
        ItemStack stack = new ItemStack(Items.PAPER);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("variant", variant);
        stack.setTagCompound(tag);
        return stack;
    }

    private static ItemBlackHoleBackpack newUninitializedBlackHoleBackpack() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            ItemBlackHoleBackpack item = (ItemBlackHoleBackpack) unsafe.allocateInstance(ItemBlackHoleBackpack.class);

            Field delegateField = IForgeRegistryEntry.Impl.class.getDeclaredField("delegate");
            delegateField.setAccessible(true);
            delegateField.set(item, new IRegistryDelegate<Item>() {
                @Override
                public Item get() {
                    return item;
                }

                @Override
                public ResourceLocation name() {
                    return new ResourceLocation("hbm", "test_black_hole_backpack");
                }

                @Override
                public Class<Item> type() {
                    return Item.class;
                }
            });
            return item;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not create isolated black-hole backpack test item", exception);
        }
    }

    private static final class TestPlayer extends EntityPlayer {
        private TestPlayer(World world) {
            super(world, new GameProfile(UUID.randomUUID(), "black-hole-page-sync-test"));
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }
    }

    private static final class TestDragContainer extends Container {
        private void add(Slot slot) {
            addSlotToContainer(slot);
        }

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return true;
        }
    }

    private static final class VirtualCountHandler implements IItemHandlerModifiable {
        private final ItemStack prototype;
        private long count;

        private VirtualCountHandler(ItemStack prototype, long count) {
            this.prototype = prototype.copy();
            this.prototype.setCount(1);
            this.count = count;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 && count > 0L ? prototype.copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !ItemStack.areItemsEqual(prototype, stack)
                    || !ItemStack.areItemStackTagsEqual(prototype, stack)) {
                return stack;
            }
            if (!simulate) count += stack.getCount();
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? Integer.MAX_VALUE : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && !stack.isEmpty()
                    && ItemStack.areItemsEqual(prototype, stack)
                    && ItemStack.areItemStackTagsEqual(prototype, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
        }
    }

    private static final class TestClientWorld extends World {
        private TestClientWorld() {
            super(new SaveHandlerMP(), new WorldInfo(new NBTTagCompound()),
                    new WorldProviderSurface(), new Profiler(), true);
            this.provider.setWorld(this);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return true;
        }

        @Override
        public Entity getEntityByID(int id) {
            return null;
        }
    }
}
