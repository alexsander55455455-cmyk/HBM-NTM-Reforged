package com.hbm.crafting;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.main.CraftingManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

/** EE block-tab bench crafts missing from the port. */
public class EEBlockRecipes {

    public static void register() {
        addColoredConcreteCrafts();

        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.muffler, 1), "III", "IWI", "III", 'I', ANY_RUBBER.ingot(), 'W', Blocks.WOOL);
        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.brick_dungeon, 4), "CC", "CC", 'C', ModBlocks.brick_dungeon_flat);
        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.brick_dungeon_tile, 4), "CC", "CC", 'C', ModBlocks.brick_dungeon);
        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.brick_dungeon_circle, 1), "CCC", "C C", "CCC", 'C', ModBlocks.brick_dungeon_tile);
        CraftingManager.addRecipeAuto(new ItemStack(ModBlocks.spinny_light), " G ", "GFG", "SRS", 'G', KEY_ANYGLASS, 'F', ModItems.fuse, 'S', new ItemStack(Blocks.STONE_SLAB, 1, 0), 'R', REDSTONE.dust());

        CraftingManager.addShapelessAuto(new ItemStack(ModBlocks.ladder_red), "dyeRed", ModBlocks.ladder_steel);
        CraftingManager.addShapelessAuto(new ItemStack(ModBlocks.ladder_red_top), ModBlocks.ladder_red);

        CraftingManager.addShapelessAuto(new ItemStack(ModBlocks.block_pu_mix, 3), PU239.block(), PU239.block(), PU240.block());

        ItemStack tritiumCell = new ItemStack(ModItems.cell, 1, Fluids.TRITIUM.getID());
        CraftingManager.addShapelessAuto(new ItemStack(ModBlocks.block_tritium),
                tritiumCell, tritiumCell, tritiumCell, tritiumCell, tritiumCell, tritiumCell, tritiumCell, tritiumCell, tritiumCell);
        CraftingManager.addShapelessAuto(new ItemStack(ModItems.cell, 9, Fluids.TRITIUM.getID()), ModBlocks.block_tritium);
    }

    private static void addColoredConcreteCrafts() {
        addConcreteColor("dyeWhite", ModBlocks.concrete_white);
        addConcreteColor("dyeOrange", ModBlocks.concrete_orange);
        addConcreteColor("dyeBlack", ModBlocks.concrete_black);
        addConcreteColor("dyeBlue", ModBlocks.concrete_blue);
        addConcreteColor("dyeBrown", ModBlocks.concrete_brown);
        addConcreteColor("dyeCyan", ModBlocks.concrete_cyan);
        addConcreteColor("dyeGray", ModBlocks.concrete_gray);
        addConcreteColor("dyeGreen", ModBlocks.concrete_green);
        addConcreteColor("dyeLightBlue", ModBlocks.concrete_light_blue);
        addConcreteColor("dyeLime", ModBlocks.concrete_lime);
        addConcreteColor("dyeMagenta", ModBlocks.concrete_magenta);
        addConcreteColor("dyePink", ModBlocks.concrete_pink);
        addConcreteColor("dyePurple", ModBlocks.concrete_purple);
        addConcreteColor("dyeLightGray", ModBlocks.concrete_silver);
        addConcreteColor("dyeRed", ModBlocks.concrete_red);
        addConcreteColor("dyeYellow", ModBlocks.concrete_yellow);
    }

    private static void addConcreteColor(String dye, net.minecraft.block.Block block) {
        CraftingManager.addShapelessAuto(new ItemStack(block, 8),
                ModBlocks.concrete_smooth, ModBlocks.concrete_smooth, ModBlocks.concrete_smooth, ModBlocks.concrete_smooth,
                ModBlocks.concrete_smooth, ModBlocks.concrete_smooth, ModBlocks.concrete_smooth, ModBlocks.concrete_smooth, dye);
        CraftingManager.addShapelessAuto(new ItemStack(ModBlocks.concrete_smooth), block);
    }
}