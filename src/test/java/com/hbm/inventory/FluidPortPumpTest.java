package com.hbm.inventory;

import com.hbm.api.energymk2.IEnergyReceiverMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.FluidNetMK2;
import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;
import com.hbm.render.misc.EnumSymbol;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.network.TileEntityFlowControlPump;
import com.hbm.uninos.UniNodespace;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Isolated Java 8 process; run again against the final downgraded dev JAR.
class FluidPortPumpTest {
    private static LaunchClassLoader previousClassLoader;
    @BeforeAll static void bootstrap() throws Exception {
        previousClassLoader = Launch.classLoader;
        if (previousClassLoader == null) Launch.classLoader = new LaunchClassLoader(new URL[0]);
        AutoCrystallizerOreDictionaryTest.bootstrap();
        if (Fluids.NONE == null) Fluids.NONE = new FluidType("NONE", 0x888888, 0, 0, 0, EnumSymbol.NONE);
        if (Fluids.WATER == null) Fluids.WATER = new FluidType("WATER", 0x3333ff, 0, 0, 0, EnumSymbol.NONE);
        if (Fluids.OIL == null) Fluids.OIL = new FluidType("OIL", 0x222222, 0, 0, 0, EnumSymbol.NONE);
        GameRegistry.registerTileEntity(TestPump.class, new ResourceLocation("hbm", "test_flow_control_pump"));
    }
    @AfterAll static void cleanup() throws Exception {
        if (previousClassLoader == null && Launch.classLoader != null) {
            Launch.classLoader.close();
            Launch.classLoader = null;
        }
    }
    private FluidNode node(TestWorld world, BlockPos pos, FluidType type) {
        return UniNodespace.getNode(world, pos, type.getNetworkProvider());
    }
    private PortDef[] shape(int x) { return new PortDef[] { PortDef.make(x, 64, 0, ForgeDirection.WEST, ForgeDirection.EAST) }; }
    private FluidMachine machine(TestWorld world, int fill) {
        FluidMachine machine = new FluidMachine();
        machine.setWorld(world);
        machine.tanks[0].setFill(fill);
        return machine;
    }
    private TestPump pump(TestWorld world, FluidNetMK2 input, FluidNetMK2 output) {
        TestPump pump = new TestPump();
        pump.setWorld(world); pump.setPos(new BlockPos(0, 64, 0));
        pump.tank[0].setTankType(Fluids.WATER);
        pump.update();
        input.joinLink(node(world, pump.getInPorts()[0].portPositions[0], Fluids.WATER));
        output.joinLink(node(world, pump.getOutPorts()[0].portPositions[0], Fluids.WATER));
        pump.update();
        return pump;
    }
    private void connectPass(TestWorld world, FluidType type) throws Exception {
        Field field = UniNodespace.class.getDeclaredField("managers"); field.setAccessible(true);
        Object manager = ((Map<?, ?>) field.get(null)).get(type.getNetworkProvider());
        Method pass = manager.getClass().getDeclaredMethod("updateForWorld", World.class); pass.setAccessible(true);
        pass.invoke(manager, world);
    }

