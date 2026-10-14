package com.hbm.inventory;

import com.hbm.items.tool.ItemBombCaller;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class AirstrikeBlockUseTest {
    private static Unsafe unsafe;
    @BeforeAll static void bootstrap() throws Exception {
        AutoCrystallizerOreDictionaryTest.bootstrap();
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (Unsafe) field.get(null);
    }
    @Test void validBlockUseIsHandledOnClientWithoutConsumingDesignator() throws Exception {
        ItemBombCaller item = new ItemBombCaller("airstrike_block_test");
        TestWorld world = (TestWorld) unsafe.allocateInstance(TestWorld.class);
        Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.setBoolean(world, true);
        TestPlayer player = (TestPlayer) unsafe.allocateInstance(TestPlayer.class);
        for (int type = 0; type < 8; type++) {
            player.held = new ItemStack(item, 2, type);
            assertEquals(EnumActionResult.SUCCESS, item.onItemUse(player, world, new BlockPos(2, 3, 4), EnumHand.MAIN_HAND, EnumFacing.UP, 0, 0, 0));
            assertEquals(2, player.held.getCount());
        }
    }
    @Test void invalidVariantDoesNotStartAnAirstrike() throws Exception {
        ItemBombCaller item = new ItemBombCaller("airstrike_invalid_test");
        TestWorld world = (TestWorld) unsafe.allocateInstance(TestWorld.class);
        TestPlayer player = (TestPlayer) unsafe.allocateInstance(TestPlayer.class);
        player.held = new ItemStack(item, 2, 99);
        assertEquals(EnumActionResult.FAIL, item.onItemUse(player, world, BlockPos.ORIGIN, EnumHand.MAIN_HAND, EnumFacing.UP, 0, 0, 0));
        assertEquals(2, player.held.getCount());
    }
    private static class TestPlayer extends EntityPlayer {
        ItemStack held;
        private TestPlayer() { super(null, null); }
        @Override public ItemStack getHeldItem(EnumHand hand) { return held; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }
    private static class TestWorld extends World {
        private TestWorld() { super(null, null, null, null, true); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
    }
}
