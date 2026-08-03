package com.hbm.saveddata.satellites;

import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import com.hbm.items.machine.ItemSatellitePayload.EnumSatelliteType;
import com.hbmspace.items.ModItemsSpace;
import com.hbmspace.saveddata.satellites.SatelliteDysonRelay;
import com.hbm.packet.toclient.SatelliteSnapshotPacket;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import com.hbmspace.tileentity.TESpaceUtil;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Stable satellite identity registry. Numeric IDs are retained only for legacy
 * saves and packet compatibility; new saves use the string key.
 */
public final class SatelliteTypeRegistry {

    private static final Map<Integer, Descriptor> BY_ID = new LinkedHashMap<>();
    private static final Map<String, Descriptor> BY_KEY = new LinkedHashMap<>();
    private static final Map<ItemKey, Descriptor> BY_ITEM = new LinkedHashMap<>();
    private static final Map<Class<? extends Satellite>, Descriptor> BY_CLASS = new LinkedHashMap<>();
    private static boolean defaultsRegistered;
    private static boolean spaceRegistered;
    private static boolean itemBindingsInitialized;

    private SatelliteTypeRegistry() { }

    public static synchronized void registerDefaults() {
        if(defaultsRegistered) return;
        defaultsRegistered = true;

        register(0, "mapper", SatelliteMapper::new, () -> canonical(EnumSatelliteType.SPY), 16_000, true,
                () -> new ItemStack(ModItems.sat_mapper));
        register(1, "scanner", SatelliteScanner::new, () -> canonical(EnumSatelliteType.SCANNER), 16_000, true,
                () -> new ItemStack(ModItems.sat_scanner));
        register(2, "radar", SatelliteRadar::new, () -> canonical(EnumSatelliteType.RADAR), 16_000, true,
                () -> new ItemStack(ModItems.sat_radar));
        register(3, "death_ray", SatelliteLaser::new, () -> canonical(EnumSatelliteType.DEATH_RAY), 48_000, false,
                () -> new ItemStack(ModItems.sat_laser));
        register(4, "resonator", SatelliteResonator::new, () -> canonical(EnumSatelliteType.XENIUM_RESONATOR), 20_000, false,
                () -> new ItemStack(ModItems.sat_resonator));
        register(5, "relay", SatelliteRelay::new, () -> canonical(EnumSatelliteType.RELAY), 20_000, false,
                () -> new ItemStack(ModItems.sat_foeq));
        register(6, "miner_astro", SatelliteMiner::new, () -> canonical(EnumSatelliteType.MINER_ASTRO), 64_000, false,
                () -> new ItemStack(ModItems.sat_miner));
        register(7, "miner_lunar", SatelliteLunarMiner::new, () -> canonical(EnumSatelliteType.MINER_LUNAR), 64_000, false,
                () -> new ItemStack(ModItems.sat_lunar_miner));
        register(8, "horizons", SatelliteHorizons::new, () -> new ItemStack(ModItems.sat_gerald), 128_000, false);
        register(9, "precision_laser", SatellitePrecisionLaser::new, () -> canonical(EnumSatelliteType.PRECISION_LASER), 48_000, false);
        register(10, "detector", SatelliteDetector::new, () -> canonical(EnumSatelliteType.DETECTOR), 16_000, true);
        register(11, "ray_scan", SatelliteRayScan::new, () -> canonical(EnumSatelliteType.RAY_SCAN), 16_000, true);
    }

    public static synchronized void registerSpaceExtensions() {
        registerDefaults();
        if(spaceRegistered) return;
        spaceRegistered = true;
        register(12, "dyson_relay", SatelliteDysonRelay::new,
                () -> new ItemStack(ModItemsSpace.sat_dyson_relay), 32_000, false);
    }

    private static ItemStack canonical(EnumSatelliteType type) {
        return new ItemStack(ModItems.satellite, 1, type.ordinal());
    }

