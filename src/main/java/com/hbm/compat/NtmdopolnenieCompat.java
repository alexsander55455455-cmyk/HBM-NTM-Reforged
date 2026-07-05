package com.hbm.compat;

import com.hbm.config.GeneralConfig;
import com.hbm.main.MainRegistry;
import com.hbm.util.Compat;

/**
 * Optional compatibility for the ntmdopolnenie structure addon (NTMstructure).
 * All logic here is inactive unless the addon JAR is present and enabled in config.
 */
public final class NtmdopolnenieCompat {

    private static boolean active;
    private static final ThreadLocal<Boolean> remappingAddonTemplate = ThreadLocal.withInitial(() -> false);

    private NtmdopolnenieCompat() {
    }

    public static void init() {
        active = false;
        if (!Compat.isNtmdopolnenieLoaded()) {
            return;
        }
        if (!GeneralConfig.enableNtmdopolnenieCompat) {
            MainRegistry.logger.warn("ntmdopolnenie is installed but compat is disabled in hbmConfig (enableNtmdopolnenieCompat=false). Structure remapping is off.");
            return;
        }
        active = true;
        MainRegistry.logger.info("ntmdopolnenie structure addon detected. Compat layer enabled (legacy structure remapping active).");
    }

    public static boolean isActive() {
        return active;
    }

    public static void beginAddonTemplateRemap() {
        remappingAddonTemplate.set(true);
    }

    public static void endAddonTemplateRemap() {
        remappingAddonTemplate.set(false);
    }

    public static boolean isRemappingAddonTemplate() {
        return Boolean.TRUE.equals(remappingAddonTemplate.get());
    }
}