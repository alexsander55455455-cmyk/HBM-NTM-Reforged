package com.hbm.saveddata.satellites;

import com.hbm.main.MainRegistry;
import com.hbmspace.dim.CelestialBody;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central satellite persistence, split into stable celestial-body namespaces.
 *
 * <p>The registered root lives in the overworld. Callers receive a body-bound
 * view, so cached references cannot accidentally switch to another planet when
 * a different dimension queries the store.</p>
 */
public class SatelliteSavedData extends WorldSavedData {

    public static final String LEGACY_DATA_NAME = "satellites";
    public static final String DATA_NAME = "hbm_satellites_v2";
    public static final int MAX_CLIENT_SNAPSHOT_RECORDS = 4_096;
    private static final int FORMAT_VERSION = 2;

    private final Map<String, Int2ObjectOpenHashMap<Satellite>> satellitesByBody = new HashMap<>();
    private final Map<String, List<NBTTagCompound>> quarantineByBody = new HashMap<>();
    private final Set<String> migratedBodies = new HashSet<>();
    private final Map<String, SatelliteSavedData> views = new HashMap<>();

    @Nullable
    private final SatelliteSavedData root;
    private final String bodyKey;
    public final Int2ObjectOpenHashMap<Satellite> sats;
    private long revision;

    /** Constructor used by MapStorage. */
    public SatelliteSavedData(String name) {
        super(name);
        root = null;
        bodyKey = "";
        sats = new Int2ObjectOpenHashMap<>();
    }

    public SatelliteSavedData() {
        this(DATA_NAME);
        super.markDirty();
    }

    private SatelliteSavedData(SatelliteSavedData root, String bodyKey) {
        super(DATA_NAME + "@" + bodyKey);
        this.root = root;
        this.bodyKey = bodyKey;
        this.sats = root.satellitesByBody.computeIfAbsent(bodyKey, ignored -> new Int2ObjectOpenHashMap<>());
    }

    private SatelliteSavedData root() {
        return root == null ? this : root;
    }

    private SatelliteSavedData view(String key) {
        SatelliteSavedData actualRoot = root();
        OrbitKey parsed = OrbitKey.parse(key);
        String canonical = parsed == null ? key : parsed.asString();
        return actualRoot.views.computeIfAbsent(canonical, ignored -> new SatelliteSavedData(actualRoot, canonical));
    }

    public String getBodyKey() {
        return bodyKey;
    }

    @Nullable
    public OrbitKey getOrbitKey() {
        return OrbitKey.parse(bodyKey);
    }

    public long getRevision() {
        return root().revision;
    }

    public NBTTagCompound createClientSnapshot() {
        NBTTagCompound snapshot = new NBTTagCompound();
        snapshot.setString("bodyKey", bodyKey);
        snapshot.setLong("revision", getRevision());
        NBTTagList list = new NBTTagList();
        int[] frequencies = sats.keySet().toIntArray();
        Arrays.sort(frequencies);
        int recordCount = Math.min(frequencies.length, MAX_CLIENT_SNAPSHOT_RECORDS);
        for(int index = 0; index < recordCount; index++) {
            int frequency = frequencies[index];
            Satellite satellite = sats.get(frequency);
            SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.bySatellite(satellite);
            if(descriptor == null) continue;
            NBTTagCompound record = new NBTTagCompound();
            record.setInteger("frequency", frequency);
            record.setString("type", descriptor.getKey());
            record.setInteger("legacyId", descriptor.getLegacyId());
            record.setTag("orbit", satellite.getOrbitSettings().writeToNBT());
            list.appendTag(record);
        }
        snapshot.setTag("satellites", list);
        snapshot.setBoolean("truncated", frequencies.length > recordCount);
        snapshot.setInteger("totalRecords", frequencies.length);
        return snapshot;
    }

    public boolean isFreqTaken(int frequency) {
        return getSatFromFreq(frequency) != null;
    }

    @Nullable
    public Satellite getSatFromFreq(int frequency) {
        return sats.get(frequency);
    }

    public void putSatellite(int frequency, Satellite satellite) {
        if(satellite == null) return;
        sats.put(frequency, satellite);
        markSatelliteDirty();
    }

