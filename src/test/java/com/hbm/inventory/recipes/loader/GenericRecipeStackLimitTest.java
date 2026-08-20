package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.RecipesCommon;
import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenericRecipeStackLimitTest {

    @Test
    void splitsNonStackableIngredientsAcrossMachineSlots() {
        Item nonStackable = new Item().setMaxStackSize(1);

        RecipesCommon.AStack[] normalized = GenericRecipes.normalizeInputStacks(
                "test.recipe",
                new RecipesCommon.AStack[] {new RecipesCommon.ComparableStack(nonStackable, 4)},
                4);

        assertEquals(4, normalized.length);
        for(RecipesCommon.AStack ingredient : normalized) assertEquals(1, ingredient.stacksize);
    }

    @Test
    void keepsIngredientsThatFitOneStackUnchanged() {
        Item stackable = new Item().setMaxStackSize(16);

        RecipesCommon.AStack[] normalized = GenericRecipes.normalizeInputStacks(
                "test.recipe",
                new RecipesCommon.AStack[] {new RecipesCommon.ComparableStack(stackable, 12)},
                1);

        assertEquals(1, normalized.length);
        assertEquals(12, normalized[0].stacksize);
    }

    @Test
    void rejectsRecipesThatNeedMoreSlotsThanTheMachineProvides() {
        Item nonStackable = new Item().setMaxStackSize(1);

        assertThrows(IllegalStateException.class, () -> GenericRecipes.normalizeInputStacks(
                "test.recipe",
                new RecipesCommon.AStack[] {new RecipesCommon.ComparableStack(nonStackable, 4)},
                3));
    }
}