    @SafeVarargs
    private static void register(int legacyId, String key, Supplier<? extends Satellite> factory,
                                 Supplier<ItemStack> canonical, int mass, boolean handLaunch,
                                 Supplier<ItemStack>... aliases) {
        Satellite sample = factory.get();
        @SuppressWarnings("unchecked")
        Class<? extends Satellite> satelliteClass = (Class<? extends Satellite>) sample.getClass();
        Descriptor descriptor = new Descriptor(legacyId, key, factory, satelliteClass, canonical,
                aliases == null ? Collections.emptyList() : Arrays.asList(aliases), mass, handLaunch);

        if(BY_ID.containsKey(legacyId) || BY_KEY.containsKey(key) || BY_CLASS.containsKey(satelliteClass)) {
            throw new IllegalStateException("Duplicate satellite registration: " + key + " / " + legacyId);
        }

        BY_ID.put(legacyId, descriptor);
        BY_KEY.put(key, descriptor);
        BY_CLASS.put(satelliteClass, descriptor);
        if(itemBindingsInitialized) bindItems(descriptor);
    }

    private static synchronized void ensureItemBindings() {
        if(itemBindingsInitialized) return;
        for(Descriptor descriptor : BY_ID.values()) bindItems(descriptor);
        itemBindingsInitialized = true;
    }

    private static void bindItems(Descriptor descriptor) {
        bindItem(descriptor.canonicalStack.get(), descriptor);
        for(Supplier<ItemStack> alias : descriptor.aliasStacks) bindItem(alias.get(), descriptor);
    }

    private static void bindItem(ItemStack stack, Descriptor descriptor) {
        if(stack == null || stack.isEmpty()) return;
        ItemKey itemKey = new ItemKey(stack.getItem(), stack.getMetadata());
        Descriptor old = BY_ITEM.put(itemKey, descriptor);
        if(old != null && old != descriptor) {
            throw new IllegalStateException("Satellite item alias collision: " + stack);
        }
    }

    @Nullable
    public static Descriptor byLegacyId(int id) {
        registerDefaults();
        return BY_ID.get(id);
    }

    @Nullable
    public static Descriptor byKey(String key) {
        registerDefaults();
        return BY_KEY.get(key);
    }

    @Nullable
    public static Descriptor byItem(ItemStack stack) {
        registerDefaults();
        ensureItemBindings();
        if(stack == null || stack.isEmpty()) return null;
        Descriptor exact = BY_ITEM.get(new ItemKey(stack.getItem(), stack.getMetadata()));
        if(exact != null) return exact;
        if(stack.getItem().getHasSubtypes()) return null;
        return BY_ITEM.get(new ItemKey(stack.getItem(), 0));
    }

    @Nullable
    public static Descriptor byItem(Item item) {
        return item == null ? null : byItem(new ItemStack(item));
    }

    @Nullable
    public static Descriptor bySatellite(Satellite satellite) {
        registerDefaults();
        return satellite == null ? null : BY_CLASS.get(satellite.getClass());
    }

    @Nullable
    public static Satellite createByLegacyId(int id) {
        Descriptor descriptor = byLegacyId(id);
        return descriptor == null ? null : descriptor.create();
    }

    @Nullable
    public static Satellite createByKey(String key) {
        Descriptor descriptor = byKey(key);
        return descriptor == null ? null : descriptor.create();
    }

    @Nullable
    public static Satellite createFromItem(ItemStack stack) {
        Descriptor descriptor = byItem(stack);
        return descriptor == null ? null : descriptor.create();
    }

