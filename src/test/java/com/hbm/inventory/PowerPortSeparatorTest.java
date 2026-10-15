package com.hbm.inventory;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.api.energymk2.PowerNetMK2;
import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TilePort;
import com.hbm.tileentity.TileEntityProxyPowerPort;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.network.TileEntityNetworkSeparator;
import com.hbm.uninos.UniNodespace;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Run in an isolated Java 8 process against the built, downgraded dev JAR.
class PowerPortSeparatorTest {
    private static LaunchClassLoader previousClassLoader;
    @BeforeAll static void bootstrap() throws Exception {
        previousClassLoader = Launch.classLoader;
        if (previousClassLoader == null) Launch.classLoader = new LaunchClassLoader(new URL[0]);
        AutoCrystallizerOreDictionaryTest.bootstrap();
        GameRegistry.registerTileEntity(TestSeparator.class, new ResourceLocation("hbm", "test_network_separator"));
    }
    @AfterAll static void cleanup() throws Exception {
        if (previousClassLoader == null && Launch.classLoader != null) {
            Launch.classLoader.close();
            Launch.classLoader = null;
        }
    }

    private TilePort port(TestWorld world, Object owner, int x, PowerNetMK2 net) {
        TilePort port = new TilePort().setupOwner(owner).setupType(Nodespace.THE_POWER_PROVIDER)
                .setupPort(PortDef.make(x, 64, 0, ForgeDirection.WEST, ForgeDirection.EAST));
        port.update(world);
        net.joinLink(Nodespace.getNode(world, new BlockPos(x, 64, 0)));
        port.update(world);
        return port;
    }

    @Test void providerFactoryCreatesThePowerNodeRequiredByLegacyCables() {
        assertTrue(Nodespace.THE_POWER_PROVIDER.provideNode(BlockPos.ORIGIN) instanceof Nodespace.PowerNode);
    }

    @Test void adjacentMachinePortsJoinThroughTheActualNodespaceConnectionPass() throws Exception {
        TestWorld world = new TestWorld();
        Battery source = new Battery(1000), sink = new Battery(0);
        TilePort left = new TilePort().setupOwner(source).setupType(Nodespace.THE_POWER_PROVIDER)
                .setupPort(PortDef.make(0, 64, 0, ForgeDirection.EAST));
        TilePort right = new TilePort().setupOwner(sink).setupType(Nodespace.THE_POWER_PROVIDER)
                .setupPort(PortDef.make(1, 64, 0, ForgeDirection.WEST));
        left.update(world); right.update(world);
        // Dispatch the real per-world pass without registering this isolated world in DimensionManager.
        Field field = UniNodespace.class.getDeclaredField("managers");
        field.setAccessible(true);
        Object manager = ((Map<?, ?>) field.get(null)).get(Nodespace.THE_POWER_PROVIDER);
        Method pass = manager.getClass().getDeclaredMethod("updateForWorld", World.class);
        pass.setAccessible(true); pass.invoke(manager, world);
        left.update(world); left.checkProvide(world);
        right.update(world); right.checkSubscribe(world);
        assertNotNull(left.getNetwork());
        assertSame(left.getNetwork(), right.getNetwork());
        left.getNetwork().update();
        assertEquals(1000, sink.power);
        assertEquals(0, source.power);
    }

    @Test void portMembershipSurvivesLegacyTimeoutWithoutRefreshingTimestamps() {
        TestWorld world = new TestWorld();
        PowerNetMK2 net = new PowerNetMK2();
        Battery source = new Battery(1000), sink = new Battery(0);
        TilePort input = port(world, source, 0, net), output = port(world, sink, 1, net);
        input.checkProvide(world);
        output.checkSubscribe(world);
        net.providerEntries.put(source, 0);
        net.receiverEntries.put(sink, 0);
        net.update();
        assertEquals(1000, sink.power);
        assertEquals(0, source.power);
        assertEquals(0, net.providerEntries.getLong(source));
        assertEquals(0, net.receiverEntries.getLong(sink));
    }

