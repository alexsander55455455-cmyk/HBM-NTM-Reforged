package com.hbm.world.gen.nbt;

import com.hbm.handler.ThreeInts;
import com.hbm.testutil.ForgeTestBootstrap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.WorldSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnJre(JRE.JAVA_8)
class CraneTerrainTest {

    private static int sequence;

    @BeforeAll
    static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        if (!Bootstrap.isRegistered()) Bootstrap.register();
    }

    @Test
    void rejectsCliffUnderFoundationInEveryRotation() throws Exception {
        for (int rotation = 0; rotation < 4; rotation++) {
            Fixture fixture = fixture(rotation, true);
            final int facing = rotation;
            fixture.world.ground = pos -> localX(pos, facing) > 16 ? 30 : 64;

            fixture.start.generateStructure(fixture.world, new Random(1), chunk(0, 0));

            assertEquals(0, fixture.structure.builds, "Crane must be rejected before placing its first chunk");
            assertTrue(fixture.start.getComponents().isEmpty(), "Rejected crane must not resume in later chunks");
        }
    }

    @Test
    void treeCanopyDoesNotRaiseCraneAboveGround() throws Exception {
        Fixture fixture = fixture(0, true);
        fixture.world.canopy = 45;

        assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));

        assertEquals(55, fixture.component.getBoundingBox().minY);
    }

    @Test
    void levelTerrainPlacementDoesNotDependOnFirstChunkOrRotation() throws Exception {
        for (int rotation = 0; rotation < 4; rotation++) {
            for (boolean farChunkFirst : new boolean[] {false, true}) {
                Fixture fixture = fixture(rotation, true);
                StructureBoundingBox first = farChunkFirst
                        ? (rotation % 2 == 0 ? chunk(2, 0) : chunk(0, 2)) : chunk(0, 0);

                assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), first));
                assertEquals(55, fixture.component.getBoundingBox().minY);
                assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
                assertEquals(55, fixture.component.getBoundingBox().minY, "Height must be resolved only once");
            }
        }
    }

    @Test
    void gentleSlopeIsAllowedAndUsesFoundationHeight() throws Exception {
        Fixture fixture = fixture(0, true);
        fixture.world.ground = pos -> pos.getX() <= 16 ? 64 : 68;

        assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));

        assertEquals(56, fixture.component.getBoundingBox().minY);
    }

    @Test
    void fiveBlockDropIsRejected() throws Exception {
        Fixture fixture = fixture(0, true);
        fixture.world.ground = pos -> pos.getX() <= 16 ? 64 : 69;

        assertFalse(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
        assertEquals(0, fixture.structure.builds);
    }

    @Test
    void heightClampCannotLiftCraneOffShallowTerrain() throws Exception {
        Fixture fixture = fixture(0, true);
        fixture.world.ground = pos -> 4;

        assertFalse(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
        assertEquals(0, fixture.structure.builds);
    }

    @Test
    void waterUnderFoundationRejectsCraneBeforePlacement() throws Exception {
        Fixture fixture = fixture(0, true);
        fixture.world.waterX = 16;

        assertFalse(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
        assertEquals(0, fixture.structure.builds);
    }

    @Test
    void unevenTerrainUnderBoomDoesNotRejectOrRaiseCraneInEveryRotation() throws Exception {
        for (int rotation = 0; rotation < 4; rotation++) {
            Fixture fixture = fixture(rotation, true);
            final int facing = rotation;
            fixture.world.ground = pos -> localX(pos, facing) > 30 ? 90 : 64;

            assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
            assertEquals(55, fixture.component.getBoundingBox().minY);
            assertEquals(1, fixture.structure.builds);
        }
    }

    @Test
    void waterUnderBoomDoesNotRejectCrane() throws Exception {
        Fixture fixture = fixture(0, true);
        fixture.world.waterX = 40;

        assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
        assertEquals(55, fixture.component.getBoundingBox().minY);
        assertEquals(1, fixture.structure.builds);
    }

    @Test
    void superflatWorldKeepsCraneFoundationBelowSurface() throws Exception {
        Fixture fixture = fixture(0, true);
        fixture.world.ground = pos -> 4;
        fixture.world.info = worldInfo(WorldType.FLAT);

        assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));

        assertEquals(-5, fixture.component.getBoundingBox().minY);
    }

    @Test
    void unguardedStructuresKeepExistingHeightBehavior() throws Exception {
        Fixture fixture = fixture(0, false);
        fixture.world.ground = pos -> pos.getX() > 30 ? 30 : 64;
        fixture.world.canopy = 20;

        assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));

        assertEquals(75, fixture.component.getBoundingBox().minY);
        assertEquals(1, fixture.structure.builds);
    }

    @Test
    void waitsForLoadedTerrainThenPlacesWholeCraneOnce() throws Exception {
        Fixture fixture = fixture(0, true);
        fixture.world.chunksReady = false;
        fixture.world.ground = pos -> { throw new AssertionError("Must not read unloaded terrain"); };

        assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
        assertEquals(0, fixture.structure.builds);
        assertFalse(fixture.component.heightUpdated);

        fixture.world.chunksReady = true;
        fixture.world.ground = pos -> 64;
        assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(2, 0)));
        assertEquals(1, fixture.structure.builds);
        assertEquals(fixture.component.getBoundingBox(), fixture.structure.generatedBounds);
        assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
        assertEquals(1, fixture.structure.builds, "Later chunks must not overwrite the completed crane");
    }

    @Test
    void completedCraneStaysCompletedAfterSavingAndReloading() throws Exception {
        Fixture fixture = fixture(0, true);
        assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
        NBTTagCompound nbt = new NBTTagCompound();
        fixture.component.writeStructureToNBT(nbt);
        NBTStructure.Component reloaded = new NBTStructure.Component();
        reloaded.readStructureFromNBT(nbt, null);

        assertTrue(reloaded.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
        assertEquals(1, fixture.structure.builds);
    }

    @Test
    void legacyPartlyGeneratedCraneCanFinishExistingChunks() throws Exception {
        Fixture fixture = fixture(0, true);
        fixture.component.offsetYHeight(55);
        fixture.world.chunksReady = false;
        fixture.world.ground = pos -> { throw new AssertionError("Legacy height is already resolved"); };

        assertTrue(fixture.component.addComponentParts(fixture.world, new Random(1), chunk(0, 0)));
        assertEquals(1, fixture.structure.builds);
        assertEquals(chunk(0, 0).toString(), fixture.structure.generatedBounds.toString());
    }

    private static Fixture fixture(final int rotation, boolean guarded) throws Exception {
        RecordingStructure structure = allocate(RecordingStructure.class);
        structure.size = new ThreeInts(53, 61, 21);
        JigsawPiece piece = new JigsawPiece("crane_terrain_test_" + sequence++, structure, -9);
        if (guarded) piece.maxTerrainVariation = 4;
        SpawnCondition spawn = new SpawnCondition(piece.name);
        spawn.structure = piece;
        TerrainWorld world = allocate(TerrainWorld.class);
        world.ground = pos -> 64;
        world.waterX = Integer.MAX_VALUE;
        world.chunksReady = true;
        world.info = worldInfo(WorldType.DEFAULT);
        NBTStructure.Start start = new NBTStructure.Start(world, new Random(1) {
            @Override public int nextInt(int bound) { return rotation; }
        }, spawn, 0, 0);
        return new Fixture(world, structure, start, (NBTStructure.Component) start.getComponents().get(0));
    }

    private static StructureBoundingBox chunk(int x, int z) {
        return new StructureBoundingBox(x * 16 + 8, 0, z * 16 + 8, x * 16 + 23, 255, z * 16 + 23);
    }

    private static int localX(BlockPos pos, int rotation) {
        switch (rotation) {
            case 1: return pos.getZ();
            case 2: return 52 - pos.getX();
            case 3: return 52 - pos.getZ();
            default: return pos.getX();
        }
    }

    private static WorldInfo worldInfo(WorldType type) {
        return new WorldInfo(new WorldSettings(1, GameType.CREATIVE, false, false, type), "crane-test");
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static class Fixture {
        final TerrainWorld world;
        final RecordingStructure structure;
        final NBTStructure.Start start;
        final NBTStructure.Component component;

        Fixture(TerrainWorld world, RecordingStructure structure, NBTStructure.Start start, NBTStructure.Component component) {
            this.world = world;
            this.structure = structure;
            this.start = start;
            this.component = component;
        }
    }

    private static class RecordingStructure extends NBTStructure {
        int builds;
        StructureBoundingBox generatedBounds;

        RecordingStructure() { super(new ResourceLocation("hbm", "unused-test-template")); }

        @Override
        protected boolean isFoundationColumn(int x, int z, int groundLevel) {
            return x >= 8 && x <= 24 && z >= 6 && z <= 18;
        }

        @Override
        protected boolean build(World world, JigsawPiece piece, StructureBoundingBox bounds,
                                StructureBoundingBox generatingBounds, int rotation, String name) {
            builds++;
            this.generatedBounds = generatingBounds;
            return true;
        }
    }

    private static class TerrainWorld extends World {
        ToIntFunction<BlockPos> ground;
        int canopy;
        int waterX;
        WorldInfo info;
        boolean chunksReady;

        TerrainWorld() { super(null, null, null, null, false); }

        @Override public WorldInfo getWorldInfo() { return info; }
        @Override public BlockPos getHeight(BlockPos pos) { return new BlockPos(pos.getX(), ground.applyAsInt(pos) + canopy, pos.getZ()); }
        @Override public BlockPos getTopSolidOrLiquidBlock(BlockPos pos) { return new BlockPos(pos.getX(), ground.applyAsInt(pos), pos.getZ()); }
        @Override public IBlockState getBlockState(BlockPos pos) {
            return (pos.getX() == waterX ? Blocks.WATER : Blocks.STONE).getDefaultState();
        }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return chunksReady; }
        @Override public Entity getEntityByID(int id) { return null; }
    }
}
