package com.hbm.inventory;

import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.items.machine.ItemSatellitePayload.EnumSatelliteType;
import com.hbm.saveddata.satellites.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

// Run in an isolated Java 8 process against the downgraded dev JAR.
class SatelliteScienceTest {
    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap();
        SatelliteTypeRegistry.registerSpaceExtensions();
    }
    @Test void preservesAllOldPayloadMetadataAndAppendsScienceAtEleven() {
        assertArrayEquals(new String[]{"SPY","SCANNER","RADAR","MINER_ASTRO","MINER_LUNAR","PRECISION_LASER","DEATH_RAY","XENIUM_RESONATOR","RELAY","DETECTOR","RAY_SCAN","SCIENCE"}, Arrays.stream(EnumSatelliteType.values()).map(Enum::name).toArray(String[]::new));
        assertEquals(11, EnumSatelliteType.SCIENCE.ordinal());
    }
    @Test void scienceUsesThirteenAndNeverReplacesDysonTwelve() {
        String[] oldKeys = {"mapper","scanner","radar","death_ray","resonator","relay","miner_astro","miner_lunar","horizons","precision_laser","detector","ray_scan","dyson_relay"};
        for(int id = 0; id < oldKeys.length; id++) assertEquals(oldKeys[id], SatelliteTypeRegistry.byLegacyId(id).getKey());
        assertEquals("dyson_relay", SatelliteTypeRegistry.byLegacyId(12).getKey());
        assertEquals("science", SatelliteTypeRegistry.byLegacyId(13).getKey());
        assertInstanceOf(SatelliteScience.class, SatelliteTypeRegistry.createByLegacyId(13));
        ItemStack canonical = SatelliteTypeRegistry.byKey("science").getCanonicalStack();
        assertSame(ModItems.satellite, canonical.getItem()); assertEquals(11, canonical.getItemDamage());
    }
    @Test void strictOriginalCooldownStartsOnlyAfterEighteenThousandTicks() {
        TestWorld world = new TestWorld(false); SatelliteScience science = new SatelliteScience();
        world.time = 18000; assertFalse(science.hasData(world));
        world.time++; assertTrue(science.hasData(world)); assertEquals(18001, science.lastScience);
        assertEquals(EnumDriveType.DISK_FLIGHTDATA, science.getOutputData(EnumDriveType.DISK_EMPTY));
        assertNull(science.getOutputData(EnumDriveType.FLASH_EMPTY));
    }
    @Test void eachConsumptionNeedsAnotherCooldownAndCannotDuplicateARecord() {
        TestWorld world = new TestWorld(false); SatelliteScience science = new SatelliteScience();
        world.time = 18001; assertTrue(science.hasData(world)); science.consumeData();
        assertFalse(science.hasData(world)); world.time = 36001; assertFalse(science.hasData(world));
        world.time++; assertTrue(science.hasData(world)); science.consumeData(); assertFalse(science.hasData(world));
    }
    @Test void pendingRecordIsNeverReplacedOrRetimedDuringLongAbsence() {
        TestWorld world = new TestWorld(false); SatelliteScience science = new SatelliteScience();
        world.time = 18001; science.hasData(world); world.time = 1_000_000;
        assertTrue(science.hasData(world)); assertEquals(18001, science.lastScience);
        science.consumeData(); assertTrue(science.hasData(world)); assertEquals(1_000_000, science.lastScience);
        science.consumeData(); assertFalse(science.hasData(world));
    }
    @Test void scienceNeverOverwritesAnExistingDifferentDataRecord() {
        TestWorld world = new TestWorld(false); world.time = 1_000_000;
        SatelliteScience science = new SatelliteScience(); science.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_ORBITDATA);
        assertTrue(science.hasData(world)); assertEquals(0, science.lastScience);
        assertEquals(EnumDriveType.DISK_ORBITDATA, science.driveOutput);
    }
    @Test void clientQueriesNeverGenerateServerData() {
        TestWorld world = new TestWorld(true); world.time = 18001;
        SatelliteScience science = new SatelliteScience(); assertFalse(science.hasData(world)); assertEquals(0, science.lastScience);
    }
    @Test void producingWithoutACompatibleDriveStillMarksTheCentralSaveDirty() {
        TestWorld world = new TestWorld(false); SatelliteSavedData local = SatelliteSavedData.getData(world);
        SatelliteSavedData remote = SatelliteSavedData.getDataForOrbit(world, OrbitKey.dimension(5846));
        SatelliteScience science = new SatelliteScience(); remote.putSatellite(5846, science); long revision = local.getRevision();
        world.time = 18001; assertTrue(science.hasData(world)); assertTrue(local.getRevision() > revision);
        assertSame(science, remote.getSatFromFreq(5846)); assertNull(local.getSatFromFreq(5846));
    }
    @Test void nbtRetainsPendingDataTimerTargetsAndTransmission() {
        TestWorld world = new TestWorld(false); world.time = 1_234_567;
        SatelliteScience source = new SatelliteScience(); source.onCommand(null, Satellite.CMD_SETTARGET, "12", "-34"); source.onCommand(null, Satellite.CMD_GETTARGET); source.hasData(world);
        NBTTagCompound tag = new NBTTagCompound(); source.writeToNBT(tag); SatelliteScience copy = new SatelliteScience(); copy.readFromNBT(tag);
        assertEquals(1_234_567, copy.lastScience); assertEquals(12, copy.getTargetX()); assertEquals(-34, copy.getTargetZ()); assertEquals("12;-34", copy.getTransmission());
        assertEquals(EnumDriveType.DISK_FLIGHTDATA, copy.getOutputData(EnumDriveType.DISK_EMPTY));
        copy.consumeData(); tag = new NBTTagCompound(); copy.writeToNBT(tag); source.readFromNBT(tag); assertFalse(source.hasData(world));
    }
    @Test void oldMissingTimerNbtDoesNotInventAPendingRecord() {
        SatelliteScience science = new SatelliteScience(); science.lastScience = 999; science.readFromNBT(new NBTTagCompound());
        assertEquals(0, science.lastScience); assertNull(science.driveInput); assertNull(science.driveOutput);
    }
    @Test void laboratoryRecipeHasAllSixOriginalIngredientsAndCostsInBothModes() {
        for(boolean expensive : new boolean[]{false, true}) {
            DataConsumerRecipeTest.defaults(expensive, false); GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.recipeNameMap.get("ass.spacelab");
            assertNotNull(recipe); assertEquals(1200, recipe.duration); assertEquals(25000, recipe.power); assertFalse(recipe.isPooled());
            assertEquals(6, recipe.inputItem.length); assertArrayEquals(new int[]{16,32,16,4,16,1}, Arrays.stream(recipe.inputItem).mapToInt(input -> input.stacksize).toArray());
            assertSame(ModItems.satellite, recipe.outputItem[0].getSingle().getItem()); assertEquals(11, recipe.outputItem[0].getSingle().getItemDamage());
            for(RecipesCommon.AStack input : recipe.inputItem) assertFalse(input.getStackList().isEmpty());
        }
    }
    private static class TestWorld extends World {
        long time; private final MapStorage storage = new MapStorage(null);
        TestWorld(boolean client) { super(new SaveHandlerMP(), new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), client); }
        @Override public long getTotalWorldTime() { return time; }
        @Override public MapStorage getPerWorldStorage() { return storage; }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
    }
}
