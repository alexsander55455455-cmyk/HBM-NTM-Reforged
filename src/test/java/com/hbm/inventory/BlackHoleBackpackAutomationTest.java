package com.hbm.inventory;

import com.hbm.config.BackpackConfig;
import com.hbm.items.tool.ItemBlackHoleBackpack;
import com.hbm.items.tool.ItemDineutroniumBackpack;
import com.hbm.items.tool.ItemPocketHoleBackpack;
import com.hbm.saveddata.PocketHoleBackpackSavedData;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackHoleBackpackAutomationTest {

    @BeforeAll
    static void registerVanillaItems() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void clientAndServerFacingInventoryExposeTheBuiltInAutoSortSwitch() {
        ItemBlackHoleBackpack item = newUninitializedBlackHoleBackpack();
        ItemStack stack = new ItemStack(item);
        IBackpackInventory inventory = item.createInventory(stack, null);

        assertTrue(BackpackUpgradeManager.supportsCapacityUpgrade(stack));
        assertEquals(BackpackConfig.MAX_STORAGE_SLOTS, item.getStorageSlots(stack));
        assertTrue(inventory.supportsAutoSorting());
        assertTrue(inventory.isAutoSortEnabled());
        inventory.setAutoSortEnabled(false);
        assertFalse(inventory.isAutoSortEnabled());
        inventory.setAutoSortEnabled(true);
        assertTrue(inventory.isAutoSortEnabled());
    }

    @Test
    void pocketHoleAutoSortSwitchIsBuiltInAndPersistsOnItsOwnStack() {
        ItemPocketHoleBackpack item = newUninitializedPocketHoleBackpack();
        ItemStack stack = new ItemStack(item);
        IBackpackInventory inventory = item.createInventory(stack, null);

        assertTrue(BackpackUpgradeManager.supportsCapacityUpgrade(stack));
        assertEquals(ItemPocketHoleBackpack.VISIBLE_SLOTS, item.getStorageSlots(stack));
        assertTrue(inventory.supportsAutoSorting());
        assertTrue(inventory.isAutoSortEnabled());
        inventory.setAutoSortEnabled(false);
        assertFalse(inventory.isAutoSortEnabled());
        assertFalse(item.createInventory(stack, null).isAutoSortEnabled());
        inventory.setAutoSortEnabled(true);
        assertTrue(item.createInventory(stack, null).isAutoSortEnabled());
    }

    @Test
    void pocketHoleClientRebindRefreshesNewVirtualSlotCountWithoutScrolling() {
        ItemPocketHoleBackpack item = newUninitializedPocketHoleBackpack();
        UUID storageId = UUID.randomUUID();
        ItemStack initial = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId(ItemPocketHoleBackpack.STORAGE_ID_TAG, storageId);
        initial.setTagCompound(tag);

        PocketHoleBackpackSavedData data = new PocketHoleBackpackSavedData();
        PocketHoleBackpackSavedData.PocketHoleStorage storage = data.getStorage(storageId);
        item.setCachedSummary(initial, storage);
        IBackpackInventory client = item.createInventory(initial, null);

        ItemStack cyanPowder = new ItemStack(Blocks.CONCRETE_POWDER, 704, 9);
        assertTrue(storage.insertItem(0, cyanPowder, false, item.getStorageSlots(initial)).isEmpty());
        ItemStack synchronizedCopy = initial.copy();
        item.setCachedSummary(synchronizedCopy, storage);

        client.setStackInSlot(0, new ItemStack(Blocks.CONCRETE_POWDER, 1, 9));
        assertEquals(1L, client.getTrueSlotCount(0));
        assertTrue(client.tryRebindClientBackpack(synchronizedCopy));
        assertEquals(704L, client.getTrueSlotCount(0));
    }

    @Test
    void builtInDineutroniumMagnetConsumesConfiguredEnergyPerSuccessfulOperation() {
        ItemDineutroniumBackpack item = newUninitializedDineutroniumBackpack();
        ItemStack stack = new ItemStack(item);
        item.setCharge(stack, 2_000L);

        assertEquals(10_000_000L, item.getMaxCharge(stack));
        assertTrue(BackpackUpgradeManager.canAutoPickup(stack));
        assertTrue(BackpackUpgradeManager.consumeAutoPickupEnergy(stack));
        assertEquals(1_000L, item.getCharge(stack));
        assertTrue(BackpackUpgradeManager.consumeAutoPickupEnergy(stack));
        assertEquals(0L, item.getCharge(stack));
        assertFalse(BackpackUpgradeManager.canAutoPickup(stack));
    }

    private static ItemBlackHoleBackpack newUninitializedBlackHoleBackpack() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            ItemBlackHoleBackpack item = (ItemBlackHoleBackpack) unsafe.allocateInstance(ItemBlackHoleBackpack.class);

            Field delegateField = IForgeRegistryEntry.Impl.class.getDeclaredField("delegate");
            unsafe.putObject(item, unsafe.objectFieldOffset(delegateField), new IRegistryDelegate<Item>() {
                @Override
                public Item get() {
                    return item;
                }

                @Override
                public ResourceLocation name() {
                    return new ResourceLocation("hbm", "backpack_black_hole");
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

    private static ItemPocketHoleBackpack newUninitializedPocketHoleBackpack() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            ItemPocketHoleBackpack item = (ItemPocketHoleBackpack) unsafe.allocateInstance(ItemPocketHoleBackpack.class);

            Field delegateField = IForgeRegistryEntry.Impl.class.getDeclaredField("delegate");
            unsafe.putObject(item, unsafe.objectFieldOffset(delegateField), new IRegistryDelegate<Item>() {
                @Override
                public Item get() {
                    return item;
                }

                @Override
                public ResourceLocation name() {
                    return new ResourceLocation("hbm", "backpack_pocket_hole");
                }

                @Override
                public Class<Item> type() {
                    return Item.class;
                }
            });
            return item;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not create isolated pocket-hole backpack test item", exception);
        }
    }


    private static ItemDineutroniumBackpack newUninitializedDineutroniumBackpack() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            ItemDineutroniumBackpack item = (ItemDineutroniumBackpack) unsafe.allocateInstance(
                    ItemDineutroniumBackpack.class);
            Field delegateField = IForgeRegistryEntry.Impl.class.getDeclaredField("delegate");
            unsafe.putObject(item, unsafe.objectFieldOffset(delegateField), new IRegistryDelegate<Item>() {
                @Override
                public Item get() {
                    return item;
                }

                @Override
                public ResourceLocation name() {
                    return new ResourceLocation("hbm", "backpack_dineutronium");
                }

                @Override
                public Class<Item> type() {
                    return Item.class;
                }
            });
            return item;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not create isolated dineutronium backpack test item", exception);
        }
    }
}
