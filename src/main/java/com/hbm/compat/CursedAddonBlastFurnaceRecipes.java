package com.hbm.compat;

import com.hbm.config.GeneralConfig;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.util.Tuple;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

/** Legacy di-furnace recipes needed only by Cursed Addon's RTG furnace mixin. */
public final class CursedAddonBlastFurnaceRecipes {

    private static final List<Tuple.Triplet<Object, Object, ItemStack>> RECIPES = new ArrayList<>();

    static {
        addRecipe(IRON, COAL, new ItemStack(ModItems.ingot_steel));
        addRecipe(IRON, ANY_COKE, new ItemStack(ModItems.ingot_steel));
        addRecipe(IRON.ore(), COAL, new ItemStack(ModItems.ingot_steel, 2));
        addRecipe(IRON.ore(), ANY_COKE, new ItemStack(ModItems.ingot_steel, 3));
        addRecipe(IRON.ore(), new RecipesCommon.ComparableStack(ModItems.powder_flux), new ItemStack(ModItems.ingot_steel, 3));
        addRecipe(CU, REDSTONE, new ItemStack(ModItems.ingot_red_copper, 2));
        addRecipe(STEEL, MINGRADE, new ItemStack(ModItems.ingot_advanced_alloy, 2));
        addRecipe(W, COAL, new ItemStack(ModItems.neutron_reflector, 2));
        addRecipe(W, ANY_COKE, new ItemStack(ModItems.neutron_reflector, 2));
        addRecipe(new RecipesCommon.ComparableStack(ModItems.canister_full, 1, Fluids.GASOLINE.getID()), "slimeball", new ItemStack(ModItems.canister_napalm));
        addRecipe(W, SA326.nugget(), new ItemStack(ModItems.ingot_magnetized_tungsten));
        addRecipe(STEEL, TC99.nugget(), new ItemStack(ModItems.ingot_tcalloy));
        addRecipe(GOLD.plate(), ModItems.plate_mixed, new ItemStack(ModItems.plate_paa, 2));
        addRecipe(BIGMT, ModItems.ingot_meteorite, new ItemStack(ModItems.ingot_starmetal, 2));
        addRecipe(CO, ModItems.powder_meteorite, new ItemStack(ModItems.ingot_meteorite));
        addRecipe(ModItems.meteorite_sword_hardened, CO, new ItemStack(ModItems.meteorite_sword_alloyed));

        if (GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleChemsitry) {
            addRecipe(ModItems.canister_empty, COAL, new ItemStack(ModItems.canister_full, 1, Fluids.OIL.getID()));
        }
    }

    private CursedAddonBlastFurnaceRecipes() {
    }

    private static void addRecipe(Object in1, Object in2, ItemStack output) {
        if (in1 instanceof Item) in1 = new RecipesCommon.ComparableStack((Item) in1);
        if (in1 instanceof Block) in1 = new RecipesCommon.ComparableStack((Block) in1);
        if (in2 instanceof Item) in2 = new RecipesCommon.ComparableStack((Item) in2);
        if (in2 instanceof Block) in2 = new RecipesCommon.ComparableStack((Block) in2);
        RECIPES.add(new Tuple.Triplet<>(in1, in2, output));
    }

    public static Tuple.Triplet<Integer, Integer, ItemStack> getRequiredCounts(ItemStack in1, ItemStack in2) {
        if (in1.isEmpty() || in2.isEmpty()) return null;

        for (Tuple.Triplet<Object, Object, ItemStack> recipe : RECIPES) {
            RecipesCommon.AStack[] first = getRecipeStacks(recipe.getX());
            RecipesCommon.AStack[] second = getRecipeStacks(recipe.getY());

            RecipesCommon.AStack match1 = findMatching(first, in1);
            RecipesCommon.AStack match2 = findMatching(second, in2);
            if (match1 != null && match2 != null) {
                return new Tuple.Triplet<>(requiredCountFor(match1, in1), requiredCountFor(match2, in2), recipe.getZ().copy());
            }

            match1 = findMatching(first, in2);
            match2 = findMatching(second, in1);
            if (match1 != null && match2 != null) {
                return new Tuple.Triplet<>(requiredCountFor(match2, in1), requiredCountFor(match1, in2), recipe.getZ().copy());
            }
        }
        return null;
    }

    private static RecipesCommon.AStack findMatching(RecipesCommon.AStack[] recipe, ItemStack input) {
        for (RecipesCommon.AStack stack : recipe) {
            if (stack.matchesRecipe(input, true)) return stack;
        }
        return null;
    }

    private static int requiredCountFor(RecipesCommon.AStack definition, ItemStack input) {
        List<ItemStack> candidates = definition.extractForJEI();
        if (candidates.isEmpty()) return 1;
        for (ItemStack candidate : candidates) {
            if (OreDictionary.itemMatches(candidate, input, false)) return Math.max(1, candidate.getCount());
        }
        return Math.max(1, candidates.get(0).getCount());
    }

    private static RecipesCommon.AStack[] getRecipeStacks(Object input) {
        if (input instanceof DictFrame) {
            DictFrame frame = (DictFrame) input;
            return new RecipesCommon.AStack[]{
                    new RecipesCommon.OreDictStack(frame.ingot()),
                    new RecipesCommon.OreDictStack(frame.plate()),
                    new RecipesCommon.OreDictStack(frame.gem()),
                    new RecipesCommon.OreDictStack(frame.dust())
            };
        }
        if (input instanceof RecipesCommon.AStack) return new RecipesCommon.AStack[]{(RecipesCommon.AStack) input};
        if (input instanceof String) return new RecipesCommon.AStack[]{new RecipesCommon.OreDictStack((String) input)};
        return new RecipesCommon.AStack[0];
    }
}
