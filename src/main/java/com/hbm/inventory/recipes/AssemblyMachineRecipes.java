package com.hbm.inventory.recipes;

import com.hbm.blocks.BlockEnums;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.AssemblyMachineInventory;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.*;
import com.hbm.items.ModItems;
import com.hbm.items.machine.*;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.items.machine.ItemSatellitePayload.EnumSatelliteType;
import com.hbm.items.weapon.ItemAmmoHIMARS;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

public class AssemblyMachineRecipes extends GenericRecipes<GenericRecipe> {

    public static final AssemblyMachineRecipes INSTANCE = new AssemblyMachineRecipes();

    @Override public int inputItemLimit() { return 12; }
    @Override public int inputFluidLimit() { return 1; }
    @Override public int outputItemLimit() { return 1; }
    @Override public int outputFluidLimit() { return 1; }
    @Override protected int inputItemStackLimit() { return AssemblyMachineInventory.INPUT_STACK_LIMIT; }

    @Override public String getFileName() { return "hbmAssemblyMachine.json"; }
    @Override public GenericRecipe instantiateRecipe(String name) { return new GenericRecipe(name); }

    @Override
    public void registerDefaults() {
        boolean no528 = !GeneralConfig.enable528;
        // plates and ingots
        String autoPlate = "autoswitch.plates";
        this.register(new GenericRecipe("ass.plateiron").setup(60, 100).outputItems(new ItemStack(ModItems.plate_iron, 1)).inputItems(new RecipesCommon.OreDictStack(IRON.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.plategold").setup(60, 100).outputItems(new ItemStack(ModItems.plate_gold, 1)).inputItems(new RecipesCommon.OreDictStack(GOLD.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platetitanium").setup(60, 100).outputItems(new ItemStack(ModItems.plate_titanium, 1)).inputItems(new RecipesCommon.OreDictStack(TI.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platealu").setup(60, 100).outputItems(new ItemStack(ModItems.plate_aluminium, 1)).inputItems(new RecipesCommon.OreDictStack(AL.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platesteel").setup(60, 100).outputItems(new ItemStack(ModItems.plate_steel, 1)).inputItems(new RecipesCommon.OreDictStack(STEEL.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platelead").setup(60, 100).outputItems(new ItemStack(ModItems.plate_lead, 1)).inputItems(new RecipesCommon.OreDictStack(PB.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platecopper").setup(60, 100).outputItems(new ItemStack(ModItems.plate_copper, 1)).inputItems(new RecipesCommon.OreDictStack(CU.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platealloy").setup(60, 100).outputItems(new ItemStack(ModItems.plate_advanced_alloy, 1)).inputItems(new RecipesCommon.OreDictStack(ALLOY.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.plateschrab").setup(60, 100).outputItems(new ItemStack(ModItems.plate_schrabidium, 1)).inputItems(new RecipesCommon.OreDictStack(SA326.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platecmb").setup(60, 100).outputItems(new ItemStack(ModItems.plate_combine_steel, 1)).inputItems(new RecipesCommon.OreDictStack(CMB.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.plategunmetal").setup(60, 100).outputItems(new ItemStack(ModItems.plate_gunmetal, 1)).inputItems(new RecipesCommon.OreDictStack(GUNMETAL.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.plateweaponsteel").setup(60, 100).outputItems(new ItemStack(ModItems.plate_weaponsteel, 1)).inputItems(new RecipesCommon.OreDictStack(WEAPONSTEEL.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platesaturnite").setup(60, 100).outputItems(new ItemStack(ModItems.plate_saturnite, 1)).inputItems(new RecipesCommon.OreDictStack(BIGMT.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platedura").setup(60, 100).outputItems(new ItemStack(ModItems.plate_dura_steel, 1)).inputItems(new RecipesCommon.OreDictStack(DURA.ingot())).setPools(GenericRecipes.POOL_PREFIX_ALT + "plates").setGroup(autoPlate, this));
        this.register(new GenericRecipe("ass.platemixed").setup(50, 100).outputItems(new ItemStack(ModItems.plate_mixed, 4))
                .inputItems(new RecipesCommon.OreDictStack(CU.plate(), 2), new RecipesCommon.OreDictStack(OreDictManager.getReflector(), 1), new RecipesCommon.OreDictStack(BIGMT.plate(), 1)));
        this.register(new GenericRecipe("ass.dalekanium").setup(200, 100).outputItems(new ItemStack(ModItems.plate_dalekanium, 1))
                .inputItems(new RecipesCommon.ComparableStack(ModBlocks.block_meteor, 1)));
        this.register(new GenericRecipe("ass.meteorblock").setup(500, 100).outputItems(new ItemStack(ModBlocks.block_meteor, 1))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.fragment_meteorite, 64), new RecipesCommon.ComparableStack(ModItems.fragment_meteorite, 36)));
        this.register(new GenericRecipe("ass.platedesh").setup(200, 100).outputItems(new ItemStack(ModItems.plate_desh, 4))
                .inputItems(new RecipesCommon.OreDictStack(DESH.ingot(), 4), new RecipesCommon.OreDictStack(ANY_PLASTIC.dust(), 2), new RecipesCommon.OreDictStack(DURA.ingot(), 1)));
        this.register(new GenericRecipe("ass.platebismuth").setup(200, 100).outputItems(new ItemStack(ModItems.plate_bismuth, 1))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.nugget_bismuth, 2), new RecipesCommon.OreDictStack(U238.billet(), 2), new RecipesCommon.OreDictStack(NB.dust(), 1)));
        // expensive parts
        this.register(new GenericRecipe("ass.exsteelplating").setup(200, 400).outputItems(new ItemStack(ModItems.item_expensive, 1, EnumExpensiveType.STEEL_PLATING.ordinal()))
                .inputItems(new RecipesCommon.OreDictStack(STEEL.plateCast(), 4), new RecipesCommon.OreDictStack(TI.plate(), 4), new RecipesCommon.OreDictStack(STEEL.bolt(), 16)));
        this.register(new GenericRecipe("ass.exheavyframe").setup(600, 800).outputItems(new ItemStack(ModItems.item_expensive, 1, EnumExpensiveType.HEAVY_FRAME.ordinal()))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.STEEL_PLATING), new RecipesCommon.OreDictStack(ANY_PLASTIC.ingot(), 8), new RecipesCommon.OreDictStack(CU.plateWelded(), 4), new RecipesCommon.OreDictStack(DESH.ingot(), 1), new RecipesCommon.OreDictStack(DURA.bolt(), 32)));
        this.register(new GenericRecipe("ass.excircuit").setup(400, 4_000).outputItems(new ItemStack(ModItems.item_expensive, 1, EnumExpensiveType.CIRCUIT.ordinal()))
                .inputItems(new ComparableStack(ModItems.circuit, 12, EnumCircuitType.BASIC), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.CAPACITOR), new OreDictStack(RUBBER.ingot(), 4))
                .inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 1_000)));
        this.register(new GenericRecipe("ass.exleadplating").setup(400, 4_000).outputItems(new ItemStack(ModItems.item_expensive, 1, EnumExpensiveType.LEAD_PLATING.ordinal()))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.STEEL_PLATING), new RecipesCommon.OreDictStack(PB.plateCast(), 8), new RecipesCommon.OreDictStack(B.ingot(), 2), new RecipesCommon.OreDictStack(W.bolt(), 32))
                .inputFluids(new FluidStack(Fluids.LUBRICANT, 1_000)));
        this.register(new GenericRecipe("ass.exferroplating").setup(1_200, 10_000).outputItems(new ItemStack(ModItems.item_expensive, 1, EnumExpensiveType.FERRO_PLATING.ordinal()))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.LEAD_PLATING), new RecipesCommon.OreDictStack(FERRO.plateCast(), 4), new RecipesCommon.OreDictStack(ANY_RESISTANTALLOY.ingot(), 4))
                .inputFluids(new FluidStack(Fluids.UNSATURATEDS, 1_000)));
        this.register(new GenericRecipe("ass.excomputer").setup(1_200, 16_000).outputItems(new ItemStack(ModItems.item_expensive, 1, EnumExpensiveType.COMPUTER.ordinal()))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.CIRCUIT), new RecipesCommon.ComparableStack(ModItems.circuit, 4, EnumCircuitType.CONTROLLER), new RecipesCommon.ComparableStack(ModItems.circuit, 4, EnumCircuitType.CAPACITOR_BOARD), new RecipesCommon.ComparableStack(ModBlocks.glass_quartz, 8))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 2_000)));
        this.register(new GenericRecipe("ass.bronzetubes").setup(3_000, 250_000).outputItems(new ItemStack(ModItems.item_expensive, 1, EnumExpensiveType.BRONZE_TUBES.ordinal()))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.HEAVY_FRAME), new RecipesCommon.ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.FERRO_PLATING), new RecipesCommon.OreDictStack(ANY_BISMOIDBRONZE.plateCast(), 4), new RecipesCommon.OreDictStack(ZR.plateWelded(), 1))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 4_000))
                .outputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 4_000)));
        this.register(new GenericRecipe("ass.explastic").setup(600, 20_000).outputItems(new ItemStack(ModItems.item_expensive, 1, EnumExpensiveType.PLASTIC.ordinal()))
                .inputItems(new OreDictStack(ANY_HARDPLASTIC.ingot(), 4), new OreDictStack(ANY_PLASTIC.ingot(), 16), new OreDictStack(ANY_RUBBER.ingot(), 8))
                .inputFluids(new FluidStack(Fluids.SOLVENT, 1_000)));
        this.register(new GenericRecipe("ass.exgold").setup(600, 10_000).outputItems(new ItemStack(ModItems.item_expensive, 1, EnumExpensiveType.GOLD_DUST.ordinal()))
                .inputItems(new OreDictStack(GOLD.dust(), 64), new OreDictStack(GOLD.dust(), 64)));

        // cloth
        this.register(new GenericRecipe("ass.hazcloth").setup(50, 100).outputItems(new ItemStack(ModItems.hazmat_cloth, 4))
                .inputItems(new RecipesCommon.OreDictStack(PB.dust(), 4), new RecipesCommon.ComparableStack(Items.STRING, 8)));
        this.register(new GenericRecipe("ass.firecloth").setup(50, 100).outputItems(new ItemStack(ModItems.asbestos_cloth, 4))
                .inputItems(new RecipesCommon.OreDictStack(ASBESTOS.ingot(), 1), new RecipesCommon.ComparableStack(Items.STRING, 8)));
        this.register(new GenericRecipe("ass.filtercoal").setup(50, 100).outputItems(new ItemStack(ModItems.filter_coal, 1))
                .inputItems(new RecipesCommon.OreDictStack(COAL.dust(), 4), new RecipesCommon.ComparableStack(Items.STRING, 2), new RecipesCommon.ComparableStack(Items.PAPER, 1)));

        // machine parts
        this.register(new GenericRecipe("ass.centrifugetower").setup(100, 100).outputItems(new ItemStack(ModItems.centrifuge_element, 1))
                .inputItems(new OreDictStack(DURA.plate(), 4), new OreDictStack(TI.plate(), 4), new ComparableStack(ModItems.motor, 1)));
        this.register(new GenericRecipe("ass.reactorcore").setup(100, 100).outputItems(new ItemStack(ModItems.reactor_core, 1))
                .inputItems(new RecipesCommon.OreDictStack(PB.plateCast(), 4), new RecipesCommon.OreDictStack(BE.ingot(), 8), new RecipesCommon.OreDictStack(OreDictManager.getReflector(), 8), new RecipesCommon.OreDictStack(ASBESTOS.ingot(), 4)));
        this.register(new GenericRecipe("ass.thermoelement").setup(60, 100).outputItems(new ItemStack(ModItems.thermo_element, 1))
                .inputItems(new RecipesCommon.OreDictStack(STEEL.plate(), 1), new RecipesCommon.OreDictStack(MINGRADE.wireFine(), 2), new RecipesCommon.OreDictStack(NETHERQUARTZ.dust(), 2)));
        this.register(new GenericRecipe("ass.thermoelementsilicon").setup(60, 100).outputItems(new ItemStack(ModItems.thermo_element, 1))
                .inputItems(new RecipesCommon.OreDictStack(STEEL.plate(), 1), new RecipesCommon.OreDictStack(GOLD.wireFine(), 2), new RecipesCommon.OreDictStack(SI.billet(), 1)));
        this.register(new GenericRecipe("ass.rtgunit").setup(100, 100).outputItems(new ItemStack(ModItems.rtg_unit, 1))
                .inputItems(new RecipesCommon.OreDictStack(PB.plateCast(), 2), new RecipesCommon.OreDictStack(CU.plate(), 4), new RecipesCommon.ComparableStack(ModItems.thermo_element, 2)));
        this.register(new GenericRecipe("ass.magnetron").setup(40, 100).outputItems(new ItemStack(ModItems.magnetron, 1))
                .inputItems(new RecipesCommon.OreDictStack(CU.plate(), 3), new RecipesCommon.OreDictStack(W.wireFine(), 4)));
        this.register(new GenericRecipe("ass.satlink").setup(100, 1_000)
                .outputItems(new ItemStack(ModBlocks.machine_satlink))
                .inputItems(new ComparableStack(ModBlocks.steel_scaffold, 16),
                        new OreDictStack(AL.plate(), 16),
                        new ComparableStack(ModItems.magnetron, 3),
                        new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER)));
        this.register(new GenericRecipe("ass.australium_iii").setup(150, 100).outputItems(new ItemStack(ModItems.australium_iii, 1))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.rod_australium, 1), new RecipesCommon.OreDictStack(STEEL.ingot(), 1), new RecipesCommon.OreDictStack(STEEL.plate(), 6), new RecipesCommon.OreDictStack(CU.plate(), 2), new RecipesCommon.ComparableStack(ModItems.wire, 6, Mats.MAT_COPPER.id)));
        this.register(new GenericRecipe("ass.titaniumdrill").setup(100, 100).outputItems(new ItemStack(ModItems.drill_titanium, 1))
                .inputItems(new RecipesCommon.OreDictStack(DURA.plateCast(), 1), new RecipesCommon.OreDictStack(TI.plate(), 8)));
        this.register(new GenericRecipe("ass.entanglementkit").setup(200, 100).outputItems(new ItemStack(ModItems.entanglement_kit, 1))
                .inputItems(new RecipesCommon.OreDictStack(DURA.plateCast(), 4), new RecipesCommon.OreDictStack(CU.plate(), 24), new RecipesCommon.OreDictStack(GOLD.wireDense(), 16))
                .inputFluids(new FluidStack(Fluids.XENON, 8_000)));
        this.register(new GenericRecipe("ass.protoreactor").setup(200, 100).outputItems(new ItemStack(ModItems.dysfunctional_reactor, 1))
                .inputItems(new RecipesCommon.OreDictStack(STEEL.shell(), 4), new RecipesCommon.OreDictStack(PB.plateCast(), 4), new RecipesCommon.ComparableStack(ModItems.rod_quad_empty, 10), new RecipesCommon.OreDictStack(KEY_BROWN, 3)));

        String autoPileRod = "autoswitch.pilerod";
        this.register(new GenericRecipe("ass.pilepabe").setup(40, 200)
                .outputItems(new ItemStack(ModItems.pile_rod, 1, EnumPileRod.RA226BE.ordinal()))
                .inputItems(new ComparableStack(ModItems.billet_ra226be, 3)).setGroup(autoPileRod, this));
        this.register(new GenericRecipe("ass.pilepobe").setup(40, 200)
                .outputItems(new ItemStack(ModItems.pile_rod, 1, EnumPileRod.PO210BE.ordinal()))
                .inputItems(new ComparableStack(ModItems.billet_po210be, 3)).setGroup(autoPileRod, this));
        this.register(new GenericRecipe("ass.pilezr").setup(40, 200)
                .outputItems(new ItemStack(ModItems.pile_rod, 3, EnumPileRod.ZR.ordinal()))
                .inputItems(new OreDictStack(ZR.billet(), 1)).setGroup(autoPileRod, this));
        this.register(new GenericRecipe("ass.pilenu").setup(40, 200)
                .outputItems(new ItemStack(ModItems.pile_rod, 1, EnumPileRod.NU.ordinal()))
                .inputItems(new OreDictStack(U.billet(), 3)).setGroup(autoPileRod, this));

        // powders
        this.register(new GenericRecipe("ass.partlith").setup(40, 100).outputItems(new ItemStack(ModItems.part_lithium, 8))
                .inputItems(new RecipesCommon.OreDictStack(LI.dust(), 1)));
        this.register(new GenericRecipe("ass.partberyl").setup(40, 100).outputItems(new ItemStack(ModItems.part_beryllium, 8))
                .inputItems(new RecipesCommon.OreDictStack(BE.dust(), 1)));
        this.register(new GenericRecipe("ass.partcoal").setup(40, 100).outputItems(new ItemStack(ModItems.part_carbon, 8))
                .inputItems(new RecipesCommon.OreDictStack(COAL.dust(), 1)));
        this.register(new GenericRecipe("ass.partcop").setup(40, 100).outputItems(new ItemStack(ModItems.part_copper, 8))
                .inputItems(new RecipesCommon.OreDictStack(CU.dust(), 1)));
        this.register(new GenericRecipe("ass.partplut").setup(40, 100).outputItems(new ItemStack(ModItems.part_plutonium, 8))
                .inputItems(new RecipesCommon.OreDictStack(PU.dust(), 1)));

        // bunker blocks
        this.register(new GenericRecipe("ass.cmbtile").setup(100, 100).outputItems(new ItemStack(ModBlocks.cmb_brick, 8))
                .inputItems(new RecipesCommon.OreDictStack(ANY_CONCRETE.any(), 4), new RecipesCommon.OreDictStack(CMB.plate(), 4)));
        this.register(new GenericRecipe("ass.cmbbrick").setup(100, 100).outputItems(new ItemStack(ModBlocks.cmb_brick_reinforced, 8))
                .inputItems(new RecipesCommon.OreDictStack(MAGTUNG.ingot(), 8), new RecipesCommon.ComparableStack(ModBlocks.ducrete, 4), new RecipesCommon.ComparableStack(ModBlocks.cmb_brick, 8)));
        this.register(new GenericRecipe("ass.sealframe").setup(100, 100).outputItems(new ItemStack(ModBlocks.seal_frame, 1))
                .inputItems(new RecipesCommon.OreDictStack(DURA.ingot(), 1), new RecipesCommon.OreDictStack(STEEL.plateCast(), 1), new RecipesCommon.OreDictStack(MINGRADE.wireDense(), 1)));
        this.register(new GenericRecipe("ass.sealcontroller").setup(100, 100).outputItems(new ItemStack(ModBlocks.seal_controller, 1))
                .inputItems(new RecipesCommon.OreDictStack(DURA.ingot(), 1), new RecipesCommon.OreDictStack(STEEL.plateCast(), 1), new RecipesCommon.OreDictStack(ANY_PLASTIC.ingot(), 4), new RecipesCommon.OreDictStack(MINGRADE.wireDense(), 4)));

        // blocks
        this.register(new GenericRecipe("ass.yellowbarrel").setup(400, 400).outputItems(new ItemStack(ModBlocks.yellow_barrel, 1))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.tank_steel, 1), new RecipesCommon.OreDictStack(PB.plate(), 2), new RecipesCommon.ComparableStack(ModItems.nuclear_waste, 10)));
        this.register(new GenericRecipe("ass.vitrifiedbarrel").setup(400, 400).outputItems(new ItemStack(ModBlocks.vitrified_barrel, 1))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.tank_steel, 1), new RecipesCommon.OreDictStack(PB.plate(), 2), new RecipesCommon.ComparableStack(ModItems.nuclear_waste_vitrified, 10)));
        this.register(new GenericRecipe("ass.pileblock").setup(20, 250).outputItems(new ItemStack(ModBlocks.pile_brick, 1))
                .inputItems(new OreDictStack(KEY_PLANKS, 1), new OreDictStack(GRAPHITE.ingot(), 4), new OreDictStack(STEEL.bolt(), 2))
                .inputItemsEx(new OreDictStack(KEY_PLANKS, 1), new OreDictStack(GRAPHITE.ingot(), 8), new OreDictStack(STEEL.plate(), 1)));

        // nuclear door mod
        this.register(new GenericRecipe("ass.vaultdoor").setup(200, 100).outputItems(new ItemStack(ModBlocks.vault_door, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 32), new OreDictStack(W.ingot(), 32), new OreDictStack(PB.plate(), 16), new OreDictStack(ALLOY.plateCast(), 4), new OreDictStack(ANY_RUBBER.ingot(), 4), new OreDictStack(W.bolt(), 16), new OreDictStack(DURA.bolt(), 16), new ComparableStack(ModItems.motor, 3)));
        this.register(new GenericRecipe("ass.blastdoor").setup(300, 100).outputItems(new ItemStack(ModBlocks.blast_door, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 12), new OreDictStack(W.ingot(), 8), new OreDictStack(PB.plate(), 6), new OreDictStack(ANY_RUBBER.ingot(), 3), new OreDictStack(W.bolt(), 4), new OreDictStack(DURA.bolt(), 4), new ComparableStack(ModItems.motor, 1)));
        this.register(new GenericRecipe("ass.firedoor").setup(500, 100).outputItems(new ItemStack(ModBlocks.fire_door, 1))
                .inputItems(new OreDictStack(PB.plate(), 12), new OreDictStack(STEEL.plate(), 36), new OreDictStack(ASBESTOS.ingot(), 12), new ComparableStack(ModItems.plate_polymer, 6), new OreDictStack(STEEL.block(), 4), new ComparableStack(ModItems.motor, 4), new ComparableStack(ModItems.bolt, 6, Mats.MAT_DURA.id), new OreDictStack(KEY_RED, 8)));
        this.register(new GenericRecipe("ass.seal").setup(1_200, 100).outputItems(new ItemStack(ModBlocks.transition_seal, 1))
                .inputItems(new ComparableStack(ModBlocks.cmb_brick_reinforced, 16), new OreDictStack(STEEL.plateWelded(), 64), new OreDictStack(ANY_RUBBER.ingot(), 36), new OreDictStack(STEEL.block(), 32), new ComparableStack(ModItems.motor_desh, 16), new OreDictStack(KEY_YELLOW, 4)));
        this.register(new GenericRecipe("ass.slidingdoor").setup(300, 100).outputItems(new ItemStack(ModBlocks.sliding_blast_door, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 16), new OreDictStack(W.ingot(), 8), new ComparableStack(ModBlocks.reinforced_glass, 4), new OreDictStack(ANY_RUBBER.ingot(), 4), new ComparableStack(ModItems.bolt, 16, Mats.MAT_DURA.id), new ComparableStack(ModItems.motor, 2)));
        this.register(new GenericRecipe("ass.slidingdoor2").setup(300, 100).outputItems(new ItemStack(ModBlocks.sliding_blast_door_2, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 16), new OreDictStack(W.ingot(), 8), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.BASIC), new OreDictStack(ANY_RUBBER.ingot(), 4), new ComparableStack(ModItems.bolt, 16, Mats.MAT_DURA.id), new ComparableStack(ModItems.motor, 2)));
        this.register(new GenericRecipe("ass.vehicledoor").setup(400, 100).outputItems(new ItemStack(ModBlocks.large_vehicle_door, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 16), new ComparableStack(ModItems.plate_polymer, 4), new ComparableStack(ModItems.motor, 4), new OreDictStack(DURA.bolt(), 16), new OreDictStack(KEY_GREEN, 4)));
        this.register(new GenericRecipe("ass.waterdoor").setup(200, 100).outputItems(new ItemStack(ModBlocks.water_door, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 16), new OreDictStack(DURA.bolt(), 4), new OreDictStack(KEY_RED, 1)));
        this.register(new GenericRecipe("ass.qedoor").setup(400, 100).outputItems(new ItemStack(ModBlocks.qe_containment, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 6), new ComparableStack(ModItems.plate_polymer, 8), new ComparableStack(ModItems.motor, 2), new OreDictStack(DURA.bolt(), 32), new OreDictStack(KEY_BLACK, 4)));
        this.register(new GenericRecipe("ass.queslidingdoor").setup(200, 100).outputItems(new ItemStack(ModBlocks.qe_sliding_door, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 4), new ComparableStack(ModItems.plate_polymer, 4), new ComparableStack(ModItems.motor, 2), new OreDictStack(DURA.bolt(), 4), new OreDictStack(KEY_WHITE, 4), new ComparableStack(Blocks.GLASS, 4)));
        this.register(new GenericRecipe("ass.roundairlock").setup(500, 100).outputItems(new ItemStack(ModBlocks.round_airlock_door, 1))
                .inputItems(new OreDictStack(PB.plate(), 16), new OreDictStack(STEEL.plate(), 32), new ComparableStack(ModItems.plate_polymer, 12), new OreDictStack(STEEL.block(), 6), new ComparableStack(ModItems.motor, 6), new ComparableStack(ModItems.bolt, 12, Mats.MAT_DURA.id), new OreDictStack(KEY_GRAY, 4)));
        this.register(new GenericRecipe("ass.secureaccess").setup(400, 100).outputItems(new ItemStack(ModBlocks.secure_access_door, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 12), new ComparableStack(ModItems.plate_polymer, 8), new ComparableStack(ModItems.motor, 4), new OreDictStack(DURA.bolt(), 32), new OreDictStack(KEY_CYAN, 8)));
        this.register(new GenericRecipe("ass.slidingseal").setup(200, 100).outputItems(new ItemStack(ModBlocks.sliding_seal_door, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 12), new ComparableStack(ModItems.plate_polymer, 4), new ComparableStack(ModItems.motor, 2), new OreDictStack(DURA.bolt(), 4), new OreDictStack(KEY_WHITE, 2)));
        this.register(new GenericRecipe("ass.smallhatch").setup(200, 100).outputItems(new ItemStack(ModBlocks.small_hatch, 1))
                .inputItems(new OreDictStack(PB.plate(), 4), new OreDictStack(STEEL.plate(), 8), new OreDictStack(ALLOY.plateCast(), 2), new ComparableStack(ModItems.bolt, 1, Mats.MAT_DURA.id), new ComparableStack(ModBlocks.brick_concrete, 1), new ComparableStack(ModBlocks.ladder_red, 1)));
        this.register(new GenericRecipe("ass.slidinggate").setup(500, 100).outputItems(new ItemStack(ModBlocks.sliding_gate_door, 1))
                .inputItems(new OreDictStack(PB.plate(), 24), new OreDictStack(STEEL.plate(), 12), new OreDictStack(ALLOY.plateCast(), 4), new ComparableStack(ModItems.plate_polymer, 2), new OreDictStack(STEEL.block(), 1), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.bolt, 2, Mats.MAT_DURA.id), new OreDictStack(KEY_WHITE, 2)));
        this.register(new GenericRecipe("ass.silohatchdrill").setup(200, 100).outputItems(new ItemStack(ModBlocks.silo_hatch_drillgon, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 4), new ComparableStack(ModItems.plate_polymer, 4), new ComparableStack(ModItems.motor, 2), new OreDictStack(STEEL.bolt(), 16), new OreDictStack(KEY_GREEN, 4)));
        this.register(new GenericRecipe("ass.silohatch").setup(200, 100).outputItems(new ItemStack(ModBlocks.silo_hatch, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 4), new ComparableStack(ModItems.plate_polymer, 4), new ComparableStack(ModItems.motor, 2), new OreDictStack(STEEL.bolt(), 16), new OreDictStack(KEY_GRAY, 4)));
        this.register(new GenericRecipe("ass.silohatchlarge").setup(300, 100).outputItems(new ItemStack(ModBlocks.silo_hatch_large, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 6), new ComparableStack(ModItems.plate_polymer, 8), new ComparableStack(ModItems.motor, 2), new OreDictStack(STEEL.bolt(), 16), new OreDictStack(KEY_GREEN, 8)));

        // decoration
        this.register(new GenericRecipe("ass.capnuka").setup(10, 100).outputItems(DictFrame.fromOne(ModBlocks.block_cap, BlockEnums.EnumBlockCapType.NUKA))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.cap_nuka, 64), new RecipesCommon.ComparableStack(ModItems.cap_nuka, 64)));
        this.register(new GenericRecipe("ass.capquantum").setup(10, 100).outputItems(DictFrame.fromOne(ModBlocks.block_cap, BlockEnums.EnumBlockCapType.QUANTUM))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.cap_quantum, 64), new RecipesCommon.ComparableStack(ModItems.cap_quantum, 64)));
        this.register(new GenericRecipe("ass.capsparkle").setup(10, 100).outputItems(DictFrame.fromOne(ModBlocks.block_cap, BlockEnums.EnumBlockCapType.SPARKLE))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.cap_sparkle, 64), new RecipesCommon.ComparableStack(ModItems.cap_sparkle, 64)));
        this.register(new GenericRecipe("ass.caprad").setup(10, 100).outputItems(DictFrame.fromOne(ModBlocks.block_cap, BlockEnums.EnumBlockCapType.RAD))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.cap_rad, 64), new RecipesCommon.ComparableStack(ModItems.cap_rad, 64)));
        this.register(new GenericRecipe("ass.capfritz").setup(10, 100).outputItems(DictFrame.fromOne(ModBlocks.block_cap, BlockEnums.EnumBlockCapType.FRITZ))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.cap_fritz, 64), new RecipesCommon.ComparableStack(ModItems.cap_fritz, 64)));
        this.register(new GenericRecipe("ass.capkorl").setup(10, 100).outputItems(DictFrame.fromOne(ModBlocks.block_cap, BlockEnums.EnumBlockCapType.KORL))
                .inputItems(new RecipesCommon.ComparableStack(ModItems.cap_korl, 64), new RecipesCommon.ComparableStack(ModItems.cap_korl, 64)));

        // machines
        this.register(new GenericRecipe("ass.shredder").setup(100, 100).outputItems(new ItemStack(ModBlocks.machine_shredder, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 8), new OreDictStack(CU.plate(), 4), new ComparableStack(ModItems.motor, 2))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.STEEL_PLATING), new OreDictStack(CU.plate(), 4), new ComparableStack(ModItems.motor, 2)));
        this.register(new GenericRecipe("ass.assembler").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_assembly_machine, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 4), new OreDictStack(CU.plate(), 4), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ANALOG)));
        this.register(new GenericRecipe("ass.chemplant").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_chemical_plant, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 8), new OreDictStack(CU.pipe(), 2), new ComparableStack(ModItems.plate_polymer, 16), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.coil_tungsten, 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ANALOG))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.STEEL_PLATING), new OreDictStack(CU.pipe(), 2), new ComparableStack(ModItems.plate_polymer, 16), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.coil_tungsten, 2), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.ANALOG)));
        this.register(new GenericRecipe("ass.purex").setup(300, 100).outputItems(new ItemStack(ModBlocks.machine_purex, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 4), new OreDictStack(RUBBER.pipe(), 8), new OreDictStack(PB.plateCast(), 4), new ComparableStack(ModItems.motor_desh, 1), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.LEAD_PLATING), new OreDictStack(STEEL.shell(), 4), new OreDictStack(RUBBER.pipe(), 12), new ComparableStack(ModItems.motor_desh, 3), new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.precass").setup(1_200, 100).outputItems(new ItemStack(ModBlocks.machine_precass, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 8), new OreDictStack(ZR.ingot(), 8), new ComparableStack(ModItems.motor, 4), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CAPACITOR_BOARD))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.STEEL_PLATING), new OreDictStack(ZR.ingot(), 8), new ComparableStack(ModItems.motor, 4), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.CAPACITOR_BOARD)));
        this.register(new GenericRecipe("ass.centrifuge").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_centrifuge, 1))
                .inputItems(new ComparableStack(ModItems.centrifuge_element, 1), new OreDictStack(ANY_PLASTIC.ingot(), 4), new OreDictStack(STEEL.plate(), 8), new OreDictStack(CU.plate(), 4), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ANALOG))
                .inputItemsEx(new ComparableStack(ModItems.centrifuge_element, 1), new OreDictStack(ANY_PLASTIC.ingot(), 4), new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.STEEL_PLATING), new OreDictStack(CU.plateCast(), 4), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ANALOG)));
        this.register(new GenericRecipe("ass.gascent").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_gascent, 1))
                .inputItems(new ComparableStack(ModItems.centrifuge_element, 4), new OreDictStack(ANY_PLASTIC.ingot(), 8), new OreDictStack(DESH.ingot(), 2), new OreDictStack(STEEL.plate(), 8), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED.ordinal()))
                .inputItemsEx(new ComparableStack(ModItems.centrifuge_element, 4), new OreDictStack(STEEL.plateWelded(), 4), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.CIRCUIT))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "gascent"));
        this.register(new GenericRecipe("ass.arcfurnace").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_arc_furnace, 1))
                .inputItems(new OreDictStack(ANY_CONCRETE.any(), 12), new OreDictStack(ANY_PLASTIC.ingot(), 8), new ComparableStack(ModItems.ingot_firebrick, 16),new OreDictStack(STEEL.plateCast(), 8), new ComparableStack(ModBlocks.machine_transformer, 1), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ANALOG.ordinal()))
                .inputItemsEx(new OreDictStack(ANY_CONCRETE.any(), 12), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.ingot_firebrick, 16), new ComparableStack(ModBlocks.machine_transformer, 1), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.ANALOG.ordinal())));
        this.register(new GenericRecipe("ass.acidizer").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_crystallizer, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 2), new OreDictStack(TI.shell(), 3), new OreDictStack(DESH.ingot(), 4), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(TI.shell(), 3), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.electrolyzer").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_electrolyser, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 8), new OreDictStack(CU.plate(), 16), new OreDictStack(TI.shell(), 3), new OreDictStack(RUBBER.ingot(), 8), new ComparableStack(ModItems.ingot_firebrick, 16), new ComparableStack(ModItems.coil_copper, 16), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(TI.shell(), 3), new OreDictStack(RUBBER.ingot(), 8), new ComparableStack(ModItems.ingot_firebrick, 16), new ComparableStack(ModItems.coil_copper, 16), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.rtg").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_rtg_grey, 1))
                .inputItems(new ComparableStack(ModItems.rtg_unit, 3), new OreDictStack(STEEL.plate(), 4), new OreDictStack(MINGRADE.wireFine(), 16), new OreDictStack(ANY_PLASTIC.ingot(), 4)));
        this.register(new GenericRecipe("ass.derrick").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_well, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 8), new OreDictStack(CU.plateCast(), 2), new OreDictStack(STEEL.pipe(), 4), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.drill_titanium, 1))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.STEEL_PLATING), new OreDictStack(STEEL.pipe(), 4), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.drill_titanium, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "steel"));
        this.register(new GenericRecipe("ass.pumpjack").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_pumpjack, 1))
                .inputItems(new OreDictStack(DURA.plate(), 8), new OreDictStack(STEEL.plateWelded(), 8), new OreDictStack(STEEL.pipe(), 12), new ComparableStack(ModItems.motor_desh), new ComparableStack(ModItems.drill_titanium, 1))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(STEEL.pipe(), 12), new ComparableStack(ModItems.motor_desh, 3), new ComparableStack(ModItems.drill_titanium, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "steel"));
        this.register(new GenericRecipe("ass.fracker").setup(600, 100).outputItems(new ItemStack(ModBlocks.machine_fracking_tower, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 24), new OreDictStack(STEEL.pipe(), 12), new ComparableStack(ModBlocks.concrete_smooth, 64), new ComparableStack(ModItems.drill_titanium), new ComparableStack(ModItems.motor_desh, 2), new ComparableStack(ModItems.plate_desh, 24), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.CAPACITOR))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.FERRO_PLATING), new OreDictStack(STEEL.pipe(), 12), new ComparableStack(ModBlocks.concrete_smooth, 64), new ComparableStack(ModItems.drill_titanium), new ComparableStack(ModItems.motor_desh, 5), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.CIRCUIT))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "plastic"));
        this.register(new GenericRecipe("ass.flarestack").setup(100, 100).outputItems(new ItemStack(ModBlocks.machine_flare, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 12), new OreDictStack(CU.plate(), 4), new OreDictStack(STEEL.shell(), 4), new ComparableStack(ModItems.thermo_element, 3))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.STEEL_PLATING), new OreDictStack(CU.plate(), 4), new ComparableStack(ModItems.thermo_element, 3))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "plastic"));
        this.register(new GenericRecipe("ass.refinery").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_refinery, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 3), new OreDictStack(CU.plate(), 8), new OreDictStack(STEEL.shell(), 4), new OreDictStack(STEEL.pipe(), 12), new ComparableStack(ModItems.plate_polymer, 8), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.ANALOG))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.STEEL_PLATING), new OreDictStack(STEEL.pipe(), 12), new ComparableStack(ModItems.plate_polymer, 8), new ComparableStack(ModItems.circuit, 5, EnumCircuitType.ANALOG)));
        this.register(new GenericRecipe("ass.crackingtower").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_catalytic_cracker, 1))
                .inputItems(new ComparableStack(ModBlocks.steel_scaffold, 16), new OreDictStack(STEEL.shell(), 6), new OreDictStack(DESH.ingot(), 12), new OreDictStack(NB.ingot(), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.STEEL_PLATING), new OreDictStack(ANY_PLASTIC.ingot(), 16), new OreDictStack(NB.ingot(), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "plastic"));
        this.register(new GenericRecipe("ass.radiolysis").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_radiolysis, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 4), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new OreDictStack(PB.plate(), 12), new OreDictStack(CU.plateCast(), 4), new OreDictStack(RUBBER.ingot(), 8), new ComparableStack(ModItems.thermo_element, 8))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.LEAD_PLATING), new OreDictStack(CU.plateCast(), 4), new OreDictStack(RUBBER.ingot(), 16), new ComparableStack(ModItems.thermo_element, 8)));
        this.register(new GenericRecipe("ass.coker").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_coker, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 8), new OreDictStack(STEEL.shell(), 4), new OreDictStack(CU.plate(), 8), new OreDictStack(RUBBER.ingot(), 4), new OreDictStack(NB.ingot(), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.STEEL_PLATING), new OreDictStack(RUBBER.ingot(), 16), new OreDictStack(NB.ingot(), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "rubber"));
        this.register(new GenericRecipe("ass.vaccumrefinery").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_vacuum_distill, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 16), new OreDictStack(CU.plate(), 16), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new ComparableStack(ModItems.sphere_steel, 1), new OreDictStack(STEEL.pipe(), 12), new ComparableStack(ModItems.motor_desh, 3), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CHIP_BISMOID))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.BRONZE_TUBES), new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.FERRO_PLATING), new OreDictStack(DURA.pipe(), 16), new ComparableStack(ModItems.motor_desh, 3), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.CHIP_BISMOID), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.COMPUTER)));
        this.register(new GenericRecipe("ass.reformer").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_catalytic_reformer, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 12), new OreDictStack(CU.plate(), 8), new OreDictStack(NB.ingot(), 8), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new OreDictStack(STEEL.shell(), 3), new OreDictStack(STEEL.pipe(), 8), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BISMOID))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.BRONZE_TUBES), new OreDictStack(NB.ingot(), 8), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new OreDictStack(DURA.pipe(), 16), new ComparableStack(ModItems.motor, 8), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BISMOID), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.COMPUTER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "hardplastic"));
        this.register(new GenericRecipe("ass.hydrotreater").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_hydrotreater, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 8), new OreDictStack(CU.plateCast(), 4), new OreDictStack(NB.ingot(), 8), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new OreDictStack(STEEL.shell(), 2), new OreDictStack(STEEL.pipe(), 8), new ComparableStack(ModItems.motor_desh, 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BISMOID))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.BRONZE_TUBES), new OreDictStack(NB.ingot(), 8), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new OreDictStack(DURA.pipe(), 16), new ComparableStack(ModItems.motor_desh, 8), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BISMOID), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.COMPUTER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "hardplastic"));
        this.register(new GenericRecipe("ass.pyrooven").setup(300, 100).outputItems(new ItemStack(ModBlocks.machine_pyrooven, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 16), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.ingot_cft, 4), new OreDictStack(CU.pipe(), 12), new ComparableStack(ModItems.motor_desh, 1), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BISMOID))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 6, EnumExpensiveType.BRONZE_TUBES), new OreDictStack(ANY_HARDPLASTIC.ingot(), 32), new ComparableStack(ModItems.ingot_cft, 4), new ComparableStack(ModItems.motor_bismuth, 4), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BISMOID), new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.COMPUTER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "hardplastic"));
        this.register(new GenericRecipe("ass.liquefactor").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_liquefactor, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 4), new OreDictStack(CU.plate(), 12), new OreDictStack(ANY_TAR.any(), 4), new ComparableStack(ModItems.circuit, 12, EnumCircuitType.CAPACITOR), new ComparableStack(ModItems.coil_tungsten, 8))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(ANY_TAR.any(), 16), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.CAPACITOR)));
        this.register(new GenericRecipe("ass.solidifier").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_solidifier, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 4), new OreDictStack(AL.plate(), 12), new OreDictStack(ANY_PLASTIC.ingot(), 4), new ComparableStack(ModItems.circuit, 12, EnumCircuitType.CAPACITOR), new ComparableStack(ModItems.coil_copper, 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(ANY_PLASTIC.ingot(), 16), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.CAPACITOR)));
        this.register(new GenericRecipe("ass.compressor").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_compressor, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 8), new OreDictStack(CU.plate(), 4), new OreDictStack(STEEL.shell(), 2), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ANALOG))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.STEEL_PLATING), new OreDictStack(STEEL.shell(), 4), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.ANALOG)));
        this.register(new GenericRecipe("ass.compactcompressor").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_compressor_compact, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 8), new OreDictStack(TI.shell(), 4), new OreDictStack(CU.pipe(), 4), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.STEEL_PLATING), new OreDictStack(CU.pipe(), 16), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.epress").setup(100, 100).outputItems(new ItemStack(ModBlocks.machine_epress, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 8), new OreDictStack(ANY_RUBBER.ingot(), 4), new ComparableStack(ModItems.part_generic, 2, EnumPartType.PISTON_HYDRAULIC.ordinal()), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.part_generic, 4, EnumPartType.PISTON_HYDRAULIC.ordinal()), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.fel").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_fel, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM), new OreDictStack(GOLD.wireDense(), 64), new OreDictStack(STEEL.plateCast(), 12), new OreDictStack(ANY_PLASTIC.ingot(), 16), new ComparableStack(ModItems.part_generic, 4, EnumPartType.GLASS_POLARIZED), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.CAPACITOR), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.FERRO_PLATING), new OreDictStack(GOLD.wireDense(), 64), new OreDictStack(ANY_PLASTIC.ingot(), 16), new ComparableStack(ModItems.part_generic, 4, EnumPartType.GLASS_POLARIZED), new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.silex").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_silex, 1))
                .inputItems(new ComparableStack(ModBlocks.glass_quartz, 16), new OreDictStack(STEEL.plateCast(), 8), new OreDictStack(DESH.ingot(), 4), new OreDictStack(RUBBER.ingot(), 8), new OreDictStack(STEEL.pipe(), 8))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModBlocks.glass_quartz, 16), new OreDictStack(RUBBER.ingot(), 16)));
        this.register(new GenericRecipe("ass.excavator").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_excavator, 1))
                .inputItems(new ComparableStack(Blocks.STONEBRICK, 8), new OreDictStack(STEEL.ingot(), 8), new OreDictStack(IRON.ingot(), 8), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ANALOG)));
        this.register(new GenericRecipe("ass.drillsteel").setup(100, 100).outputItems(new ItemStack(ModItems.drillbit, 1, EnumDrillType.STEEL.ordinal()))
                .inputItems(new OreDictStack(STEEL.ingot(), 12), new OreDictStack(W.ingot(), 4)));
        this.register(new GenericRecipe("ass.drillsteeldiamond").setup(100, 100).outputItems(new ItemStack(ModItems.drillbit, 1, EnumDrillType.STEEL_DIAMOND.ordinal()))
                .inputItems(new ComparableStack(ModItems.drillbit, 1, EnumDrillType.STEEL.ordinal()), new OreDictStack(DIAMOND.dust(), 16)));
        this.register(new GenericRecipe("ass.drilldura").setup(100, 100).outputItems(new ItemStack(ModItems.drillbit, 1, EnumDrillType.HSS.ordinal()))
                .inputItems(new OreDictStack(DURA.ingot(), 12), new OreDictStack(ANY_PLASTIC.ingot(), 12), new OreDictStack(TI.ingot(), 8)));
        this.register(new GenericRecipe("ass.drillduradiamond").setup(100, 100).outputItems(new ItemStack(ModItems.drillbit, 1, EnumDrillType.HSS_DIAMOND.ordinal()))
                .inputItems(new ComparableStack(ModItems.drillbit, 1, EnumDrillType.HSS.ordinal()), new OreDictStack(DIAMOND.dust(), 24)));
        this.register(new GenericRecipe("ass.drilldesh").setup(100, 100).outputItems(new ItemStack(ModItems.drillbit, 1, EnumDrillType.DESH.ordinal()))
                .inputItems(new OreDictStack(DESH.ingot(), 16), new OreDictStack(RUBBER.ingot(), 12), new OreDictStack(NB.ingot(), 4)));
        this.register(new GenericRecipe("ass.drilldeshdiamond").setup(100, 100).outputItems(new ItemStack(ModItems.drillbit, 1, EnumDrillType.DESH_DIAMOND.ordinal()))
                .inputItems(new ComparableStack(ModItems.drillbit, 1, EnumDrillType.DESH.ordinal()), new OreDictStack(DIAMOND.dust(), 32)));
        this.register(new GenericRecipe("ass.drilltc").setup(100, 100).outputItems(new ItemStack(ModItems.drillbit, 1, EnumDrillType.TCALLOY.ordinal()))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.ingot(), 20), new OreDictStack(DESH.ingot(), 12), new OreDictStack(RUBBER.ingot(), 8)));
        this.register(new GenericRecipe("ass.drilltcdiamond").setup(100, 100).outputItems(new ItemStack(ModItems.drillbit, 1, EnumDrillType.TCALLOY_DIAMOND.ordinal()))
                .inputItems(new ComparableStack(ModItems.drillbit, 1, EnumDrillType.TCALLOY.ordinal()), new OreDictStack(DIAMOND.dust(), 48)));
        this.register(new GenericRecipe("ass.drillferro").setup(100, 100).outputItems(new ItemStack(ModItems.drillbit, 1, EnumDrillType.FERRO.ordinal()))
                .inputItems(new OreDictStack(FERRO.ingot(), 24), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 12), new OreDictStack(BI.ingot(), 4)));
        this.register(new GenericRecipe("ass.drillferrodiamond").setup(100, 100).outputItems(new ItemStack(ModItems.drillbit, 1, EnumDrillType.FERRO_DIAMOND.ordinal()))
                .inputItems(new ComparableStack(ModItems.drillbit, 1, EnumDrillType.FERRO.ordinal()), new OreDictStack(DIAMOND.dust(), 56)));
        this.register(new GenericRecipe("ass.slopper").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_ore_slopper, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 6), new OreDictStack(TI.plate(), 8), new OreDictStack(CU.pipe(), 3), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ANALOG)));
        this.register(new GenericRecipe("ass.mininglaser").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_mining_laser, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 16), new OreDictStack(TI.shell(), 4), new OreDictStack(DURA.plate(), 4), new ComparableStack(ModItems.crystal_redstone, 3), new ComparableStack(Items.DIAMOND, 3), new OreDictStack(ANY_PLASTIC.ingot(), 8), new ComparableStack(ModItems.motor, 3))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(DURA.plate(), 4), new ComparableStack(ModItems.crystal_redstone, 12), new OreDictStack(ANY_PLASTIC.ingot(), 16), new ComparableStack(ModItems.motor_desh, 3)));
        this.register(new GenericRecipe("ass.teleporter").setup(100, 100).outputItems(new ItemStack(ModBlocks.machine_teleporter, 1))
                .inputItems(new OreDictStack(TI.plate(), 12), new OreDictStack(DURA.plate(), 12), new OreDictStack(GOLD.wireFine(), 32), new ComparableStack(ModItems.entanglement_kit, 1), new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM)));
        this.register(new GenericRecipe("ass.radar").setup(300, 100).outputItems(new ItemStack(ModBlocks.machine_radar, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 12), new OreDictStack(ANY_RUBBER.ingot(), 12), new ComparableStack(ModItems.magnetron, 5), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.BASIC.ordinal()), new ComparableStack(ModItems.crt_display, 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.magnetron, 16), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.CIRCUIT), new ComparableStack(ModItems.crt_display, 4)));
        this.register(new GenericRecipe("ass.radarlarge").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_radar_large, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 6), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new OreDictStack(ANY_RUBBER.ingot(), 24), new ComparableStack(ModItems.magnetron, 16), new ComparableStack(ModItems.motor_desh, 1), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.ADVANCED), new ComparableStack(ModItems.crt_display, 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 8), new OreDictStack(ANY_RUBBER.ingot(), 24), new ComparableStack(ModItems.magnetron, 16), new ComparableStack(ModItems.motor_desh, 3), new ComparableStack(ModItems.item_expensive, 6, EnumExpensiveType.CIRCUIT), new ComparableStack(ModItems.crt_display, 4)));
        this.register(new GenericRecipe("ass.forcefield").setup(600, 100).outputItems(new ItemStack(ModBlocks.machine_forcefield, 1))
                .inputItems(new OreDictStack(DURA.plate(), 8), new ComparableStack(ModItems.plate_desh, 4), new ComparableStack(ModItems.coil_gold_torus, 6), new ComparableStack(ModItems.coil_magnetized_tungsten, 12), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.upgrade_radius, 1), new ComparableStack(ModItems.upgrade_health, 1), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.ADVANCED), new ComparableStack(ModBlocks.machine_transformer, 1)));

        this.register(new GenericRecipe("ass.strandcaster").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_strand_caster, 1))
                .inputItems(new ComparableStack(ModItems.ingot_firebrick, 16), new OreDictStack(STEEL.plateCast(), 6), new OreDictStack(CU.plateWelded(), 2), new OreDictStack(STEEL.shell(), 2), new OreDictStack(ANY_CONCRETE.any(), 8))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.ingot_firebrick, 16), new OreDictStack(STEEL.shell(), 4), new OreDictStack(ANY_CONCRETE.any(), 8)));
        this.register(new GenericRecipe("ass.assemfac").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_assembly_factory, 1))
                .inputItems(new OreDictStack(DURA.ingot(), 16), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 8), new OreDictStack(RUBBER.ingot(), 16), new OreDictStack(B.ingot(), 8), new OreDictStack(STEEL.shell(), 4), new ComparableStack(ModItems.motor, 12), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 16), new OreDictStack(RUBBER.ingot(), 16), new OreDictStack(B.ingot(), 8), new OreDictStack(STEEL.shell(), 4), new ComparableStack(ModItems.motor, 24), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.chemfac").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_chemical_factory, 1))
                .inputItems(new OreDictStack(DURA.ingot(), 16), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 8), new OreDictStack(RUBBER.ingot(), 16), new OreDictStack(STEEL.shell(), 12), new OreDictStack(CU.pipe(), 8), new ComparableStack(ModItems.motor_desh, 4), new ComparableStack(ModItems.coil_tungsten, 16), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 16), new OreDictStack(RUBBER.ingot(), 16), new OreDictStack(STEEL.shell(), 12), new OreDictStack(CU.pipe(), 16), new ComparableStack(ModItems.motor_desh, 16), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.CIRCUIT)));

        // generators
        this.register(new GenericRecipe("ass.dieselgen").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_diesel, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 1), new OreDictStack(CU.plateCast(), 2), new ComparableStack(ModItems.coil_copper, 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.STEEL_PLATING), new OreDictStack(CU.pipe(), 2), new ComparableStack(ModItems.coil_copper, 4)));
        this.register(new GenericRecipe("ass.combustiongen").setup(300, 100).outputItems(new ItemStack(ModBlocks.machine_combustion_engine, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 16), new OreDictStack(CU.ingot(), 12), new OreDictStack(GOLD.wireDense(), 8), new ComparableStack(ModItems.canister_empty, 4), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(GOLD.wireDense(), 16), new ComparableStack(ModItems.canister_empty, 4), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.pistonsetsteel").setup(200, 100).outputItems(new ItemStack(ModItems.piston_set, 1, ItemPistons.EnumPistonType.STEEL.ordinal()))
                .inputItems(new OreDictStack(STEEL.plate(), 16), new OreDictStack(CU.plate(), 4), new OreDictStack(W.ingot(), 8), new OreDictStack(W.bolt(), 16)));
        this.register(new GenericRecipe("ass.pistonsetdura").setup(200, 100).outputItems(new ItemStack(ModItems.piston_set, 1, ItemPistons.EnumPistonType.DURA.ordinal()))
                .inputItems(new OreDictStack(DURA.ingot(), 24), new OreDictStack(TI.plate(), 8), new OreDictStack(W.ingot(), 8), new OreDictStack(DURA.bolt(), 16)));
        this.register(new GenericRecipe("ass.pistonsetdesh").setup(200, 100).outputItems(new ItemStack(ModItems.piston_set, 1, ItemPistons.EnumPistonType.DESH.ordinal()))
                .inputItems(new OreDictStack(DESH.ingot(), 24), new OreDictStack(ANY_PLASTIC.ingot(), 12), new OreDictStack(CU.plate(), 24), new OreDictStack(W.ingot(), 16), new OreDictStack(DURA.pipe(), 4)));
        this.register(new GenericRecipe("ass.pistonsetstar").setup(200, 100).outputItems(new ItemStack(ModItems.piston_set, 1, ItemPistons.EnumPistonType.STARMETAL.ordinal()))
                .inputItems(new OreDictStack(STAR.ingot(), 24), new OreDictStack(RUBBER.ingot(), 16), new OreDictStack(BIGMT.plate(), 24), new OreDictStack(NB.ingot(), 16), new OreDictStack(DURA.pipe(), 4)));
        this.register(new GenericRecipe("ass.turbofan").setup(300, 100).outputItems(new ItemStack(ModBlocks.machine_turbofan, 1))
                .inputItems(new OreDictStack(TI.shell(), 8), new OreDictStack(DURA.pipe(), 4), new OreDictStack(ANY_PLASTIC.ingot(), 12), new ComparableStack(ModItems.turbine_tungsten, 1), new OreDictStack(GOLD.wireDense(), 12), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.BASIC.ordinal()))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(ANY_PLASTIC.ingot(), 16), new ComparableStack(ModItems.turbine_tungsten, 3), new OreDictStack(GOLD.wireDense(), 16), new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.gasturbine").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_turbinegas, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 10), new OreDictStack(GOLD.wireDense(), 12), new OreDictStack(DURA.pipe(), 4),new OreDictStack(STEEL.pipe(), 4), new ComparableStack(ModItems.turbine_tungsten, 1), new ComparableStack(ModItems.ingot_rubber, 12), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.BASIC.ordinal()))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(GOLD.wireDense(), 16), new OreDictStack(DURA.pipe(), 16), new ComparableStack(ModItems.turbine_tungsten, 2), new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.hephaestus").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_hephaestus, 1))
                .inputItems(new OreDictStack(STEEL.pipe(), 12), new OreDictStack(STEEL.ingot(), 24), new OreDictStack(CU.plate(), 24), new OreDictStack(NB.ingot(), 4), new OreDictStack(RUBBER.ingot(), 12), new ComparableStack(ModBlocks.glass_quartz, 16))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(NB.ingot(), 16), new OreDictStack(RUBBER.ingot(), 16), new ComparableStack(ModBlocks.glass_quartz, 16)));
        this.register(new GenericRecipe("ass.iturbine").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_industrial_turbine, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 12), new OreDictStack(RUBBER.ingot(), 4), new ComparableStack(ModItems.turbine_titanium, 2), new OreDictStack(GOLD.wireDense(), 6), new OreDictStack(DURA.pipe(), 3), new OreDictStack(STEEL.pipe(), 4), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.turbine_titanium, 3), new OreDictStack(GOLD.wireDense(), 16), new OreDictStack(DURA.pipe(), 16), new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.largeturbine").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_large_turbine, 1))
                .inputItems(new ComparableStack(ModItems.generator_steel, 1), new OreDictStack(STEEL.plate(), 12), new OreDictStack(RUBBER.ingot(), 4), new ComparableStack(ModItems.turbine_titanium, 3), new OreDictStack(GOLD.wireDense(), 6), new OreDictStack(DURA.pipe(), 3), new OreDictStack(STEEL.pipe(), 4), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.turbine_titanium, 3), new OreDictStack(GOLD.wireDense(), 16), new OreDictStack(DURA.pipe(), 16), new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.rtgfurnace").setup(150, 100).outputItems(new ItemStack(ModBlocks.machine_rtg_furnace_off, 1))
                .inputItems(new ComparableStack(Blocks.FURNACE, 1), new ComparableStack(ModItems.rtg_unit, 3), new OreDictStack(PB.plate(), 6), new OreDictStack(OreDictManager.getReflector(), 4), new OreDictStack(CU.plate(), 2)));
        this.register(new GenericRecipe("ass.railgun").setup(500, 100).outputItems(new ItemStack(ModBlocks.railgun_plasma, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 24), new ComparableStack(ModItems.hull_big_steel, 2), new ComparableStack(ModItems.hull_small_steel, 6), new ComparableStack(ModItems.pipes_steel, 2), new ComparableStack(ModBlocks.machine_schrabidium_battery, 1), new ComparableStack(ModItems.coil_copper, 16), new ComparableStack(ModItems.coil_copper_torus, 8), new ComparableStack(ModItems.plate_desh, 4), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.ADVANCED), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CAPACITOR), new OreDictStack(ANY_PLASTIC.ingot(), 4)));
        this.register(new GenericRecipe("ass.leviturbine").setup(600, 100).outputItems(new ItemStack(ModBlocks.machine_chungus, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 6), new OreDictStack(STEEL.plateWelded(), 16), new OreDictStack(TI.plate(), 12), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 16), new ComparableStack(ModItems.turbine_tungsten, 5), new ComparableStack(ModItems.turbine_titanium, 3), new ComparableStack(ModItems.flywheel_beryllium, 1), new OreDictStack(GOLD.wireDense(), 48), new OreDictStack(DURA.pipe(), 16), new OreDictStack(STEEL.pipe(), 16))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModItems.turbine_tungsten, 5), new ComparableStack(ModItems.turbine_titanium, 12), new ComparableStack(ModItems.flywheel_beryllium, 1), new OreDictStack(GOLD.wireDense(), 64)));
        this.register(new GenericRecipe("ass.radgen").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_radgen, 1)).setPools(GenericRecipes.POOL_PREFIX_DISCOVER + "radgen")
                .inputItems(new OreDictStack(STEEL.ingot(), 8), new OreDictStack(STEEL.plate(), 32), new ComparableStack(ModItems.coil_magnetized_tungsten, 6), new OreDictStack(MAGTUNG.wireFine(), 24), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.BASIC), new ComparableStack(ModItems.reactor_core, 3), new OreDictStack(STAR.ingot(), 1), new OreDictStack(KEY_RED, 1))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.coil_magnetized_tungsten, 16), new ComparableStack(ModItems.reactor_core, 3), new OreDictStack(STAR.ingot(), 1), new OreDictStack(KEY_RED, 1), new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.CIRCUIT)));

        // condensers
        this.register(new GenericRecipe("ass.hpcondenser").setup(600, 100).outputItems(new ItemStack(ModBlocks.machine_condenser_powered, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 8), new OreDictStack(ANY_RESISTANTALLOY.plateWelded(), 4), new OreDictStack(CU.plate528(), 16), new ComparableStack(ModItems.motor_desh, 3), new OreDictStack(STEEL.pipe(), 24), new OreDictStack(Fluids.LUBRICANT.getDict(1_000), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModItems.motor_desh, 5), new OreDictStack(STEEL.pipe(), 24), new OreDictStack(Fluids.LUBRICANT.getDict(1_000), 16)));

        // batteries
        this.register(new GenericRecipe("ass.capacitorgold").setup(100, 100).outputItems(new ItemStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.CAPACITOR_GOLD.ordinal()))
                .inputItems(new OreDictStack(STEEL.plate(), 8),
                        new OreDictStack(GOLD.wireDense(), 16)));
        this.register(new GenericRecipe("ass.capacitorniobium").setup(100, 1_000).outputItems(new ItemStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.CAPACITOR_NIOBIUM.ordinal()))
                .inputItems(new OreDictStack(ANY_PLASTIC.ingot(), 12),
                        new OreDictStack(NB.wireDense(), 24)));
        this.register(new GenericRecipe("ass.capacitortantalum").setup(100, 10_000).outputItems(new ItemStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.CAPACITOR_TANTALUM.ordinal()))
                .inputItems(new OreDictStack(ANY_HARDPLASTIC.ingot(), 16),
                        new OreDictStack(TA.ingot(), 24)));
        this.register(new GenericRecipe("ass.capacitorbismuth").setup(100, 25_000).outputItems(new ItemStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.CAPACITOR_BISMUTH.ordinal()))
                .inputItems(new OreDictStack(ANY_HARDPLASTIC.ingot(), 24),
                        new OreDictStack(BI.ingot(), 24),
                        new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CHIP_QUANTUM)));
        this.register(new GenericRecipe("ass.capacitorspark").setup(100, 100_000).outputItems(new ItemStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.CAPACITOR_SPARK.ordinal()))
                .inputItems(new OreDictStack(CMB.plateCast(), 12),
                        new ComparableStack(ModItems.powder_spark_mix, 32),
                        new ComparableStack(ModItems.pellet_charged, 32),
                        new ComparableStack(ModItems.circuit, 16, EnumCircuitType.CHIP_QUANTUM))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 8_000))
                .outputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 8_000)));
        this.register(new GenericRecipe("ass.machinebattery").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_battery, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 4), new OreDictStack(S.dust(), 12), new OreDictStack(PB.dust(), 12), new OreDictStack(MINGRADE.ingot(), 2), new ComparableStack(ModItems.wire, 4, Mats.MAT_MINGRADE.id)));
        this.register(new GenericRecipe("ass.machinelithiumbattery").setup(300, 100).outputItems(new ItemStack(ModBlocks.machine_lithium_battery, 1))
                .inputItems(new OreDictStack(POLYMER.ingot(), 6), new OreDictStack(CO.dust(), 12), new OreDictStack(LI.dust(), 12), new OreDictStack(ALLOY.ingot(), 3), new ComparableStack(ModItems.wire, 4, Mats.MAT_MINGRADE.id)));
        this.register(new GenericRecipe("ass.machinedeshbattery").setup(400, 100).outputItems(new ItemStack(ModBlocks.machine_desh_battery, 1))
                .inputItems(new OreDictStack(DESH.ingot(), 12), new OreDictStack(BAKELITE.dust(), 12), new OreDictStack(P_RED.dust(), 12), new OreDictStack(GRAPHITE.ingot(), 6), new OreDictStack(RA226.nugget(), 2)));
        this.register(new GenericRecipe("ass.machinesaturnitebattery").setup(600, 100).outputItems(new ItemStack(ModBlocks.machine_saturnite_battery, 1))
                .inputItems(new OreDictStack(BIGMT.ingot(), 8), new OreDictStack(DURA.dust(), 12), new OreDictStack(ZR.dust(), 12), new OreDictStack(PU238.ingot(), 4), new OreDictStack(TC99.nugget(), 2)));
        this.register(new GenericRecipe("ass.machineschrabidiumbattery").setup(800, 100).outputItems(new ItemStack(ModBlocks.machine_schrabidium_battery, 1))
                .inputItems(new OreDictStack(SA326.ingot(), 16), new OreDictStack(SA326.dust(), 12), new OreDictStack(NP237.dust(), 12), new OreDictStack(REIIUM.ingot(), 8), new OreDictStack(ANY_BISMOID.nugget(), 2)));
        this.register(new GenericRecipe("ass.machinedineutroniumbattery").setup(4800, 100).outputItems(new ItemStack(ModBlocks.machine_dineutronium_battery, 1))
                .inputItems(new OreDictStack(DNT.ingot(), 48), new ComparableStack(ModItems.powder_spark_mix, 32), new ComparableStack(ModItems.battery_spark_cell_1000, 1), new OreDictStack(CMB.ingot(), 24), new ComparableStack(ModItems.coil_magnetized_tungsten, 12)));
        this.register(new GenericRecipe("ass.machineeuphemiumbattery").setup(1600, 100).outputItems(new ItemStack(ModBlocks.machine_euphemium_battery, 1))
                .inputItems(new OreDictStack(EUPH.ingot(), 24), new OreDictStack(XE135.dust(), 16), new OreDictStack(SBD.dust(), 16), new OreDictStack(STAR.ingot(), 12), new OreDictStack(GH336.nugget(), 8)));
        this.register(new GenericRecipe("ass.machineradspicebattery").setup(3200, 100).outputItems(new ItemStack(ModBlocks.machine_radspice_battery, 1))
                .inputItems(new ComparableStack(ModItems.ingot_radspice, 32), new ComparableStack(ModItems.powder_chlorophyte, 24), new ComparableStack(ModItems.powder_balefire, 24), new ComparableStack(ModItems.ingot_verticium, 16), new OreDictStack(AS.nugget(), 8)));
        this.register(new GenericRecipe("ass.machineelectroniumbattery").setup(6400, 100).outputItems(new ItemStack(ModBlocks.machine_electronium_battery, 1))
                .inputItems(new ComparableStack(ModItems.ingot_electronium, 64), new OreDictStack(OSMIRIDIUM.dust(), 48), new ComparableStack(ModItems.battery_spark_cell_1000, 1), new OreDictStack(AU198.ingot(), 32), new ComparableStack(ModItems.nugget_u238m2, 16)));
        // fluid tanks
        this.register(new GenericRecipe("ass.tank").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_fluidtank, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 8), new OreDictStack(STEEL.shell(), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.STEEL_PLATING), new OreDictStack(ANY_TAR.any(), 16)));
        this.register(new GenericRecipe("ass.bat9k").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_bat9000, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 16), new OreDictStack(ANY_RESISTANTALLOY.plateWelded(), 2), new ComparableStack(ModBlocks.steel_scaffold, 16), new OreDictStack(ANY_TAR.any(), 16))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModBlocks.steel_scaffold, 16), new OreDictStack(ANY_TAR.any(), 16)));
        this.register(new GenericRecipe("ass.orbus").setup(300, 100).outputItems(new ItemStack(ModBlocks.machine_orbus, 1))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.plateWelded(), 8), new OreDictStack(BIGMT.plateCast(), 4), new OreDictStack(BSCCO.wireDense(), 8), new ComparableStack(ModItems.battery_sc, 1, ItemBatterySC.EnumBatterySC.PO210))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.FERRO_PLATING), new OreDictStack(BIGMT.plateCast(), 16), new OreDictStack(BSCCO.wireDense(), 16), new ComparableStack(ModItems.battery_sc, 1, ItemBatterySC.EnumBatterySC.PO210)));

        // accelerators
        this.register(new GenericRecipe("ass.cyclotron").setup(600, 100).outputItems(new ItemStack(ModBlocks.machine_cyclotron, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM), new OreDictStack(ND.wireDense(), 32), new OreDictStack(STEEL.plateCast(), 16), new OreDictStack(STEEL.plate(), 32), new OreDictStack(AL.plate(), 32), new OreDictStack(ANY_PLASTIC.ingot(), 24), new OreDictStack(RUBBER.ingot(), 24), new OreDictStack(CU.plateCast(), 8), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.FERRO_PLATING), new OreDictStack(ND.wireDense(), 32), new OreDictStack(AL.plateWelded(), 16), new OreDictStack(RUBBER.ingot(), 32), new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.schrabidium_transmutator").setup(500, 100).outputItems(new ItemStack(ModBlocks.machine_schrabidium_transmutator, 1))
                .inputItems(new OreDictStack(MAGTUNG.ingot(), 1), new OreDictStack(TI.ingot(), 24), new OreDictStack(ALLOY.plate(), 18), new OreDictStack(STEEL.plateWelded(), 12), new ComparableStack(ModItems.plate_desh, 6), new OreDictStack(RUBBER.ingot(), 8), new ComparableStack(ModBlocks.machine_battery, 5), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.ADVANCED)));
        this.register(new GenericRecipe("ass.beamline").setup(200, 100).outputItems(new ItemStack(ModBlocks.pa_beamline, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 8), new OreDictStack(CU.plate(), 16), new OreDictStack(GOLD.wireDense(), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.STEEL_PLATING), new OreDictStack(CU.plate(), 16), new OreDictStack(GOLD.wireDense(), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.rfc").setup(400, 100).outputItems(new ItemStack(ModBlocks.pa_rfc, 1))
                .inputItems(new ComparableStack(ModBlocks.pa_beamline, 3), new OreDictStack(STEEL.plateCast(), 16), new OreDictStack(CU.plate(), 64), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.magnetron, 16))
                .inputItemsEx(new ComparableStack(ModBlocks.pa_beamline, 3), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.LEAD_PLATING), new OreDictStack(CU.plate(), 64), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.magnetron, 16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.quadrupole").setup(400, 100).outputItems(new ItemStack(ModBlocks.pa_quadrupole, 1))
                .inputItems(new ComparableStack(ModBlocks.pa_beamline, 1), new OreDictStack(STEEL.plateCast(), 16), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BISMOID))
                .inputItemsEx(new ComparableStack(ModBlocks.pa_beamline, 1), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.LEAD_PLATING), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BISMOID), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.COMPUTER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.dipole").setup(400, 100).outputItems(new ItemStack(ModBlocks.pa_dipole, 1))
                .inputItems(new ComparableStack(ModBlocks.pa_beamline, 2), new OreDictStack(STEEL.plateCast(), 16), new OreDictStack(ANY_HARDPLASTIC.ingot(), 32), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BISMOID))
                .inputItemsEx(new ComparableStack(ModBlocks.pa_beamline, 2), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.LEAD_PLATING), new OreDictStack(ANY_HARDPLASTIC.ingot(), 32), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.BISMOID), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.COMPUTER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.source").setup(400, 100).outputItems(new ItemStack(ModBlocks.pa_source, 1))
                .inputItems(new ComparableStack(ModBlocks.pa_beamline, 3), new OreDictStack(STEEL.plateCast(), 16), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.magnetron, 16), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.QUANTUM))
                .inputItemsEx(new ComparableStack(ModBlocks.pa_beamline, 3), new ComparableStack(ModItems.item_expensive, 16, EnumExpensiveType.LEAD_PLATING), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.magnetron, 16), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.QUANTUM), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.COMPUTER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.detector").setup(400, 100).outputItems(new ItemStack(ModBlocks.pa_detector, 1))
                .inputItems(new ComparableStack(ModBlocks.pa_beamline, 3), new OreDictStack(STEEL.plateCast(), 24), new OreDictStack(GOLD.wireDense(), 16), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.QUANTUM))
                .inputItemsEx(new ComparableStack(ModBlocks.pa_beamline, 3), new ComparableStack(ModItems.item_expensive, 32, EnumExpensiveType.LEAD_PLATING), new OreDictStack(GOLD.wireDense(), 64), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.QUANTUM), new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.COMPUTER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));
        this.register(new GenericRecipe("ass.pagold").setup(400, 100).outputItems(new ItemStack(ModItems.pa_coil, 1, ItemPACoil.EnumCoilType.GOLD.ordinal())).inputItems(new OreDictStack(GOLD.wireDense(), 64), new OreDictStack(GOLD.wireDense(), 64)));
        this.register(new GenericRecipe("ass.panbti").setup(400, 100).outputItems(new ItemStack(ModItems.pa_coil, 1, ItemPACoil.EnumCoilType.NIOBIUM.ordinal())).inputItems(new OreDictStack(NB.wireDense(), 64), new OreDictStack(TI.wireDense(), 64)));
        this.register(new GenericRecipe("ass.pabscco").setup(400, 100).outputItems(new ItemStack(ModItems.pa_coil, 1, ItemPACoil.EnumCoilType.BSCCO.ordinal())).inputItems(new OreDictStack(BSCCO.wireDense(), 64), new OreDictStack(ANY_PLASTIC.ingot(), 64)));
        this.register(new GenericRecipe("ass.pachlorophyte").setup(400, 100).outputItems(new ItemStack(ModItems.pa_coil, 1, ItemPACoil.EnumCoilType.CHLOROPHYTE.ordinal())).inputItems(new OreDictStack(CU.wireDense(), 64), new OreDictStack(CU.wireDense(), 64), new ComparableStack(ModItems.powder_chlorophyte, 16)));
        this.register(new GenericRecipe("ass.exposurechamber").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_exposure_chamber, 1))
                .inputItems(new OreDictStack(AL.plateCast(), 12), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new OreDictStack(ANY_HARDPLASTIC.ingot(), 12), new OreDictStack(GOLD.wireDense(), 32), new ComparableStack(ModItems.motor_desh, 2), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BISMOID), new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.CAPACITOR_TANTALUM), new ComparableStack(ModBlocks.glass_quartz, 16))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.LEAD_PLATING), new OreDictStack(ANY_HARDPLASTIC.ingot(), 24), new OreDictStack(GOLD.wireDense(), 32), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BISMOID), new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.COMPUTER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"));

        // reactors
        this.register(new GenericRecipe("ass.breedingreactor").setup(200, 100).outputItems(new ItemStack(ModBlocks.machine_reactor_breeding, 1))
                .inputItems(new ComparableStack(ModItems.reactor_core, 1), new OreDictStack(STEEL.ingot(), 12), new OreDictStack(PB.plate(), 16), new ComparableStack(ModBlocks.reinforced_glass, 4), new OreDictStack(ASBESTOS.ingot(), 4), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new ComparableStack(ModItems.crt_display, 1))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.LEAD_PLATING), new ComparableStack(ModItems.reactor_core, 1), new OreDictStack(ASBESTOS.ingot(), 16), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new ComparableStack(ModItems.crt_display, 1)));
        this.register(new GenericRecipe("ass.researchreactor").setup(200, 100).outputItems(new ItemStack(ModBlocks.reactor_research, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 8), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new ComparableStack(ModItems.motor_desh, 2), new OreDictStack(B.ingot(), 5), new OreDictStack(PB.plate(), 8), new ComparableStack(ModItems.crt_display, 3), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModItems.motor_desh, 2), new ComparableStack(ModItems.crt_display, 3), new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.cirnox").setup(600, 100).outputItems(new ItemStack(ModBlocks.reactor_zirnox, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 4), new OreDictStack(STEEL.pipe(), 8), new OreDictStack(B.ingot(), 8), new OreDictStack(GRAPHITE.ingot(), 16), new OreDictStack(RUBBER.ingot(), 16), new OreDictStack(ANY_CONCRETE.any(), 16), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.LEAD_PLATING), new OreDictStack(GRAPHITE.ingot(), 16), new OreDictStack(RUBBER.ingot(), 16), new OreDictStack(ANY_CONCRETE.any(), 16), new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.rbmk").setup(100, 100).outputItems(new ItemStack(ModBlocks.rbmk_blank, 1))
                .inputItems(new ComparableStack(ModBlocks.concrete_asbestos, 4), new OreDictStack(STEEL.plateCast(), 2), new OreDictStack(CU.plate(), 4), new OreDictStack(RUBBER.ingot(), 2))
                .inputItemsEx(new ComparableStack(ModBlocks.concrete_asbestos, 4), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.FERRO_PLATING), new OreDictStack(CU.plate(), 16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "ferrouranium"));
        this.register(new GenericRecipe("ass.rbmkautoloader").setup(100, 100).outputItems(new ItemStack(ModBlocks.rbmk_autoloader, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 4), new OreDictStack(PB.plateCast(), 4), new OreDictStack(B.ingot(), 4), new ComparableStack(ModItems.motor, 3))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModItems.motor_desh, 3))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "ferrouranium"));

        // PWR
        this.register(new GenericRecipe("ass.pwrfuel").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_fuelrod, 4))
                .inputItems(new OreDictStack(PB.plateCast(), 4), new OreDictStack(ZR.plateWelded(), 2))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.LEAD_PLATING), new OreDictStack(ZR.plateWelded(), 2)));
        this.register(new GenericRecipe("ass.pwrcontrol").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_control, 4))
                .inputItems(new OreDictStack(STEEL.plateCast(), 2), new OreDictStack(B.ingot(), 4), new ComparableStack(ModItems.motor, 1))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.STEEL_PLATING), new OreDictStack(B.ingot(), 4), new ComparableStack(ModItems.motor, 1)));
        this.register(new GenericRecipe("ass.pwrchannel").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_channel, 4))
                .inputItems(new OreDictStack(STEEL.pipe(), 4), new OreDictStack(CU.plate(), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.STEEL_PLATING), new OreDictStack(STEEL.pipe(), 4)));
        this.register(new GenericRecipe("ass.pwrheatex").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_heatex, 4))
                .inputItems(new OreDictStack(CU.plateCast(), 4), new ComparableStack(ModItems.motor, 1))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.STEEL_PLATING), new OreDictStack(CU.plateCast(), 4))
                .inputFluidsEx(new FluidStack(Fluids.PERFLUOROMETHYL, 4_000)));
        this.register(new GenericRecipe("ass.pwrheatsink").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_heatsink, 4))
                .inputItems(new OreDictStack(BIGMT.plateCast(), 4), new OreDictStack(CU.plateCast(), 4))
                .inputItemsEx(new OreDictStack(BIGMT.plateCast(), 4), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.FERRO_PLATING)));
        this.register(new GenericRecipe("ass.pwrreflector").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_reflector, 4))
                .inputItems(new OreDictStack(STEEL.plateCast(), 2), new OreDictStack(OreDictManager.getReflector(), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.STEEL_PLATING), new OreDictStack(OreDictManager.getReflector(), 16)));
        this.register(new GenericRecipe("ass.pwrreflectoralt").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_reflector, 4))
                .inputItems(new OreDictStack(STEEL.plateCast(), 2), new OreDictStack(WEAPONSTEEL.plate(), 16))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.STEEL_PLATING), new OreDictStack(WEAPONSTEEL.plate(), 32)));
        this.register(new GenericRecipe("ass.pwrcasing").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_casing, 4))
                .inputItems(new OreDictStack(PB.plate(), 4), new OreDictStack(ANY_CONCRETE.any(), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.LEAD_PLATING), new OreDictStack(ANY_CONCRETE.any(), 4)));
        this.register(new GenericRecipe("ass.pwrcontroller").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_controller, 1))
                .inputItems(new OreDictStack(PB.plateCast(), 4), new OreDictStack(RUBBER.ingot(), 4), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.LEAD_PLATING), new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.CIRCUIT)));
        this.register(new GenericRecipe("ass.pwrport").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_port, 4))
                .inputItems(new OreDictStack(PB.plate(), 4), new OreDictStack(ANY_CONCRETE.any(), 4), new OreDictStack(STEEL.pipe(), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.LEAD_PLATING), new OreDictStack(ANY_CONCRETE.any(), 4), new OreDictStack(STEEL.pipe(), 4)));
        this.register(new GenericRecipe("ass.pwrneutronsource").setup(200, 500).outputItems(new ItemStack(ModBlocks.pwr_neutron_source, 1))
                .inputItems(new OreDictStack(ZR.plateWelded(), 1), new ComparableStack(ModItems.billet_ra226be, 3))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.LEAD_PLATING), new OreDictStack(ZR.plateWelded(), 1), new ComparableStack(ModItems.billet_ra226be, 3)));

        // fusion reactor
        this.register(new GenericRecipe("ass.fusioncore").setup(600, 100).outputItems(new ItemStack(ModBlocks.struct_torus_core, 1))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.plateWelded(), 8), new OreDictStack(ANY_HARDPLASTIC.ingot(), 32), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.BISMOID))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.PLASTIC), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.BISMOID))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.fusionbscco").setup(100, 100).outputItems(new ItemStack(ModBlocks.fusion_component, 2, 0))
                .inputItems(new OreDictStack(BSCCO.wireDense(), 1), new OreDictStack(CU.pipe(), 1), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 1), new OreDictStack(ANY_PLASTIC.ingot(), 4))
                .inputItemsEx(new OreDictStack(BSCCO.wireDense(), 4), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.PLASTIC))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.fusionblanket").setup(100, 100).outputItems(new ItemStack(ModBlocks.fusion_component, 4, 2))
                .inputItems(new OreDictStack(W.plateWelded(), 1), new OreDictStack(STEEL.plateWelded(), 2), new OreDictStack(BE.ingot(), 4))
                .inputItemsEx(new OreDictStack(W.plateWelded(), 4), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.HEAVY_FRAME), new OreDictStack(BE.ingot(), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.fusionpipes").setup(100, 100).outputItems(new ItemStack(ModBlocks.fusion_component, 4, 3))
                .inputItems(new OreDictStack(ANY_HARDPLASTIC.ingot(), 4), new OreDictStack(CU.pipe(), 2), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.PLASTIC), new OreDictStack(CU.pipe(), 4), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.CIRCUIT))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.fusionshieldtungsten").setup(600, 100).outputItems(new ItemStack(ModItems.fusion_shield_tungsten, 1))
                .inputItems(new OreDictStack(W.block(), 32), new OreDictStack(getReflector(), 64), new OreDictStack(getReflector(), 32)));
        this.register(new GenericRecipe("ass.fusionshielddesh").setup(600, 100).outputItems(new ItemStack(ModItems.fusion_shield_desh, 1))
                .inputItems(new OreDictStack(DESH.block(), 16), new OreDictStack(CO.block(), 16), new OreDictStack(BIGMT.plate(), 64), new OreDictStack(BIGMT.plate(), 32)));
        this.register(new GenericRecipe("ass.fusionshieldchlorophyte").setup(600, 100).outputItems(new ItemStack(ModItems.fusion_shield_chlorophyte, 1))
                .inputItems(new OreDictStack(W.block(), 16), new ComparableStack(ModBlocks.block_dura_steel, 16), new OreDictStack(getReflector(), 48), new ComparableStack(ModItems.powder_chlorophyte, 48)));
        this.register(new GenericRecipe("ass.fusionshieldvaporwave").setup(4800, 100).outputItems(new ItemStack(ModItems.fusion_shield_vaporwave, 1))
                .inputItems(new OreDictStack(DNT.block(), 16), new ComparableStack(ModBlocks.block_euphemium_cluster, 16), new OreDictStack(SBD.dust(), 64), new ComparableStack(ModItems.powder_spark_mix, 48), new ComparableStack(ModItems.powder_daffergon, 48)));
        this.register(new GenericRecipe("ass.structitercore").setup(600, 100).outputItems(new ItemStack(ModBlocks.struct_iter_core, 1))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.plateWelded(), 6), new OreDictStack(W.plateWelded(), 6), new OreDictStack(getReflector(), 12), new ComparableStack(ModItems.coil_advanced_alloy, 12), new OreDictStack(ANY_PLASTIC.ingot(), 8), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CONTROLLER_ADVANCED)));
        this.register(new GenericRecipe("ass.pelletchlorophyte").setup(50, 100).outputItems(new ItemStack(ModItems.pellet_chlorophyte, 2))
                .inputItems(new ComparableStack(ModItems.powder_chlorophyte, 1), new OreDictStack(PB.nugget(), 12)));
        this.register(new GenericRecipe("ass.fusionheater").setup(200, 100).outputItems(new ItemStack(ModBlocks.fusion_heater, 4))
                .inputItems(new OreDictStack(W.plateWelded(), 2), new OreDictStack(STEEL.plateWelded(), 2), new OreDictStack(getReflector(), 2), new ComparableStack(ModItems.magnetron, 2)));
        this.register(new GenericRecipe("ass.fusionconductor").setup(150, 100).outputItems(new ItemStack(ModBlocks.fusion_conductor, 3))
                .inputItems(new OreDictStack(ND.ingot(), 1), new ComparableStack(ModBlocks.deco_pipe, 1), new ComparableStack(ModItems.coil_advanced_alloy, 10)));
        this.register(new GenericRecipe("ass.fusioncenter").setup(200, 100).outputItems(new ItemStack(ModBlocks.fusion_center, 1))
                .inputItems(new OreDictStack(ANY_HARDPLASTIC.ingot(), 4), new OreDictStack(STEEL.plate(), 6), new OreDictStack(ALLOY.wireFine(), 24)));
        this.register(new GenericRecipe("ass.fusionmotor").setup(250, 100).outputItems(new ItemStack(ModBlocks.fusion_motor, 1))
                .inputItems(new OreDictStack(TI.ingot(), 4), new OreDictStack(STEEL.ingot(), 2), new ComparableStack(ModItems.motor, 4)));
        this.register(new GenericRecipe("ass.redcoil").setup(200, 100).outputItems(new ItemStack(ModItems.redcoil_capacitor, 1))
                .inputItems(new OreDictStack(GOLD.plate(), 3), new ComparableStack(ModItems.fuse, 1), new OreDictStack(ALLOY.wireFine(), 4), new ComparableStack(ModItems.coil_advanced_alloy, 6), new ComparableStack(Blocks.REDSTONE_BLOCK, 2)));
        this.register(new GenericRecipe("ass.euphemiumcapacitor").setup(600, 100).outputItems(new ItemStack(ModItems.euphemium_capacitor, 1))
                .inputItems(new OreDictStack(NB.ingot(), 4), new ComparableStack(ModItems.redcoil_capacitor, 1), new ComparableStack(ModItems.ingot_euphemium, 4), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.CAPACITOR_BOARD), new ComparableStack(ModItems.powder_nitan_mix, 18)));


        this.register(new GenericRecipe("ass.fusionklystron").setup(300, 100).outputItems(new ItemStack(ModBlocks.fusion_klystron, 1))
                .inputItems(new OreDictStack(W.plateWelded(), 4), new OreDictStack(ANY_RESISTANTALLOY.plateCast(), 16), new OreDictStack(CU.plate(), 32), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new OreDictStack(BSCCO.wireDense(), 8), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.BISMOID))
                .inputItemsEx(new OreDictStack(BSCCO.wireDense(), 32), new ComparableStack(ModItems.item_expensive, 16, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.PLASTIC), new ComparableStack(ModItems.item_expensive, 1, EnumExpensiveType.COMPUTER)));
        this.register(new GenericRecipe("ass.fusioncollector").setup(300, 100).outputItems(new ItemStack(ModBlocks.fusion_collector, 1))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.plateCast(), 4), new OreDictStack(STEEL.plate(), 16), new OreDictStack(GRAPHITE.ingot(), 16), new OreDictStack(ANY_HARDPLASTIC.ingot(), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.FERRO_PLATING), new OreDictStack(GRAPHITE.ingot(), 64), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.PLASTIC)));
        this.register(new GenericRecipe("ass.fusionbreeder").setup(300, 100).outputItems(new ItemStack(ModBlocks.fusion_breeder, 1))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.plateCast(), 4), new OreDictStack(STEEL.pipe(), 4), new OreDictStack(B.ingot(), 16), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.FERRO_PLATING), new OreDictStack(B.ingot(), 64), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.PLASTIC)));
        this.register(new GenericRecipe("ass.fusionboiler").setup(300, 100).outputItems(new ItemStack(ModBlocks.fusion_boiler, 1))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.plateCast(), 16), new OreDictStack(CU.shell(), 16), new OreDictStack(STEEL.pipe(), 8), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModItems.item_expensive, 8, EnumExpensiveType.HEAVY_FRAME), new ComparableStack(ModItems.item_expensive, 16, EnumExpensiveType.PLASTIC)));
        this.register(new GenericRecipe("ass.fusionmhdt").setup(1_200, 100).outputItems(new ItemStack(ModBlocks.fusion_mhdt, 1))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.plateWelded(), 16), new OreDictStack(CU.plateWelded(), 64), new OreDictStack(ANY_BISMOIDBRONZE.plateCast(), 16), new OreDictStack(SBD.wireDense(), 64), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.QUANTUM))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 16, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModItems.item_expensive, 16, EnumExpensiveType.PLASTIC), new OreDictStack(ANY_BISMOIDBRONZE.plateCast(), 32), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.QUANTUM))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));
        this.register(new GenericRecipe("ass.fusioncoupler").setup(300, 100).outputItems(new ItemStack(ModBlocks.fusion_coupler, 1))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.plateWelded(), 4), new OreDictStack(CU.plate(), 32), new OreDictStack(BSCCO.wireDense(), 16), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BISMOID))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.FERRO_PLATING), new OreDictStack(BSCCO.wireDense(), 16), new ComparableStack(ModItems.item_expensive, 2, EnumExpensiveType.COMPUTER)));
        this.register(new GenericRecipe("ass.fusionplasmaforge").setup(1_200, 100).outputItems(new ItemStack(ModBlocks.fusion_plasma_forge, 1))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.plateWelded(), 8), new OreDictStack(BSCCO.wireDense(), 32), new OreDictStack(ANY_BISMOIDBRONZE.plateCast(), 16), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.BISMOID))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 16, EnumExpensiveType.FERRO_PLATING), new ComparableStack(ModItems.item_expensive, 16, EnumExpensiveType.PLASTIC), new OreDictStack(BSCCO.wireDense(), 32), new ComparableStack(ModItems.item_expensive, 4, EnumExpensiveType.COMPUTER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "chlorophyte"));

        // watz
        this.register(new GenericRecipe("ass.watzrod").setup(200, 100).outputItems(new ItemStack(ModBlocks.watz_element, 3))
                .inputItems(new OreDictStack(STEEL.plateCast(), 2), new OreDictStack(ZR.ingot(), 2), new OreDictStack(BIGMT.ingot(), 2), new OreDictStack(ANY_HARDPLASTIC.ingot(), 4))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.FERRO_PLATING), new OreDictStack(BIGMT.ingot(), 6), new OreDictStack(ANY_HARDPLASTIC.ingot(), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.watzcooler").setup(200, 100).outputItems(new ItemStack(ModBlocks.watz_cooler, 3))
                .inputItems(new OreDictStack(STEEL.plateCast(), 2), new OreDictStack(CU.plateCast(), 4), new OreDictStack(RUBBER.ingot(), 2))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 3, EnumExpensiveType.FERRO_PLATING), new OreDictStack(RUBBER.ingot(), 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));
        this.register(new GenericRecipe("ass.watzcasing").setup(100, 100).outputItems(new ItemStack(ModBlocks.watz_casing, 3))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.plateWelded()), new OreDictStack(B.ingot(), 3), new OreDictStack(STEEL.plateWelded(), 2))
                .inputItemsEx(new ComparableStack(ModItems.item_expensive, 6, EnumExpensiveType.LEAD_PLATING), new OreDictStack(ANY_RESISTANTALLOY.plateWelded()))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy"));

        // fwatz
        this.register(new GenericRecipe("ass.fwatz_hatch").setup(250, 100).outputItems(new ItemStack(ModBlocks.fwatz_hatch, 1))
                .inputItems(new ComparableStack(ModBlocks.fwatz_scaffold, 4), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CHIP_BISMOID), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CHIP), new ComparableStack(ModItems.wire, 16, Mats.MAT_MINGRADE.id), new OreDictStack(CMB.plate(), 16)));
        this.register(new GenericRecipe("ass.fwatz_computer").setup(300, 100).outputItems(new ItemStack(ModBlocks.fwatz_computer, 1))
                .inputItems(new ComparableStack(ModBlocks.block_meteor, 1), new ComparableStack(ModItems.wire, 16, Mats.MAT_MAGTUNG.id), new OreDictStack(SBD.dust(), 4), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.BISMOID)));
        this.register(new GenericRecipe("ass.fwatz_core").setup(450, 100).outputItems(new ItemStack(ModBlocks.fwatz_core, 1))
                .inputItems(new ComparableStack(ModBlocks.block_meteor, 1), new ComparableStack(ModItems.wire, 24, Mats.MAT_MAGTUNG.id), new OreDictStack(DIAMOND.dust(), 8), new OreDictStack(MAGTUNG.dust(), 12), new OreDictStack(DESH.heavyComp()), new ComparableStack(ModItems.upgrade_screm, 1), new ComparableStack(ModItems.upgrade_overdrive_3, 1), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.QUANTUM)));
        this.register(new GenericRecipe("ass.ams_emitter").setup(6000, 100_000).outputItems(new ItemStack(ModBlocks.ams_emitter, 1))
                .inputItems(new ComparableStack(ModItems.component_emitter, 16), new OreDictStack(W.heavyComp(), 1), new OreDictStack(TI.heavyComp(), 2), new ComparableStack(ModItems.plate_dineutronium, 32), new ComparableStack(ModItems.circuit, 12, EnumCircuitType.CONTROLLER_ADVANCED), new ComparableStack(ModItems.coil_advanced_torus, 24), new ComparableStack(ModItems.entanglement_kit, 3), new ComparableStack(ModItems.crystal_horn, 1), new ComparableStack(ModBlocks.fwatz_core, 1)));
        this.register(new GenericRecipe("ass.uugigafactory").setup(900, 100).outputItems(new ItemStack(ModBlocks.uu_gigafactory, 1))
                .inputItems(new ComparableStack(ModBlocks.machine_orbus, 1), new ComparableStack(ModBlocks.machine_saturnite_battery, 2), new ComparableStack(ModBlocks.hadron_analysis, 16), new ComparableStack(ModItems.laser_crystal_cmb, 1), new ComparableStack(ModItems.powder_nitan_mix, 8), new OreDictStack(AL.heavyComp(), 1), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER_QUANTUM)));

        // upgrades
        if(no528) {
            this.register(new GenericRecipe("ass.overdrive1").setup(200, 100).outputItems(new ItemStack(ModItems.upgrade_overdrive_1, 1))
                    .inputItems(new ComparableStack(ModItems.upgrade_speed_3, 1), new ComparableStack(ModItems.upgrade_effect_3, 1), new OreDictStack(BIGMT.ingot(), 16), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.ADVANCED)));
            this.register(new GenericRecipe("ass.overdrive2").setup(600, 100).outputItems(new ItemStack(ModItems.upgrade_overdrive_2, 1))
                    .inputItems(new ComparableStack(ModItems.upgrade_overdrive_1, 1), new ComparableStack(ModItems.upgrade_speed_3, 1), new ComparableStack(ModItems.upgrade_effect_3, 1), new OreDictStack(BIGMT.ingot(), 16), new ComparableStack(ModItems.ingot_cft, 8), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.CAPACITOR_BOARD)));
            this.register(new GenericRecipe("ass.overdrive3").setup(1_200, 100).outputItems(new ItemStack(ModItems.upgrade_overdrive_3, 1))
                    .inputItems(new ComparableStack(ModItems.upgrade_overdrive_2, 1), new ComparableStack(ModItems.upgrade_speed_3, 1), new ComparableStack(ModItems.upgrade_effect_3, 1), new OreDictStack(ANY_BISMOIDBRONZE.ingot(), 16), new ComparableStack(ModItems.ingot_cft, 16), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.BISMOID)));
        }

        // rancid shit mob spawners
        this.register(new GenericRecipe("ass.chopper").setup(1_200, 100).outputItems(new ItemStack(ModItems.spawn_chopper, 1))
                .inputItems(new RecipesCommon.OreDictStack(CMB.plateCast(), 24), new RecipesCommon.OreDictStack(STEEL.plate(), 32), new RecipesCommon.OreDictStack(MAGTUNG.wireFine(), 48), new RecipesCommon.ComparableStack(ModItems.motor_desh, 5), new RecipesCommon.ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER_ADVANCED)));
        this.register(new GenericRecipe("ass.ballsotron").setup(1_200, 100).outputItems(new ItemStack(ModItems.spawn_worm, 1))
                .inputItems(new RecipesCommon.OreDictStack(TI.plateWelded(), 32), new RecipesCommon.OreDictStack(RUBBER.ingot(), 64), new RecipesCommon.ComparableStack(ModItems.motor, 64), new RecipesCommon.OreDictStack(GOLD.wireDense(), 64), new RecipesCommon.OreDictStack(U238.block(), 10), new RecipesCommon.ComparableStack(ModItems.mech_key, 1)));

        // weapon parts
        this.register(new GenericRecipe("ass.clusterpellets").setup(50, 100).outputItems(new ItemStack(ModItems.pellet_cluster, 1))
                .inputItems(new RecipesCommon.OreDictStack(STEEL.plate(), 4), new RecipesCommon.OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 1)));
        this.register(new GenericRecipe("ass.buckshot").setup(50, 100).outputItems(new ItemStack(ModItems.pellet_buckshot, 1))
                .inputItems(new RecipesCommon.OreDictStack(PB.nugget(), 6)));

        // bombs
        this.register(new GenericRecipe("ass.minenaval").setup(300, 100).outputItems(new ItemStack(ModBlocks.mine_naval, 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel, 1), new OreDictStack(STEEL.pipe(), 3), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED), new OreDictStack(ANY_PLASTICEXPLOSIVE.ingot(), 24)));
        this.register(new GenericRecipe("ass.gadget").setup(300, 100).outputItems(new ItemStack(ModBlocks.nuke_gadget, 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel, 1), new ComparableStack(ModItems.fins_flat, 2), new ComparableStack(ModItems.pedestal_steel, 1), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.CONTROLLER), new OreDictStack(KEY_GRAY, 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.littleboy").setup(300, 100).outputItems(new ItemStack(ModBlocks.nuke_boy, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 2), new ComparableStack(ModItems.fins_small_steel, 1), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CONTROLLER), new OreDictStack(KEY_BLUE, 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.fatman").setup(300, 100).outputItems(new ItemStack(ModBlocks.nuke_man, 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel, 1), new OreDictStack(STEEL.shell(), 2), new ComparableStack(ModItems.fins_big_steel, 1), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.CONTROLLER), new OreDictStack(KEY_YELLOW, 6))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.ivymike").setup(600, 100).outputItems(new ItemStack(ModBlocks.nuke_mike, 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel, 1), new OreDictStack(AL.shell(), 4), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.CONTROLLER_ADVANCED), new OreDictStack(KEY_LIGHTGRAY, 16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.tsarbomba").setup(1_200, 100).outputItems(new ItemStack(ModBlocks.nuke_tsar, 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel, 1), new OreDictStack(TI.shell(), 6), new OreDictStack(STEEL.shell(), 2), new ComparableStack(ModItems.fins_tri_steel, 1), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.CONTROLLER_ADVANCED), new OreDictStack(KEY_BLACK, 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.ninadidnothingwrong").setup(300, 100).outputItems(new ItemStack(ModBlocks.nuke_prototype, 1))
                .inputItems(new ComparableStack(ModItems.dysfunctional_reactor, 1), new OreDictStack(STEEL.shell(), 2), new ComparableStack(ModItems.ingot_euphemium, 3), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.CONTROLLER_ADVANCED))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.fleija").setup(300, 100).outputItems(new ItemStack(ModBlocks.nuke_fleija, 1))
                .inputItems(new OreDictStack(AL.shell(), 1), new ComparableStack(ModItems.fins_quad_titanium, 1), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER), new OreDictStack(KEY_WHITE, 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.solinium").setup(300, 100).outputItems(new ItemStack(ModBlocks.nuke_solinium, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 2), new ComparableStack(ModItems.fins_quad_titanium, 1), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER), new OreDictStack(KEY_GRAY, 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.n2mine").setup(200, 100).outputItems(new ItemStack(ModBlocks.nuke_n2, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 6), new OreDictStack(MAGTUNG.wireFine(), 12), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CONTROLLER), new OreDictStack(KEY_GREEN, 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.balefirebomb").setup(400, 100).outputItems(new ItemStack(ModBlocks.nuke_fstbmb, 1))
                .inputItems(new ComparableStack(ModItems.sphere_steel, 1), new OreDictStack(TI.shell(), 6), new ComparableStack(ModItems.fins_big_steel, 1), new ComparableStack(ModItems.powder_magic, 8), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CONTROLLER_ADVANCED), new OreDictStack(KEY_GRAY, 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.customnuke").setup(300, 100).outputItems(new ItemStack(ModBlocks.nuke_custom, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 2), new ComparableStack(ModItems.fins_small_steel, 1), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.CONTROLLER_ADVANCED), new OreDictStack(KEY_GRAY, 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.levibomb").setup(200, 100).outputItems(new ItemStack(ModBlocks.float_bomb, 1))
                .inputItems(new OreDictStack(TI.plate(), 12), new OreDictStack(SA326.nugget(), 3), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.ADVANCED), new OreDictStack(GOLD.wireDense(), 8)));
        this.register(new GenericRecipe("ass.endobomb").setup(200, 100).outputItems(new ItemStack(ModBlocks.therm_endo, 1))
                .inputItems(new OreDictStack(TI.plate(), 12), new ComparableStack(ModItems.powder_ice, 32), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED), new ComparableStack(ModItems.coil_gold, 4)));
        this.register(new GenericRecipe("ass.exobomb").setup(200, 100).outputItems(new ItemStack(ModBlocks.therm_exo, 1))
                .inputItems(new OreDictStack(TI.plate(), 12), new OreDictStack(P_RED.dust(), 32), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED), new ComparableStack(ModItems.coil_gold, 4)));

        // bomb parts
        this.register(new GenericRecipe("ass.gadget_explosive_single").setup(200, 100).outputItems(new ItemStack(ModItems.gadget_explosive, 4))
                .inputItems(new OreDictStack(AL.plate(), 4), new OreDictStack(GOLD.wireFine(), 8), new ComparableStack(ModBlocks.det_cord, 4), new OreDictStack(CU.plate(), 1), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 10), new OreDictStack(ANY_PLASTIC.ingot(), 2)));
        this.register(new GenericRecipe("ass.man_explosive_single").setup(250, 100).outputItems(new ItemStack(ModItems.man_explosive, 4))
                .inputItems(new OreDictStack(AL.plate(), 4), new ComparableStack(ModItems.wire, 8, Mats.MAT_MINGRADE.id), new OreDictStack(ANY_PLASTICEXPLOSIVE.ingot(), 2), new OreDictStack(CU.plate(), 1), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 8), new OreDictStack(RUBBER.ingot(), 1)));
        this.register(new GenericRecipe("ass.explosivelenses1").setup(400, 100).outputItems(new ItemStack(ModItems.early_explosive_lenses, 1))
                .inputItems(new OreDictStack(AL.plate(), 8), new ComparableStack(ModBlocks.det_cord, 8), new OreDictStack(BIGMT.plate(), 2), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 20), new OreDictStack(ANY_PLASTIC.ingot(), 4)));
        this.register(new GenericRecipe("ass.explosivelenses2").setup(400, 100).outputItems(new ItemStack(ModItems.explosive_lenses, 1))
                .inputItems(new OreDictStack(AL.plate(), 8), new OreDictStack(ANY_PLASTICEXPLOSIVE.ingot(), 4), new ComparableStack(ModItems.neutron_reflector, 2), new ComparableStack(ModItems.ball_tatb, 16), new OreDictStack(RUBBER.ingot(), 2)));
        this.register(new GenericRecipe("ass.wiring").setup(200, 100).outputItems(new ItemStack(ModItems.gadget_wireing, 1))
                .inputItems(new OreDictStack(GOLD.wireFine(), 24)));
        this.register(new GenericRecipe("ass.core1").setup(1_200, 100).outputItems(new ItemStack(ModItems.gadget_core, 1))
                .inputItems(new OreDictStack(PU239.nugget(), 7), new OreDictStack(U238.nugget(), 3)));
        this.register(new GenericRecipe("ass.boyshield").setup(200, 100).outputItems(new ItemStack(ModItems.boy_shielding, 1))
                .inputItems(new OreDictStack(OreDictManager.getReflector(), 12), new OreDictStack(STEEL.plate(), 4)));
        this.register(new GenericRecipe("ass.boytarget").setup(200, 100).outputItems(new ItemStack(ModItems.boy_target, 1))
                .inputItems(new OreDictStack(U235.nugget(), 18)));
        this.register(new GenericRecipe("ass.boybullet").setup(200, 100).outputItems(new ItemStack(ModItems.boy_bullet, 1))
                .inputItems(new OreDictStack(U235.nugget(), 9)));
        this.register(new GenericRecipe("ass.boypropellant").setup(200, 100).outputItems(new ItemStack(ModItems.boy_propellant, 1))
                .inputItems(new ComparableStack(ModItems.cordite, 8), new OreDictStack(IRON.plate(), 8), new OreDictStack(AL.plate(), 4), new OreDictStack(MINGRADE.wireFine(), 4)));
        this.register(new GenericRecipe("ass.boyigniter").setup(200, 100).outputItems(new ItemStack(ModItems.boy_igniter, 1))
                .inputItems(new OreDictStack(AL.shell(), 3), new OreDictStack(DURA.plateCast(), 1), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED.ordinal()), new OreDictStack(MINGRADE.wireFine(), 16)));
        this.register(new GenericRecipe("ass.manigniter").setup(200, 100).outputItems(new ItemStack(ModItems.man_igniter, 1))
                .inputItems(new ComparableStack(ModItems.circuit, 3, EnumCircuitType.ADVANCED.ordinal()), new OreDictStack(GOLD.wireFine(), 24)));
        this.register(new GenericRecipe("ass.mancore").setup(1_200, 100).outputItems(new ItemStack(ModItems.man_core, 1))
                .inputItems(new OreDictStack(PU239.nugget(), 8), new OreDictStack(BE.nugget(), 2)));
        this.register(new GenericRecipe("ass.mikecore").setup(1_200, 100).outputItems(new ItemStack(ModItems.mike_core, 1))
                .inputItems(new OreDictStack(U238.nugget(), 24), new OreDictStack(PB.plate(), 6)));
        this.register(new GenericRecipe("ass.mikedeut").setup(600, 100).outputItems(new ItemStack(ModItems.mike_deut, 1))
                .inputItems(new OreDictStack(WEAPONSTEEL.plate(), 16), new OreDictStack(TI.plate(), 16))
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 10_000)));
        // Tsar Core recipe, proudly added by movblock
        this.register(new GenericRecipe("ass.tsarcore").setup(1_200, 100).outputItems(new ItemStack(ModItems.tsar_core, 1))
                .inputItems(new ComparableStack(ModItems.mike_deut, 1), new OreDictStack(LI.ingot(), 9), new ComparableStack(ModItems.cell, 1, Fluids.TRITIUM.getID()), new OreDictStack(PU239.nugget(), 1), new OreDictStack(BE.nugget(), 1), new OreDictStack(PB.plate(), 12), new OreDictStack(TI.shell(), 2)));
        this.register(new GenericRecipe("ass.tsarcore1").setup(1_200, 100).outputItems(new ItemStack(ModItems.tsar_core, 1))
                .inputItems(new ComparableStack(ModItems.tank_steel, 1), new ComparableStack(ModItems.fluid_pack_full, 42, Fluids.DEUTERIUM.getID()), new ComparableStack(ModItems.fluid_pack_full, 40, Fluids.TRITIUM.getID()), new OreDictStack(PU239.nugget(), 1), new OreDictStack(BE.nugget(), 1), new OreDictStack(PB.plate(), 12), new OreDictStack(TI.shell(), 2)));
        this.register(new GenericRecipe("ass.mikecooler").setup(300, 100).outputItems(new ItemStack(ModItems.mike_cooling_unit, 1))
                .inputItems(new OreDictStack(DURA.plate(), 8), new ComparableStack(ModItems.coil_copper, 5), new ComparableStack(ModItems.coil_tungsten, 5), new ComparableStack(ModItems.motor, 2)));
        this.register(new GenericRecipe("ass.fleijaigniter").setup(200, 100).outputItems(new ItemStack(ModItems.fleija_igniter, 1))
                .inputItems(new OreDictStack(TI.plate(), 6), new OreDictStack(SA326.wireFine(), 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED.ordinal())));
        this.register(new GenericRecipe("ass.fleijacore").setup(600, 100).outputItems(new ItemStack(ModItems.fleija_core, 1))
                .inputItems(new OreDictStack(U235.nugget(), 8), new OreDictStack(NP237.nugget(), 2), new OreDictStack(BE.nugget(), 4), new ComparableStack(ModItems.coil_copper, 2)));
        this.register(new GenericRecipe("ass.fleijacharge").setup(300, 100).outputItems(new ItemStack(ModItems.fleija_propellant, 1))
                .inputItems(new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 3), new OreDictStack(SA326.plate(), 8)));
        this.register(new GenericRecipe("ass.soliniumigniter").setup(200, 100).outputItems(new ItemStack(ModItems.solinium_igniter, 1))
                .inputItems(new OreDictStack(TI.plate(), 4), new OreDictStack(MINGRADE.wireFine(), 8), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED.ordinal()), new ComparableStack(ModItems.coil_gold, 1)));
        this.register(new GenericRecipe("ass.soliniumcore").setup(600, 100).outputItems(new ItemStack(ModItems.solinium_core, 1))
                .inputItems(new OreDictStack(SA327.nugget(), 9), new OreDictStack(EUPH.nugget(), 1)));
        this.register(new GenericRecipe("ass.soliniumcharge").setup(300, 100).outputItems(new ItemStack(ModItems.solinium_propellant, 1))
                .inputItems(new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 3), new OreDictStack(OreDictManager.getReflector(), 2), new ComparableStack(ModItems.plate_polymer, 6), new OreDictStack(W.wireFine(), 6), new ComparableStack(ModItems.biomass_compressed, 4)));

        // grenades (EE assembler parity)
        this.register(new GenericRecipe("ass.grenadefire").setup(150, 100).outputItems(new ItemStack(ModItems.grenade_fire, 1))
                .inputItems(new ComparableStack(ModItems.grenade_frag, 1), new OreDictStack(P_RED.dust(), 1), new OreDictStack(CU.plate(), 2)));
        this.register(new GenericRecipe("ass.grenadeshrapnel").setup(150, 100).outputItems(new ItemStack(ModItems.grenade_shrapnel, 1))
                .inputItems(new ComparableStack(ModItems.grenade_frag, 1), new ComparableStack(ModItems.pellet_buckshot, 1), new OreDictStack(STEEL.plate(), 2)));
        this.register(new GenericRecipe("ass.grenadecluster").setup(200, 100).outputItems(new ItemStack(ModItems.grenade_cluster, 1))
                .inputItems(new ComparableStack(ModItems.grenade_frag, 1), new ComparableStack(ModItems.pellet_cluster, 1), new OreDictStack(STEEL.plate(), 2)));
        this.register(new GenericRecipe("ass.grenadeflare").setup(100, 100).outputItems(new ItemStack(ModItems.grenade_flare, 1))
                .inputItems(new ComparableStack(ModItems.grenade_generic, 1), new ComparableStack(Items.GLOWSTONE_DUST, 1), new OreDictStack(AL.plate(), 2), new OreDictStack(CS.ingot(), 1)));
        this.register(new GenericRecipe("ass.grenadeelectric").setup(200, 100).outputItems(new ItemStack(ModItems.grenade_electric, 1))
                .inputItems(new ComparableStack(ModItems.grenade_generic, 1), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.CAPACITOR.ordinal()), new OreDictStack(GOLD.plate(), 2)));
        this.register(new GenericRecipe("ass.grenadepulse").setup(300, 100).outputItems(new ItemStack(ModItems.grenade_pulse, 4))
                .inputItems(new OreDictStack(STEEL.plate(), 1), new OreDictStack(IRON.plate(), 3), new OreDictStack(MINGRADE.wireFine(), 6), new ComparableStack(Items.DIAMOND, 1)));
        this.register(new GenericRecipe("ass.grenadeplasma").setup(300, 100).outputItems(new ItemStack(ModItems.grenade_plasma, 2))
                .inputItems(new OreDictStack(STEEL.plate(), 3), new OreDictStack(ALLOY.plate(), 1), new ComparableStack(ModItems.coil_advanced_torus, 1), new ComparableStack(ModItems.cell, 1, Fluids.DEUTERIUM.getID()), new ComparableStack(ModItems.cell, 1, Fluids.TRITIUM.getID())));
        this.register(new GenericRecipe("ass.grenadetau").setup(300, 100).outputItems(new ItemStack(ModItems.grenade_tau, 2))
                .inputItems(new OreDictStack(PB.plate(), 3), new OreDictStack(ALLOY.plate(), 1), new ComparableStack(ModItems.coil_advanced_torus, 1), new ComparableStack(ModItems.gun_xvl1456_ammo, 1)));
        this.register(new GenericRecipe("ass.grenadeschrabidium").setup(300, 100).outputItems(new ItemStack(ModItems.grenade_schrabidium, 1))
                .inputItems(new ComparableStack(ModItems.grenade_flare, 1), new OreDictStack(SA326.dust(), 1), new OreDictStack(OreDictManager.getReflector(), 2)));
        this.register(new GenericRecipe("ass.grenadenuclear").setup(200, 100).outputItems(new ItemStack(ModItems.grenade_nuclear, 1))
                .inputItems(new OreDictStack(IRON.plate(), 1), new OreDictStack(STEEL.plate(), 1), new OreDictStack(PU239.nugget(), 2), new OreDictStack(MINGRADE.wireFine(), 2)));
        this.register(new GenericRecipe("ass.grenadezomg").setup(300, 100).outputItems(new ItemStack(ModItems.grenade_zomg, 1))
                .inputItems(new ComparableStack(ModItems.plate_paa, 3), new OreDictStack(OreDictManager.getReflector(), 1), new ComparableStack(ModItems.coil_magnetized_tungsten, 3), new ComparableStack(ModItems.powder_power, 3), new ComparableStack(ModItems.nugget_radspice, 1)));
        this.register(new GenericRecipe("ass.grenadeblackhole").setup(500, 100).outputItems(new ItemStack(ModItems.grenade_black_hole, 1))
                .inputItems(new OreDictStack(ANY_PLASTIC.ingot(), 6), new OreDictStack(OreDictManager.getReflector(), 3), new ComparableStack(ModItems.coil_magnetized_tungsten, 2), new ComparableStack(ModItems.black_hole, 1)));

        // turrets
        this.register(new GenericRecipe("ass.turretchekhov").setup(200, 100).outputItems(new ItemStack(ModBlocks.turret_chekhov, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD), new OreDictStack(STEEL.ingot(), 16), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED), new OreDictStack(STEEL.pipe(), 3), new OreDictStack(GUNMETAL.mechanism(), 3), new ComparableStack(ModBlocks.crate_iron, 1), new ComparableStack(ModItems.crt_display, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turretfriendly").setup(200, 100).outputItems(new ItemStack(ModBlocks.turret_friendly, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD), new OreDictStack(STEEL.ingot(), 16), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC), new OreDictStack(STEEL.pipe(), 3), new OreDictStack(GUNMETAL.mechanism(), 1), new ComparableStack(ModBlocks.crate_iron, 1), new ComparableStack(ModItems.crt_display, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turretjeremy").setup(200, 100).outputItems(new ItemStack(ModBlocks.turret_jeremy, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD), new OreDictStack(STEEL.ingot(), 16), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED), new ComparableStack(ModItems.motor_desh, 1), new OreDictStack(STEEL.shell(), 3), new OreDictStack(WEAPONSTEEL.mechanism(), 3), new ComparableStack(ModBlocks.crate_steel, 1), new ComparableStack(ModItems.crt_display, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turrettauon").setup(200, 100).outputItems(new ItemStack(ModBlocks.turret_tauon, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.CAPACITOR_NIOBIUM), new OreDictStack(STEEL.ingot(), 16), new OreDictStack(ANY_PLASTIC.ingot(), 4), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED), new ComparableStack(ModItems.motor_desh, 1), new OreDictStack(CU.ingot(), 32), new OreDictStack(BIGMT.mechanism(), 3), new ComparableStack(ModItems.crt_display, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turretrichard").setup(200, 100).outputItems(new ItemStack(ModBlocks.turret_richard, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD), new OreDictStack(STEEL.ingot(), 16), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED), new OreDictStack(ANY_PLASTIC.ingot(), 2), new OreDictStack(STEEL.shell(), 8), new OreDictStack(WEAPONSTEEL.mechanism(), 3), new ComparableStack(ModBlocks.crate_steel, 1), new ComparableStack(ModItems.crt_display, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turrethoward").setup(200, 100).outputItems(new ItemStack(ModBlocks.turret_howard, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD), new OreDictStack(STEEL.ingot(), 24), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.motor_desh, 2), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.ADVANCED), new OreDictStack(STEEL.pipe(), 10), new OreDictStack(WEAPONSTEEL.mechanism(), 3), new ComparableStack(ModBlocks.crate_steel, 1), new ComparableStack(ModItems.crt_display, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.maxwell").setup(200, 100).outputItems(new ItemStack(ModBlocks.turret_maxwell, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.CAPACITOR_NIOBIUM), new OreDictStack(STEEL.ingot(), 24), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.ADVANCED), new OreDictStack(STEEL.pipe(), 4), new OreDictStack(BIGMT.mechanism(), 3), new ComparableStack(ModItems.magnetron, 16), new OreDictStack(ANY_RESISTANTALLOY.ingot(), 8), new ComparableStack(ModItems.crt_display, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.fritz").setup(200, 100).outputItems(new ItemStack(ModBlocks.turret_fritz, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD), new OreDictStack(STEEL.ingot(), 16), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED), new OreDictStack(STEEL.pipe(), 8), new OreDictStack(GUNMETAL.mechanism(), 3), new ComparableStack(ModBlocks.barrel_steel))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "bmg"));
        this.register(new GenericRecipe("ass.turretheavy").setup(250, 100).outputItems(new ItemStack(ModBlocks.turret_heavy, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 8), new OreDictStack(AL.ingot(), 4), new ComparableStack(ModItems.pipes_steel, 2), new ComparableStack(ModItems.hull_small_steel, 1), new OreDictStack(MINGRADE.ingot(), 4), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.turretrocket").setup(300, 100).outputItems(new ItemStack(ModBlocks.turret_rocket, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 12), new OreDictStack(TI.ingot(), 4), new ComparableStack(ModItems.hull_small_steel, 8), new OreDictStack(MINGRADE.ingot(), 6), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.ANALOG)));
        this.register(new GenericRecipe("ass.turretflamer").setup(250, 100).outputItems(new ItemStack(ModBlocks.turret_flamer, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 8), new OreDictStack(W.ingot(), 2), new ComparableStack(ModItems.pipes_steel, 1), new ComparableStack(ModItems.tank_steel, 2), new OreDictStack(MINGRADE.ingot(), 4), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.ANALOG)));
        this.register(new GenericRecipe("ass.turrettau").setup(350, 100).outputItems(new ItemStack(ModBlocks.turret_tau, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 16), new OreDictStack(TI.ingot(), 8), new OreDictStack(ALLOY.plate(), 4), new ComparableStack(ModItems.redcoil_capacitor, 3), new OreDictStack(MINGRADE.ingot(), 12), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.ADVANCED)));
        this.register(new GenericRecipe("ass.turretspitfire").setup(350, 100).outputItems(new ItemStack(ModBlocks.turret_spitfire, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 6), new OreDictStack(MINGRADE.ingot(), 6), new OreDictStack(STEEL.plate(), 16), new OreDictStack(IRON.plate(), 8), new ComparableStack(ModItems.hull_small_steel, 4), new ComparableStack(ModItems.pipes_steel, 2), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.turretlight").setup(200, 100).outputItems(new ItemStack(ModBlocks.turret_light, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 6), new ComparableStack(ModItems.pipes_steel, 2), new OreDictStack(MINGRADE.ingot(), 2), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.turretcwis").setup(400, 100).outputItems(new ItemStack(ModBlocks.turret_cwis, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 6), new OreDictStack(MINGRADE.ingot(), 8), new OreDictStack(STEEL.plate(), 10), new OreDictStack(TI.plate(), 4), new ComparableStack(ModItems.hull_small_aluminium, 2), new ComparableStack(ModItems.pipes_steel, 6), new ComparableStack(ModItems.motor, 4), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.ADVANCED), new ComparableStack(ModItems.magnetron, 3)));
        this.register(new GenericRecipe("ass.turretcheapo").setup(200, 100).outputItems(new ItemStack(ModBlocks.turret_cheapo, 1))
                .inputItems(new OreDictStack(STEEL.ingot(), 4), new OreDictStack(IRON.plate(), 4), new ComparableStack(ModItems.pipes_steel, 3), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CHIP)));
        this.register(new GenericRecipe("ass.arty").setup(1_200, 100).outputItems(new ItemStack(ModBlocks.turret_arty, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM), new OreDictStack(STEEL.ingot(), 64), new OreDictStack(STEEL.ingot(), 64), new ComparableStack(ModItems.motor_desh, 5), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.ADVANCED), new OreDictStack(STEEL.pipe(), 12), new OreDictStack(WEAPONSTEEL.mechanism(), 16), new ComparableStack(ModBlocks.machine_radar, 1), new ComparableStack(ModItems.crt_display, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "arty"));
        this.register(new GenericRecipe("ass.himars").setup(1_200, 100).outputItems(new ItemStack(ModBlocks.turret_himars, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM), new OreDictStack(STEEL.ingot(), 64), new OreDictStack(STEEL.ingot(), 64), new OreDictStack(ANY_PLASTIC.ingot(), 64), new ComparableStack(ModItems.motor_desh, 5), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.ADVANCED), new OreDictStack(BIGMT.mechanism(), 8), new ComparableStack(ModBlocks.machine_radar, 1), new ComparableStack(ModItems.crt_display, 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "arty"));
        this.register(new GenericRecipe("ass.himarssmall").setup(100, 100).outputItems(new ItemStack(ModItems.ammo_himars, 1, ItemAmmoHIMARS.RocketType.SMALL.ordinal()))
                .inputItems(new OreDictStack(STEEL.plate(), 24), new OreDictStack(ANY_PLASTIC.ingot(), 12), new ComparableStack(ModItems.rocket_fuel, 48), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 48), new ComparableStack(ModItems.circuit, 6, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.himarssmallhe").setup(100, 100).outputItems(new ItemStack(ModItems.ammo_himars, 1, ItemAmmoHIMARS.RocketType.SMALL_HE.ordinal()))
                .inputItems(new OreDictStack(STEEL.plate(), 24), new OreDictStack(ANY_PLASTIC.ingot(), 24), new ComparableStack(ModItems.rocket_fuel, 48), new OreDictStack(ANY_PLASTICEXPLOSIVE.ingot(), 18), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 48), new ComparableStack(ModItems.circuit, 6, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.himarssmallwp").setup(100, 100).outputItems(new ItemStack(ModItems.ammo_himars, 1, ItemAmmoHIMARS.RocketType.SMALL_WP.ordinal()))
                .inputItems(new OreDictStack(STEEL.plate(), 24), new OreDictStack(ANY_PLASTIC.ingot(), 24), new ComparableStack(ModItems.rocket_fuel, 48), new OreDictStack(P_WHITE.ingot(), 18), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 48), new ComparableStack(ModItems.circuit, 6, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.himarssmalltb").setup(100, 100).outputItems(new ItemStack(ModItems.ammo_himars, 1, ItemAmmoHIMARS.RocketType.SMALL_TB.ordinal()))
                .inputItems(new OreDictStack(STEEL.plate(), 24), new OreDictStack(ANY_PLASTIC.ingot(), 24), new ComparableStack(ModItems.rocket_fuel, 48), new ComparableStack(ModItems.ball_tatb, 32), new OreDictStack(Fluids.KEROSENE_REFORM.getDict(1_000), 12), new OreDictStack(Fluids.PEROXIDE.getDict(1_000), 12), new ComparableStack(ModItems.circuit, 6, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.himarssmallnuke").setup(100, 100).outputItems(new ItemStack(ModItems.ammo_himars, 1, ItemAmmoHIMARS.RocketType.SMALL_MINI_NUKE.ordinal()))
                .inputItems(new OreDictStack(STEEL.plate(), 24), new OreDictStack(ANY_PLASTIC.ingot(), 24), new ComparableStack(ModItems.rocket_fuel, 48), new ComparableStack(ModItems.ball_tatb, 6), new OreDictStack(PU239.nugget(), 12), new OreDictStack(OreDictManager.getReflector(), 12), new ComparableStack(ModItems.circuit, 6, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.himarssmalllava").setup(100, 100).outputItems(new ItemStack(ModItems.ammo_himars, 1, ItemAmmoHIMARS.RocketType.SMALL_LAVA.ordinal()))
                .inputItems(new OreDictStack(STEEL.plate(), 24), new OreDictStack(ANY_HARDPLASTIC.ingot(), 12), new ComparableStack(ModItems.rocket_fuel, 32), new ComparableStack(ModItems.ball_tatb, 4), new OreDictStack(VOLCANIC.gem(), 1), new ComparableStack(ModItems.circuit, 6, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.himarslarge").setup(200, 100).outputItems(new ItemStack(ModItems.ammo_himars, 1, ItemAmmoHIMARS.RocketType.LARGE.ordinal()))
                .inputItems(new OreDictStack(STEEL.plate(), 24), new OreDictStack(ANY_HARDPLASTIC.ingot(), 12), new ComparableStack(ModItems.rocket_fuel, 36), new ComparableStack(ModItems.ball_tatb, 16), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED)));
        this.register(new GenericRecipe("ass.himarslargetb").setup(200, 100).outputItems(new ItemStack(ModItems.ammo_himars, 1, ItemAmmoHIMARS.RocketType.LARGE_TB.ordinal()))
                .inputItems(new OreDictStack(STEEL.plate(), 24), new OreDictStack(ANY_HARDPLASTIC.ingot(), 12), new ComparableStack(ModItems.rocket_fuel, 36), new ComparableStack(ModItems.ball_tatb, 24), new OreDictStack(Fluids.KEROSENE_REFORM.getDict(1_000), 16), new OreDictStack(Fluids.PEROXIDE.getDict(1_000), 16), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED)));

        // missile parts
        this.register(new GenericRecipe("ass.missileassembly").setup(200, 100).outputItems(new ItemStack(ModItems.missile_assembly, 1))
                .inputItems(new OreDictStack(AL.shell(), 2), new OreDictStack(TI.shell(), 4), new OreDictStack(ANY_PLASTIC.ingot(), 8), new ComparableStack(ModItems.rocket_fuel, 8), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.warheadhe1").setup(100, 100).outputItems(new ItemStack(ModItems.warhead_generic_small, 1))
                .inputItems(new OreDictStack(TI.plate(), 4), new ComparableStack(ModItems.ball_dynamite, 2), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CHIP)));
        this.register(new GenericRecipe("ass.warheadhe2").setup(200, 100).outputItems(new ItemStack(ModItems.warhead_generic_medium, 1))
                .inputItems(new OreDictStack(TI.plate(), 8), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 4), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.warheadhe3").setup(400, 100).outputItems(new ItemStack(ModItems.warhead_generic_large, 1))
                .inputItems(new OreDictStack(TI.plate(), 16), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 8), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED)));
        this.register(new GenericRecipe("ass.warheadinc1").setup(100, 100).outputItems(new ItemStack(ModItems.warhead_incendiary_small, 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_small, 1), new OreDictStack(P_RED.dust(), 2)));
        this.register(new GenericRecipe("ass.warheadinc2").setup(200, 100).outputItems(new ItemStack(ModItems.warhead_incendiary_medium, 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_medium, 1), new OreDictStack(P_RED.dust(), 4)));
        this.register(new GenericRecipe("ass.warheadinc3").setup(400, 100).outputItems(new ItemStack(ModItems.warhead_incendiary_large, 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_large, 1), new OreDictStack(P_RED.dust(), 8)));
        this.register(new GenericRecipe("ass.warheadcl1").setup(100, 100).outputItems(new ItemStack(ModItems.warhead_cluster_small, 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_small, 1), new ComparableStack(ModItems.pellet_cluster, 2)));
        this.register(new GenericRecipe("ass.warheadcl2").setup(200, 100).outputItems(new ItemStack(ModItems.warhead_cluster_medium, 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_medium, 1), new ComparableStack(ModItems.pellet_cluster, 4)));
        this.register(new GenericRecipe("ass.warheadcl3").setup(400, 100).outputItems(new ItemStack(ModItems.warhead_cluster_large, 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_large, 1), new ComparableStack(ModItems.pellet_cluster, 8)));
        this.register(new GenericRecipe("ass.warheadbb1").setup(100, 100).outputItems(new ItemStack(ModItems.warhead_buster_small, 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_small, 1), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 2)));
        this.register(new GenericRecipe("ass.warheadbb2").setup(200, 100).outputItems(new ItemStack(ModItems.warhead_buster_medium, 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_medium, 1), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 4)));
        this.register(new GenericRecipe("ass.warheadbb3").setup(400, 100).outputItems(new ItemStack(ModItems.warhead_buster_large, 1))
                .inputItems(new ComparableStack(ModItems.warhead_generic_large, 1), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 8)));
        this.register(new GenericRecipe("ass.warheadnuke").setup(400, 100).outputItems(new ItemStack(ModItems.warhead_nuclear, 1))
                .inputItems(new OreDictStack(TI.plateCast(), 12), new OreDictStack(PB.plateCast(), 6), new OreDictStack(U235.billet(), 6), new ComparableStack(ModItems.cordite, 12), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.warheadthermonuke").setup(600, 100).outputItems(new ItemStack(ModItems.warhead_mirv, 1))
                .inputItems(new OreDictStack(TI.plateCast(), 12), new OreDictStack(PB.plateCast(), 6), new OreDictStack(PU239.billet(), 8), new ComparableStack(ModItems.ball_tatb, 12), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CONTROLLER_ADVANCED))
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 4_000))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.warheadvolcano").setup(600, 100).outputItems(new ItemStack(ModItems.warhead_volcano, 1))
                .inputItems(new OreDictStack(TI.plateCast(), 12), new OreDictStack(STEEL.plateCast(), 6), new ComparableStack(ModBlocks.det_nuke, 3), new OreDictStack(U238.block(), 24), new ComparableStack(ModItems.circuit, 5, EnumCircuitType.CAPACITOR_BOARD.ordinal())));
        this.register(new GenericRecipe("ass.warheadn2").setup(300, 100).outputItems(new ItemStack(ModItems.warhead_n2, 1))
                .inputItems(new ComparableStack(ModItems.n2_charge, 5), new ComparableStack(ModItems.hull_big_steel, 2), new ComparableStack(ModItems.hull_small_steel, 1), new ComparableStack(ModItems.wire, 6, Mats.MAT_COPPER.id), new OreDictStack(STEEL.plate(), 20), new OreDictStack(TI.plate(), 12), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER)));
        this.register(new GenericRecipe("ass.warheadmirvlet").setup(100, 100).outputItems(new ItemStack(ModItems.warhead_mirvlet, 1))
                .inputItems(new OreDictStack(W.ingot(), 1), new OreDictStack(STEEL.plate(), 3), new OreDictStack(PU239.ingot(), 1), new OreDictStack(ANY_PLASTICEXPLOSIVE.ingot(), 1)));
        this.register(new GenericRecipe("ass.warheadthermoendo").setup(300, 100).outputItems(new ItemStack(ModItems.warhead_thermo_endo, 1))
                .inputItems(new ComparableStack(ModBlocks.therm_endo, 2), new OreDictStack(TI.plate(), 12), new OreDictStack(STEEL.plate(), 6)));
        this.register(new GenericRecipe("ass.warheadthermoexo").setup(300, 100).outputItems(new ItemStack(ModItems.warhead_thermo_exo, 1))
                .inputItems(new ComparableStack(ModBlocks.therm_exo, 2), new OreDictStack(TI.plate(), 12), new OreDictStack(STEEL.plate(), 6)));
        this.register(new GenericRecipe("ass.thrusternerva").setup(600, 100).outputItems(new ItemStack(ModItems.thruster_nuclear, 1))
                .inputItems(new OreDictStack(DURA.ingot(), 32), new OreDictStack(B.ingot(), 8), new OreDictStack(PB.plate(), 16), new OreDictStack(STEEL.pipe(), 4)));
        this.register(new GenericRecipe("ass.stealthmissile").setup(1_200, 100).outputItems(new ItemStack(ModItems.missile_stealth, 1))
                .inputItems(new OreDictStack(TI.plate(), 20), new OreDictStack(AL.plate(), 20), new OreDictStack(KEY_BLACK, 16), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 4), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.ADVANCED.ordinal()), new OreDictStack(STEEL.bolt(), 32)));
        this.register(new GenericRecipe("ass.shuttlemissile").setup(200, 100).outputItems(new ItemStack(ModItems.missile_shuttle, 1))
                .inputItems(new ComparableStack(ModItems.missile_generic, 1), new ComparableStack(ModItems.missile_strong, 1), new OreDictStack(KEY_ORANGE, 5), new ComparableStack(ModItems.canister_full, 24, Fluids.GASOLINE_LEADED.getID()), new OreDictStack(FIBER.ingot(), 12), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.BASIC), new OreDictStack(ANY_PLASTICEXPLOSIVE.ingot(), 8), new OreDictStack(KEY_ANYPANE, 6), new OreDictStack(STEEL.plate(), 4)));
        this.register(new GenericRecipe("ass.launchpad").setup(200, 100).outputItems(new ItemStack(ModBlocks.launch_pad_large, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 6), new OreDictStack(ANY_CONCRETE.any(), 64), new OreDictStack(ANY_PLASTIC.ingot(), 16), new ComparableStack(ModBlocks.steel_scaffold, 24), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.ADVANCED)));
        this.register(new GenericRecipe("ass.launchpadsilo").setup(200, 100).outputItems(new ItemStack(ModBlocks.launch_pad, 1))
                .inputItems(new OreDictStack(STEEL.plateWelded(), 8), new OreDictStack(ANY_CONCRETE.any(), 8), new OreDictStack(ANY_HARDPLASTIC.ingot(), 16), new ComparableStack(ModItems.circuit, 4, EnumCircuitType.ADVANCED)));

        // custom missile thrusters
        this.register(new GenericRecipe("ass.mpt10kero").setup(100, 100).outputItems(new ItemStack(ModItems.mp_thruster_10_kerosene, 1))
                .inputItems(new ComparableStack(ModItems.seg_10, 1), new OreDictStack(STEEL.pipe(), 1), new OreDictStack(W.ingot(), 4), new OreDictStack(STEEL.plate(), 4)));
        this.register(new GenericRecipe("ass.mpt10solid").setup(100, 100).outputItems(new ItemStack(ModItems.mp_thruster_10_solid, 1))
                .inputItems(new ComparableStack(ModItems.seg_10, 1), new OreDictStack(DURA.pipe(), 1), new OreDictStack(W.ingot(), 4), new OreDictStack(STEEL.plate(), 4)));
        this.register(new GenericRecipe("ass.mpt10xenon").setup(100, 100).outputItems(new ItemStack(ModItems.mp_thruster_10_xenon, 1))
                .inputItems(new ComparableStack(ModItems.seg_10, 1), new OreDictStack(DURA.plate(), 4), new ComparableStack(ModItems.arc_electrode, 1)));
        this.register(new GenericRecipe("ass.mpt15kero").setup(200, 100).outputItems(new ItemStack(ModItems.mp_thruster_15_kerosene, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(STEEL.pipe(), 3), new OreDictStack(W.ingot(), 8), new OreDictStack(STEEL.plate(), 8)));
        this.register(new GenericRecipe("ass.mpt15kerodual").setup(200, 100).outputItems(new ItemStack(ModItems.mp_thruster_15_kerosene_dual, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(STEEL.pipe(), 3), new OreDictStack(W.ingot(), 8), new OreDictStack(STEEL.plate(), 8)));
        this.register(new GenericRecipe("ass.mpt15kerotriple").setup(200, 100).outputItems(new ItemStack(ModItems.mp_thruster_15_kerosene_triple, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(STEEL.pipe(), 3), new OreDictStack(W.ingot(), 8), new OreDictStack(STEEL.plate(), 8)));
        this.register(new GenericRecipe("ass.mpt15solid").setup(200, 100).outputItems(new ItemStack(ModItems.mp_thruster_15_solid, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(DURA.pipe(), 3), new OreDictStack(W.ingot(), 8), new OreDictStack(STEEL.plate(), 8)));
        this.register(new GenericRecipe("ass.mpt15solid16").setup(200, 100).outputItems(new ItemStack(ModItems.mp_thruster_15_solid_hexdecuple, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(DURA.pipe(), 3), new OreDictStack(W.ingot(), 8), new OreDictStack(STEEL.plate(), 8)));
        this.register(new GenericRecipe("ass.mpt15hydro").setup(200, 100).outputItems(new ItemStack(ModItems.mp_thruster_15_hydrogen, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(DURA.pipe(), 3), new OreDictStack(W.ingot(), 8), new OreDictStack(DESH.ingot(), 4)));
        this.register(new GenericRecipe("ass.mpt15hydrodual").setup(200, 100).outputItems(new ItemStack(ModItems.mp_thruster_15_hydrogen_dual, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(DURA.pipe(), 3), new OreDictStack(W.ingot(), 8), new OreDictStack(DESH.ingot(), 4)));
        this.register(new GenericRecipe("ass.mpt15bfshort").setup(200, 100).outputItems(new ItemStack(ModItems.mp_thruster_15_balefire_short, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(DURA.pipe(), 5), new OreDictStack(W.plateCast(), 8), new OreDictStack(BIGMT.plate(), 8)));
        this.register(new GenericRecipe("ass.mpt15bf").setup(200, 100).outputItems(new ItemStack(ModItems.mp_thruster_15_balefire, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(DURA.pipe(), 5), new OreDictStack(W.plateCast(), 16), new OreDictStack(BIGMT.plate(), 16)));
        this.register(new GenericRecipe("ass.mpt15bflarge").setup(200, 100).outputItems(new ItemStack(ModItems.mp_thruster_15_balefire_large, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(DURA.pipe(), 10), new OreDictStack(W.plateCast(), 16), new OreDictStack(BIGMT.plate(), 24)));
        this.register(new GenericRecipe("ass.mpt20kero").setup(400, 100).outputItems(new ItemStack(ModItems.mp_thruster_20_kerosene, 1))
                .inputItems(new ComparableStack(ModItems.seg_20, 1), new OreDictStack(STEEL.pipe(), 6), new OreDictStack(W.ingot(), 16), new OreDictStack(STEEL.plate(), 16)));
        this.register(new GenericRecipe("ass.mpt20kerodual").setup(400, 100).outputItems(new ItemStack(ModItems.mp_thruster_20_kerosene_dual, 1))
                .inputItems(new ComparableStack(ModItems.seg_20, 1), new OreDictStack(STEEL.pipe(), 6), new OreDictStack(W.ingot(), 16), new OreDictStack(STEEL.plate(), 16)));
        this.register(new GenericRecipe("ass.mpt20kerotriple").setup(400, 100).outputItems(new ItemStack(ModItems.mp_thruster_20_kerosene_triple, 1))
                .inputItems(new ComparableStack(ModItems.seg_20, 1), new OreDictStack(STEEL.pipe(), 6), new OreDictStack(W.ingot(), 16), new OreDictStack(STEEL.plate(), 16)));
        this.register(new GenericRecipe("ass.mpt20solid").setup(400, 100).outputItems(new ItemStack(ModItems.mp_thruster_20_solid, 1))
                .inputItems(new ComparableStack(ModItems.seg_20, 1), new OreDictStack(DURA.pipe(), 6), new OreDictStack(W.ingot(), 16), new OreDictStack(STEEL.plate(), 16)));
        this.register(new GenericRecipe("ass.mpt20solidmulti").setup(400, 100).outputItems(new ItemStack(ModItems.mp_thruster_20_solid_multi, 1))
                .inputItems(new ComparableStack(ModItems.seg_20, 1), new OreDictStack(DURA.pipe(), 6), new OreDictStack(W.ingot(), 16), new OreDictStack(STEEL.plate(), 16)));
        this.register(new GenericRecipe("ass.mpt20solidmultier").setup(400, 100).outputItems(new ItemStack(ModItems.mp_thruster_20_solid_multier, 1))
                .inputItems(new ComparableStack(ModItems.seg_20, 1), new OreDictStack(DURA.pipe(), 6), new OreDictStack(W.ingot(), 16), new OreDictStack(STEEL.plate(), 16)));

        // custom missile warheads
        this.register(new GenericRecipe("ass.mpw10he").setup(100, 100).outputItems(new ItemStack(ModItems.mp_warhead_10_he, 1))
                .inputItems(new ComparableStack(ModItems.seg_10, 1), new OreDictStack(STEEL.plate(), 6), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 3), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.mpw10inc").setup(100, 100).outputItems(new ItemStack(ModItems.mp_warhead_10_incendiary, 1))
                .inputItems(new ComparableStack(ModItems.seg_10, 1), new OreDictStack(STEEL.plate(), 6), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 2), new OreDictStack(P_RED.dust(), 6), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.mpw10bus").setup(100, 100).outputItems(new ItemStack(ModItems.mp_warhead_10_buster, 1))
                .inputItems(new ComparableStack(ModItems.seg_10, 1), new OreDictStack(WEAPONSTEEL.plate(), 6), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 6), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.mpw10nukesmall").setup(200, 100).outputItems(new ItemStack(ModItems.mp_warhead_10_nuclear, 1))
                .inputItems(new ComparableStack(ModItems.seg_10, 1), new OreDictStack(WEAPONSTEEL.plate(), 16), new OreDictStack(PU239.billet(), 2), new OreDictStack(OreDictManager.getReflector(), 4), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 4), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.mpw10nukelarge").setup(200, 100).outputItems(new ItemStack(ModItems.mp_warhead_10_nuclear_large, 1))
                .inputItems(new ComparableStack(ModItems.seg_10, 1), new OreDictStack(WEAPONSTEEL.plate(), 16), new OreDictStack(PU239.billet(), 6), new OreDictStack(OreDictManager.getReflector(), 8), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 12), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.mpw10taint").setup(100, 100).outputItems(new ItemStack(ModItems.mp_warhead_10_taint, 1))
                .inputItems(new ComparableStack(ModItems.seg_10, 1), new OreDictStack(STEEL.plate(), 12), new ComparableStack(ModBlocks.det_cord, 2), new ComparableStack(ModItems.powder_magic, 12), new OreDictStack(Fluids.WATZ.getDict(1_000), 1)));
        this.register(new GenericRecipe("ass.mpw10cloud").setup(100, 100).outputItems(new ItemStack(ModItems.mp_warhead_10_cloud, 1))
                .inputItems(new ComparableStack(ModItems.seg_10, 1), new OreDictStack(STEEL.plate(), 12), new ComparableStack(ModBlocks.det_cord, 2), new ComparableStack(ModItems.powder_magic, 16)));
        this.register(new GenericRecipe("ass.mpw15he").setup(200, 100).outputItems(new ItemStack(ModItems.mp_warhead_15_he, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(STEEL.plate(), 12), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 12), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.mpw15inc").setup(200, 100).outputItems(new ItemStack(ModItems.mp_warhead_15_incendiary, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(STEEL.plate(), 12), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 8), new OreDictStack(P_RED.dust(), 16), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.BASIC)));
        this.register(new GenericRecipe("ass.mpw15nuke").setup(400, 100).outputItems(new ItemStack(ModItems.mp_warhead_15_nuclear, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(WEAPONSTEEL.plate(), 32), new OreDictStack(PU239.billet(), 12), new OreDictStack(OreDictManager.getReflector(), 12), new ComparableStack(ModItems.ball_tatb, 24), new ComparableStack(ModItems.circuit, 3, EnumCircuitType.CONTROLLER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.mpw15n2").setup(400, 100).outputItems(new ItemStack(ModItems.mp_warhead_15_n2, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(WEAPONSTEEL.plate(), 32), new ComparableStack(ModItems.ball_tatb, 32), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.ADVANCED))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.mpw15bf").setup(400, 100).outputItems(new ItemStack(ModItems.mp_warhead_15_balefire, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(WEAPONSTEEL.plate(), 32), new OreDictStack(OreDictManager.getReflector(), 16), new ComparableStack(ModItems.powder_magic, 8), new ComparableStack(ModItems.egg_balefire_shard, 4), new OreDictStack(ANY_HIGHEXPLOSIVE.ingot(), 16), new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CONTROLLER))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.mpw15thermo").setup(600, 100).outputItems(new ItemStack(ModItems.mp_warhead_15_thermo, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(STEEL.plate(), 32), new OreDictStack(TI.plate(), 16), new ComparableStack(ModBlocks.det_nuke, 2), new ComparableStack(ModItems.mike_deut, 1), new ComparableStack(ModItems.mike_core, 1), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.CONTROLLER_ADVANCED))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));
        this.register(new GenericRecipe("ass.mpw15volcano").setup(600, 100).outputItems(new ItemStack(ModItems.mp_warhead_15_volcano, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(STEEL.plate(), 32), new OreDictStack(TI.plate(), 16), new ComparableStack(ModBlocks.det_nuke, 4), new OreDictStack(U238.block(), 32), new ComparableStack(ModItems.circuit, 6, EnumCircuitType.CAPACITOR_TANTALIUM), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED)));
        this.register(new GenericRecipe("ass.mpw15mirv").setup(600, 100).outputItems(new ItemStack(ModItems.mp_warhead_15_mirv, 1))
                .inputItems(new ComparableStack(ModItems.seg_15, 1), new OreDictStack(STEEL.plate(), 24), new OreDictStack(TI.plate(), 16), new ComparableStack(ModItems.warhead_mirvlet, 7), new OreDictStack(ANY_PLASTICEXPLOSIVE.ingot(), 2), new ComparableStack(ModItems.hull_small_steel, 1), new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER_ADVANCED))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));

        // ammo
        this.register(new GenericRecipe("ass.50bmgsm").setup(100, 100).outputItems(new ItemStack(ModItems.ammo_standard, 6, GunFactory.EnumAmmo.BMG50_SM.ordinal()))
                .inputItems(new ComparableStack(ModItems.casing, 1, EnumCasingType.LARGE_STEEL), new OreDictStack(ANY_SMOKELESS.dust(), 6), new OreDictStack(STAR.ingot(), 3))
                .setPools(GenericRecipes.POOL_PREFIX_DISCOVER + "silverstorm"));
        this.register(new GenericRecipe("ass.50bmgbypass").setup(100, 100).outputItems(new ItemStack(ModItems.ammo_secret, 12, GunFactory.EnumAmmoSecret.BMG50_BLACK.ordinal()))
                .inputItems(new ComparableStack(ModItems.casing, 2, EnumCasingType.LARGE_STEEL), new OreDictStack(ANY_SMOKELESS.dust(), 24), new ComparableStack(ModItems.item_secret, 1, EnumSecretType.SELENIUM_STEEL), new ComparableStack(ModItems.black_diamond))
                .setPools(GenericRecipes.POOL_PREFIX_SECRET + "psalm"));
        this.register(new GenericRecipe("chem.shellchlorine").setup(100, 1_000).outputItems(new ItemStack(ModItems.ammo_arty, 1, 9))
                .inputItems(new ComparableStack(ModItems.ammo_arty, 1, 0), new OreDictStack(ANY_PLASTIC.ingot(), 1))
                .inputFluids(new FluidStack(Fluids.CHLORINE, 4_000)));
        this.register(new GenericRecipe("ass.shellphosgene").setup(100, 1_000).outputItems(new ItemStack(ModItems.ammo_arty, 1, 10))
                .inputItems(new ComparableStack(ModItems.ammo_arty, 1, 0), new OreDictStack(ANY_PLASTIC.ingot(), 1))
                .inputFluids(new FluidStack(Fluids.PHOSGENE, 4_000)));
        this.register(new GenericRecipe("ass.shellmustard").setup(100, 1_000).outputItems(new ItemStack(ModItems.ammo_arty, 1, 11))
                .inputItems(new ComparableStack(ModItems.ammo_arty, 1, 0), new OreDictStack(ANY_PLASTIC.ingot(), 1))
                .inputFluids(new FluidStack(Fluids.MUSTARDGAS, 4_000)));

        // tools
        this.register(new GenericRecipe("ass.multitool").setup(100, 100).outputItems(new ItemStack(ModItems.multitool_hit, 1))
                .inputItems(new OreDictStack(ANY_RESISTANTALLOY.ingot(), 4), new OreDictStack(STEEL.plate(), 4), new OreDictStack(GOLD.wireFine(), 12), new ComparableStack(ModItems.motor, 4), new ComparableStack(ModItems.circuit, 16, EnumCircuitType.CAPACITOR_BOARD)));

        // space
        this.register(new GenericRecipe("ass.soyuz").setup(6_000, 100).outputItems(new ItemStack(ModItems.missile_soyuz, 1))
                .inputItems(new OreDictStack(TI.shell(), 32),
                        new OreDictStack(RUBBER.ingot(), 64),
                        new ComparableStack(ModItems.rocket_fuel, 64),
                        new ComparableStack(ModItems.thruster_small, 12),
                        new ComparableStack(ModItems.thruster_medium, 12),
                        new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CONTROLLER),
                        new ComparableStack(ModItems.part_generic, 32, EnumPartType.LDE)).setPools(GenericRecipes.POOL_PREFIX_DISCOVER + "soyuz"));
        this.register(new GenericRecipe("ass.lander").setup(2_400, 100).outputItems(new ItemStack(ModItems.missile_soyuz_lander, 1))
                .inputItems(new OreDictStack(AL.shell(), 4),
                        new OreDictStack(RUBBER.ingot(), 16),
                        new ComparableStack(ModItems.rocket_fuel, 16),
                        new ComparableStack(ModItems.thruster_small, 3),
                        new ComparableStack(ModItems.circuit, 3, EnumCircuitType.CONTROLLER_ADVANCED),
                        new ComparableStack(ModItems.part_generic, 12, EnumPartType.LDE)).setPools(GenericRecipes.POOL_PREFIX_DISCOVER + "soyuz"));
        this.register(new GenericRecipe("ass.satellitebase").setup(600, 100).outputItems(new ItemStack(ModItems.sat_base, 1))
                .inputItems(new OreDictStack(RUBBER.ingot(), 12),
                        new OreDictStack(TI.shell(), 3),
                        new ComparableStack(ModItems.thruster_large, 1),
                        new ComparableStack(ModItems.part_generic, 8, EnumPartType.LDE),
                        new ComparableStack(ModItems.plate_desh, 4),
                        new ComparableStack(ModItems.fluid_barrel_full, 1, Fluids.KEROSENE.getID()),
                        new ComparableStack(ModItems.photo_panel, 24),
                        new ComparableStack(ModItems.circuit, 12, EnumCircuitType.BASIC),
                        new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM)));
        this.register(new GenericRecipe("ass.satellitemapper").setup(600, 100).outputItems(new ItemStack(ModItems.sat_head_mapper, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 3),
                        new ComparableStack(ModItems.plate_desh, 4),
                        new ComparableStack(ModItems.circuit, 4, EnumCircuitType.ADVANCED),
                        new ComparableStack(ModBlocks.glass_quartz, 8)));
        this.register(new GenericRecipe("ass.satellitescanner").setup(600, 100).outputItems(new ItemStack(ModItems.sat_head_scanner, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 3),
                        new OreDictStack(TI.plateCast(), 8),
                        new ComparableStack(ModItems.plate_desh, 4),
                        new ComparableStack(ModItems.magnetron, 8),
                        new ComparableStack(ModItems.circuit, 8, EnumCircuitType.ADVANCED.ordinal())));
        this.register(new GenericRecipe("ass.satelliteradar").setup(600, 100).outputItems(new ItemStack(ModItems.sat_head_radar, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 3),
                        new OreDictStack(TI.plateCast(), 12),
                        new ComparableStack(ModItems.magnetron, 12),
                        new ComparableStack(ModItems.coil_gold, 16),
                        new ComparableStack(ModItems.circuit, 4, EnumCircuitType.ADVANCED.ordinal())));
        this.register(new GenericRecipe("ass.satellitelaser").setup(600, 100).outputItems(new ItemStack(ModItems.sat_head_laser, 1))
                .inputItems(new OreDictStack(STEEL.shell(), 6),
                        new OreDictStack(CU.plateCast(), 24),
                        new OreDictStack(ANY_HARDPLASTIC.ingot(), 16),
                        new ComparableStack(ModItems.circuit, 8, EnumCircuitType.CONTROLLER_ADVANCED),
                        new ComparableStack(ModItems.circuit, 16, EnumCircuitType.CAPACITOR_BOARD),
                        new ComparableStack(ModItems.crystal_diamond, 8),
                        new ComparableStack(ModBlocks.glass_quartz, 8)));
        this.register(new GenericRecipe("ass.satelliteresonator").setup(600, 100).outputItems(new ItemStack(ModItems.sat_head_resonator, 1))
                .inputItems(new OreDictStack(STEEL.plateCast(), 6),
                        new OreDictStack(STAR.ingot(), 12),
                        new OreDictStack(ANY_PLASTIC.ingot(), 48),
                        new ComparableStack(ModItems.crystal_xen, 1),
                        new ComparableStack(ModItems.circuit, 16, EnumCircuitType.ADVANCED)));
        this.register(new GenericRecipe("ass.satelliterelay").setup(600, 100).outputItems(new ItemStack(ModItems.satellite, 1, EnumSatelliteType.RELAY.ordinal()))
                .inputItems(new OreDictStack(TI.shell(), 3),
                        new ComparableStack(ModItems.plate_desh, 8),
                        new ComparableStack(ModItems.fluid_barrel_full, 1, Fluids.HYDROGEN.getID()),
                        new ComparableStack(ModItems.photo_panel, 16),
                        new ComparableStack(ModItems.thruster_nuclear, 1),
                        new ComparableStack(ModItems.ingot_uranium_fuel, 6),
                        new ComparableStack(ModItems.circuit, 24, EnumCircuitType.BASIC),
                        new ComparableStack(ModItems.magnetron, 3),
                        new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM)));
        this.register(new GenericRecipe("ass.satelliteasteroidminer").setup(600, 100).outputItems(new ItemStack(ModItems.satellite, 1, EnumSatelliteType.MINER_ASTRO.ordinal()))
                .inputItems(new OreDictStack(BIGMT.plate(), 24),
                        new ComparableStack(ModItems.motor_desh, 2),
                        new ComparableStack(ModItems.drill_titanium, 2),
                        new ComparableStack(ModItems.circuit, 12, EnumCircuitType.ADVANCED),
                        new ComparableStack(ModItems.fluid_barrel_full, 1, Fluids.KEROSENE.getID()),
                        new ComparableStack(ModItems.thruster_small, 1),
                        new ComparableStack(ModItems.photo_panel, 12),
                        new ComparableStack(ModItems.centrifuge_element, 4),
                        new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM)));
        this.register(new GenericRecipe("ass.satellitelunarminer").setup(600, 100).outputItems(new ItemStack(ModItems.satellite, 1, EnumSatelliteType.MINER_LUNAR.ordinal()))
                .inputItems(new ComparableStack(ModItems.ingot_meteorite, 4),
                        new ComparableStack(ModItems.plate_desh, 4),
                        new ComparableStack(ModItems.motor, 2),
                        new ComparableStack(ModItems.drill_titanium, 2),
                        new ComparableStack(ModItems.circuit, 8, EnumCircuitType.ADVANCED),
                        new ComparableStack(ModItems.fluid_barrel_full, 1, Fluids.KEROSENE.getID()),
                        new ComparableStack(ModItems.thruster_small, 1),
                        new ComparableStack(ModItems.photo_panel, 12),
                        new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM)));
        this.register(new GenericRecipe("ass.orbitallaser").setup(1_200, 100)
                .outputItems(new ItemStack(ModItems.satellite, 1, EnumSatelliteType.PRECISION_LASER.ordinal()))
                .inputItems(new OreDictStack(WEAPONSTEEL.shell(), 16),
                        new ComparableStack(ModItems.photo_panel, 32),
                        new ComparableStack(ModItems.battery_pack, 1, ItemBatteryPack.EnumBatteryPack.CAPACITOR_TANTALUM),
                        new ComparableStack(ModItems.crystal_redstone, 16),
                        new ComparableStack(ModItems.part_generic, 16, EnumPartType.LDE),
                        new ComparableStack(ModItems.circuit, 3, EnumCircuitType.CONTROLLER_ADVANCED)));
        this.register(new GenericRecipe("ass.detectorsat").setup(1_200, 100)
                .outputItems(new ItemStack(ModItems.satellite, 1, EnumSatelliteType.DETECTOR.ordinal()))
                .inputItems(new OreDictStack(GOLD.plateCast(), 16),
                        new ComparableStack(ModItems.photo_panel, 64),
                        new OreDictStack(BSCCO.wireDense(), 16),
                        new ComparableStack(ModItems.circuit, 24, EnumCircuitType.BISMOID),
                        new ComparableStack(ModItems.part_generic, 16, EnumPartType.LDE),
                        new ComparableStack(ModItems.circuit, 1, EnumCircuitType.CONTROLLER_ADVANCED)));
        this.register(new GenericRecipe("ass.rayscansat").setup(1_200, 100)
                .outputItems(new ItemStack(ModItems.satellite, 1, EnumSatelliteType.RAY_SCAN.ordinal()))
                .inputItems(new OreDictStack(BIGMT.shell(), 16),
                        new ComparableStack(ModItems.photo_panel, 32),
                        new OreDictStack(SBD.wireDense(), 16),
                        new ComparableStack(ModItems.circuit, 16, EnumCircuitType.QUANTUM),
                        new ComparableStack(ModItems.part_generic, 16, EnumPartType.LDE),
                        new ComparableStack(ModItems.circuit, 3, EnumCircuitType.CONTROLLER_ADVANCED)));
        this.register(new GenericRecipe("ass.gundefabricator").setup(200, 100).outputItems(new ItemStack(ModItems.gun_defabricator, 1))
                .inputItems(
                        new OreDictStack(STEEL.ingot(), 2),
                        new OreDictStack(ANY_PLASTIC.ingot(), 8),
                        new OreDictStack(IRON.plateWelded(), 5),
                        new ComparableStack(ModItems.mechanism_special, 3),
                        new ComparableStack(ModItems.laser_crystal_dem, 1),
                        new ComparableStack(ModItems.plate_dalekanium, 3)
                ));
        this.register(new GenericRecipe("ass.cliprevolveriron").setup(40, 100).outputItems(new ItemStack(ModItems.clip_revolver_iron, 1))
                .inputItems(new ComparableStack(ModItems.plate_polymer), new ComparableStack(ModItems.gun_revolver_iron_ammo, 24)));
        this.register(new GenericRecipe("ass.cliprevolver").setup(40, 100).outputItems(new ItemStack(ModItems.clip_revolver, 1))
                .inputItems(new ComparableStack(ModItems.plate_polymer), new ComparableStack(ModItems.gun_revolver_ammo, 12)));
        this.register(new GenericRecipe("ass.cliprevolvergold").setup(40, 100).outputItems(new ItemStack(ModItems.clip_revolver_gold, 1))
                .inputItems(new ComparableStack(ModItems.plate_polymer), new ComparableStack(ModItems.gun_revolver_gold_ammo, 12)));
        this.register(new GenericRecipe("ass.cliprevolverlead").setup(40, 100).outputItems(new ItemStack(ModItems.clip_revolver_lead, 1))
                .inputItems(new ComparableStack(ModItems.plate_polymer), new ComparableStack(ModItems.gun_revolver_lead_ammo, 12)));
        this.register(new GenericRecipe("ass.clipdefabricator").setup(40, 100).outputItems(new ItemStack(ModItems.clip_defabricator, 1))
                .inputItems(new ComparableStack(ModItems.plate_polymer), new ComparableStack(ModItems.gun_defabricator_ammo, 16)));
        this.register(new GenericRecipe("ass.clipspark").setup(40, 100).outputItems(new ItemStack(ModItems.clip_spark, 1))
                .inputItems(new ComparableStack(ModItems.plate_polymer), new ComparableStack(ModItems.gun_spark_ammo, 4)));
        this.register(new GenericRecipe("ass.gunegon").setup(256, 100).outputItems(new ItemStack(ModItems.gun_egon, 1))
                .inputItems(
                        new ComparableStack(ModItems.mechanism_special, 4),
                        new OreDictStack(STEEL.plate(), 16),
                        new OreDictStack(PB.plate(), 24),
                        new ComparableStack(ModItems.coil_advanced_torus, 32),
                        new ComparableStack(ModItems.circuit, 4, EnumCircuitType.CHIP_QUANTUM),
                        new ComparableStack(ModItems.plate_polymer, 8),
                        new ComparableStack(ModBlocks.machine_schrabidium_battery, 2),
                        new ComparableStack(ModBlocks.machine_waste_drum, 1),
                        new ComparableStack(ModItems.wire, 8, Mats.MAT_COPPER.id)
                ));
        this.register(new GenericRecipe("ass.emptypackage").setup(40, 100).outputItems(new ItemStack(ModItems.fluid_pack_empty, 1))
                .inputItems(new OreDictStack(TI.plate(), 4), new OreDictStack(ANY_PLASTIC.ingot(), 2)));
        FluidType[] order = Fluids.getInNiceOrder();
        for(int i = 1; i < order.length; ++i) {
            FluidType type = order[i];
            if(type.hasNoContainer()) continue;
            this.register(new GenericRecipe("ass.package" + type.getTranslationKey()).setup(40, 100).outputItems(new ItemStack(ModItems.fluid_pack_full, 1, type.getID()))
                    .inputItems(new ComparableStack(ModItems.fluid_pack_empty)).inputFluids(new FluidStack(type, 32_000)));
            this.register(new GenericRecipe("ass.unpackage" + type.getTranslationKey()).setup(40, 100).setIcon(ItemFluidIcon.make(type, 32_000)).outputItems(new ItemStack(ModItems.fluid_pack_empty))
                    .inputItems(new ComparableStack(ModItems.fluid_pack_full, 1, type.getID())).outputFluids(new FluidStack(type, 32_000)));
        }

        /*if(GeneralConfig.enableMekanismChanges && Loader.isModLoaded("Mekanism")) {
            Block mb = (Block) Block.blockRegistry.getObject("Mekanism:MachineBlock");
            if(mb != null) {

                this.register(new GenericRecipe("ass.digimemer").setup(1_200, 100).outputItems(new ItemStack(mb, 1, 4))
                        .inputItems(new OreDictStack(BIGMT.plateCast(), 16),
                                new OreDictStack(CU.plateWelded(), 12),
                                new OreDictStack("alloyUltimate", 32),
                                new ComparableStack(ModItems.circuit, 16, EnumCircuitType.BISMOID),
                                new ComparableStack(ModItems.circuit, 32, EnumCircuitType.CAPACITOR_BOARD),
                                new ComparableStack(ModItems.wire_dense, 32, Mats.MAT_GOLD.id),
                                new ComparableStack(ModItems.motor_bismuth, 3)));
            }
        }*/

        registerBackpackUpgradeRecipes();
        registerSecretBackpackRecipes();
    }

    @Override
    public void registerPost() {
        registerBackpackUpgradeRecipes();
        registerSecretBackpackRecipes();
    }

    private void registerBackpackUpgradeRecipes() {
        registerBackpackUpgradeRecipe(new GenericRecipe("ass.backpack_upgrade_capacity_2")
                .setup(200, 2_000)
                .outputItems(new ItemStack(ModItems.backpack_upgrade_capacity_2))
                .inputItems(
                        new ComparableStack(ModItems.backpack_upgrade_capacity_1),
                        new OreDictStack(TI.plate(), 4),
                        new ComparableStack(ModItems.plate_polymer, 4),
                        new ComparableStack(ModItems.motor, 2),
                        new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC)));

        registerBackpackUpgradeRecipe(new GenericRecipe("ass.backpack_upgrade_capacity_3")
                .setup(600, 10_000)
                .outputItems(new ItemStack(ModItems.backpack_upgrade_capacity_3))
                .inputItems(
                        new ComparableStack(ModItems.backpack_upgrade_capacity_2),
                        new OreDictStack(DESH.plate(), 4),
                        new OreDictStack(SA326.plate(), 2),
                        new ComparableStack(ModItems.motor_desh, 2),
                        new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED)));

        registerBackpackUpgradeRecipe(new GenericRecipe("ass.backpack_upgrade_magnet_2")
                .setup(400, 4_000)
                .outputItems(new ItemStack(ModItems.backpack_upgrade_magnet_2))
                .inputItems(
                        new ComparableStack(ModItems.backpack_upgrade_magnet_1),
                        new ComparableStack(ModItems.battery_lithium),
                        new ComparableStack(ModItems.motor_desh),
                        new OreDictStack(MAGTUNG.wireFine(), 8),
                        new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED)));

        registerBackpackUpgradeRecipe(new GenericRecipe("ass.backpack_upgrade_range_2")
                .setup(400, 4_000)
                .outputItems(new ItemStack(ModItems.backpack_upgrade_range_2))
                .inputItems(
                        new ComparableStack(ModItems.backpack_upgrade_range_1),
                        new ComparableStack(ModItems.radar_linker),
                        new ComparableStack(ModItems.sat_chip, 2),
                        new OreDictStack(DESH.plate(), 2),
                        new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED)));

        registerSecretBackpackRecipe(new GenericRecipe("ass.backpack_upgrade_magnet_3")
                .setup(1_200, 100_000)
                .outputItems(new ItemStack(ModItems.backpack_upgrade_magnet_3))
                .inputItems(
                        new ComparableStack(ModItems.backpack_upgrade_magnet_2),
                        new ComparableStack(ModItems.battery_schrabidium_cell),
                        new ComparableStack(ModItems.entanglement_kit),
                        new ComparableStack(ModItems.plate_dineutronium, 2),
                        new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CHIP_QUANTUM)),
                "secret.backpack_pocket_hole");

        registerSecretBackpackRecipe(new GenericRecipe("ass.backpack_upgrade_range_3")
                .setup(1_200, 100_000)
                .outputItems(new ItemStack(ModItems.backpack_upgrade_range_3))
                .inputItems(
                        new ComparableStack(ModItems.backpack_upgrade_range_2),
                        new ComparableStack(ModItems.entanglement_kit),
                        new ComparableStack(ModItems.sat_chip, 8),
                        new ComparableStack(ModItems.plate_dineutronium, 2),
                        new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CHIP_QUANTUM)),
                "secret.backpack_pocket_hole");

        registerSecretBackpackRecipe(new GenericRecipe("ass.backpack_upgrade_ammo_feeder")
                .setup(600, 10_000)
                .outputItems(new ItemStack(ModItems.backpack_upgrade_ammo_feeder))
                .inputItems(
                        new ComparableStack(ModItems.ammo_bag),
                        new ComparableStack(ModItems.motor, 2),
                        new OreDictStack(WEAPONSTEEL.mechanism(), 4),
                        new OreDictStack(W.plateCast(), 2),
                        new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CONTROLLER)),
                "secret.backpack_sapper");
    }

    private void registerBackpackUpgradeRecipe(GenericRecipe recipe) {
        if (!recipeNameMap.containsKey(recipe.getInternalName())) register(recipe);
    }

    /**
     * These recipes are gameplay secrets and must remain available even when a
     * custom assembly-machine JSON replaces the default recipe set.
     */
    private void registerSecretBackpackRecipes() {
        registerSecretBackpackRecipe(new GenericRecipe("ass.backpack_stalker")
                .setup(600, 10_000)
                .outputItems(new ItemStack(ModItems.backpack_stalker))
                .inputItems(
                        new ComparableStack(ModItems.backpack_reinforced_steel),
                        new ComparableStack(ModItems.hazmat_cloth_grey, 8),
                        new ComparableStack(ModItems.gas_mask_filter_combo, 2),
                        new ComparableStack(ModItems.geiger_counter),
                        new ComparableStack(ModItems.stealth_boy),
                        new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CHIP)),
                "secret.backpack_stalker");

        registerSecretBackpackRecipe(new GenericRecipe("ass.backpack_black_box")
                .setup(600, 10_000)
                .outputItems(new ItemStack(ModItems.backpack_black_box))
                .inputItems(
                        new ComparableStack(ModItems.backpack_lead),
                        new ComparableStack(ModItems.containment_box),
                        new OreDictStack(W.plateWelded(), 4),
                        new OreDictStack(DURA.plate(), 4),
                        new ComparableStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED)),
                "secret.backpack_black_box");

        registerSecretBackpackRecipe(new GenericRecipe("ass.backpack_ash")
                .setup(400, 4_000)
                .outputItems(new ItemStack(ModItems.backpack_ash))
                .inputItems(
                        new ComparableStack(ModItems.backpack_steel),
                        new ComparableStack(ModItems.asbestos_cloth, 8),
                        new OreDictStack(BIGMT.plate(), 4),
                        new ComparableStack(ModItems.powder_ash, 8, EnumAshType.SOOT),
                        new OreDictStack(P_RED.dust(), 4)),
                "secret.backpack_ash");

        registerSecretBackpackRecipe(new GenericRecipe("ass.backpack_nuclear_tourist")
                .setup(400, 4_000)
                .outputItems(new ItemStack(ModItems.backpack_nuclear_tourist))
                .inputItems(
                        new ComparableStack(ModItems.backpack_titanium),
                        new OreDictStack(PB.plate(), 4),
                        new ComparableStack(ModItems.hazmat_cloth, 8),
                        new ComparableStack(ModItems.geiger_counter),
                        new ComparableStack(ModItems.circuit, 2, EnumCircuitType.BASIC)),
                "secret.backpack_nuclear_tourist");

        registerSecretBackpackRecipe(new GenericRecipe("ass.backpack_pocket_hole")
                .setup(1_200, 100_000)
                .outputItems(new ItemStack(ModItems.backpack_pocket_hole))
                .inputItems(
                        new ComparableStack(ModItems.backpack_dineutronium),
                        new ComparableStack(ModItems.tiny_black_hole),
                        new ComparableStack(ModItems.entanglement_kit),
                        new OreDictStack(SA326.plate(), 4),
                        new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CHIP_QUANTUM)),
                "secret.backpack_pocket_hole");

        registerSecretBackpackRecipe(new GenericRecipe("ass.backpack_sapper")
                .setup(600, 10_000)
                .outputItems(new ItemStack(ModItems.backpack_sapper))
                .inputItems(
                        new ComparableStack(ModItems.backpack_reinforced_steel),
                        new ComparableStack(ModItems.defuser),
                        new ComparableStack(ModItems.defuser),
                        new ComparableStack(ModItems.detonator_multi),
                        new OreDictStack(W.plateCast(), 4),
                        new ComparableStack(ModItems.stick_c4, 4),
                        new ComparableStack(ModItems.stick_semtex, 4),
                        new ComparableStack(ModItems.ducttape, 4),
                        new ComparableStack(ModItems.circuit, 2, EnumCircuitType.CONTROLLER)),
                "secret.backpack_sapper");

        registerSecretBackpackRecipe(new GenericRecipe("ass.backpack_smuggler")
                .setup(400, 4_000)
                .outputItems(new ItemStack(ModItems.backpack_smuggler))
                .inputItems(
                        new ComparableStack(ModItems.backpack_reinforced_steel),
                        new ComparableStack(ModItems.plastic_bag, 9),
                        new ComparableStack(ModItems.ducttape, 8),
                        new ComparableStack(ModItems.key_kit),
                        new ComparableStack(ModItems.key_fake),
                        new ComparableStack(ModItems.key_fake),
                        new ComparableStack(ModItems.plate_polymer, 4),
                        new ComparableStack(ModItems.circuit, 1, EnumCircuitType.BASIC)),
                "secret.backpack_smuggler");
    }

    private void registerSecretBackpackRecipe(GenericRecipe recipe, String pool) {
        GenericRecipe existing = recipeNameMap.get(recipe.getInternalName());
        if (existing != null) {
            if (hasSameOutput(existing, recipe)) {
                if (!existing.isPartOfPool(pool)) {
                    List<String> pools = new ArrayList<>();
                    if (existing.getPools() != null) pools.addAll(Arrays.asList(existing.getPools()));
                    pools.add(pool);
                    removeBlueprintReferences(existing.getInternalName());
                    existing.setPools(pools.toArray(new String[0]));
                }
                return;
            }

            removeRecipeByName(existing.getInternalName());
            removeBlueprintReferences(existing.getInternalName());
        }
        recipe.setPools(pool);
        register(recipe);
    }

    private static boolean hasSameOutput(GenericRecipe first, GenericRecipe second) {
        if (first.outputItem == null || first.outputItem.length == 0
                || second.outputItem == null || second.outputItem.length == 0) {
            return false;
        }
        for (ItemStack left : first.outputItem[0].getAllPossibilities()) {
            for (ItemStack right : second.outputItem[0].getAllPossibilities()) {
                if (ItemStack.areItemsEqual(left, right)
                        && ItemStack.areItemStackTagsEqual(left, right)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void removeBlueprintReferences(String recipeName) {
        for (List<String> recipes : GenericRecipes.blueprintPools.values()) {
            recipes.removeIf(recipeName::equals);
        }
        GenericRecipes.pooledBlueprints.remove(recipeName);
    }

    public static HashMap<Object, Object> getRecipes() {
        HashMap<Object, Object> recipes = new HashMap<>();

        for(GenericRecipe recipe : INSTANCE.recipeOrderedList) {
            List<Object> input = new ArrayList<>();
            if(recipe.inputItem != null) input.addAll(Arrays.asList(recipe.inputItem));
            if(recipe.inputFluid != null) for(FluidStack stack : recipe.inputFluid) input.add(ItemFluidIcon.make(stack));
            List<Object> output = new ArrayList<>();
            if(recipe.outputItem != null) for(IOutput stack : recipe.outputItem) output.add(stack.getAllPossibilities());
            if(recipe.outputFluid != null) for(FluidStack stack : recipe.outputFluid) output.add(ItemFluidIcon.make(stack));
            recipes.put(input.toArray(), output.toArray());
        }

        return recipes;
    }
}