    @Test void legacyMembershipStillExpires() {
        PowerNetMK2 net = new PowerNetMK2();
        Battery source = new Battery(1000), sink = new Battery(0);
        net.providerEntries.put(source, 0);
        net.receiverEntries.put(sink, 0);
        net.update();
        assertEquals(1000, source.power);
        assertEquals(0, sink.power);
    }

    @Test void removingOnePortRebindsTheSurvivingPortEvenOnTheSameNetwork() {
        TestWorld world = new TestWorld();
        PowerNetMK2 net = new PowerNetMK2();
        Battery owner = new Battery(0);
        TilePort first = port(world, owner, 0, net), second = port(world, owner, 1, net);
        first.checkSubscribe(world);
        second.checkSubscribe(world);
        first.disableIfPresent(world);
        assertTrue(net.receiverEntries.isEmpty());
        second.update(world);
        second.checkSubscribe(world);
        assertSame(net, second.getNetwork());
        assertTrue(net.isSubscribed(owner));
        second.disableIfPresent(world);
        assertFalse(net.isSubscribed(owner));
    }

    @Test void bridgeSplitRemovesCrossNetworkSubscriptionsAndMergeRestoresThem() {
        TestWorld world = new TestWorld();
        PowerNetMK2 net = new PowerNetMK2();
        Battery source = new Battery(1000), sink = new Battery(0);
        TilePort left = port(world, source, 0, net), right = port(world, sink, 2, net);
        Nodespace.PowerNode bridge = new Nodespace.PowerNode(new BlockPos(1, 64, 0)).setConnections(
                new DirPos(0, 64, 0, ForgeDirection.WEST), new DirPos(2, 64, 0, ForgeDirection.EAST));
        UniNodespace.createNode(world, bridge);
        net.joinLink(bridge);
        left.checkProvide(world);
        right.checkSubscribe(world);
        UniNodespace.destroyNode(world, bridge);
        left.update(world); left.checkProvide(world);
        right.update(world); right.checkSubscribe(world);
        PowerNetMK2 leftNet = (PowerNetMK2) left.getNetwork(), rightNet = (PowerNetMK2) right.getNetwork();
        assertNotSame(leftNet, rightNet);
        assertFalse(leftNet.isSubscribed(sink));
        assertFalse(rightNet.isProvider(source));
        leftNet.update(); rightNet.update();
        assertEquals(0, sink.power);
        leftNet.joinNetworks(rightNet);
        left.update(world); left.checkProvide(world);
        right.update(world); right.checkSubscribe(world);
        leftNet.update();
        assertEquals(1000, sink.power);
        assertEquals(0, source.power);
    }

    @Test void disabledPortDetachesAndCanBeEnabledAgain() {
        TestWorld world = new TestWorld();
        Battery owner = new Battery(0);
        PowerNetMK2 net = new PowerNetMK2();
        TilePort port = port(world, owner, 0, net);
        port.checkSubscribe(world);
        port.disable(); port.update(world);
        assertNull(Nodespace.getNode(world, new BlockPos(0, 64, 0)));
        assertFalse(net.isSubscribed(owner));
        port.enable(); port.update(world);
        assertNotNull(Nodespace.getNode(world, new BlockPos(0, 64, 0)));
    }

    private TestSeparator separator(TestWorld world, PowerNetMK2 input, PowerNetMK2 output) {
        TestSeparator separator = new TestSeparator();
        separator.setWorld(world);
        separator.setPos(new BlockPos(0, 64, 0));
        separator.update();
        input.joinLink(Nodespace.getNode(world, separator.getInPort()[0].portPositions[0]));
        output.joinLink(Nodespace.getNode(world, separator.getOutPort()[0].portPositions[0]));
        separator.update();
        return separator;
    }

    @Test void separatorTransfersOnlyForwardWithinItsLimitAndConservesEnergy() {
        TestWorld world = new TestWorld();
        PowerNetMK2 input = new PowerNetMK2(), output = new PowerNetMK2();
        TestSeparator separator = separator(world, input, output);
        Battery source = new Battery(5000), sink = new Battery(0);
        input.addProvider(source); output.addReceiver(sink);
        for (int tick = 0; tick < 4; tick++) {
            separator.update(); input.update(); output.update();
            assertEquals(5000, source.power + sink.power + separator.getPower());
        }
        assertEquals(4000, sink.power);
        assertEquals(1000, source.power);
        assertEquals(1000, separator.deltaTick[19]);
    }

