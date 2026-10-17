package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemDrive.EnumDriveType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class SatelliteScience extends Satellite {
    public static final int COOLDOWN = 15 * 60 * 20;
    public long lastScience;

    @Override
    public boolean hasData(World world) {
        if(super.hasData(world)) return true;
        if(!world.isRemote && world.getTotalWorldTime() > lastScience + COOLDOWN) {
            produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_FLIGHTDATA);
            lastScience = world.getTotalWorldTime();
            // All orbit views share the central save, including explicit remote chips.
            SatelliteSavedData.getData(world).markSatelliteDirty();
        }
        return super.hasData(world);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setLong("lastScience", lastScience);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        lastScience = nbt.getLong("lastScience");
    }

    @Override
    public float[] getColor() { return new float[] { 0.25F, 0.75F, 1.0F }; }
}
