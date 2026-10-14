package com.hbm.inventory;

import com.hbm.api.energymk2.Nodespace;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.config.GeneralConfig;
import com.hbm.items.ModItems;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class BatterySocketModeTest {
    private static Unsafe unsafe;
    private static Field energy;
    private static Object oldEnergy;
    private static double oldRate;
    @BeforeAll static void bootstrap() throws Exception {
        AutoCrystallizerOreDictionaryTest.bootstrap();
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (Unsafe) field.get(null);
        energy = CapabilityEnergy.class.getDeclaredField("ENERGY");
        energy.setAccessible(true);
        oldEnergy = energy.get(null);
        unsafe.putObject(unsafe.staticFieldBase(energy), unsafe.staticFieldOffset(energy), unsafe.allocateInstance(Capability.class));
        oldRate = GeneralConfig.conversionRateHeToRF;
        GeneralConfig.conversionRateHeToRF = 2;
    }
    @AfterAll static void restore() {
        unsafe.putObject(unsafe.staticFieldBase(energy), unsafe.staticFieldOffset(energy), oldEnergy);
        GeneralConfig.conversionRateHeToRF = oldRate;
    }
    @Test void socketUpdatesOutputModeWithoutAnHbmNetwork() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        TestWorld world = (TestWorld) ((Unsafe) field.get(null)).allocateInstance(TestWorld.class);
        TestSocket socket = new TestSocket();
        socket.setWorld(world);
        socket.inventory.setStackInSlot(0, new ItemStack(ModItems.battery_creative));
        socket.redLow = TileEntityBatterySocket.mode_output;
        socket.update();
        assertTrue(socket.getProviderSpeed() > 0, "Forge Energy output needs the actual mode even without HBM cables");
        socket.redLow = TileEntityBatterySocket.mode_none;
        socket.update();
        assertEquals(0, socket.getProviderSpeed());
    }
    @Test void socketPushesIntoAReceivingForgeEnergyTileAndDrainsAcceptedHe() throws Exception {
        TestWorld world = (TestWorld) unsafe.allocateInstance(TestWorld.class);
        FeReceiver receiver = new FeReceiver();
        world.target = receiver;
        TestSocket socket = new TestSocket();
        socket.setWorld(world);
        socket.inventory.setStackInSlot(0, new ItemStack(ModItems.battery_creative));
        socket.redLow = TileEntityBatterySocket.mode_output;
        socket.update();
        assertEquals(640, receiver.storage.getEnergyStored(), "EnderIO and RS accept pushed energy without pulling it");
        assertEquals(EnumFacing.WEST, receiver.face);
        assertEquals(320, socket.usedPower, "Accepted FE must be converted back to HE before draining the battery");
    }
    @Test void socketOnlyPushesInOutputOrBufferMode() throws Exception {
        TestWorld world = (TestWorld) unsafe.allocateInstance(TestWorld.class);
        FeReceiver receiver = new FeReceiver();
        world.target = receiver;
        TestSocket socket = new TestSocket();
        socket.setWorld(world);
        socket.inventory.setStackInSlot(0, new ItemStack(ModItems.battery_creative));
        for (int mode : new int[]{TileEntityBatterySocket.mode_none, TileEntityBatterySocket.mode_input}) {
            socket.redLow = (short) mode;
            socket.update();
            assertEquals(0, receiver.storage.getEnergyStored());
        }
        socket.redLow = TileEntityBatterySocket.mode_buffer;
        socket.update();
        assertEquals(640, receiver.storage.getEnergyStored());
    }
    @Test void forgeEnergyPushDoesNotBypassTheNativeHbmNetwork() throws Exception {
        TestWorld world = (TestWorld) unsafe.allocateInstance(TestWorld.class);
        NativeReceiver receiver = new NativeReceiver();
        world.target = receiver;
        TestSocket socket = new TestSocket();
        socket.setWorld(world);
        socket.inventory.setStackInSlot(0, new ItemStack(ModItems.battery_creative));
        socket.redLow = TileEntityBatterySocket.mode_output;
        socket.update();
        assertEquals(0, receiver.power);
        assertEquals(0, socket.usedPower);
    }
    private static class TestSocket extends TileEntityBatterySocket {
        long usedPower;
        TestSocket() { node = new Nodespace.PowerNode(BlockPos.ORIGIN); }
        @Override public BlockPos[] getPortPos() { return new BlockPos[]{BlockPos.ORIGIN}; }
        @Override public DirPos[] getConPos() { return new DirPos[]{new DirPos(1, 0, 0, ForgeDirection.EAST)}; }
        @Override public void usePower(long power) { usedPower += power; super.usePower(power); }
        @Override public void networkPackNT(int range) { }
        @Override public byte getComparatorPower() { return 0; }
    }
    private static class TestWorld extends World {
        TileEntity target;
        private TestWorld() { super(null, null, null, null, false); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
        @Override public boolean isBlockPowered(BlockPos pos) { return false; }
        @Override public TileEntity getTileEntity(BlockPos pos) { return pos.equals(new BlockPos(1, 0, 0)) ? target : null; }
        @Override public IBlockState getBlockState(BlockPos pos) { return Blocks.AIR.getDefaultState(); }
    }
    private static class FeReceiver extends TileEntity {
        final EnergyStorage storage = new EnergyStorage(10000, 640, 0);
        EnumFacing face;
        @Override public boolean hasCapability(Capability<?> capability, EnumFacing facing) { return capability == CapabilityEnergy.ENERGY; }
        @Override public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            face = facing;
            return (T) storage;
        }
    }
    private static class NativeReceiver extends TileEntity implements IEnergyReceiverMK2 {
        long power;
        @Override public boolean isLoaded() { return true; }
        @Override public long getPower() { return power; }
        @Override public long getMaxPower() { return 10000; }
        @Override public void setPower(long value) { power = value; }
    }
}
