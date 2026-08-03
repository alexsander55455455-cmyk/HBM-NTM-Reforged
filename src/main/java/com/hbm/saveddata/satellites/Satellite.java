package com.hbm.saveddata.satellites;

import com.hbm.items.ModItems;
import com.hbm.tileentity.network.RTTYSystem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Satellite {
	
	public static List<Class<? extends Satellite>> satellites = new ArrayList<Class<? extends Satellite>>();
	public static HashMap<Item, Class<? extends Satellite>> itemToClass = new HashMap<Item, Class<? extends Satellite>>();
	
	public static enum InterfaceActions {
		HAS_MAP,		//lets the interface display loaded chunks
		CAN_CLICK,		//enables onClick events
		SHOW_COORDS,	//enables coordinates as a mouse tooltip
		HAS_RADAR,		//lets the interface display loaded entities
		HAS_ORES		//like HAS_MAP but only shows ores
	}
	
	public static enum CoordActions {
		HAS_Y		//enables the Y-coord field which is disabled by default
	}
	
	public static enum Interfaces {
		NONE,		//does not interact with any sat interface (i.e. asteroid miners)
		SAT_PANEL,	//allows to interact with the sat interface panel (for graphical applications)
		SAT_COORD	//allows to interact with the sat coord remote (for teleportation or other coord related actions)
	}

	public List<InterfaceActions> ifaceAcs = new ArrayList<InterfaceActions>();
	public List<CoordActions> coordAcs = new ArrayList<CoordActions>();
	public Interfaces satIface = Interfaces.NONE;

	public static final String CHAN_SATLINK = "SAT_LINK";
	public static final String CMD_SETTARGET = "settarget";
	public static final String CMD_GETTARGET = "gettarget";
	public static final String CMD_GETTARGETX = "gettargetx";
	public static final String CMD_GETTARGETZ = "gettargetz";

	protected int targetX;
	protected int targetZ;
	protected String tx = "";
	private OrbitSettings orbitSettings;
	
	public static void register() {
		SatelliteTypeRegistry.registerDefaults();
		satellites.clear();
		itemToClass.clear();
		for(SatelliteTypeRegistry.Descriptor descriptor : SatelliteTypeRegistry.descriptors()) {
			satellites.add(descriptor.getSatelliteClass());
			ItemStack canonical = descriptor.getCanonicalStack();
			if(!canonical.isEmpty()) itemToClass.put(canonical.getItem(), descriptor.getSatelliteClass());
		}
		registerSatelliteAlias(SatelliteMapper.class, ModItems.sat_mapper);
		registerSatelliteAlias(SatelliteScanner.class, ModItems.sat_scanner);
		registerSatelliteAlias(SatelliteRadar.class, ModItems.sat_radar);
		registerSatelliteAlias(SatelliteLaser.class, ModItems.sat_laser);
		registerSatelliteAlias(SatelliteResonator.class, ModItems.sat_resonator);
		registerSatelliteAlias(SatelliteRelay.class, ModItems.sat_foeq);
		registerSatelliteAlias(SatelliteMiner.class, ModItems.sat_miner);
		registerSatelliteAlias(SatelliteLunarMiner.class, ModItems.sat_lunar_miner);
		registerSatelliteAlias(SatelliteHorizons.class, ModItems.sat_gerald);
	}
	
	private static void registerSatelliteAlias(Class<? extends Satellite> sat, Item item) {
		itemToClass.put(item, sat);
	}
	
	public static void orbit(World world, int id, int freq, double x, double y, double z) {
		SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byLegacyId(id);
		if(descriptor != null) {
			SatelliteTypeRegistry.orbit(world, descriptor.getCanonicalStack(), freq, x, y, z, null);
		}
	}
	
	public static Satellite create(int id) {
		return SatelliteTypeRegistry.createByLegacyId(id);
	}
	
	public static int getIDFromItem(Item item) {
		SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byItem(item);
		return descriptor == null ? -1 : descriptor.getLegacyId();
	}
	
	public int getID() {
		SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.bySatellite(this);
		return descriptor == null ? -1 : descriptor.getLegacyId();
	}
	
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("targetX", targetX);
		nbt.setInteger("targetZ", targetZ);
		nbt.setString("tx", tx);
	}
	
	public void readFromNBT(NBTTagCompound nbt) {
		targetX = nbt.getInteger("targetX");
		targetZ = nbt.getInteger("targetZ");
		tx = nbt.getString("tx");
	}
	
	/**
	 * Called when the satellite reaches space, used to trigger achievements and other funny stuff.
	 * @param x posX of the rocket
	 * @param y ditto
	 * @param z ditto
	 */
	public void onOrbit(World world, double x, double y, double z) {
		setTarget((int) Math.floor(x), (int) Math.floor(z));
		SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.bySatellite(this);
		String type = descriptor == null ? getClass().getSimpleName() : descriptor.getKey();
		RTTYSystem.broadcast(world, CHAN_SATLINK,
				"Established connection to " + type + " at " + targetX + " / " + targetZ);
	}

	/**
	 * Called when another payload is delivered to an occupied frequency.
	 *
	 * @return true only when the existing satellite actually accepted the part
	 */
	public boolean onPartDelivered(World world, ItemStack part) { return false; }

	public void onCommand(World world, String... command) {
		onCommandTarget(command);
		onCommandImpl(world, command);
	}

	private void onCommandTarget(String... command) {
		if(command == null || command.length == 0) return;
		switch(command[0]) {
			case CMD_SETTARGET:
				if(command.length >= 3) {
					targetX = parseInt(command[1], targetX);
					targetZ = parseInt(command[command.length - 1], targetZ);
				}
				break;
			case CMD_GETTARGET:
				tx = targetX + ";" + targetZ;
				break;
			case CMD_GETTARGETX:
				tx = Integer.toString(targetX);
				break;
			case CMD_GETTARGETZ:
				tx = Integer.toString(targetZ);
				break;
			default:
				break;
		}
	}

	protected void onCommandImpl(World world, String... command) { }

	public void setTarget(int x, int z) {
		targetX = x;
		targetZ = z;
	}

	public int getTargetX() { return targetX; }
	public int getTargetZ() { return targetZ; }
	public String getTransmission() { return tx; }

	public OrbitSettings getOrbitSettings() {
		if(orbitSettings == null) orbitSettings = OrbitSettings.defaultsFor(this);
		return orbitSettings;
	}

	public void setOrbitSettings(OrbitSettings settings) {
		orbitSettings = settings == null ? OrbitSettings.defaultsFor(this) : settings.copy();
		orbitSettings.validate();
	}

	public float[] getRenderColor() {
		OrbitSettings settings = getOrbitSettings();
		return new float[] { settings.getRed(), settings.getGreen(), settings.getBlue() };
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value);
		} catch(NumberFormatException ignored) {
			return fallback;
		}
	}
	
	/**
	 * Called by the sat interface when clicking on the screen
	 *
	 * @param player
	 * @param x      the x-coordinate translated from the on-screen coords to actual world coordinates
	 * @param z      ditto
	 */
	public void onClick(World world, EntityPlayerMP player, int x, int z) { }
	
	/**
	 * Called by the coord sat interface
	 * @param x the specified x-coordinate
	 * @param y ditto
	 * @param z ditto
	 */
	public void onCoordAction(World world, EntityPlayerMP player, int x, int y, int z) { }

	public abstract float[] getColor();
}
