package com.hbm.saveddata.satellites;

import com.hbm.entity.projectile.EntityTom;
import com.hbm.main.AdvancementManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.Locale;

public class SatelliteHorizons extends Satellite {

	public static final String CMD_FIRE = "fire";
	public static final String CMD_CANFIRE = "canfire";
	public static final String CMD_CANFIRE_LEGACY = "settarget";

	boolean used = false;
	public long lastOp;
	
	public SatelliteHorizons() {
		this.satIface = Interfaces.SAT_COORD;
	}

	public void onOrbit(World world, double x, double y, double z) {
		super.onOrbit(world, x, y, z);

		for(EntityPlayer p : world.playerEntities)
			AdvancementManager.grantAchievement(p, AdvancementManager.horizonsStart);
	}
	
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("used", used);
		nbt.setLong("lastOp", lastOp);
	}
	
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		used = nbt.getBoolean("used");
		lastOp = nbt.getLong("lastOp");
	}
	
	@Override
	protected void onCommandImpl(World world, String... command) {
		if(command == null || command.length == 0) return;
		if(CMD_FIRE.equals(command[0])) {
			activate(world, targetX, targetZ);
		} else if(CMD_CANFIRE.equals(command[0]) || CMD_CANFIRE_LEGACY.equals(command[0])) {
			tx = Boolean.toString(!used).toUpperCase(Locale.US);
		}
	}

	public void onCoordAction(World world, EntityPlayerMP player, int x, int y, int z) {
		setTarget(x, z);
		activate(world, x, z);
	}

	private boolean activate(World world, int x, int z) {
		if(used)
			return false;
		used = true;
		
		EntityTom tom = new EntityTom(world);
		tom.setPosition(x + 0.5, 600, z + 0.5);
		
		IChunkProvider provider = world.getChunkProvider();
		provider.provideChunk(x >> 4, z >> 4);
		
		world.spawnEntity(tom);

		for(EntityPlayer p : world.playerEntities)
			AdvancementManager.grantAchievement(p, AdvancementManager.horizonsEnd);
		
		//not necessary but JUST to make sure
		if(!world.isRemote) {
			FMLCommonHandler.instance().getMinecraftServerInstance().sendMessage(new TextComponentTranslation("chat.gerald.detonated"));
		}
		return true;
	}

	@Override
	public float[] getColor() {
		return new float[] { 0.0F, 0.0F, 0.0F };
	}
}
