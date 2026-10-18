package com.hbm.inventory;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIDMulti;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.tileentity.machine.TileEntityPWRController;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import com.hbm.tileentity.machine.TileEntityICF;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKHeater;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import static org.junit.jupiter.api.Assertions.*;

class ReactorParityTest {
    @BeforeAll static void bootstrap() throws Exception { DataConsumerRecipeTest.bootstrap(); }
    private static Arguments args(final int value) { return (Arguments) Proxy.newProxyInstance(Arguments.class.getClassLoader(), new Class<?>[]{Arguments.class}, (p,m,a)->m.getName().equals("checkInteger") ? value : null); }
    private static TileEntityRBMKBoiler boiler(double heat,int feed) {
        TileEntityRBMKBoiler b=new TileEntityRBMKBoiler();b.heat=heat;b.feed.setFill(feed);b.steam.setTankType(Fluids.STEAM);b.steam.setFill(1234);return b;
    }
    // Stop at the next subsystem so the real selector and update-entry guard can
    // be tested without invoking unrelated chunk/network/simulation operations.
    private static class SelectorBoundary extends RuntimeException { }
    private static class SelectorTank extends FluidTankNTM {
        SelectorTank(){super(Fluids.COOLANT,128000);setFill(1234);}
        @Override public FluidType getTankType(){throw new SelectorBoundary();}
        FluidType actualType(){return super.getTankType();}
    }
    private static World selectorWorld() throws Exception {
        Class<?> type=Class.forName("sun.misc.Unsafe");Field f=type.getDeclaredField("theUnsafe");f.setAccessible(true);Object unsafe=f.get(null);
        return (World)type.getMethod("allocateInstance",Class.class).invoke(unsafe,WorldServer.class);
    }
    private static ItemStack selector(){ItemStack s=new ItemStack(ModItems.fluid_identifier_multi);ItemFluidIDMulti.setType(s,Fluids.WATER,true);return s;}
    private static SelectorTank pwrSelector(int loaded)throws Exception {
        TileEntityPWRController p=new TileEntityPWRController();SelectorTank t=new SelectorTank();p.amountLoaded=loaded;p.tanks[0]=t;p.inventory.setStackInSlot(2,selector());p.setWorld(selectorWorld());assertThrows(SelectorBoundary.class,p::update);return t;
    }
    private static SelectorTank heaterSelector(double heat,int fill)throws Exception {
        TileEntityRBMKHeater h=new TileEntityRBMKHeater();SelectorTank t=new SelectorTank();t.setFill(fill);h.heat=heat;h.feed=t;h.inventory.setStackInSlot(0,selector());h.setWorld(selectorWorld());assertThrows(SelectorBoundary.class,h::update);return t;
    }
    @Test void pwrLoadedFuelLocksRealIdentifierAndRetainsCoolant()throws Exception{SelectorTank t=pwrSelector(1);assertSame(Fluids.COOLANT,t.actualType());assertEquals(1234,t.getFill());}
    @Test void pwrEmptyFuelAllowsRealIdentifier()throws Exception{SelectorTank t=pwrSelector(0);assertSame(Fluids.WATER,t.actualType());assertEquals(0,t.getFill());}
    @Test void heaterHotFilledColumnLocksRealIdentifier()throws Exception{SelectorTank t=heaterSelector(50.001,1);assertSame(Fluids.COOLANT,t.actualType());assertEquals(1,t.getFill());}
    @Test void heaterAtExactly50AllowsIdentifier()throws Exception{assertSame(Fluids.WATER,heaterSelector(50,1).actualType());}
    @Test void heaterHotEmptyInputAllowsIdentifier()throws Exception{assertSame(Fluids.WATER,heaterSelector(500,0).actualType());}
    @Test void boilerHotFilledColumnRetainsSteamTypeAndBothBuffers(){TileEntityRBMKBoiler b=boiler(50.001,1);b.cyceCompressor();assertSame(Fluids.STEAM,b.steam.getTankType());assertEquals(1234,b.steam.getFill());assertEquals(1,b.feed.getFill());}
    @Test void boilerAtExactly50CanChangeCompression(){TileEntityRBMKBoiler b=boiler(50,1);b.cyceCompressor();assertSame(Fluids.HOTSTEAM,b.steam.getTankType());}
    @Test void boilerHotEmptyInputCanChangeCompression(){TileEntityRBMKBoiler b=boiler(500,0);b.cyceCompressor();assertSame(Fluids.HOTSTEAM,b.steam.getTankType());}
    @Test void boilerGuiControlCannotBypassLock(){TileEntityRBMKBoiler b=boiler(60,100);NBTTagCompound n=new NBTTagCompound();n.setBoolean("compression",true);b.receiveControl(n);assertSame(Fluids.STEAM,b.steam.getTankType());assertEquals(1234,b.steam.getFill());}
    @Test void boilerLuaCannotBypassLock(){TileEntityRBMKBoiler b=boiler(60,100);assertEquals(false,b.setSteamType(null,args(2))[0]);assertSame(Fluids.STEAM,b.steam.getTankType());assertEquals(1234,b.steam.getFill());}
    @Test void boilerLuaCanConfigureHotEmptyColumn(){TileEntityRBMKBoiler b=boiler(500,0);assertEquals(true,b.setSteamType(null,args(2))[0]);assertSame(Fluids.SUPERHOTSTEAM,b.steam.getTankType());}
    @Test void boilerLuaMutationUsesServerThread()throws Exception{Callback a=TileEntityRBMKBoiler.class.getMethod("setSteamType",li.cil.oc.api.machine.Context.class,Arguments.class).getAnnotation(Callback.class);assertFalse(a.direct());assertEquals(4,a.limit());}
    private static TileEntityReactorZirnox steam(int heat,int co2)throws Exception {
        TileEntityReactorZirnox z=new TileEntityReactorZirnox();z.heat=heat;z.water.setFill(1000);z.carbonDioxide.setFill(co2);Method m=z.getClass().getDeclaredMethod("generateSteam");m.setAccessible(true);m.invoke(z);return z;
    }
    @Test void zirnoxOriginalIncreasedSteamConsumesExactWater()throws Exception{TileEntityReactorZirnox z=steam(100000,14000);assertEquals(168,z.steam.getFill());assertEquals(832,z.water.getFill());}
    @Test void zirnoxIncreasedSteamStillScalesWithCarbonDioxide()throws Exception{TileEntityReactorZirnox z=steam(100000,7000);assertEquals(84,z.steam.getFill());assertEquals(916,z.water.getFill());}
    @Test void zirnoxBelowThresholdProducesNoSteam()throws Exception{TileEntityReactorZirnox z=steam(10256,14000);assertEquals(0,z.steam.getFill());assertEquals(1000,z.water.getFill());}
    @Test void zirnoxExistingRadioSlotReadsRealTypeAndDepletion(){TileEntityReactorZirnox z=new TileEntityReactorZirnox();ItemStack rod=new ItemStack(ModItems.rod_zirnox,1,1);ItemZirnoxRod.setLifeTime(rod,123);z.inventory.setStackInSlot(23,rod);z.runRORFunction(IRORInteractive.PREFIX_FUNCTION+"setslot",new String[]{"23"});assertEquals("URANIUM_FUEL",z.provideRORValue(IRORValueProvider.PREFIX_VALUE+"slottype"));assertEquals("123",z.provideRORValue(IRORValueProvider.PREFIX_VALUE+"slotdep"));assertEquals("200000",z.provideRORValue(IRORValueProvider.PREFIX_VALUE+"slotmaxdep"));}
    @Test void pwrExistingRadioReadsBothActualCoolantBuffers(){TileEntityPWRController p=new TileEntityPWRController();p.tanks[0].setFill(123);p.tanks[1].setFill(456);assertEquals("123",p.provideRORValue(IRORValueProvider.PREFIX_VALUE+"coldbuf"));assertEquals("456",p.provideRORValue(IRORValueProvider.PREFIX_VALUE+"hotbuf"));}
    private static TileEntityFusionTorus fusion(double speed,double bonus){FusionRecipes.INSTANCE.deleteRecipes();FusionRecipes.INSTANCE.registerDefaults();TileEntityFusionTorus f=new TileEntityFusionTorus(){@Override public void markChanged(){}};f.power=1000000;f.fusionModule.setRecipe("fus.stellar",false);f.fusionModule.setupTanks(f.fusionModule.getRecipe());f.tanks[0].setFill(1000);f.fusionModule.preUpdate(speed,bonus);return f;}
    @Test void stellarFusionConsumesFiveMillibucketsAtFullSpeed(){TileEntityFusionTorus f=fusion(1,0);f.fusionModule.update(1,1,true,ItemStack.EMPTY);assertTrue(f.fusionModule.didProcess);assertEquals(995,f.tanks[0].getFill());assertEquals(975000,f.power);}
    @Test void stellarFusionPreservesPartialSpeedRounding(){TileEntityFusionTorus f=fusion(.5,0);f.fusionModule.update(1,1,true,ItemStack.EMPTY);assertEquals(997,f.tanks[0].getFill());}
    @Test void stellarCollectorBonusDoesNotDoubleFuelDrain(){TileEntityFusionTorus f=fusion(1,2);f.fusionModule.update(1,1,true,ItemStack.EMPTY);assertEquals(995,f.tanks[0].getFill());assertEquals(.02,f.fusionModule.bonus,.000001);}
    @Test void fusionRadioSelectionRetainsFullSpeed(){TileEntityFusionTorus f=fusion(1,0);f.runRORFunction(IRORInteractive.PREFIX_FUNCTION+"setrecipe",new String[]{"fus.dt"});assertEquals("fus.dt",f.fusionModule.getRecipeName());assertFalse(f.fusionModule.restrictedMode);assertEquals("fus.dt",f.provideRORValue(IRORValueProvider.PREFIX_VALUE+"recipe"));}
    @Test void icfExistingTelemetryContractRemainsAvailable(){TileEntityICF i=new TileEntityICF();assertEquals("ntm_icf_reactor",i.getComponentName());assertArrayEquals(new String[]{"getHeat","getHeatingRate","getMaxHeat","getPower","getFluid","getPelletStats"},i.methods());assertEquals(8,i.getFluid(null,null).length);}
}
