package com.hbm.saveddata.satellites;

import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class SatelliteDetector extends Satellite {

    public static final String CMD_SURVEY = "survey";
    public static final String CMD_COUNT = "count";
    public static final String CMD_GETTYPE = "gettype";
    public static final String CMD_GETPOSITION = "getposition";

    public static final int DURATION_LOW = 15 * 20;
    public static final int DURATION_MEDIUM = 20 / 2;
    public static final int DURATION_HIGH = 60 * 20;
    public static final double INACCURACY_LOW = 10_000D;
    public static final double INACCURACY_MEDIUM = 2_500D;
    public static final double INACCURACY_HIGH = 500D;
    private static final int MAX_EVENTS = 4096;

    private static final List<RadiationBurst> BURSTS = new ArrayList<>();
    private final List<RadiationBurst> cachedResults = new ArrayList<>();

    @Override
    protected void onCommandImpl(World world, String... command) {
        if(command == null || command.length == 0) return;
        if(CMD_SURVEY.equals(command[0])) {
            cachedResults.clear();
            synchronized(BURSTS) {
                for(RadiationBurst burst : BURSTS) {
                    if(burst.serverIdentity == serverIdentity(world)
                            && burst.dimension == world.provider.getDimension()) cachedResults.add(burst);
                }
            }
        } else if(CMD_COUNT.equals(command[0])) {
            tx = Integer.toString(cachedResults.size());
        } else if(CMD_GETTYPE.equals(command[0]) && command.length == 2) {
            RadiationBurst burst = getBurst(command[1]);
            tx = burst == null ? "" : burst.intensity.name().toUpperCase(Locale.US);
        } else if(CMD_GETPOSITION.equals(command[0]) && command.length == 2) {
            RadiationBurst burst = getBurst(command[1]);
            tx = burst == null ? "" : burst.x + ";" + burst.z;
        }
    }

    private RadiationBurst getBurst(String value) {
        int index = parseOneBasedIndex(value, cachedResults.size());
        return index < 0 ? null : cachedResults.get(index);
    }

    public static void reportEvent(World world, int lifetime, BurstIntensity intensity, double x, double z) {
        if(world == null || world.isRemote) return;
        synchronized(BURSTS) {
            BURSTS.add(new RadiationBurst(world, lifetime, intensity, (int) Math.floor(x), (int) Math.floor(z)));
            while(BURSTS.size() > MAX_EVENTS) BURSTS.remove(0);
        }
    }

    public static void updateSystem(World world) {
        if(world == null || world.isRemote) return;
        synchronized(BURSTS) {
            Iterator<RadiationBurst> iterator = BURSTS.iterator();
            while(iterator.hasNext()) {
                RadiationBurst burst = iterator.next();
                if(burst.serverIdentity == serverIdentity(world)
                        && burst.dimension == world.provider.getDimension()
                        && world.getTotalWorldTime() > burst.expiresOn) {
                    iterator.remove();
                }
            }
        }
    }

    public static void clearWorld(World world) {
        if(world == null) return;
        int server = serverIdentity(world);
        int dimension = world.provider.getDimension();
        synchronized(BURSTS) {
            BURSTS.removeIf(burst -> burst.serverIdentity == server && burst.dimension == dimension);
        }
    }

    public static void clearAll() {
        synchronized(BURSTS) { BURSTS.clear(); }
    }

    private static int serverIdentity(World world) {
        return System.identityHashCode(world.getMinecraftServer());
    }

    @Override
    public float[] getColor() {
        return new float[] { 1F, 0.75F, 0.1F };
    }

    public enum BurstIntensity { LOW, MEDIUM, HIGH }

    public static final class RadiationBurst {
        public final int dimension;
        public final int serverIdentity;
        public final long expiresOn;
        public final BurstIntensity intensity;
        public final int x;
        public final int z;

        private RadiationBurst(World world, int lifetime, BurstIntensity intensity, int x, int z) {
            dimension = world.provider.getDimension();
            serverIdentity = SatelliteDetector.serverIdentity(world);
            expiresOn = world.getTotalWorldTime() + Math.max(1, lifetime);
            this.intensity = intensity;
            double inaccuracy = intensity == BurstIntensity.LOW
                    ? INACCURACY_LOW : intensity == BurstIntensity.MEDIUM ? INACCURACY_MEDIUM : INACCURACY_HIGH;
            this.x = x + (int) Math.round(world.rand.nextGaussian() * inaccuracy);
            this.z = z + (int) Math.round(world.rand.nextGaussian() * inaccuracy);
        }
    }

    private static int parseOneBasedIndex(String value, int size) {
        try {
            int index = Integer.parseInt(value) - 1;
            return index >= 0 && index < size ? index : -1;
        } catch(NumberFormatException ignored) {
            return -1;
        }
    }
}
