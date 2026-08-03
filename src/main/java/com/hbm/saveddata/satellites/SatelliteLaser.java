package com.hbm.saveddata.satellites;

import com.hbm.entity.logic.EntityDeathBlast;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.Locale;

public class SatelliteLaser extends Satellite {

	public static final String CMD_FIRE = "fire";
	public static final String CMD_CANFIRE = "canfire";
	public static final int CHARGE_TIME = 5 * 60 * 20;

	public long lastShot;
	
	public SatelliteLaser() {
		this.ifaceAcs.add(InterfaceActions.HAS_MAP);
		this.ifaceAcs.add(InterfaceActions.SHOW_COORDS);
		this.ifaceAcs.add(InterfaceActions.CAN_CLICK);
		this.satIface = Interfaces.SAT_PANEL;
	}
	
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("lastShot", lastShot);
	}
	
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		lastShot = nbt.getLong("lastShot");
	}

	@Override
	protected void onCommandImpl(World world, String... command) {
		if(command == null || command.length == 0) return;
		if(CMD_FIRE.equals(command[0])) {
			deathBlast(world, targetX, targetZ, null);
		} else if(CMD_CANFIRE.equals(command[0])) {
			tx = Boolean.toString(canFire(world)).toUpperCase(Locale.US);
		}
	}

	public void onClick(World world, EntityPlayerMP player, int x, int z) {
		setTarget(x, z);
		deathBlast(world, x, z, player);
	}

	public boolean canFire(World world) {
		return lastShot + CHARGE_TIME < world.getTotalWorldTime();
	}

	private void deathBlast(World world, int x, int z, EntityPlayerMP player) {
		if(world.isRemote || !canFire(world)) return;
		lastShot = world.getTotalWorldTime();

		EntityDeathBlast blast = new EntityDeathBlast(world);
		blast.setPosition(x, world.getHeight(x, z), z);
		blast.detonator = player;
		world.spawnEntity(blast);
	}

	@Override
	public float[] getColor() {
		return new float[] { 0.221F, 0.663F, 1.0F };
	}
}
