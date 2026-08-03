package com.hbm.inventory;

import com.hbm.items.tool.BackpackUpgradeType;
import com.hbm.items.tool.ItemRealityErrorBackpack;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.IRegistryDelegate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackUpgradeCompatibilityTest {

    @BeforeAll
    static void registerVanillaItems() {
        if (!Bootstrap.isRegistered()) Bootstrap.register();
    }

    @Test
    void realityErrorSumsDuplicateExpansionTiersAndAcceptsUtilityModulesButNotAutoSort() {
        assertEquals(7, BackpackUpgradeManager.getRealityStorageCapacity(1, Arrays.asList(1, 2, 3)));
        assertEquals(5, BackpackUpgradeManager.getRealityStorageCapacity(1, Arrays.asList(1, 1, 2)));
        assertEquals(7, BackpackUpgradeManager.getRealityStorageCapacity(1, Arrays.asList(3, 3)));
        for (int first = 1; first <= 3; first++) {
            for (int second = 1; second <= 3; second++) {
                assertEquals(Math.min(7, 1 + first + second),
                        BackpackUpgradeManager.getRealityStorageCapacity(1, Arrays.asList(first, second)));
            }
        }
        assertTrue(BackpackUpgradeManager.isUpgradeTypeCompatible(false, true, true,
                BackpackUpgradeType.CAPACITY, 1));
        assertFalse(BackpackUpgradeManager.isUpgradeTypeCompatible(false, true, true,
                BackpackUpgradeType.CAPACITY, 4));
        assertTrue(BackpackUpgradeManager.isUpgradeTypeCompatible(false, true, true,
                BackpackUpgradeType.MAGNET, 1));
        assertTrue(BackpackUpgradeManager.isUpgradeTypeCompatible(false, true, true,
                BackpackUpgradeType.RANGE, 3));
        assertTrue(BackpackUpgradeManager.isUpgradeTypeCompatible(false, true, true,
                BackpackUpgradeType.WORKBENCH, 0));
        assertTrue(BackpackUpgradeManager.isUpgradeTypeCompatible(false, true, true,
                BackpackUpgradeType.AMMO_FEEDER, 0));
        assertFalse(BackpackUpgradeManager.isUpgradeTypeCompatible(false, true, true,
                BackpackUpgradeType.AUTO_SORT, 0));
    }

    @Test
    void smugglerAcceptsWorkbenchAndActionFlagsFollowInstalledModules() {
        assertTrue(BackpackUpgradeManager.isUpgradeTypeCompatible(false, false, false,
                BackpackUpgradeType.WORKBENCH, 0));
        int allActions = BackpackUpgradeManager.ACTION_AUTO_PICKUP
                | BackpackUpgradeManager.ACTION_AUTO_SORT | BackpackUpgradeManager.ACTION_WORKBENCH;
        assertEquals(7, allActions);
        assertEquals(6, allActions & ~BackpackUpgradeManager.ACTION_AUTO_PICKUP);
    }

    @Test
    void blackBoxDefaultsToFourSlotsAndRealityErrorHasExactlyTwo() {
        assertEquals(4, BackpackUpgradeManager.getUpgradeSlotCountForName("backpack_black_box"));
        assertEquals(2, BackpackUpgradeManager.getUpgradeSlotCountForName("backpack_reality_error"));
    }

    @Test
    void realityErrorLegacyThirdUpgradeSlotIsReturnedWithExactNbt() {
        ItemRealityErrorBackpack item = newUninitializedRealityErrorBackpack();
        ItemStack backpack = new ItemStack(item);
        backpack.setTagCompound(new NBTTagCompound());

        ItemStack kept = new ItemStack(Items.APPLE, 1);
        ItemStack legacy = new ItemStack(Items.PAPER, 7);
        NBTTagCompound marker = new NBTTagCompound();
        marker.setString("exact", "legacy-upgrade-data");
        legacy.setTagCompound(marker);
        NBTTagList items = new NBTTagList();
        items.appendTag(upgradeEntry(0, kept));
        items.appendTag(upgradeEntry(2, legacy));
        NBTTagCompound upgrades = new NBTTagCompound();
        upgrades.setInteger("Size", 3);
        upgrades.setTag("Items", items);
        backpack.getTagCompound().setTag(BackpackUpgradeManager.UPGRADES_TAG, upgrades);

        List<ItemStack> removed = BackpackUpgradeManager.takeUpgradesBeyondPhysicalSlots(backpack);
        assertEquals(1, removed.size());
        assertEquals(legacy.serializeNBT(), removed.get(0).serializeNBT());
        NBTTagCompound migrated = backpack.getTagCompound()
                .getCompoundTag(BackpackUpgradeManager.UPGRADES_TAG);
        assertEquals(2, migrated.getInteger("Size"));
        assertEquals(1, migrated.getTagList("Items", 10).tagCount());
    }

    private static NBTTagCompound upgradeEntry(int slot, ItemStack stack) {
        NBTTagCompound entry = stack.serializeNBT();
        entry.setInteger("Slot", slot);
        return entry;
    }

    private static ItemRealityErrorBackpack newUninitializedRealityErrorBackpack() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            ItemRealityErrorBackpack item = (ItemRealityErrorBackpack) unsafe.allocateInstance(
                    ItemRealityErrorBackpack.class);
            Field delegateField = IForgeRegistryEntry.Impl.class.getDeclaredField("delegate");
            unsafe.putObject(item, unsafe.objectFieldOffset(delegateField), new IRegistryDelegate<Item>() {
                @Override
                public Item get() {
                    return item;
                }

                @Override
                public ResourceLocation name() {
                    return new ResourceLocation("hbm", "backpack_reality_error");
                }

                @Override
                public Class<Item> type() {
                    return Item.class;
                }
            });
            return item;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not create isolated reality-error backpack", exception);
        }
    }
}
