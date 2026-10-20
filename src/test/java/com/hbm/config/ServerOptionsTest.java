package com.hbm.config;

import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.interfaces.IDoor;
import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.DoorDecl;
import com.hbm.tileentity.TileEntityDoorGeneric;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ServerOptionsTest {
    @TempDir Path directory;
    File priorDirectory;
    Map<RunningConfig.ConfigWrapper,Object> priorValues;
    TestBlock block;
    TestDoor door;
    TestPlayer player;
    TestWorld world;
    @BeforeAll static void bootstrap() throws Exception {
        java.lang.reflect.Method setup=Class.forName("com.hbm.inventory.DataConsumerRecipeTest").getDeclaredMethod("bootstrap");
        setup.setAccessible(true);setup.invoke(null);
        com.hbm.items.weapon.sedna.factory.GunFactory.init();
    }
    @BeforeEach void setup() throws Exception {
        priorDirectory=MainRegistry.configHbmDir;MainRegistry.configHbmDir=directory.toFile();
        priorValues=new HashMap<>();
        for(Field f:ServerConfig.class.getFields()) if(f.getType()==RunningConfig.ConfigWrapper.class) { RunningConfig.ConfigWrapper w=(RunningConfig.ConfigWrapper)f.get(null);priorValues.put(w,w.get()); }
        block=allocate(TestBlock.class);door=allocate(TestDoor.class);player=allocate(TestPlayer.class);world=allocate(TestWorld.class);
        block.door=door;block.type=DoorDecl.TRANSITION_SEAL;door.allowed=true;door.toolAllowed=true;door.mode=IDoor.Mode.DEFAULT;player.held=ItemStack.EMPTY;
    }
    @AfterEach void restore() { MainRegistry.configHbmDir=priorDirectory;priorValues.forEach(RunningConfig.ConfigWrapper::set); }
    static RunningConfig.ConfigWrapper wrapper(String name) throws Exception { return (RunningConfig.ConfigWrapper)ServerConfig.class.getField(name).get(null); }
    void larp(boolean enabled) throws Exception { wrapper("ULTRA_LARP_MODE").set(enabled); }
    void file(String json) throws Exception { Files.write(directory.resolve("hbmServer.json"),json.getBytes(StandardCharsets.UTF_8)); }
    boolean click() { return block.onBlockActivated(world,BlockPos.ORIGIN,null,player,EnumHand.MAIN_HAND,EnumFacing.UP,0,0,0); }
    @Test void defaultCramLimitIsTwentyFive() throws Exception { assertEquals(25,wrapper("CONVEYOR_CRAM_MAX").get()); }
    @Test void defaultCramBreaksBelt() throws Exception { assertEquals(true,wrapper("CONVEYOR_CRAM_EXPLODE").get()); }
    @Test void defaultDoorsAllowManualUse() throws Exception { assertEquals(false,wrapper("ULTRA_LARP_MODE").get()); }
    @Test void legacyConfigAddsAllThreeDefaultsAndKeepsMineValue() throws Exception { file("{\"MINE_AP_DAMAGE\":13.5}");ServerConfig.initConfig();assertEquals(13.5F,ServerConfig.MINE_AP_DAMAGE.get());assertEquals(25,wrapper("CONVEYOR_CRAM_MAX").get());assertEquals(true,wrapper("CONVEYOR_CRAM_EXPLODE").get());assertEquals(false,wrapper("ULTRA_LARP_MODE").get());String text=new String(Files.readAllBytes(directory.resolve("hbmServer.json")),StandardCharsets.UTF_8);for(String key:new String[]{"CONVEYOR_CRAM_MAX","CONVEYOR_CRAM_EXPLODE","ULTRA_LARP_MODE"}) assertTrue(text.contains(key)); }
    @Test void configuredValuesOverrideDefaults() throws Exception { file("{\"CONVEYOR_CRAM_MAX\":7,\"CONVEYOR_CRAM_EXPLODE\":false,\"ULTRA_LARP_MODE\":true}");ServerConfig.initConfig();assertEquals(7,wrapper("CONVEYOR_CRAM_MAX").get());assertEquals(false,wrapper("CONVEYOR_CRAM_EXPLODE").get());assertEquals(true,wrapper("ULTRA_LARP_MODE").get()); }
    @Test void reloadImmediatelyChangesAllThreeWrappers() throws Exception { ServerConfig.initConfig();file("{\"CONVEYOR_CRAM_MAX\":3,\"CONVEYOR_CRAM_EXPLODE\":false,\"ULTRA_LARP_MODE\":true}");ServerConfig.reload();assertEquals(3,wrapper("CONVEYOR_CRAM_MAX").get());assertEquals(false,wrapper("CONVEYOR_CRAM_EXPLODE").get());assertEquals(true,wrapper("ULTRA_LARP_MODE").get()); }
    @Test void refreshAndReloadPreserveOverrides() throws Exception { ServerConfig.initConfig();wrapper("CONVEYOR_CRAM_MAX").set(11);larp(true);ServerConfig.refresh();wrapper("CONVEYOR_CRAM_MAX").set(2);larp(false);ServerConfig.reload();assertEquals(11,wrapper("CONVEYOR_CRAM_MAX").get());assertEquals(true,wrapper("ULTRA_LARP_MODE").get()); }
    @Test void zeroAndNegativeLimitsRetainOriginalLiteralSemantics() throws Exception { ServerConfig.initConfig();for(int limit:new int[]{0,-1}) { file("{\"CONVEYOR_CRAM_MAX\":"+limit+"}");ServerConfig.reload();assertEquals(limit,wrapper("CONVEYOR_CRAM_MAX").get()); } }
    @Test void ordinaryDoorCanOpenWhenGlobalModeIsOff() throws Exception { larp(false);assertTrue(click());assertEquals(1,door.toggles); }
    @Test void globalModeBlocksOrdinaryDoorBeforeToggle() throws Exception { larp(true);assertFalse(click());assertEquals(0,door.toggles); }
    @Test void waterDoorRemainsManualInGlobalMode() throws Exception { larp(true);block.type=DoorDecl.WATER_DOOR;assertTrue(click());assertEquals(1,door.toggles); }
    @Test void existingRedstoneModeStillBlocksWaterDoor() throws Exception { larp(true);block.type=DoorDecl.WATER_DOOR;door.redstone=true;assertFalse(click());assertEquals(0,door.toggles); }
    @Test void lockedWaterDoorStillRequiresAccess() throws Exception { larp(true);block.type=DoorDecl.WATER_DOOR;door.allowed=false;assertFalse(click());assertEquals(0,door.toggles); }
    @Test void sneakingStillDoesNotToggleDoor() throws Exception { larp(false);player.sneaking=true;assertFalse(click());assertEquals(0,door.toggles); }
    @Test void keyKitStillDoesNotToggleWaterDoor() throws Exception { larp(true);block.type=DoorDecl.WATER_DOOR;player.held=new ItemStack(ModItems.key_kit);assertFalse(click());assertEquals(0,door.toggles); }
    @Test void authorizedScrewdriverStillChangesToolableMode() throws Exception { larp(true);door.mode=IDoor.Mode.TOOLABLE;player.held=new ItemStack(ModItems.screwdriver);assertTrue(click());assertEquals(1,door.toolToggles);assertEquals(0,door.toggles); }
    @Test void lockedScrewdriverStillNeedsAuthorization() throws Exception { larp(true);door.mode=IDoor.Mode.TOOLABLE;door.toolAllowed=false;player.held=new ItemStack(ModItems.screwdriver);assertFalse(click());assertEquals(0,door.toolToggles);assertEquals(0,door.toggles); }
    @Test void returningGlobalModeToFalseRestoresManualOpening() throws Exception { larp(true);assertFalse(click());larp(false);assertTrue(click());assertEquals(1,door.toggles); }
    static <T>T allocate(Class<T> type) throws Exception { Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);return type.cast(((Unsafe)f.get(null)).allocateInstance(type)); }
    static class TestBlock extends BlockDoorGeneric {
        TestDoor door;
        TestBlock() { super(Material.IRON,DoorDecl.WATER_DOOR,false,"server_options_test"); }
        @Override public TileEntity findCoreTE(IBlockAccess world,BlockPos pos) { return door; }
    }
    static class TestDoor extends TileEntityDoorGeneric {
        boolean allowed,toolAllowed,redstone;int toggles,toolToggles;IDoor.Mode mode;
        @Override public boolean canAccess(EntityPlayer p) { return allowed; }
        @Override public boolean canToggleRedstone(EntityPlayer p) { return toolAllowed; }
        @Override public boolean isRedstoneOnly() { return redstone; }
        @Override public IDoor.Mode getConfiguredMode() { return mode; }
        @Override public void toggleRedstoneMode() { toolToggles++; }
        @Override public boolean tryToggle(EntityPlayer p) { toggles++;return true; }
    }
    static class TestPlayer extends EntityPlayer {
        ItemStack held;boolean sneaking;
        TestPlayer() { super(null,null); }
        @Override public ItemStack getHeldItem(EnumHand hand) { return held; }
        @Override public boolean isSneaking() { return sneaking; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }
    static class TestWorld extends World {
        TestWorld() { super(null,null,null,null,false); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty) { return true; }
        @Override public Entity getEntityByID(int id) { return null; }
    }
}