    public static Collection<Descriptor> descriptors() {
        registerDefaults();
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static SatelliteLaunchResult orbit(World world, ItemStack payload, int frequency,
                                               double x, double y, double z, @Nullable OrbitSettings requestedSettings) {
        if(world == null || world.isRemote) return SatelliteLaunchResult.NO_TARGET_BODY;
        OrbitKey target;
        try {
            target = OrbitKey.fromWorld(world, (int)Math.floor(x), (int)Math.floor(z));
        } catch(RuntimeException error) {
            return SatelliteLaunchResult.NO_TARGET_BODY;
        }
        return orbit(world, target, payload, frequency, x, y, z, requestedSettings);
    }

    /** Launches into an explicit orbit without loading that body's surface dimension. */
    public static SatelliteLaunchResult orbit(World contextWorld, OrbitKey targetOrbit, ItemStack payload, int frequency,
                                               double x, double y, double z, @Nullable OrbitSettings requestedSettings) {
        if(contextWorld == null || contextWorld.isRemote || targetOrbit == null) return SatelliteLaunchResult.NO_TARGET_BODY;
        if(payload == null || payload.isEmpty() || frequency < 0 || frequency > 100_000) {
            return SatelliteLaunchResult.INVALID_PAYLOAD;
        }

        Descriptor descriptor = byItem(payload);
        if(descriptor == null) return SatelliteLaunchResult.INVALID_PAYLOAD;

        SatelliteHandle handle = new SatelliteHandle(targetOrbit, frequency);
        SatelliteOperationContext context = SatelliteOperationContext.create(contextWorld, handle);
        if(!context.isTargetKnown()) return SatelliteLaunchResult.NO_TARGET_BODY;

        SatelliteSavedData data;
        try {
            OrbitKey localOrbit = OrbitKey.fromWorld(contextWorld, (int)Math.floor(x), (int)Math.floor(z));
            data = targetOrbit.equals(localOrbit)
                    ? TESpaceUtil.getData(contextWorld, (int)Math.floor(x), (int)Math.floor(z))
                    : context.getData();
            synchronized(data) {
                Satellite existing = data.getSatFromFreq(frequency);
                if(existing != null) {
                    if(context.getSurfaceWorld() == null) return SatelliteLaunchResult.FREQ_OCCUPIED;
                    if(existing.onPartDelivered(context.getSurfaceWorld(), payload.copy())) {
                        data.markSatelliteDirty();
                        SatelliteSnapshotPacket.broadcastForBody(data, contextWorld);
                        return SatelliteLaunchResult.SUCCESS;
                    }
                    return SatelliteLaunchResult.FREQ_OCCUPIED;
                }

                Satellite satellite = descriptor.create();
                if(satellite == null) return SatelliteLaunchResult.INVALID_PAYLOAD;
                OrbitSettings settings = requestedSettings == null
                        ? OrbitSettings.readFromStack(payload, satellite)
                        : requestedSettings.copy();
                settings.validate();
                satellite.setOrbitSettings(settings);
                if(context.getSurfaceWorld() != null) {
                    satellite.onOrbit(context.getSurfaceWorld(), x, y, z);
                } else {
                    satellite.setTarget((int)Math.floor(x), (int)Math.floor(z));
                }
                data.putSatellite(frequency, satellite);
            }
        } catch(Throwable error) {
            MainRegistry.logger.error("Could not store satellite {} at {}", descriptor.getKey(), handle, error);
            return SatelliteLaunchResult.STORAGE_ERROR;
        }

        SatelliteSnapshotPacket.broadcastForBody(data, contextWorld);
        return SatelliteLaunchResult.SUCCESS;
    }

    public static final class Descriptor {
        private final int legacyId;
        private final String key;
        private final Supplier<? extends Satellite> factory;
        private final Class<? extends Satellite> satelliteClass;
        private final Supplier<ItemStack> canonicalStack;
        private final List<Supplier<ItemStack>> aliasStacks;
        private final int mass;
        private final boolean handLaunch;

        private Descriptor(int legacyId, String key, Supplier<? extends Satellite> factory,
                           Class<? extends Satellite> satelliteClass, Supplier<ItemStack> canonicalStack,
                           List<Supplier<ItemStack>> aliasStacks,
                           int mass, boolean handLaunch) {
            this.legacyId = legacyId;
            this.key = key;
            this.factory = factory;
            this.satelliteClass = satelliteClass;
            this.canonicalStack = canonicalStack;
            this.aliasStacks = aliasStacks;
            this.mass = mass;
            this.handLaunch = handLaunch;
        }

        public int getLegacyId() { return legacyId; }
        public String getKey() { return key; }
        public Class<? extends Satellite> getSatelliteClass() { return satelliteClass; }
        public ItemStack getCanonicalStack() { return canonicalStack.get().copy(); }
        public int getMass() { return mass; }
        public boolean canHandLaunch() { return handLaunch; }
        public Satellite create() { return factory.get(); }
    }

    private static final class ItemKey {
        private final Item item;
        private final int metadata;

        private ItemKey(Item item, int metadata) {
            this.item = item;
            this.metadata = metadata;
        }

        @Override
        public boolean equals(Object object) {
            if(this == object) return true;
            if(!(object instanceof ItemKey)) return false;
            ItemKey other = (ItemKey) object;
            return metadata == other.metadata && item == other.item;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(item), metadata);
        }
    }
}
