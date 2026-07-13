package com.hbm.integration.groovy.script;

import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.api.documentation.annotations.RegistryDescription;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.VirtualizedRegistry;
import com.hbm.integration.groovy.util.IngredientUtils;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.loader.GenericRecipe;

@RegistryDescription(linkGenerator = "hbm", isFullyDocumented = false)
public class BlastFurnace extends VirtualizedRegistry<GenericRecipe> {

    @Override
    public void onReload() {
        removeScripted().forEach(this::removeRecipe);
        restoreFromBackup().forEach(this::addRecipe);
    }

    private void addRecipe(GenericRecipe recipe) {
        BlastFurnaceRecipesNT.INSTANCE.register(recipe);
        addScripted(recipe);
    }

    private void removeRecipe(GenericRecipe recipe) {
        BlastFurnaceRecipesNT.INSTANCE.removeRecipeByName(recipe.name);
        addBackup(recipe);
    }

    public RecipeBuilder recipeBuilder() {
        return new RecipeBuilder();
    }

    public static class RecipeBuilder extends AbstractRecipeBuilder<GenericRecipe> {

        private int duration = 800;
        private String name;

        public RecipeBuilder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public RecipeBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public String getErrorMsg() {
            return "Error adding NTM Blast Furnace recipe";
        }

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg, 2, 2, 1, 1);
        }

        @Override
        public GenericRecipe register() {
            if(!validate()) return null;

            RecipesCommon.AStack in0 = IngredientUtils.convertIngredient2Astack(input.get(0));
            RecipesCommon.AStack in1 = IngredientUtils.convertIngredient2Astack(input.get(1));
            String recipeName = name != null ? name : "groovy.blast." + System.nanoTime();

            GenericRecipe recipe = new GenericRecipe(recipeName)
                    .setDuration(duration)
                    .inputItems(in0, in1)
                    .outputItems(output.get(0));

            BlastFurnaceRecipesNT.INSTANCE.register(recipe);
            return recipe;
        }
    }
}