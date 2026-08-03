package com.hbm.items.tool;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.IRegistryDelegate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemBackpackInventorySortTest {

    @BeforeAll
    static void registerVanillaItems() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void fragmentedCompatibleStacksMergeAndHolesCompactWithoutLosingItems() {
        ItemBackpack.BackpackInventory inventory = newInventory(8);
        inventory.setStackInSlot(2, new ItemStack(Items.BONE, 20));
        inventory.setStackInSlot(7, new ItemStack(Items.BONE, 50));

        assertEquals(70, totalCount(inventory));

        inventory.sortContents();

        assertStack(inventory.getStackInSlot(0), Items.BONE, 0, 64);
        assertStack(inventory.getStackInSlot(1), Items.BONE, 0, 6);
        for (int slot = 2; slot < inventory.getSlots(); slot++) {
            assertTrue(inventory.getStackInSlot(slot).isEmpty(), "Expected compacted empty slot " + slot);
        }
        assertEquals(70, totalCount(inventory));
    }

    @Test
    void registryAndMetadataOrderKeepsNbtVariantsSeparateAndSecondSortIsIdempotent() {
        ItemBackpack.BackpackInventory inventory = newInventory(10);
        inventory.setStackInSlot(1, taggedStack(Items.PAPER, 3, 0, "beta"));
        inventory.setStackInSlot(3, new ItemStack(Items.DYE, 4, 5));
        inventory.setStackInSlot(5, new ItemStack(Items.APPLE, 2));
        inventory.setStackInSlot(7, taggedStack(Items.PAPER, 7, 0, "alpha"));
        inventory.setStackInSlot(9, new ItemStack(Items.DYE, 6, 1));

        assertEquals(22, totalCount(inventory));

        inventory.sortContents();

        assertStack(inventory.getStackInSlot(0), Items.APPLE, 0, 2);
        assertStack(inventory.getStackInSlot(1), Items.DYE, 1, 6);
        assertStack(inventory.getStackInSlot(2), Items.DYE, 5, 4);
        assertTaggedStack(inventory.getStackInSlot(3), Items.PAPER, 7, "alpha");
        assertTaggedStack(inventory.getStackInSlot(4), Items.PAPER, 3, "beta");
        for (int slot = 5; slot < inventory.getSlots(); slot++) {
            assertTrue(inventory.getStackInSlot(slot).isEmpty(), "Expected compacted empty slot " + slot);
        }
        assertEquals(22, totalCount(inventory));

        NBTTagCompound afterFirstSort = inventory.serializeNBT().copy();
        inventory.sortContents();

        assertEquals(afterFirstSort, inventory.serializeNBT());
        assertEquals(22, totalCount(inventory));
    }

    @Test
    void enlargingBackpackPreservesEveryOldSlotWithoutMovingItems() {
        ItemBackpack.BackpackInventory oldInventory = newInventory(27);
        oldInventory.setStackInSlot(0, new ItemStack(Items.BONE, 17));
        oldInventory.setStackInSlot(13, taggedStack(Items.PAPER, 5, 0, "middle"));
        oldInventory.setStackInSlot(26, new ItemStack(Items.APPLE, 2));

        ItemStack enlargedStack = new ItemStack(newUninitializedBackpack(36));
        enlargedStack.setTagCompound(new NBTTagCompound());
        enlargedStack.getTagCompound().setTag(ItemBackpack.INVENTORY_TAG, oldInventory.serializeNBT().copy());
        ItemBackpack.BackpackInventory enlarged = new ItemBackpack.BackpackInventory(enlargedStack);

        assertEquals(36, enlarged.getSlots());
        assertStack(enlarged.getStackInSlot(0), Items.BONE, 0, 17);
        assertTaggedStack(enlarged.getStackInSlot(13), Items.PAPER, 5, "middle");
        assertStack(enlarged.getStackInSlot(26), Items.APPLE, 0, 2);
        for (int slot = 27; slot < enlarged.getSlots(); slot++) {
            assertTrue(enlarged.getStackInSlot(slot).isEmpty(), "Expected new empty slot " + slot);
        }
        assertEquals(24, totalCount(enlarged));
    }

    @Test
    void autoSortOffKeepsManualDuplicateLayout() {
        ItemBackpack.BackpackInventory inventory = newInventory(8);

        inventory.setStackInSlot(7, new ItemStack(Items.BONE, 20));
        inventory.setStackInSlot(2, new ItemStack(Items.BONE, 10));

        assertStack(inventory.getStackInSlot(2), Items.BONE, 0, 10);
        assertStack(inventory.getStackInSlot(7), Items.BONE, 0, 20);
        assertTrue(inventory.getStackInSlot(0).isEmpty());
    }

    @Test
    void autoSortOnImmediatelyMaintainsOrderForManualAutomaticAndExtractChanges() {
        ItemStack backpack = new ItemStack(newUninitializedAutomatedBackpack(8));
        backpack.setTagCompound(new NBTTagCompound());
        backpack.getTagCompound().setBoolean(IAutomatedBackpack.AUTO_SORT_TAG, false);
        ItemBackpack.BackpackInventory inventory = new ItemBackpack.BackpackInventory(backpack);
        inventory.setStackInSlot(7, new ItemStack(Items.BONE, 20));

        inventory.setAutoSortEnabled(true);
        assertStack(inventory.getStackInSlot(0), Items.BONE, 0, 20);

        inventory.setStackInSlot(7, new ItemStack(Items.APPLE, 2));
        assertStack(inventory.getStackInSlot(0), Items.APPLE, 0, 2);
        assertStack(inventory.getStackInSlot(1), Items.BONE, 0, 20);

        assertTrue(inventory.insertItem(6, new ItemStack(Items.BONE, 50), false).isEmpty());
        assertStack(inventory.getStackInSlot(1), Items.BONE, 0, 64);
        assertStack(inventory.getStackInSlot(2), Items.BONE, 0, 6);

        NBTTagCompound beforeSimulation = inventory.serializeNBT().copy();
        assertTrue(inventory.insertItemAnywhere(new ItemStack(Items.PAPER, 4), true).isEmpty());
        assertEquals(beforeSimulation, inventory.serializeNBT());

        assertStack(inventory.extractItem(0, 2, false), Items.APPLE, 0, 2);
        assertStack(inventory.getStackInSlot(0), Items.BONE, 0, 64);
        assertStack(inventory.getStackInSlot(1), Items.BONE, 0, 6);

        assertTrue(inventory.insertItemAnywhere(new ItemStack(Items.PAPER, 4), false).isEmpty());
        assertStack(inventory.getStackInSlot(2), Items.PAPER, 0, 4);
        assertEquals(74, totalCount(inventory));
    }

    @Test
    void smugglerAutoSortDoesNotMoveItemsBetweenCompartments() {
        ItemStack backpack = new ItemStack(newUninitializedAutomatedSmuggler());
        ItemBackpack.BackpackInventory inventory = new ItemBackpack.BackpackInventory(backpack);

        inventory.setStackInSlot(ItemSmugglerBackpack.VISIBLE_SLOTS - 1, new ItemStack(Items.BONE, 3));
        inventory.setStackInSlot(ItemSmugglerBackpack.SLOTS - 1, new ItemStack(Items.APPLE, 2));

        assertStack(inventory.getStackInSlot(0), Items.BONE, 0, 3);
        assertStack(inventory.getStackInSlot(ItemSmugglerBackpack.VISIBLE_SLOTS), Items.APPLE, 0, 2);
        assertTrue(inventory.getStackInSlot(1).isEmpty());
        assertTrue(inventory.getStackInSlot(ItemSmugglerBackpack.VISIBLE_SLOTS + 1).isEmpty());
    }

    private static ItemStack taggedStack(Item item, int count, int metadata, String variant) {
        ItemStack stack = new ItemStack(item, count, metadata);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("variant", variant);
        stack.setTagCompound(tag);
        return stack;
    }

    private static void assertStack(ItemStack stack, Item item, int metadata, int count) {
        assertEquals(item, stack.getItem());
        assertEquals(metadata, stack.getMetadata());
        assertEquals(count, stack.getCount());
    }

    private static void assertTaggedStack(ItemStack stack, Item item, int count, String variant) {
        assertStack(stack, item, 0, count);
        assertTrue(stack.hasTagCompound());
        assertEquals(variant, stack.getTagCompound().getString("variant"));
    }

    private static int totalCount(ItemBackpack.BackpackInventory inventory) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            total += inventory.getStackInSlot(slot).getCount();
        }
        return total;
    }

    private static ItemBackpack.BackpackInventory newInventory(int slots) {
        ItemBackpack backpackItem = newUninitializedBackpack(slots);
        return new ItemBackpack.BackpackInventory(new ItemStack(backpackItem));
    }

    private static ItemBackpack newUninitializedBackpack(int slots) {
        return newUninitializedBackpack(ItemBackpack.class, slots, "test_physical_backpack");
    }

    private static ItemBackpack newUninitializedAutomatedBackpack(int slots) {
        return newUninitializedBackpack(AutomatedTestBackpack.class, slots, "backpack_dineutronium");
    }

    private static ItemBackpack newUninitializedAutomatedSmuggler() {
        return newUninitializedBackpack(AutomatedTestSmugglerBackpack.class,
                ItemSmugglerBackpack.SLOTS, "backpack_dineutronium");
    }

    private static <T extends ItemBackpack> T newUninitializedBackpack(
            Class<T> type, int slots, String registryName) {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            T item = (T) unsafe.allocateInstance(type);

            Field slotsField = ItemBackpack.class.getDeclaredField("slots");
            unsafe.putInt(item, unsafe.objectFieldOffset(slotsField), slots);

            Field delegateField = IForgeRegistryEntry.Impl.class.getDeclaredField("delegate");
            unsafe.putObject(item, unsafe.objectFieldOffset(delegateField), new IRegistryDelegate<Item>() {
                @Override
                public Item get() {
                    return item;
                }

                @Override
                public ResourceLocation name() {
                    return new ResourceLocation("hbm", registryName);
                }

                @Override
                public Class<Item> type() {
                    return Item.class;
                }
            });
            return item;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not create isolated physical backpack test item", exception);
        }
    }

    private static class AutomatedTestBackpack extends ItemBackpack implements IAutomatedBackpack {
        private AutomatedTestBackpack() {
            super("test_automated_backpack", 1, 0D, false);
        }

        @Override
        public int getStorageSlots(ItemStack stack) {
            return 8;
        }

        @Override
        public boolean canAutoPickup(ItemStack stack) {
            return true;
        }

        @Override
        public boolean consumeAutoPickupEnergy(ItemStack stack) {
            return true;
        }

        @Override
        public long getAutoPickupEnergyCost() {
            return 0L;
        }
    }

    private static final class AutomatedTestSmugglerBackpack extends ItemSmugglerBackpack
            implements IAutomatedBackpack {
        private AutomatedTestSmugglerBackpack() {
            super("test_automated_smuggler_backpack");
        }

        @Override
        public int getStorageSlots(ItemStack stack) {
            return SLOTS;
        }

        @Override
        public boolean canAutoPickup(ItemStack stack) {
            return true;
        }

        @Override
        public boolean consumeAutoPickupEnergy(ItemStack stack) {
            return true;
        }

        @Override
        public long getAutoPickupEnergyCost() {
            return 0L;
        }
    }
}
