package com.hbmspace.inventory.recipes.tweakers;

import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.special.ItemWasteShort;
import com.hbm.util.WeightedRandomObject;
import com.hbmspace.enums.EnumAddonWasteTypes;
import com.hbmspace.items.ModItemsSpace;
import net.minecraft.item.ItemStack;

import static com.hbm.inventory.recipes.SILEXRecipes.recipes;

public class SILEXRecipesTweaker {

    public static void init() {

        recipes.put(new RecipesCommon.ComparableStack(ModItemsSpace.ingot_cm_mix), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new WeightedRandomObject(new ItemStack(ModItemsSpace.nugget_cm244), 3))
                .addOut(new WeightedRandomObject(new ItemStack(ModItemsSpace.nugget_cm245), 6))
        );

        for (int i = 0; i < 5; i++) {
            recipes.put(new RecipesCommon.ComparableStack(ModItemsSpace.rbmk_pellet_bk247, 1, i), new SILEXRecipes.SILEXRecipe(600, 100, EnumWavelengths.UV)
                    .addOut(new ItemStack(ModItemsSpace.nugget_bk247), 100 - i * 20)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cf251), 1 + 10 * i / 40)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.BERKELIUM247.ordinal()), 4 + 6 * i));
            recipes.put(new RecipesCommon.ComparableStack(ModItemsSpace.rbmk_pellet_bk247, 1, i + 5), new SILEXRecipes.SILEXRecipe(600, 100, EnumWavelengths.UV)
                    .addOut(new ItemStack(ModItemsSpace.nugget_bk247), 100 - i * 20)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cf251), 1 + 10 * i / 40)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny), 3)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.BERKELIUM247.ordinal()), 4 + 6 * i));

            recipes.put(new RecipesCommon.ComparableStack(ModItemsSpace.rbmk_pellet_lecm, 1, i), new SILEXRecipes.SILEXRecipe(600, 100, EnumWavelengths.UV)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm_fuel), 100 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_pu_mix), 50 - i * 10)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm247), 1 + 2 * i / 50)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM244.ordinal()), 2 + i)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM245.ordinal()), 2 + i));
            recipes.put(new RecipesCommon.ComparableStack(ModItemsSpace.rbmk_pellet_lecm, 1, i + 5), new SILEXRecipes.SILEXRecipe(600, 100, EnumWavelengths.UV)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm_fuel), 100 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_pu_mix), 50 - i * 10)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm247), 1 + 2 * i / 50)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny), 3)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM244.ordinal()), 2 + i)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM245.ordinal()), 2 + i));

            recipes.put(new RecipesCommon.ComparableStack(ModItemsSpace.rbmk_pellet_mecm, 1, i), new SILEXRecipes.SILEXRecipe(600, 100, EnumWavelengths.UV)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm_mix), 100 - i * 20)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm247), 2 + 4 * i / 30)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM244.ordinal()), 4 + 4 * i)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM245.ordinal()), 5 + 4 * i));
            recipes.put(new RecipesCommon.ComparableStack(ModItemsSpace.rbmk_pellet_mecm, 1, i + 5), new SILEXRecipes.SILEXRecipe(600, 100, EnumWavelengths.UV)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm_mix), 100 - i * 20)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm247), 2 + 4 * i / 30)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny), 3)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM244.ordinal()), 4 + 4 * i)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM245.ordinal()), 5 + 4 * i));

            recipes.put(new RecipesCommon.ComparableStack(ModItemsSpace.rbmk_pellet_hecm, 1, i), new SILEXRecipes.SILEXRecipe(600, 100, EnumWavelengths.UV)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm245), 100 - i * 20)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm247), 3 + 5 * i / 30)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM244.ordinal()), 6 + 7 * i)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM245.ordinal()), 7 + 8 * i));
            recipes.put(new RecipesCommon.ComparableStack(ModItemsSpace.rbmk_pellet_hecm, 1, i + 5), new SILEXRecipes.SILEXRecipe(600, 100, EnumWavelengths.UV)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm245), 100 - i * 20)
                    .addOut(new ItemStack(ModItemsSpace.nugget_cm247), 3 + 5 * i / 30)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny), 3)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM244.ordinal()), 6 + 7 * i)
                    .addOut(new ItemStack(ModItems.nuclear_waste_short_tiny, 1, EnumAddonWasteTypes.CURIUM245.ordinal()), 7 + 8 * i));
        }

        recipes.put(new RecipesCommon.ComparableStack(ModItems.nuclear_waste_short, 1, EnumAddonWasteTypes.AMERICIUM241.ordinal()), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm_mix), 40)
                .addOut(new ItemStack(ModItems.nugget_pu239), 10)
                .addOut(new ItemStack(ModItems.powder_cs137_tiny), 5)
                .addOut(new ItemStack(ModItems.powder_i131_tiny), 5)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny), 10)
                .addOut(new ItemStack(ModItems.nugget_am242), 30)
        );
        recipes.put(new RecipesCommon.ComparableStack(ModItems.nuclear_waste_short_depleted, 1, EnumAddonWasteTypes.AMERICIUM241.ordinal()), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm_mix), 50)
                .addOut(new ItemStack(ModItems.nugget_pu239), 20)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny), 20)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm242), 10)
        );
        recipes.put(new RecipesCommon.ComparableStack(ModItems.nuclear_waste_short, 1, ItemWasteShort.WasteClass.AMERICIUM242.ordinal()), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm_mix), 70)
                .addOut(new ItemStack(ModItems.nugget_pu239), 10)
                .addOut(new ItemStack(ModItems.powder_cs137_tiny), 5)
                .addOut(new ItemStack(ModItems.powder_i131_tiny), 5)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny), 10)
        );
        recipes.put(new RecipesCommon.ComparableStack(ModItems.nuclear_waste_short_depleted, 1, ItemWasteShort.WasteClass.AMERICIUM242.ordinal()), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm_mix), 50)
                .addOut(new ItemStack(ModItems.nugget_pu239), 20)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny), 10)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm242), 20)
        );
        recipes.put(new RecipesCommon.ComparableStack(ModItems.nuclear_waste_short, 1, EnumAddonWasteTypes.BERKELIUM247.ordinal()), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new ItemStack(ModItemsSpace.nugget_cf251), 40)
                .addOut(new ItemStack(ModItems.nugget_am_mix), 10)
                .addOut(new ItemStack(ModItems.powder_cs137_tiny), 5)
                .addOut(new ItemStack(ModItems.powder_sr90_tiny), 5)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny), 10)
        );

        recipes.put(new RecipesCommon.ComparableStack(ModItems.nuclear_waste_short_depleted, 1, EnumAddonWasteTypes.BERKELIUM247.ordinal()), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm_mix), 50)
                .addOut(new ItemStack(ModItemsSpace.nugget_cf251), 20)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny), 10)
                .addOut(new ItemStack(ModItems.nugget_am_mix), 20)
        );

        recipes.put(new RecipesCommon.ComparableStack(ModItems.nuclear_waste_short, 1, EnumAddonWasteTypes.CURIUM244.ordinal()), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm245), 30)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm246), 15)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm247), 10)
                .addOut(new ItemStack(ModItemsSpace.nugget_cf251), 25)
                .addOut(new ItemStack(ModItemsSpace.nugget_cf252), 20)
                .addOut(new ItemStack(ModItemsSpace.nugget_es253), 10)
        );
        recipes.put(new RecipesCommon.ComparableStack(ModItems.nuclear_waste_short_depleted, 1, EnumAddonWasteTypes.CURIUM244.ordinal()), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm_mix), 40)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm246), 5)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm247), 15)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny), 10)
                .addOut(new ItemStack(ModItemsSpace.nugget_cf251), 20)
                .addOut(new ItemStack(ModItemsSpace.nugget_cf252), 10)
        );
        recipes.put(new RecipesCommon.ComparableStack(ModItems.nuclear_waste_short, 1, EnumAddonWasteTypes.CURIUM245.ordinal()), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm246), 15)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm247), 10)
                .addOut(new ItemStack(ModItemsSpace.nugget_cf251), 35)
                .addOut(new ItemStack(ModItemsSpace.nugget_cf252), 30)
                .addOut(new ItemStack(ModItemsSpace.nugget_es253), 10)
        );
        recipes.put(new RecipesCommon.ComparableStack(ModItems.nuclear_waste_short_depleted, 1, EnumAddonWasteTypes.CURIUM245.ordinal()), new SILEXRecipes.SILEXRecipe(900, 100, EnumWavelengths.XRAY)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm_mix), 10)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm246), 5)
                .addOut(new ItemStack(ModItemsSpace.nugget_cm247), 15)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny), 10)
                .addOut(new ItemStack(ModItemsSpace.nugget_cf252), 15)
                .addOut(new ItemStack(ModItemsSpace.nugget_cf251), 25)
        );
    }
}
