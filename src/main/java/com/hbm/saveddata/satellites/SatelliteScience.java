package com.hbm.saveddata.satellites;

import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.items.machine.ItemSatellitePayload.EnumSatelliteType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class SatelliteScience extends Satellite {
    public static final int COOLDOWN = 15 * 60 * 20;
    public long lastScience;
    public static final int SENSOR_DURATION = 100 * 60 * 60 * 20;
    public int sensorProgress;
    public int sensorCount;

    public static boolean isSensor(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == ModItems.satellite
                && stack.getMetadata() == EnumSatelliteType.SCIENCE_SENSOR.ordinal();
    }

    @Override
    public boolean onPartDelivered(World world, ItemStack part) {
        if(world.isRemote || !isSensor(part) || sensorCount == Integer.MAX_VALUE) return false;
        sensorCount++;
        return true;
    }

    /** Called once per server tick by the central save; true means persistent state changed. */
    public boolean tickSensors() {
        if(sensorCount <= 0) return false;
        if(sensorProgress < SENSOR_DURATION) {
            sensorProgress = (int) Math.min(SENSOR_DURATION, (long) sensorProgress + sensorCount);
            return true;
        }
        // Keep a completed sample until the previous flight/orbit record is collected.
        if(driveInput != null || driveOutput != null) return false;
        sensorProgress = 0;
        produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_ORBITDATA);
        return true;
    }

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
        nbt.setInteger("sensorProgress", sensorProgress);
        nbt.setInteger("sensorCount", sensorCount);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        lastScience = nbt.getLong("lastScience");
        sensorProgress = Math.max(0, Math.min(SENSOR_DURATION, nbt.getInteger("sensorProgress")));
        sensorCount = Math.max(0, nbt.getInteger("sensorCount"));
    }

    @Override
    public float[] getColor() { return new float[] { 0.25F, 0.75F, 1.0F }; }
}
