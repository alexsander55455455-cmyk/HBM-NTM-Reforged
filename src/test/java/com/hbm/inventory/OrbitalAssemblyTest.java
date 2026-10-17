package com.hbm.inventory;

import com.hbm.config.GeneralConfig;
import com.hbm.entity.missile.EntitySatellitePod;
import com.hbm.inventory.recipes.*;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes.ChanceOutputMulti;
import com.hbm.items.ModItems;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.machine.ItemSatellitePayload.EnumSatelliteType;
import com.hbm.saveddata.satellites.*;
import net.minecraft.entity.Entity;
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

class OrbitalAssemblyTest {
    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap(); SpaceAssemblerRecipes.INSTANCE.registerDefaults();
    }
    private static ItemStack module() { return new ItemStack(ModItems.satellite, 1, EnumSatelliteType.SCIENCE_ASSEMBLER.ordinal()); }
    private static ItemStack work() { return new ItemStack(ModItems.orbital_assembly); }
    private static SatelliteScience ordered(int count) {
        SatelliteScience science = new SatelliteScience(); science.assemblerCount = count;
        assertTrue(science.onPartDelivered(new TestWorld(false), work())); return science;
    }
    @Test void appendOnlyMetadataAndDeliveryPayloadsHaveMassWithoutSatelliteFactories() {
        assertEquals(13, EnumSatelliteType.SCIENCE_ASSEMBLER.ordinal()); assertEquals(12, EnumSatelliteType.SCIENCE_SENSOR.ordinal());
        assertEquals(20, EnumCircuitType.CRYSTAL.ordinal()); assertEquals(19, EnumCircuitType.NUMITRON.ordinal());
        for(ItemStack stack : new ItemStack[]{module(),work()}) {
            assertTrue(SatelliteTypeRegistry.isPayload(stack)); assertEquals(16_000, SatelliteTypeRegistry.getPayloadMass(stack));
            assertNull(SatelliteTypeRegistry.byItem(stack)); assertNull(SatelliteTypeRegistry.createFromItem(stack));
        }
        assertFalse(SatelliteTypeRegistry.isPayload(new ItemStack(ModItems.orbital_assembly,1,99)));
        assertEquals(1, ModItems.orbital_assembly.getItemStackLimit(work()));
    }
    @Test void moduleAndWorkDeliveryInstallOneAndEnqueueOneWithoutMutatingCaller() {
        SatelliteScience science = new SatelliteScience(); TestWorld world = new TestWorld(false);
        ItemStack module = module(), work = work(); assertTrue(science.onPartDelivered(world,module));
        assertEquals(1,science.assemblerCount); assertTrue(science.onPartDelivered(world,work));
        assertEquals(1,science.assemblerTasks.size()); assertEquals(1,module.getCount()); assertEquals(1,work.getCount());
        assertFalse(science.onPartDelivered(world, new ItemStack(ModItems.circuit,1,20)));
        assertFalse(new SatelliteMapper().onPartDelivered(world,module));
    }
    @Test void clientCannotInstallModulesOrQueueWork() {
        SatelliteScience science = new SatelliteScience(); TestWorld world = new TestWorld(true);
        assertFalse(science.onPartDelivered(world,module())); assertFalse(science.onPartDelivered(world,work()));
        assertEquals(0,science.assemblerCount); assertTrue(science.assemblerTasks.isEmpty());
    }
    @Test void noModuleMeansQueueWaitsWithoutProgress() {
        SatelliteScience science = ordered(0); assertFalse(science.tickAssembly());
        assertEquals(0,science.assemblerProgress); assertEquals(1,science.assemblerTasks.size());
        assertEquals(0,science.requestableSlots.length);
    }
    @Test void fullDurationProducesExactlyOneCrystalCircuit() {
        SatelliteScience science = ordered(1);
        for(int i=0;i<3599;i++) assertTrue(science.tickAssembly());
        assertEquals(0,science.requestableSlots.length); assertEquals(1,science.assemblerTasks.size());
        assertTrue(science.tickAssembly()); assertEquals(0,science.assemblerProgress);
        assertTrue(science.assemblerTasks.isEmpty()); assertEquals(1,science.requestableSlots.length);
        assertSame(ModItems.circuit,science.requestableSlots[0].getItem()); assertEquals(20,science.requestableSlots[0].getMetadata());
        assertEquals(1,science.requestableSlots[0].getCount()); assertFalse(science.tickAssembly());
    }
    @Test void twoModulesFinishIn1800TicksWithoutFloatingPointExtraTick() {
        SatelliteScience science=ordered(2); for(int i=0;i<1799;i++)science.tickAssembly();
        assertEquals(0,science.requestableSlots.length); science.tickAssembly(); assertEquals(1,science.requestableSlots.length);
    }
    @Test void readyCargoBlocksNextOrderUntilSuccessfulPickup() {
        SatelliteScience science=ordered(3600); science.onPartDelivered(new TestWorld(false),work()); science.tickAssembly();
        assertEquals(1,science.assemblerTasks.size()); assertFalse(science.tickAssembly());
        TestWorld world=new TestWorld(false); assertTrue(science.tryRequestItems(world,3,100,7));
        assertEquals(0,science.requestableSlots.length); assertTrue(science.tickAssembly()); assertEquals(1,science.requestableSlots.length);
        EntitySatellitePod pod=(EntitySatellitePod)world.spawned;
        assertEquals(300,pod.posY); assertEquals(100,pod.callerYPos); assertEquals(20,pod.slots[0].getMetadata());
    }
    @Test void cancelledPodSpawnRetainsCargoAndCannotDuplicatePickup() {
        SatelliteScience science=ordered(3600);science.tickAssembly();TestWorld world=new TestWorld(false);world.allowSpawn=false;
        assertFalse(science.tryRequestItems(world,0,100,0));assertEquals(1,science.requestableSlots[0].getCount());
        world.allowSpawn=true;assertTrue(science.tryRequestItems(world,0,100,0));assertFalse(science.tryRequestItems(world,0,100,0));
        assertEquals(2,world.spawnCalls);
    }
    @Test void fifoProgressAndOrderKeysSurviveNbtIncludingMissingRecipe() {
        SatelliteScience science=ordered(2);for(int i=0;i<413;i++)science.tickAssembly();science.assemblerTasks.add("custom.missing");
        NBTTagCompound tag=new NBTTagCompound();science.writeToNBT(tag);SatelliteScience restored=new SatelliteScience();restored.readFromNBT(tag);
        assertEquals(2,restored.assemblerCount);assertEquals(science.assemblerProgress,restored.assemblerProgress,0);
        assertEquals(science.assemblerTasks,restored.assemblerTasks);for(int i=413;i<1800;i++)restored.tickAssembly();
        assertEquals(1,restored.requestableSlots.length);restored.requestableSlots=new ItemStack[0];
        assertFalse(restored.tickAssembly());assertEquals("custom.missing",restored.assemblerTasks.get(0));
    }
    @Test void readyCargoAndOldSaveDefaultsSurviveNbtWithoutAliasing() {
        SatelliteScience science=ordered(3600);science.tickAssembly();NBTTagCompound tag=new NBTTagCompound();science.writeToNBT(tag);
        SatelliteScience restored=new SatelliteScience();restored.readFromNBT(tag);assertEquals(20,restored.requestableSlots[0].getMetadata());
        restored.requestableSlots[0].shrink(1);assertEquals(1,science.requestableSlots[0].getCount());
        restored.readFromNBT(new NBTTagCompound());assertEquals(0,restored.assemblerCount);assertEquals(0,restored.assemblerProgress);
        assertTrue(restored.assemblerTasks.isEmpty());assertEquals(0,restored.requestableSlots.length);
    }
    @Test void laboratoryTickAdvancesBothSubsystemsAndCentralSavePersistsAssembly() {
        SatelliteScience science=ordered(1);science.sensorCount=3;TestWorld world=new TestWorld(false);
        SatelliteSavedData data=SatelliteSavedData.getData(world);data.putSatellite(58520,science);long revision=data.getRevision();
        data.tickScience(world);assertEquals(3,science.sensorProgress);assertTrue(science.assemblerProgress>0);assertEquals(revision+1,data.getRevision());
        NBTTagCompound tag=data.writeToNBT(new NBTTagCompound());SatelliteSavedData restored=new SatelliteSavedData();restored.readFromNBT(tag);
        SatelliteScience copy=(SatelliteScience)restored.resolveSatellite(data.getOrbitKey(),null,58520).getSatellite();
        assertEquals(science.assemblerProgress,copy.assemblerProgress,0);assertEquals(science.assemblerTasks,copy.assemblerTasks);
    }
    @Test void podCopiesCargoAndRetainsFlightAndCargoInNbt() {
        TestWorld world=new TestWorld(false);ItemStack stack=new ItemStack(ModItems.circuit,3,20);
        EntitySatellitePod pod=new EntitySatellitePod(world).setup(140,stack);stack.shrink(2);pod.timer=37;pod.speed=0.125;pod.setDeployLegs(true);
        NBTTagCompound tag=new NBTTagCompound();pod.writeEntityToNBT(tag);EntitySatellitePod restored=new EntitySatellitePod(world);restored.readEntityFromNBT(tag);
        assertEquals(3,restored.slots[0].getCount());assertEquals(20,restored.slots[0].getMetadata());assertEquals(37,restored.timer);
        assertEquals(140,restored.callerYPos);assertEquals(0.125,restored.speed);assertTrue(restored.doesDeployLegs());assertTrue(restored.isLanding());
    }
    @Test void assemblerRecipePreservesOriginalInputsTimingAndCosts() {
        DataConsumerRecipeTest.defaults(false,false);GenericRecipe recipe=AssemblyMachineRecipes.INSTANCE.recipeNameMap.get("ass.0gassembler");
        assertNotNull(recipe);assertEquals(600,recipe.duration);assertEquals(25000,recipe.power);int[] counts={4,4,16,4,4,4};
        assertEquals(counts.length,recipe.inputItem.length);for(int i=0;i<counts.length;i++){assertEquals(counts[i],recipe.inputItem[i].stacksize);assertFalse(recipe.inputItem[i].getStackList().isEmpty());}
        assertEquals(13,recipe.outputItem[0].getSingle().getMetadata());
    }
    @Test void workPackageNormalAndExpensiveRecipesKeepOriginalYieldsAndFullRecycling() {
        for(boolean expensive:new boolean[]{false,true}) {
            GeneralConfig.enableExpensiveMode=expensive;PrecAssRecipes.INSTANCE.deleteRecipes();PrecAssRecipes.INSTANCE.registerDefaults();
            GenericRecipe recipe=PrecAssRecipes.INSTANCE.recipeNameMap.get("precass.crystalcircuit");assertNotNull(recipe);
            assertEquals(600,recipe.duration);assertEquals(20000,recipe.power);assertEquals(expensive?5:4,recipe.inputItem.length);
            assertEquals(expensive?8:4,recipe.inputItem[0].stacksize);ChanceOutputMulti pool=(ChanceOutputMulti)recipe.outputItem[0];
            assertEquals(expensive?25:50,pool.pool.get(0).itemWeight);assertSame(ModItems.orbital_assembly,pool.pool.get(0).stack.getItem());
            GenericRecipe recycle=PrecAssRecipes.INSTANCE.recipeNameMap.get("precass.crystalcircuit.recycle");assertNotNull(recycle);
            assertEquals(recipe.inputItem.length,recycle.outputItem.length);
            for(int i=0;i<recipe.inputItem.length;i++)assertEquals(recipe.inputItem[i].stacksize,recycle.outputItem[i].collapse().getCount());
        }
    }
    @Test void orbitalRecipeIsConfigurableAndRecognizesOnlyItsWorkPackage() {
        SpaceAssemblerRecipes recipes=SpaceAssemblerRecipes.INSTANCE;assertFalse(recipes.hasPower());assertEquals(9,recipes.outputItemLimit());
        GenericRecipe recipe=recipes.getRecipe(work());assertNotNull(recipe);assertEquals(3600,recipe.duration);assertEquals(0,recipe.power);
        assertNull(recipes.getRecipe(module()));assertNull(recipes.getRecipe(ItemStack.EMPTY));assertNull(recipes.getRecipe(new ItemStack(ModItems.orbital_assembly,1,99)));
    }
    private static class TestWorld extends World {
        private final MapStorage storage=new MapStorage(null);boolean allowSpawn=true;Entity spawned;int spawnCalls;
        TestWorld(boolean client){super(new SaveHandlerMP(),new WorldInfo(new NBTTagCompound()),new WorldProviderSurface(),new Profiler(),client);}
        @Override public boolean spawnEntity(Entity entity){spawnCalls++;spawned=entity;return allowSpawn;}
        @Override public MapStorage getPerWorldStorage(){return storage;}
        @Override protected IChunkProvider createChunkProvider(){return null;}
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty){return true;}
    }
}
