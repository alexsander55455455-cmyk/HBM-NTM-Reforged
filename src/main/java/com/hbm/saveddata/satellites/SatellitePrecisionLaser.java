package com.hbm.saveddata.satellites;

import com.hbm.entity.logic.EntityOrbitalLaser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.Locale;

public class SatellitePrecisionLaser extends Satellite {

    public static final String CMD_FIRE = "fire";
    public static final String CMD_CANFIRE = "canfire";
    public static final String CMD_SETENTITYTARGET = "setentitytarget";
    public static final int MAX_TARGET_RANGE = 1_000;
    public static final int CHARGE_TIME = 5 * 20;

    private long lastShot;
    private int targetedEntity = -1;

    public SatellitePrecisionLaser() {
        coordAcs.add(CoordActions.HAS_Y);
        satIface = Interfaces.SAT_COORD;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setLong("lastShot", lastShot);
        nbt.setInteger("targetedEntity", targetedEntity);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        lastShot = nbt.getLong("lastShot");
        targetedEntity = nbt.hasKey("targetedEntity", 99) ? nbt.getInteger("targetedEntity") : -1;
    }

    @Override
    protected void onCommandImpl(World world, String... command) {
        if(command == null || command.length == 0) return;
        if(CMD_FIRE.equals(command[0])) {
            if(targetedEntity != -1) {
                Entity entity = world.getEntityByID(targetedEntity);
                targetedEntity = -1;
                if(entity == null || entity.isDead) return;

                double dx = entity.posX - targetX;
                double dz = entity.posZ - targetZ;
                if(dx * dx + dz * dz <= MAX_TARGET_RANGE * MAX_TARGET_RANGE) {
                    fire(world, entity.posX, entity.posY, entity.posZ);
                    return;
                }
            }
            fire(world, targetX + 0.5D, world.getHeight(targetX, targetZ), targetZ + 0.5D);
        } else if(CMD_CANFIRE.equals(command[0])) {
            tx = Boolean.toString(canFire(world)).toUpperCase(Locale.US);
        } else if(CMD_SETENTITYTARGET.equals(command[0]) && command.length == 2) {
            try {
                targetedEntity = Integer.parseInt(command[1]);
            } catch(NumberFormatException ignored) {
                targetedEntity = -1;
            }
        }
    }

    @Override
    public void onCoordAction(World world, EntityPlayerMP player, int x, int y, int z) {
        setTarget(x, z);
        int targetY = y > 0 ? y : world.getHeight(x, z);
        fire(world, x + 0.5D, targetY, z + 0.5D);
    }

    public boolean canFire(World world) {
        return lastShot + CHARGE_TIME < world.getTotalWorldTime();
    }

    private void fire(World world, double x, double y, double z) {
        if(world.isRemote || !canFire(world)) return;
        lastShot = world.getTotalWorldTime();
        EntityOrbitalLaser laser = new EntityOrbitalLaser(world);
        laser.setPosition(x, y, z);
        laser.explode();
        world.spawnEntity(laser);
    }

    @Override
    public float[] getColor() {
        return new float[] { 1F, 0.15F, 0.08F };
    }
}
