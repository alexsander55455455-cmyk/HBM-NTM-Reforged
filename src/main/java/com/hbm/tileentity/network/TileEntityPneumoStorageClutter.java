package com.hbm.tileentity.network;

import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.container.ContainerPneumoStorageClutter;
import com.hbm.inventory.gui.GUIPneumoStorageClutter;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@AutoRegister
public class TileEntityPneumoStorageClutter extends TileEntityPneumaticStorageBase {

    public TileEntityPneumoStorageClutter() {
        super(6 * 9);
    }

    @Override public String getDefaultName() { return "container.pneumoStorageClutter"; }
    @Override public long getAmountAt(int index) { return getSlotAt(index).isEmpty() ? 0 : getSlotAt(index).getCount(); }
    @Override public boolean allowTypeSetting() { return true; }

    @Override
    public long useUpItem(int index, long amount) {
        if (amount <= 0) return 0;
        ItemStack stack = inventory.getStackInSlot(index);
        if (stack.isEmpty()) return amount;
        int remove = (int) Math.min(amount, stack.getCount());
        inventory.extractItem(index, remove, false);
        return amount - remove;
    }

    @Override
    public long addItem(int index, long amount) {
        if (amount <= 0) return 0;
        ItemStack stack = inventory.getStackInSlot(index);
        if (stack.isEmpty()) return amount;
        int capacity = Math.min(stack.getMaxStackSize(), inventory.getSlotLimit(index)) - stack.getCount();
        int add = (int) Math.min(amount, Math.max(0, capacity));
        if (add > 0) {
            ItemStack updated = stack.copy();
            updated.grow(add);
            inventory.setStackInSlot(index, updated);
        }
        return amount - add;
    }

    @Override
    public long setupType(int index, ItemStack zeroStack, long amount) {
        if (zeroStack == null || zeroStack.isEmpty() || amount <= 0) return amount;
        int count = (int) Math.min(amount, Math.min(zeroStack.getMaxStackSize(), inventory.getSlotLimit(index)));
        ItemStack inserted = zeroStack.copy();
        inserted.setCount(count);
        inventory.setStackInSlot(index, inserted);
        return amount - count;
    }

    @Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerPneumoStorageClutter(player.inventory, this);
    }

    @Override @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIPneumoStorageClutter(player.inventory, this);
    }
}
