package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.tool.ItemBackpack;
import com.hbm.util.BobMathUtil;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class ModuleMachineAssembler extends ModuleMachineBase {

    public ModuleMachineAssembler(int index, IEnergyHandlerMK2 battery, ItemStackHandler slots) {
        super(index, battery, slots);
        this.inputSlots = new int[12];
        this.outputSlots = new int[1];
        this.inputTanks = new FluidTankNTM[1];
        this.outputTanks = new FluidTankNTM[1];
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return AssemblyMachineRecipes.INSTANCE;
    }

    @Override
    public void setupTanks(GenericRecipe recipe) {
        super.setupTanks(recipe);
        if(recipe == null) return;
        for(int i = 0; i < inputTanks.length; i++) if(recipe.inputFluid != null && recipe.inputFluid.length > i) inputTanks[i].changeTankSize(BobMathUtil.max(inputTanks[i].getFill(), recipe.inputFluid[i].fill * 2, 4_000));
        for(int i = 0; i < outputTanks.length; i++) if(recipe.outputFluid != null && recipe.outputFluid.length > i) outputTanks[i].changeTankSize(BobMathUtil.max(outputTanks[i].getFill(), recipe.outputFluid[i].fill * 2, 4_000));
    }

    @Override
    protected boolean hasInput(GenericRecipe recipe) {
        if(recipe.inputItem == null) return super.hasInput(recipe);
        for(int i = 0; i < Math.min(recipe.inputItem.length, inputSlots.length); i++) {
            ItemStack stack = inventory.getStackInSlot(inputSlots[i]);
            if(stack.getItem() instanceof ItemBackpack && !((ItemBackpack) stack.getItem()).isEmptyForUpgrade(stack)) {
                return false;
            }
        }
        return super.hasInput(recipe);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if(stack.getItem() instanceof ItemBackpack && !((ItemBackpack) stack.getItem()).isEmptyForUpgrade(stack)) {
            return false;
        }
        return super.isItemValid(slot, stack);
    }

    public ModuleMachineAssembler itemInput(int from) { for(int i = 0; i < inputSlots.length; i++) inputSlots[i] = from + i; return this; }
    public ModuleMachineAssembler itemOutput(int a) { outputSlots[0] = a; return this; }
    public ModuleMachineAssembler fluidInput(FluidTankNTM a) { inputTanks[0] = a; return this; }
    public ModuleMachineAssembler fluidOutput(FluidTankNTM a) { outputTanks[0] = a; return this; }
}
