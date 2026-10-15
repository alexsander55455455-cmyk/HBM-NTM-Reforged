package com.hbm.inventory;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.FluidNetMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.render.misc.EnumSymbol;
import com.hbm.tileentity.network.*;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.uninos.GenNode;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.PneumaticNetwork;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Run in an isolated Java 8 process, including against the final downgraded artifact.
class PipeAnchorTest {
    @BeforeAll static void bootstrap() throws Exception {
        if (Launch.classLoader == null) Launch.classLoader = new LaunchClassLoader(new URL[0]);
        AutoCrystallizerOreDictionaryTest.bootstrap();
        if (Fluids.NONE == null) Fluids.NONE = fluid("NONE");
        if (Fluids.WATER == null) Fluids.WATER = fluid("WATER");
        if (Fluids.OIL == null) Fluids.OIL = fluid("OIL");
        if (Fluids.AIR == null) Fluids.AIR = fluid("AIR");
        if (Fluids.SMOKE == null) Fluids.SMOKE = fluid("SMOKE");
        if (Fluids.SMOKE_LEADED == null) Fluids.SMOKE_LEADED = fluid("SMOKE_LEADED");
        if (Fluids.SMOKE_POISON == null) Fluids.SMOKE_POISON = fluid("SMOKE_POISON");
        GameRegistry.registerTileEntity(Exhaust.class, new ResourceLocation("hbm", "test_exhaust_anchor"));
        GameRegistry.registerTileEntity(Pneumatic.class, new ResourceLocation("hbm", "test_pneumatic_anchor"));
        GameRegistry.registerTileEntity(Fluid.class, new ResourceLocation("hbm", "test_fluid_anchor"));
    }
    private static FluidType fluid(String name) { return new FluidType(name, 0x888888, 0, 0, 0, EnumSymbol.NONE); }
    private <T extends TileEntity> T put(TestWorld world, T tile, int x) {
        tile.setWorld(world); tile.setPos(new BlockPos(x, 64, 0)); world.tiles.put(tile.getPos(), tile); return tile;
    }
    private void link(TileEntityPipelineBase a, TileEntityPipelineBase b) {
        assertEquals(0, TileEntityPipelineBase.canConnect(a, b));
        a.addConnection(b.getPos().getX(), 64, 0); b.addConnection(a.getPos().getX(), 64, 0);
    }
    private GenNode<?> node(TestWorld world, int x, INetworkProvider provider) {
        return UniNodespace.getNode(world, new BlockPos(x, 64, 0), provider);
    }
    private void connect(TestWorld world, INetworkProvider provider) throws Exception {
        Field field = UniNodespace.class.getDeclaredField("managers"); field.setAccessible(true);
        Object manager = ((Map<?, ?>) field.get(null)).get(provider);
        Method pass = manager.getClass().getDeclaredMethod("updateForWorld", World.class); pass.setAccessible(true);
        pass.invoke(manager, world); pass.invoke(manager, world);
    }
    @Test void distinctNetworksCannotLinkOrChangeFluidType() {
        TestWorld world = new TestWorld(); Fluid fluid = put(world, new Fluid(), 0);
        Exhaust exhaust = put(world, new Exhaust(), 1); Pneumatic pneumatic = put(world, new Pneumatic(), 2);
        assertEquals(1, TileEntityPipelineBase.canConnect(fluid, exhaust));
        assertEquals(1, TileEntityPipelineBase.canConnect(exhaust, pneumatic));
        assertEquals(1, TileEntityPipelineBase.canConnect(pneumatic, fluid));
        exhaust.setType(Fluids.WATER); pneumatic.setType(Fluids.OIL);
        assertSame(Fluids.NONE, exhaust.getType()); assertSame(Fluids.NONE, pneumatic.getType());
    }
    @Test void originalFluidTypeAdoptionAndMismatchRemain() {
        TestWorld world = new TestWorld(); Fluid a = put(world, new Fluid(), 0), b = put(world, new Fluid(), 1);
        a.setType(Fluids.WATER); assertEquals(0, TileEntityPipelineBase.canConnect(a, b)); assertSame(Fluids.WATER, b.getType());
        b.setType(Fluids.OIL); assertEquals(4, TileEntityPipelineBase.canConnect(a, b));
    }
    @Test void selfAndTenMetreLimitApplyToAllNetworks() {
        TestWorld world = new TestWorld();
        for (TileEntityPipelineBase a : new TileEntityPipelineBase[]{new Fluid(), new Exhaust(), new Pneumatic()}) {
            put(world, a, 0); assertEquals(2, TileEntityPipelineBase.canConnect(a, a));
            TileEntityPipelineBase b = a instanceof Pneumatic ? new Pneumatic() : a instanceof Exhaust ? new Exhaust() : new Fluid();
            put(world, b, 10); assertEquals(0, TileEntityPipelineBase.canConnect(a, b));
            b.setPos(new BlockPos(11, 64, 0)); assertEquals(3, TileEntityPipelineBase.canConnect(a, b));
        }
    }
    @Test void rangeUsesActualMountPointsRatherThanBlockOrigins() {
        TestWorld world = new TestWorld();
        Fluid a = put(world, new Fluid() { @Override public Vec3d getMountPos() { return new Vec3d(1.5, 0.5, 0.5); } }, 0);
        Fluid b = put(world, new Fluid(), 11);
        assertEquals(0, TileEntityPipelineBase.canConnect(a, b));
        b.setPos(new BlockPos(12, 64, 0)); assertEquals(3, TileEntityPipelineBase.canConnect(a, b));
    }
    @Test void mountedSideOnlyForEveryOrientation() {
        Exhaust exhaust = new Exhaust(); Pneumatic pneumatic = new Pneumatic();
        for (int meta = 0; meta < 6; meta++) {
            exhaust.meta = meta; pneumatic.meta = meta;
            for (ForgeDirection side : ForgeDirection.VALUES) {
                boolean mounted = side == ForgeDirection.getOrientation(meta).getOpposite();
                assertEquals(mounted, exhaust.canConnect(Fluids.SMOKE, side));
                assertEquals(mounted, pneumatic.canConnectPneumatic(side));
                assertFalse(pneumatic.canConnect(Fluids.SMOKE, side));
                assertFalse(exhaust.canConnect(Fluids.WATER, side));
            }
        }
    }
    @Test void exhaustCreatesThreeSeparateNetworksAndNoNoneNode() throws Exception {
        TestWorld world = new TestWorld(); Exhaust a = put(world, new Exhaust(), 0), b = put(world, new Exhaust(), 8);
        link(a, b); a.update(); b.update();
        for (FluidType smoke : new FluidType[]{Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON}) {
            connect(world, smoke.getNetworkProvider()); assertSame(node(world, 0, smoke.getNetworkProvider()).net, node(world, 8, smoke.getNetworkProvider()).net);
        }
        assertNotSame(node(world, 0, Fluids.SMOKE.getNetworkProvider()).net, node(world, 0, Fluids.SMOKE_LEADED.getNetworkProvider()).net);
        assertNull(node(world, 0, Fluids.NONE.getNetworkProvider()));
    }
    @Test void pneumaticUsesExistingTypedNodesWithoutAFluidNetwork() throws Exception {
        TestWorld world = new TestWorld(); Pneumatic a = put(world, new Pneumatic(), 0), b = put(world, new Pneumatic(), 8);
        link(a, b); a.update(); b.update(); connect(world, PneumaticNetwork.THE_PNEUMATIC_PROVIDER);
        GenNode<?> first = node(world, 0, PneumaticNetwork.THE_PNEUMATIC_PROVIDER);
        assertTrue(first instanceof TileEntityPneumoTube.PneumaticNode);
        assertSame(first.net, node(world, 8, PneumaticNetwork.THE_PNEUMATIC_PROVIDER).net);
        assertNull(node(world, 0, Fluids.NONE.getNetworkProvider()));
    }
    @Test void mountAdjacencyHasNoConnectionOnOtherSides() throws Exception {
        TestWorld world = new TestWorld(); Pneumatic a = put(world, new Pneumatic(), 0); a.update();
        GenNode<?> anchor = node(world, 0, PneumaticNetwork.THE_PNEUMATIC_PROVIDER);
        assertEquals(2, anchor.connections.length);
        assertEquals(new BlockPos(0, 63, 0), anchor.connections[1].getPos());
        assertSame(ForgeDirection.DOWN, anchor.connections[1].getDir());
    }
    @Test void duplicateWrenchLinksDoNotAccumulate() {
        TestWorld world = new TestWorld(); Pneumatic a = put(world, new Pneumatic(), 0), b = put(world, new Pneumatic(), 8);
        link(a, b); link(a, b); assertEquals(1, a.getConnected().size()); assertEquals(1, b.getConnected().size());
    }
    @Test void exhaustDisconnectRemovesAllThreeNodesAndReciprocalLink() {
        TestWorld world = new TestWorld(); Exhaust a = put(world, new Exhaust(), 0), b = put(world, new Exhaust(), 8);
        link(a, b); a.invalidate();
        assertTrue(b.getConnected().isEmpty());
        for (FluidType smoke : new FluidType[]{Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON}) {
            assertNull(node(world, 0, smoke.getNetworkProvider())); assertNull(node(world, 8, smoke.getNetworkProvider()));
        }
        b.update(); assertNotNull(node(world, 8, Fluids.SMOKE.getNetworkProvider()));
    }
    @Test void pneumaticDisconnectRecreatesPeerWithoutStaleLink() {
        TestWorld world = new TestWorld(); Pneumatic a = put(world, new Pneumatic(), 0), b = put(world, new Pneumatic(), 8);
        link(a, b); a.invalidate(); assertTrue(b.getConnected().isEmpty());
        assertNull(node(world, 0, PneumaticNetwork.THE_PNEUMATIC_PROVIDER));
        b.update(); assertEquals(2, node(world, 8, PneumaticNetwork.THE_PNEUMATIC_PROVIDER).connections.length);
    }
    @Test void saveAndSyncPreserveLegacyCoordinatesAndLinks() {
        TestWorld world = new TestWorld(); Pneumatic a = put(world, new Pneumatic(), 0), b = put(world, new Pneumatic(), 8); link(a, b);
        NBTTagCompound tag = a.writeToNBT(new NBTTagCompound());
        assertEquals(1, tag.getInteger("conCount")); assertArrayEquals(new int[]{8, 64, 0}, tag.getIntArray("con0"));
        Pneumatic restored = new Pneumatic(); restored.readFromNBT(tag); assertArrayEquals(a.getConnected().get(0), restored.getConnected().get(0));
        ByteBuf buf = Unpooled.buffer();
        try { a.serializeInitial(buf); restored.deserializeInitial(buf); assertEquals(0, buf.readableBytes()); assertEquals(1, restored.getConnected().size()); }
        finally { buf.release(); }
    }
    @Test void renderBoundsFollowLinkAndShrinkAfterRemoval() {
        TestWorld world = new TestWorld(); Pneumatic a = put(world, new Pneumatic(), 0), b = put(world, new Pneumatic(), 8);
        assertEquals(1D, a.getRenderBoundingBox().maxX); link(a, b); assertEquals(9D, a.getRenderBoundingBox().maxX);
        b.invalidate(); assertEquals(1D, a.getRenderBoundingBox().maxX);
    }
    @Test void newBlockFactoriesCreateCorrectTilesAndKeepSixOrientations() {
        for (int meta = 0; meta < 6; meta++) {
            IBlockState exhaust = ModBlocks.pipe_anchor_exhaust.getStateFromMeta(meta), pneumatic = ModBlocks.pipe_anchor_pneumatic.getStateFromMeta(meta);
            assertEquals(meta, ModBlocks.pipe_anchor_exhaust.getMetaFromState(exhaust));
            assertEquals(meta, ModBlocks.pipe_anchor_pneumatic.getMetaFromState(pneumatic));
            assertTrue(ModBlocks.pipe_anchor_exhaust.createTileEntity(null, exhaust) instanceof TileEntityPipeExhaustAnchor);
            assertTrue(ModBlocks.pipe_anchor_pneumatic.createTileEntity(null, pneumatic) instanceof TileEntityPneumaticPipeAnchor);
        }
    }
    @Test void realExhaustPipesAndAnchorsTransferAllThreeSmokesSeparately() throws Exception {
        TestWorld world = new TestWorld(); Exhaust a = put(world, new Exhaust(), 0), b = put(world, new Exhaust(), 8); link(a, b);
        TileEntityPipeExhaust left = put(world, new TileEntityPipeExhaust(), 0), right = put(world, new TileEntityPipeExhaust(), 8);
        left.setPos(new BlockPos(0, 63, 0)); right.setPos(new BlockPos(8, 63, 0)); left.update(); right.update();
        for (FluidType smoke : new FluidType[]{Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON}) {
            connect(world, smoke.getNetworkProvider());
            FluidNode first = (FluidNode) node(world, 0, smoke.getNetworkProvider());
            assertSame(first.net, UniNodespace.getNode(world, left.getPos(), smoke.getNetworkProvider()).net);
            assertSame(first.net, UniNodespace.getNode(world, right.getPos(), smoke.getNetworkProvider()).net);
            FluidStore source = put(world, new FluidStore(smoke, 777), -1), sink = put(world, new FluidStore(smoke, 0), 9);
            FluidNetMK2 net = first.net; net.attachProviderPort(source); net.attachReceiverPort(sink); net.update();
            assertEquals(0, source.tanks[0].getFill()); assertEquals(777, sink.tanks[0].getFill());
        }
    }
    @Test void realPneumaticNetworkTransfersItemsAcrossAnchors() throws Exception {
        TestWorld world = new TestWorld(); Pneumatic a = put(world, new Pneumatic(), 0), b = put(world, new Pneumatic(), 8); link(a, b);
        for (int x : new int[]{0, 8}) {
            TileEntityPneumoTube.PneumaticNode tube = new TileEntityPneumoTube.PneumaticNode(new BlockPos(x, 63, 0))
                    .setConnections(new DirPos(x, 64, 0, ForgeDirection.UP));
            UniNodespace.createNode(world, tube);
        }
        connect(world, PneumaticNetwork.THE_PNEUMATIC_PROVIDER);
        Object net = node(world, 0, PneumaticNetwork.THE_PNEUMATIC_PROVIDER).net;
        Chest source = put(world, new Chest(), -1), sink = put(world, new Chest(), 9);
        source.setInventorySlotContents(0, new ItemStack(Items.IRON_INGOT, 64));
        TileEntityPneumoTube sender = put(world, new TileEntityPneumoTube(), 0);
        Class<?> receiver = Class.forName("com.hbm.uninos.networkproviders.PneumaticNetwork$ReceiverTarget");
        Object target = receiver.getConstructor(BlockPos.class, ForgeDirection.class, TileEntityPneumoTube.class)
                .newInstance(sink.getPos(), ForgeDirection.EAST, null);
        net.getClass().getMethod("addReceiver", receiver).invoke(net, target);
        boolean moved = (Boolean) net.getClass().getMethod("send", TileEntity.class, TileEntityPneumoTube.class, ForgeDirection.class,
                int.class, int.class, int.class, int.class).invoke(net, source, sender, ForgeDirection.EAST, 0, 0, 10, 0);
        assertTrue(moved); assertTrue(source.getStackInSlot(0).isEmpty()); assertEquals(64, sink.getStackInSlot(0).getCount());
    }
    private static class FluidStore extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2 {
        final FluidTankNTM[] tanks;
        FluidStore(FluidType type, int fill) { tanks = new FluidTankNTM[]{new FluidTankNTM(type, 1000)}; tanks[0].setFill(fill); }
        @Override public FluidTankNTM[] getSendingTanks() { return tanks; }
        @Override public FluidTankNTM[] getReceivingTanks() { return tanks; }
        @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    }
    private static class Chest extends TileEntityChest { @Override public void markDirty() {} }
    public static class Exhaust extends TileEntityPipeExhaustAnchor { int meta = 1; @Override public int getBlockMetadata() { return meta; } @Override public void markDirty() {} }
    public static class Pneumatic extends TileEntityPneumaticPipeAnchor { int meta = 1; @Override public int getBlockMetadata() { return meta; } @Override public void markDirty() {} }
    public static class Fluid extends TileEntityPipeAnchor { @Override public int getBlockMetadata() { return 1; } @Override public void markDirty() {} }
    private static class TestWorld extends World {
        final Map<BlockPos, TileEntity> tiles = new HashMap<>();
        TestWorld() { super(new SaveHandlerMP(), new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), false); }
        @Override protected IChunkProvider createChunkProvider() {
            return (IChunkProvider) Proxy.newProxyInstance(IChunkProvider.class.getClassLoader(), new Class<?>[]{IChunkProvider.class},
                    (proxy, method, args) -> method.getName().equals("isChunkGeneratedAt") ? Boolean.TRUE : null);
        }
        @Override public IChunkProvider getChunkProvider() { return createChunkProvider(); }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
        @Override public Entity getEntityByID(int id) { return null; }
        @Override public IBlockState getBlockState(BlockPos pos) { return Blocks.AIR.getDefaultState(); }
        @Override public TileEntity getTileEntity(BlockPos pos) { return tiles.get(pos); }
    }
}
