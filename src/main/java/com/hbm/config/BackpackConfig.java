package com.hbm.config;

import com.hbm.inventory.BackpackUpgradeManager;
import com.hbm.inventory.BackpackVirtualStorage;
import com.hbm.items.tool.ItemBackpack;
import com.hbm.items.tool.ItemBlackHoleBackpack;
import com.hbm.items.tool.ItemPocketHoleBackpack;
import com.hbm.items.tool.ItemRealityErrorBackpack;
import com.hbm.saveddata.PocketHoleBackpackSavedData;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/** Per-backpack server storage and built-in automation settings from hbm.cfg. */
public final class BackpackConfig {

    public static final String CATEGORY = CommonConfig.CATEGORY_BACKPACKS;
    public static final int MAX_STORAGE_SLOTS = 1_000_012;
    public static final int MAX_UPGRADE_SLOTS = 10;
    private static final int MAX_PHYSICAL_STORAGE_SLOTS = 4_096;
    public static final long MAX_AUTOMATION_ENERGY = Long.MAX_VALUE;

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        register("backpack_steel", 36, 1, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_titanium", 45, 2, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_lead", 45, 2, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_reinforced_steel", 54, 3, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_asbestos", 54, 3, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_duralumin", 54, 2, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_beryllium", 66, 3, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_high_strength_steel", 88, 4, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_tungsten", 77, 4, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_desh", 130, 5, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_schrabidium", 156, 6, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_dineutronium", 182, 7, true, true, false, false, true, 10_000_000L, 1_000L);
        register("backpack_black_hole", 143, 8, true, true, true, true, false, 10_000_000L, 1_000L);
        register("backpack_stalker", 45, 3, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_black_box", 54, 4, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_ash", 27, 1, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_nuclear_tourist", 36, 2, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_pocket_hole", 9, 5, true, true, false, true, false, 10_000_000L, 1_000L);
        register("backpack_sapper", 55, 3, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_smuggler", 54, 3, false, false, false, false, false, 10_000_000L, 1_000L);
        register("backpack_reality_error", 1, 2, false, false, false, false, false, 10_000_000L, 1_000L);
    }

    private BackpackConfig() {
    }

    public static void loadFromConfig(Configuration config) {
        for (Map.Entry<String, Entry> configured : ENTRIES.entrySet()) {
            String name = configured.getKey();
            Entry entry = configured.getValue();
            String category = CATEGORY + "." + name;
            entry.storageSlots = config.get(category, "storage_slots", entry.defaultStorageSlots,
                    "Finite base storage slots. Ignored while infinite_slots is true.",
                    1, MAX_STORAGE_SLOTS).getInt();
            entry.upgradeSlots = config.get(category, "upgrade_slots", entry.defaultUpgradeSlots,
                    "Number of backpack upgrade slots.", 0, MAX_UPGRADE_SLOTS).getInt();
            entry.defaultAutoMagnet = config.get(category, "default_auto_magnet",
                    entry.defaultBuiltInMagnet,
                    "Provides a built-in, initially enabled magnet. Magnet upgrades become incompatible.")
                    .getBoolean();
            entry.defaultAutoSort = config.get(category, "default_auto_sort",
                    entry.defaultBuiltInSort,
                    "Provides built-in, initially enabled auto-sort. Auto-sort upgrades become incompatible.")
                    .getBoolean();
            entry.autoMagnetRequiresEnergy = config.get(category, "auto_magnet_requires_energy",
                    entry.defaultAutoMagnetRequiresEnergy,
                    "When true, the built-in magnet requires stored HE for every successful pickup operation.")
                    .getBoolean();
            entry.autoMagnetEnergyCapacity = Math.max(1L, config.get(category, "auto_magnet_energy_capacity",
                    Long.toString(entry.defaultAutoMagnetEnergyCapacity),
                    "Maximum HE stored by this backpack while its built-in magnet requires energy.").getLong());
            entry.autoMagnetEnergyPerOperation = Math.max(0L, config.get(category, "auto_magnet_energy_per_operation",
                    Long.toString(entry.defaultAutoMagnetEnergyPerOperation),
                    "HE consumed by one successful built-in magnet pickup operation.").getLong());
            entry.infiniteSlots = config.get(category, "infinite_slots", entry.defaultInfiniteSlots,
                    "Uses sparse server storage with effectively unlimited paged slots.").getBoolean();
            entry.allowOverstack = config.get(category, "allow_overstack", entry.defaultAllowOverstack,
                    "Allows one logical slot to store more than the item's normal stack limit.").getBoolean();
        }
    }

    public static int getBaseStorageSlots(ItemBackpack item, int fallback) {
        Entry entry = get(item);
        return entry == null ? fallback : entry.storageSlots;
    }

    public static int getStorageSlots(ItemStack backpack, int fallbackBase) {
        if (!(backpack.getItem() instanceof ItemBackpack item)) return 0;
        if (hasInfiniteSlots(backpack)) return MAX_STORAGE_SLOTS;
        return Math.max(1, Math.min(MAX_STORAGE_SLOTS,
                BackpackUpgradeManager.getStorageCapacity(backpack, getBaseStorageSlots(item, fallbackBase))));
    }

    public static int getUpgradeSlots(ItemStack backpack, int fallback) {
        Entry entry = get(backpack);
        return entry == null ? clampUpgradeSlots(fallback) : entry.upgradeSlots;
    }

    public static int getUpgradeSlots(String registryPath, int fallback) {
        Entry entry = ENTRIES.get(registryPath);
        return entry == null ? clampUpgradeSlots(fallback) : entry.upgradeSlots;
    }

    public static boolean hasBuiltInMagnet(ItemStack backpack) {
        Entry entry = get(backpack);
        return entry != null && entry.defaultAutoMagnet;
    }

    public static boolean hasBuiltInAutoSort(ItemStack backpack) {
        Entry entry = get(backpack);
        return entry != null && entry.defaultAutoSort;
    }

    public static boolean hasInfiniteSlots(ItemStack backpack) {
        Entry entry = get(backpack);
        return entry != null && entry.infiniteSlots;
    }

    public static boolean allowsOverstack(ItemStack backpack) {
        Entry entry = get(backpack);
        return entry != null && entry.allowOverstack;
    }

    public static boolean autoMagnetRequiresEnergy(ItemStack backpack) {
        Entry entry = get(backpack);
        return entry != null && entry.defaultAutoMagnet && entry.autoMagnetRequiresEnergy;
    }

    public static long getAutoMagnetEnergyCapacity(ItemStack backpack) {
        Entry entry = get(backpack);
        return entry == null ? 0L : Math.max(1L, entry.autoMagnetEnergyCapacity);
    }

    public static long getAutoMagnetEnergyPerOperation(ItemStack backpack) {
        Entry entry = get(backpack);
        return entry == null ? 0L : Math.max(0L, entry.autoMagnetEnergyPerOperation);
    }

    public static boolean usesPocketStorage(ItemStack backpack) {
        return backpack.getItem() instanceof ItemPocketHoleBackpack
                && !hasInfiniteSlots(backpack) && allowsOverstack(backpack)
                && getStorageSlots(backpack, ItemPocketHoleBackpack.VISIBLE_SLOTS)
                <= PocketHoleBackpackSavedData.PocketHoleStorage.MAX_SLOTS;
    }

    public static boolean usesSparseStorage(ItemStack backpack) {
        if (backpack.getItem() instanceof ItemBlackHoleBackpack) return true;
        if (backpack.hasTagCompound()
                && backpack.getTagCompound().hasUniqueId(BackpackVirtualStorage.STORAGE_ID_TAG)) return true;
        if (backpack.getItem() instanceof ItemPocketHoleBackpack && !usesPocketStorage(backpack)) return true;
        if (backpack.getItem() instanceof ItemRealityErrorBackpack && hasBuiltInAutoSort(backpack)) return true;
        Entry entry = get(backpack);
        if (entry != null && getStorageSlots(backpack, entry.storageSlots) > MAX_PHYSICAL_STORAGE_SLOTS) return true;
        return !usesPocketStorage(backpack)
                && (hasInfiniteSlots(backpack) || allowsOverstack(backpack));
    }

    private static int clampUpgradeSlots(int value) {
        return Math.max(0, Math.min(MAX_UPGRADE_SLOTS, value));
    }

    private static Entry get(ItemStack backpack) {
        return backpack.getItem() instanceof ItemBackpack item ? get(item) : null;
    }

    private static Entry get(ItemBackpack item) {
        return item.getRegistryName() == null ? null : ENTRIES.get(item.getRegistryName().getPath());
    }

    private static void register(String name, int storageSlots, int upgradeSlots,
                                 boolean autoMagnet, boolean autoSort,
                                 boolean infiniteSlots, boolean allowOverstack,
                                 boolean autoMagnetRequiresEnergy, long energyCapacity,
                                 long energyPerOperation) {
        ENTRIES.put(name, new Entry(storageSlots, upgradeSlots, autoMagnet, autoSort,
                infiniteSlots, allowOverstack, autoMagnetRequiresEnergy,
                energyCapacity, energyPerOperation));
    }

    private static final class Entry {
        private final int defaultStorageSlots;
        private final int defaultUpgradeSlots;
        private final boolean defaultBuiltInMagnet;
        private final boolean defaultBuiltInSort;
        private final boolean defaultInfiniteSlots;
        private final boolean defaultAllowOverstack;
        private final boolean defaultAutoMagnetRequiresEnergy;
        private final long defaultAutoMagnetEnergyCapacity;
        private final long defaultAutoMagnetEnergyPerOperation;
        private int storageSlots;
        private int upgradeSlots;
        private boolean defaultAutoMagnet;
        private boolean defaultAutoSort;
        private boolean infiniteSlots;
        private boolean allowOverstack;
        private boolean autoMagnetRequiresEnergy;
        private long autoMagnetEnergyCapacity;
        private long autoMagnetEnergyPerOperation;

        private Entry(int storageSlots, int upgradeSlots, boolean autoMagnet, boolean autoSort,
                      boolean infiniteSlots, boolean allowOverstack,
                      boolean autoMagnetRequiresEnergy, long energyCapacity,
                      long energyPerOperation) {
            this.defaultStorageSlots = this.storageSlots = storageSlots;
            this.defaultUpgradeSlots = this.upgradeSlots = clampUpgradeSlots(upgradeSlots);
            this.defaultBuiltInMagnet = this.defaultAutoMagnet = autoMagnet;
            this.defaultBuiltInSort = this.defaultAutoSort = autoSort;
            this.defaultInfiniteSlots = this.infiniteSlots = infiniteSlots;
            this.defaultAllowOverstack = this.allowOverstack = allowOverstack;
            this.defaultAutoMagnetRequiresEnergy = this.autoMagnetRequiresEnergy = autoMagnetRequiresEnergy;
            this.defaultAutoMagnetEnergyCapacity = this.autoMagnetEnergyCapacity = Math.max(1L, energyCapacity);
            this.defaultAutoMagnetEnergyPerOperation = this.autoMagnetEnergyPerOperation =
                    Math.max(0L, energyPerOperation);
        }
    }
}
