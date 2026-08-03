package com.hbm.tileentity.machine;

import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.container.ContainerBlackBoxRecovery;
import com.hbm.inventory.gui.GUIBlackBoxRecovery;
import com.hbm.items.tool.ItemBlackBoxBackpack;
import com.hbm.tileentity.IGUIProvider;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;

/**
 * Persistent one-item store for a Black Box backpack. It deliberately exposes
 * no item-handler capability, so automation cannot insert, replace, or extract
 * the recovered backpack.
 */
@AutoRegister
public final class TileEntityBlackBoxRecovery extends TileEntity implements IGUIProvider {

    private static final String BACKPACK_TAG = "Backpack";
    private final RecoveryInventory inventory = new RecoveryInventory();

    /**
     * Transfers ownership of one Black Box backpack into this tile.
     *
     * <p>On success the supplied stack is consumed. Callers must pass the
     * authoritative stack removed from the player's equipment, not a copy.</p>
     */
    public synchronized boolean acceptBackpack(ItemStack backpack) {
        if (world == null || world.isRemote
                || backpack.isEmpty()
                || backpack.getCount() != 1
                || !(backpack.getItem() instanceof ItemBlackBoxBackpack)
                || inventory.hasBackpack()) {
            return false;
        }

        inventory.storeTransferred(backpack);
        return true;
    }

    public synchronized boolean hasBackpack() {
        return inventory.hasBackpack();
    }

    /**
     * Used exclusively by the block before its tile entity is invalidated.
     * Clearing first ensures repeated block-removal callbacks cannot duplicate
     * the stored backpack.
     */
    public synchronized ItemStack takeBackpackForBlockRemoval() {
        return inventory.extractItem(0, 1, false);
    }

    public IItemHandlerModifiable getInventoryForContainer() {
        return inventory;
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        if (world == null || world.getTileEntity(pos) != this) {
            return false;
        }
        ItemStack backpack = inventory.getStackInSlot(0);
        if (backpack.isEmpty()
                || !(backpack.getItem() instanceof ItemBlackBoxBackpack blackBox)
                || !blackBox.canAccess(backpack, player)) {
            return false;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        return player.getDistanceSq(x, y, z) <= 64.0D;
    }

    public void removeIfEmpty() {
        if (world != null && !world.isRemote && !hasBackpack() && world.getTileEntity(pos) == this) {
            world.setBlockToAir(pos);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        ItemStack loaded = ItemStack.EMPTY;
        if (nbt.hasKey(BACKPACK_TAG, Constants.NBT.TAG_COMPOUND)) {
            loaded = new ItemStack(nbt.getCompoundTag(BACKPACK_TAG));
        }
        inventory.load(loaded);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        ItemStack backpack = inventory.getStackInSlot(0);
        if (!backpack.isEmpty()) {
            nbt.setTag(BACKPACK_TAG, backpack.writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerBlackBoxRecovery(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIBlackBoxRecovery(player.inventory, this);
    }

    private final class RecoveryInventory implements IItemHandlerModifiable {

        private ItemStack stored = ItemStack.EMPTY;

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        @Nonnull
        public synchronized ItemStack getStackInSlot(int slot) {
            validateSlot(slot);
            return stored;
        }

        @Override
        @Nonnull
        public synchronized ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            validateSlot(slot);
            return stack;
        }

        @Override
        @Nonnull
        public synchronized ItemStack extractItem(int slot, int amount, boolean simulate) {
            validateSlot(slot);
            if (amount <= 0 || stored.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack extracted = stored.copy();
            if (!simulate) {
                stored = ItemStack.EMPTY;
                changed();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            validateSlot(slot);
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            validateSlot(slot);
            return false;
        }

        /**
         * Container synchronization needs a modifiable handler on the client.
         * The handler is not exposed as a capability, and normal slot insertion
         * is rejected by both isItemValid and insertItem.
         */
        @Override
        public synchronized void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            validateSlot(slot);
            if (stack.isEmpty()) {
                stored = ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemBlackBoxBackpack) {
                stored = stack.copy();
                stored.setCount(1);
            } else {
                stored = ItemStack.EMPTY;
            }
            changed();
        }

        private synchronized boolean hasBackpack() {
            return !stored.isEmpty();
        }

        private synchronized void storeTransferred(ItemStack source) {
            stored = source.copy();
            stored.setCount(1);
            source.shrink(1);
            changed();
        }

        private synchronized void load(ItemStack stack) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemBlackBoxBackpack) {
                stored = stack.copy();
                stored.setCount(1);
            } else {
                stored = ItemStack.EMPTY;
            }
        }

        private void changed() {
            TileEntityBlackBoxRecovery.this.markDirty();
        }

        private void validateSlot(int slot) {
            if (slot != 0) {
                throw new RuntimeException("Black Box recovery inventory has only slot 0");
            }
        }
    }
}
