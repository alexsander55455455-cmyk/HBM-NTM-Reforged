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
import static org.junit.jupiter.api.Assertions.*;

class SatelliteSensorTest {
    @BeforeAll static void bootstrap() throws Exception { DataConsumerRecipeTest.bootstrap(); }
    private static ItemStack sensor() { return new ItemStack(ModItems.satellite, 1, EnumSatelliteType.SCIENCE_SENSOR.ordinal()); }

    @Test void sensorAppendsMetadataWithoutPretendingToBeAStandaloneSatellite() {
        assertEquals(11, EnumSatelliteType.SCIENCE.ordinal());
        assertEquals(12, EnumSatelliteType.SCIENCE_SENSOR.ordinal());
        assertEquals(16_000, SatelliteTypeRegistry.getPayloadMass(sensor()));
        assertTrue(SatelliteTypeRegistry.isPayload(sensor()));
        assertFalse(SatelliteScience.isSensor(null));
        assertFalse(SatelliteScience.isSensor(ItemStack.EMPTY));
        assertFalse(SatelliteScience.isSensor(new ItemStack(ModItems.satellite, 1, 11)));
        assertFalse(SatelliteScience.isSensor(new ItemStack(ModItems.drive, 1, 12)));
    }
    @Test void onlySensorIsAcceptedAndOneDeliveryAddsOneModule() {
        SatelliteScience science = new SatelliteScience(); TestWorld world = new TestWorld(false);
        ItemStack part = sensor();
        assertTrue(science.onPartDelivered(world, part)); assertEquals(1, science.sensorCount);
        assertEquals(1, part.getCount());
        assertFalse(science.onPartDelivered(world, new ItemStack(ModItems.satellite, 1, 11)));
        assertFalse(science.onPartDelivered(world, ItemStack.EMPTY)); assertEquals(1, science.sensorCount);
        assertFalse(new SatelliteMapper().onPartDelivered(world, part));
    }
    @Test void clientDeliveryCannotInstallModules() {
        SatelliteScience science = new SatelliteScience(); assertFalse(science.onPartDelivered(new TestWorld(true), sensor()));
        assertEquals(0, science.sensorCount);
    }
    @Test void emptyLaboratoryDoesNotInventSensorProgressOrData() {
        SatelliteScience science = new SatelliteScience(); assertFalse(science.tickSensors());
        assertEquals(0, science.sensorProgress); assertNull(science.driveOutput);
    }
    @Test void oneSensorNeedsAllSevenMillionTwoHundredThousandTicksPlusPublicationTick() {
        assertEquals(7_200_000, SatelliteScience.SENSOR_DURATION);
        SatelliteScience science = new SatelliteScience(); science.sensorCount = 1;
        for(int i = 0; i < SatelliteScience.SENSOR_DURATION; i++) {
            assertTrue(science.tickSensors());
            if(i < SatelliteScience.SENSOR_DURATION - 1 && i % 1_000_000 == 0) assertNull(science.driveOutput);
        }
        assertEquals(SatelliteScience.SENSOR_DURATION, science.sensorProgress); assertNull(science.driveOutput);
        assertTrue(science.tickSensors()); assertEquals(0, science.sensorProgress);
        assertEquals(EnumDriveType.DISK_ORBITDATA, science.getOutputData(EnumDriveType.DISK_EMPTY));
    }
    @Test void multipleModulesIncreaseProgressLinearlyAndDoNotOverflowAtThreshold() {
        SatelliteScience science = new SatelliteScience(); science.sensorCount = 17;
        science.tickSensors(); assertEquals(17, science.sensorProgress);
        science.sensorProgress = SatelliteScience.SENSOR_DURATION - 3; science.tickSensors();
        assertEquals(SatelliteScience.SENSOR_DURATION, science.sensorProgress); assertNull(science.driveOutput);
        science.tickSensors(); assertEquals(EnumDriveType.DISK_ORBITDATA, science.driveOutput);
        science.consumeData(); science.sensorCount = Integer.MAX_VALUE; science.tickSensors();
        assertEquals(SatelliteScience.SENSOR_DURATION, science.sensorProgress);
        assertFalse(science.onPartDelivered(new TestWorld(false), sensor())); assertEquals(Integer.MAX_VALUE, science.sensorCount);
    }
    @Test void completedSampleWaitsWithoutOverwritingFlightData() {
        SatelliteScience science = new SatelliteScience(); science.sensorCount = 1; science.sensorProgress = SatelliteScience.SENSOR_DURATION;
        science.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_FLIGHTDATA);
        for(int i = 0; i < 100; i++) assertFalse(science.tickSensors());
        assertEquals(EnumDriveType.DISK_FLIGHTDATA, science.driveOutput); assertEquals(SatelliteScience.SENSOR_DURATION, science.sensorProgress);
        science.consumeData(); assertTrue(science.tickSensors());
        assertEquals(EnumDriveType.DISK_ORBITDATA, science.driveOutput); assertEquals(0, science.sensorProgress);
    }
    @Test void completedSampleAlsoWaitsForPendingOrbitalData() {
        SatelliteScience science = new SatelliteScience(); science.sensorCount = 1; science.sensorProgress = SatelliteScience.SENSOR_DURATION;
        science.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_ORBITDATA);
        assertFalse(science.tickSensors()); assertEquals(SatelliteScience.SENSOR_DURATION, science.sensorProgress);
        science.consumeData(); assertTrue(science.tickSensors()); assertEquals(0, science.sensorProgress);
        science.consumeData(); assertTrue(science.tickSensors()); assertNull(science.driveOutput); assertEquals(1, science.sensorProgress);
    }
    @Test void sensorStateAndPendingFlightRecordSurviveNbtRoundtrip() {
        SatelliteScience science = new SatelliteScience(); science.sensorCount = 123; science.sensorProgress = 456789; science.lastScience = 999;
        science.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_FLIGHTDATA);
        NBTTagCompound tag = new NBTTagCompound(); science.writeToNBT(tag);
        SatelliteScience restored = new SatelliteScience(); restored.readFromNBT(tag);
        assertEquals(123, restored.sensorCount); assertEquals(456789, restored.sensorProgress); assertEquals(999, restored.lastScience);
        assertEquals(EnumDriveType.DISK_FLIGHTDATA, restored.driveOutput); restored.tickSensors(); assertEquals(456912, restored.sensorProgress);
    }
    @Test void oldScienceSavesDefaultToZeroSensors() {
        SatelliteScience science = new SatelliteScience(); science.sensorCount = 2; science.sensorProgress = 3; science.readFromNBT(new NBTTagCompound());
        assertEquals(0, science.sensorCount); assertEquals(0, science.sensorProgress); assertFalse(science.tickSensors());
    }
    @Test void centralTickAdvancesEachOrbitExactlyOnceAndMarksChangesDirty() {
        TestWorld world = new TestWorld(false); SatelliteSavedData local = SatelliteSavedData.getData(world);
        SatelliteSavedData remote = SatelliteSavedData.getDataForOrbit(world, OrbitKey.dimension(5846));
        SatelliteScience a = new SatelliteScience(), b = new SatelliteScience(); a.sensorCount = 2; b.sensorCount = 3;
        local.putSatellite(58510, a); remote.putSatellite(58510, b); remote.putSatellite(58511, new SatelliteMapper());
        long revision = local.getRevision(); local.tickScience(world);
        assertEquals(2, a.sensorProgress); assertEquals(3, b.sensorProgress); assertEquals(revision + 1, remote.getRevision());
        NBTTagCompound nbt = local.writeToNBT(new NBTTagCompound()); SatelliteSavedData restored = new SatelliteSavedData(); restored.readFromNBT(nbt);
        assertEquals(2, ((SatelliteScience)restored.resolveSatellite(local.getOrbitKey(), null, 58510).getSatellite()).sensorProgress);
    }
    @Test void clientCentralTickIsReadOnlyAndInactiveLabsDoNotDirtyEveryTick() {
        TestWorld world = new TestWorld(false); SatelliteSavedData data = SatelliteSavedData.getData(world);
        SatelliteScience science = new SatelliteScience(); data.putSatellite(58510, science); long revision = data.getRevision();
        data.tickScience(world); assertEquals(revision, data.getRevision());
        science.sensorCount = 1; data.tickScience(new TestWorld(true)); assertEquals(0, science.sensorProgress); assertEquals(revision, data.getRevision());
    }
    @Test void additiveRecipeHasOriginalNormalAndExpensiveIngredientsAndCosts() {
        for(boolean expensive : new boolean[]{false, true}) {
            DataConsumerRecipeTest.defaults(expensive, false); GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.recipeNameMap.get("ass.sensorrelay");
            assertNotNull(recipe); assertEquals(600, recipe.duration); assertEquals(25_000, recipe.power); assertFalse(recipe.isPooled());
            assertEquals(expensive ? 3 : 4, recipe.inputItem.length);
            int[] counts = expensive ? new int[]{1,1,1} : new int[]{1,4,1,1};
            for(int i = 0; i < counts.length; i++) { assertEquals(counts[i], recipe.inputItem[i].stacksize); assertFalse(recipe.inputItem[i].getStackList().isEmpty()); }
            assertSame(ModItems.satellite, recipe.outputItem[0].getSingle().getItem()); assertEquals(12, recipe.outputItem[0].getSingle().getMetadata());
            assertNotNull(AssemblyMachineRecipes.INSTANCE.recipeNameMap.get("ass.spacelab"));
        }
    }
    private static class TestWorld extends World {
        private final MapStorage storage = new MapStorage(null);
        TestWorld(boolean client) { super(new SaveHandlerMP(), new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), client); }
        @Override public MapStorage getPerWorldStorage() { return storage; }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
    }
}
