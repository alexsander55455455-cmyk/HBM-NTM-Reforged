package com.hbm.inventory;

import com.hbm.config.BackpackConfig;
import com.hbm.items.tool.BackpackUpgradeType;
import com.hbm.items.tool.IAutomatedBackpack;
import com.hbm.items.tool.ItemBackpack;
import com.hbm.items.tool.ItemBackpackMagnetUpgrade;
import com.hbm.items.tool.ItemBackpackUpgrade;
import com.hbm.items.tool.ItemBlackHoleBackpack;
import com.hbm.items.tool.ItemRealityErrorBackpack;
import com.hbm.items.tool.ItemSmugglerBackpack;
import com.hbm.lib.Library;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class BackpackUpgradeManager {

    public static final String UPGRADES_TAG = "BackpackUpgrades";
    public static final int ACTION_AUTO_PICKUP = 1;
    public static final int ACTION_AUTO_SORT = 1 << 1;
    public static final int ACTION_WORKBENCH = 1 << 2;
    private static final int MAX_UPGRADE_SLOTS = BackpackConfig.MAX_UPGRADE_SLOTS;

    private BackpackUpgradeManager() {
    }

    public static int getUpgradeSlotCount(ItemStack backpack) {
        if (!(backpack.getItem() instanceof ItemBackpack)) return 0;
        String name = backpack.getItem().getRegistryName() == null
                ? "" : backpack.getItem().getRegistryName().getPath();
        return getUpgradeSlotCountForName(name);
    }

    static int getUpgradeSlotCountForName(String name) {
        return BackpackConfig.getUpgradeSlots(name, getDefaultUpgradeSlotCountForName(name));
    }

    private static int getDefaultUpgradeSlotCountForName(String name) {
        switch (name) {
            case "backpack_steel":
            case "backpack_ash":
                return 1;
            case "backpack_titanium":
            case "backpack_lead":
            case "backpack_duralumin":
            case "backpack_nuclear_tourist":
                return 2;
            case "backpack_black_box":
                return 4;
            case "backpack_reality_error":
                return 2;
            case "backpack_reinforced_steel":
            case "backpack_asbestos":
            case "backpack_beryllium":
            case "backpack_stalker":
            case "backpack_sapper":
            case "backpack_smuggler":
                return 3;
            case "backpack_high_strength_steel":
            case "backpack_tungsten":
                return 4;
            case "backpack_desh":
            case "backpack_pocket_hole":
                return 5;
            case "backpack_schrabidium":
                return 6;
            case "backpack_dineutronium":
                return 7;
            case "backpack_black_hole":
                return 8;
            default:
                return 2;
        }
    }

    public static UpgradeInventory createInventory(ItemStack backpack) {
        return new UpgradeInventory(backpack, getUpgradeSlotCount(backpack));
    }

    public static int getStorageCapacity(ItemStack backpack, int baseCapacity) {
        if (backpack.getItem() instanceof ItemRealityErrorBackpack) {
            List<Integer> tiers = new ArrayList<>();
            for (ItemStack stack : getInstalledStacks(backpack, BackpackUpgradeType.CAPACITY)) {
                tiers.add(((ItemBackpackUpgrade) stack.getItem()).getTier());
            }
            return getRealityStorageCapacity(baseCapacity, tiers);
        }
        // The ordinary black-hole backpack already has virtual long-count
        // storage. Capacity modules are accepted as harmless filler modules,
        // but must never change the number of logical cells it exposes.
        if (backpack.getItem() instanceof ItemBlackHoleBackpack) {
            return baseCapacity;
        }
        int rows = 0;
        for (ItemStack stack : getInstalledStacks(backpack, BackpackUpgradeType.CAPACITY)) {
            ItemBackpackUpgrade upgrade = (ItemBackpackUpgrade) stack.getItem();
            rows += Math.max(0, upgrade.getCapacityBonus() / 9);
        }
        return baseCapacity + rows * getStorageColumnCount(baseCapacity);
    }

    static int getRealityStorageCapacity(int baseCapacity, List<Integer> installedTiers) {
        int capacity = baseCapacity;
        for (int tier : installedTiers) {
            if (tier >= 1 && tier <= 3) {
                capacity += tier;
            }
        }
        return Math.min(BackpackConfig.MAX_STORAGE_SLOTS, Math.min(baseCapacity + 6, capacity));
    }

    public static int getStorageColumnCount(int baseCapacity) {
        if (baseCapacity <= 54) return 9;
        if (baseCapacity <= 88) return 11;
        return 13;
    }

    public static boolean supportsCapacityUpgrade(ItemStack backpack) {
        return backpack.getItem() instanceof ItemBackpack
                && !(backpack.getItem() instanceof ItemSmugglerBackpack);
    }

    public static int getAvailableActions(ItemStack backpack) {
        int actions = 0;
        if (supportsAutoPickup(backpack)) actions |= ACTION_AUTO_PICKUP;
        if (supportsAutoSorting(backpack)) actions |= ACTION_AUTO_SORT;
        if (hasUpgrade(backpack, BackpackUpgradeType.WORKBENCH)) actions |= ACTION_WORKBENCH;
        return actions;
    }

    public static boolean hasUpgrade(ItemStack backpack, BackpackUpgradeType type) {
        return getInstalled(backpack, type) != null;
    }

    @Nullable
    public static ItemBackpackUpgrade getInstalled(ItemStack backpack, BackpackUpgradeType type) {
        ItemStack stack = getInstalledStack(backpack, type);
        return stack.getItem() instanceof ItemBackpackUpgrade upgrade ? upgrade : null;
    }

    public static ItemStack getInstalledStack(ItemStack backpack, BackpackUpgradeType type) {
        List<ItemStack> stacks = getInstalledStacks(backpack, type);
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0);
    }

    private static List<ItemStack> getInstalledStacks(ItemStack backpack, BackpackUpgradeType type) {
        List<ItemStack> result = new ArrayList<>();
        if (!backpack.hasTagCompound()) return result;
        NBTTagList items = backpack.getTagCompound().getCompoundTag(UPGRADES_TAG)
                .getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            ItemStack stack = new ItemStack(items.getCompoundTagAt(i));
            if (stack.getItem() instanceof ItemBackpackUpgrade upgrade
                    && upgrade.getUpgradeType() == type
                    && isIntrinsicallyCompatible(backpack, stack)) {
                result.add(stack);
            }
        }
        return result;
    }

    public static boolean supportsAutoPickup(ItemStack backpack) {
        return BackpackConfig.hasBuiltInMagnet(backpack)
                || hasUpgrade(backpack, BackpackUpgradeType.MAGNET);
    }

    public static boolean isAutoPickupEnabled(ItemStack backpack) {
        if (!supportsAutoPickup(backpack)) return false;
        if (backpack.hasTagCompound() && backpack.getTagCompound().hasKey(IAutomatedBackpack.AUTO_PICKUP_TAG)) {
            return backpack.getTagCompound().getBoolean(IAutomatedBackpack.AUTO_PICKUP_TAG);
        }
        return BackpackConfig.hasBuiltInMagnet(backpack)
                || hasUpgrade(backpack, BackpackUpgradeType.MAGNET);
    }

    public static void setAutoPickupEnabled(ItemStack backpack, boolean enabled) {
        IAutomatedBackpack.getOrCreateAutomationTag(backpack).setBoolean(IAutomatedBackpack.AUTO_PICKUP_TAG, enabled);
    }

    public static boolean supportsAutoSorting(ItemStack backpack) {
        return BackpackConfig.hasBuiltInAutoSort(backpack)
                || hasUpgrade(backpack, BackpackUpgradeType.AUTO_SORT);
    }

    public static boolean isAutoSortEnabled(ItemStack backpack) {
        if (!supportsAutoSorting(backpack)) return false;
        if (backpack.hasTagCompound() && backpack.getTagCompound().hasKey(IAutomatedBackpack.AUTO_SORT_TAG)) {
            return backpack.getTagCompound().getBoolean(IAutomatedBackpack.AUTO_SORT_TAG);
        }
        return BackpackConfig.hasBuiltInAutoSort(backpack);
    }

    public static void setAutoSortEnabled(ItemStack backpack, boolean enabled) {
        IAutomatedBackpack.getOrCreateAutomationTag(backpack).setBoolean(IAutomatedBackpack.AUTO_SORT_TAG, enabled);
    }

    public static boolean canAutoPickup(ItemStack backpack) {
        if (!isAutoPickupEnabled(backpack)) return false;
        if (BackpackConfig.hasBuiltInMagnet(backpack)) {
            long cost = BackpackConfig.getAutoMagnetEnergyPerOperation(backpack);
            return !BackpackConfig.autoMagnetRequiresEnergy(backpack)
                    || backpack.getItem() instanceof ItemBackpack item
                    && item.getCharge(backpack) >= cost;
        }
        ItemStack magnet = getInstalledStack(backpack, BackpackUpgradeType.MAGNET);
        return magnet.getItem() instanceof ItemBackpackMagnetUpgrade item
                && item.getCharge(magnet) >= item.getPickupCost();
    }

    public static boolean consumeAutoPickupEnergy(ItemStack backpack) {
        if (BackpackConfig.hasBuiltInMagnet(backpack)) {
            if (!BackpackConfig.autoMagnetRequiresEnergy(backpack)) return true;
            long cost = BackpackConfig.getAutoMagnetEnergyPerOperation(backpack);
            if (cost <= 0L) return true;
            return backpack.getItem() instanceof ItemBackpack
                    && Library.dischargeBatteryIfValid(backpack, cost, true) == cost;
        }
        UpgradeInventory upgrades = createInventory(backpack);
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            ItemStack magnet = upgrades.getStackInSlot(slot);
            if (!(magnet.getItem() instanceof ItemBackpackMagnetUpgrade item)) continue;
            long cost = item.getPickupCost();
            if (item.getCharge(magnet) < cost) return false;
            if (Library.dischargeBatteryIfValid(magnet, cost, true) != cost) return false;
            upgrades.setStackInSlot(slot, magnet);
            return true;
        }
        return false;
    }

    public static double getPickupRange(ItemStack backpack) {
        double range = 1.25D;
        for (ItemStack magnet : getInstalledStacks(backpack, BackpackUpgradeType.MAGNET)) {
            if (magnet.getItem() instanceof ItemBackpackMagnetUpgrade item) {
                range = Math.max(range, item.getPickupRange());
            }
        }
        for (ItemStack stack : getInstalledStacks(backpack, BackpackUpgradeType.RANGE)) {
            range += ((ItemBackpackUpgrade) stack.getItem()).getRangeBonus();
        }
        return range;
    }

    /**
     * Removes storage entries that no longer fit after a capacity module is
     * extracted. The caller owns the returned stacks and must give or drop them.
     */
    public static List<ItemStack> takeOverflowItems(ItemStack backpack, int capacity) {
        if (backpack.getItem() instanceof ItemRealityErrorBackpack) {
            return RealityErrorBackpackInventory.takeOverflowItems(backpack, capacity);
        }
        List<ItemStack> overflow = new ArrayList<>();
        if (!backpack.hasTagCompound()) return overflow;

        NBTTagCompound inventory = backpack.getTagCompound().getCompoundTag(ItemBackpack.INVENTORY_TAG).copy();
        NBTTagList items = inventory.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        NBTTagList kept = new NBTTagList();
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound itemTag = items.getCompoundTagAt(i);
            if (itemTag.getInteger("Slot") >= capacity) {
                ItemStack stack = new ItemStack(itemTag);
                if (!stack.isEmpty()) overflow.add(stack);
            } else {
                kept.appendTag(itemTag.copy());
            }
        }
        inventory.setInteger("Size", Math.max(0, capacity));
        inventory.setTag("Items", kept);
        backpack.getTagCompound().setTag(ItemBackpack.INVENTORY_TAG, inventory);
        return overflow;
    }

    public static boolean isUpgradeCompatible(ItemStack backpack, ItemStack candidate,
                                               UpgradeInventory installed, int targetSlot) {
        if (!isIntrinsicallyCompatible(backpack, candidate)) return false;
        ItemBackpackUpgrade upgrade = (ItemBackpackUpgrade) candidate.getItem();
        BackpackUpgradeType type = upgrade.getUpgradeType();
        for (int slot = 0; slot < installed.getSlots(); slot++) {
            if (slot == targetSlot) continue;
            ItemStack present = installed.getStackInSlot(slot);
            if (!(present.getItem() instanceof ItemBackpackUpgrade existing)
                    || existing.getUpgradeType() != type) continue;
            if (type != BackpackUpgradeType.CAPACITY && type != BackpackUpgradeType.RANGE) return false;
        }
        return true;
    }

    private static boolean isIntrinsicallyCompatible(ItemStack backpack, ItemStack candidate) {
        if (!(backpack.getItem() instanceof ItemBackpack)
                || !(candidate.getItem() instanceof ItemBackpackUpgrade upgrade)) return false;
        BackpackUpgradeType type = upgrade.getUpgradeType();
        if (type == BackpackUpgradeType.MAGNET && BackpackConfig.hasBuiltInMagnet(backpack)) return false;
        if (type == BackpackUpgradeType.AUTO_SORT && BackpackConfig.hasBuiltInAutoSort(backpack)) return false;
        return isUpgradeTypeCompatible(false,
                backpack.getItem() instanceof ItemRealityErrorBackpack,
                supportsCapacityUpgrade(backpack), type, upgrade.getTier());
    }

    static boolean isUpgradeTypeCompatible(boolean automated, boolean realityError,
                                           boolean capacitySupported, BackpackUpgradeType type, int tier) {
        if (automated && (type == BackpackUpgradeType.MAGNET || type == BackpackUpgradeType.AUTO_SORT)) {
            return false;
        }
        if (realityError) {
            // Reality-error storage deliberately has no automatic sorter: its
            // manual Sort action is the roll/materialize step. Other utility
            // modules are safe because locked glitch cells are not extractable
            // by the ammo feeder until that roll has happened.
            if (type == BackpackUpgradeType.AUTO_SORT) return false;
            if (type == BackpackUpgradeType.CAPACITY && (tier < 1 || tier > 3)) return false;
        }
        return type != BackpackUpgradeType.CAPACITY || capacitySupported;
    }

    /** Removes malformed or legacy-incompatible modules, preserving the first legal module of each unique type. */
    public static List<ItemStack> takeIncompatibleUpgrades(ItemStack backpack) {
        UpgradeInventory inventory = createInventory(backpack);
        List<ItemStack> removed = new ArrayList<>();
        Set<BackpackUpgradeType> uniqueTypes = EnumSet.noneOf(BackpackUpgradeType.class);
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            boolean valid = isIntrinsicallyCompatible(backpack, stack);
            if (valid) {
                ItemBackpackUpgrade upgrade = (ItemBackpackUpgrade) stack.getItem();
                BackpackUpgradeType type = upgrade.getUpgradeType();
                if (type != BackpackUpgradeType.CAPACITY && type != BackpackUpgradeType.RANGE) {
                    valid = uniqueTypes.add(type);
                }
            }
            if (!valid) {
                ItemStack extracted = inventory.extractItem(slot, stack.getCount(), false);
                if (!extracted.isEmpty()) removed.add(extracted);
            }
        }
        return removed;
    }

    /**
     * Migrates serialized upgrade entries that belong to slots removed by a
     * newer layout. Returned stacks retain their complete metadata and NBT.
     */
    public static List<ItemStack> takeUpgradesBeyondPhysicalSlots(ItemStack backpack) {
        List<ItemStack> removed = new ArrayList<>();
        if (!backpack.hasTagCompound()) return removed;

        int slots = getUpgradeSlotCount(backpack);
        NBTTagCompound serialized = backpack.getTagCompound().getCompoundTag(UPGRADES_TAG).copy();
        NBTTagList items = serialized.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        NBTTagList kept = new NBTTagList();
        boolean changed = false;
        for (int index = 0; index < items.tagCount(); index++) {
            NBTTagCompound itemTag = items.getCompoundTagAt(index);
            if (itemTag.getInteger("Slot") >= slots) {
                ItemStack stack = new ItemStack(itemTag);
                if (!stack.isEmpty()) removed.add(stack);
                changed = true;
            } else {
                kept.appendTag(itemTag.copy());
            }
        }
        if (changed) {
            serialized.setInteger("Size", slots);
            serialized.setTag("Items", kept);
            backpack.getTagCompound().setTag(UPGRADES_TAG, serialized);
        }
        return removed;
    }

    public static final class UpgradeInventory extends ItemStackHandler {
        private final ItemStack backpack;

        private UpgradeInventory(ItemStack backpack, int slots) {
            super(Math.max(0, Math.min(MAX_UPGRADE_SLOTS, slots)));
            this.backpack = backpack;
            if (!backpack.hasTagCompound()) backpack.setTagCompound(new NBTTagCompound());
            NBTTagCompound tag = backpack.getTagCompound().getCompoundTag(UPGRADES_TAG).copy();
            tag.setInteger("Size", getSlots());
            deserializeNBT(tag);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isUpgradeCompatible(backpack, stack, this, slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (stack.isEmpty() || isItemValid(slot, stack)) super.setStackInSlot(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            backpack.getTagCompound().setTag(UPGRADES_TAG, serializeNBT());
        }
    }
}
