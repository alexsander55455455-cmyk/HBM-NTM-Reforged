package com.hbm.testutil;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.IFMLSidedHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/** Minimal sided delegate required when Forge classes are exercised outside a game launch. */
public final class ForgeTestBootstrap {

    private ForgeTestBootstrap() { }

    public static void ensureServerSide() {
        try {
            Field field = FMLCommonHandler.class.getDeclaredField("sidedDelegate");
            field.setAccessible(true);
            if (field.get(FMLCommonHandler.instance()) != null) return;

            IFMLSidedHandler delegate = (IFMLSidedHandler) Proxy.newProxyInstance(
                    IFMLSidedHandler.class.getClassLoader(),
                    new Class<?>[] { IFMLSidedHandler.class },
                    (proxy, method, args) -> {
                        if (method.getName().equals("getSide")) return Side.SERVER;
                        Class<?> type = method.getReturnType();
                        if (type == boolean.class) return false;
                        if (type == int.class) return 0;
                        if (type == long.class) return 0L;
                        if (type == float.class) return 0F;
                        if (type == double.class) return 0D;
                        return null;
                    });
            field.set(FMLCommonHandler.instance(), delegate);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to initialize Forge test sided delegate", e);
        }
    }
}
