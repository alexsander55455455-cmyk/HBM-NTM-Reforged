package com.hbm.saveddata.satellites;

import com.hbm.items.ModItems;
import com.hbm.entity.missile.EntitySatellitePod;
import com.hbm.inventory.recipes.SpaceAssemblerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import net.minecraft.nbt.NBTTagList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public int assemblerCount;
    public double assemblerProgress;
    public final List<String> assemblerTasks = new ArrayList<>();
    public ItemStack[] requestableSlots = new ItemStack[0];

    public static boolean isModule(ItemStack stack) {
        return isSensor(stack) || (stack != null && !stack.isEmpty() && stack.getItem() == ModItems.satellite
                && stack.getMetadata() == EnumSatelliteType.SCIENCE_ASSEMBLER.ordinal());
    }

    public static boolean isDeliveryPayload(ItemStack stack) {
        return isModule(stack) || (stack != null && !stack.isEmpty() && stack.getItem() == ModItems.orbital_assembly
                && SpaceAssemblerRecipes.INSTANCE.getRecipe(stack) != null);
    }

    public static boolean isSensor(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == ModItems.satellite
                && stack.getMetadata() == EnumSatelliteType.SCIENCE_SENSOR.ordinal();
    }

    @Override
    public boolean onPartDelivered(World world, ItemStack part) {
        if(world.isRemote) return false;
        if(isSensor(part)) {
            if(sensorCount == Integer.MAX_VALUE) return false;
            sensorCount++;
            return true;
        }
        if(isModule(part)) {
            if(assemblerCount == Integer.MAX_VALUE) return false;
            assemblerCount++;
            return true;
        }
        if(!isDeliveryPayload(part)) return false;
        GenericRecipe recipe = SpaceAssemblerRecipes.INSTANCE.getRecipe(part);
        assemblerTasks.add(recipe.getInternalName());
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

    public boolean tickLaboratory() {
        boolean changed = tickSensors();
        return tickAssembly() || changed;
    }

    public boolean tickAssembly() {
        if(assemblerCount <= 0 || requestableSlots.length > 0 || assemblerTasks.isEmpty()) return false;
        GenericRecipe recipe = SpaceAssemblerRecipes.INSTANCE.recipeNameMap.get(assemblerTasks.get(0));
        // Keep orders if a custom recipe is temporarily unavailable.
        if(recipe == null) return false;
        assemblerProgress += (double) assemblerCount / Math.max(1, recipe.duration);
        if(assemblerProgress + 1E-12 >= 1) {
            requestableSlots = new ItemStack[recipe.outputItem == null ? 0 : recipe.outputItem.length];
            for(int i = 0; i < requestableSlots.length; i++) {
                ItemStack output = recipe.outputItem[i].collapse();
                requestableSlots[i] = output == null ? ItemStack.EMPTY : output;
            }
            assemblerProgress = 0;
            assemblerTasks.remove(0);
        }
        return true;
    }

    public boolean tryRequestItems(World world, int x, int y, int z) {
        if(world.isRemote || requestableSlots.length == 0) return false;
        EntitySatellitePod pod = new EntitySatellitePod(world).setup(y, requestableSlots);
        pod.setPosition(x + 0.5, 300, z + 0.5);
        if(!world.spawnEntity(pod)) return false;
        requestableSlots = new ItemStack[0];
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
        nbt.setInteger("assemblerCount", assemblerCount);
        nbt.setDouble("assemblerProgress", assemblerProgress);
        nbt.setInteger("taskCount", assemblerTasks.size());
        for(int i = 0; i < assemblerTasks.size(); i++) nbt.setString("task" + i, assemblerTasks.get(i));
        nbt.setInteger("itemCount", requestableSlots.length);
        NBTTagList items = new NBTTagList();
        for(int i = 0; i < requestableSlots.length; i++) {
            if(requestableSlots[i].isEmpty()) continue;
            NBTTagCompound item = requestableSlots[i].writeToNBT(new NBTTagCompound());
            item.setByte("slot", (byte) i);
            items.appendTag(item);
        }
        nbt.setTag("requestableSlots", items);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        lastScience = nbt.getLong("lastScience");
        sensorProgress = Math.max(0, Math.min(SENSOR_DURATION, nbt.getInteger("sensorProgress")));
        sensorCount = Math.max(0, nbt.getInteger("sensorCount"));
        assemblerCount = Math.max(0, nbt.getInteger("assemblerCount"));
        double progress = nbt.getDouble("assemblerProgress");
        assemblerProgress = Double.isFinite(progress) ? Math.max(0, Math.min(1, progress)) : 0;
        assemblerTasks.clear();
        for(int i = 0; i < nbt.getInteger("taskCount"); i++) assemblerTasks.add(nbt.getString("task" + i));
        requestableSlots = new ItemStack[Math.max(0, Math.min(256, nbt.getInteger("itemCount")))];
        Arrays.fill(requestableSlots, ItemStack.EMPTY);
        NBTTagList items = nbt.getTagList("requestableSlots", 10);
        for(int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound item = items.getCompoundTagAt(i);
            int slot = item.getByte("slot") & 255;
            if(slot < requestableSlots.length) requestableSlots[slot] = new ItemStack(item);
        }
    }

    @Override
    public float[] getColor() { return new float[] { 0.25F, 0.75F, 1.0F }; }
}
