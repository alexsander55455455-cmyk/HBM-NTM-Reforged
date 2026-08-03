package com.hbm.tileentity.network;

import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.container.ContainerPneumoStorageMono;
import com.hbm.inventory.gui.GUIPneumoStorageMono;
import com.hbm.tileentity.IControlReceiverFilter;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@AutoRegister
public class TileEntityPneumoStorageMono extends TileEntityPneumaticStorageBase implements IControlReceiverFilter {

    public static final int CAPACITY = 100_000;
    public int[] amounts = new int[3];

    public TileEntityPneumoStorageMono() {
        super(3);
    }

    @Override public String getDefaultName() { return "container.pneumoStorageMono"; }
    @Override public long getAmountAt(int index) { return validIndex(index) ? amounts[index] : 0; }
    @Override public boolean allowTypeSetting() { return false; }

    @Override
    public long useUpItem(int index, long amount) {
        if (!validIndex(index) || amount <= 0 || amounts[index] <= 0) return Math.max(0, amount);
        int removed = (int) Math.min(amount, amounts[index]);
        amounts[index] -= removed;
        markDirty();
        dataChanged();
        return amount - removed;
    }

    @Override
    public long addItem(int index, long amount) {
        if (!validIndex(index) || amount <= 0) return Math.max(0, amount);
        int added = (int) Math.min(amount, CAPACITY - amounts[index]);
        amounts[index] += added;
        if (added > 0) {
            markDirty();
            dataChanged();
        }
        return amount - added;
    }

    @Override public long setupType(int index, ItemStack zeroStack, long amount) { return amount; }

    @Override
    public void receiveControl(NBTTagCompound data) {
        super.receiveControl(data);
        if (data.hasKey("slot")) setFilterContents(data);
    }

    @Override public int[] getFilterSlots() { return new int[] { 0, 3 }; }
    @Override public void nextMode(int i) { }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        for (int amount : amounts) buf.writeInt(amount);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        for (int i = 0; i < amounts.length; i++) amounts[i] = buf.readInt();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        amounts = sanitizeAmounts(nbt.getIntArray("amounts"));
    }

    static int[] sanitizeAmounts(int[] stored) {
        int[] sanitized = Arrays.copyOf(stored, 3);
        for (int i = 0; i < sanitized.length; i++) sanitized[i] = Math.max(0, Math.min(CAPACITY, sanitized[i]));
        return sanitized;
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setIntArray("amounts", Arrays.copyOf(amounts, 3));
        return super.writeToNBT(nbt);
    }

    private boolean validIndex(int index) { return index >= 0 && index < amounts.length; }

    @Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerPneumoStorageMono(player.inventory, this);
    }

    @Override @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIPneumoStorageMono(player.inventory, this);
    }
}