    @Test void factoryCreatesLegacyCompatibleFluidNode() {
        assertTrue(Fluids.WATER.getNetworkProvider().provideNode(BlockPos.ORIGIN) instanceof FluidNode);
    }
    @Test void fluidPortsConnectThroughTheRealNodespacePass() throws Exception {
        TestWorld world = new TestWorld();
        FluidMachine source = machine(world, 500), sink = machine(world, 0);
        ModulePortManFluidAdaptive out = new ModulePortManFluidAdaptive(source).setOutputTanks(source.tanks);
        ModulePortManFluidAdaptive in = new ModulePortManFluidAdaptive(sink).setInputTanks(sink.tanks);
        PortDef[] left = shape(0), right = shape(1);
        out.update(left); in.update(right); connectPass(world, Fluids.WATER); out.update(left); in.update(right);
        FluidNode a = node(world, new BlockPos(0, 64, 0), Fluids.WATER), b = node(world, new BlockPos(1, 64, 0), Fluids.WATER);
        assertSame(a.net, b.net);
        a.net.update();
        assertEquals(500, sink.tanks[0].getFill()); assertEquals(0, source.tanks[0].getFill());
    }
    @Test void permanentPortSubscriptionsDoNotExpireButLegacyOnesStillDo() {
        TestWorld world = new TestWorld();
        FluidMachine source = machine(world, 500), sink = machine(world, 0);
        ModulePortManFluidAdaptive out = new ModulePortManFluidAdaptive(source).setOutputTanks(source.tanks);
        ModulePortManFluidAdaptive in = new ModulePortManFluidAdaptive(sink).setInputTanks(sink.tanks);
        PortDef[] left = shape(0), right = shape(2);
        out.update(left); in.update(right);
        FluidNetMK2 net = new FluidNetMK2(Fluids.WATER);
        net.joinLink(node(world, new BlockPos(0, 64, 0), Fluids.WATER));
        net.joinLink(node(world, new BlockPos(2, 64, 0), Fluids.WATER));
        out.update(left); in.update(right);
        FluidMachine expired = machine(world, 999);
        net.providerEntries.put(source, 0); net.receiverEntries.put(sink, 0); net.providerEntries.put(expired, 0);
        net.update();
        assertEquals(500, sink.tanks[0].getFill()); assertFalse(net.isProvider(expired));
        assertEquals(999, expired.tanks[0].getFill());
    }
    @Test void duplicateTankTypesAndSharedInputOutputUseOneNode() {
        TestWorld world = new TestWorld(); FluidMachine owner = machine(world, 0);
        FluidTankNTM second = new FluidTankNTM(Fluids.WATER, 100);
        ModulePortManFluidAdaptive manager = new ModulePortManFluidAdaptive(owner).setInputTanks(owner.tanks[0], second).setOutputTanks(owner.tanks);
        PortDef[] defs = shape(0); manager.update(defs);
        FluidNode node = node(world, new BlockPos(0, 64, 0), Fluids.WATER);
        FluidNetMK2 net = new FluidNetMK2(Fluids.WATER); net.joinLink(node); manager.update(defs);
        assertSame(node, node(world, new BlockPos(0, 64, 0), Fluids.WATER));
        assertTrue(net.isProvider(owner)); assertTrue(net.isSubscribed(owner));
        manager.toggleInput(owner.tanks[0], false); manager.update(defs);
        assertTrue(net.isSubscribed(owner));
        manager.toggleInput(second, false); manager.update(defs);
        assertFalse(net.isSubscribed(owner)); assertTrue(net.isProvider(owner));
        manager.destroy(); assertFalse(net.isProvider(owner));
        assertNull(node(world, new BlockPos(0, 64, 0), Fluids.WATER));
    }
    @Test void adaptivePortsFollowFluidChangesAndDisableNone() {
        TestWorld world = new TestWorld(); FluidMachine owner = machine(world, 0);
        ModulePortManFluidAdaptive manager = new ModulePortManFluidAdaptive(owner).setInputTanks(owner.tanks);
        PortDef[] defs = shape(0); manager.update(defs);
        FluidNode water = node(world, new BlockPos(0, 64, 0), Fluids.WATER);
        owner.tanks[0].setTankType(Fluids.OIL); manager.update(defs);
        assertTrue(water.expired); assertNull(node(world, new BlockPos(0, 64, 0), Fluids.WATER));
        assertNotNull(node(world, new BlockPos(0, 64, 0), Fluids.OIL));
        owner.tanks[0].setTankType(Fluids.NONE); manager.update(defs);
        assertNull(node(world, new BlockPos(0, 64, 0), Fluids.OIL));
        assertNull(node(world, new BlockPos(0, 64, 0), Fluids.NONE));
    }
    @Test void togglingAndChangingShapeDetachesOldPorts() {
        TestWorld world = new TestWorld(); FluidMachine owner = machine(world, 0);
        ModulePortManFluidAdaptive manager = new ModulePortManFluidAdaptive(owner).setInputTanks(owner.tanks);
        PortDef[] defs = shape(0); manager.update(defs);
        manager.toggleInput(owner.tanks[0], false); manager.update(defs);
        assertNull(node(world, new BlockPos(0, 64, 0), Fluids.WATER));
        manager.toggleInput(owner.tanks[0], true); manager.update(defs);
        assertNotNull(node(world, new BlockPos(0, 64, 0), Fluids.WATER));
        manager.update(shape(3));
        assertNull(node(world, new BlockPos(0, 64, 0), Fluids.WATER));
        assertNotNull(node(world, new BlockPos(3, 64, 0), Fluids.WATER));
    }
    @Test void pumpTransfersForwardAtConfiguredRateAndConservesFluid() {
        TestWorld world = new TestWorld();
        FluidNetMK2 input = new FluidNetMK2(Fluids.WATER), output = new FluidNetMK2(Fluids.WATER);
        TestPump pump = pump(world, input, output);
        FluidMachine source = machine(world, 5000), sink = machine(world, 0);
        input.addProvider(source); output.addReceiver(sink);
        for (int tick = 0; tick < 5; tick++) {
            pump.update(); input.update(); output.update();
            assertEquals(5000, source.tanks[0].getFill() + pump.tank[0].getFill() + sink.tanks[0].getFill());
        }
        assertEquals(500, sink.tanks[0].getFill());
    }
    @Test void pumpCannotTransferBackwards() {
        TestWorld world = new TestWorld();
        FluidNetMK2 input = new FluidNetMK2(Fluids.WATER), output = new FluidNetMK2(Fluids.WATER);
        TestPump pump = pump(world, input, output);
        FluidMachine source = machine(world, 500), sink = machine(world, 0);
        output.addProvider(source); input.addReceiver(sink); input.update(); output.update();
        assertEquals(500, source.tanks[0].getFill()); assertEquals(0, sink.tanks[0].getFill()); assertEquals(0, pump.tank[0].getFill());
    }
    @Test void lowRatePumpKeepsFlowingWithTheAddonsLegacyExpiryCheck() {
        TestWorld world = new TestWorld();
        LegacyExpiryFluidNet input = new LegacyExpiryFluidNet(), output = new LegacyExpiryFluidNet();
        TestPump pump = pump(world, input, output);
        pump.bufferSize = 10;
        FluidMachine source = machine(world, 5000), sink = machine(world, 0);
        for (int tick = 0; tick < 200; tick++) {
            input.providerEntries.put(source, input.started + tick * 50L);
            output.receiverEntries.put(sink, output.started + tick * 50L);
            pump.update(); input.tickAt(input.started + tick * 50L); output.tickAt(output.started + tick * 50L);
            assertEquals((tick + 1) * 10, sink.tanks[0].getFill(), "Flow stopped on tick " + tick);
            assertEquals(5000, source.tanks[0].getFill() + pump.tank[0].getFill() + sink.tanks[0].getFill());
        }
    }
    @Test void persistentEntriesSurviveLegacyRefreshAndDetachOnlyAfterTheLastPort() {
        FluidMachine owner = machine(new TestWorld(), 0);
        FluidNetMK2 net = new FluidNetMK2(Fluids.WATER);
        net.attachProviderPort(owner); net.attachProviderPort(owner);
        net.attachReceiverPort(owner); net.attachReceiverPort(owner);
        net.addProvider(owner); net.addReceiver(owner);
        long future = System.currentTimeMillis() + 10000;
        assertFalse(future - net.providerEntries.getLong(owner) > 3000);
        assertFalse(future - net.receiverEntries.getLong(owner) > 3000);
        net.detachProviderPort(owner); net.detachReceiverPort(owner);
        assertTrue(net.isProvider(owner)); assertTrue(net.isSubscribed(owner));
        net.detachProviderPort(owner); net.detachReceiverPort(owner);
        assertFalse(net.isProvider(owner)); assertFalse(net.isSubscribed(owner));
        net.addProvider(owner); net.addReceiver(owner);
        assertTrue(future - net.providerEntries.getLong(owner) > 3000);
        assertTrue(future - net.receiverEntries.getLong(owner) > 3000);
    }
    @Test void legacyExpiryStillRemovesUnloadedAndInvalidPersistentOwners() {
        TestWorld world = new TestWorld();
        LegacyExpiryFluidNet net = new LegacyExpiryFluidNet();
        FluidMachine unloaded = machine(world, 100), invalid = machine(world, 100), stale = machine(world, 100);
        net.attachProviderPort(unloaded); net.attachProviderPort(invalid); net.addProvider(stale);
        unloaded.isLoaded = false; invalid.invalidate();
        net.tickAt(System.currentTimeMillis() + 10000);
        assertFalse(net.isProvider(unloaded)); assertFalse(net.isProvider(invalid)); assertFalse(net.isProvider(stale));
    }
    @Test void legacyProxyFacesAndDirectInsertionRespectDirectionForAllRotations() {
        for (int meta = 12; meta <= 15; meta++) {
            TestWorld world = new TestWorld(); TestPump pump = new TestPump(); pump.meta = meta;
            pump.setWorld(world); pump.setPos(new BlockPos(0, 64, 0)); pump.tank[0].setTankType(Fluids.WATER); pump.update();
            TestProxy input = new TestProxy(pump), output = new TestProxy(pump);
            input.setWorld(world); output.setWorld(world);
            input.setPos(pump.getInPorts()[0].portPositions[0]); output.setPos(pump.getOutPorts()[0].portPositions[0]);
            assertTrue(input.canConnect(Fluids.WATER, pump.getDir(true)));
            assertTrue(output.canConnect(Fluids.WATER, pump.getDir(false)));
            assertFalse(input.canConnect(Fluids.WATER, pump.getDir(false)));
            assertFalse(output.canConnect(Fluids.WATER, ForgeDirection.UP));
            assertEquals(0, output.getDemand(Fluids.WATER, 0));
            assertEquals(50, output.transferFluid(Fluids.WATER, 0, 50));
            assertEquals(0, input.transferFluid(Fluids.WATER, 0, 50));
            assertEquals(50, pump.tank[0].getFill());
        }
    }
    @Test void wrongFluidAndPressureAreRejected() {
        TestWorld world = new TestWorld(); TestPump pump = pump(world, new FluidNetMK2(Fluids.WATER), new FluidNetMK2(Fluids.WATER));
        assertEquals(50, pump.transferFluid(Fluids.OIL, 0, 50));
        assertEquals(50, pump.transferFluid(Fluids.WATER, 1, 50));
        assertEquals(0, pump.tank[0].getFill());
        pump.tank[0].withPressure(2);
        assertEquals(0, pump.transferFluid(Fluids.WATER, 2, 50));
    }
    @Test void redstoneAtCoreOrEitherPortStopsBothDirectionsAndRestarts() {
        TestWorld world = new TestWorld(); TestPump pump = pump(world, new FluidNetMK2(Fluids.WATER), new FluidNetMK2(Fluids.WATER));
        for (BlockPos powered : new BlockPos[] {pump.getPos(), pump.getInPorts()[0].portPositions[0], pump.getOutPorts()[0].portPositions[0]}) {
            world.powered.add(powered); pump.update();
            assertTrue(pump.redstone); assertEquals(0, pump.getDemand(Fluids.WATER, 0)); assertEquals(0, pump.getProviderSpeed(Fluids.WATER, 0));
            assertEquals(20, pump.transferFluid(Fluids.WATER, 0, 20));
            world.powered.clear(); pump.update(); assertFalse(pump.redstone);
        }
        assertEquals(100, pump.getDemand(Fluids.WATER, 0));
    }
    @Test void decreasingTheLimitKeepsBufferedFluidUntilDrained() {
        TestWorld world = new TestWorld();
        FluidNetMK2 in = new FluidNetMK2(Fluids.WATER), out = new FluidNetMK2(Fluids.WATER); TestPump pump = pump(world, in, out);
        pump.bufferSize = 1000; pump.update(); pump.transferFluid(Fluids.WATER, 0, 900);
        pump.bufferSize = 100; pump.update();
        assertEquals(900, pump.tank[0].getFill()); assertTrue(pump.bufferMismatch()); assertEquals(0, pump.getDemand(Fluids.WATER, 0));
        FluidMachine sink = machine(world, 0); out.addReceiver(sink); out.update(); pump.update();
        assertEquals(900, sink.tanks[0].getFill()); assertEquals(0, pump.tank[0].getFill()); assertEquals(100, pump.tank[0].getMaxFill());
    }
    @Test void controlsClampFlowAndKeepPressureAndPriority() {
        TestPump pump = new TestPump(); NBTTagCompound control = new NBTTagCompound();
        control.setInteger("capacity", Integer.MAX_VALUE); control.setByte("pressure", (byte) 9); control.setByte("priority", (byte) 4);
        pump.receiveControl(control);
        assertEquals(10000, pump.bufferSize); assertEquals(5, pump.tank[0].getPressure()); assertEquals(ConnectionPriority.HIGHEST, pump.priority);
        control.setInteger("capacity", -1); control.setByte("pressure", (byte) -1); pump.receiveControl(control);
        assertEquals(0, pump.bufferSize); assertEquals(0, pump.tank[0].getPressure());
    }
    @Test void nbtAndClientSyncKeepFluidSettingsAndAnimationState() {
        TestPump pump = new TestPump(); pump.tank[0].setTankType(Fluids.WATER); pump.tank[0].withPressure(3);
        pump.tank[0].changeTankSize(1000); pump.tank[0].setFill(900); pump.bufferSize = 100; pump.priority = ConnectionPriority.HIGH;
        TestPump restored = new TestPump(); restored.readFromNBT(pump.writeToNBT(new NBTTagCompound()));
        assertSame(Fluids.WATER, restored.tank[0].getTankType()); assertEquals(900, restored.tank[0].getFill()); assertEquals(100, restored.bufferSize);
        assertEquals(1000, restored.tank[0].getMaxFill()); assertEquals(3, restored.tank[0].getPressure()); assertEquals(ConnectionPriority.HIGH, restored.priority);
        pump.redstone = true; pump.isWorking = true;
        ByteBuf buf = Unpooled.buffer();
        try {
            pump.serialize(buf); restored.deserialize(buf);
            assertEquals(900, restored.tank[0].getFill()); assertEquals(3, restored.tank[0].getPressure());
            assertTrue(restored.redstone); assertTrue(restored.isWorking); assertEquals(0, buf.readableBytes());
        } finally { buf.release(); }
    }
    @Test void unloadRemovesBothNodesAndSubscriptions() {
        TestWorld world = new TestWorld(); FluidNetMK2 in = new FluidNetMK2(Fluids.WATER), out = new FluidNetMK2(Fluids.WATER);
        TestPump pump = pump(world, in, out); pump.onChunkUnload();
        assertNull(node(world, pump.getInPorts()[0].portPositions[0], Fluids.WATER));
        assertNull(node(world, pump.getOutPorts()[0].portPositions[0], Fluids.WATER));
        assertFalse(in.isSubscribed(pump)); assertFalse(out.isProvider(pump));
    }

