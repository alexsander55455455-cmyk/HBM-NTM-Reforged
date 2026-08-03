package com.hbm.saveddata.satellites;

/**
 * Explicit result used by launchers so payloads are consumed only after a
 * successful insert or delivery.
 */
public enum SatelliteLaunchResult {
    SUCCESS(true),
    FREQ_OCCUPIED(false),
    INVALID_PAYLOAD(false),
    NO_TARGET_BODY(false),
    NO_SURFACE_WORLD(false),
    STORAGE_ERROR(false);

    private final boolean success;

    SatelliteLaunchResult(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
