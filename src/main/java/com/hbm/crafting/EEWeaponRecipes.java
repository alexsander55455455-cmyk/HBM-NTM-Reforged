package com.hbm.crafting;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.CraftingManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

/** EE weapon crafts missing from CE WeaponRecipes. */
public class EEWeaponRecipes {

    public static void register() {
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.mechanism_revolver_1, 1), " II", "ICA", "IKW", 'I', IRON.plate(), 'C', CU.ingot(), 'A', AL.ingot(), 'K', new ItemStack(ModItems.wire, 1, Mats.MAT_COPPER.id), 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_ALUMINIUM.id));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.mechanism_revolver_2, 1), " II", "ICA", "IKW", 'I', ALLOY.plate(), 'C', DURA.ingot(), 'A', W.ingot(), 'K', new ItemStack(ModItems.bolt, 1, Mats.MAT_DURA.id), 'W', new ItemStack(ModItems.bolt, 1, Mats.MAT_STEEL.id));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.mechanism_rifle_1, 1), "ICI", "CMA", "IAM", 'I', IRON.plate(), 'C', CU.ingot(), 'A', AL.ingot(), 'M', ModItems.mechanism_revolver_1);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.mechanism_rifle_2, 1), "ICI", "CMA", "IAM", 'I', ALLOY.plate(), 'C', DURA.ingot(), 'A', W.ingot(), 'M', ModItems.mechanism_revolver_2);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.mechanism_launcher_1, 1), "TTT", "SSS", "BBI", 'T', TI.plate(), 'S', STEEL.ingot(), 'B', new ItemStack(ModItems.bolt, 1, Mats.MAT_STEEL.id), 'I', MINGRADE.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.mechanism_launcher_2, 1), "TTT", "SSS", "BBI", 'T', ALLOY.plate(), 'S', ANY_PLASTIC.ingot(), 'B', new ItemStack(ModItems.bolt, 1, Mats.MAT_DURA.id), 'I', DESH.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.mechanism_special, 1), "PCI", "ISS", "PCI", 'P', ModItems.plate_desh, 'C', ModItems.coil_advanced_alloy, 'I', STAR.ingot(), 'S', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.ADVANCED));

        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_kit_1, 4), "I ", "LB", "P ", 'I', ANY_RUBBER.ingot(), 'L', Fluids.LUBRICANT.getDict(1_000), 'B', new ItemStack(ModItems.bolt, 1, Mats.MAT_STEEL.id), 'P', IRON.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_kit_2, 1), "III", "GLG", "PPP", 'I', ANY_RUBBER.ingot(), 'L', ModItems.ducttape, 'G', ModItems.gun_kit_1, 'P', IRON.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_rpg, 1), "SSW", " MW", 'S', ModItems.hull_small_steel, 'W', IRON.plate(), 'M', ModItems.mechanism_launcher_1);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_karl, 1), "SSW", " MW", 'S', ModItems.hull_small_steel, 'W', ALLOY.plate(), 'M', ModItems.mechanism_launcher_2);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver, 1), "SSM", " RW", 'S', STEEL.plate(), 'W', KEY_PLANKS, 'R', new ItemStack(ModItems.wire, 1, Mats.MAT_ALUMINIUM.id), 'M', ModItems.mechanism_revolver_1);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_saturnite, 1), "SSM", " RW", 'S', BIGMT.plate(), 'W', KEY_PLANKS, 'R', new ItemStack(ModItems.wire, 1, Mats.MAT_TUNGSTEN.id), 'M', ModItems.mechanism_revolver_2);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_iron, 1), "SSM", " RW", 'S', IRON.plate(), 'W', KEY_PLANKS, 'R', new ItemStack(ModItems.wire, 1, Mats.MAT_ALUMINIUM.id), 'M', ModItems.mechanism_revolver_1);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_gold, 1), "SSM", " RW", 'S', GOLD.plate(), 'W', GOLD.ingot(), 'R', new ItemStack(ModItems.wire, 1, Mats.MAT_GOLD.id), 'M', ModItems.mechanism_revolver_1);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_lead, 1), "SSM", " RW", 'S', PB.plate(), 'W', W.ingot(), 'R', new ItemStack(ModItems.wire, 1, Mats.MAT_TUNGSTEN.id), 'M', ModItems.mechanism_revolver_2);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_cursed, 1), "TTM", "SRI", 'S', STEEL.plate(), 'I', STEEL.ingot(), 'R', new ItemStack(ModItems.wire, 1, Mats.MAT_MINGRADE.id), 'T', TI.plate(), 'M', ModItems.mechanism_revolver_2);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_nightmare, 1), "SEM", " RW", 'S', STEEL.plate(), 'W', KEY_PLANKS, 'R', new ItemStack(ModItems.wire, 1, Mats.MAT_ALUMINIUM.id), 'E', ModItems.powder_power, 'M', ModItems.mechanism_revolver_2);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_nightmare2, 1), "SSM", "RRW", 'S', getReflector(), 'W', W.ingot(), 'R', new ItemStack(ModItems.wire, 1, Mats.MAT_GOLD.id), 'M', ModItems.mechanism_special);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_mirv, 1), "LLL", "WFW", "SSS", 'S', STEEL.plate(), 'L', PB.plate(), 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_GOLD.id), 'F', ModItems.gun_fatman);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_proto, 1), "LLL", "WFW", "SSS", 'S', ANY_RUBBER.ingot(), 'L', ModItems.plate_desh, 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_TUNGSTEN.id), 'F', ModItems.gun_fatman);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_bf, 1), new Object[] { "LLL", "WFW", "SSS", 'S', ModItems.plate_paa, 'L', OreDictManager.getReflector(), 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_ALLOY.id), 'F', ModItems.gun_mirv });
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_mp40, 1), "IIM", " SW", " S ", 'S', STEEL.plate(), 'I', STEEL.ingot(), 'W', KEY_PLANKS, 'M', ModItems.mechanism_rifle_2);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_flechette, 1), "PPM", "TIS", "G  ", 'P', STEEL.plate(), 'M', ModItems.mechanism_rifle_2, 'T', ModItems.hull_small_steel, 'I', STEEL.ingot(), 'S', ANY_PLASTIC.ingot(), 'G', ModItems.mechanism_launcher_1);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_uboinik, 1), "IIM", "SPW", 'P', STEEL.plate(), 'I', STEEL.ingot(), 'W', KEY_PLANKS, 'S', Items.STICK, 'M', ModItems.mechanism_revolver_2);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_xvl1456, 1), "PBB", "ACC", "PRY", 'P', STEEL.plate(), 'R', ModItems.redcoil_capacitor, 'A', ModItems.coil_advanced_alloy, 'B', ModItems.battery_generic, 'C', ModItems.coil_advanced_torus, 'Y', ModItems.mechanism_special);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_osipr, 1), "CCT", "WWI", "MCC", 'C', CMB.plate(), 'T', W.ingot(), 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_MAGTUNG.id), 'I', ModItems.mechanism_rifle_2, 'M', ModItems.coil_magnetized_tungsten);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_immolator, 1), "WCC", "PMT", "WAA", 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_GOLD.id), 'C', CU.plate(), 'P', ALLOY.plate(), 'M', ModItems.mechanism_launcher_1, 'T', ModItems.tank_steel, 'A', STEEL.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_cryolator, 1), "SSS", "IWL", "LMI", 'S', STEEL.plate(), 'I', IRON.plate(), 'L', Items.LEATHER, 'M', ModItems.mechanism_launcher_1, 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_ALUMINIUM.id));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_jack, 1), "WW ", "TSD", " TT", 'W', ModItems.ingot_weidanium, 'T', ModItems.toothpicks, 'S', ModItems.gun_uboinik, 'D', ModItems.ducttape);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_euthanasia, 1), "TDT", "AAS", " T ", 'A', AUSTRALIUM.ingot(), 'T', ModItems.toothpicks, 'S', ModItems.gun_mp40, 'D', ModItems.ducttape);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_spark, 1), "TTD", "AAS", "  T", 'A', ModItems.ingot_daffergon, 'T', ModItems.toothpicks, 'S', ModItems.gun_rpg, 'D', ModItems.ducttape);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_skystinger, 1), "TTT", "AAS", " D ", 'A', ModItems.ingot_unobtainium, 'T', ModItems.toothpicks, 'S', ModItems.gun_stinger, 'D', ModItems.ducttape);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_hp, 1), "TDT", "ASA", " T ", 'A', REIIUM.ingot(), 'T', ModItems.toothpicks, 'S', ModItems.gun_xvl1456, 'D', ModItems.ducttape);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_lever_action, 1), "PPI", "SWD", 'P', IRON.plate(), 'I', ModItems.mechanism_rifle_1, 'S', Items.STICK, 'D', KEY_PLANKS, 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_ALUMINIUM.id));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_lever_action_dark, 1), "PPI", "SWD", 'P', STEEL.plate(), 'I', ModItems.mechanism_rifle_1, 'S', Items.STICK, 'D', KEY_PLANKS, 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_ALUMINIUM.id));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_bolt_action, 1), "PPI", "SWD", 'P', STEEL.plate(), 'I', ModItems.mechanism_rifle_1, 'S', Items.STICK, 'D', KEY_PLANKS, 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_COPPER.id));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_bolt_action_green, 1), "PPI", "SWD", 'P', IRON.plate(), 'I', ModItems.mechanism_rifle_1, 'S', Items.STICK, 'D', KEY_PLANKS, 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_COPPER.id));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_bolt_action_saturnite, 1), "PPI", "SWD", 'P', BIGMT.plate(), 'I', ModItems.mechanism_rifle_1, 'S', Items.STICK, 'D', KEY_PLANKS, 'W', new ItemStack(ModItems.wire, 1, Mats.MAT_TUNGSTEN.id));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_uzi, 1), "SMS", " PB", " P ", 'S', STEEL.ingot(), 'M', ModItems.mechanism_rifle_2, 'P', STEEL.plate(), 'B', new ItemStack(ModItems.bolt, 1, Mats.MAT_DURA.id));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_minigun, 1), "PIB", "PCM", "PIB", 'P', ModItems.pipes_steel, 'B', STEEL.block(), 'I', ANY_PLASTIC.ingot(), 'C', ModItems.mechanism_rifle_2, 'M', ModItems.motor);

        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_uzi_silencer, 1), "P  ", " P ", "  U", 'P', ANY_PLASTIC.ingot(), 'U', ModItems.gun_uzi);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_uzi_saturnite, 1), "SMS", " PB", " P ", 'S', BIGMT.ingot(), 'M', ModItems.mechanism_rifle_2, 'P', BIGMT.plate(), 'B', new ItemStack(ModItems.bolt, 1, Mats.MAT_STEEL.id));
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_uzi_saturnite_silencer, 1), "P  ", " P ", "  U", 'P', ANY_PLASTIC.ingot(), 'U', ModItems.gun_uzi_saturnite);

        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_vortex, 1), "AS ", "SIP", " SC", 'S', ModItems.plate_armor_lunar, 'I', ModItems.gun_xvl1456, 'A', ModItems.levitation_unit, 'P', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.CONTROLLER_ADVANCED), 'C', ModItems.crystal_trixite);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_pip, 1), " G ", "SSP", " TI", 'G', KEY_ANYPANE, 'S', STEEL.plate(), 'P', ModItems.mechanism_revolver_2, 'T', new ItemStack(ModItems.wire, 1, Mats.MAT_TUNGSTEN.id), 'I', ANY_PLASTIC.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_nopip, 1), "SSP", " TI", 'S', STEEL.plate(), 'P', ModItems.mechanism_revolver_2, 'T', new ItemStack(ModItems.wire, 1, Mats.MAT_TUNGSTEN.id), 'I', ANY_PLASTIC.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_silver, 1), "SSP", " TI", 'S', AL.plate(), 'P', ModItems.mechanism_revolver_2, 'T', new ItemStack(ModItems.wire, 1, Mats.MAT_TUNGSTEN.id), 'I', KEY_PLANKS);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_blackjack, 1), "SSP", " TI", 'S', STEEL.plate(), 'P', ModItems.mechanism_revolver_2, 'T', new ItemStack(ModItems.wire, 1, Mats.MAT_TUNGSTEN.id), 'I', KEY_PLANKS);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_revolver_red, 1), "R ", " B", 'R', ModItems.key_red, 'B', ModItems.gun_revolver_blackjack);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_calamity, 1), " PI", "BBM", " PI", 'P', IRON.plate(), 'B', ModItems.pipes_steel, 'M', ModItems.mechanism_rifle_1, 'I', STEEL.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_calamity_dual, 1), "BBM", " PI", "BBM", 'P', IRON.plate(), 'B', ModItems.pipes_steel, 'M', ModItems.mechanism_rifle_1, 'I', STEEL.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_avenger, 1), "PIB", "PCM", "PIB", 'P', ModItems.pipes_steel, 'B', BE.block(), 'I', DESH.ingot(), 'C', ModItems.mechanism_rifle_2, 'M', ModItems.motor);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_lacunae, 1), "TIT", "ILI", "PRP", 'T', ModItems.syringe_taint, 'I', STAR.ingot(), 'L', ModItems.gun_minigun, 'P', ModItems.pellet_rtg, 'R', ModBlocks.machine_rtg_grey);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_mp, 1), "EEE", "SSM", "III", 'E', EUPH.ingot(), 'S', STEEL.plate(), 'I', STEEL.ingot(), 'M', ModItems.mechanism_rifle_2);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_emp, 1), "CPG", "CMF", "CPI", 'C', ModItems.coil_copper, 'P', PB.plate(), 'G', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.CONTROLLER), 'M', ModItems.magnetron, 'I', W.ingot(), 'F', ModItems.mechanism_special);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.gun_b93, 1), "PCE", "SEB", "PCE", 'P', ModItems.plate_dineutronium, 'C', ModItems.weaponized_starblaster_cell, 'E', ModItems.component_emitter, 'B', ModItems.gun_b92, 'S', ModItems.singularity_spark);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.jshotgun, 1), "LPP", "SSW", "PPD", 'S', ModItems.gun_uboinik, 'P', STEEL.plate(), 'D', new ItemStack(Items.DYE, 1, EnumDyeColor.GREEN.getDyeDamage()), 'L', ModBlocks.spinny_light, 'W', ModItems.mechanism_rifle_2);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.gun_moist_nugget, 12), Items.BREAD, Items.WHEAT, Items.COOKED_CHICKEN, Items.EGG);

    }
}
