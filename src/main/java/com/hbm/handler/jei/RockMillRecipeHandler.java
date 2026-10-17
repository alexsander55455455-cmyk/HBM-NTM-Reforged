package com.hbm.handler.jei;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.RockMillRecipes;
import mezz.jei.api.IGuiHelper;

public class RockMillRecipeHandler extends JEIGenericRecipeHandler {
    public RockMillRecipeHandler(IGuiHelper helper) {
        super(helper, JEIConfig.ROCK_MILL, ModBlocks.machine_rockmill.getTranslationKey(), RockMillRecipes.INSTANCE, ModBlocks.machine_rockmill);
    }
}
