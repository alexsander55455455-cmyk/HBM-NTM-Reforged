package com.hbm.handler.imc;

import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.event.FMLInterModComms;

import java.util.ArrayList;

public class IMCBlastFurnace extends IMCHandler {

    public static final ArrayList<GenericRecipe> buffer = new ArrayList<>();
    private static int imcCounter;

    @Override
    public void process(FMLInterModComms.IMCMessage message) {
        final NBTTagCompound data = message.getNBTValue();
        final NBTTagCompound outputData = data.getCompoundTag("output");
        final ItemStack output = new ItemStack(outputData);

        if(output.isEmpty()) {
            printError(message, "Output stack could not be read!");
            return;
        }

        RecipesCommon.AStack input1 = readInput(data, "inputType1", "input1", message);
        if(input1 == null) return;
        RecipesCommon.AStack input2 = readInput(data, "inputType2", "input2", message);
        if(input2 == null) return;

        GenericRecipe recipe = new GenericRecipe("imc.blast." + (imcCounter++))
                .setDuration(800)
                .inputItems(input1, input2)
                .outputItems(output);
        buffer.add(recipe);
    }

    private RecipesCommon.AStack readInput(NBTTagCompound data, String typeKey, String valueKey, FMLInterModComms.IMCMessage message) {
        switch(data.getString(typeKey)) {
            case "ore":
                return new RecipesCommon.OreDictStack(data.getString(valueKey));
            case "orelist": {
                NBTTagList list = data.getTagList(valueKey, 8);
                if(list.tagCount() == 0) {
                    printError(message, "Ore list is empty!");
                    return null;
                }
                return new RecipesCommon.OreDictStack(list.getStringTagAt(0));
            }
            case "itemstack":
                return new RecipesCommon.ComparableStack(new ItemStack(data.getCompoundTag(valueKey)));
            default:
                printError(message, "Unhandled input type!");
                return null;
        }
    }
}