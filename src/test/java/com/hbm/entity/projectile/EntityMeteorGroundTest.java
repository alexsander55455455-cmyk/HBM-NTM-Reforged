package com.hbm.entity.projectile;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.fluid.ModFluids;
import com.hbm.testutil.ForgeTestBootstrap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@EnabledOnJre(JRE.JAVA_8)
class EntityMeteorGroundTest {

    @BeforeAll
    static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        ModFluids.init();
    }

    @Test
    void grassBecomesNonradioactiveDeadDirt() throws Exception {
        assertSame(ModBlocks.dirt_dead, damage(Blocks.GRASS, false).replacement.getBlock());
    }

    @Test
    void dirtStillBecomesDeadDirt() throws Exception {
        assertSame(ModBlocks.dirt_dead, damage(Blocks.DIRT, false).replacement.getBlock());
    }

    @Test
    void stoneStillBecomesCobblestone() throws Exception {
        assertSame(Blocks.COBBLESTONE, damage(Blocks.STONE, false).replacement.getBlock());
    }

    @Test
    void safeMeteorDoesNotChangeGrass() throws Exception {
        assertNull(damage(Blocks.GRASS, true).replacement);
    }

    private static RecordingWorld damage(Block block, boolean safe) throws Exception {
        RecordingWorld world = allocate(RecordingWorld.class);
        world.original = block.getDefaultState();
        TestMeteor meteor = allocate(TestMeteor.class);
        meteor.safe = safe;
        meteor.damageRoll();
        meteor.damageOrDestroyBlock(world, 0, 64, 0);
        return world;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static class TestMeteor extends EntityMeteor {
        TestMeteor(World world) { super(world); }

        void damageRoll() {
            rand = new Random() {
                @Override
                public int nextInt(int bound) { return 1; }
            };
        }
    }

    private static class RecordingWorld extends World {
        IBlockState original;
        IBlockState replacement;

        RecordingWorld() { super(null, null, null, null, false); }

        @Override
        public IBlockState getBlockState(BlockPos pos) { return original; }

        @Override
        public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
            replacement = state;
            return true;
        }

        @Override
        protected IChunkProvider createChunkProvider() { return null; }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }

        @Override
        public Entity getEntityByID(int id) { return null; }
    }
}
