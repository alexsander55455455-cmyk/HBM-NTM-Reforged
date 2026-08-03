package com.hbm.saveddata.satellites;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SatelliteRayScan extends Satellite {

    public static final int MAX_SCAN_RANGE = 250;
    public static final String CMD_SURVEY = "survey";
    public static final String CMD_COUNT = "count";
    public static final String CMD_GETINFO = "getinfo";
    public static final String CMD_GETPOSITION = "getposition";
    private static final int MAX_EVENTS = 4096;

    private static final LinkedHashMap<DimPos, RayEvent> EVENTS = new LinkedHashMap<>();
    private final List<RayEvent> cachedResults = new ArrayList<>();

    @Override
    protected void onCommandImpl(World world, String... command) {
        if(command == null || command.length == 0) return;
        if(CMD_SURVEY.equals(command[0])) {
            cachedResults.clear();
            synchronized(EVENTS) {
                for(Map.Entry<DimPos, RayEvent> entry : EVENTS.entrySet()) {
                    DimPos pos = entry.getKey();
                    if(pos.serverIdentity != serverIdentity(world)
                            || pos.dimension != world.provider.getDimension()) continue;
                    long dx = (long)pos.x - targetX;
                    long dz = (long)pos.z - targetZ;
                    if(dx * dx + dz * dz <= MAX_SCAN_RANGE * MAX_SCAN_RANGE) {
                        cachedResults.add(entry.getValue());
                    }
                }
            }
        } else if(CMD_COUNT.equals(command[0])) {
            tx = Integer.toString(cachedResults.size());
        } else if(CMD_GETINFO.equals(command[0]) && command.length == 2) {
            RayEvent event = getEvent(command[1]);
            tx = event == null ? "" : event.info;
        } else if(CMD_GETPOSITION.equals(command[0]) && command.length == 2) {
            RayEvent event = getEvent(command[1]);
            tx = event == null ? "" : event.x + ";" + event.z;
        }
    }

    private RayEvent getEvent(String value) {
        try {
            int index = Integer.parseInt(value) - 1;
            return index >= 0 && index < cachedResults.size() ? cachedResults.get(index) : null;
        } catch(NumberFormatException ignored) {
            return null;
        }
    }

    public static void reportEvent(World world, int x, int y, int z, String info, int lifetime) {
        if(world == null || world.isRemote) return;
        synchronized(EVENTS) {
            EVENTS.put(new DimPos(serverIdentity(world), world.provider.getDimension(), new BlockPos(x, y, z)),
                    new RayEvent(world.getTotalWorldTime() + Math.max(1, lifetime), x, z, info));
            while(EVENTS.size() > MAX_EVENTS) {
                Iterator<DimPos> oldest = EVENTS.keySet().iterator();
                if(!oldest.hasNext()) break;
                oldest.next();
                oldest.remove();
            }
        }
    }

    public static void updateSystem(World world) {
        if(world == null || world.isRemote) return;
        synchronized(EVENTS) {
            Iterator<Map.Entry<DimPos, RayEvent>> iterator = EVENTS.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<DimPos, RayEvent> entry = iterator.next();
                if(entry.getKey().serverIdentity == serverIdentity(world)
                        && entry.getKey().dimension == world.provider.getDimension()
                        && world.getTotalWorldTime() > entry.getValue().expiresOn) {
                    iterator.remove();
                }
            }
        }
    }

    public static void clearWorld(World world) {
        if(world == null) return;
        int server = serverIdentity(world);
        int dimension = world.provider.getDimension();
        synchronized(EVENTS) {
            EVENTS.entrySet().removeIf(entry -> entry.getKey().serverIdentity == server
                    && entry.getKey().dimension == dimension);
        }
    }

    public static void clearAll() {
        synchronized(EVENTS) { EVENTS.clear(); }
    }

    private static int serverIdentity(World world) {
        return System.identityHashCode(world.getMinecraftServer());
    }

    @Override
    public float[] getColor() {
        return new float[] { 0.75F, 0.2F, 1F };
    }

    public static final class RayEvent {
        public static final String INFO_ARC_FLASH = "ARC_FLASH";
        public static final String INFO_NUCLEAR = "NEUTRON_EMISSION";
        public static final String INFO_PARTICLE = "HIGH_ENERGY_PARTICLES";
        public static final String INFO_RADAR = "RADAR_WAVES";
        public static final String INFO_RADIO = "RADIO_WAVES";

        public final long expiresOn;
        public final int x;
        public final int z;
        public final String info;

        private RayEvent(long expiresOn, int x, int z, String info) {
            this.expiresOn = expiresOn;
            this.x = x;
            this.z = z;
            this.info = info == null ? "" : info;
        }
    }

    private static final class DimPos {
        private final int serverIdentity;
        private final int dimension;
        private final int x;
        private final int y;
        private final int z;

        private DimPos(int serverIdentity, int dimension, BlockPos pos) {
            this.serverIdentity = serverIdentity;
            this.dimension = dimension;
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }

        @Override
        public boolean equals(Object object) {
            if(this == object) return true;
            if(!(object instanceof DimPos)) return false;
            DimPos other = (DimPos) object;
            return serverIdentity == other.serverIdentity && dimension == other.dimension
                    && x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(serverIdentity, dimension, x, y, z);
        }
    }
}
