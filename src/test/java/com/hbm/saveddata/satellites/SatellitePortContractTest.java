package com.hbm.saveddata.satellites;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import com.hbm.items.ISatChip;
import net.minecraftforge.common.util.Constants.NBT;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatellitePortContractTest {

    @Test
    void allOfficialDetectorAndRayScanEmittersStayConnected() throws Exception {
        assertSourceContains("com/hbm/blocks/bomb/Landmine.java", "SatelliteDetector.reportEvent");
        assertSourceContains("com/hbm/items/weapon/grenade/ItemGrenadeFilling.java", "SatelliteDetector.reportEvent");
        assertSourceContains("com/hbm/items/weapon/sedna/factory/XFactoryCatapult.java", "SatelliteDetector.reportEvent");
        assertSourceContains("com/hbm/entity/logic/EntityNukeExplosionMK3.java", "SatelliteDetector.reportEvent");
        assertSourceContains("com/hbm/entity/logic/EntityNukeExplosionMK5.java", "SatelliteDetector.reportEvent");
        assertSourceContains("com/hbm/tileentity/machine/TileEntityMachineRadarNT.java", "SatelliteRayScan.reportEvent");
        assertSourceContains("com/hbm/tileentity/machine/albion/TileEntityPADetector.java", "SatelliteRayScan.reportEvent");
        assertSourceContains("com/hbm/tileentity/machine/rbmk/TileEntityRBMKRod.java", "SatelliteRayScan.reportEvent");
        assertSourceContains("com/hbm/tileentity/machine/fusion/TileEntityFusionTorus.java", "SatelliteRayScan.reportEvent");
        assertSourceContains("com/hbm/tileentity/machine/TileEntityReactorZirnox.java", "SatelliteRayScan.reportEvent");
        assertSourceContains("com/hbm/tileentity/machine/TileEntityPWRController.java", "SatelliteRayScan.reportEvent");
        assertSourceContains("com/hbm/tileentity/machine/TileEntityCore.java", "SatelliteRayScan.reportEvent");
        assertSourceContains("com/hbm/tileentity/machine/TileEntityICF.java", "SatelliteRayScan.reportEvent");
        assertSourceContains("com/hbm/tileentity/machine/TileEntityMachineSatLink.java", "SatelliteRayScan.reportEvent");
    }

    static {
        Bootstrap.register();
    }

    @Test
    void orbitSettingsClampUntrustedValuesAndRoundTripOwnership() {
        UUID owner = UUID.randomUUID();
        OrbitSettings settings = new OrbitSettings();
        settings.setInclination(Float.NaN);
        settings.setAltitudeKm(500F);
        settings.setPhase(-20F);
        settings.setColor(-1F, 0.5F, Float.POSITIVE_INFINITY);
        settings.setBlinking(false);
        settings.setBlinkSeconds(0.01F);
        settings.setOwner(owner, repeat('x', 80));

        OrbitSettings restored = OrbitSettings.readFromNBT(settings.writeToNBT(), null);

        assertEquals(0F, restored.getInclination());
        assertEquals(OrbitSettings.MAX_ALTITUDE_KM, restored.getAltitudeKm());
        assertEquals(340F, restored.getPhase());
        assertEquals(0F, restored.getRed());
        assertEquals(0.5F, restored.getGreen());
        assertEquals(1F, restored.getBlue());
        assertEquals(false, restored.isBlinking());
        assertEquals(OrbitSettings.MIN_BLINK_SECONDS, restored.getBlinkSeconds());
        assertEquals(owner, restored.getOwnerUuid());
        assertEquals(64, restored.getOwnerName().length());
    }

    @Test
    void minerKeepsBaseTargetTransmissionAndCooldownState() {
        SatelliteMiner miner = new SatelliteMiner();
        miner.onCommand(null, Satellite.CMD_SETTARGET, "18", "-27");
        miner.onCommand(null, Satellite.CMD_GETTARGET);
        miner.lastOp = 123456789L;

        NBTTagCompound nbt = new NBTTagCompound();
        miner.writeToNBT(nbt);
        SatelliteMiner restored = new SatelliteMiner();
        restored.readFromNBT(nbt);

        assertEquals(18, restored.getTargetX());
        assertEquals(-27, restored.getTargetZ());
        assertEquals("18;-27", restored.getTransmission());
        assertEquals(123456789L, restored.lastOp);
    }

    @Test
    void jameOrbitTagsMigrateWithoutLosingVisualSettings() {
        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setFloat("satInclination", -42F);
        legacy.setFloat("satAltitude", 112F);
        legacy.setFloat("satPhaseOffset", 375F);
        legacy.setFloat("satColorR", 0.2F);
        legacy.setFloat("satColorG", 0.3F);
        legacy.setFloat("satColorB", 0.4F);
        legacy.setBoolean("satIsBlinking", true);
        legacy.setFloat("satBlink", 0.8F);
        legacy.setString("satOwner", "Jame owner");

        OrbitSettings restored = OrbitSettings.readFromNBT(legacy, new SatelliteScanner());

        assertEquals(-42F, restored.getInclination());
        assertEquals(112F, restored.getAltitudeKm());
        assertEquals(15F, restored.getPhase());
        assertEquals(0.2F, restored.getRed());
        assertEquals(0.3F, restored.getGreen());
        assertEquals(0.4F, restored.getBlue());
        assertEquals(true, restored.isBlinking());
        assertEquals(0.8F, restored.getBlinkSeconds());
        assertEquals("Jame owner", restored.getOwnerName());
    }

    @Test
    void commonTargetCommandsAndStateSurviveNbtRoundTrip() {
        SatelliteScanner satellite = new SatelliteScanner();
        satellite.onCommand(null, Satellite.CMD_SETTARGET, "120", "-340");
        satellite.onCommand(null, Satellite.CMD_GETTARGET);
        assertEquals("120;-340", satellite.getTransmission());

        NBTTagCompound nbt = new NBTTagCompound();
        satellite.writeToNBT(nbt);
        SatelliteScanner restored = new SatelliteScanner();
        restored.readFromNBT(nbt);

        assertEquals(120, restored.getTargetX());
        assertEquals(-340, restored.getTargetZ());
        assertEquals("120;-340", restored.getTransmission());

        restored.onCommand(null, Satellite.CMD_SETTARGET, "invalid", "15");
        assertEquals(120, restored.getTargetX());
        assertEquals(15, restored.getTargetZ());
    }

    @Test
    void centralSaveQuarantinesUnknownAndDuplicateRecordsWithoutMultiplyingThem() {
        NBTTagCompound input = new NBTTagCompound();
        NBTTagCompound body = new NBTTagCompound();
        body.setString("key", "body:Kerbin");

        NBTTagList satellites = new NBTTagList();
        satellites.appendTag(record(77, "scanner"));
        satellites.appendTag(record(77, "radar"));
        NBTTagCompound unknownFutureRecord = record(99, "future_satellite_type");
        unknownFutureRecord.setInteger("legacyId", 2);
        satellites.appendTag(unknownFutureRecord);
        body.setTag("satellites", satellites);

        NBTTagList bodies = new NBTTagList();
        bodies.appendTag(body);
        input.setTag("bodies", bodies);

        SatelliteSavedData data = new SatelliteSavedData();
        data.readFromNBT(input);
        NBTTagCompound firstWrite = data.writeToNBT(new NBTTagCompound());
        NBTTagCompound firstBody = firstWrite.getTagList("bodies", NBT.TAG_COMPOUND).getCompoundTagAt(0);

        assertEquals(1, firstBody.getTagList("satellites", NBT.TAG_COMPOUND).tagCount());
        NBTTagList quarantine = firstBody.getTagList("quarantine", NBT.TAG_COMPOUND);
        assertEquals(2, quarantine.tagCount());
        assertTrue(hasQuarantineReason(quarantine, "duplicate_frequency"));
        assertTrue(hasQuarantineReason(quarantine, "unknown_type"));

        SatelliteSavedData reloaded = new SatelliteSavedData();
        reloaded.readFromNBT(firstWrite);
        NBTTagCompound secondWrite = reloaded.writeToNBT(new NBTTagCompound());
        NBTTagCompound secondBody = secondWrite.getTagList("bodies", NBT.TAG_COMPOUND).getCompoundTagAt(0);
        assertEquals(2, secondBody.getTagList("quarantine", NBT.TAG_COMPOUND).tagCount());
    }

    @Test
    void stableRegistryContainsAllOfficialTypesAndDysonExtension() {
        SatelliteTypeRegistry.registerSpaceExtensions();
        Map<Integer, String> expected = new HashMap<>();
        expected.put(0, "mapper");
        expected.put(1, "scanner");
        expected.put(2, "radar");
        expected.put(3, "death_ray");
        expected.put(4, "resonator");
        expected.put(5, "relay");
        expected.put(6, "miner_astro");
        expected.put(7, "miner_lunar");
        expected.put(8, "horizons");
        expected.put(9, "precision_laser");
        expected.put(10, "detector");
        expected.put(11, "ray_scan");
        expected.put(12, "dyson_relay");

        assertEquals(expected.size(), SatelliteTypeRegistry.descriptors().size());
        for(Map.Entry<Integer, String> entry : expected.entrySet()) {
            SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byLegacyId(entry.getKey());
            assertEquals(entry.getValue(), descriptor.getKey());
            assertEquals(entry.getKey().intValue(), descriptor.getLegacyId());
            assertEquals(descriptor.getSatelliteClass(), descriptor.create().getClass());
        }
        assertTrue(SatelliteTypeRegistry.byKey("mapper").canHandLaunch());
        assertTrue(SatelliteTypeRegistry.byKey("scanner").canHandLaunch());
        assertTrue(SatelliteTypeRegistry.byKey("radar").canHandLaunch());
        assertTrue(SatelliteTypeRegistry.byKey("detector").canHandLaunch());
        assertTrue(SatelliteTypeRegistry.byKey("ray_scan").canHandLaunch());
        assertEquals(null, SatelliteTypeRegistry.byKey("sat_war"));
    }

    @Test
    void everySatelliteTypeAndSameFrequencyOnDifferentBodiesSurviveCentralSave() {
        SatelliteTypeRegistry.registerSpaceExtensions();
        NBTTagCompound input = new NBTTagCompound();
        NBTTagList bodies = new NBTTagList();
        bodies.appendTag(bodyWithAllTypes("body:kerbin", 42));
        bodies.appendTag(bodyWithAllTypes("body:duna", 42));
        input.setTag("bodies", bodies);

        SatelliteSavedData restored = new SatelliteSavedData();
        restored.readFromNBT(input);
        NBTTagCompound output = restored.writeToNBT(new NBTTagCompound());

        assertEquals(2, output.getTagList("bodies", NBT.TAG_COMPOUND).tagCount());
        for(int i = 0; i < 2; i++) {
            NBTTagCompound body = output.getTagList("bodies", NBT.TAG_COMPOUND).getCompoundTagAt(i);
            assertEquals(13, body.getTagList("satellites", NBT.TAG_COMPOUND).tagCount());
            assertEquals(0, body.getTagList("quarantine", NBT.TAG_COMPOUND).tagCount());
        }
    }

    @Test
    void clientSnapshotIsDeterministicAndBounded() {
        SatelliteSavedData data = new SatelliteSavedData();
        for(int frequency = SatelliteSavedData.MAX_CLIENT_SNAPSHOT_RECORDS; frequency >= 0; frequency--) {
            data.putSatellite(frequency, new SatelliteScanner());
        }

        NBTTagCompound snapshot = data.createClientSnapshot();
        NBTTagList satellites = snapshot.getTagList("satellites", NBT.TAG_COMPOUND);
        assertEquals(SatelliteSavedData.MAX_CLIENT_SNAPSHOT_RECORDS, satellites.tagCount());
        assertEquals(0, satellites.getCompoundTagAt(0).getInteger("frequency"));
        assertEquals(SatelliteSavedData.MAX_CLIENT_SNAPSHOT_RECORDS - 1,
                satellites.getCompoundTagAt(satellites.tagCount() - 1).getInteger("frequency"));
        assertTrue(snapshot.getBoolean("truncated"));
        assertEquals(SatelliteSavedData.MAX_CLIENT_SNAPSHOT_RECORDS + 1, snapshot.getInteger("totalRecords"));
    }

    @Test
    void orbitKeysAndChipBindingsAreCanonicalAndBackwardCompatible() {
        assertEquals("body:kerbin", OrbitKey.parse(" BODY:Kerbin ").asString());
        assertEquals("dim:-42", OrbitKey.parse("dim:-42").asString());
        assertNull(OrbitKey.parse("planet:kerbin"));

        TestChip item = new TestChip();
        ItemStack oldChip = new ItemStack(item);
        ISatChip.setFreqS(oldChip, 1234);
        assertNull(ISatChip.getOrbitKeyS(oldChip));

        ItemStack linked = new ItemStack(item);
        ISatChip.copyLink(oldChip, linked, OrbitKey.body("Duna"));
        assertEquals(1234, ISatChip.getFreqS(linked));
        assertEquals(OrbitKey.body("duna"), ISatChip.getOrbitKeyS(linked));
    }

    @Test
    void resolverUsesExplicitThenLocalThenUniqueGlobalAndRejectsAmbiguity() {
        NBTTagCompound input = new NBTTagCompound();
        NBTTagList bodies = new NBTTagList();
        bodies.appendTag(bodyWithRecord("body:kerbin", 42, "scanner"));
        bodies.appendTag(bodyWithRecord("body:duna", 42, "radar"));
        bodies.appendTag(bodyWithRecord("body:eve", 77, "mapper"));
        input.setTag("bodies", bodies);

        SatelliteSavedData data = new SatelliteSavedData();
        data.readFromNBT(input);

        SatelliteLookupResult explicit = data.resolveSatellite(OrbitKey.body("duna"), OrbitKey.body("kerbin"), 42);
        assertEquals(SatelliteLookupResult.Status.FOUND, explicit.getStatus());
        assertEquals(OrbitKey.body("duna"), explicit.getHandle().getOrbitKey());
        assertEquals(SatelliteRadar.class, explicit.getSatellite().getClass());

        SatelliteLookupResult local = data.resolveSatellite(null, OrbitKey.body("kerbin"), 42);
        assertEquals(OrbitKey.body("kerbin"), local.getHandle().getOrbitKey());
        assertEquals(SatelliteScanner.class, local.getSatellite().getClass());

        SatelliteLookupResult unique = data.resolveSatellite(null, OrbitKey.body("moho"), 77);
        assertEquals(OrbitKey.body("eve"), unique.getHandle().getOrbitKey());

        assertEquals(SatelliteLookupResult.Status.AMBIGUOUS,
                data.resolveSatellite(null, OrbitKey.body("moho"), 42).getStatus());
    }

    @Test
    void allLegacyIdsZeroThroughEightMigrateAndInvalidFrequenciesAreQuarantined() {
        SatelliteTypeRegistry.registerDefaults();
        NBTTagCompound body = new NBTTagCompound();
        body.setString("key", "body:kerbin");
        NBTTagList records = new NBTTagList();
        for(int legacyId = 0; legacyId <= 8; legacyId++) {
            NBTTagCompound record = new NBTTagCompound();
            record.setInteger("frequency", 100 + legacyId);
            record.setInteger("legacyId", legacyId);
            record.setTag("data", new NBTTagCompound());
            records.appendTag(record);
        }
        NBTTagCompound invalid = record(100_001, "scanner");
        records.appendTag(invalid);
        body.setTag("satellites", records);
        NBTTagList bodies = new NBTTagList();
        bodies.appendTag(body);
        NBTTagCompound input = new NBTTagCompound();
        input.setTag("bodies", bodies);

        SatelliteSavedData data = new SatelliteSavedData();
        data.readFromNBT(input);
        NBTTagCompound output = data.writeToNBT(new NBTTagCompound());
        NBTTagCompound savedBody = output.getTagList("bodies", NBT.TAG_COMPOUND).getCompoundTagAt(0);
        assertEquals(9, savedBody.getTagList("satellites", NBT.TAG_COMPOUND).tagCount());
        assertTrue(hasQuarantineReason(savedBody.getTagList("quarantine", NBT.TAG_COMPOUND), "invalid_frequency"));
    }

    @Test
    void launchResultContractMatchesTransactionalPlan() {
        assertEquals(6, SatelliteLaunchResult.values().length);
        assertTrue(SatelliteLaunchResult.SUCCESS.isSuccess());
        assertEquals(false, SatelliteLaunchResult.FREQ_OCCUPIED.isSuccess());
        assertEquals(false, SatelliteLaunchResult.INVALID_PAYLOAD.isSuccess());
        assertEquals(false, SatelliteLaunchResult.NO_TARGET_BODY.isSuccess());
        assertEquals(false, SatelliteLaunchResult.NO_SURFACE_WORLD.isSuccess());
        assertEquals(false, SatelliteLaunchResult.STORAGE_ERROR.isSuccess());
    }

    private static NBTTagCompound record(int frequency, String type) {
        NBTTagCompound record = new NBTTagCompound();
        record.setInteger("frequency", frequency);
        record.setString("type", type);
        record.setTag("data", new NBTTagCompound());
        record.setTag("orbit", new NBTTagCompound());
        return record;
    }

    private static NBTTagCompound bodyWithAllTypes(String key, int firstFrequency) {
        NBTTagCompound body = new NBTTagCompound();
        body.setString("key", key);
        NBTTagList satellites = new NBTTagList();
        int frequency = firstFrequency;
        for(SatelliteTypeRegistry.Descriptor descriptor : SatelliteTypeRegistry.descriptors()) {
            satellites.appendTag(record(frequency++, descriptor.getKey()));
        }
        body.setTag("satellites", satellites);
        return body;
    }

    private static NBTTagCompound bodyWithRecord(String key, int frequency, String type) {
        NBTTagCompound body = new NBTTagCompound();
        body.setString("key", key);
        NBTTagList satellites = new NBTTagList();
        satellites.appendTag(record(frequency, type));
        body.setTag("satellites", satellites);
        return body;
    }

    private static boolean hasQuarantineReason(NBTTagList quarantine, String reason) {
        for(int i = 0; i < quarantine.tagCount(); i++) {
            if(reason.equals(quarantine.getCompoundTagAt(i).getString("quarantineReason"))) return true;
        }
        return false;
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for(int i = 0; i < count; i++) builder.append(value);
        return builder.toString();
    }

    private static void assertSourceContains(String relativePath, String expected) throws Exception {
        Path source = Path.of(System.getProperty("user.dir"), "src/main/java").resolve(relativePath);
        assertTrue(Files.readString(source, StandardCharsets.UTF_8).contains(expected), source.toString());
    }

    private static final class TestChip extends Item implements ISatChip { }
}
