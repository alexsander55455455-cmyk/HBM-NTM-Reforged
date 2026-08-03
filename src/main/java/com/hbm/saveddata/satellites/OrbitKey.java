package com.hbm.saveddata.satellites;

import com.hbmspace.dim.CelestialBody;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

/** Stable namespace for one celestial orbit or a third-party dimension. */
public final class OrbitKey implements Comparable<OrbitKey> {

    private static final String BODY_PREFIX = "body:";
    private static final String DIMENSION_PREFIX = "dim:";

    private final String value;

    private OrbitKey(String value) {
        this.value = value;
    }

    @Nullable
    public static OrbitKey parse(@Nullable String value) {
        if(value == null) return null;
        String normalized = value.trim();
        if(normalized.regionMatches(true, 0, BODY_PREFIX, 0, BODY_PREFIX.length())) {
            String name = normalized.substring(BODY_PREFIX.length()).trim().toLowerCase(Locale.ROOT);
            return name.isEmpty() ? null : new OrbitKey(BODY_PREFIX + name);
        }
        if(normalized.regionMatches(true, 0, DIMENSION_PREFIX, 0, DIMENSION_PREFIX.length())) {
            try {
                return dimension(Integer.parseInt(normalized.substring(DIMENSION_PREFIX.length()).trim()));
            } catch(NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static OrbitKey body(String name) {
        OrbitKey key = parse(BODY_PREFIX + name);
        if(key == null) throw new IllegalArgumentException("Empty celestial body name");
        return key;
    }

    public static OrbitKey body(CelestialBody body) {
        if(body == null) throw new IllegalArgumentException("Celestial body is null");
        return body(body.name);
    }

    public static OrbitKey dimension(int dimensionId) {
        return new OrbitKey(DIMENSION_PREFIX + dimensionId);
    }

    public static OrbitKey fromWorld(World world, int x, int z) {
        if(world == null || world.provider == null) throw new IllegalArgumentException("World is unavailable");
        int dimensionId = world.provider.getDimension();
        if(!CelestialBody.inOrbit(world)) {
            CelestialBody localBody = CelestialBody.getBodyOrNull(dimensionId);
            return localBody == null ? dimension(dimensionId) : body(localBody);
        }
        try {
            com.hbmspace.items.ItemVOTVdrive.Target target = CelestialBody.getTarget(world, x, z);
            if(target != null && target.body != null) return body(target.body);
        } catch(Throwable ignored) {
            // Orbit stations may still be initializing. Fall back to the provider dimension.
        }
        CelestialBody body = CelestialBody.getBodyOrNull(dimensionId);
        return body == null ? dimension(dimensionId) : body(body);
    }

    public boolean isBody() {
        return value.startsWith(BODY_PREFIX);
    }

    public boolean isDimension() {
        return value.startsWith(DIMENSION_PREFIX);
    }

    public String getBodyName() {
        return isBody() ? value.substring(BODY_PREFIX.length()) : "";
    }

    public int getDimensionId() {
        if(!isDimension()) throw new IllegalStateException("Not a dimension orbit key: " + value);
        return Integer.parseInt(value.substring(DIMENSION_PREFIX.length()));
    }

    @Nullable
    public CelestialBody findBody() {
        if(!isBody()) return null;
        for(CelestialBody body : CelestialBody.getAllBodies()) {
            if(getBodyName().equalsIgnoreCase(body.name)) return body;
        }
        return null;
    }

    public String asString() {
        return value;
    }

    @Override
    public int compareTo(OrbitKey other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof OrbitKey && value.equals(((OrbitKey) object).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
