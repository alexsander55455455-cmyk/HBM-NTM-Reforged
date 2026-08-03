package com.hbm.saveddata.satellites;

import com.hbm.items.ISatChip;
import com.hbmspace.tileentity.TESpaceUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Resolves old frequency-only chips without silently choosing the wrong body. */
public final class SatelliteResolver {

    public enum Status { FOUND, NOT_FOUND, AMBIGUOUS, NO_TARGET_BODY, NO_SURFACE_WORLD }

    private SatelliteResolver() { }

    public static Result resolve(World world, int x, int z, ItemStack chip, boolean requireSurface) {
        int frequency = ISatChip.getFreqS(chip);
        OrbitKey explicit = ISatChip.getOrbitKeyS(chip);
        return resolve(world, x, z, frequency, explicit, requireSurface);
    }

    public static Result resolve(World world, int x, int z, int frequency,
                                 @Nullable OrbitKey explicitOrbit, boolean requireSurface) {
        if(world == null || world.isRemote) return Result.notFound();
        OrbitKey localOrbit = OrbitKey.fromWorld(world, x, z);
        SatelliteSavedData localData = TESpaceUtil.getData(world, x, z);
        SatelliteLookupResult lookup = localData.resolveSatellite(explicitOrbit, localOrbit, frequency);
        if(lookup.getStatus() == SatelliteLookupResult.Status.AMBIGUOUS) return Result.ambiguous();
        if(!lookup.isFound() || lookup.getHandle() == null || lookup.getSatellite() == null || lookup.getData() == null) {
            return Result.notFound();
        }

        SatelliteOperationContext context = SatelliteOperationContext.create(world, lookup.getHandle());
        if(!context.isTargetKnown()) return Result.noTargetBody();
        if(requireSurface && context.getSurfaceWorld() == null) return Result.noSurfaceWorld(context);
        return Result.found(context, lookup.getSatellite(), lookup.getData());
    }

    public static final class Result {
        private final Status status;
        @Nullable private final SatelliteOperationContext context;
        @Nullable private final Satellite satellite;
        @Nullable private final SatelliteSavedData data;

        private Result(Status status, @Nullable SatelliteOperationContext context,
                       @Nullable Satellite satellite, @Nullable SatelliteSavedData data) {
            this.status = status;
            this.context = context;
            this.satellite = satellite;
            this.data = data;
        }

        private static Result found(SatelliteOperationContext context, Satellite satellite, SatelliteSavedData data) {
            return new Result(Status.FOUND, context, satellite, data);
        }

        private static Result notFound() { return new Result(Status.NOT_FOUND, null, null, null); }
        private static Result ambiguous() { return new Result(Status.AMBIGUOUS, null, null, null); }
        private static Result noTargetBody() { return new Result(Status.NO_TARGET_BODY, null, null, null); }
        private static Result noSurfaceWorld(SatelliteOperationContext context) {
            return new Result(Status.NO_SURFACE_WORLD, context, null, null);
        }

        public Status getStatus() { return status; }
        public boolean isFound() { return status == Status.FOUND; }
        @Nullable public SatelliteOperationContext getContext() { return context; }
        @Nullable public Satellite getSatellite() { return satellite; }
        @Nullable public SatelliteSavedData getData() { return data; }
    }
}
