package com.hbm.inventory;

import com.hbm.items.tool.ItemBackpack;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackpackUpgradeOverflowTest {

    @BeforeAll
    static void registerVanillaItems() {
        if (!Bootstrap.isRegistered()) Bootstrap.register();
    }

    @Test
    void shrinkingCapacityReturnsEveryDisappearingStackAndKeepsEveryValidSlot() {
        ItemStack backpack = new ItemStack(Items.STICK);
        backpack.setTagCompound(new NBTTagCompound());
        NBTTagCompound inventory = new NBTTagCompound();
        inventory.setInteger("Size", 45);
        NBTTagList items = new NBTTagList();
        items.appendTag(stored(new ItemStack(Items.BONE, 7), 3));
        items.appendTag(stored(new ItemStack(Items.APPLE, 2), 35));
        items.appendTag(stored(new ItemStack(Items.PAPER, 11), 36));
        items.appendTag(stored(new ItemStack(Items.DIAMOND, 4), 44));
        inventory.setTag("Items", items);
        backpack.getTagCompound().setTag(ItemBackpack.INVENTORY_TAG, inventory);

        List<ItemStack> overflow = BackpackUpgradeManager.takeOverflowItems(backpack, 36);

        assertEquals(2, overflow.size());
        assertEquals(Items.PAPER, overflow.get(0).getItem());
        assertEquals(11, overflow.get(0).getCount());
        assertEquals(Items.DIAMOND, overflow.get(1).getItem());
        assertEquals(4, overflow.get(1).getCount());

        NBTTagCompound kept = backpack.getTagCompound().getCompoundTag(ItemBackpack.INVENTORY_TAG);
        assertEquals(36, kept.getInteger("Size"));
        NBTTagList keptItems = kept.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        assertEquals(2, keptItems.tagCount());
        assertEquals(3, keptItems.getCompoundTagAt(0).getInteger("Slot"));
        assertEquals(35, keptItems.getCompoundTagAt(1).getInteger("Slot"));
    }

    private static NBTTagCompound stored(ItemStack stack, int slot) {
        NBTTagCompound tag = stack.writeToNBT(new NBTTagCompound());
        tag.setInteger("Slot", slot);
        return tag;
    }
}
