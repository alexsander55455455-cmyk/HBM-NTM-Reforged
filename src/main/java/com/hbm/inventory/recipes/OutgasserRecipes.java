package com.hbm.inventory.recipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ItemEnums;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.util.Tuple;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static com.hbm.inventory.OreDictManager.*;

public class OutgasserRecipes extends SerializableRecipe {

	public static Map<RecipesCommon.AStack, OutgasserRecipe> recipes = new HashMap();

	@Override
	public void registerDefaults() {

		/* lithium to tritium */
		recipes.put(new RecipesCommon.OreDictStack(LI.block()),		new OutgasserRecipe(null, new FluidStack(Fluids.TRITIUM, 10_000)));
		recipes.put(new RecipesCommon.OreDictStack(LI.ingot()),		new OutgasserRecipe(null, new FluidStack(Fluids.TRITIUM, 1_000)));
		recipes.put(new RecipesCommon.OreDictStack(LI.dust()),		new OutgasserRecipe(null, new FluidStack(Fluids.TRITIUM, 1_000)));
		recipes.put(new RecipesCommon.OreDictStack(LI.dustTiny()),	new OutgasserRecipe(null, new FluidStack(Fluids.TRITIUM, 100)));

		/* gold to gold-198 */
		recipes.put(new RecipesCommon.OreDictStack(GOLD.ingot()),		new OutgasserRecipe(new ItemStack(ModItems.ingot_au198), null));
		recipes.put(new RecipesCommon.OreDictStack(GOLD.nugget()),	new OutgasserRecipe(new ItemStack(ModItems.nugget_au198), null));
		recipes.put(new RecipesCommon.OreDictStack(GOLD.dust()),		new OutgasserRecipe(new ItemStack(ModItems.powder_au198), null));

		/* thorium to thorium fuel */
		recipes.put(new RecipesCommon.OreDictStack(TH232.ingot()),	new OutgasserRecipe(new ItemStack(ModItems.ingot_thorium_fuel), null));
		recipes.put(new RecipesCommon.OreDictStack(TH232.nugget()),	new OutgasserRecipe(new ItemStack(ModItems.nugget_thorium_fuel), null));
		recipes.put(new RecipesCommon.OreDictStack(TH232.billet()),	new OutgasserRecipe(new ItemStack(ModItems.billet_thorium_fuel), null));

		/* mushrooms to glowing mushrooms */
		recipes.put(new ComparableStack(Blocks.BROWN_MUSHROOM),	new OutgasserRecipe(new ItemStack(ModBlocks.mush), null));
		recipes.put(new ComparableStack(Blocks.RED_MUSHROOM),	new OutgasserRecipe(new ItemStack(ModBlocks.mush), null));
		recipes.put(new ComparableStack(Items.MUSHROOM_STEW),	new OutgasserRecipe(new ItemStack(ModItems.glowing_stew), null));

		recipes.put(new RecipesCommon.OreDictStack(COAL.gem()),		new OutgasserRecipe(DictFrame.fromOne(ModItems.oil_tar, ItemEnums.EnumTarType.COAL, 1), new FluidStack(Fluids.SYNGAS, 50)));
		recipes.put(new RecipesCommon.OreDictStack(COAL.dust()),		new OutgasserRecipe(DictFrame.fromOne(ModItems.oil_tar, ItemEnums.EnumTarType.COAL, 1), new FluidStack(Fluids.SYNGAS, 50)));
		recipes.put(new RecipesCommon.OreDictStack(COAL.block()),		new OutgasserRecipe(DictFrame.fromOne(ModItems.oil_tar, ItemEnums.EnumTarType.COAL, 9), new FluidStack(Fluids.SYNGAS, 500)));

		recipes.put(new ComparableStack(DictFrame.fromOne(ModItems.oil_tar, ItemEnums.EnumTarType.COAL)),	new OutgasserRecipe(null, new FluidStack(Fluids.COALOIL, 100)));
		recipes.put(new ComparableStack(DictFrame.fromOne(ModItems.oil_tar, ItemEnums.EnumTarType.WAX)),	new OutgasserRecipe(null, new FluidStack(Fluids.RADIOSOLVENT, 100)));
		/* EE outgasser parity */
		recipes.put(new RecipesCommon.OreDictStack(GOLD.dustTiny()), new OutgasserRecipe(new ItemStack(ModItems.powder_au198_tiny, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(U233.billet()), new OutgasserRecipe(new ItemStack(ModItems.billet_u235, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(U235.ingot()), new OutgasserRecipe(new ItemStack(ModItems.ingot_neptunium_fuel, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(NP237.billet()), new OutgasserRecipe(new ItemStack(ModItems.billet_pu238, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(PU239.billet()), new OutgasserRecipe(new ItemStack(ModItems.billet_pu240, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(PU240.billet()), new OutgasserRecipe(new ItemStack(ModItems.billet_pu241, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(PU241.billet()), new OutgasserRecipe(new ItemStack(ModItems.billet_am241, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(AM241.billet()), new OutgasserRecipe(new ItemStack(ModItems.billet_am242, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(SA326.billet()), new OutgasserRecipe(new ItemStack(ModItems.billet_solinium, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(CO.dustTiny()), new OutgasserRecipe(new ItemStack(ModItems.powder_co60_tiny, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(CO.ingot()), new OutgasserRecipe(new ItemStack(ModItems.ingot_co60, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(CO.dust()), new OutgasserRecipe(new ItemStack(ModItems.powder_co60, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(SR.nugget()), new OutgasserRecipe(new ItemStack(ModItems.nugget_sr90, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(SR.ingot()), new OutgasserRecipe(new ItemStack(ModItems.powder_sr90, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(SR.dust()), new OutgasserRecipe(new ItemStack(ModItems.powder_sr90, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(I.ingot()), new OutgasserRecipe(new ItemStack(ModItems.ingot_i131, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(I.dust()), new OutgasserRecipe(new ItemStack(ModItems.powder_i131, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(AC.nugget()), new OutgasserRecipe(new ItemStack(ModItems.nugget_ac227, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(AC.ingot()), new OutgasserRecipe(new ItemStack(ModItems.ingot_ac227, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(AC.dust()), new OutgasserRecipe(new ItemStack(ModItems.powder_ac227, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(CS.dust()), new OutgasserRecipe(new ItemStack(ModItems.powder_cs137, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(AT.dust()), new OutgasserRecipe(new ItemStack(ModItems.powder_at209, 1), null));
		recipes.put(new ComparableStack(ModItems.billet_australium), new OutgasserRecipe(new ItemStack(ModItems.billet_australium_lesser, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(PB.dustTiny()), new OutgasserRecipe(new ItemStack(ModItems.powder_pb209_tiny, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(PB.ingot()), new OutgasserRecipe(new ItemStack(ModItems.ingot_pb209, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(PB.dust()), new OutgasserRecipe(new ItemStack(ModItems.powder_pb209, 1), null));
		recipes.put(new RecipesCommon.OreDictStack(GOLD.block()), new OutgasserRecipe(new ItemStack(ModBlocks.block_au198, 1), null));
		recipes.put(new ComparableStack(ModItems.meteorite_sword_bred), new OutgasserRecipe(new ItemStack(ModItems.meteorite_sword_irradiated), null));

		recipes.put(new ComparableStack(ModBlocks.sand_gold), new OutgasserRecipe(new ItemStack(ModBlocks.sand_gold198, 1), null));
		recipes.put(new ComparableStack(ModItems.scrap), new OutgasserRecipe(new ItemStack(ModItems.fallout, 1), null));
		recipes.put(new ComparableStack(ModBlocks.block_scrap), new OutgasserRecipe(new ItemStack(ModBlocks.block_fallout, 1), null));
		recipes.put(new ComparableStack(Blocks.STONE), new OutgasserRecipe(new ItemStack(ModBlocks.sellafield_slaked, 1), null));
		recipes.put(new ComparableStack(ModBlocks.sellafield_slaked), new OutgasserRecipe(new ItemStack(ModBlocks.sellafield_0, 1), null));
		recipes.put(new ComparableStack(ModBlocks.sellafield_0), new OutgasserRecipe(new ItemStack(ModBlocks.sellafield_1, 1), null));
		recipes.put(new ComparableStack(ModBlocks.sellafield_1), new OutgasserRecipe(new ItemStack(ModBlocks.sellafield_2, 1), null));
		recipes.put(new ComparableStack(ModBlocks.sellafield_2), new OutgasserRecipe(new ItemStack(ModBlocks.sellafield_3, 1), null));
		recipes.put(new ComparableStack(ModBlocks.sellafield_3), new OutgasserRecipe(new ItemStack(ModBlocks.sellafield_4, 1), null));
		recipes.put(new ComparableStack(ModBlocks.sellafield_4), new OutgasserRecipe(new ItemStack(ModBlocks.sellafield_core, 1), null));
		recipes.put(new ComparableStack(ModBlocks.block_corium_cobble), new OutgasserRecipe(new ItemStack(ModBlocks.block_corium, 1), null));

	}

	public static OutgasserRecipe getRecipe(ItemStack input) {

		ComparableStack comp = new ComparableStack(input).makeSingular();

		if(recipes.containsKey(comp)) {
			return recipes.get(comp);
		}

		String[] dictKeys = comp.getDictKeys();

		for(String key : dictKeys) {
			RecipesCommon.OreDictStack dict = new RecipesCommon.OreDictStack(key);
			if(recipes.containsKey(dict)) {
				return recipes.get(dict);
			}
		}

		return null;
	}

	public static Tuple.Pair<ItemStack, FluidStack> getOutput(ItemStack input) {
		OutgasserRecipe recipe = getRecipe(input);
		if(recipe == null) return null;
		return new Tuple.Pair<>(recipe.solidOutput, recipe.fluidOutput);
	}

	public static HashMap<Object, Object[]> getRecipes() {

		HashMap<Object, Object[]> recipes = new HashMap<>();

		for(Entry<RecipesCommon.AStack, OutgasserRecipe> entry : OutgasserRecipes.recipes.entrySet()) {

			RecipesCommon.AStack input = entry.getKey();
			ItemStack solidOutput = entry.getValue().solidOutput;
			FluidStack fluidOutput = entry.getValue().fluidOutput;

			if(solidOutput != null && fluidOutput != null) recipes.put(input, new Object[] {solidOutput, ItemFluidIcon.make(fluidOutput)});
			if(solidOutput != null && fluidOutput == null) recipes.put(input, new Object[] {solidOutput});
			if(solidOutput == null && fluidOutput != null) recipes.put(input, new Object[] {ItemFluidIcon.make(fluidOutput)});
		}

		return recipes;
	}

	@Override
	public String getFileName() {
		return "hbmIrradiation.json";
	}

	@Override
	public Object getRecipeObject() {
		return recipes;
	}

	@Override
	public void readRecipe(JsonElement recipe) {
		JsonObject obj = (JsonObject) recipe;

		RecipesCommon.AStack input = readAStack(obj.get("input").getAsJsonArray());
		ItemStack solidOutput = null;
		FluidStack fluidOutput = null;

		if(obj.has("solidOutput")) {
			solidOutput = readItemStack(obj.get("solidOutput").getAsJsonArray());
		}

		if(obj.has("fluidOutput")) {
			fluidOutput = readFluidStack(obj.get("fluidOutput").getAsJsonArray());
		}

		OutgasserRecipe outgasserRecipe = new OutgasserRecipe(solidOutput, fluidOutput);
		if(obj.has("fusionOnly") && obj.get("fusionOnly").getAsBoolean()) {
			outgasserRecipe.fusionOnly();
		}

		if(solidOutput != null || fluidOutput != null) {
			recipes.put(input, outgasserRecipe);
		}
	}

	@Override
	public void writeRecipe(Object recipe, JsonWriter writer) throws IOException {
		Entry<RecipesCommon.AStack, OutgasserRecipe> rec = (Entry<RecipesCommon.AStack, OutgasserRecipe>) recipe;

		writer.name("input");
		writeAStack(rec.getKey(), writer);

		if(rec.getValue().solidOutput != null) {
			writer.name("solidOutput");
			writeItemStack(rec.getValue().solidOutput, writer);
		}

		if(rec.getValue().fluidOutput != null) {
			writer.name("fluidOutput");
			writeFluidStack(rec.getValue().fluidOutput, writer);
		}

		writer.name("fusionOnly").value(rec.getValue().fusionOnly);
	}

	@Override
	public void deleteRecipes() {
		recipes.clear();
	}

	public static class OutgasserRecipe {
		public final ItemStack solidOutput;
		public final FluidStack fluidOutput;
		public boolean fusionOnly = false;

		public OutgasserRecipe(ItemStack solidOutput, FluidStack fluidOutput) {
			this.solidOutput = solidOutput;
			this.fluidOutput = fluidOutput;
		}

		public OutgasserRecipe fusionOnly() {
			this.fusionOnly = true;
			return this;
		}
	}
}
