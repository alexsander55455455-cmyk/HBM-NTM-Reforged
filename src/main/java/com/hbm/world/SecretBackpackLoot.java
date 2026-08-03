package com.hbm.world;

import com.hbm.blocks.generic.BlockLoot;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.util.Tuple.Quartet;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.List;
import java.util.Random;

/**
 * Local, explicit blueprint rewards for secret backpacks.
 *
 * These helpers deliberately do not register the blueprints in shared loot pools:
 * each caller owns its chance and its one-time persistence.
 */
public final class SecretBackpackLoot {

    public static final String STALKER = GenericRecipes.POOL_PREFIX_SECRET + "backpack_stalker";
    public static final String BLACK_BOX = GenericRecipes.POOL_PREFIX_SECRET + "backpack_black_box";
    public static final String ASH = GenericRecipes.POOL_PREFIX_SECRET + "backpack_ash";
    public static final String NUCLEAR_TOURIST = GenericRecipes.POOL_PREFIX_SECRET + "backpack_nuclear_tourist";
    public static final String POCKET_HOLE = GenericRecipes.POOL_PREFIX_SECRET + "backpack_pocket_hole";
    public static final String SAPPER = GenericRecipes.POOL_PREFIX_SECRET + "backpack_sapper";
    public static final String SMUGGLER = GenericRecipes.POOL_PREFIX_SECRET + "backpack_smuggler";

    private SecretBackpackLoot() {
    }

    public static boolean roll(Random rand, int denominator) {
        return denominator > 0 && (denominator == 1 || rand.nextInt(denominator) == 0);
    }

    /**
     * Inserts the requested blueprint without replacing existing loot.
     *
     * @return true when the blueprint already existed or was inserted successfully.
     */
    public static boolean insertBlueprint(World world, BlockPos pos, String pool) {
        if (world == null || pos == null || !isSecretPool(pool)) return false;

        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) return false;

        boolean inserted = insertBlueprint(tile, pool);
        if (inserted) {
            tile.markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
        return inserted;
    }

    public static boolean insertBlueprint(TileEntity tile, String pool) {
        if (tile == null || !isSecretPool(pool)) return false;

        if (tile instanceof BlockLoot.TileEntityLoot loot) {
            for (Quartet<ItemStack, Double, Double, Double> entry : loot.items) {
                if (entry != null && isBlueprint(entry.getW(), pool)) return true;
            }
            loot.addItem(ItemBlueprints.make(pool), 0D, 0.03125D, 0D);
            return true;
        }

        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (handler instanceof IItemHandlerModifiable modifiable) {
            return insertBlueprint(modifiable, pool);
        }
        if (tile instanceof IInventory inventory) {
            return insertBlueprint(inventory, pool);
        }
        return false;
    }

    public static boolean insertBlueprint(IInventory inventory, String pool) {
        if (inventory == null || !isSecretPool(pool)) return false;

        int emptySlot = -1;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isBlueprint(stack, pool)) return true;
            if (emptySlot < 0 && (stack == null || stack.isEmpty())) emptySlot = slot;
        }
        if (emptySlot < 0) return false;

        inventory.setInventorySlotContents(emptySlot, ItemBlueprints.make(pool));
        inventory.markDirty();
        return true;
    }

    public static boolean insertBlueprint(IItemHandlerModifiable inventory, String pool) {
        if (inventory == null || !isSecretPool(pool)) return false;

        int emptySlot = -1;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isBlueprint(stack, pool)) return true;
            if (emptySlot < 0 && (stack == null || stack.isEmpty())) emptySlot = slot;
        }
        if (emptySlot < 0) return false;

        inventory.setStackInSlot(emptySlot, ItemBlueprints.make(pool));
        return true;
    }

    public static boolean insertBlueprint(List<ItemStack> items, String pool) {
        if (items == null || !isSecretPool(pool)) return false;

        for (ItemStack stack : items) {
            if (isBlueprint(stack, pool)) return true;
        }
        items.add(ItemBlueprints.make(pool));
        return true;
    }

    private static boolean isSecretPool(String pool) {
        return pool != null && pool.startsWith(GenericRecipes.POOL_PREFIX_SECRET);
    }

    private static boolean isBlueprint(ItemStack stack, String pool) {
        return pool.equals(ItemBlueprints.grabPool(stack));
    }
}
