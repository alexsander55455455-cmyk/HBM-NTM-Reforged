package com.hbm.util;

import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.items.block.ItemBlockStorageCrate;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public final class CrateUtil {

    private CrateUtil() {
    }

    public static boolean isCrateItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        if (item instanceof ItemBlockStorageCrate) {
            return true;
        }

        if (item instanceof ItemBlock itemBlock) {
            Block block = itemBlock.getBlock();
            return block instanceof BlockStorageCrate;
        }

        return false;
    }
}