package com.hbm.saveddata.satellites;

import com.hbm.main.AdvancementManager;
import com.hbm.tileentity.network.RTTYSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class SatelliteRelay extends Satellite {
	public static final String CMD_RELAY = "relay";
	
	public SatelliteRelay() {
		this.satIface = Interfaces.NONE;
	}

	@Override
	protected void onCommandImpl(World world, String... command) {
		if(world == null || world.isRemote || command == null || command.length < 4
				|| !CMD_RELAY.equals(command[0])) return;
		int dimension;
		try {
			dimension = Integer.parseInt(command[1]);
		} catch(NumberFormatException ignored) {
			return;
		}
		World targetWorld = DimensionManager.getWorld(dimension, false);
		if(targetWorld == null) return;
		StringBuilder signal = new StringBuilder();
		for(int i = 3; i < command.length; i++) {
			if(i > 3) signal.append(" ");
			signal.append(command[i]);
		}
		RTTYSystem.broadcast(targetWorld, command[2], signal.toString());
	}

	public void onOrbit(World world, double x, double y, double z) {
		super.onOrbit(world, x, y, z);

		for(EntityPlayer p : world.playerEntities)
			AdvancementManager.grantAchievement(p, AdvancementManager.achFOEQ);
	}
	@Override
	public float[] getColor() {
		return new float[] { 1.0F, 0.15F, 0.15F };
	}

}
