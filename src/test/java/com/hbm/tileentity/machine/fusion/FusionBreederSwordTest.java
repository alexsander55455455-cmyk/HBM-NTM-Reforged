package com.hbm.tileentity.machine.fusion;

import com.hbm.blocks.fluid.ModFluids;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.render.misc.EnumSymbol;
import com.hbm.tileentity.machine.TileEntityITER;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.main.MaterialRegistry;
import com.hbm.items.ModItems;
import com.hbm.testutil.ForgeTestBootstrap;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnJre(JRE.JAVA_8)
class FusionBreederSwordTest {

    @BeforeAll
    static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        MainRegistry.logger = LogManager.getLogger("FusionBreederSwordTest");
        HBMSoundHandler.init();
        MaterialRegistry.init();
        ModFluids.init();
        // Standalone Forge tests do not run the game's item/fluid registration events.
        if (Fluids.NONE == null) Fluids.NONE = new FluidType("NONE", 0, 0, 0, 0, EnumSymbol.NONE);
        if (Fluids.PLASMA_DT == null) Fluids.PLASMA_DT = new FluidType("PLASMA_DT", 0, 0, 0, 0, EnumSymbol.NONE);
        if (Fluids.PLASMA_BF == null) Fluids.PLASMA_BF = new FluidType("PLASMA_BF", 0, 0, 0, 0, EnumSymbol.NONE);
        if (Item.REGISTRY.getNameForObject(ModItems.meteorite_sword_bred) == null)
            ForgeRegistries.ITEMS.register(ModItems.meteorite_sword_bred);
        if (Item.REGISTRY.getNameForObject(ModItems.meteorite_sword_irradiated) == null)
            ForgeRegistries.ITEMS.register(ModItems.meteorite_sword_irradiated);
        new OutgasserRecipes().registerDefaults();
    }

    @Test
    void acceptsFusedSwordAsInput() {
        assertTrue(new TileEntityFusionBreeder().isItemValidForSlot(1,
                new ItemStack(ModItems.meteorite_sword_fused)));
    }

    @Test
    void balefireUpgradesFusedSword() {
        TileEntityFusionBreeder breeder = breeder(ModItems.meteorite_sword_fused);
        breeder.receiveFusionPower(1, TileEntityFusionBreeder.capacity + 1, 0.2F, 1F, 0.2F, true);
        assertTrue(breeder.inventory.getStackInSlot(1).isEmpty());
        assertSame(ModItems.meteorite_sword_baleful, breeder.inventory.getStackInSlot(2).getItem());
        assertEquals(1, breeder.inventory.getStackInSlot(2).getCount());
    }

    @Test
    void ordinaryPlasmaCannotUpgradeFusedSwordEvenWithMoreNeutrons() {
        TileEntityFusionBreeder breeder = breeder(ModItems.meteorite_sword_fused);
        breeder.receiveFusionPower(1, 1_000_000_000D, 0.2F, 1F, 0.2F, false);
        assertSame(ModItems.meteorite_sword_fused, breeder.inventory.getStackInSlot(1).getItem());
        assertTrue(breeder.inventory.getStackInSlot(2).isEmpty());
        assertEquals(0D, breeder.progress);
    }

    @Test
    void ordinaryPlasmaStillUpgradesIrradiatedSword() {
        TileEntityFusionBreeder breeder = breeder(ModItems.meteorite_sword_irradiated);
        breeder.receiveFusionPower(1, TileEntityFusionBreeder.capacity + 1, 1F, 0.2F, 0.2F);
        assertTrue(breeder.inventory.getStackInSlot(1).isEmpty());
        assertSame(ModItems.meteorite_sword_fused, breeder.inventory.getStackInSlot(2).getItem());
    }

    @Test
    void stillProcessesBredSwordUsingExistingOutgasserRecipe() {
        TileEntityFusionBreeder breeder = breeder(ModItems.meteorite_sword_bred);
        breeder.receiveFusionPower(1, TileEntityFusionBreeder.capacity + 1, 1F, 0.2F, 0.2F);
        assertTrue(breeder.inventory.getStackInSlot(1).isEmpty());
        assertSame(ModItems.meteorite_sword_irradiated, breeder.inventory.getStackInSlot(2).getItem());
    }

    @Test
    void occupiedOutputDoesNotConsumeSwordOrOverwriteItem() {
        TileEntityFusionBreeder breeder = breeder(ModItems.meteorite_sword_fused);
        breeder.inventory.setStackInSlot(2, new ItemStack(ModItems.meteorite_sword_baleful));
        breeder.receiveFusionPower(1, TileEntityFusionBreeder.capacity + 1, 0.2F, 1F, 0.2F, true);
        assertSame(ModItems.meteorite_sword_fused, breeder.inventory.getStackInSlot(1).getItem());
        assertSame(ModItems.meteorite_sword_baleful, breeder.inventory.getStackInSlot(2).getItem());
        assertEquals(1, breeder.inventory.getStackInSlot(2).getCount());
        assertEquals(0D, breeder.progress);
    }

    @Test
    void losingBalefireResetsPartialSwordProgress() {
        TileEntityFusionBreeder breeder = breeder(ModItems.meteorite_sword_fused);
        breeder.receiveFusionPower(1, TileEntityFusionBreeder.capacity / 2, 0.2F, 1F, 0.2F, true);
        assertEquals(TileEntityFusionBreeder.capacity / 2, breeder.progress);
        breeder.receiveFusionPower(1, TileEntityFusionBreeder.capacity, 0.2F, 1F, 0.2F);
        assertEquals(0D, breeder.progress);
        assertFalse(breeder.canProcessSolid());
        assertSame(ModItems.meteorite_sword_fused, breeder.inventory.getStackInSlot(1).getItem());
    }

    @Test
    void balefireWithoutNeutronEnergyDoesNotUpgradeSword() {
        TileEntityFusionBreeder breeder = breeder(ModItems.meteorite_sword_fused);
        breeder.receiveFusionPower(1, 0D, 0.2F, 1F, 0.2F, true);
        assertEquals(0D, breeder.progress);
        assertTrue(breeder.inventory.getStackInSlot(2).isEmpty());
    }

    @Test
    void newContextOverloadStillCallsExistingReceiverImplementations() {
        TileEntityFusionMHDT mhd = new TileEntityFusionMHDT();
        IFusionPowerReceiver legacyReceiver = mhd;
        legacyReceiver.receiveFusionPower(123, 456, 0.2F, 1F, 0.2F, true);
        assertEquals(123, mhd.plasmaEnergy);
    }

    @Test
    void legacyIterStillUpgradesIrradiatedSword() throws Exception {
        TileEntityITER iter = iter(ModItems.meteorite_sword_irradiated, false);
        runIterBreeder(iter);
        assertSame(ModItems.meteorite_sword_fused, iter.inventory.getStackInSlot(2).getItem());
    }

    @Test
    void legacyIterStillUpgradesFusedSwordWithBalefire() throws Exception {
        TileEntityITER iter = iter(ModItems.meteorite_sword_fused, true);
        runIterBreeder(iter);
        assertSame(ModItems.meteorite_sword_baleful, iter.inventory.getStackInSlot(2).getItem());
    }

    @Test
    void legacyIterStillRejectsFusedSwordWithWeakPlasma() throws Exception {
        TileEntityITER iter = iter(ModItems.meteorite_sword_fused, false);
        runIterBreeder(iter);
        assertTrue(iter.inventory.getStackInSlot(2).isEmpty());
        assertSame(ModItems.meteorite_sword_fused, iter.inventory.getStackInSlot(1).getItem());
    }

    private static TileEntityFusionBreeder breeder(Item item) {
        TileEntityFusionBreeder breeder = new TileEntityFusionBreeder();
        breeder.inventory.setStackInSlot(1, new ItemStack(item));
        return breeder;
    }

    private static TileEntityITER iter(Item item, boolean balefire) {
        TileEntityITER iter = new TileEntityITER();
        iter.plasma.setTankType(balefire ? Fluids.PLASMA_BF : Fluids.PLASMA_DT);
        iter.plasma.setFill(1000);
        iter.inventory.setStackInSlot(1, new ItemStack(item));
        return iter;
    }

    private static void runIterBreeder(TileEntityITER iter) throws Exception {
        Method method = TileEntityITER.class.getDeclaredMethod("doBreederStuff");
        method.setAccessible(true);
        for (int tick = 0; tick <= TileEntityITER.duration; tick++) method.invoke(iter);
    }
}
