package com.hbm.handler.jei;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.SuperComputerRecipes;
import mezz.jei.api.IGuiHelper;

public class SuperComputerRecipeHandler extends JEIGenericRecipeHandler {
    public SuperComputerRecipeHandler(IGuiHelper helper) {
        super(helper, JEIConfig.SUPER_COMPUTER, ModBlocks.machine_supercomputer.getTranslationKey(), SuperComputerRecipes.INSTANCE, ModBlocks.machine_supercomputer);
    }
}
