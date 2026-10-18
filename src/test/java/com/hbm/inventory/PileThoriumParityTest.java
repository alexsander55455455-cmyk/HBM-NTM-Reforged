package com.hbm.inventory;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import static org.junit.jupiter.api.Assertions.*;

class PileThoriumParityTest {
    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap();
        GameRegistry.registerTileEntity(TileEntityPileLoader.class,new ResourceLocation("hbm","test_pile_loader"));
        GameRegistry.registerTileEntity(TileEntityPileControl.class,new ResourceLocation("hbm","test_pile_control"));
    }
    private static ItemStack rod(int meta){return new ItemStack(ModItems.pile_rod,1,meta);}
    private static Arguments args(final Object...values){return (Arguments)Proxy.newProxyInstance(Arguments.class.getClassLoader(),new Class<?>[]{Arguments.class},(p,m,a)->m.getName().equals("checkDouble")?((Number)values[(Integer)a[0]]).doubleValue():null);}
    private static Object[] call(Object tile,String name,Object...values) throws Exception {
        try{return (Object[])tile.getClass().getMethod(name,Context.class,Arguments.class).invoke(tile,null,args(values));}
        catch(InvocationTargetException e){if(e.getCause() instanceof Exception)throw (Exception)e.getCause();throw e;}
    }
    private static String[] methods(Object tile)throws Exception{return (String[])tile.getClass().getMethod("methods").invoke(tile);}
    private static Object[] dispatch(Object tile,String name,Object...values)throws Exception {
        try{return (Object[])tile.getClass().getMethod("invoke",String.class,Context.class,Arguments.class).invoke(tile,name,null,args(values));}
        catch(InvocationTargetException e){if(e.getCause() instanceof Exception)throw (Exception)e.getCause();throw e;}
    }
    @Test void sevenOldRodMetadataValuesDoNotMove(){String[] names={"RA226BE","PO210BE","ZR","NU","PU239","RGP","WASTE"};for(int i=0;i<names.length;i++)assertEquals(i,EnumPileRod.valueOf(names[i]).ordinal());}
    @Test void thoriumTypesAppendTheirPinnedOriginalParameters(){
        EnumPileRod a=EnumPileRod.valueOf("THORIUM"),b=EnumPileRod.valueOf("THORIUM_FUEL");assertEquals(7,a.ordinal());assertEquals(8,b.ordinal());assertEquals(35000,a.life);assertEquals(2000,b.life);assertEquals(8,a.turnsInto);assertEquals(6,b.turnsInto);assertEquals(.25,a.heatMult);assertEquals(.5,b.heatMult);assertEquals(1,a.reactionMult);assertEquals(1,b.reactionMult);assertEquals(0,a.neutronSource);
    }
    @Test void thoriumBreedsAt35000AndStartsFreshFuelDepletion(){ItemStack r=rod(7);ItemPileRodMK2.setDepletion(r,34998);assertSame(r,ItemPileRodMK2.react(r,1));assertEquals(34999,ItemPileRodMK2.getDepletion(r));ItemStack b=ItemPileRodMK2.react(r,1);assertEquals(8,b.getMetadata());assertEquals(0,ItemPileRodMK2.getDepletion(b));assertEquals(1,b.getCount());}
    @Test void thoriumFuelBecomesExistingWasteAt2000(){ItemStack r=rod(8);ItemPileRodMK2.setDepletion(r,1999);ItemStack b=ItemPileRodMK2.react(r,1);assertEquals(6,b.getMetadata());assertEquals(0,ItemPileRodMK2.getDepletion(b));}
    @Test void newRodsKeepRealFluxAndHeatCoefficients(){assertEquals(ItemPileRodMK2.getReactivity(rod(3),17),ItemPileRodMK2.getReactivity(rod(7),17));assertEquals(.25,ItemPileRodMK2.getHeatPerNeutron(rod(7)));assertEquals(.5,ItemPileRodMK2.getHeatPerNeutron(rod(8)));}
    @Test void negativeFluxDoesNotReverseOwnDepletionGuard(){ItemStack r=rod(7);ItemPileRodMK2.setDepletion(r,100);assertSame(r,ItemPileRodMK2.react(r,-50));assertEquals(100,ItemPileRodMK2.getDepletion(r));}
    @Test void partialThoriumSurvivesItemNbtAndContinuesBreeding(){ItemStack r=rod(7);ItemPileRodMK2.setDepletion(r,34500);ItemStack copy=new ItemStack(r.writeToNBT(new NBTTagCompound()));assertEquals(34500,ItemPileRodMK2.getDepletion(copy));assertEquals(8,ItemPileRodMK2.react(copy,500).getMetadata());}
    @Test void loaderEmptyTelemetryDoesNotInventFuel()throws Exception{TileEntityPileLoader l=new TileEntityPileLoader();l.channelDepletion=99;assertEquals(0D,call(l,"getLifetime")[0]);assertEquals(0D,call(l,"getDepletion")[0]);assertEquals(-1,call(l,"getType")[0]);assertEquals(-1,call(l,"getLoadingType")[0]);}
    @Test void loaderReadsRealThoriumChannelPercentLifetimeAndTemperature()throws Exception{TileEntityPileLoader l=new TileEntityPileLoader();l.channelStack=rod(7);l.channelDepletion=37.5;l.channelTemp=123.75;assertEquals(7,call(l,"getType")[0]);assertEquals(35000D,call(l,"getLifetime")[0]);assertEquals(37.5,call(l,"getDepletion")[0]);assertEquals(123.75,call(l,"getTemp")[0]);l.channelStack=rod(8);assertEquals(2000D,call(l,"getLifetime")[0]);}
    @Test void loadingFuelTypeReadsInventoryRatherThanChannel()throws Exception{TileEntityPileLoader l=new TileEntityPileLoader();l.channelStack=rod(7);l.setStack(rod(8));assertEquals(7,call(l,"getType")[0]);assertEquals(8,call(l,"getLoadingType")[0]);assertTrue(TileEntityPileLoader.isItemLoadable(l.getStack()));}
    @Test void loadStartsOnlyWhenRetractedAndReady()throws Exception{TileEntityPileLoader l=new TileEntityPileLoader();assertEquals(false,call(l,"isLoading")[0]);assertEquals(0,call(l,"load").length);assertTrue(l.loading);assertEquals(true,call(l,"isLoading")[0]);}
    @Test void loadRespectsBothDelayAndExtendedPiston()throws Exception{for(int test=0;test<2;test++){TileEntityPileLoader l=new TileEntityPileLoader();if(test==0)l.delay=1;else l.level=.1;call(l,"load");assertFalse(l.loading);}}
    @Test void loadingCommandAndRodInventorySurviveNbt()throws Exception{TileEntityPileLoader l=new TileEntityPileLoader();ItemStack r=rod(7);ItemPileRodMK2.setDepletion(r,123);l.setStack(r);call(l,"load");l.level=.4;l.delay=3;TileEntityPileLoader c=new TileEntityPileLoader();c.readFromNBT(l.writeToNBT(new NBTTagCompound()));assertTrue(c.loading);assertEquals(.4,c.level);assertEquals(3,c.delay);assertEquals(7,c.getStack().getMetadata());assertEquals(123,ItemPileRodMK2.getDepletion(c.getStack()));}
    @Test void loaderExposesAllSevenOriginalOperationsThroughProxyDispatch()throws Exception{TileEntityPileLoader l=new TileEntityPileLoader();assertArrayEquals(new String[]{"getTemp","getDepletion","getLifetime","getType","getLoadingType","isLoading","load"},methods(l));assertEquals("ntm_pile_loader",l.getClass().getMethod("getComponentName").invoke(l));assertEquals(-1,dispatch(l,"getType")[0]);dispatch(l,"load");assertTrue(l.loading);assertThrows(NoSuchMethodException.class,()->dispatch(l,"missing"));}
    @Test void stateChangingCallbacksUseTheServerThreadAndKeepOriginalLimit()throws Exception{for(Method m:new Method[]{TileEntityPileLoader.class.getMethod("load",Context.class,Arguments.class),TileEntityPileControl.class.getMethod("setLevel",Context.class,Arguments.class)}){Callback c=m.getAnnotation(Callback.class);assertNotNull(c);assertFalse(c.direct());assertEquals(4,c.limit());}}
    @Test void controlReturnsOriginalNormalizedLevel()throws Exception{TileEntityPileControl c=new TileEntityPileControl();c.level=.375;assertEquals(.375,call(c,"getLevel")[0]);}
    @Test void controlAcceptsPercentAndClampsToPhysicalTravel()throws Exception{TileEntityPileControl c=new TileEntityPileControl();call(c,"setLevel",35.5);assertEquals(.355,c.targetLevel);call(c,"setLevel",250);assertEquals(1,c.targetLevel);call(c,"setLevel",-20);assertEquals(0,c.targetLevel);}
    @Test void controlExposesBothOriginalOperationsThroughProxyDispatch()throws Exception{TileEntityPileControl c=new TileEntityPileControl();assertArrayEquals(new String[]{"getLevel","setLevel"},methods(c));assertEquals("ntm_pile_control",c.getClass().getMethod("getComponentName").invoke(c));dispatch(c,"setLevel",75);assertEquals(.75,c.targetLevel);assertThrows(NoSuchMethodException.class,()->dispatch(c,"missing"));}
    @Test void controlTargetAndPositionSurviveNbt()throws Exception{TileEntityPileControl c=new TileEntityPileControl();call(c,"setLevel",35);c.level=.125;TileEntityPileControl copy=new TileEntityPileControl();copy.readFromNBT(c.writeToNBT(new NBTTagCompound()));assertEquals(.35,copy.targetLevel);assertEquals(.125,copy.level);}
    @Test void existingRorControlAndLoaderValuesRemainAvailable()throws Exception{TileEntityPileControl c=new TileEntityPileControl();c.runRORFunction(IRORInteractive.PREFIX_FUNCTION+"setrods",new String[]{"20"});c.runRORFunction(IRORInteractive.PREFIX_FUNCTION+"extendrods",new String[]{"30"});assertEquals(.5,c.targetLevel);TileEntityPileLoader l=new TileEntityPileLoader();l.channelStack=rod(7);ItemPileRodMK2.setDepletion(l.channelStack,17500);l.channelDepletion=50;assertEquals("17500",l.provideRORValue(IRORValueProvider.PREFIX_VALUE+"depletion"));assertEquals("50",l.provideRORValue(IRORValueProvider.PREFIX_VALUE+"deppercent"));assertEquals("35000",l.provideRORValue(IRORValueProvider.PREFIX_VALUE+"lifetime"));}
    @Test void assemblerUsesThreeThoriumBilletsAndOriginalCost(){DataConsumerRecipeTest.defaults(false,false);GenericRecipe r=AssemblyMachineRecipes.INSTANCE.recipeNameMap.get("ass.pilethorium");assertNotNull(r);assertEquals(40,r.duration);assertEquals(200,r.power);assertEquals(1,r.inputItem.length);assertEquals(3,r.inputItem[0].stacksize);assertFalse(r.inputItem[0].getStackList().isEmpty());assertEquals(ModItems.pile_rod,r.outputItem[0].getSingle().getItem());assertEquals(7,r.outputItem[0].getSingle().getMetadata());assertEquals(1,r.outputItem[0].getSingle().getCount());}
    @Test void purexReturnsTwoFuelBilletsAndOneWasteWithOriginalAcid(){PUREXRecipes.INSTANCE.deleteRecipes();PUREXRecipes.INSTANCE.registerDefaults();GenericRecipe r=PUREXRecipes.INSTANCE.recipeNameMap.get("purex.pilethorium");assertNotNull(r);assertEquals(40,r.duration);assertEquals(8,r.inputItem[0].getStackList().get(0).getMetadata());assertEquals(100,r.inputFluid[0].fill);assertSame(Fluids.SULFURIC_ACID,r.inputFluid[0].type);assertSame(ModItems.billet_thorium_fuel,r.outputItem[0].getSingle().getItem());assertEquals(2,r.outputItem[0].getSingle().getCount());assertSame(ModItems.billet_nuclear_waste,r.outputItem[1].getSingle().getItem());assertEquals(1,r.outputItem[1].getSingle().getCount());}
}
