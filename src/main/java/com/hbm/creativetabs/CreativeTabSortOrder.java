package com.hbm.creativetabs;

import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creative-tab item order from assets/hbm/creative_tab_order.txt.
 * Keys are registry paths (e.g. deco_sat_mapper), not Java field names.
 */
public final class CreativeTabSortOrder {

    public static final int UNKNOWN_SORT_INDEX = 1_500_000;

    private static final Map<String, String> REGISTRY_ALIASES = new HashMap<>();

    private static final Map<String, Map<String, Integer>> TAB_ORDERS = new HashMap<>();
    private static final Map<String, List<String>> TAB_REGISTRY_ORDER = new HashMap<>();
    private static final Map<String, Integer> GLOBAL_FALLBACK = new HashMap<>();
    private static final Map<String, Integer> REGISTRY_BUCKET = new HashMap<>();
    private static boolean loaded = false;
    private static boolean registryBucketLoaded = false;

    private CreativeTabSortOrder() {}

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        try (InputStream stream = openOrderStream()) {
            if (stream == null) {
                MainRegistry.logger.log(Level.ERROR, "[HBM] Missing assets/hbm/creative_tab_order.txt — creative/JEI sort disabled");
                loaded = true;
                return;
            }
            String currentTab = null;
            Map<String, Integer> currentOrder = null;
            int global = 0;
            for (String rawLine : IOUtils.readLines(stream, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("@")) {
                    currentTab = line.substring(1);
                    currentOrder = new HashMap<>();
                    TAB_ORDERS.put(currentTab, currentOrder);
                    continue;
                }
                int split = line.indexOf('=');
                if (split <= 0 || currentTab == null || currentOrder == null) {
                    continue;
                }
                String name = line.substring(0, split);
                int idx = Integer.parseInt(line.substring(split + 1));
                currentOrder.put(name, idx);
                GLOBAL_FALLBACK.putIfAbsent(name, global++);
            }
            for (Map.Entry<String, Map<String, Integer>> tabEntry : TAB_ORDERS.entrySet()) {
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(tabEntry.getValue().entrySet());
                sorted.sort(Comparator.comparingInt(Map.Entry::getValue));
                List<String> keys = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : sorted) {
                    keys.add(entry.getKey());
                }
                TAB_REGISTRY_ORDER.put(tabEntry.getKey(), Collections.unmodifiableList(keys));
            }
        } catch (Exception e) {
            MainRegistry.logger.log(Level.ERROR, "[HBM] Failed to load creative_tab_order.txt", e);
        }
        loaded = true;
        ensureRegistryBucket();
    }

    private static synchronized void ensureRegistryBucket() {
        if (registryBucketLoaded) {
            return;
        }
        int ordinal = 0;
        try {
            for (Item item : ModItems.ALL_ITEMS) {
                ordinal = registerItemBucket(item, ordinal);
            }
            Class<?> spaceItems = Class.forName("com.hbmspace.items.ModItemsSpace");
            @SuppressWarnings("unchecked")
            List<Item> spaceList = (List<Item>) spaceItems.getField("ALL_ITEMS").get(null);
            for (Item item : spaceList) {
                ordinal = registerItemBucket(item, ordinal);
            }
        } catch (Throwable ignored) {
            ordinal = loadRegistryBucketFromSources(ordinal);
        }
        registryBucketLoaded = true;
    }

    private static int registerItemBucket(Item item, int ordinal) {
        if (item == null) {
            return ordinal;
        }
        ResourceLocation reg = item.getRegistryName();
        if (reg == null) {
            return ordinal;
        }
        if (!REGISTRY_BUCKET.containsKey(reg.toString())) {
            REGISTRY_BUCKET.put(reg.toString(), ordinal);
            REGISTRY_BUCKET.put(reg.getNamespace() + ":" + reg.getPath(), ordinal);
            REGISTRY_BUCKET.putIfAbsent(reg.getPath(), ordinal);
            ordinal++;
        }
        return ordinal;
    }

    private static int loadRegistryBucketFromSources(int ordinal) {
        ordinal = loadRegistryBucketFromSource("src/main/java/com/hbm/items/ModItems.java", "hbm", ordinal);
        return loadRegistryBucketFromSource("src/main/java/com/hbmspace/items/ModItemsSpace.java", "hbmspace", ordinal);
    }

    private static int loadRegistryBucketFromSource(String relativePath, String defaultNamespace, int ordinal) {
        Path path = resolveSourcePath(relativePath);
        if (path == null || !Files.exists(path)) {
            return ordinal;
        }
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            Matcher fieldMatcher = Pattern.compile("public static final (?:Item\\w*|Item)\\s+(\\w+)\\s*=")
                    .matcher(text);
            while (fieldMatcher.find()) {
                String field = fieldMatcher.group(1);
                String key = defaultNamespace + ":" + field;
                if (!REGISTRY_BUCKET.containsKey(key)) {
                    REGISTRY_BUCKET.put(key, ordinal);
                    REGISTRY_BUCKET.putIfAbsent(field, ordinal);
                    ordinal++;
                }
            }
        } catch (Exception ignored) {
        }
        return ordinal;
    }

    private static Path resolveSourcePath(String relativePath) {
        Path direct = Paths.get(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }
        Path fromUserDir = Paths.get(System.getProperty("user.dir"), relativePath);
        if (Files.exists(fromUserDir)) {
            return fromUserDir;
        }
        return null;
    }

    private static InputStream openOrderStream() {
        String absolute = "/assets/hbm/creative_tab_order.txt";
        String relative = "assets/hbm/creative_tab_order.txt";
        for (Class<?> anchor : new Class<?>[] { MainRegistry.class, CreativeTabSortOrder.class }) {
            InputStream stream = anchor.getResourceAsStream(absolute);
            if (stream != null) {
                return stream;
            }
        }
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            InputStream stream = context.getResourceAsStream(relative);
            if (stream != null) {
                return stream;
            }
        }
        ClassLoader system = ClassLoader.getSystemClassLoader();
        if (system != null) {
            return system.getResourceAsStream(relative);
        }
        return null;
    }

    public static ResourceLocation resolveRegistryKey(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        ResourceLocation key;
        if (path.contains(":")) {
            String[] parts = path.split(":", 2);
            key = new ResourceLocation(parts[0], parts[1]);
        } else {
            key = new ResourceLocation("hbm", path);
        }
        return resolveAlias(key);
    }

    private static ResourceLocation resolveAlias(ResourceLocation key) {
        if (key == null) {
            return null;
        }
        String alias = REGISTRY_ALIASES.get(key.toString());
        if (alias == null) {
            alias = REGISTRY_ALIASES.get(key.getPath());
        }
        if (alias == null) {
            return key;
        }
        if (alias.contains(":")) {
            String[] parts = alias.split(":", 2);
            return new ResourceLocation(parts[0], parts[1]);
        }
        return new ResourceLocation(key.getNamespace(), alias);
    }

    private static Integer lookup(Map<String, Integer> map, ResourceLocation key) {
        if (map == null || key == null) {
            return null;
        }
        ResourceLocation resolved = resolveAlias(key);
        Integer idx = map.get(resolved.toString());
        if (idx != null) {
            return idx;
        }
        idx = map.get(key.toString());
        if (idx != null) {
            return idx;
        }
        idx = map.get(resolved.getPath());
        if (idx != null) {
            return idx;
        }
        return map.get(key.getPath());
    }

    /** Registration-order bucket for items without an explicit tab index. */
    public static int getRegistryBucket(ResourceLocation key) {
        ensureLoaded();
        if (key == null) {
            return Integer.MAX_VALUE;
        }
        ResourceLocation resolved = resolveAlias(key);
        Integer bucket = REGISTRY_BUCKET.get(resolved.toString());
        if (bucket == null) {
            bucket = REGISTRY_BUCKET.get(key.toString());
        }
        if (bucket == null) {
            bucket = REGISTRY_BUCKET.get(resolved.getPath());
        }
        if (bucket == null) {
            bucket = REGISTRY_BUCKET.get(key.getPath());
        }
        return bucket != null ? bucket : Integer.MAX_VALUE;
    }

    /** Explicit {@code creative_tab_order.txt} entry for this tab, or null if absent. */
    public static Integer getExplicitSortIndex(ResourceLocation key, String tabKey) {
        ensureLoaded();
        if (key == null) {
            return null;
        }
        return lookup(TAB_ORDERS.get(tabKey), key);
    }

    public static int getSortIndex(ResourceLocation key, String tabKey) {
        ensureLoaded();
        if (key == null) {
            return Integer.MAX_VALUE;
        }
        ResourceLocation resolved = resolveAlias(key);
        Integer idx = getExplicitSortIndex(resolved, tabKey);
        if (idx == null) {
            idx = getExplicitSortIndex(key, tabKey);
        }
        if (idx != null) {
            return idx;
        }
        Integer fallback = GLOBAL_FALLBACK.get(resolved.toString());
        if (fallback == null) {
            fallback = GLOBAL_FALLBACK.get(key.toString());
        }
        if (fallback == null) {
            fallback = GLOBAL_FALLBACK.get(resolved.getPath());
        }
        if (fallback == null) {
            fallback = GLOBAL_FALLBACK.get(key.getPath());
        }
        if (fallback != null) {
            return 1_000_000 + fallback;
        }
        return UNKNOWN_SORT_INDEX;
    }

    public static int getSortIndex(ItemStack stack, String tabKey) {
        ensureLoaded();
        if (stack.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        ResourceLocation key = stack.getItem().getRegistryName();
        if (key == null) {
            return Integer.MAX_VALUE;
        }
        return getSortIndex(key, tabKey);
    }

    public static List<String> getTabRegistryOrder(String tabKey) {
        ensureLoaded();
        List<String> order = TAB_REGISTRY_ORDER.get(tabKey);
        if (order == null) {
            return Collections.emptyList();
        }
        return order;
    }

    public static void logLoadHealth() {
        int weapon = getTabRegistryOrder("weaponTab").size();
        int parts = getTabRegistryOrder("partsTab").size();
        if (weapon < 100 || parts < 100) {
            MainRegistry.logger.log(Level.ERROR,
                    "[HBM] creative_tab_order.txt not loaded (weaponTab={}, partsTab={})",
                    weapon, parts);
        } else {
            MainRegistry.logger.log(Level.INFO,
                    "[HBM] creative_tab_order.txt ready (weaponTab={}, partsTab={})",
                    weapon, parts);
        }
    }
}