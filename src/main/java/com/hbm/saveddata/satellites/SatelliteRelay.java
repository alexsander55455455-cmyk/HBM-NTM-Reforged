package com.hbm.saveddata.satellites;

import com.hbm.main.AdvancementManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class SatelliteRelay extends Satellite {
	
	public SatelliteRelay() {
		this.satIface = Interfaces.NONE;
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
