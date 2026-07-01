package com.hbm.crafting;

import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemCanister;
import com.hbm.main.CraftingManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;
import static com.hbm.inventory.fluid.Fluids.*;

/** EE weapon-tab bench crafts missing from the port. */
public class EEWeaponTabRecipes {

    public static void register() {
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.ammo_dart, 16), "IPI", "ICI", "IPI",
                'I', ModItems.plate_polymer, 'P', IRON.plate(),
                'C', new ItemStack(ModItems.fluid_tank_full, 1, Fluids.WATZ.getID()));

        CraftingManager.addRecipeAuto(new ItemStack(ModItems.ammo_rocket_rpc, 2), "BP ", "CBH", " DR",
                'B', ModItems.blades_steel, 'P', STEEL.plate(), 'C', ItemCanister.getStackFromFluid(DIESEL),
                'H', ModItems.hull_small_steel, 'D', ModItems.piston_selenium, 'R', ModItems.ammo_rocket);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.ammo_rocket_rpc, 2), "BP ", "CBH", " DR",
                'B', ModItems.blades_steel, 'P', STEEL.plate(), 'C', ItemCanister.getStackFromFluid(PETROIL),
                'H', ModItems.hull_small_steel, 'D', ModItems.piston_selenium, 'R', ModItems.ammo_rocket);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.ammo_rocket_rpc, 2), "BP ", "CBH", " DR",
                'B', ModItems.blades_steel, 'P', STEEL.plate(), 'C', ItemCanister.getStackFromFluid(BIOFUEL),
                'H', ModItems.hull_small_steel, 'D', ModItems.piston_selenium, 'R', ModItems.ammo_rocket);

        CraftingManager.addRecipeAuto(new ItemStack(ModItems.turret_light_ammo, 1), " L ", "IGI", "ICI",
                'L', PB.plate(), 'I', IRON.plate(), 'C', CU.plate(), 'G', Items.GUNPOWDER);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.turret_heavy_ammo, 1), "LGC", "LGC", "LGC",
                'L', PB.plate(), 'C', CU.plate(), 'G', Items.GUNPOWDER);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.turret_rocket_ammo, 1), "TS ", "SGS", " SR",
                'T', Blocks.TNT, 'S', STEEL.plate(), 'G', Items.GUNPOWDER, 'R', REDSTONE.dust());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.turret_spitfire_ammo, 1), "CP ", "PTP", " PR",
                'P', STEEL.plate(), 'C', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.BASIC), 'T', Blocks.TNT, 'R', REDSTONE.dust());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.turret_cwis_ammo, 1), "LLL", "GGG", "IGI",
                'L', PB.plate(), 'I', IRON.plate(), 'G', Items.GUNPOWDER);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.turret_cheapo_ammo, 1), "ILI", "IGI", "ICI",
                'L', PB.plate(), 'I', STEEL.plate(), 'C', CU.plate(), 'G', Items.GUNPOWDER);

        CraftingManager.addRecipeAuto(new ItemStack(ModItems.turret_biometry, 1), "CC ", "GGS", "SSS",
                'C', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.BASIC), 'S', STEEL.plate(), 'G', GOLD.plate());
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.turret_control, 1), "R12", "PPI", "  I",
                'R', Items.REDSTONE, '1', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.CAPACITOR_BOARD),
                '2', DictFrame.fromOne(ModItems.circuit, EnumCircuitType.ADVANCED), 'P', STEEL.plate(), 'I', STEEL.ingot());

        CraftingManager.addRecipeAuto(new ItemStack(ModItems.hf_sword), "MEM", "YDY", "YCY",
                'M', ModItems.blade_meteorite, 'E', ModItems.ingot_radspice, 'Y', ModItems.billet_unobtainium,
                'D', ModItems.particle_strange, 'C', ModItems.ingot_chainsteel);
        CraftingManager.addRecipeAuto(new ItemStack(ModItems.hs_sword), "MEM", "YDY", "YCY",
                'M', ModItems.blade_meteorite, 'E', GH336.ingot(), 'Y', ModItems.billet_gh336,
                'D', ModItems.particle_dark, 'C', ModItems.ingot_chainsteel);
    }
}