package com.hbm.tileentity.network;

import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.container.ContainerPneumoStorageImporter;
import com.hbm.inventory.gui.GUIPneumoStorageImporter;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

@AutoRegister
public class TileEntityPneumoStorageImporter extends TileEntityPneumaticMachineBase {

    private static final int[] SLOT_ACCESS = { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
    public final int[] delay = new int[9];

    public TileEntityPneumoStorageImporter() {
        super(9);
    }

    @Override
    protected ItemStackHandler getNewInventory(int slots, int slotLimit) {
        return new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                markDirty();
                if (delay != null && slot >= 0 && slot < delay.length) {
                    delay[slot] = Math.max(delay[slot], 1);
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return slotLimit;
            }
        };
    }

    @Override public String getDefaultName() { return "container.pneumoStorageImporter"; }
    @Override public int[] getAccessibleSlotsFromSide(EnumFacing side) { return SLOT_ACCESS.clone(); }

    @Override
    public void update() {
        super.update();
        if (world == null || world.isRemote || cache == null || cache.hasExpired) return;

        for (int i = 0; i < 9; i++) {
            if (delay[i] > 0) {
                delay[i]--;
                continue;
            }
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            int original = stack.getCount();
            int leftover = (int) cache.addItemsAndReturnQuantity(stack, original);
            if (leftover == original) {
                delay[i] = 100;
            } else {
                inventory.extractItem(i, original - leftover, false);
            }
        }
    }

    @Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerPneumoStorageImporter(player.inventory, this);
    }

    @Override @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIPneumoStorageImporter(player.inventory, this);
    }
}
