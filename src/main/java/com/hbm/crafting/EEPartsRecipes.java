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

/** EE parts-tab bench crafts missing from the port. */
public class EEPartsRecipes {

    public static void register() {
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.folly_bullet, 1), " S ", "STS", "SMS", 'S', STAR.ingot(), 'T', ModItems.powder_magic, 'M', ModBlocks.block_meteor);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.folly_bullet_du, 1), " U ", "UDU", "UTU", 'U', U238.block(), 'D', DESH.block(), 'T', W.block());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.pellet_claws, 1), " X ", "X X", " XX", 'X', STEEL.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.thermo_unit_endo, 1), "EEE", "ETE", "EEE", 'E', Item.getItemFromBlock(Blocks.ICE), 'T', ModItems.thermo_unit_empty);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.thermo_unit_exo, 1), "LLL", "LTL", "LLL", 'L', Items.LAVA_BUCKET, 'T', ModItems.thermo_unit_empty);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.pellet_coal, 1), "PFP", "FOF", "PFP", 'P', COAL.dust(), 'F', Items.FLINT, 'O', ModBlocks.gravel_obsidian);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.cap_aluminium, 1), "PIP", 'P', AL.plate(), 'I', AL.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.hull_small_steel, 1), "PPP", "   ", "PPP", 'P', STEEL.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.hull_small_aluminium, 1), "PPP", "   ", "PPP", 'P', AL.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.hull_big_steel, 1), "III", "   ", "III", 'I', STEEL.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.hull_big_aluminium, 1), "III", "   ", "III", 'I', AL.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.hull_big_titanium, 1), "III", "   ", "III", 'I', TI.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.rotor_steel, 3), "CCC", "SSS", "CCC", 'C', ModItems.coil_gold, 'S', STEEL.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.generator_steel, 1), "RRR", "CCC", "SSS", 'C', ModItems.coil_gold_torus, 'S', STEEL.ingot(), 'R', ModItems.rotor_steel);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.arc_electrode_desh, 1), "C", "T", "C", 'C', DESH.dust(), 'T', ModItems.arc_electrode);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.stamp_schrabidium_flat, 1), " R ", "III", "SSS", 'R', REDSTONE.dust(), 'I', "ingotBrick", 'S', SA326.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.decontamination_module, 1), "GAG", "WTW", "GAG", 'W', AC.ingot(), 'T', ModBlocks.decon, 'G', RA226.nugget(), 'A', TCALLOY.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.coltass, 1), "ACA", "CXC", "ACA", 'A', ALLOY.ingot(), 'C', ModItems.cinnebar, 'X', Items.COMPASS);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.medal_ghoul, 1), "GEG", "BFB", "GEG", 'G',ModItems.nugget_u238m2, 'B', ModBlocks.pribris_digamma, 'E', ModItems.glitch, 'F', ModItems.medal_liquidator);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.pocket_ptsd, 1), " R ", "PBP", "PSP", 'R', ModBlocks.machine_radar, 'P', ANY_PLASTIC.ingot(), 'B', ModItems.battery_sc_polonium, 'S', ModBlocks.machine_siren);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gas_sensor, 1), "A", "B", "C", 'A', GOLD.plate(), 'B', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.VACUUM_TUBE), 'C', IRON.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.piston_pneumatic, 4), " I ", "CPC", " I ", 'I', IRON.ingot(), 'C', CU.ingot(), 'P', IRON.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.piston_hydraulic, 4), " I ", "CPC", " I ", 'I', STEEL.ingot(), 'C', TI.ingot(), 'P', Fluids.LUBRICANT.getDict(1_000));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.piston_electro, 4), " I ", "CPC", " I ", 'I', ANY_RESISTANTALLOY.ingot(), 'C', ANY_PLASTIC.ingot(), 'P', ModItems.motor);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.capsule_empty, 1), "STS", "GXG", "STS", 'S', ModItems.plate_armor_lunar, 'T', ModItems.coil_advanced_torus, 'G', GH336.ingot(), 'X', ModItems.particle_empty);

        CraftingManager.addShapelessAuto(new ItemStack(ModItems.billet_les, 9), SA326.billet(), NP237.billet(), NP237.billet(), NP237.billet(), NP237.billet(), BE.billet(), BE.billet(), BE.billet());
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.billet_hes, 4), SA326.billet(), SA326.billet(), NP237.billet(), BE.billet());
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.ingot_americium_fuel, 3), U238.ingot(), U238.ingot(), AMRG.ingot());
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.powder_tcalloy, 1), STEEL.dust(), TC99.nugget());
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.powder_radspice_tiny, 1), CO60.dustTiny(), SR90.dustTiny(), I131.dustTiny(), CS137.dustTiny(), XE135.dustTiny(), AU198.dustTiny(), PB209.dustTiny(), AT209.dustTiny(), AC227.dustTiny());
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.tem_flakes1, 1), GOLD.nugget(), GOLD.nugget(), GOLD.nugget(), Items.PAPER);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.tem_flakes2, 1), Items.GOLD_INGOT, Items.GOLD_INGOT, GOLD.nugget(), GOLD.nugget(), Items.PAPER);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.bobmazon_materials), Items.BOOK, GOLD.nugget(), Items.STRING);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.bobmazon_machines), Items.BOOK, GOLD.nugget(), new ItemStack(Items.DYE, 1, 1));
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.bobmazon_weapons), Items.BOOK, GOLD.nugget(), new ItemStack(Items.DYE, 1, 8));
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.bobmazon_tools), Items.BOOK, GOLD.nugget(), new ItemStack(Items.DYE, 1, 2));
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.billet_unobtainium), ModItems.nugget_radspice, AMRG.nugget(), ModItems.nugget_unobtainium_lesser, ModItems.nugget_unobtainium_greater, ModItems.nugget_unobtainium_greater, ModItems.nugget_unobtainium_greater);
    }
}