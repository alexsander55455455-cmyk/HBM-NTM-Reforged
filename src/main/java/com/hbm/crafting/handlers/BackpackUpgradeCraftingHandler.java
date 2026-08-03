package com.hbm.crafting.handlers;

import com.hbm.items.tool.ItemBackpack;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * Prevents a crafting upgrade from silently deleting a backpack's contents.
 */
public final class BackpackUpgradeCraftingHandler extends ShapedOreRecipe {

    public BackpackUpgradeCraftingHandler(ResourceLocation group, ItemStack result, Object... recipe) {
        super(group, result, recipe);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        if (!super.matches(inventory, world)) return false;

        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof ItemBackpack backpack && !backpack.isEmptyForUpgrade(stack, world)) {
                return false;
            }
        }
        return true;
    }
}
