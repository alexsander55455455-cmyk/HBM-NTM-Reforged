package com.hbm.compat;

import com.hbm.compat.mixin.MixinCursedAddonResourceReload;
import com.hbm.testutil.ForgeTestBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.asm.FMLSanityChecker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CursedAddonResourceReloadTest {
    private static Object oldMinecraft;
    private static File oldFmlLocation;
    private Object oldLoader;
    private Object oldThread;
    private Object oldError;
    private Object oldEnabled;
    private LoadController controller;

    @BeforeAll
    static void resourcePackBootstrap() throws Exception {
        ForgeTestBootstrap.ensureServerSide();
        oldMinecraft = field(Minecraft.class, "instance").get(null);
        oldFmlLocation = FMLSanityChecker.fmlLocation;
        Unsafe unsafe = (Unsafe) field(Unsafe.class, "theUnsafe").get(null);
        field(Minecraft.class, "instance").set(null, unsafe.allocateInstance(Minecraft.class));
        FMLSanityChecker.fmlLocation = new File(System.getProperty("java.io.tmpdir"));
    }

    @AfterAll
    static void restoreResourcePackBootstrap() throws Exception {
        field(Minecraft.class, "instance").set(null, oldMinecraft);
        FMLSanityChecker.fmlLocation = oldFmlLocation;
    }

    @BeforeEach
    void completedSplash() throws Exception {
        oldLoader = field(Loader.class, "instance").get(null);
        oldThread = field(SplashProgress.class, "thread").get(null);
        oldError = field(SplashProgress.class, "threadError").get(null);
        oldEnabled = field(SplashProgress.class, "enabled").get(null);
        Unsafe unsafe = (Unsafe) field(Unsafe.class, "theUnsafe").get(null);
        Loader loader = (Loader) unsafe.allocateInstance(Loader.class);
        controller = (LoadController) unsafe.allocateInstance(LoadController.class);
        field(LoadController.class, "state").set(controller, LoaderState.AVAILABLE);
        field(Loader.class, "modController").set(loader, controller);
        field(Loader.class, "instance").set(null, loader);
        Thread splash = new Thread(() -> { });
        splash.start();
        splash.join();
        field(SplashProgress.class, "thread").set(null, splash);
        field(SplashProgress.class, "threadError").set(null, null);
        field(SplashProgress.class, "enabled").set(null, true);
    }

    @AfterEach
    void restoreForgeState() throws Exception {
        field(Loader.class, "instance").set(null, oldLoader);
        field(SplashProgress.class, "thread").set(null, oldThread);
        field(SplashProgress.class, "threadError").set(null, oldError);
        field(SplashProgress.class, "enabled").set(null, oldEnabled);
    }

    @Test
    void installedAddonCallsTerminatedSplashBeforeUploadingModels() throws Exception {
        ClassNode addon = new ClassNode();
        try (InputStream bytes = getClass().getResourceAsStream("/com/leafia/init/ResourceInit.class")) {
            assertNotNull(bytes);
            new ClassReader(bytes).accept(addon, 0);
        }
        List<String> calls = new ArrayList<>();
        for (MethodNode method : addon.methods) {
            if (!method.name.equals("init") || !method.desc.equals("()V")) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals("net/minecraftforge/fml/client/SplashProgress")
                        || call.owner.equals("com/hbm/render/loader/WaveFrontObjectVAO")) calls.add(call.name);
            }
        }
        assertEquals(Arrays.asList("pause", "uploadModels", "resume"), calls);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, SplashProgress::pause);
        assertEquals("Splash thread", thrown.getMessage());
    }

    @Test
    void repeatedReloadDoesNotTouchTerminatedSplashThread() throws Exception {
        for (int reload = 0; reload < 2; reload++) {
            guard("hbm$pauseStartupSplash").invoke(null);
            guard("hbm$resumeStartupSplash").invoke(null);
        }
    }

    @Test
    void initialLoadingStillReportsSplashFailures() throws Exception {
        field(LoadController.class, "state").set(controller, LoaderState.POSTINITIALIZATION);
        for (String method : new String[] { "hbm$pauseStartupSplash", "hbm$resumeStartupSplash" }) {
            InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> guard(method).invoke(null));
            assertTrue(thrown.getCause() instanceof IllegalStateException);
            assertEquals("Splash thread", thrown.getCause().getMessage());
        }
    }

    private static Method guard(String name) throws Exception {
        Method method = MixinCursedAddonResourceReload.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return method;
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