    @Test void separatorCannotTransferFromOutputBackToInput() {
        TestWorld world = new TestWorld();
        PowerNetMK2 input = new PowerNetMK2(), output = new PowerNetMK2();
        TestSeparator separator = separator(world, input, output);
        Battery source = new Battery(5000), sink = new Battery(0);
        output.addProvider(source); input.addReceiver(sink);
        input.update(); output.update();
        assertEquals(5000, source.power);
        assertEquals(0, sink.power);
        assertEquals(0, separator.getPower());
        assertFalse(separator.allowDirectProvision());
    }

    @Test void legacyMachinesConnectThroughOnlyTheTwoOutwardProxyFaces() {
        for (int meta = 12; meta <= 15; meta++) {
            TestWorld world = new TestWorld();
            PowerNetMK2 input = new PowerNetMK2(), output = new PowerNetMK2();
            TestSeparator separator = new TestSeparator();
            separator.meta = meta;
            separator.setWorld(world); separator.setPos(new BlockPos(0, 64, 0)); separator.update();
            BlockPos in = separator.getInPort()[0].portPositions[0], out = separator.getOutPort()[0].portPositions[0];
            input.joinLink(Nodespace.getNode(world, in)); output.joinLink(Nodespace.getNode(world, out));
            separator.update();
            TestProxy inProxy = new TestProxy(separator), outProxy = new TestProxy(separator);
            inProxy.setWorld(world); inProxy.setPos(in); outProxy.setWorld(world); outProxy.setPos(out);
            world.tiles.put(in, inProxy); world.tiles.put(out, outProxy);
            ForgeDirection inward = separator.getDir(true), outward = separator.getDir(false);
            assertTrue(inProxy.canConnect(inward)); assertFalse(inProxy.canConnect(outward));
            assertTrue(outProxy.canConnect(outward)); assertFalse(outProxy.canConnect(inward));
            assertFalse(outProxy.canConnect(ForgeDirection.UP));
            Battery source = new Battery(5000), sink = new Battery(0);
            source.tryProvide(world, in, inward.getOpposite());
            sink.trySubscribe(world, out, outward.getOpposite());
            assertTrue(input.isProvider(source)); assertTrue(output.isSubscribed(sink));
            assertEquals(0, separator.getPower(), "Legacy direct provision must not bypass the port");
            separator.update(); // Recalculate demand after the legacy receiver subscribed.
            input.update(); output.update();
            assertEquals(1000, sink.power);
        }
    }

    @Test void redstoneBlocksBothDirectionsWithoutDeletingStoredPower() {
        TestWorld world = new TestWorld();
        PowerNetMK2 input = new PowerNetMK2(), output = new PowerNetMK2();
        TestSeparator separator = separator(world, input, output);
        Battery source = new Battery(5000), sink = new Battery(0);
        separator.setPower(500);
        input.addProvider(source); output.addReceiver(sink);
        world.powered = true;
        separator.update(); input.update(); output.update();
        assertTrue(separator.redstone);
        assertEquals(500, separator.getPower());
        assertEquals(0, sink.power);
        assertEquals(5000, source.power);
        world.powered = false;
        separator.update(); input.update(); output.update();
        assertTrue(sink.power > 0);
    }

    @Test void reducingTheLimitCannotDumpTheOldBufferAboveTheNewLimit() {
        TestWorld world = new TestWorld();
        PowerNetMK2 input = new PowerNetMK2(), output = new PowerNetMK2();
        TestSeparator separator = separator(world, input, output);
        Battery sink = new Battery(0);
        output.addReceiver(sink);
        separator.setPower(1000);
        separator.configuredLimit = 100;
        separator.update(); output.update();
        assertEquals(100, sink.power);
        assertEquals(900, separator.getPower());
        assertEquals(0, separator.getReceiverSpeed());
    }

