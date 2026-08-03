package com.hbm.saveddata.satellites;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatelliteResourceContractTest {

    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path ASSETS = ROOT.resolve("src/main/resources/assets/hbm");

    @Test
    void pileAndSatelliteManualsAreValidJson() throws Exception {
        assertJsonDirectory(ASSETS.resolve("manual/pile"), 5);
        assertJsonDirectory(ASSETS.resolve("manual/satellite"), 12);
    }

    @Test
    void requiredTexturesModelsAndObjGroupsExist() throws Exception {
        List<String> textures = Arrays.asList(
                "satellite.spy.png", "satellite.scanner.png", "satellite.radar.png",
                "satellite.miner_astro.png", "satellite.miner_lunar.png",
                "satellite.precision_laser.png", "satellite.death_ray.png",
                "satellite.xenium_resonator.png", "satellite.relay.png",
                "satellite.detector.png", "satellite.ray_scan.png", "sat_dyson_relay.png"
        );
        for(String texture : textures) assertPng(ASSETS.resolve("textures/items").resolve(texture));
        assertPng(ASSETS.resolve("textures/models/pile/pile_loader.png"));
        assertPng(ASSETS.resolve("textures/models/pile/pile_vent.png"));
        assertPng(ASSETS.resolve("textures/models/pile/pile_control.png"));
        assertPng(ASSETS.resolve("textures/models/machines/satlink.png"));

        assertObjGroups(ASSETS.resolve("models/pile/pile_loader.obj"), "Rod", "Lever", "Slider", "Loader");
        assertObjGroups(ASSETS.resolve("models/pile/pile_vent.obj"), "Fan", "Pipe");
        assertObjGroups(ASSETS.resolve("models/pile/pile_control.obj"), "Rod", "Base");
        assertObjGroups(ASSETS.resolve("models/machines/satlink.obj"), "Rotor", "Dish", "Base");

        for(String language : Arrays.asList("en_us.lang", "ru_ru.lang")) {
            String text = Files.readString(ASSETS.resolve("lang").resolve(language), StandardCharsets.UTF_8);
            assertTrue(text.contains("tile.pile_brick.name="), language);
            assertTrue(text.contains("tile.machine_satlink.name="), language);
            assertTrue(text.contains("item.satellite.spy.name="), language);
            assertTrue(text.contains("item.sat_dyson_relay.name="), language);
            assertTrue(text.contains("gui.satellite_orbit.preview="), language);
            assertTrue(text.contains("gui.satellite_orbit.scroll_hint="), language);
        }
    }

    private static void assertJsonDirectory(Path directory, int expectedCount) throws Exception {
        List<Path> files;
        try(Stream<Path> stream = Files.list(directory)) {
            files = stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted().collect(Collectors.toList());
        }
        assertEquals(expectedCount, files.size(), directory.toString());
        for(Path file : files) {
            try(Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                assertTrue(new JsonParser().parse(reader).isJsonObject(), file.toString());
            }
        }
    }

    private static void assertPng(Path file) throws Exception {
        byte[] bytes = Files.readAllBytes(file);
        byte[] signature = {(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        assertTrue(bytes.length > signature.length, file.toString());
        assertTrue(Arrays.equals(signature, Arrays.copyOf(bytes, signature.length)), file.toString());
    }

    private static void assertObjGroups(Path file, String... groups) throws Exception {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        for(String group : groups) {
            assertTrue(text.contains("o " + group + "\n") || text.contains("o " + group + "\r\n"),
                    file + " missing group " + group);
        }
    }
}
