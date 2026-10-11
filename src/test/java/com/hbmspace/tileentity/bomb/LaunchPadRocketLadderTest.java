package com.hbmspace.tileentity.bomb;

import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.ClimbableRegistry;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchPadRocketLadderTest {

    @AfterEach
    void clearRegistry() {
        ClimbableRegistry.clearAll();
    }

    @Test
    void bothLadderSectionsWorkInEveryOrientationWithoutClimbingTheGap() throws Exception {
        for (ForgeDirection direction : new ForgeDirection[] {
                ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST}) {
            PadProbe pad = pad(direction);
            pad.height = 12;
            EntityLivingBase lower = entity(pad, 6.5, 1);
            EntityLivingBase upper = entity(pad, 2.5, 10);
            assertTrue(pad.isEntityInClimbAABB(lower), direction.name());
            assertTrue(pad.isEntityInClimbAABB(upper), direction.name());
            assertTrue(pad.getClimbAABBForIndexing().intersects(lower.getEntityBoundingBox()));
            assertTrue(pad.getClimbAABBForIndexing().intersects(upper.getEntityBoundingBox()));
            assertFalse(pad.isEntityInClimbAABB(entity(pad, 4.5, 10)));
            assertFalse(pad.isEntityInClimbAABB(entity(pad, 2.5, 16)));
        }
    }

    @Test
    void ladderBoundsFollowTowerGrowthAndRemoval() throws Exception {
        PadProbe pad = pad(ForgeDirection.NORTH);
        assertEquals(67, pad.getClimbAABBForIndexing().maxY);
        pad.height = 12;
        assertEquals(79, pad.getClimbAABBForIndexing().maxY);
        pad.height = 20;
        assertEquals(87, pad.getClimbAABBForIndexing().maxY);
        assertTrue(pad.isEntityInClimbAABB(entity(pad, 2.5, 20)));
        pad.height = 0;
        assertEquals(67, pad.getClimbAABBForIndexing().maxY);
        assertFalse(pad.isEntityInClimbAABB(entity(pad, 2.5, 10)));
    }

    @Test
    void lifecycleRegistersAndRemovesBothSectionsAcrossChunkBoundaries() throws Exception {
        PadProbe pad = pad(ForgeDirection.NORTH);
        World world = world(false);
        pad.setWorld(world);
        pad.height = 12;
        EntityLivingBase upper = entity(pad, 2.5, 10);
        assertEquals(0, ClimbableRegistry.countClimbablesInDim(world, 0));
        pad.onLoad();
        assertEquals(1, ClimbableRegistry.countClimbablesInDim(world, 0));
        assertTrue(ClimbableRegistry.isEntityOnAny(world, entity(pad, 6.5, 1)));
        assertTrue(ClimbableRegistry.isEntityOnAny(world, upper));
        pad.onChunkUnload();
        assertEquals(0, ClimbableRegistry.countClimbablesInDim(world, 0));
        assertFalse(ClimbableRegistry.isEntityOnAny(world, upper));
        pad.onLoad();
        assertTrue(ClimbableRegistry.isEntityOnAny(world, upper));
        pad.invalidate();
        assertEquals(0, ClimbableRegistry.countClimbablesInDim(world, 0));
    }

    @Test
    void savedAndSyncedTowerChangesRefreshTheRegistry() throws Exception {
        PadProbe pad = pad(ForgeDirection.NORTH);
        World world = world(true);
        pad.setWorld(world);
        pad.onLoad();
        EntityLivingBase upper = entity(pad, 2.5, 10);
        assertFalse(ClimbableRegistry.isEntityOnAny(world, upper));

        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("x", pad.getPos().getX());
        nbt.setInteger("y", pad.getPos().getY());
        nbt.setInteger("z", pad.getPos().getZ());
        nbt.setInteger("height", 12);
        nbt.setBoolean("sky", true);
        pad.readFromNBT(nbt);
        assertTrue(ClimbableRegistry.isEntityOnAny(world, upper));

        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeBoolean(false);
            buffer.writeLong(0);
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeBoolean(true);
            buffer.writeBoolean(false);
            pad.deserialize(buffer);
        } finally {
            buffer.release();
        }
        assertEquals(1, ClimbableRegistry.countClimbablesInDim(world, 0));
        assertFalse(ClimbableRegistry.isEntityOnAny(world, upper));
        pad.onChunkUnload();
        nbt.setInteger("height", 20);
        pad.readFromNBT(nbt);
        assertEquals(0, ClimbableRegistry.countClimbablesInDim(world, 0));
    }

    private static PadProbe pad(ForgeDirection direction) throws Exception {
        // Only ladder geometry and lifecycle are tested; skip unrelated item/fluid registration.
        PadProbe pad = allocate(PadProbe.class);
        pad.direction = direction;
        pad.canSeeSky = true;
        pad.setPos(new BlockPos(4, 64, 4));
        pad.solidFuel = new TileEntityLaunchPadRocket.SolidFuelTank();
        pad.tanks = new FluidTankNTM[0];
        return pad;
    }

    private static EntityLivingBase entity(PadProbe pad, double ladderOffset, double y) throws Exception {
        ForgeDirection rot = pad.direction.getRotation(ForgeDirection.UP);
        double x = pad.getPos().getX() + 0.5 - rot.offsetX * ladderOffset - pad.direction.offsetX * 5;
        double z = pad.getPos().getZ() + 0.5 - rot.offsetZ * ladderOffset - pad.direction.offsetZ * 5;
        EntityArmorStand entity = allocate(EntityArmorStand.class);
        entity.setEntityBoundingBox(new AxisAlignedBB(x - 0.3, pad.getPos().getY() + y, z - 0.3,
                x + 0.3, pad.getPos().getY() + y + 1.8, z + 0.3));
        return entity;
    }

    private static World world(boolean client) throws Exception {
        World world = allocate(TestWorld.class);
        Field provider = World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(world, new WorldProviderSurface());
        Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.setBoolean(world, client);
        return world;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    public static class PadProbe extends TileEntityLaunchPadRocket {
        ForgeDirection direction;
        @Override public int getBlockMetadata() { return BlockDummyable.offset + direction.ordinal(); }
    }

    private static class TestWorld extends World {
        private TestWorld() { super(null, null, null, null, false); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
    }
}
