package com.hbm.entity.effect;

import com.hbm.config.BombConfig;
import com.hbm.config.WorldConfig;
import com.hbm.lib.maps.NonBlockingHashMapLong;
import com.hbm.testutil.ForgeTestBootstrap;
import com.hbm.world.biome.BiomeGenCraterBase;
import net.minecraft.entity.Entity;
import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.Loader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledOnJre(JRE.JAVA_8)
class EntityFalloutRainBiomeTest {

    @BeforeAll
    static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        if (!Bootstrap.isRegistered()) Bootstrap.register();
    }

    @Test
    void nonradioactiveExplosionsDoNotCreateCraterBiomes() throws Exception {
        boolean previous = WorldConfig.enableCraterBiomes;
        boolean previousCommit = BombConfig.safeCommit;
        Field loaderInstance = Loader.class.getDeclaredField("instance");
        loaderInstance.setAccessible(true);
        Object previousLoader = loaderInstance.get(null);
        LaunchClassLoader previousClassLoader = Launch.classLoader;
        try {
            if (previousClassLoader == null) Launch.classLoader = new LaunchClassLoader(new URL[0]);
            Loader loader = allocate(Loader.class);
            Field loadedMods = Loader.class.getDeclaredField("namedMods");
            loadedMods.setAccessible(true);
            loadedMods.set(loader, Collections.emptyMap());
            loaderInstance.set(null, loader);
            WorldConfig.enableCraterBiomes = true;
            BombConfig.safeCommit = true;
            for (int scale : new int[] {25, 100, 250}) {
                for (int chunkX : new int[] {0, 3, 12}) {
                    EntityFalloutRain rain = allocate(EntityFalloutRain.class);
                    rain.doFallout = false;
                    rain.world = allocate(PlainsWorld.class);
                    rain.world.provider = new WorldProviderSurface();
                    rain.biomeChange = true;
                    rain.pendingChunks = 2;
                    Field tasks = EntityFalloutRain.class.getDeclaredField("mainTasks");
                    tasks.setAccessible(true);
                    tasks.set(rain, tasks.getType().getConstructor(int.class).newInstance(16));
                    rain.mirror = new NonBlockingHashMapLong<>();
                    long chunkPos = ChunkPos.asLong(chunkX, 0);
                    rain.mirror.put(chunkPos, allocate(EmptyChunk.class));
                    Field radius = EntityFalloutRain.class.getDeclaredField("s0");
                    radius.setAccessible(true);
                    radius.setDouble(rain, Double.MAX_VALUE);

                    rain.processChunkOffThread(chunkPos, scale, false);

                    assertTrue(EntityFalloutRain.TL_WORKER.get().biomeChanges.isEmpty(),
                            "Nonradioactive explosion must leave the existing biome unchanged");
                    assertTrue(rain.mainTasks.isEmpty());
                }
            }
        } finally {
            WorldConfig.enableCraterBiomes = previous;
            BombConfig.safeCommit = previousCommit;
            loaderInstance.set(null, previousLoader);
            if (previousClassLoader == null) {
                Launch.classLoader.close();
                Launch.classLoader = null;
            }
        }
    }

    @Test
    void nuclearFalloutKeepsAllThreeCraterBiomes() {
        boolean previous = WorldConfig.enableCraterBiomes;
        try {
            WorldConfig.enableCraterBiomes = true;
            assertSame(BiomeGenCraterBase.craterInnerBiome, EntityFalloutRain.getBiomeChange(0, 250, Biomes.PLAINS));
            assertSame(BiomeGenCraterBase.craterBiome, EntityFalloutRain.getBiomeChange(20, 250, Biomes.PLAINS));
            assertSame(BiomeGenCraterBase.craterOuterBiome, EntityFalloutRain.getBiomeChange(80, 250, Biomes.PLAINS));
        } finally {
            WorldConfig.enableCraterBiomes = previous;
        }
    }

    @Test
    void disabledCraterBiomesStayDisabledForNuclearFallout() {
        boolean previous = WorldConfig.enableCraterBiomes;
        try {
            WorldConfig.enableCraterBiomes = false;
            assertNull(EntityFalloutRain.getBiomeChange(0, 250, Biomes.PLAINS));
        } finally {
            WorldConfig.enableCraterBiomes = previous;
        }
    }

    @Test
    void biomeSelectorPreservesCursedAddonStaticShadowContract() throws Exception {
        assertTrue(Modifier.isStatic(EntityFalloutRain.class
                .getDeclaredMethod("getBiomeChange", double.class, int.class, Biome.class).getModifiers()));
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static class EmptyChunk extends Chunk {
        EmptyChunk(World world) { super(world, 0, 0); }

        @Override
        public ExtendedBlockStorage[] getBlockStorageArray() { return new ExtendedBlockStorage[16]; }
    }

    private static class PlainsWorld extends World {
        PlainsWorld() { super(null, null, null, null, false); }

        @Override
        public Biome getBiome(BlockPos pos) { return Biomes.PLAINS; }

        @Override
        protected IChunkProvider createChunkProvider() { return null; }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }

        @Override
        public Entity getEntityByID(int id) { return null; }
    }
}
