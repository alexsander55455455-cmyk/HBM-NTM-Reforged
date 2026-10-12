package com.hbm.command;

import com.hbm.testutil.ForgeTestBootstrap;
import com.hbm.lib.HbmWorld;
import com.hbm.world.gen.NTMWorldGenerator;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.LoadController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnJre(JRE.JAVA_8)
class CommandLocateSearchTest {
    @BeforeAll static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        try {
            Field controller = Loader.class.getDeclaredField("modController");
            controller.setAccessible(true);
            if (controller.get(Loader.instance()) == null) controller.set(Loader.instance(), new LoadController(Loader.instance()));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test void skipsRejectedCandidateAndFindsNextBuiltStructure() throws Exception {
        Fixture f = fixture("crane");
        f.generator.predictions.add(new ChunkPos(0, 0));
        f.generator.starts.put(new ChunkPos(0, 0), start("crane", 0, 0));
        addBuilt(f, "crane", -1, -1);
        finish(f.search);
        assertEquals(new ChunkPos(-1, -1), f.search.found);
    }

    @Test void neverReportsUnbuiltCandidateForAnyNbtStructureName() throws Exception {
        for (String name : Arrays.asList("crane", "spire", "oil_rig", "ruin1", "meteor_dungeon")) {
            Fixture f = fixture(name);
            f.generator.predictions.add(new ChunkPos(0, 0));
            f.generator.starts.put(new ChunkPos(0, 0), start(name, 0, 0));
            finish(f.search);
            assertNull(f.search.found, name);
        }
    }

    @Test void generatedStructuresRemainFindableWhenSeedPredictionChanges() throws Exception {
        Fixture f = fixture("meteor_dungeon");
        addBuilt(f, "meteor_dungeon", 0, 0);
        assertTrue(f.generator.predictions.isEmpty());
        finish(f.search);
        assertEquals(new ChunkPos(0, 0), f.search.found);
    }

    @Test void newTerrainIsLoadedBeforeSuccessIsReported() throws Exception {
        Fixture f = fixture("crane");
        f.generator.predictions.add(new ChunkPos(0, 0));
        f.world.onLoad = () -> {
            if (f.world.loaded.size() == 4) addBuilt(f, "crane", 0, 0);
        };
        assertFalse(f.search.step());
        assertNull(f.search.found);
        assertTrue(f.world.loaded.isEmpty());
        finish(f.search);
        assertEquals(new ChunkPos(0, 0), f.search.found);
        assertTrue(f.world.loaded.contains(new ChunkPos(-1, -1)), "Population border must be loaded");
        assertTrue(f.world.loaded.contains(new ChunkPos(3, 1)), "Whole 53-block footprint must be loaded");
    }

    @Test void oldChunkWithAnotherSavedStructureIsNotRegenerated() throws Exception {
        Fixture f = fixture("crane");
        f.generator.predictions.add(new ChunkPos(0, 0));
        f.generator.starts.put(new ChunkPos(0, 0), start("spire", 0, 0));
        addBuilt(f, "crane", -1, -1);
        finish(f.search);
        assertEquals(new ChunkPos(-1, -1), f.search.found);
        assertFalse(f.world.loaded.contains(new ChunkPos(1, 1)), "Do not populate the mismatched candidate");
    }

    @Test void rejectedEmptyStartIsSkippedWithoutLoadingItsSite() throws Exception {
        Fixture f = fixture("crane");
        NBTStructure.Start rejected = start("crane", 0, 0);
        rejected.getComponents().clear();
        f.generator.starts.put(new ChunkPos(0, 0), rejected);
        f.generator.predictions.add(new ChunkPos(0, 0));
        assertFalse(f.search.step());
        assertNull(f.search.candidate);
        assertTrue(f.world.loaded.isEmpty());
    }

    @Test void negativeCoordinatesUseFloorDivision() throws Exception {
        Fixture f = fixture("crane");
        f.player.posX = -0.1;
        f.player.posZ = -16.1;
        CommandLocate.Search search = new CommandLocate().new Search(f.player, f.spawn, f.generator);
        assertEquals(-1, search.chunkX);
        assertEquals(-2, search.chunkZ);
    }

    @Test void atMostOneCandidateChunkIsLoadedPerServerTick() throws Exception {
        Fixture f = fixture("crane");
        f.generator.predictions.add(new ChunkPos(0, 0));
        f.world.playerEntities.add(f.player);
        f.search.onTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        assertEquals(1, f.world.loaded.size());
        assertNull(f.search.found);
        f.search.onTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        assertEquals(2, f.world.loaded.size());
    }

    @Test void missingStructureNameReturnsUsageInsteadOfCrashing() throws Exception {
        Fixture f = fixture("crane");
        assertThrows(WrongUsageException.class, () -> new CommandLocate().execute(null, f.player, new String[] {"structure"}));
    }

    @Test void searchCancelsWhenPlayerLeavesWorld() throws Exception {
        Fixture f = fixture("crane");
        f.generator.predictions.add(new ChunkPos(0, 0));
        f.search.step();
        f.search.onTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        assertTrue(f.world.loaded.isEmpty());
    }

    @Test void commandUsesTickHandlerReplacesSearchAndCancelsOnUnload() throws Exception {
        Fixture f = fixture("locate_command_event_test");
        f.generator.predictions.add(new ChunkPos(0, 0));
        f.world.playerEntities.add(f.player);
        Field namesField = NBTStructure.class.getDeclaredField("namedMap");
        namesField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String, SpawnCondition> names = (Map<String, SpawnCondition>) namesField.get(null);
        names.put(f.spawn.name, f.spawn);
        NTMWorldGenerator previousGenerator = HbmWorld.worldGenerator;
        HbmWorld.worldGenerator = f.generator;
        CommandLocate command = new CommandLocate();
        Field searchesField = CommandLocate.class.getDeclaredField("searches");
        searchesField.setAccessible(true);
        Map<?, ?> searches = (Map<?, ?>) searchesField.get(command);
        Loader.instance().setActiveModContainer(new DummyModContainer(new ModMetadata()));
        MinecraftForge.EVENT_BUS.register(CommandLocate.class);
        try {
            command.execute(null, f.player, new String[] {"structure", f.spawn.name});
            MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            assertEquals(1, f.world.loaded.size());
            command.execute(null, f.player, new String[] {"structure", f.spawn.name});
            assertEquals(1, searches.size());
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(f.world));
            assertTrue(searches.isEmpty());
            MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            assertEquals(1, f.world.loaded.size(), "Cancelled handlers must stop loading chunks");
        } finally {
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(f.world));
            HbmWorld.worldGenerator = previousGenerator;
            names.remove(f.spawn.name);
            MinecraftForge.EVENT_BUS.unregister(CommandLocate.class);
            Loader.instance().setActiveModContainer(null);
        }
    }

    @Test void perimeterVisitsEveryChunkOnceWithinItsBound() throws Exception {
        Fixture f = fixture("crane");
        Set<ChunkPos> visited = new HashSet<>();
        f.generator.visited = visited;
        finish(f.search);
        assertEquals(511 * 511, visited.size());
        assertTrue(visited.contains(new ChunkPos(-255, -255)));
        assertTrue(visited.contains(new ChunkPos(255, 255)));
        assertFalse(visited.contains(new ChunkPos(256, 0)));
    }

    private static void finish(CommandLocate.Search search) {
        for (int i = 0; i < 300000; i++) if (search.step()) return;
        fail("Search did not finish within its finite radius");
    }

    private static Fixture fixture(String name) throws Exception {
        Fixture f = new Fixture();
        f.spawn = new SpawnCondition(name);
        f.generator = allocate(FakeGenerator.class);
        f.generator.spawn = f.spawn;
        f.generator.predictions = new HashSet<>();
        f.generator.starts = new HashMap<>();
        f.generator.built = new HashSet<>();
        f.world = allocate(SearchWorld.class);
        f.world.loaded = new ArrayList<>();
        Field players = World.class.getDeclaredField("playerEntities");
        players.setAccessible(true);
        players.set(f.world, new ArrayList<EntityPlayer>());
        f.player = allocate(TestPlayer.class);
        f.player.world = f.world;
        f.search = new CommandLocate().new Search(f.player, f.spawn, f.generator);
        return f;
    }

    private static void addBuilt(Fixture f, String name, int x, int z) {
        f.generator.starts.put(new ChunkPos(x, z), start(name, x, z));
        f.generator.built.add(new ChunkPos(x, z));
    }

    private static NBTStructure.Start start(String name, int x, int z) {
        NBTStructure.Start start = new NBTStructure.Start();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("ChunkX", x);
        tag.setInteger("ChunkZ", z);
        tag.setIntArray("BB", new int[] {x * 16, 0, z * 16, x * 16 + 52, 60, z * 16 + 20});
        start.readStructureComponentsFromNBT(null, tag);
        start.name = name;
        start.getComponents().add(new StubComponent(start.getBoundingBox()));
        return start;
    }

    static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static class Fixture {
        TestPlayer player; SearchWorld world; FakeGenerator generator;
        SpawnCondition spawn; CommandLocate.Search search;
    }

    private static class FakeGenerator extends NTMWorldGenerator {
        SpawnCondition spawn;
        Set<ChunkPos> predictions, built, visited;
        Map<ChunkPos, StructureStart> starts;
        @Override public SpawnCondition getStructureAt(World world, int x, int z) {
            return predictions.contains(new ChunkPos(x, z)) ? spawn : null;
        }
        @Override public StructureStart getStructureStartAt(World world, int x, int z) {
            if (visited != null) assertTrue(visited.add(new ChunkPos(x, z)), "Duplicate search position");
            return starts.get(new ChunkPos(x, z));
        }
        @Override public StructureStart getVerifiedStructureAt(World world, SpawnCondition spawn, int x, int z) {
            return built.contains(new ChunkPos(x, z)) ? starts.get(new ChunkPos(x, z)) : null;
        }
    }

    private static class SearchWorld extends World {
        List<ChunkPos> loaded;
        Runnable onLoad;
        SearchWorld() { super(null, null, null, null, false); }
        @Override public Chunk getChunk(int x, int z) {
            loaded.add(new ChunkPos(x, z));
            if (onLoad != null) onLoad.run();
            return null;
        }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
        @Override public Entity getEntityByID(int id) { return null; }
    }

    private static class TestPlayer extends EntityPlayer {
        TestPlayer() { super(null, new GameProfile(UUID.randomUUID(), "test")); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return true; }
        @Override public UUID getUniqueID() { return new UUID(0, 1); }
        @Override public void sendMessage(ITextComponent message) { }
    }

    private static class StubComponent extends StructureComponent {
        StubComponent(StructureBoundingBox box) { boundingBox = box; }
        @Override protected void writeStructureToNBT(NBTTagCompound nbt) { }
        @Override protected void readStructureFromNBT(NBTTagCompound nbt, TemplateManager manager) { }
        @Override public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) { return true; }
    }
}
