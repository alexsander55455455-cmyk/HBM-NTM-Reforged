package com.hbm.saveddata.satellites;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionHandler.PollutionData;
import com.hbm.handler.pollution.PollutionHandler.PollutionType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SatelliteMapper extends Satellite {

	public static final String CMD_TARGET_LOADED = "targetloaded";
	public static final String CMD_GETSMOG = "getsmog";
	public static final String CMD_SPOT_PLAYER = "spotplayers";
	public static final int SPOT_PLAYER_MAX_RANGE = 250;

	public SatelliteMapper() {
		this.ifaceAcs.add(InterfaceActions.HAS_MAP);
		this.satIface = Interfaces.SAT_PANEL;
	}

	@Override
	protected void onCommandImpl(World world, String... command) {
		if(command == null || command.length == 0) return;
		if(CMD_TARGET_LOADED.equals(command[0])) {
			tx = Boolean.toString(world.isBlockLoaded(new BlockPos(targetX, 0, targetZ)))
					.toUpperCase(Locale.US);
		} else if(CMD_GETSMOG.equals(command[0])) {
			PollutionData data = PollutionHandler.getPollutionData(world, new BlockPos(targetX, 255, targetZ));
			float soot = data == null ? 0F : data.pollution[PollutionType.SOOT.ordinal()];
			tx = Integer.toString((int) Math.ceil(soot));
		} else if(CMD_SPOT_PLAYER.equals(command[0])) {
			List<String> names = new ArrayList<>();
			for(EntityPlayer player : world.playerEntities) {
				double dx = Math.floor(player.posX) - targetX;
				double dz = Math.floor(player.posZ) - targetZ;
				if(dx * dx + dz * dz > SPOT_PLAYER_MAX_RANGE * SPOT_PLAYER_MAX_RANGE) continue;
				int height = world.getHeight((int) Math.floor(player.posX), (int) Math.floor(player.posZ));
				if(height < player.posY + 2D) names.add(player.getName());
			}
			tx = names.isEmpty() ? "NONE" : String.join(";", names);
		}
	}

	@Override
	public float[] getColor() {
		return new float[] { 0.538F, 1.0F, 0.523F };
	}
}