    @Nullable
    public Satellite removeSatellite(int frequency) {
        Satellite removed = sats.remove(frequency);
        if(removed != null) markSatelliteDirty();
        return removed;
    }

    public void markSatelliteDirty() {
        SatelliteSavedData actualRoot = root();
        actualRoot.revision++;
        actualRoot.markDirty();
    }

    @Override
    public void markDirty() {
        if(root != null) {
            root.markSatelliteDirty();
        } else {
            super.markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        if(root != null) {
            root.readFromNBT(nbt);
            return;
        }

        satellitesByBody.clear();
        quarantineByBody.clear();
        migratedBodies.clear();
        views.clear();
        revision = nbt.getLong("revision");

        NBTTagList bodyList = nbt.getTagList("bodies", NBT.TAG_COMPOUND);
        for(int bodyIndex = 0; bodyIndex < bodyList.tagCount(); bodyIndex++) {
            NBTTagCompound bodyTag = bodyList.getCompoundTagAt(bodyIndex);
            OrbitKey orbitKey = OrbitKey.parse(bodyTag.getString("key"));
            if(orbitKey == null) continue;
            String key = orbitKey.asString();

            Int2ObjectOpenHashMap<Satellite> bodySatellites =
                    satellitesByBody.computeIfAbsent(key, ignored -> new Int2ObjectOpenHashMap<>());
            NBTTagList satelliteList = bodyTag.getTagList("satellites", NBT.TAG_COMPOUND);
            for(int satelliteIndex = 0; satelliteIndex < satelliteList.tagCount(); satelliteIndex++) {
                NBTTagCompound record = satelliteList.getCompoundTagAt(satelliteIndex);
                Satellite satellite = createFromRecord(record);
                if(satellite == null) {
                    quarantine(key, record, "unknown_type");
                    continue;
                }

                int frequency = record.getInteger("frequency");
                if(frequency < 0 || frequency > 100_000) {
                    quarantine(key, record, "invalid_frequency");
                    continue;
                }
                if(bodySatellites.containsKey(frequency)) {
                    quarantine(key, record, "duplicate_frequency");
                    continue;
                }

                readSatelliteState(satellite, record);
                bodySatellites.put(frequency, satellite);
            }

            NBTTagList quarantineList = bodyTag.getTagList("quarantine", NBT.TAG_COMPOUND);
            for(int quarantineIndex = 0; quarantineIndex < quarantineList.tagCount(); quarantineIndex++) {
                quarantineByBody.computeIfAbsent(key, ignored -> new ArrayList<>())
                        .add(quarantineList.getCompoundTagAt(quarantineIndex).copy());
            }
        }

        NBTTagList migratedList = nbt.getTagList("migratedBodies", NBT.TAG_STRING);
        for(int i = 0; i < migratedList.tagCount(); i++) {
            migratedBodies.add(migratedList.getStringTagAt(i));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if(root != null) return root.writeToNBT(nbt);

        nbt.setInteger("formatVersion", FORMAT_VERSION);
        nbt.setLong("revision", revision);
        NBTTagList bodyList = new NBTTagList();

        Set<String> allBodyKeys = new HashSet<>(satellitesByBody.keySet());
        allBodyKeys.addAll(quarantineByBody.keySet());
        for(String key : allBodyKeys) {
            NBTTagCompound bodyTag = new NBTTagCompound();
            bodyTag.setString("key", key);

            NBTTagList satelliteList = new NBTTagList();
            List<NBTTagCompound> runtimeUnknown = new ArrayList<>();
            Int2ObjectOpenHashMap<Satellite> bodySatellites = satellitesByBody.get(key);
            if(bodySatellites != null) {
                for(Int2ObjectMap.Entry<Satellite> entry : bodySatellites.int2ObjectEntrySet()) {
                    SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.bySatellite(entry.getValue());
                    if(descriptor == null) {
                        NBTTagCompound unknown = new NBTTagCompound();
                        unknown.setInteger("frequency", entry.getIntKey());
                        unknown.setString("runtimeClass", entry.getValue().getClass().getName());
                        unknown.setString("quarantineReason", "unregistered_runtime_type");
                        runtimeUnknown.add(unknown);
                        continue;
                    }

                    NBTTagCompound record = new NBTTagCompound();
                    record.setInteger("frequency", entry.getIntKey());
                    record.setString("type", descriptor.getKey());
                    record.setInteger("legacyId", descriptor.getLegacyId());
                    writeSatelliteState(entry.getValue(), record);
                    satelliteList.appendTag(record);
                }
            }
            bodyTag.setTag("satellites", satelliteList);

            NBTTagList quarantineList = new NBTTagList();
            List<NBTTagCompound> quarantined = quarantineByBody.get(key);
            if(quarantined != null) {
                for(NBTTagCompound record : quarantined) quarantineList.appendTag(record.copy());
            }
            for(NBTTagCompound record : runtimeUnknown) quarantineList.appendTag(record);
            bodyTag.setTag("quarantine", quarantineList);
            bodyList.appendTag(bodyTag);
        }
        nbt.setTag("bodies", bodyList);

        NBTTagList migratedList = new NBTTagList();
        for(String key : migratedBodies) migratedList.appendTag(new net.minecraft.nbt.NBTTagString(key));
        nbt.setTag("migratedBodies", migratedList);
        return nbt;
    }

    @Nullable
    private static Satellite createFromRecord(NBTTagCompound record) {
        String type = record.getString("type");
        if(!type.isEmpty()) return SatelliteTypeRegistry.createByKey(type);
        if(record.hasKey("legacyId", NBT.TAG_ANY_NUMERIC)) {
            return SatelliteTypeRegistry.createByLegacyId(record.getInteger("legacyId"));
        }
        return null;
    }

    private static void writeSatelliteState(Satellite satellite, NBTTagCompound record) {
        NBTTagCompound satelliteData = new NBTTagCompound();
        satellite.writeToNBT(satelliteData);
        record.setTag("data", satelliteData);
        record.setInteger("targetX", satellite.targetX);
        record.setInteger("targetZ", satellite.targetZ);
        record.setString("tx", satellite.tx);
        record.setTag("orbit", satellite.getOrbitSettings().writeToNBT());
    }

    private static void readSatelliteState(Satellite satellite, NBTTagCompound record) {
        satellite.readFromNBT(record.getCompoundTag("data"));
        if(record.hasKey("targetX", NBT.TAG_ANY_NUMERIC)) satellite.targetX = record.getInteger("targetX");
        if(record.hasKey("targetZ", NBT.TAG_ANY_NUMERIC)) satellite.targetZ = record.getInteger("targetZ");
        if(record.hasKey("tx", NBT.TAG_STRING)) satellite.tx = record.getString("tx");
        satellite.setOrbitSettings(OrbitSettings.readFromNBT(record.getCompoundTag("orbit"), satellite));
    }

    private void quarantine(String key, NBTTagCompound original, String reason) {
        NBTTagCompound record = original.copy();
        record.setString("quarantineReason", reason);
        quarantineByBody.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
    }

    private void migrateLegacy(World sourceWorld, String key) {
        SatelliteSavedData actualRoot = root();
        if(actualRoot.migratedBodies.contains(key)) return;

        LegacySatelliteSavedData legacy = (LegacySatelliteSavedData) sourceWorld.getPerWorldStorage()
                .getOrLoadData(LegacySatelliteSavedData.class, LEGACY_DATA_NAME);
        if(legacy != null) {
            Int2ObjectOpenHashMap<Satellite> target =
                    actualRoot.satellitesByBody.computeIfAbsent(key, ignored -> new Int2ObjectOpenHashMap<>());
            for(NBTTagCompound legacyRecord : legacy.records) {
                int frequency = legacyRecord.getInteger("frequency");
                if(frequency < 0 || frequency > 100_000) {
                    actualRoot.quarantine(key, legacyRecord, "invalid_legacy_frequency");
                    continue;
                }
                if(target.containsKey(frequency)) {
                    actualRoot.quarantine(key, legacyRecord, "migration_frequency_collision");
                    continue;
                }

                int legacyId = legacyRecord.getInteger("legacyId");
                Satellite satellite = SatelliteTypeRegistry.createByLegacyId(legacyId);
                if(satellite == null) {
                    actualRoot.quarantine(key, legacyRecord, "unknown_legacy_id");
                    continue;
                }

                NBTTagCompound legacyData = legacyRecord.getCompoundTag("data");
                satellite.readFromNBT(legacyData);
                if(legacyData.hasKey("targetX", NBT.TAG_ANY_NUMERIC)) satellite.targetX = legacyData.getInteger("targetX");
                if(legacyData.hasKey("targetZ", NBT.TAG_ANY_NUMERIC)) satellite.targetZ = legacyData.getInteger("targetZ");
                if(legacyData.hasKey("tx", NBT.TAG_STRING)) satellite.tx = legacyData.getString("tx");
                satellite.setOrbitSettings(OrbitSettings.readFromNBT(legacyData, satellite));
                target.put(frequency, satellite);
            }
        }

        actualRoot.migratedBodies.add(key);
        actualRoot.revision++;
        actualRoot.markDirty();
    }

    public static SatelliteSavedData getData(World world) {
        return getData(world, bodyKey(world), true);
    }

    /** Returns a body-bound view without loading that body's surface dimension. */
    public static SatelliteSavedData getDataForBody(World contextWorld, CelestialBody body) {
        if(body == null) return getData(contextWorld);
        return getDataForOrbit(contextWorld, OrbitKey.body(body));
    }

    /** Returns a body-bound view without loading a surface dimension. */
    public static SatelliteSavedData getDataForOrbit(World contextWorld, OrbitKey orbitKey) {
        if(contextWorld == null || orbitKey == null) throw new IllegalArgumentException("Orbit storage context is incomplete");
        return getData(contextWorld, orbitKey.asString(), false);
    }

    private static SatelliteSavedData getData(World world, String key, boolean migrateCurrentWorld) {
        World storageWorld = centralStorageWorld(world);
        MapStorage storage = storageWorld.getPerWorldStorage();
        SatelliteSavedData root = (SatelliteSavedData) storage.getOrLoadData(SatelliteSavedData.class, DATA_NAME);
        if(root == null) {
            root = new SatelliteSavedData();
            storage.setData(DATA_NAME, root);
        }

        if(migrateCurrentWorld && !world.isRemote) root.migrateLegacy(world, key);
        return root.view(key);
    }

    private static World centralStorageWorld(World world) {
        if(world != null && !world.isRemote && world.getMinecraftServer() != null) {
            WorldServer overworld = world.getMinecraftServer().getWorld(0);
            if(overworld != null) return overworld;
        }
        return world;
    }

    public static String bodyKey(World world) {
        try {
            return OrbitKey.fromWorld(world, 0, 0).asString();
        } catch(Throwable error) {
            MainRegistry.logger.debug("Could not resolve satellite celestial body for dimension "
                    + world.provider.getDimension(), error);
        }
        return "dim:" + world.provider.getDimension();
    }

    public static String bodyKey(CelestialBody body) {
        return body == null || body.name == null || body.name.isEmpty() ? "" : OrbitKey.body(body).asString();
    }

    /**
     * Resolves a chip in the compatibility order: explicit orbit, local orbit,
     * then a unique match across the central store.
     */
    public SatelliteLookupResult resolveSatellite(@Nullable OrbitKey explicitOrbit,
                                                  @Nullable OrbitKey localOrbit, int frequency) {
        if(frequency < 0 || frequency > 100_000) return SatelliteLookupResult.notFound();
        SatelliteSavedData actualRoot = root();

        SatelliteLookupResult explicit = actualRoot.lookup(explicitOrbit, frequency);
        if(explicit.isFound()) return explicit;
        if(localOrbit != null && !localOrbit.equals(explicitOrbit)) {
            SatelliteLookupResult local = actualRoot.lookup(localOrbit, frequency);
            if(local.isFound()) return local;
        }

        List<String> keys = new ArrayList<>(actualRoot.satellitesByBody.keySet());
        Collections.sort(keys);
        SatelliteLookupResult unique = null;
        for(String key : keys) {
            Int2ObjectOpenHashMap<Satellite> map = actualRoot.satellitesByBody.get(key);
            Satellite satellite = map == null ? null : map.get(frequency);
            if(satellite == null) continue;
            OrbitKey orbitKey = OrbitKey.parse(key);
            if(orbitKey == null) continue;
            if(unique != null) return SatelliteLookupResult.ambiguous();
            SatelliteSavedData data = actualRoot.view(orbitKey.asString());
            unique = SatelliteLookupResult.found(new SatelliteHandle(orbitKey, frequency), satellite, data);
        }
        return unique == null ? SatelliteLookupResult.notFound() : unique;
    }

    private SatelliteLookupResult lookup(@Nullable OrbitKey orbitKey, int frequency) {
        if(orbitKey == null) return SatelliteLookupResult.notFound();
        Int2ObjectOpenHashMap<Satellite> map = satellitesByBody.get(orbitKey.asString());
        Satellite satellite = map == null ? null : map.get(frequency);
        if(satellite == null) return SatelliteLookupResult.notFound();
        SatelliteSavedData data = view(orbitKey.asString());
        return SatelliteLookupResult.found(new SatelliteHandle(orbitKey, frequency), satellite, data);
    }

    private static final Map<String, Int2ObjectOpenHashMap<Satellite>> CLIENT_SATS_BY_BODY = new HashMap<>();
    private static String activeClientBodyKey = "";

    @SideOnly(Side.CLIENT)
    public static void setClientSats(Int2ObjectOpenHashMap<Satellite> satellites) {
        setClientSats(activeClientBodyKey, satellites);
    }

    @SideOnly(Side.CLIENT)
    public static void setClientSats(String bodyKey, Int2ObjectOpenHashMap<Satellite> satellites) {
        activeClientBodyKey = bodyKey == null ? "" : bodyKey;
        CLIENT_SATS_BY_BODY.put(activeClientBodyKey,
                satellites == null ? new Int2ObjectOpenHashMap<>() : satellites);
    }

    @SideOnly(Side.CLIENT)
    public static Int2ObjectOpenHashMap<Satellite> getClientSats() {
        return CLIENT_SATS_BY_BODY.getOrDefault(activeClientBodyKey, new Int2ObjectOpenHashMap<>());
    }

    @SideOnly(Side.CLIENT)
    public static Int2ObjectOpenHashMap<Satellite> getClientSats(String bodyKey) {
        return CLIENT_SATS_BY_BODY.getOrDefault(bodyKey == null ? "" : bodyKey, new Int2ObjectOpenHashMap<>());
    }

    @SideOnly(Side.CLIENT)
    public static void clearClientSnapshots() {
        CLIENT_SATS_BY_BODY.clear();
        activeClientBodyKey = "";
    }

    @SideOnly(Side.CLIENT)
    public static void applyClientSnapshot(@Nullable NBTTagCompound snapshot) {
        Int2ObjectOpenHashMap<Satellite> satellites = new Int2ObjectOpenHashMap<>();
        if(snapshot != null) {
            NBTTagList list = snapshot.getTagList("satellites", NBT.TAG_COMPOUND);
            int recordCount = Math.min(list.tagCount(), MAX_CLIENT_SNAPSHOT_RECORDS);
            for(int i = 0; i < recordCount; i++) {
                NBTTagCompound record = list.getCompoundTagAt(i);
                Satellite satellite = createFromRecord(record);
                if(satellite == null) continue;
                satellite.setOrbitSettings(OrbitSettings.readFromNBT(record.getCompoundTag("orbit"), satellite));
                satellites.put(record.getInteger("frequency"), satellite);
            }
        }
        String key = snapshot == null ? "" : snapshot.getString("bodyKey");
        setClientSats(key, satellites);
    }

    /**
     * Read-only adapter for the original numeric per-dimension save. It is
     * deliberately never written back, so rollback remains possible.
     */
    public static final class LegacySatelliteSavedData extends WorldSavedData {
        private final List<NBTTagCompound> records = new ArrayList<>();

        public LegacySatelliteSavedData(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            records.clear();
            int count = Math.max(0, nbt.getInteger("satCount"));
            for(int i = 0; i < count; i++) {
                NBTTagCompound record = new NBTTagCompound();
                record.setInteger("frequency", nbt.getInteger("sat_freq_" + i));
                record.setInteger("legacyId", nbt.getInteger("sat_id_" + i));
                NBTTagCompound data = nbt.getCompoundTag("sat_data_" + i);
                record.setTag("data", data.copy());
                records.add(record);
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            return nbt;
        }
    }
}
