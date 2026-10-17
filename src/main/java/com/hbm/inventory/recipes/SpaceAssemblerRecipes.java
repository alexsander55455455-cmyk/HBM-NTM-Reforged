package com.hbm.inventory.recipes;

import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.machine.ItemOrbitalAssembly.EnumOrbitalAssembly;
import net.minecraft.item.ItemStack;

public class SpaceAssemblerRecipes extends GenericRecipes<GenericRecipe> {
    public static final SpaceAssemblerRecipes INSTANCE = new SpaceAssemblerRecipes();

    @Override public int inputItemLimit() { return 1; }
    @Override public int inputFluidLimit() { return 0; }
    @Override public int outputItemLimit() { return 9; }
    @Override public int outputFluidLimit() { return 0; }
    @Override public boolean hasPower() { return false; }
    @Override public String getFileName() { return "hbmSpaceAssembler.json"; }
    @Override public GenericRecipe instantiateRecipe(String name) { return new GenericRecipe(name); }

    @Override
    public void registerDefaults() {
        register(new GenericRecipe("space.crystalcircuit").setup(3 * 60 * 20, 0)
                .inputItems(new ComparableStack(ModItems.orbital_assembly, 1, EnumOrbitalAssembly.CRYSTAL_CIRCUIT))
                .outputItems(DictFrame.fromOne(ModItems.circuit, EnumCircuitType.CRYSTAL)));
    }

    public GenericRecipe getRecipe(ItemStack stack) {
        if(stack == null || stack.isEmpty()) return null;
        for(GenericRecipe recipe : recipeOrderedList) {
            if(recipe.inputItem != null && recipe.inputItem.length == 1
                    && recipe.inputItem[0].matchesRecipe(stack, true)
                    && stack.getCount() >= recipe.inputItem[0].stacksize) return recipe;
        }
        return null;
    }
}
