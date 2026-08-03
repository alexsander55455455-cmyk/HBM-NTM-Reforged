package com.hbm.saveddata.satellites;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SatelliteRegistryContractTest {

    @Test
    void officialPayloadMassesAndHandLaunchFlagsAreStable() {
        assertDescriptor("mapper", 16_000, true);
        assertDescriptor("scanner", 16_000, true);
        assertDescriptor("radar", 16_000, true);
        assertDescriptor("death_ray", 48_000, false);
        assertDescriptor("resonator", 20_000, false);
        assertDescriptor("relay", 20_000, false);
        assertDescriptor("miner_astro", 64_000, false);
        assertDescriptor("miner_lunar", 64_000, false);
        assertDescriptor("horizons", 128_000, false);
        assertDescriptor("precision_laser", 48_000, false);
        assertDescriptor("detector", 16_000, true);
        assertDescriptor("ray_scan", 16_000, true);
    }

    @Test
    void keysIdsCanonicalMetasAndAliasesRemainUniqueAndStable() throws Exception {
        SatelliteTypeRegistry.registerSpaceExtensions();
        Set<Integer> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for(SatelliteTypeRegistry.Descriptor descriptor : SatelliteTypeRegistry.descriptors()) {
            assertTrue(ids.add(descriptor.getLegacyId()), descriptor.getKey());
            assertTrue(keys.add(descriptor.getKey()), descriptor.getKey());
        }

        Path sourcePath = Path.of(System.getProperty("user.dir"))
                .resolve("src/main/java/com/hbm/saveddata/satellites/SatelliteTypeRegistry.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        assertTrue(source.contains("register(0, \"mapper\", SatelliteMapper::new, () -> canonical(EnumSatelliteType.SPY)"));
        assertTrue(source.contains("register(5, \"relay\", SatelliteRelay::new, () -> canonical(EnumSatelliteType.RELAY)"));
        assertTrue(source.contains("() -> new ItemStack(ModItems.sat_foeq)"));
        assertTrue(source.contains("register(11, \"ray_scan\", SatelliteRayScan::new, () -> canonical(EnumSatelliteType.RAY_SCAN)"));
        assertFalse(source.contains("register(13, \"sat_war\""));
    }

    private static void assertDescriptor(String key, int mass, boolean handLaunch) {
        SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byKey(key);
        assertNotNull(descriptor, key);
        assertEquals(mass, descriptor.getMass(), key);
        assertEquals(handLaunch, descriptor.canHandLaunch(), key);
    }
}
