package com.hbm.saveddata.satellites;

import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SatelliteRadar extends Satellite {

	public static final int MAX_SCAN_RANGE = 1_000;
	public static final String CMD_SURVEY = "survey";
	public static final String CMD_FILTER = "filter";
	public static final String CMD_COUNT = "count";
	public static final String CMD_GETTARGETID = "gettargetid";
	public static final String CMD_GETPOSITION = "getposition";
	public static final String CMD_GETNAME = "getname";

	private final List<Entity> cachedRadarResults = new ArrayList<>();
	private final List<Entity> filteredRadarResults = new ArrayList<>();

	public SatelliteRadar() {
		this.ifaceAcs.add(InterfaceActions.HAS_MAP);
		this.ifaceAcs.add(InterfaceActions.HAS_RADAR);
		this.satIface = Interfaces.SAT_PANEL;
	}

	@Override
	protected void onCommandImpl(World world, String... command) {
		if(command == null || command.length == 0) return;
		if(CMD_SURVEY.equals(command[0])) {
			cachedRadarResults.clear();
			for(Entity entity : TileEntityMachineRadarNT.matchingEntities) {
				if(entity == null || entity.isDead || entity.dimension != world.provider.getDimension()) continue;
				double dx = Math.floor(entity.posX) - targetX;
				double dz = Math.floor(entity.posZ) - targetZ;
				if(dx * dx + dz * dz <= MAX_SCAN_RANGE * MAX_SCAN_RANGE) cachedRadarResults.add(entity);
			}
			filteredRadarResults.clear();
			filteredRadarResults.addAll(cachedRadarResults);
		} else if(CMD_FILTER.equals(command[0]) && command.length == 2) {
			filteredRadarResults.clear();
			String filter = command[1].toLowerCase(Locale.US);
			for(Entity entity : cachedRadarResults) {
				if(entity != null && !entity.isDead
						&& entity.getClass().getSimpleName().toLowerCase(Locale.US).contains(filter)) {
					filteredRadarResults.add(entity);
				}
			}
		} else if(CMD_COUNT.equals(command[0])) {
			tx = Integer.toString(filteredRadarResults.size());
		} else if(CMD_GETTARGETID.equals(command[0]) && command.length == 2) {
			Entity target = getTarget(command[1]);
			tx = target == null ? "" : Integer.toString(target.getEntityId());
		} else if(CMD_GETPOSITION.equals(command[0]) && command.length == 2) {
			Entity target = getTarget(command[1]);
			tx = target == null ? "" : (int) Math.floor(target.posX) + ";"
					+ (int) Math.floor(target.posY) + ";" + (int) Math.floor(target.posZ);
		} else if(CMD_GETNAME.equals(command[0]) && command.length == 2) {
			Entity target = getTarget(command[1]);
			tx = target == null ? "" : target.getClass().getSimpleName().toLowerCase(Locale.US);
		}
	}

	private Entity getTarget(String value) {
		try {
			int index = Integer.parseInt(value) - 1;
			if(index < 0 || index >= filteredRadarResults.size()) return null;
			Entity target = filteredRadarResults.get(index);
			return target == null || target.isDead ? null : target;
		} catch(NumberFormatException ignored) {
			return null;
		}
	}

	@Override
	public float[] getColor() {
		return new float[] { 0.134F, 1.0F, 0.134F };
	}
}

