package com.hbm.world.gen.nbt;

import com.hbm.handler.ThreeInts;
import com.hbm.testutil.ForgeTestBootstrap;
import com.hbm.world.gen.NTMWorldGenerator;
import com.hbm.world.gen.MapGenNTMFeatures;
import com.hbm.world.gen.component.BunkerComponents;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.gen.structure.*;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.event.world.WorldEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnJre(JRE.JAVA_8)
class StructureLocateTest {
    private static int sequence;

    @BeforeAll static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        NBTStructure.register();
    }

    @Test void deferredCraneIsNotFoundBeforeTerrainLoads() throws Exception {
        Fixture f = fixture("crane", true);
        f.world.ready = false;
        assertNull(f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
        assertEquals(0, f.template.builds);
    }

    @Test void rejectedCraneIsNotFoundAndItsRejectionIsSaved() throws Exception {
        Fixture f = fixture("crane", true);
        f.world.cliff = true;
        assertNull(f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
        assertTrue(f.start.getComponents().isEmpty());
        assertEquals(0, f.template.builds);
        assertFalse(NBTStructure.GenStructure.matchesStructure(new TestGen().getStructureStartAt(f.world, 0, 0), f.spawn));
    }

    @Test void acceptedCraneIsFoundAfterActualPlacementAndDoesNotRebuild() throws Exception {
        Fixture f = fixture("crane", true);
        assertSame(f.start, f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
        assertEquals(1, f.template.builds);
        assertSame(f.start, f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
        assertEquals(1, f.template.builds);
    }

    @Test void ordinaryNbtPlanIsNotReportedUntilItBuilds() throws Exception {
        Fixture f = fixture("spire", false);
        assertNull(f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
        f.start.generateStructure(f.world, new Random(1), f.start.getBoundingBox());
        assertSame(f.start, f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
    }

    @Test void invalidTemplateNeverProducesLocateSuccess() throws Exception {
        Fixture f = fixture("oil_rig", false);
        f.template.valid = false;
        f.start.generateStructure(f.world, new Random(1), f.start.getBoundingBox());
        assertNull(f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
    }

    @Test void terrainConformingRuinHasPlacementEvidenceDespiteNoHeightUpdate() throws Exception {
        Fixture f = fixture("ruin1", false);
        f.spawn.structure.conformToTerrain = true;
        f.start.generateStructure(f.world, new Random(1), f.start.getBoundingBox());
        assertSame(f.start, f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
    }

    @Test void placementEvidenceSurvivesSaveReload() throws Exception {
        Fixture f = fixture("meteor_dungeon", false);
        f.start.generateStructure(f.world, new Random(1), f.start.getBoundingBox());
        MapGenStructureData data = (MapGenStructureData) f.world.storage.getOrLoadData(MapGenStructureData.class, "NBTStructures");
        data.writeInstance(f.start.writeStructureComponentsToNBT(0, 0), 0, 0);
        TestGen reloaded = new TestGen();
        assertNotNull(reloaded.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
        assertEquals(1, f.template.builds);
    }

    @Test void matchingUsesSavedNameRatherThanNewSpawnWeights() throws Exception {
        Fixture f = fixture("crane", true);
        f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0);
        assertNull(f.gen.getVerifiedStructureAt(f.world, new SpawnCondition("spire"), 0, 0));
        assertNotNull(f.gen.getVerifiedStructureAt(f.world, new SpawnCondition("crane"), 0, 0));
    }

    @Test void legacyFeaturesAndBunkersUseTheirRealStartTypes() throws Exception {
        Fixture f = fixture("spire", false);
        StructureStart features = new MapGenNTMFeatures.Start();
        StructureStart bunker = new BunkerComponents.BunkerStart();
        features.getComponents().add(f.start.getComponents().get(0));
        bunker.getComponents().add(f.start.getComponents().get(0));
        assertTrue(NBTStructure.GenStructure.matchesStructure(features, new SpawnCondition("features")));
        assertFalse(NBTStructure.GenStructure.matchesStructure(features, new SpawnCondition("bunker")));
        assertTrue(NBTStructure.GenStructure.matchesStructure(bunker, new SpawnCondition("bunker")));
        assertFalse(NBTStructure.GenStructure.matchesStructure(bunker, new SpawnCondition("features")));
        assertFalse(NBTStructure.GenStructure.matchesStructure(new BunkerComponents.BunkerStart(), new SpawnCondition("bunker")));
    }

    @Test void generatorRecordsAreIsolatedByWorldAndRemovedOnUnload() throws Exception {
        Fixture first = fixture("crane", true);
        Fixture second = fixture("spire", false);
        NTMWorldGenerator generator = allocate(NTMWorldGenerator.class);
        Map<World, NBTStructure.GenStructure> gens = new IdentityHashMap<>();
        gens.put(first.world, first.gen);
        gens.put(second.world, second.gen);
        setField(generator, "nbtGens", gens);
        setField(generator, "populationWorlds", Collections.newSetFromMap(new IdentityHashMap<World, Boolean>()));
        assertSame(first.start, generator.getStructureStartAt(first.world, 0, 0));
        assertSame(second.start, generator.getStructureStartAt(second.world, 0, 0));
        generator.onUnload(new WorldEvent.Unload(first.world));
        assertFalse(gens.containsKey(first.world));
        assertSame(second.start, generator.getStructureStartAt(second.world, 0, 0));
    }

    @Test void legacyStartRequiresPopulatedTerrainBeforeItIsReported() throws Exception {
        Fixture f = fixture("features", false);
        StructureStart start = new MapGenNTMFeatures.Start();
        start.getComponents().add(new LegacyComponent());
        f.gen.put(start);
        f.world.chunk = allocate(TestChunk.class);
        assertNull(f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
        f.world.chunk.populated = true;
        assertSame(start, f.gen.getVerifiedStructureAt(f.world, f.spawn, 0, 0));
    }

    @Test void clientUnloadDoesNotAccessServerGeneratorMaps() throws Exception {
        Fixture f = fixture("crane", true);
        Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.setBoolean(f.world, true);
        NTMWorldGenerator generator = allocate(NTMWorldGenerator.class);
        // Server maps are intentionally absent; a client event must not access them.
        assertDoesNotThrow(() -> generator.onUnload(new WorldEvent.Unload(f.world)));
        assertDoesNotThrow(() -> com.hbm.command.CommandLocate.onWorldUnload(new WorldEvent.Unload(f.world)));
    }

    private static Fixture fixture(String name, boolean guarded) throws Exception {
        Fixture f = new Fixture();
        f.world = allocate(TestWorld.class);
        f.world.ready = true;
        f.world.storage = new MapStorage(null);
        f.world.info = new WorldInfo(new WorldSettings(1, GameType.CREATIVE, true, false, WorldType.DEFAULT), "locate-test");
        f.template = allocate(RecordingStructure.class);
        f.template.valid = true;
        f.template.size = new ThreeInts(53, 61, 21);
        f.spawn = new SpawnCondition(name);
        f.spawn.structure = new JigsawPiece("locate_test_" + sequence++, f.template, -9);
        if (guarded) f.spawn.structure.maxTerrainVariation = 4;
        f.start = new NBTStructure.Start(f.world, new Random(1), f.spawn, 0, 0);
        f.gen = new TestGen();
        f.gen.getStructureStartAt(f.world, 0, 0);
        f.gen.put(f.start);
        return f;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = NTMWorldGenerator.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static class Fixture {
        TestWorld world; TestGen gen; RecordingStructure template; SpawnCondition spawn; NBTStructure.Start start;
    }

    private static class TestGen extends NBTStructure.GenStructure {
        void put(StructureStart start) { structureMap.put(ChunkPos.asLong(0, 0), start); }
    }

    private static class RecordingStructure extends NBTStructure {
        int builds;
        boolean valid;
        RecordingStructure() { super(new ResourceLocation("hbm", "unused")); }
        @Override protected boolean isFoundationColumn(int x, int z, int groundLevel) { return true; }
        @Override protected boolean build(World world, JigsawPiece piece, StructureBoundingBox bounds,
                                          StructureBoundingBox generating, int rotation, String name) {
            builds++;
            return valid;
        }
    }

    private static class TestWorld extends World {
        MapStorage storage;
        WorldInfo info;
        boolean ready, cliff;
        TestChunk chunk;
        TestWorld() { super(null, null, null, null, false); }
        @Override public long getSeed() { return 1; }
        @Override public Chunk getChunk(int x, int z) { return chunk; }
        @Override public MapStorage getPerWorldStorage() { return storage; }
        @Override public WorldInfo getWorldInfo() { return info; }
        @Override public ISaveHandler getSaveHandler() {
            return (ISaveHandler) Proxy.newProxyInstance(ISaveHandler.class.getClassLoader(),
                    new Class<?>[] { ISaveHandler.class }, (proxy, method, args) -> null);
        }
        @Override public BlockPos getTopSolidOrLiquidBlock(BlockPos pos) {
            return new BlockPos(pos.getX(), cliff && (pos.getX() > 30 || pos.getZ() > 30) ? 30 : 64, pos.getZ());
        }
        @Override public BlockPos getHeight(BlockPos pos) { return getTopSolidOrLiquidBlock(pos); }
        @Override public IBlockState getBlockState(BlockPos pos) { return Blocks.STONE.getDefaultState(); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return ready; }
        @Override public Entity getEntityByID(int id) { return null; }
    }

    private static class TestChunk extends Chunk {
        boolean populated;
        TestChunk() { super(null, 0, 0); }
        @Override public boolean isTerrainPopulated() { return populated; }
    }

    private static class LegacyComponent extends StructureComponent {
        LegacyComponent() { boundingBox = new StructureBoundingBox(0, 0, 0, 16, 16, 16); }
        @Override protected void writeStructureToNBT(NBTTagCompound nbt) { }
        @Override protected void readStructureFromNBT(NBTTagCompound nbt, TemplateManager manager) { }
        @Override public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) { return true; }
    }
}
