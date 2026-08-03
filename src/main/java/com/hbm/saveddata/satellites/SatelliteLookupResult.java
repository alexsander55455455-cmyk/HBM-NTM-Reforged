package com.hbm.saveddata.satellites;

import javax.annotation.Nullable;

/** Result of resolving a legacy frequency or a body-bound satellite chip. */
public final class SatelliteLookupResult {

    public enum Status { FOUND, NOT_FOUND, AMBIGUOUS }

    private final Status status;
    @Nullable private final SatelliteHandle handle;
    @Nullable private final Satellite satellite;
    @Nullable private final SatelliteSavedData data;

    private SatelliteLookupResult(Status status, @Nullable SatelliteHandle handle,
                                  @Nullable Satellite satellite, @Nullable SatelliteSavedData data) {
        this.status = status;
        this.handle = handle;
        this.satellite = satellite;
        this.data = data;
    }

    static SatelliteLookupResult found(SatelliteHandle handle, Satellite satellite, SatelliteSavedData data) {
        return new SatelliteLookupResult(Status.FOUND, handle, satellite, data);
    }

    static SatelliteLookupResult notFound() {
        return new SatelliteLookupResult(Status.NOT_FOUND, null, null, null);
    }

    static SatelliteLookupResult ambiguous() {
        return new SatelliteLookupResult(Status.AMBIGUOUS, null, null, null);
    }

    public Status getStatus() { return status; }
    public boolean isFound() { return status == Status.FOUND; }
    @Nullable public SatelliteHandle getHandle() { return handle; }
    @Nullable public Satellite getSatellite() { return satellite; }
    @Nullable public SatelliteSavedData getData() { return data; }
}