    // Cursed Addon v51 overwrites setupFluidProviders and uses this raw timestamp check.
    private static final class LegacyExpiryFluidNet extends FluidNetMK2 {
        final long started = System.currentTimeMillis();
        LegacyExpiryFluidNet() { super(Fluids.WATER); }
        @Override public void setupFluidProviders() {
            Iterator<Map.Entry<IFluidProviderMK2, Long>> it = providerEntries.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<IFluidProviderMK2, Long> entry = it.next();
                if (currentTime - entry.getValue() > timeout || isBadLink(entry.getKey())) it.remove();
            }
            super.setupFluidProviders();
        }
        void tickAt(long now) {
            currentTime = now;
            setupFluidProviders(); setupFluidReceivers(); transferFluid(); cleanUp();
        }
    }
    public static class TestPump extends TileEntityFlowControlPump {
        int meta = 12;
        @Override public int getBlockMetadata() { return meta; }
        @Override public void networkPackNT(int range) { }
        @Override public void markChanged() { }
        @Override public void markDirty() { }
    }
    private static final class TestProxy extends TileEntityProxyCombo {
        private final TileEntity core;
        TestProxy(TileEntity core) { this.core = core; fluid(); }
        @Override public TileEntity getTile() { return core; }
    }
    private static final class FluidMachine extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2 {
        final FluidTankNTM[] tanks = {new FluidTankNTM(Fluids.WATER, 10000)};
        @Override public FluidTankNTM[] getSendingTanks() { return tanks; }
        @Override public FluidTankNTM[] getReceivingTanks() { return tanks; }
        @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    }
    private static final class TestWorld extends World {
        final Set<BlockPos> powered = new HashSet<>();
        TestWorld() { super(new SaveHandlerMP(), new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), false); }
        @Override protected IChunkProvider createChunkProvider() {
            return (IChunkProvider) Proxy.newProxyInstance(IChunkProvider.class.getClassLoader(), new Class<?>[] {IChunkProvider.class},
                    (proxy, method, args) -> method.getName().equals("isChunkGeneratedAt") ? Boolean.TRUE : null);
        }
        @Override public IChunkProvider getChunkProvider() { return createChunkProvider(); }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
        @Override public Entity getEntityByID(int id) { return null; }
        @Override public boolean isBlockPowered(BlockPos pos) { return powered.contains(pos); }
        @Override public IBlockState getBlockState(BlockPos pos) { return Blocks.AIR.getDefaultState(); }
    }
}
