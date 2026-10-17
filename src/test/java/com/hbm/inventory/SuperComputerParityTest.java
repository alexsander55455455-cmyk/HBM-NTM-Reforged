package com.hbm.inventory;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.SuperComputerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes.ChanceOutputMulti;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.render.misc.EnumSymbol;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityMachineSuperComputer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.StringWriter;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SuperComputerParityTest {
    @BeforeAll static void bootstrap() throws Exception {
        AutoCrystallizerOreDictionaryTest.bootstrap();
        com.hbm.main.MainRegistry.proxy = new com.hbm.main.ServerProxy();
        for(String name : new String[]{"NONE","WATER","SPENTSTEAM","PERFLUOROMETHYL_COLD","PERFLUOROMETHYL","HELIUM4","SLOP"}) {
            java.lang.reflect.Field field = Fluids.class.getField(name);
            if(field.get(null) == null) field.set(null, new FluidType(name, 0x888888, 0, 0, 0, EnumSymbol.NONE));
        }
        SuperComputerRecipes.INSTANCE.deleteRecipes();SuperComputerRecipes.INSTANCE.registerDefaults();
        GameRegistry.registerTileEntity(Computer.class, new ResourceLocation("hbm","test_supercomputer"));
    }
    private static GenericRecipe recipe(String name) { return SuperComputerRecipes.INSTANCE.recipeNameMap.get("com."+name); }
    private static Computer computer(String name) {
        Computer c=new Computer();c.computerModule.setRecipe("com."+name,false);c.computerModule.setupTanks(recipe(name));return c;
    }
    private static void load(Computer c,GenericRecipe r) {
        if(r.inputItem!=null)for(int i=0;i<r.inputItem.length;i++)c.inventory.setStackInSlot(i+2,r.inputItem[i].getStackList().get(0));
        if(r.inputFluid!=null)c.inputTank.setFill(r.inputFluid[0].fill);
        c.power=r.power*(r.duration+1L);c.maxPower=c.power;
    }
    private static void tick(Computer c) { c.computerModule.update(1,1,true,ItemStack.EMPTY); }
    private static void weights(String name,int success,int failure) {
        ChanceOutputMulti out=(ChanceOutputMulti)recipe(name).outputItem[0];assertEquals(success,out.pool.get(0).itemWeight);assertEquals(failure,out.pool.get(1).itemWeight);
    }
    @Test void allElevenDriveOrdinalsAndNamesRemainOriginal() {
        assertArrayEquals(new String[]{"FLASH_EMPTY","DISK_EMPTY","FLASH_BROKEN","DISK_BROKEN","FLASH_FLIGHTSIM","FLASH_PARTICLESIM","DISK_FLIGHTDATA","DISK_FLIGHTDATA_PROCESSED","DISK_ORBITDATA","DISK_ORBITDATA_PROCESSED","KLAUS"},Arrays.stream(EnumDriveType.values()).map(Enum::name).toArray(String[]::new));
        for(EnumDriveType type:EnumDriveType.values())assertEquals("item.drive."+type.name().toLowerCase(Locale.US),ModItems.drive.getTranslationKey(new ItemStack(ModItems.drive,1,type.ordinal())));
    }
    @Test void allEighteenDefaultsHaveUsableInputsAndNoUnrequestedAutoSwitchGroups() {
        assertEquals(18,SuperComputerRecipes.INSTANCE.recipeNameMap.size());
        for(GenericRecipe r:SuperComputerRecipes.INSTANCE.recipeOrderedList){assertTrue(r.duration>0&&r.power>0);assertNull(r.autoSwitchGroup);assertTrue(r.inputItem.length<=3);for(RecipesCommon.AStack input:r.inputItem)assertFalse(input.getStackList().isEmpty(),r.getInternalName());assertTrue(r.outputItem.length<=3);assertNotNull(r.getIcon());}
    }
    @Test void simulationDurationsAndFailureWeightsMatchAllThreeCoolants() {
        for(String prefix:new String[]{"flightcalc","particlecalc"}){assertEquals(18000,recipe(prefix+"_water").duration);assertEquals(3000,recipe(prefix+"_pfm").duration);assertEquals(1200,recipe(prefix+"_helium").duration);weights(prefix+"_water",95,5);weights(prefix+"_pfm",50,50);weights(prefix+"_helium",25,75);}
    }
    @Test void processingDurationsAndProbabilitiesMatchTheOriginal() {
        String[] coolants={"water","pfm","helium"};int[] flight={36000,18000,2400},orbit={72000,36000,6000},flightChance={99,95,90},orbitChance={75,65,50};
        for(int i=0;i<3;i++){assertEquals(flight[i],recipe("processflight_"+coolants[i]).duration);assertEquals(orbit[i],recipe("processorbit_"+coolants[i]).duration);weights("processflight_"+coolants[i],flightChance[i],100-flightChance[i]);weights("processorbit_"+coolants[i],orbitChance[i],100-orbitChance[i]);}
    }
    @Test void coolingConsumesSixteenBucketsAndProducesOnlyTheOriginalSpentFluids() {
        for(String prefix:new String[]{"flightcalc","particlecalc","processflight","processorbit"}){
            GenericRecipe water=recipe(prefix+"_water"),pfm=recipe(prefix+"_pfm"),helium=recipe(prefix+"_helium");
            for(GenericRecipe r:new GenericRecipe[]{water,pfm,helium}){assertEquals(16000,r.inputFluid[0].fill);assertEquals(10000,r.power);}
            assertSame(Fluids.WATER,water.inputFluid[0].type);assertSame(Fluids.SPENTSTEAM,water.outputFluid[0].type);assertEquals(16000,water.outputFluid[0].fill);
            assertSame(Fluids.PERFLUOROMETHYL_COLD,pfm.inputFluid[0].type);assertSame(Fluids.PERFLUOROMETHYL,pfm.outputFluid[0].type);assertEquals(16000,pfm.outputFluid[0].fill);
            assertSame(Fluids.HELIUM4,helium.inputFluid[0].type);assertNull(helium.outputFluid);
        }
    }
    @Test void copyingConsumesOriginalAndBlankToProduceExactlyTwoDrives() {
        for(String name:new String[]{"copyflightcalc","copyparticlecalc","copyfligthdata"}){GenericRecipe r=recipe(name);assertNotNull(r);assertEquals(18000,r.duration);assertEquals(2,r.inputItem.length);assertEquals(2,((ChanceOutputMulti)r.outputItem[0]).pool.get(0).stack.getCount());assertEquals(2,((ChanceOutputMulti)r.outputItem[0]).pool.get(1).stack.getCount());assertEquals("com.copy",r.nameWrapper);}
        weights("copyflightcalc",95,5);weights("copyparticlecalc",95,5);weights("copyfligthdata",75,25);
    }
    @Test void blueprintRecoveryAndKlausUseTheOriginalCostsAndWeights() {
        weights("blueprints",20,80);weights("beigeprints",10,90);assertEquals(16,recipe("blueprints").inputItem[0].stacksize);assertEquals(24,recipe("beigeprints").inputItem[0].stacksize);
        GenericRecipe klaus=recipe("klaus");assertEquals(72000,klaus.duration);assertEquals(5000000,klaus.power);assertEquals(3,klaus.inputItem.length);for(RecipesCommon.AStack input:klaus.inputItem)assertEquals(64,input.stacksize);assertEquals(1000000,klaus.inputFluid[0].fill);assertEquals(1000,klaus.outputFluid[0].fill);assertSame(Fluids.SLOP,klaus.outputFluid[0].type);assertEquals(10,klaus.outputItem[0].getSingle().getItemDamage());
    }
    @Test void allEighteenRecipesCompleteWithExactPerTickEnergyAndInputOutputConservation() {
        GenericRecipes.RNG.setSeed(5846);
        for(GenericRecipe r:SuperComputerRecipes.INSTANCE.recipeOrderedList){Computer c=computer(r.getInternalName().substring(4));load(c,r);long initial=c.power;int ticks=0;while(c.inventory.getStackInSlot(5).isEmpty()&&ticks<=r.duration){tick(c);ticks++;}
            assertFalse(c.inventory.getStackInSlot(5).isEmpty(),r.getInternalName());assertTrue(ticks>=r.duration&&ticks<=r.duration+1,r.getInternalName());assertEquals(r.power*ticks,initial-c.power,r.getInternalName());for(int i=2;i<=4;i++)assertTrue(c.inventory.getStackInSlot(i).isEmpty(),r.getInternalName());if(r.inputFluid!=null)assertEquals(0,c.inputTank.getFill(),r.getInternalName());assertEquals(r.outputFluid==null?0:r.outputFluid[0].fill,c.outputTank.getFill(),r.getInternalName());
        }
    }
    @Test void insufficientPowerAndFluidPreserveAllResources() {
        Computer c=computer("flightcalc_water");load(c,recipe("flightcalc_water"));c.power=9999;tick(c);assertEquals(9999,c.power);assertEquals(16000,c.inputTank.getFill());assertEquals(1,c.inventory.getStackInSlot(2).getCount());c.power=10000;c.inputTank.setFill(15999);tick(c);assertEquals(10000,c.power);assertEquals(15999,c.inputTank.getFill());
    }
    @Test void blockedItemOrSpentFluidOutputPreventsConsumption() {
        Computer c=computer("flightcalc_water");load(c,recipe("flightcalc_water"));c.inventory.setStackInSlot(5,new ItemStack(Items.DIAMOND));long energy=c.power;tick(c);assertEquals(energy,c.power);c.inventory.setStackInSlot(5,ItemStack.EMPTY);c.outputTank.setFill(20000);tick(c);assertEquals(energy,c.power);assertEquals(16000,c.inputTank.getFill());
    }
    @Test void sameFluidTankResizeKeepsExistingContentsAndKlausAllocatesTwoMillion() {
        Computer c=computer("klaus");assertEquals(2000000,c.inputTank.getMaxFill());c.inputTank.setFill(1700000);c.computerModule.setupTanks(recipe("flightcalc_water"));assertEquals(1700000,c.inputTank.getFill());assertEquals(1700000,c.inputTank.getMaxFill());
    }
    @Test void checkedGuiSlotsRejectForeignInputsAndProtectOutputs() {
        Computer c=computer("flightcalc_water");assertFalse(c.getCheckedInventory().insertItem(0,new ItemStack(Items.DIAMOND),false).isEmpty());assertFalse(c.getCheckedInventory().insertItem(1,new ItemStack(Items.DIAMOND),false).isEmpty());assertFalse(c.getCheckedInventory().insertItem(2,new ItemStack(ModItems.drive,1,EnumDriveType.DISK_EMPTY.ordinal()),false).isEmpty());assertTrue(c.getCheckedInventory().insertItem(2,new ItemStack(ModItems.drive,1,EnumDriveType.FLASH_EMPTY.ordinal()),false).isEmpty());assertFalse(c.getCheckedInventory().insertItem(5,new ItemStack(ModItems.drive),false).isEmpty());
    }
    @Test void originalFivePortOffsetsRotateWithAllFourCoreDirections() {
        for(int meta=12;meta<=15;meta++){Computer c=new Computer();c.metadata=meta;c.setPos(new BlockPos(10,20,30));Set<BlockPos> positions=new HashSet<>();for(PortDef port:c.getPorts()){assertTrue(positions.add(port.portPositions[0]));BlockPos relative=port.portPositions[0].subtract(c.getPos());assertTrue(Math.abs(relative.getX())+Math.abs(relative.getZ())==6||Math.abs(relative.getX())+Math.abs(relative.getZ())==8);assertNotEquals(port.portPositions[0],port.portConnections[0].getPos());}assertEquals(5,positions.size());}
    }
    @Test void nbtPreservesSelectedCalculationLargeBuffersAndProgress() {
        Computer c=computer("klaus");load(c,recipe("klaus"));c.computerModule.progress=.375;Computer restored=new Computer();restored.readFromNBT(c.writeToNBT(new NBTTagCompound()));assertEquals(c.power,restored.power);assertEquals(1000000,restored.inputTank.getFill());assertEquals(2000000,restored.inputTank.getMaxFill());assertEquals("com.klaus",restored.computerModule.recipe);assertEquals(.375,restored.computerModule.progress);assertEquals(64,restored.inventory.getStackInSlot(2).getCount());assertEquals(ModItems.drive,restored.inventory.getStackInSlot(2).getItem());
    }
    @Test void packetRoundtripConsumesTheWholeBuffer() {
        Computer c=computer("flightcalc_pfm");c.power=56789;c.maxPower=1000000;c.inputTank.setFill(12345);c.didProcess=true;c.computerModule.progress=.25;ByteBuf buffer=Unpooled.buffer();try{c.serialize(buffer);Computer restored=new Computer();restored.deserialize(buffer);assertEquals(56789,restored.power);assertEquals(1000000,restored.maxPower);assertEquals(12345,restored.inputTank.getFill());assertSame(Fluids.PERFLUOROMETHYL_COLD,restored.inputTank.getTankType());assertTrue(restored.didProcess);assertEquals(.25,restored.computerModule.progress);assertEquals("com.flightcalc_pfm",restored.computerModule.recipe);assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
    }
    @Test void jsonConfigurationRoundtripsAllEighteenRecipesAndWeights() throws Exception {
        SuperComputerRecipes copy=new SuperComputerRecipes();for(GenericRecipe r:SuperComputerRecipes.INSTANCE.recipeOrderedList){StringWriter text=new StringWriter();try(JsonWriter writer=new JsonWriter(text)){writer.beginObject();SuperComputerRecipes.INSTANCE.writeRecipe(r,writer);writer.endObject();}copy.readRecipe(new JsonParser().parse(text.toString()));}assertEquals(18,copy.recipeNameMap.size());assertEquals(5000000,copy.recipeNameMap.get("com.klaus").power);assertEquals(99,((ChanceOutputMulti)copy.recipeNameMap.get("com.processflight_water").outputItem[0]).pool.get(0).itemWeight);
    }
    @Test void weightedSimulationOutputsStayWithinOriginalResultSet() {
        GenericRecipes.RNG.setSeed(5846);ChanceOutputMulti out=(ChanceOutputMulti)recipe("flightcalc_pfm").outputItem[0];int good=0;for(int i=0;i<10000;i++){ItemStack result=out.collapse();assertEquals(ModItems.drive,result.getItem());assertEquals(1,result.getCount());int meta=result.getItemDamage();assertTrue(meta==EnumDriveType.FLASH_FLIGHTSIM.ordinal()||meta==EnumDriveType.FLASH_BROKEN.ordinal());if(meta==EnumDriveType.FLASH_FLIGHTSIM.ordinal())good++;}assertTrue(good>4700&&good<5300);
    }
    public static class Computer extends TileEntityMachineSuperComputer {
        int metadata=12;
        @Override public int getBlockMetadata(){return metadata;}
        @Override public void markDirty() { }
        @Override public void markChanged() { }
    }
}
