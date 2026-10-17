package com.hbm.tileentity.machine;

import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.container.ContainerTapeDrive;
import com.hbm.inventory.gui.GUITapeDrive;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TileEntityProxyBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@AutoRegister
public class TileEntityMachineTapeDrive extends TileEntityMachineBase implements ITickable, IGUIProvider {
    public static final byte SLOT_EMPTY = 0, SLOT_ANY = 1, SLOT_EMPTY_TAPE = 2, SLOT_FILLED_TAPE = 3;
    public byte[] tapes = new byte[12];

    public TileEntityMachineTapeDrive() { super(12, 1); }
    @Override public String getDefaultName() { return "container.machineTapeDrive"; }

    @Override public void update() {
        if(world.isRemote) return;
        if(world.getTotalWorldTime() % 10 == 0) {
            TileEntity connected = world.getTileEntity(pos.offset(EnumFacing.byIndex(getBlockMetadata()).getOpposite()));
            if(connected instanceof TileEntityProxyBase) connected = ((TileEntityProxyBase) connected).getTE();
            if(connected instanceof TileEntityMachineSatLink && ((TileEntityMachineSatLink) connected).connected) {
                TileEntityMachineSatLink link = (TileEntityMachineSatLink) connected;
                SatelliteSavedData data = link.getSatelliteData();
                Satellite satellite = data.getSatFromFreq(link.getFrequency());
                if(satellite != null && satellite.hasData(world)) {
                    for(int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if(stack.isEmpty() || stack.getItem() != ModItems.drive) continue;
                        int metadata = stack.getItemDamage();
                        if(metadata < 0 || metadata >= EnumDriveType.values().length) continue;
                        EnumDriveType output = satellite.getOutputData(EnumDriveType.values()[metadata]);
                        if(output == null) continue;
                        satellite.consumeData();
                        inventory.setStackInSlot(i, new ItemStack(ModItems.drive, 1, output.ordinal()));
                        data.markSatelliteDirty();
                        markDirty();
                        break;
                    }
                }
            }
        }
        networkPackNT(50);
    }

    @Override public int[] getAccessibleSlotsFromSide(EnumFacing side) { return new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}; }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return stack.getItem() == ModItems.drive; }
    @Override public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return stack.getItemDamage() != EnumDriveType.DISK_EMPTY.ordinal() && stack.getItemDamage() != EnumDriveType.FLASH_EMPTY.ordinal();
    }
    @Override public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return super.getCapability(capability, capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == null ? EnumFacing.UP : facing);
    }

    @Override public void serialize(ByteBuf buf) {
        super.serialize(buf);
        for(int i = 0; i < tapes.length; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            byte type = SLOT_EMPTY;
            if(!stack.isEmpty()) {
                type = SLOT_ANY;
                if(stack.getItem() == ModItems.drive) {
                    int metadata = stack.getItemDamage();
                    if(metadata == EnumDriveType.DISK_EMPTY.ordinal() || metadata == EnumDriveType.FLASH_EMPTY.ordinal()) type = SLOT_EMPTY_TAPE;
                    else if(metadata >= 0 && metadata < EnumDriveType.values().length && metadata != EnumDriveType.DISK_BROKEN.ordinal() && metadata != EnumDriveType.FLASH_BROKEN.ordinal()) type = SLOT_FILLED_TAPE;
                }
            }
            buf.writeByte(type);
        }
    }
    @Override public void deserialize(ByteBuf buf) { super.deserialize(buf); for(int i = 0; i < tapes.length; i++) tapes[i] = buf.readByte(); }
    @Override public Container provideContainer(int id, EntityPlayer player, World world, int x, int y, int z) { return new ContainerTapeDrive(player.inventory, getCheckedInventory()); }
    @Override @SideOnly(Side.CLIENT) public net.minecraft.client.gui.GuiScreen provideGUI(int id, EntityPlayer player, World world, int x, int y, int z) { return new GUITapeDrive(player.inventory, this); }
}
