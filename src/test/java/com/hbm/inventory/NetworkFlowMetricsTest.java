package com.hbm.inventory;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.PowerNetMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.api.fluidmk2.FluidNetMK2;
import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.blocks.network.FluidDuctGauge.TileEntityPipeGauge;
import com.hbm.blocks.network.energy.BlockCableGauge.TileEntityCableGauge;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.render.misc.EnumSymbol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class NetworkFlowMetricsTest {
    private static LaunchClassLoader previousLoader;
    private static boolean autoConversion;
    @BeforeAll static void bootstrap() throws Exception {
        previousLoader = Launch.classLoader;
        if (previousLoader == null) Launch.classLoader = new LaunchClassLoader(new URL[0]);
        AutoCrystallizerOreDictionaryTest.bootstrap();
        if (Fluids.NONE == null) Fluids.NONE = new FluidType("NONE", 0x888888, 0, 0, 0, EnumSymbol.NONE);
        if (Fluids.WATER == null) Fluids.WATER = new FluidType("WATER", 0x3333ff, 0, 0, 0, EnumSymbol.NONE);
        autoConversion = GeneralConfig.autoCableConversion;
        GeneralConfig.autoCableConversion = false;
    }
    @AfterAll static void cleanup() throws Exception {
        GeneralConfig.autoCableConversion = autoConversion;
        if (previousLoader == null) { Launch.classLoader.close(); Launch.classLoader = null; }
    }
    @Test void powerBuffersDoNotReportTheirOwnCirculation() {
        PowerNetMK2 net = new PowerNetMK2();
        PowerStore a = new PowerStore(1000, 2000, 100), b = new PowerStore(1000, 2000, 100);
        net.addProvider(a); net.addReceiver(a); net.addProvider(b); net.addReceiver(b);
        net.update();
        assertTrue(a.received + b.received > 0);
        assertEquals(2000, a.power + b.power);
        assertEquals(0, net.energyTracker);
    }
    @Test void powerBuffersAndConsumerReportOnlyConsumerDemand() {
        PowerNetMK2 net = new PowerNetMK2();
        PowerStore a = new PowerStore(1000, 2000, 100), b = new PowerStore(1000, 2000, 100), load = new PowerStore(0, 2000, 25);
        net.addProvider(a); net.addReceiver(a); net.addProvider(b); net.addReceiver(b); net.addReceiver(load);
        net.update();
        assertEquals(load.received, net.energyTracker); assertTrue(load.received > 0);
        assertEquals(2000, a.power + b.power + load.power);
    }
    @Test void creativePowerSourceStillReportsDeliveredPower() {
        PowerNetMK2 net = new PowerNetMK2();
        PowerStore source = new PowerStore(1000, 1000, 100), load = new PowerStore(0, 1000, 100);
        source.creative = true; net.addProvider(source); net.addReceiver(load); net.update();
        assertEquals(1000, source.power); assertEquals(100, load.power); assertEquals(100, net.energyTracker);
    }
    @Test void partialPowerAcceptanceAndInactiveProviderRoleAreCounted() {
        PowerNetMK2 net = new PowerNetMK2();
        PowerStore source = new PowerStore(1000, 1000, 100), load = new PowerStore(0, 1000, 100);
        load.acceptLimit = 37; net.addProvider(source); net.addReceiver(load); net.update();
        assertEquals(37, net.energyTracker); assertEquals(963, source.power);
    }
    @Test void diodeSimulationAndBufferInjectionDoNotInflatePowerFlow() {
        PowerNetMK2 net = new PowerNetMK2(); PowerStore buffer = new PowerStore(1000, 2000, 100);
        net.addProvider(buffer); net.addReceiver(buffer);
        assertEquals(0, net.sendPowerDiode(100, true)); assertEquals(0, buffer.received); assertEquals(0, net.energyTracker);
        assertEquals(0, net.sendPowerDiode(100, false)); assertEquals(1100, buffer.power); assertEquals(0, net.energyTracker);
    }
    @Test void expiredProviderRoleDoesNotHideDiodeConsumerDemand() {
        PowerNetMK2 net = new PowerNetMK2(); PowerStore load = new PowerStore(0, 1000, 100);
        net.providerEntries.put(load, 0); net.addReceiver(load);
        assertEquals(0, net.sendPowerDiode(100, false)); assertEquals(100, net.energyTracker);
    }
    @Test void diodeExtractionCountsRealDrawAndSaturatesTheMetric() {
        PowerNetMK2 net = new PowerNetMK2(); PowerStore source = new PowerStore(1000, 1000, 100);
        net.addProvider(source); net.energyTracker = Long.MAX_VALUE - 5;
        assertEquals(10, net.extractPowerDiode(10, true)); assertEquals(Long.MAX_VALUE - 5, net.energyTracker);
        assertEquals(10, net.extractPowerDiode(10, false)); assertEquals(Long.MAX_VALUE, net.energyTracker); assertEquals(990, source.power);
    }
    @Test void powerUpdateAndDiodeSendSaturateTheMetric() {
        PowerNetMK2 net = new PowerNetMK2(); PowerStore source = new PowerStore(1000, 1000, 100), load = new PowerStore(0, 1000, 100);
        net.addProvider(source); net.addReceiver(load); net.energyTracker = Long.MAX_VALUE - 5;
        net.update(); assertEquals(Long.MAX_VALUE, net.energyTracker); assertEquals(100, load.power);
        net.resetTrackers(); assertEquals(0, net.energyTracker);
        net.energyTracker = Long.MAX_VALUE - 5; net.sendPowerDiode(10, false); assertEquals(Long.MAX_VALUE, net.energyTracker);
    }
    @Test void hugeStoredSupplyDoesNotOverflowSmallFlowReadings() {
        PowerNetMK2 net = new PowerNetMK2();
        PowerStore a = new PowerStore(Long.MAX_VALUE, Long.MAX_VALUE, 10), b = new PowerStore(Long.MAX_VALUE, Long.MAX_VALUE, 10), load = new PowerStore(0, 1000, 100);
        net.addProvider(a); net.addProvider(b); net.addReceiver(load); net.update();
        assertEquals(20, load.power); assertEquals(20, net.energyTracker);
    }
    @Test void fluidBuffersDoNotReportTheirOwnCirculation() {
        FluidNetMK2 net = new FluidNetMK2(Fluids.WATER);
        FluidStore a = new FluidStore(1000, 0, 0), b = new FluidStore(1000, 0, 0);
        net.addProvider(a); net.addReceiver(a); net.addProvider(b); net.addReceiver(b); net.update();
        assertTrue(a.received + b.received > 0); assertEquals(2000, a.stored[0] + b.stored[0]); assertEquals(0, net.fluidTracker);
    }
    @Test void fluidConsumersAndCreativeSourcesWorkAtEveryPressure() {
        for (int p = 0; p <= 5; p++) {
            FluidNetMK2 net = new FluidNetMK2(Fluids.WATER);
            FluidStore source = new FluidStore(1000, p, p), load = new FluidStore(0, p, p);
            source.creative = true; load.acceptLimit = 37;
            net.addProvider(source); net.addReceiver(load); net.update();
            assertEquals(37, load.stored[p]); assertEquals(37, net.fluidTracker); assertEquals(1000, source.stored[p]);
        }
    }
    @Test void fluidReceivedOutsideTheProvidersSendingTankStillCounts() {
        FluidNetMK2 net = new FluidNetMK2(Fluids.WATER);
        FluidStore source = new FluidStore(1000, 0, 0), machine = new FluidStore(1000, 0, 1);
        net.addProvider(source); net.addProvider(machine); net.addReceiver(machine); net.update();
        assertEquals(100, machine.stored[0]); assertEquals(100, net.fluidTracker);
    }
    @Test void fluidReceivedIntoASeparateTankAtTheSamePressureStillCounts() {
        FluidNetMK2 net = new FluidNetMK2(Fluids.WATER);
        FluidStore machine = new FluidStore(0, 0, 0) {
            long output = 1000;
            @Override public long getFluidAvailable(FluidType type, int pressure) { return pressure == 0 ? output : 0; }
            @Override public void useUpFluid(FluidType type, int pressure, long amount) { output -= amount; }
        };
        net.addProvider(machine); net.addReceiver(machine); net.update();
        assertEquals(100, machine.stored[0]); assertEquals(100, net.fluidTracker);
        assertEquals(900, machine.getFluidAvailable(Fluids.WATER, 0));
    }
    @Test void fluidBuffersAndConsumerReportOnlyConsumerDemand() {
        FluidNetMK2 net = new FluidNetMK2(Fluids.WATER);
        FluidStore a = new FluidStore(1000, 0, 0), b = new FluidStore(1000, 0, 0), load = new FluidStore(0, 0, 0);
        net.addProvider(a); net.addReceiver(a); net.addProvider(b); net.addReceiver(b); net.addReceiver(load); net.update();
        assertTrue(load.received > 0); assertEquals(load.received, net.fluidTracker);
        assertEquals(2000, a.stored[0] + b.stored[0] + load.stored[0]);
    }
    @Test void fluidMetricSaturatesAndResetsWithoutChangingTransfer() {
        FluidNetMK2 net = new FluidNetMK2(Fluids.WATER); FluidStore source = new FluidStore(1000, 0, 0), load = new FluidStore(0, 0, 0);
        net.addProvider(source); net.addReceiver(load); net.fluidTracker = Long.MAX_VALUE - 5;
        net.update(); assertEquals(100, load.stored[0]); assertEquals(Long.MAX_VALUE, net.fluidTracker);
        net.resetTrackers(); assertEquals(0, net.fluidTracker);
    }
    @Test void disconnectedPowerGaugeClearsItsReadingsAndSecondBucket() throws Exception {
        TestWorld world = new TestWorld(); PowerGauge gauge = new PowerGauge(); gauge.setWorld(world);
        set(gauge, "deltaTick", 5L); set(gauge, "deltaSecond", 100L); gauge.deltaLastSecond = 100;
        for (world.tick = 1; world.tick <= 40; world.tick++) gauge.update();
        ByteBuf packet = Unpooled.buffer();
        try { gauge.serialize(packet); assertEquals(0, packet.readLong()); assertEquals(0, packet.readLong()); } finally { packet.release(); }
    }
    @Test void disconnectedFluidGaugeClearsItsReadingsAndSecondBucket() throws Exception {
        TestWorld world = new TestWorld(); FluidGauge gauge = new FluidGauge(); gauge.setWorld(world);
        set(gauge, "deltaTick", 5L); set(gauge, "deltaSecond", 100L); set(gauge, "lastSecond", 100L);
        for (world.tick = 1; world.tick <= 40; world.tick++) gauge.update();
        Object[] reading = gauge.getTransfer(null, null); assertEquals(0L, reading[0]); assertEquals(0L, reading[1]);
    }
    @Test void powerGaugeSecondBucketSaturatesAndSynchronizes() throws Exception {
        TestWorld world = new TestWorld(); PowerGauge gauge = new PowerGauge(); gauge.setWorld(world);
        PowerNetMK2 net = new PowerNetMK2(); net.energyTracker = 10;
        Nodespace.PowerNode node = new Nodespace.PowerNode(BlockPos.ORIGIN); net.joinLink(node); set(gauge, "node", node);
        set(gauge, "deltaSecond", Long.MAX_VALUE - 5); world.tick = 1; gauge.update(); world.tick = 20; gauge.update();
        assertEquals(Long.MAX_VALUE, gauge.deltaLastSecond);
        ByteBuf packet = Unpooled.buffer();
        try { gauge.serialize(packet); PowerGauge client = new PowerGauge(); client.deserialize(packet); assertEquals(Long.MAX_VALUE, client.deltaLastSecond); } finally { packet.release(); }
    }
    @Test void fluidGaugeSecondBucketSaturates() throws Exception {
        TestWorld world = new TestWorld(); FluidGauge gauge = new FluidGauge(); gauge.setWorld(world);
        FluidNetMK2 net = new FluidNetMK2(Fluids.WATER); net.fluidTracker = 10;
        FluidNode node = new FluidNode(Fluids.WATER.getNetworkProvider(), BlockPos.ORIGIN); net.joinLink(node);
        set(gauge, "node", node); set(gauge, "type", Fluids.WATER); set(gauge, "deltaSecond", Long.MAX_VALUE - 5);
        world.tick = 1; gauge.update(); world.tick = 20; gauge.update();
        Object[] reading = gauge.getTransfer(null, null); assertEquals(10L, reading[0]); assertEquals(Long.MAX_VALUE, reading[1]);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); f.set(target, value); return; } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static class PowerStore implements IEnergyProviderMK2, IEnergyReceiverMK2 {
        long power, received; final long max, speed; long acceptLimit = Long.MAX_VALUE; boolean creative;
        PowerStore(long power, long max, long speed) { this.power = power; this.max = max; this.speed = speed; }
        @Override public long getPower() { return power; }
        @Override public void setPower(long amount) { power = amount; }
        @Override public long getMaxPower() { return max; }
        @Override public long getProviderSpeed() { return speed; }
        @Override public long getReceiverSpeed() { return speed; }
        @Override public void usePower(long amount) { if (!creative) power -= amount; }
        @Override public long transferPower(long amount, boolean simulate) {
            long accepted = Math.min(amount, Math.min(acceptLimit, max - power));
            if (!simulate) { power += accepted; received += accepted; }
            return amount - accepted;
        }
        @Override public boolean isLoaded() { return true; }
    }
    private static class FluidStore implements IFluidProviderMK2, IFluidReceiverMK2 {
        final long[] stored = new long[6]; final int inputPressure, outputPressure;
        long received, acceptLimit = Long.MAX_VALUE; boolean creative;
        FluidStore(long fill, int input, int output) { stored[output] = fill; inputPressure = input; outputPressure = output; }
        @Override public long getFluidAvailable(FluidType type, int pressure) { return pressure == outputPressure ? stored[pressure] : 0; }
        @Override public void useUpFluid(FluidType type, int pressure, long amount) { if (!creative) stored[pressure] -= amount; }
        @Override public long getProviderSpeed(FluidType type, int pressure) { return 100; }
        @Override public long getDemand(FluidType type, int pressure) { return pressure == inputPressure ? 10000 - stored[pressure] : 0; }
        @Override public long getReceiverSpeed(FluidType type, int pressure) { return 100; }
        @Override public long transferFluid(FluidType type, int pressure, long amount) {
            long accepted = pressure == inputPressure ? Math.min(amount, acceptLimit) : 0;
            stored[pressure] += accepted; received += accepted; return amount - accepted;
        }
        @Override public int[] getProvidingPressureRange(FluidType type) { return new int[]{outputPressure, outputPressure}; }
        @Override public int[] getReceivingPressureRange(FluidType type) { return new int[]{inputPressure, inputPressure}; }
        @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[0]; }
        @Override public boolean isLoaded() { return true; }
    }
    private static class PowerGauge extends TileEntityCableGauge {
        @Override public boolean shouldCreateNode() { return false; }
        @Override public void networkPackNT(int range) { }
    }
    private static class FluidGauge extends TileEntityPipeGauge {
        @Override public boolean shouldCreateNode() { return false; }
        @Override public void networkPackNT(int range) { }
    }
    private static class TestWorld extends World {
        long tick;
        TestWorld() { super(new SaveHandlerMP(), new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), false); }
        @Override public long getTotalWorldTime() { return tick; }
        @Override protected IChunkProvider createChunkProvider() {
            return (IChunkProvider) Proxy.newProxyInstance(IChunkProvider.class.getClassLoader(), new Class<?>[]{IChunkProvider.class},
                    (proxy, method, args) -> method.getName().equals("isChunkGeneratedAt") ? Boolean.TRUE : null);
        }
        @Override public IChunkProvider getChunkProvider() { return createChunkProvider(); }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
        @Override public Entity getEntityByID(int id) { return null; }
        @Override public IBlockState getBlockState(BlockPos pos) { return Blocks.AIR.getDefaultState(); }
    }
}
