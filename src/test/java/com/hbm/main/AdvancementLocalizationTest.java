package com.hbm.main;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancementLocalizationTest {

    private static final Path ASSETS = Paths.get("src/main/resources/assets/hbm");

    @Test
    void englishAdvancementKeysResolve() throws Exception {
        assertTranslationKeys("en_us");
    }

    @Test
    void russianAdvancementKeysResolve() throws Exception {
        assertTranslationKeys("ru_ru");
    }

    private static void assertTranslationKeys(String language) throws Exception {
        Set<String> keys = new HashSet<>();
        for (String line : Files.readAllLines(ASSETS.resolve("lang/" + language + ".lang"), StandardCharsets.UTF_8)) {
            int separator = line.indexOf('=');
            if (separator > 0 && !line.startsWith("#")) keys.add(line.substring(0, separator));
        }

        List<Path> files;
        try (Stream<Path> stream = Files.walk(ASSETS.resolve("advancements"))) {
            files = stream.filter(path -> path.toString().endsWith(".json"))
                    .sorted().collect(Collectors.toList());
        }

        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject display = new JsonParser().parse(reader).getAsJsonObject().getAsJsonObject("display");
                if (display == null) continue;
                for (String field : new String[] {"title", "description"}) {
                    String key = display.getAsJsonObject(field).get("translate").getAsString();
                    checked++;
                    if (!keys.contains(key)) missing.add(file.getFileName() + ": " + key);
                }
            }
        }

        assertTrue(checked > 0, "No advancement translation keys checked");
        assertTrue(missing.isEmpty(), language + ": " + String.join(", ", missing));
    }
}
