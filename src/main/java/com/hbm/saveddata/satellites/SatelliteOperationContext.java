package com.hbm.saveddata.satellites;

import com.hbmspace.dim.CelestialBody;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;

/** Server context for a satellite operation without forcing a dimension load. */
public final class SatelliteOperationContext {

    private final World contextWorld;
    private final SatelliteHandle handle;
    @Nullable private final MinecraftServer server;
    @Nullable private final WorldServer surfaceWorld;
    private final boolean targetKnown;

    private SatelliteOperationContext(World contextWorld, SatelliteHandle handle,
                                      @Nullable MinecraftServer server, @Nullable WorldServer surfaceWorld,
                                      boolean targetKnown) {
        this.contextWorld = contextWorld;
        this.handle = handle;
        this.server = server;
        this.surfaceWorld = surfaceWorld;
        this.targetKnown = targetKnown;
    }

    public static SatelliteOperationContext create(World contextWorld, SatelliteHandle handle) {
        if(contextWorld == null || handle == null) throw new IllegalArgumentException("Satellite context is incomplete");
        OrbitKey key = handle.getOrbitKey();
        MinecraftServer server = contextWorld.getMinecraftServer();
        WorldServer surface = null;
        boolean known;

        if(key.isBody()) {
            CelestialBody body = key.findBody();
            known = body != null;
            if(body != null && body.canLand) {
                if(contextWorld instanceof WorldServer && contextWorld.provider.getDimension() == body.dimensionId) {
                    surface = (WorldServer) contextWorld;
                } else {
                    surface = DimensionManager.getWorld(body.dimensionId, false);
                }
            }
        } else {
            int dimensionId = key.getDimensionId();
            known = (contextWorld.provider != null && contextWorld.provider.getDimension() == dimensionId)
                    || DimensionManager.isDimensionRegistered(dimensionId);
            if(contextWorld instanceof WorldServer && contextWorld.provider.getDimension() == dimensionId) {
                surface = (WorldServer) contextWorld;
            } else if(known) {
                surface = DimensionManager.getWorld(dimensionId, false);
            }
        }

        return new SatelliteOperationContext(contextWorld, handle, server, surface, known);
    }

    public World getContextWorld() { return contextWorld; }
    public SatelliteHandle getHandle() { return handle; }
    public OrbitKey getOrbitKey() { return handle.getOrbitKey(); }
    public int getFrequency() { return handle.getFrequency(); }
    @Nullable public MinecraftServer getServer() { return server; }
    @Nullable public WorldServer getSurfaceWorld() { return surfaceWorld; }
    public boolean isTargetKnown() { return targetKnown; }

    public SatelliteSavedData getData() {
        return SatelliteSavedData.getDataForOrbit(contextWorld, getOrbitKey());
    }
}
