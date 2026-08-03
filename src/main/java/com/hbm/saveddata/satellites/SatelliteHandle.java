package com.hbm.saveddata.satellites;

import java.util.Objects;

/** Unambiguous identity of one satellite in the central store. */
public final class SatelliteHandle {

    private final OrbitKey orbitKey;
    private final int frequency;

    public SatelliteHandle(OrbitKey orbitKey, int frequency) {
        if(orbitKey == null) throw new IllegalArgumentException("Orbit key is null");
        if(frequency < 0 || frequency > 100_000) throw new IllegalArgumentException("Frequency out of range: " + frequency);
        this.orbitKey = orbitKey;
        this.frequency = frequency;
    }

    public OrbitKey getOrbitKey() {
        return orbitKey;
    }

    public int getFrequency() {
        return frequency;
    }

    @Override
    public boolean equals(Object object) {
        if(this == object) return true;
        if(!(object instanceof SatelliteHandle)) return false;
        SatelliteHandle other = (SatelliteHandle) object;
        return frequency == other.frequency && orbitKey.equals(other.orbitKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orbitKey, frequency);
    }

    @Override
    public String toString() {
        return orbitKey + "#" + frequency;
    }
}
