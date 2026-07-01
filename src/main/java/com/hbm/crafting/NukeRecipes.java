package com.hbm.crafting;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import com.hbm.main.CraftingManager;
import net.minecraft.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

public class NukeRecipes {

    public static void register() {
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.early_explosive_lenses, 1),
                "EEE", "EPE", "EEE", 'E', ModItems.gadget_explosive, 'P', AL.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.explosive_lenses, 1),
                "EEE", "ESE", "EEE", 'E', ModItems.man_explosive, 'S', STEEL.plate());

        CraftingManager.addShapelessAuto(new ItemStack(ModItems.gadget_kit, 1), ModBlocks.nuke_gadget, ModItems.early_explosive_lenses, ModItems.early_explosive_lenses, ModItems.early_explosive_lenses, ModItems.early_explosive_lenses, ModItems.gadget_wireing, ModItems.gadget_core, ModItems.hazmat_kit);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.boy_kit, 1), ModBlocks.nuke_boy, ModItems.boy_shielding, ModItems.boy_target, ModItems.boy_bullet, ModItems.boy_propellant, ModItems.boy_igniter, ModItems.hazmat_kit);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.man_kit, 1), ModBlocks.nuke_man, ModBlocks.det_nuke, ModItems.man_igniter, ModItems.hazmat_kit);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.mike_kit, 1), ModBlocks.nuke_mike, ModBlocks.det_nuke, ModItems.mike_core, ModItems.mike_deut, ModItems.mike_cooling_unit, ModItems.hazmat_red_kit);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.tsar_kit, 1), ModBlocks.nuke_tsar, ModBlocks.det_nuke, ModItems.mike_core, ModItems.mike_deut, ModItems.mike_core, ModItems.mike_deut, ModItems.hazmat_grey_kit);

        CraftingManager.addRecipeAuto(new ItemStack(ModItems.defuser_desh), " SD", "S S", " S ", 'D', DESH.ingot(), 'S', POLYMER.ingot());
        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.det_n2, 1), "PDT", "DDD", "PDP", 'P', STEEL.plateCast(), 'D', ModItems.n2_charge, 'T', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.BASIC));
        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.det_bale, 1), "DAP", "DCD", "DBD", 'D', TI.plateCast(), 'A', ModItems.powder_power, 'B', ModItems.powder_magic, 'C', ModItems.egg_balefire, 'P', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.CONTROLLER_ADVANCED));
        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.machine_telelinker), "PSP", "SCS", "PSP", 'P', STEEL.plate(), 'S', ALLOY.ingot(), 'C', ModItems.turret_biometry);
    }
}