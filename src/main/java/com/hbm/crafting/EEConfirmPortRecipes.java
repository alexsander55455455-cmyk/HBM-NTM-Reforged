package com.hbm.crafting;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import com.hbm.main.CraftingManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

/** Manually verified EE bench crafts still missing from the port after deep audit. */
public class EEConfirmPortRecipes {

    public static void register() {
        addConveyorBlockCrafts();
        addMachineCrafts();
        addConsumableCrafts();
        addControlCrafts();
        addMeleeWeaponCrafts();
    }

    private static void addConveyorBlockCrafts() {
        Item conveyor = Item.getItemFromBlock(ModBlocks.conveyor);
        Item conveyorDouble = Item.getItemFromBlock(ModBlocks.conveyor_double);
        CraftingManager.addRecipeAuto(new ItemStack(conveyor, 16), "LLL", "I I", "LLL", 'L', Items.LEATHER, 'I', IRON.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(conveyor, 64), "LLL", "I I", "LLL", 'L', ANY_RUBBER.ingot(), 'I', IRON.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(Item.getItemFromBlock(ModBlocks.conveyor_express), 8), "CCC", "CLC", "CCC", 'C', conveyor, 'L', Fluids.LUBRICANT.getDict(1_000));
        CraftingManager.addRecipeAuto(new ItemStack(conveyorDouble, 3), "CPC", "CPC", "CPC", 'C', conveyor, 'P', IRON.plate());
        CraftingManager.addRecipeAuto(new ItemStack(Item.getItemFromBlock(ModBlocks.conveyor_triple), 3), "CPC", "CPC", "CPC", 'C', conveyorDouble, 'P', STEEL.plate());
        CraftingManager.addRecipeAuto(new ItemStack(Item.getItemFromBlock(ModBlocks.conveyor_chute), 3), "IGI", "IGI", "ICI", 'I', IRON.ingot(), 'G', ModBlocks.steel_grate, 'C', conveyor);
        CraftingManager.addRecipeAuto(new ItemStack(Item.getItemFromBlock(ModBlocks.conveyor_lift), 3), "IGI", "IGI", "ICI", 'I', IRON.ingot(), 'G', ModBlocks.chain, 'C', conveyor);
    }

    private static void addMachineCrafts() {
        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.machine_minirtg, 1), "CRC", "CPC", "TAT", 'C', TI.plateCast(), 'R', ModItems.rtg_unit, 'P', ModItems.pellet_rtg, 'T', ModBlocks.brick_compound, 'A', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.BASIC));
        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.machine_powerrtg, 1), "CRC", "CPC", "TAT", 'C', W.plateWelded(), 'R', ModItems.rtg_unit, 'P', ModItems.pellet_rtg_polonium, 'T', ModBlocks.brick_compound, 'A', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.BISMOID));
        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.hadron_core, 1), "CCC", "DSD", "CCC", 'C', ModBlocks.hadron_coil_alloy, 'D', ModBlocks.hadron_diode, 'S', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.CONTROLLER_QUANTUM));
    }

    private static void addConsumableCrafts() {
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.mask_damp, 1), "RRR", 'R', ModItems.rag_damp);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.euphemium_stopper, 1), "I", "S", "S", 'I', EUPH.ingot(), 'S', Items.STICK);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.bismuth_tool, 1), "TBT", "SRS", "SCS", 'T', TA.nugget(), 'B', ANY_BISMOID.nugget(), 'S', TCALLOY.ingot(), 'R', ModItems.reacher, 'C', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.CHIP));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.canteen_13, 1), "O", "P", 'O', Items.POTIONITEM, 'P', STEEL.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.canteen_fab, 1), "VMV", "MVM", "VMV", 'V', ModItems.canteen_vodka, 'M', ModItems.powder_magic);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.bottle2_korl_special, 1), ModItems.bottle2_empty, Items.POTIONITEM, Items.SUGAR, CU.dust(), SR.dust());
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.bottle2_fritz_special, 1), ModItems.bottle2_empty, Items.POTIONITEM, Items.SUGAR, W.dust(), TH232.dust());
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.bottle2_sunset, 1), ModItems.bottle2_empty, Items.POTIONITEM, Items.SUGAR, GOLD.dust());
    }

    private static void addControlCrafts() {
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.inf_water_mk3, 1), "BPB", "PTP", "BPB", 'B', ModItems.inf_water_mk2, 'P', ModBlocks.fluid_duct_neo, 'T', ModBlocks.machine_fluidtank);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.inf_water_mk4, 1), "BPB", "PTP", "BPB", 'B', ModItems.inf_water_mk3, 'P', ModBlocks.fluid_duct_neo, 'T', ModBlocks.machine_bat9000);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.capsule_xen), ModItems.capsule_empty, ModItems.crystal_xen);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.fluid_tank_lead_full, 2), "LUL", "LTL", "LUL", 'L', PB.plate(), 'U', U238.billet(), 'T', ModItems.fluid_tank_full);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.pellet_rtg_balefire), ModItems.egg_balefire, ModItems.egg_balefire, ModItems.egg_balefire, IRON.plate());
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.nugget_americium_fuel, 3), U238.nugget(), U238.nugget(), AMRG.nugget());
    }

    private static void addMeleeWeaponCrafts() {
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.bat, 1), "P", "P", "S", 'S', STEEL.plate(), 'P', KEY_PLANKS);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.bat_nail, 1), ModItems.bat, STEEL.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.golf_club, 1), "IP", " P", " P", 'P', STEEL.plate(), 'I', STEEL.ingot());
    }
}