package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import static com.hbm.inventory.OreDictManager.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

public class DFCRecipes {

    public static LinkedHashMap<ComparableStack, Object[]> dfcRecipes = new LinkedHashMap<>();
    public static List<DFCRecipe> jeiDFCRecipes = null;

    public static void register() {
        jeiDFCRecipes = null;
        dfcRecipes.clear();

        DFCRecipes.setRecipe(100_000L, ModItems.marshmallow, new ItemStack(ModItems.marshmallow_roasted));

        DFCRecipes.setRecipe(2_000_000L, REDSTONE.dust(), new ItemStack(ModItems.nugget_mercury));
        DFCRecipes.setRecipe(2_000_000L, REDSTONE.block(), new ItemStack(ModItems.bottle_mercury));

        DFCRecipes.setRecipe(10_000_000L, W.dust(), new ItemStack(ModItems.powder_magnetized_tungsten));
        DFCRecipes.setRecipe(10_000_000L, W.ingot(), new ItemStack(ModItems.ingot_magnetized_tungsten));

        DFCRecipes.setRecipe(60_000_000L, MAGTUNG.dust(), new ItemStack(ModItems.powder_chlorophyte));
        DFCRecipes.setRecipe(60_000_000L, MAGTUNG.ingot(), new ItemStack(ModItems.powder_chlorophyte));

        DFCRecipes.setRecipe(200_000_000L, ModItems.powder_chlorophyte, new ItemStack(ModItems.powder_balefire));

        DFCRecipes.setRecipe(600_000_000L, ModItems.powder_balefire, new ItemStack(ModItems.egg_balefire_shard));

        DFCRecipes.setRecipe(800_000_000L, ModItems.billet_thorium_fuel, new ItemStack(ModItems.billet_zfb_bismuth));

        DFCRecipes.setRecipe(1_200_000_000L, Items.STICK, new ItemStack(Blocks.LOG));
        DFCRecipes.setRecipe(1_200_000_000L, Blocks.STONE, new ItemStack(Blocks.IRON_ORE));
        DFCRecipes.setRecipe(1_200_000_000L, Blocks.GRAVEL, new ItemStack(Blocks.COAL_ORE));
        DFCRecipes.setRecipe(1_200_000_000L, Blocks.NETHERRACK, new ItemStack(Blocks.QUARTZ_ORE));

        DFCRecipes.setRecipe(1_500_000_000L, ModItems.nugget_unobtainium_lesser, new ItemStack(ModItems.nugget_unobtainium_greater));

        DFCRecipes.setRecipe(2_000_000_000L, U.nugget(), new ItemStack(ModItems.nugget_schrabidium));
        DFCRecipes.setRecipe(2_000_000_000L, U.ingot(), new ItemStack(ModItems.ingot_schrabidium));
        DFCRecipes.setRecipe(2_000_000_000L, U.dust(), new ItemStack(ModItems.powder_schrabidium));

        DFCRecipes.setRecipe(2_500_000_000L, ModItems.powder_nitan_mix, new ItemStack(ModItems.powder_spark_mix));
        DFCRecipes.setRecipe(5_000_000_000L, ModItems.particle_hydrogen, new ItemStack(ModItems.particle_amat));

        DFCRecipes.setRecipe(20_000_000_000L, PU.nugget(), new ItemStack(ModItems.nugget_euphemium));
        DFCRecipes.setRecipe(20_000_000_000L, PU.ingot(), new ItemStack(ModItems.ingot_euphemium));
        DFCRecipes.setRecipe(20_000_000_000L, PU.dust(), new ItemStack(ModItems.powder_euphemium));

        DFCRecipes.setRecipe(30_000_000_000L, ModItems.particle_amat, new ItemStack(ModItems.particle_aschrab));

        DFCRecipes.setRecipe(40_000_000_000L, ModItems.nugget_verticium, new ItemStack(ModItems.nugget_radspice));
        DFCRecipes.setRecipe(40_000_000_000L, ModItems.ingot_verticium, new ItemStack(ModItems.ingot_radspice));
        DFCRecipes.setRecipe(40_000_000_000L, ModItems.powder_verticium, new ItemStack(ModItems.powder_radspice));

        DFCRecipes.setRecipe(50_000_000_000L, ModItems.billet_polonium, new ItemStack(ModItems.billet_yharonite));

        DFCRecipes.setRecipe(100_000_000_000L, ModItems.meteorite_sword_warped, new ItemStack(ModItems.meteorite_sword_demonic));

        DFCRecipes.setRecipe(200_000_000_000L, SBD.dust(), new ItemStack(ModItems.powder_dineutronium));
        DFCRecipes.setRecipe(200_000_000_000L, SBD.ingot(), new ItemStack(ModItems.ingot_dineutronium));

        DFCRecipes.setRecipe(400_000_000_000L, U238.ingot(), new ItemStack(ModItems.ingot_u238m2));
        DFCRecipes.setRecipe(420_000_000_000L, U238.nugget(), new ItemStack(ModItems.nugget_u238m2));
        DFCRecipes.setRecipe(690_000_000_000L, ModItems.gun_uboinik, new ItemStack(ModItems.gun_supershotgun));

        DFCRecipes.setRecipe(8_000_000_000_000L, ModItems.undefined, new ItemStack(ModItems.glitch));
        DFCRecipes.setRecipe(16_000_000_000_000L, ModItems.glitch, new ItemStack(ModItems.backpack_reality_error));
        DFCRecipes.setRecipe(69_000_000_000_000L, Items.WRITABLE_BOOK, new ItemStack(ModItems.book_of_));
    }

