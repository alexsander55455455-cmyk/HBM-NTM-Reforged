package com.hbm.inventory.recipes;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.imc.IMCBlastFurnace;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemCanister;
import com.hbm.main.MainRegistry;
import com.hbmspace.items.ModItemsSpace;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;
import static com.hbmspace.inventory.OreDictManagerSpace.NI;

public class BlastFurnaceRecipesNT extends GenericRecipes<GenericRecipe> {

    public static final BlastFurnaceRecipesNT INSTANCE = new BlastFurnaceRecipesNT();

    @Override public int inputItemLimit() { return 2; }
    @Override public int inputFluidLimit() { return 0; }
    @Override public int outputItemLimit() { return 2; }
    @Override public int outputFluidLimit() { return 0; }
    @Override public boolean hasPower() { return false; }

    @Override
    public GenericRecipe instantiateRecipe(String name) {
        return new GenericRecipe(name);
    }

    @Override
    public void registerDefaults() {
        this.register(new GenericRecipe("blast.steelFromIngot").setDuration(800)
                .inputItems(new OreDictStack(IRON.ingot(), 2), new OreDictStack(KEY_SAND))
                .outputItems(new ItemStack(ModItems.ingot_steel, 2), new ItemStack(ModItems.ingot_raw, 1, Mats.MAT_SLAG.id)));
        this.register(new GenericRecipe("blast.steelFromDust").setDuration(800)
                .inputItems(new OreDictStack(IRON.dust(), 2), new OreDictStack(KEY_SAND))
                .outputItems(new ItemStack(ModItems.ingot_steel, 2), new ItemStack(ModItems.ingot_raw, 1, Mats.MAT_SLAG.id)));
        this.register(new GenericRecipe("blast.steelFromOre").setDuration(800)
                .inputItems(new OreDictStack(IRON.ore()), new OreDictStack(KEY_SAND))
                .outputItems(new ItemStack(ModItems.ingot_steel, 2), new ItemStack(ModItems.ingot_raw, 2, Mats.MAT_SLAG.id)));
        this.register(new GenericRecipe("blast.steelWithFlux").setDuration(1_200)
                .inputItems(new OreDictStack(IRON.ore()), new ComparableStack(ModItems.powder_flux))
                .outputItems(new ItemStack(ModItems.ingot_steel, 3), new ItemStack(ModItems.ingot_raw, 2, Mats.MAT_SLAG.id)));

        this.register(new GenericRecipe("blast.mingrade").setDuration(400)
                .inputItems(new OreDictStack(CU.ingot()), new OreDictStack(REDSTONE.dust()))
                .outputItems(new ItemStack(ModItems.ingot_red_copper, 2)));
        this.register(new GenericRecipe("blast.mingradeDust").setDuration(400)
                .inputItems(new OreDictStack(CU.dust()), new OreDictStack(REDSTONE.dust()))
                .outputItems(new ItemStack(ModItems.ingot_red_copper, 2)));
        this.register(new GenericRecipe("blast.mingradeIngot").setDuration(400)
                .inputItems(new OreDictStack(CU.ingot()), new OreDictStack(REDSTONE.ingot()))
                .outputItems(new ItemStack(ModItems.ingot_red_copper, 2)));
        this.register(new GenericRecipe("blast.mingradeCursed").setDuration(400)
                .inputItems(new OreDictStack(CU.dust()), new OreDictStack(REDSTONE.ingot()))
                .outputItems(new ItemStack(ModItems.ingot_red_copper, 2)));
        this.register(new GenericRecipe("blast.mingradeOre").setDuration(1_200)
                .inputItems(new OreDictStack(CU.ore()), new OreDictStack(REDSTONE.dust(), 6))
                .outputItems(new ItemStack(ModItems.ingot_red_copper, 6), new ItemStack(ModItems.ingot_raw, 1, Mats.MAT_SLAG.id)));

        this.register(new GenericRecipe("blast.meteorSword").setDuration(1_200)
                .inputItems(new OreDictStack(CO.ingot()), new ComparableStack(ModItems.meteorite_sword_hardened, 1))
                .outputItems(new ItemStack(ModItems.meteorite_sword_alloyed, 1)));
        this.register(new GenericRecipe("blast.meteorSwordDust").setDuration(1_200)
                .inputItems(new OreDictStack(CO.dust()), new ComparableStack(ModItems.meteorite_sword_hardened, 1))
                .outputItems(new ItemStack(ModItems.meteorite_sword_alloyed, 1)));

        this.register(new GenericRecipe("blast.meteorite").setDuration(400)
                .inputItems(new ComparableStack(ModBlocks.block_meteor), new OreDictStack(CO.ingot()))
                .outputItems(new ItemStack(ModItems.ingot_meteorite, 1)));
        this.register(new GenericRecipe("blast.meteoriteDust").setDuration(400)
                .inputItems(new ComparableStack(ModBlocks.block_meteor), new OreDictStack(CO.dust()))
                .outputItems(new ItemStack(ModItems.ingot_meteorite, 1)));

        this.register(new GenericRecipe("blast.napalm").setDuration(400)
                .inputItems(new ComparableStack(ItemCanister.getStackFromFluid(Fluids.DIESEL)), new ComparableStack(Items.SLIME_BALL))
                .outputItems(new ItemStack(ModItems.canister_napalm, 1)));

        this.register(new GenericRecipe("blast.starmetal").setDuration(600)
                .inputItems(new OreDictStack(BIGMT.ingot()), new ComparableStack(ModItems.powder_meteorite, 1))
                .outputItems(new ItemStack(ModItems.ingot_starmetal, 1)));

        this.register(new GenericRecipe("blast.paa").setDuration(600)
                .inputItems(new OreDictStack(GOLD.ingot()), new ComparableStack(ModItems.plate_mixed, 1))
                .outputItems(new ItemStack(ModItems.plate_paa, 1)));

        this.register(new GenericRecipe("blast.firebrick").setDuration(800)
                .inputItems(new OreDictStack(AL.dust()), new ComparableStack(Items.CLAY_BALL, 7))
                .outputItems(new ItemStack(ModItems.ingot_firebrick, 8)));
        this.register(new GenericRecipe("blast.firebrickLimestone").setDuration(800)
                .inputItems(new OreDictStack(LIMESTONE.ore()), new ComparableStack(Items.CLAY_BALL, 6))
                .outputItems(new ItemStack(ModItems.ingot_firebrick, 8)));

        this.register(new GenericRecipe("blast.stainless").setDuration(400)
                .inputItems(new OreDictStack(STEEL.ingot()), new OreDictStack(NI.ingot()))
                .outputItems(new ItemStack(ModItemsSpace.ingot_stainless, 2)));

        if(!IMCBlastFurnace.buffer.isEmpty()) {
            for(GenericRecipe recipe : IMCBlastFurnace.buffer) this.register(recipe);
            MainRegistry.logger.info("Fetched {} IMC blast furnace recipes!", IMCBlastFurnace.buffer.size());
            IMCBlastFurnace.buffer.clear();
        }
    }

    @Override
    public String getFileName() {
        return "hbmBlastFurnace.json";
    }

    @Override
    public String getComment() {
        return "Generic blast furnace recipes for machine_blast_furnace. Duration is in ticks.";
    }

    public GenericRecipe getRecipe(ItemStack s0, ItemStack s1) {
        for(GenericRecipe recipe : this.recipeOrderedList) {
            if(recipe.inputItem.length == 1) {
                if(!s0.isEmpty() && s1.isEmpty() && recipe.inputItem[0].matchesRecipe(s0, false)) return recipe;
                if(s0.isEmpty() && !s1.isEmpty() && recipe.inputItem[0].matchesRecipe(s1, false)) return recipe;
            }
            if(recipe.inputItem.length == 2 && !s0.isEmpty() && !s1.isEmpty()) {
                if(recipe.inputItem[0].matchesRecipe(s0, true) && recipe.inputItem[1].matchesRecipe(s1, false)) return recipe;
                if(recipe.inputItem[1].matchesRecipe(s0, true) && recipe.inputItem[0].matchesRecipe(s1, false)) return recipe;
            }
        }
        return null;
    }
}
