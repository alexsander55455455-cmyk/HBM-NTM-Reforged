package com.hbm.inventory;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.LaunchpadSoyuz;
import com.hbm.entity.missile.EntityRocketSoyuz;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.saveddata.satellites.*;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz.*;
import io.netty.buffer.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.*;
import net.minecraft.world.storage.*;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SoyuzLaunchpadTest {
    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap();
        GameRegistry.registerTileEntity(Pad.class, new ResourceLocation("hbm", "test_soyuz_pad"));
    }
    @AfterEach void resetModes() { com.hbm.config.GeneralConfig.enableExpensiveMode = false; com.hbm.config.GeneralConfig.enable528 = false; }
    private static ItemStack satellite(int meta) { ItemStack s = new ItemStack(ModItems.satellite, 1, meta); ISatChip.setFreqS(s, 58690); return s; }
    private static Pad ready(TestWorld world) {
        Pad p = new Pad(); p.setWorld(world); p.setPos(new BlockPos(0, 64, 0)); p.power = 1_000_000;
        p.tanks[0].setFill(128000); p.tanks[1].setFill(128000);
        p.inventory.setStackInSlot(0, new ItemStack(ModItems.missile_soyuz, 1, 2));
        p.inventory.setStackInSlot(2, satellite(0)); p.loadedType = 2; p.soyuzStatus = SoyuzStatus.READY;
        return p;
    }
    private static void cargo(Pad p) {
        p.cargoMode = true; p.inventory.setStackInSlot(1, new ItemStack(ModItems.designator));
        NBTTagCompound target = new NBTTagCompound(); target.setInteger("xCoord", -123); target.setInteger("zCoord", 456);
        p.inventory.getStackInSlot(1).setTagCompound(target);
        for(int i = 9; i < 27; i++) { ItemStack s = new ItemStack(Items.DIAMOND, i - 8); s.setStackDisplayName("cargo-" + i); p.inventory.setStackInSlot(i, s); }
    }
    @Test void capacityFuelAndOriginalSlotCount() { Pad p = new Pad(); assertEquals(27, p.inventory.getSlots()); assertSame(Fluids.KEROSENE_REFORM, p.tanks[0].getTankType()); assertSame(Fluids.OXYGEN, p.tanks[1].getTankType()); for(int i = 0; i < 2; i++) assertEquals(128000, p.tanks[i].getMaxFill()); assertEquals(300, p.FUEL_DURATION); assertEquals(600, p.COUNTDOWN_DURATION); }
    @Test void resourceChecksUseActualRocketRatherThanStaleDisplayType() { Pad p = ready(new TestWorld(false)); p.loadedType = -1; assertTrue(p.canLaunch()); p.inventory.setStackInSlot(0, ItemStack.EMPTY); p.loadedType = 2; assertFalse(p.canLaunch()); }
    @Test void unrelatedMetadataSevenIsNotAnOrbitalPayload() { assertFalse(TileEntityLaunchpadSoyuz.needsOrbiter(new ItemStack(Items.COAL, 1, 7))); assertFalse(TileEntityLaunchpadSoyuz.acceptsPayload(new ItemStack(Items.COAL, 1, 7))); }
    @Test void lunarAndGeraldUseTypedDescriptorRatherThanItemMetadata() { assertTrue(TileEntityLaunchpadSoyuz.needsOrbiter(new ItemStack(ModItems.satellite, 1, 4))); assertTrue(TileEntityLaunchpadSoyuz.needsOrbiter(new ItemStack(ModItems.sat_gerald))); assertFalse(TileEntityLaunchpadSoyuz.needsOrbiter(satellite(7))); }
    @Test void lunarRequiresOrbitalModuleAndPlainSatelliteDoesNot() { Pad p = ready(new TestWorld(false)); p.inventory.setStackInSlot(2, new ItemStack(ModItems.satellite, 1, 4)); assertEquals(1, p.orbital()); assertFalse(p.canLaunch()); p.inventory.setStackInSlot(3, new ItemStack(ModItems.missile_soyuz_lander)); assertEquals(2, p.orbital()); assertTrue(p.canLaunch()); }
    @Test void satelliteModeRejectsUnregisteredPayloadAndEmptyPayload() { Pad p = ready(new TestWorld(false)); p.inventory.setStackInSlot(2, new ItemStack(Items.DIAMOND)); assertFalse(p.canLaunch()); p.inventory.setStackInSlot(2, ItemStack.EMPTY); assertFalse(p.canLaunch()); }
    @Test void emptyTanksAndInsufficientPowerPreventLaunch() { Pad p = ready(new TestWorld(false)); p.power = 9999; assertFalse(p.canLaunch()); p.power = 10000; p.tanks[0].setFill(99999); assertFalse(p.canLaunch()); p.tanks[0].setFill(100000); p.tanks[1].setFill(99999); assertFalse(p.canLaunch()); p.tanks[1].setFill(100000); assertTrue(p.canLaunch()); }
    @Test void clientCannotConsumeLaunchResources() { TestWorld w = new TestWorld(true); Pad p = ready(w); assertFalse(p.liftOff()); assertEquals(0, w.spawned.size()); assertEquals(128000, p.tanks[0].getFill()); assertFalse(p.inventory.getStackInSlot(0).isEmpty()); }
    @Test void cancelledSatelliteSpawnKeepsAllResourcesAndCopiesPayloadBeforeEvent() { TestWorld w = new TestWorld(false); w.allowSpawn = false; Pad p = ready(w); ItemStack s = p.inventory.getStackInSlot(2); assertFalse(p.liftOff()); assertEquals(128000, p.tanks[0].getFill()); assertEquals(128000, p.tanks[1].getFill()); assertFalse(p.inventory.getStackInSlot(0).isEmpty()); assertSame(s, p.inventory.getStackInSlot(2)); assertEquals(58690, ISatChip.getFreqS(((EntityRocketSoyuz)w.spawned.get(0)).getSat())); }
    @Test void satelliteLaunchConsumesExactlyOneRocketAndOneHundredBucketsEach() { TestWorld w = new TestWorld(false); Pad p = ready(w); assertTrue(p.liftOff()); assertEquals(28000, p.tanks[0].getFill()); assertEquals(28000, p.tanks[1].getFill()); assertTrue(p.inventory.getStackInSlot(0).isEmpty()); assertTrue(p.inventory.getStackInSlot(2).isEmpty()); EntityRocketSoyuz r = (EntityRocketSoyuz) w.spawned.get(0); assertEquals(2, r.getSkin()); assertEquals(0, r.mode); assertEquals(SoyuzStatus.ABSENT, p.soyuzStatus); assertEquals(0, p.countdown); assertEquals(58690, ISatChip.getFreqS(r.getSat())); }
    @Test void orbitalModuleIsConsumedOnlyForPayloadThatNeedsIt() { TestWorld w = new TestWorld(false); Pad p = ready(w); p.inventory.setStackInSlot(3, new ItemStack(ModItems.missile_soyuz_lander)); assertTrue(p.liftOff()); assertFalse(p.inventory.getStackInSlot(3).isEmpty()); p = ready(w); p.inventory.setStackInSlot(2, new ItemStack(ModItems.sat_gerald)); p.inventory.setStackInSlot(3, new ItemStack(ModItems.missile_soyuz_lander)); w.allowSpawn = false; assertFalse(p.liftOff()); assertFalse(p.inventory.getStackInSlot(3).isEmpty()); w.allowSpawn = true; assertTrue(p.liftOff()); assertTrue(p.inventory.getStackInSlot(3).isEmpty()); }
    @Test void cargoNeedsConfiguredDesignatorAndNonemptyCargo() { Pad p = ready(new TestWorld(false)); cargo(p); assertTrue(p.canLaunch()); p.inventory.getStackInSlot(1).setTagCompound(null); assertFalse(p.canLaunch()); p.inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND)); assertFalse(p.canLaunch()); cargo(p); for(int i = 9; i < 27; i++) p.inventory.setStackInSlot(i, ItemStack.EMPTY); assertFalse(p.canLaunch()); }
    @Test void cargoCoordinatesComeFromInterfaceIncludingNegativeFloors() { TestWorld w = new TestWorld(false); Pad p = ready(w); cargo(p); p.inventory.setStackInSlot(1, new ItemStack(new TestDesignator())); assertTrue(p.liftOff()); EntityRocketSoyuz r = (EntityRocketSoyuz) w.spawned.get(0); assertEquals(-43, r.targetX); assertEquals(19, r.targetZ); }
    @Test void cancelledCargoLaunchConservesEverySlotAndBothTanks() { TestWorld w = new TestWorld(false); w.allowSpawn = false; Pad p = ready(w); cargo(p); assertFalse(p.liftOff()); for(int i = 9; i < 27; i++) assertEquals(i - 8, p.inventory.getStackInSlot(i).getCount()); assertEquals(128000, p.tanks[0].getFill()); assertEquals(128000, p.tanks[1].getFill()); assertFalse(p.inventory.getStackInSlot(1).isEmpty()); assertFalse(p.inventory.getStackInSlot(0).isEmpty()); }
    @Test void cargoLaunchCopiesAllEighteenSlotsAndKeepsDesignatorAndSatellite() { TestWorld w = new TestWorld(false); Pad p = ready(w); cargo(p); ItemStack input = p.inventory.getStackInSlot(26); assertTrue(p.liftOff()); EntityRocketSoyuz r = (EntityRocketSoyuz) w.spawned.get(0); assertEquals(1, r.mode); assertEquals(-123, r.targetX); assertEquals(456, r.targetZ); NBTTagCompound n = new NBTTagCompound(); r.writeToNBT(n); assertEquals(18, n.getTagList("items", 10).tagCount()); input.setCount(1); assertEquals(18, n.getTagList("items", 10).getCompoundTagAt(17).getByte("Count")); for(int i = 9; i < 27; i++) assertTrue(p.inventory.getStackInSlot(i).isEmpty()); assertFalse(p.inventory.getStackInSlot(1).isEmpty()); assertFalse(p.inventory.getStackInSlot(2).isEmpty()); }
    @Test void cargoModeReservesOnlyRocketAndDesignatorAndCanTransportSatellitesAndLanders() {
        Pad p = ready(new TestWorld(false)); p.cargoMode = true;
        assertFalse(p.isItemValidForSlot(9, new ItemStack(ModItems.missile_soyuz)));
        assertFalse(p.isItemValidForSlot(9, new ItemStack(ModItems.designator)));
        for(ItemStack stack : new ItemStack[] {satellite(0), new ItemStack(ModItems.missile_soyuz_lander), new ItemStack(Items.DIAMOND)})
            assertTrue(p.isItemValidForSlot(9, stack));
        assertFalse(p.isItemValidForSlot(2, satellite(0)));
        assertFalse(p.isItemValidForSlot(3, new ItemStack(ModItems.missile_soyuz_lander)));
        p.cargoMode = false; assertFalse(p.isItemValidForSlot(9, new ItemStack(Items.DIAMOND)));
    }

    @Test void outputAndBatterySlotsRespectAutomationDirection() { Pad p = new Pad(); assertFalse(p.isItemValidForSlot(5, new ItemStack(Items.DIAMOND))); assertFalse(p.isItemValidForSlot(7, new ItemStack(Items.DIAMOND))); for(int i : new int[] {4,6,8}) assertFalse(p.canExtractItem(i, ItemStack.EMPTY, 0)); assertTrue(p.canExtractItem(9, ItemStack.EMPTY, 0)); }
    @Test void launchControlRequiresReadyAndValidPayload() { Pad p = ready(new TestWorld(false)); NBTTagCompound n = new NBTTagCompound(); n.setBoolean("launch", true); p.soyuzStatus = SoyuzStatus.LOADING; p.receiveControl(n); assertEquals(0, p.countdown); p.soyuzStatus = SoyuzStatus.READY; p.receiveControl(n); assertEquals(600, p.countdown); assertEquals(SoyuzStatus.LAUNCHING, p.soyuzStatus); }
    @Test void modeChangeCancelsActiveCountdownWithoutConsumingAnything() { Pad p = ready(new TestWorld(false)); p.soyuzStatus = SoyuzStatus.LAUNCHING; p.countdown = 120; NBTTagCompound n = new NBTTagCompound(); n.setBoolean("cargo", true); p.receiveControl(n); assertTrue(p.cargoMode); assertEquals(0, p.countdown); assertEquals(SoyuzStatus.READY, p.soyuzStatus); assertEquals(128000, p.tanks[0].getFill()); }
    @Test void removalOfPayloadCancelsCountdown() { Pad p = ready(new TestWorld(false)); p.soyuzStatus = SoyuzStatus.LAUNCHING; p.countdown = 100; p.inventory.setStackInSlot(2, ItemStack.EMPTY); p.updateStates(); assertEquals(0, p.countdown); assertEquals(SoyuzStatus.READY, p.soyuzStatus); }
    @Test void removalOfRocketCancelsCountdown() { Pad p = ready(new TestWorld(false)); p.soyuzStatus = SoyuzStatus.LAUNCHING; p.countdown = 100; p.inventory.setStackInSlot(0, ItemStack.EMPTY); p.updateStates(); assertEquals(0, p.countdown); assertEquals(SoyuzStatus.ABSENT, p.soyuzStatus); }
    @Test void missingFuelDuringCountdownCancelsIt() { Pad p = ready(new TestWorld(false)); p.soyuzStatus = SoyuzStatus.LAUNCHING; p.countdown = 100; p.tanks[0].setFill(99999); p.updateStates(); assertEquals(0, p.countdown); assertEquals(0, ((TestWorld)p.getWorld()).spawned.size()); }
    @Test void fuelCountdownPausesUntilBothFuelTypesAreAvailable() { Pad p = ready(new TestWorld(false)); p.soyuzStatus = SoyuzStatus.FUELING; p.fuelCountdown = 13; p.tanks[0].setFill(99999); p.updateStates(); assertEquals(13, p.fuelCountdown); p.tanks[0].setFill(100000); p.updateStates(); assertEquals(12, p.fuelCountdown); }
    @Test void completeLoadingAndFuelingSequenceReachesReady() { Pad p = ready(new TestWorld(false)); p.soyuzStatus = SoyuzStatus.ABSENT; for(int i = 0; i < 900; i++) { p.updateStates(); p.move(); } assertEquals(SoyuzStatus.READY, p.soyuzStatus); assertEquals(ComponentStatus.DEPLOY, p.strutStatus); for(int i = 0; i < 7; i++) assertEquals(1F, p.positions[i]); }
    @Test void fullCountdownLaunchesOnceAndReturnsSupportStruts() { TestWorld w = new TestWorld(false); Pad p = ready(w); p.soyuzStatus = SoyuzStatus.LAUNCHING; p.countdown = 600; for(int i = 0; i < 601; i++) { p.updateStates(); p.move(); } assertEquals(1, w.spawned.size()); assertEquals(SoyuzStatus.ABSENT, p.soyuzStatus); for(int i = 0; i < 5; i++) assertEquals(0F, p.target[i]); }
    @Test void cancelledSpawnReturnsToReadyWithSupportAndResourcesIntact() { TestWorld w = new TestWorld(false); w.allowSpawn = false; Pad p = ready(w); for(int i = 0; i < 5; i++) p.positions[i] = p.target[i] = 1; p.soyuzStatus = SoyuzStatus.LAUNCHING; p.updateStates(); assertEquals(SoyuzStatus.READY, p.soyuzStatus); for(int i = 0; i < 5; i++) assertEquals(1F, p.target[i]); assertEquals(128000, p.tanks[0].getFill()); }
    @Test void savedStateRestoresAllEightAnimationChannelsAndCountdowns() { Pad p = ready(new TestWorld(false)); cargo(p); p.soyuzStatus = SoyuzStatus.LAUNCHING; p.strutStatus = p.carriageStatus = p.rotorStatus = ComponentStatus.DEPLOY; p.countdown = 123; p.fuelCountdown = 17; for(int i = 0; i < 8; i++) { p.positions[i] = i*.1F; p.speed[i] = i*.001F; p.target[i] = 1; } Pad copy = new Pad(); copy.readFromNBT(p.writeToNBT(new NBTTagCompound())); assertArrayEquals(p.positions, copy.positions); assertArrayEquals(p.positions, copy.prevPositions); assertArrayEquals(p.positions, copy.syncPositions); assertArrayEquals(p.speed, copy.speed); assertArrayEquals(p.target, copy.target); assertEquals(123, copy.countdown); assertEquals(17, copy.fuelCountdown); assertEquals(2, copy.loadedType); assertTrue(copy.cargoMode); assertEquals(SoyuzStatus.LAUNCHING, copy.soyuzStatus); assertEquals(ComponentStatus.DEPLOY, copy.rotorStatus); assertEquals(18, copy.inventory.getStackInSlot(26).getCount()); }
    @Test void packetRoundTripAndThreeTickInterpolationCoverAllChannels() { Pad p = ready(new TestWorld(false)); p.countdown = 123; p.fuelCountdown = 17; p.cargoMode = true; p.soyuzStatus = SoyuzStatus.LAUNCHING; p.carriageStatus = ComponentStatus.DEPLOY; for(int i = 0; i < 8; i++) { p.positions[i] = 3*(i+1); p.speed[i] = .1F; p.target[i] = 1; } ByteBuf b = Unpooled.buffer(); try { p.serialize(b); Pad copy = new Pad(); copy.setWorld(new TestWorld(true)); copy.deserialize(b); assertEquals(0, b.readableBytes()); assertEquals(123, copy.countdown); assertEquals(17, copy.fuelCountdown); assertTrue(copy.cargoMode); assertEquals(ComponentStatus.DEPLOY, copy.carriageStatus); assertArrayEquals(p.speed, copy.speed); assertArrayEquals(p.target, copy.target); copy.update(); for(int i = 0; i < 8; i++) assertEquals(i+1, copy.positions[i]); copy.update(); copy.update(); assertArrayEquals(p.positions, copy.positions); } finally { b.release(); } }
    @Test void allFourRotationsDeclareTheSameTwelvePhysicalPortsAsTile() { LaunchpadSoyuz block = (LaunchpadSoyuz) ModBlocks.launchpad_soyuz; for(ForgeDirection dir : new ForgeDirection[] {ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST}) { Pad p = new Pad(); p.meta = dir.ordinal()+10; p.setPos(new BlockPos(12,64,34)); int[][] ports = block.getAllPorts(dir); assertEquals(12, ports.length); com.hbm.tileentity.TilePort.PortDef[] actual = p.getPorts(); for(int i = 0; i < 12; i++) { assertEquals(12+ports[i][0], actual[i].portPositions[0].getX()); assertEquals(65, actual[i].portPositions[0].getY()); assertEquals(34+ports[i][2], actual[i].portPositions[0].getZ()); } } }
    @Test void rocketStoresAllEighteenCopiedSlotsAndFlightState() { TestRocket r = new TestRocket(new TestWorld(false)); List<ItemStack> items = new ArrayList<>(); for(int i = 0; i < 18; i++) items.add(new ItemStack(Items.DIAMOND, i+1)); r.setPayload(items); items.get(17).setCount(1); r.setSkin(2); r.mode = 1; r.targetX = -123; r.targetZ = 456; r.motionY = 1; r.onUpdate(); TestRocket copy = new TestRocket(new TestWorld(false)); copy.load(r.save()); assertEquals(2, copy.getSkin()); assertEquals(1, copy.mode); assertEquals(-123, copy.targetX); assertEquals(456, copy.targetZ); assertEquals(18, copy.save().getTagList("items",10).getCompoundTagAt(17).getByte("Count")); assertTrue(copy.save().getDouble("acceleration") > 0); }
    @Test void satelliteDeploysOnceOnServerAndKeepsForeignOrbitAsData() { TestWorld w = new TestWorld(false); TestRocket r = new TestRocket(w); ItemStack s = satellite(0); ISatChip.setOrbitKeyS(s, OrbitKey.dimension(5848)); r.setSat(s); r.setPosition(0,601,0); r.onUpdate(); assertTrue(r.isDead); assertTrue(r.getSat().isEmpty()); assertNotNull(com.hbmspace.tileentity.TESpaceUtil.getData(w,0,0).getSatFromFreq(58690)); TestRocket client = new TestRocket(new TestWorld(true)); client.setSat(s); client.setPosition(0,601,0); client.onUpdate(); assertFalse(client.isDead); assertFalse(client.getSat().isEmpty()); }
    @Test void orphanSensorReturnsOriginalFrequencyAndOrbitInsteadOfLosingPayload() { TestWorld w = new TestWorld(false); TestRocket r = new TestRocket(w); ItemStack s = satellite(12); ISatChip.setOrbitKeyS(s, OrbitKey.dimension(5849)); r.setSat(s); r.setPosition(0,601,0); r.onUpdate(); assertTrue(r.isDead); assertEquals(1,w.spawned.size()); ItemStack returned = ((EntityItem)w.spawned.get(0)).getItem(); assertEquals(58690,ISatChip.getFreqS(returned)); assertEquals(OrbitKey.dimension(5849),ISatChip.getOrbitKeyS(returned)); }
    @Test void normalAndExpensiveRecipesProduceSeparatePadWithoutReplacingOldLauncher() { for(boolean expensive : new boolean[] {false,true}) { DataConsumerRecipeTest.defaults(expensive,false); GenericRecipe r = AssemblyMachineRecipes.INSTANCE.recipeNameMap.get("ass.launchpadsoyuz"); assertEquals(1200,r.duration); assertEquals(5000,r.power); assertEquals(expensive?5:6,r.inputItem.length); assertSame(ModBlocks.launchpad_soyuz,((ItemBlock)r.outputItem[0].getSingle().getItem()).getBlock()); assertNotSame(ModBlocks.soyuz_launcher,ModBlocks.launchpad_soyuz); } }

    public static class Pad extends TileEntityLaunchpadSoyuz {
        int meta = 12;
        @Override public void markDirty() { }
        @Override public void markChanged() { }
        @Override public int getBlockMetadata() { return meta; }
        @Override public void networkPackNT(int range) { }
        @Override protected void handleSound(int index, boolean running) { }
    }
    static class TestRocket extends EntityRocketSoyuz {
        TestRocket(World w) { super(w); }
        NBTTagCompound save() { NBTTagCompound n = new NBTTagCompound(); writeEntityToNBT(n); return n; }
        void load(NBTTagCompound n) { readEntityFromNBT(n); }
    }
    static class TestDesignator extends Item implements IDesignatorItem {
        @Override public boolean isReady(World w, ItemStack s, int x, int y, int z) { return true; }
        @Override public Vec3d getCoords(World w, ItemStack s, int x, int y, int z) { return new Vec3d(-42.75,64,19.5); }
    }
    static class TestWorld extends World {
        private final MapStorage storage = new MapStorage(null);
        boolean allowSpawn = true;
        final List<Entity> spawned = new ArrayList<>();
        TestWorld(boolean client) { super(new SaveHandlerMP(),new WorldInfo(new NBTTagCompound()),new WorldProviderSurface(),new Profiler(),client); }
        @Override public boolean spawnEntity(Entity e) { spawned.add(e); return allowSpawn; }
        @Override public MapStorage getPerWorldStorage() { return storage; }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty) { return true; }
        @Override public List<Entity> getEntitiesWithinAABBExcludingEntity(Entity e, AxisAlignedBB box) { return Collections.emptyList(); }
        @Override public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> type, AxisAlignedBB box) { return Collections.emptyList(); }
        @Override public void playSound(net.minecraft.entity.player.EntityPlayer p, double x, double y, double z, net.minecraft.util.SoundEvent s, net.minecraft.util.SoundCategory cat, float volume, float pitch) { }
    }
}