    public static void setRecipe(long requiredFlux, ItemStack in, ItemStack out) {
        dfcRecipes.put(new ComparableStack(in), new Object[]{requiredFlux, out});
    }

    public static void setRecipe(long requiredFlux, Item in, ItemStack out) {
        dfcRecipes.put(new ComparableStack(in), new Object[]{requiredFlux, out});
    }

    public static void setRecipe(long requiredFlux, Block in, ItemStack out) {
        dfcRecipes.put(new ComparableStack(in), new Object[]{requiredFlux, out});
    }

    public static void setRecipe(long requiredFlux, String in, ItemStack out) {
        dfcRecipes.put(new ComparableStack(OreDictionary.getOres(in).getFirst()), new Object[]{requiredFlux, out});
    }

    public static void removeRecipe(ItemStack in) {
        dfcRecipes.remove(new ComparableStack(in));
    }

    public static long getRequiredFlux(ItemStack stack) {

        if (stack == null || stack.isEmpty())
            return -1;

        ComparableStack comp = new ComparableStack(stack).makeSingular();
        if (dfcRecipes.containsKey(comp)) {
            return (long) dfcRecipes.get(comp)[0];
        }

        String[] dictKeys = comp.getDictKeys();

        for (String key : dictKeys) {
            if (dfcRecipes.containsKey(key)) {
                return (long) dfcRecipes.get(key)[1];
            }
        }
        return -1;
    }

    public static ItemStack getOutput(ItemStack stack) {
        if (stack == null) return ItemStack.EMPTY;

        ComparableStack comp = new ComparableStack(stack).makeSingular();
        if (dfcRecipes.containsKey(comp)) {
            return (ItemStack) dfcRecipes.get(comp)[1];
        }

        String[] dictKeys = comp.getDictKeys();

        for (String key : dictKeys) {
            if (dfcRecipes.containsKey(key)) {
                return (ItemStack) dfcRecipes.get(key)[1];
            }
        }
        return ItemStack.EMPTY;
    }

    public static List<DFCRecipe> getDFCRecipes() {
        if (jeiDFCRecipes == null) {
            jeiDFCRecipes = new ArrayList<>();
            for (Entry<ComparableStack, Object[]> e : dfcRecipes.entrySet()) {
                ItemStack input = e.getKey().toStack();
                ItemStack output = (ItemStack) e.getValue()[1];
                if (input.getItem() == ModItems.glitch && output.getItem() == ModItems.backpack_reality_error) continue;
                jeiDFCRecipes.add(new DFCRecipe(input, (long) e.getValue()[0], output));
            }
        }
        return jeiDFCRecipes;
    }

    public static class DFCRecipe implements IRecipeWrapper {

        private final ItemStack input;
        private final long requiredFlux;
        private final ItemStack output;

        public DFCRecipe(ItemStack input, long requiredFlux, ItemStack output) {
            this.input = input;
            this.requiredFlux = requiredFlux;
            this.output = output;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInput(VanillaTypes.ITEM, input);
            ingredients.setOutput(VanillaTypes.ITEM, output);
        }

        @Override
        public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
            FontRenderer fontRenderer = minecraft.fontRenderer;

            fontRenderer.drawString("Spark", 8, 8, 4210752);
            String number = Library.getShortNumber(requiredFlux);
            fontRenderer.drawString(number, 80 - fontRenderer.getStringWidth(number), 8, 0xa82a0e);
            GlStateManager.color(1, 1, 1, 1);
        }
    }
}