    @Test void settingsAndStoredEnergySurviveNbtAndClientSync() {
        TestSeparator original = new TestSeparator();
        original.configuredLimit = 8_000_000_000L;
        original.priority = IEnergyReceiverMK2.ConnectionPriority.HIGH;
        original.setPower(750);
        TestSeparator loaded = new TestSeparator();
        loaded.readFromNBT(original.writeToNBT(new NBTTagCompound()));
        assertEquals(original.configuredLimit, loaded.configuredLimit);
        assertEquals(original.priority, loaded.priority);
        assertEquals(750, loaded.getPower());
        ByteBuf buf = Unpooled.buffer();
        try {
            original.deltaTick[19] = 123; original.deltaSecond = 456; original.redstone = true;
            original.serialize(buf); loaded.deserialize(buf);
            assertEquals(0, buf.readableBytes());
            assertEquals(123, loaded.deltaTick[19]);
            assertEquals(456, loaded.deltaSecond);
            assertTrue(loaded.redstone);
        } finally { buf.release(); }
    }

    @Test void guiAndRadioAcceptLongLimitsAndEnforceMaximum() {
        TestSeparator separator = new TestSeparator();
        NBTTagCompound control = new NBTTagCompound();
        control.setLong("limit", Long.MAX_VALUE);
        separator.receiveControl(control);
        assertEquals(TileEntityNetworkSeparator.MAX_LIMIT, separator.configuredLimit);
        control.setLong("limit", -1);
        separator.receiveControl(control);
        assertEquals(0, separator.configuredLimit);
        separator.runRORFunction(IRORInfo.PREFIX_FUNCTION + "setlimit", new String[] {"9000000000"});
        assertEquals(9_000_000_000L, separator.configuredLimit);
    }

    @Test void chunkUnloadDestroysBothMachineOwnedNodes() {
        TestWorld world = new TestWorld();
        PowerNetMK2 input = new PowerNetMK2(), output = new PowerNetMK2();
        TestSeparator separator = separator(world, input, output);
        separator.onChunkUnload();
        assertNull(Nodespace.getNode(world, separator.getInPort()[0].portPositions[0]));
        assertNull(Nodespace.getNode(world, separator.getOutPort()[0].portPositions[0]));
        assertFalse(input.isSubscribed(separator));
        assertFalse(output.isProvider(separator));
    }

    public static class TestSeparator extends TileEntityNetworkSeparator {
        int meta = 12;
        @Override public int getBlockMetadata() { return meta; }
        @Override public void networkPackNT(int range) { }
        @Override public void markChanged() { }
        @Override public void markDirty() { }
    }
    private static class TestProxy extends TileEntityProxyPowerPort {
        private final TileEntity core;
        TestProxy(TileEntity core) { this.core = core; }
        @Override public TileEntity getTile() { return core; }
    }
    private static final class Battery implements IEnergyProviderMK2, IEnergyReceiverMK2 {
        private long power;
        Battery(long power) { this.power = power; }
        @Override public long getPower() { return power; }
        @Override public void setPower(long power) { this.power = power; }
        @Override public long getMaxPower() { return 10_000; }
        @Override public boolean isLoaded() { return true; }
    }
    private static final class TestWorld extends World {
        boolean powered;
        final Map<BlockPos, TileEntity> tiles = new HashMap<>();
        TestWorld() { super(new SaveHandlerMP(), new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), false); }
        @Override protected IChunkProvider createChunkProvider() {
            return (IChunkProvider) Proxy.newProxyInstance(IChunkProvider.class.getClassLoader(), new Class<?>[] {IChunkProvider.class},
                    (proxy, method, args) -> method.getName().equals("isChunkGeneratedAt") ? Boolean.TRUE : null);
        }
        @Override public IChunkProvider getChunkProvider() { return createChunkProvider(); }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
        @Override public Entity getEntityByID(int id) { return null; }
        @Override public boolean isBlockPowered(BlockPos pos) { return powered; }
        @Override public TileEntity getTileEntity(BlockPos pos) { return tiles.get(pos); }
    }
}
