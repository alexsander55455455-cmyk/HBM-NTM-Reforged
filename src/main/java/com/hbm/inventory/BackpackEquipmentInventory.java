package com.hbm.inventory;

import com.hbm.capability.BackpackCapability;
import com.hbm.handler.BackpackHandler;
import com.hbm.items.tool.ItemBackpack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class BackpackEquipmentInventory implements IInventory {
    private final EntityPlayer player;

    public BackpackEquipmentInventory(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getStackInSlot(0).isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index == 0 ? BackpackCapability.getData(player).getEquippedBackpack() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index != 0 || count <= 0) return ItemStack.EMPTY;

        ItemStack stack = getStackInSlot(index);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack removed = stack.splitStack(Math.min(count, stack.getCount()));
        setEquippedBackpack(stack);
        return removed;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return decrStackSize(index, 1);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index != 0) return;
        setEquippedBackpack(stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.player == player;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && stack.getItem() instanceof ItemBackpack;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        setInventorySlotContents(0, ItemStack.EMPTY);
    }

    @Override
    public String getName() {
        return "container.hbm_backpack_slot";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }

    private void setEquippedBackpack(ItemStack stack) {
        if (player.world.isRemote) {
            BackpackCapability.getData(player).setEquippedBackpack(stack);
        } else {
            BackpackHandler.setEquippedBackpackFromContainer(player, stack);
        }
    }
}
