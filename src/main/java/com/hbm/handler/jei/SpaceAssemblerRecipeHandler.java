package com.hbm.handler.jei;

import com.hbm.inventory.recipes.SpaceAssemblerRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemSatellitePayload.EnumSatelliteType;
import mezz.jei.api.IGuiHelper;
import net.minecraft.item.ItemStack;

public class SpaceAssemblerRecipeHandler extends JEIGenericRecipeHandler {
    public SpaceAssemblerRecipeHandler(IGuiHelper helper) {
        super(helper, JEIConfig.SPACE_ASSEMBLER, "item.satellite.science_assembler", SpaceAssemblerRecipes.INSTANCE,
                new ItemStack(ModItems.satellite, 1, EnumSatelliteType.SCIENCE_ASSEMBLER.ordinal()));
    }

    @Override
    public int[][] getOutputSlotPositions(int count) {
        if(count == 9) return new int[][]{{102, 6}, {120, 6}, {138, 6}, {102, 24}, {120, 24}, {138, 24}, {102, 42}, {120, 42}, {138, 42}};
        return super.getOutputSlotPositions(count);
    }
}
