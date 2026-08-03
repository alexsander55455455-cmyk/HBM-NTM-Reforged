package com.hbmspace.dim;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolarSystemSnapshotTest {

    private static final Path SOURCE = Path.of(System.getProperty("user.dir"))
            .resolve("src/main/java/com/hbmspace/dim/SolarSystem.java");
    private static final String EXPECTED_SHA256 = "ED2127F075C16041E8435E72F619AC991F44D1E6D138094AE4338AA880BEBAAD";

    @Test
    void celestialBodyTreeAndAllPhysicalParametersRemainFrozen() throws Exception {
        byte[] bytes = Files.readAllBytes(SOURCE);
        assertEquals(EXPECTED_SHA256, sha256(bytes));

        String source = new String(bytes, StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("new CelestialBody\\(\"([a-z]+)\"").matcher(source);
        List<String> names = new ArrayList<>();
        while(matcher.find()) names.add(matcher.group(1));
        assertEquals(Arrays.asList(
                "kerbol", "moho", "eve", "gilly", "kerbin", "mun", "minmus", "duna",
                "ike", "dres", "jool", "laythe", "vall", "tylo", "bop", "pol", "sarnus",
                "hale", "ovok", "eeloo", "slate", "tekto", "urlum", "polta", "priax", "wal",
                "tal", "neidon", "thatmo", "nissee", "plock", "karen"
        ), names);
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder value = new StringBuilder(digest.length * 2);
        for(byte current : digest) value.append(String.format("%02X", current));
        return value.toString();
    }
}
