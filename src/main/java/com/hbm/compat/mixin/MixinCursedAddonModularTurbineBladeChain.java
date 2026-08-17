package com.hbm.compat.mixin;

import com.hbm.main.MainRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;

/** Prevents malformed modular-turbine blade links from hanging the render thread forever. */
@Pseudo
@Mixin(targets = "com.leafia.contents.machines.misc.modular_turbine.ModularTurbineComponentTE", remap = false)
public abstract class MixinCursedAddonModularTurbineBladeChain {

    @Unique
    private static Field hbm$nextBladeField;
    @Unique
    private static boolean hbm$reflectionFailureLogged;

    @Inject(method = "local$checkForBlades", at = @At("RETURN"), remap = false)
    private void hbm$breakCyclicBladeChain(boolean rebuild, CallbackInfo ci) {
        try {
            Field nextBlade = hbm$getNextBladeField(this);
            IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
            Object current = this;
            visited.put(current, Boolean.TRUE);

            for (int links = 0; links < 64; links++) {
                Object next = nextBlade.get(current);
                if (next == null) return;
                if (visited.put(next, Boolean.TRUE) != null) {
                    nextBlade.set(current, null);
                    return;
                }
                current = next;
            }

            // A valid turbine cannot contain an unbounded number of linked stages.
            nextBlade.set(current, null);
        } catch (ReflectiveOperationException exception) {
            if (!hbm$reflectionFailureLogged) {
                hbm$reflectionFailureLogged = true;
                MainRegistry.logger.error("Unable to guard Leafia modular turbine blade links", exception);
            }
        }
    }

    @Unique
    private static Field hbm$getNextBladeField(Object instance) throws NoSuchFieldException {
        if (hbm$nextBladeField != null) return hbm$nextBladeField;

        Class<?> type = instance.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("local$nextBlade");
                field.setAccessible(true);
                hbm$nextBladeField = field;
                return field;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException("local$nextBlade");
    }
}
