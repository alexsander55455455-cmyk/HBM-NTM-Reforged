package com.hbm.tileentity.machine;
import com.hbm.entity.missile.EntityRocketLambda;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerLaunchpadLambda;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GUILaunchpadLambda;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.Vec3NT;
import com.hbm.module.portmanager.ModulePortManPower;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.saveddata.satellites.SatelliteTypeRegistry;
import com.hbm.lib.HBMSoundHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import com.hbm.lib.ForgeDirection;
@com.hbm.interfaces.AutoRegister
public class TileEntityLaunchpadLambda extends TileEntityMachineBase implements ITickable, IEnergyReceiverMK2, IFluidStandardReceiverMK2, IGUIProvider, IControlReceiver {

	public long power;
	public static final long maxPower = 1_000_000;
	public static final long CONSUMPTION = 10_000;
    private final ModulePortManPower powerPorts = new ModulePortManPower(this).onlyInput();
    private final ModulePortManFluidAdaptive fluidPorts;
	public FluidTankNTM[] tanks;

	public static final int INDEX_DOORS		= 0;
	public static final int INDEX_ERECTOR	= 1;
	public static final int INDEX_ROTOR		= 2;
	public static final int INDEX_CLAMPS	= 3;
	public static final int INDEX_PISTONS	= 4;

	public float[] positions		= new float[5];
	public float[] prevPositions	= new float[5];
	public float[] speed			= new float[5];
	public float[] target			= new float[5];
	public float[] syncPositions	= new float[5];
	
	protected int turnProgress;
	
	/** True if the rocket has been placed on the silo door, detaches the rendering from the erector */
	public boolean erected = false;
	/** True if the erector is moving up, placing the rocket on the silo door */
	public boolean erecting = false;
	
	// Persisted animation counters.
	public int animationProgress = 0;
	public int animationDelay = 0;
	
	public boolean autolaunch = false;
	
	private AudioWrapper[] audios;

	public static final int COUNTDOWN_DURATION = 200;
	public int countdown;

	public TileEntityLaunchpadLambda() {
		super(7, true, true);
		tanks = new FluidTankNTM[2];
		tanks[0] = new FluidTankNTM(Fluids.GASOLINE_LEADED, 64_000).withOwner(this);
		tanks[1] = new FluidTankNTM(Fluids.PEROXIDE, 64_000).withOwner(this);
        fluidPorts = new ModulePortManFluidAdaptive(this).setInputTanks(tanks);
		
		this.audios = new AudioWrapper[4];
	}

	protected PortDef[] cachedPorts;

	public PortDef[] getPorts() {
		if(cachedPorts == null) {
			
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
			ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
			
			cachedPorts = new PortDef[] {
					PortDef.make(pos.getX() + dir.offsetX * 7 - rot.offsetX * 2, pos.getY() + 1, pos.getZ() + dir.offsetZ * 7 - rot.offsetZ * 2, dir),
					PortDef.make(pos.getX() + dir.offsetX * 7 - rot.offsetX * 3, pos.getY() + 1, pos.getZ() + dir.offsetZ * 7 - rot.offsetZ * 3, dir),
					PortDef.make(pos.getX() + dir.offsetX * 7 - rot.offsetX * 4, pos.getY() + 1, pos.getZ() + dir.offsetZ * 7 - rot.offsetZ * 4, dir),
					PortDef.make(pos.getX() + dir.offsetX * 7 - rot.offsetX * 5, pos.getY() + 1, pos.getZ() + dir.offsetZ * 7 - rot.offsetZ * 5, dir),
					PortDef.make(pos.getX() + dir.offsetX * 7 - rot.offsetX * 6, pos.getY() + 1, pos.getZ() + dir.offsetZ * 7 - rot.offsetZ * 6, dir),
			};
		}
		return cachedPorts;
	}

	@Override
	public String getDefaultName() {
		return "container.launchpadLambda";
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == 0) return stack.getItem() == ModItems.missile_lambda;
		if(slot == 1) return acceptsPayload(stack);
		if(slot == 2) return Library.isStackDrainableForTank(stack, tanks[0]);
		if(slot == 3) return false;
		if(slot == 4) return Library.isStackDrainableForTank(stack, tanks[1]);
		if(slot == 5) return false;
		if(slot == 6) return Library.isBattery(stack);
		return true;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing side) {
		return new int[] {
			0, 1,	// lambda, satellite
		};
	}

	@Override
	public void update() {
		
		if(!world.isRemote) {

			this.powerPorts.update(getPorts());
			this.fluidPorts.update(getPorts());
			
			this.power = Library.chargeTEFromItems(inventory, 6, power, maxPower);
			
			tanks[0].loadTank(2, 3, inventory);
			tanks[1].loadTank(4, 5, inventory);
			
			if(!this.hasRocketLoaded()) {
				this.erected = false;
				this.erecting = false;
				this.countdown = 0;
			}
			
			if(this.power >= CONSUMPTION) {
				this.updateStates();
				this.move();
				this.power -= CONSUMPTION;
                markDirty();
				
				if(this.autolaunch && this.erected && this.canLaunch() && this.countdown <= 0) {
					this.countdown = this.COUNTDOWN_DURATION;
				}
			}
			
			this.networkPackNT(300);
			
		} else {
			
			for(int i = 0; i < this.positions.length; i++) {

				this.prevPositions[i] = this.positions[i];

				if(this.turnProgress > 0) {
					this.positions[i] = this.positions[i] + ((this.syncPositions[i] - this.positions[i]) / (float) this.turnProgress);

				} else {
					this.positions[i] = this.syncPositions[i];
				}
			}
			
            if(turnProgress > 0) turnProgress--;

			if(this.erected && this.hasOxidizer() && MainRegistry.proxy.me() != null && MainRegistry.proxy.me().getDistanceSq(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5) <= 100 * 100) {
				
				NBTTagCompound data = new NBTTagCompound();
				data.setString("type", "tower");
				data.setFloat("lift", 0F);
				data.setFloat("base", 0.5F);
				data.setFloat("max", 2F);
				data.setInteger("life", 70 + world.rand.nextInt(30));
				data.setDouble("posX", pos.getX() + 0.5 + world.rand.nextGaussian() * 0.25);
				data.setDouble("posZ", pos.getZ() + 0.5 + world.rand.nextGaussian() * 0.25);
				data.setDouble("posY", pos.getY() + 2);
				data.setBoolean("noWind", true);
				data.setFloat("alphaMod", 2F);
				data.setFloat("strafe", 0.075F);
				for(int i = 0; i < 3; i++) MainRegistry.proxy.effectNT(data);
			}

			handleSound(0, this.syncPositions[INDEX_DOORS] > 0F && this.syncPositions[INDEX_DOORS] < 3F);
			handleSound(1, !this.finishedMoving(INDEX_ERECTOR) && this.wasMoving(INDEX_ERECTOR));
			handleSound(2, this.syncPositions[INDEX_ROTOR] > 0F && this.syncPositions[INDEX_ROTOR] < 180F);
			handleSound(3, this.countdown > 0);
			
			if(this.wasMoving(INDEX_CLAMPS) && this.syncPositions[INDEX_CLAMPS] == 90F)
				MainRegistry.proxy.playSoundClient(pos.getX() + 0.5F, pos.getY() + 12, pos.getZ() + 0.5F, HBMSoundHandler.sliding_seal_stop, SoundCategory.BLOCKS, 25F, 0.75F);
		}
	}
	
	protected void handleSound(int index, boolean isRunning) {
		
		if(isRunning) {
			
			if(this.audios[index] == null) {
				this.audios[index] = createSound(index);
				this.audios[index].startSound();
				
			} else if(!this.audios[index].isPlaying()){
				this.audios[index].stopSound();
				this.audios[index] = createSound(index);
				this.audios[index].startSound();
			}

			Vec3NT pos = getSoundPosition(index);
			this.audios[index].keepAlive();
			this.audios[index].updatePosition((float) pos.x, (float) pos.y, (float) pos.z);
			
		} else {
			
			if(this.audios[index] != null) {
				this.audios[index].stopSound();
				this.audios[index] = null;

				if(Math.abs(this.positions[INDEX_ERECTOR] - this.prevPositions[INDEX_ERECTOR]) > 5) return;
				
				Vec3NT pos = getSoundPosition(index);
				if(index == 0) MainRegistry.proxy.playSoundClient((float) pos.x, (float) pos.y, (float) pos.z, HBMSoundHandler.garage_stop, SoundCategory.BLOCKS, 35F, 1F);
				if(index == 1 && this.target[INDEX_ERECTOR] == 25F) MainRegistry.proxy.playSoundClient((float) pos.x, (float) pos.y, (float) pos.z, HBMSoundHandler.wgh_big_stop, SoundCategory.BLOCKS, 25F, 0.5F);
				if(index == 1 && this.target[INDEX_ERECTOR] != 25F) MainRegistry.proxy.playSoundClient((float) pos.x, (float) pos.y, (float) pos.z, HBMSoundHandler.wgh_stop, SoundCategory.BLOCKS, 25F, 1F);
				if(index == 2) MainRegistry.proxy.playSoundClient((float) pos.x, (float) pos.y, (float) pos.z, HBMSoundHandler.wgh_big_stop, SoundCategory.BLOCKS, 25F, 0.75F);
			}
		}
	}
	
	protected AudioWrapper createSound(int index) {
		
		Vec3NT pos = getSoundPosition(index);
		
		if(index == 0) return MainRegistry.proxy.getLoopedSound(HBMSoundHandler.garage, SoundCategory.BLOCKS, (float) pos.x, (float) pos.y, (float) pos.z, 2.0F, 35F, 1.0F, 10);
		if(index == 1) return MainRegistry.proxy.getLoopedSound(HBMSoundHandler.wgh_start, SoundCategory.BLOCKS, (float) pos.x, (float) pos.y, (float) pos.z, 2.0F, 35F, 1.0F, 10);
		if(index == 2) return MainRegistry.proxy.getLoopedSound(HBMSoundHandler.wgh_big_start, SoundCategory.BLOCKS, (float) pos.x, (float) pos.y, (float) pos.z, 2.0F, 35F, 0.75F, 10);
		if(index == 3) return MainRegistry.proxy.getLoopedSound(HBMSoundHandler.alarmRegular, SoundCategory.BLOCKS, (float) pos.x, (float) pos.y, (float) pos.z, 10F, 50F, 1F, 20);
		
		return null;
	}
	
	protected Vec3NT getSoundPosition(int index) {

		double x = pos.getX() + 0.5;
		double y = pos.getY() + 3;
		double z = pos.getZ() + 0.5;
		
		if(index == 2) {
			y += 12;
		}
		
		return new Vec3NT(x, y, z);
	}
	
	public void updateStates() {
		
		// rocket is being erected
		if(finishedAllMoving() && this.erecting && !this.erected) {
			
			if(this.erectorDown() && this.doorsClosed()) {
				this.setTarget(INDEX_DOORS, 3F, 3F, 60);
				this.setTarget(INDEX_PISTONS, 0F, 1F, 1);
				this.setTarget(INDEX_CLAMPS, 0F, 1F, 1);
				this.setTarget(INDEX_ROTOR, 180F, 180F, 1);
			} else if(this.erectorDown() && this.doorsOpen()) {
				this.animationDelay = 20;
				this.setTarget(INDEX_ERECTOR, 27F, 27F, 100);
			} else if(this.erectorUp() && this.rotorDown()) {
				this.animationDelay = 20;
				this.setTarget(INDEX_ROTOR, 0F, 180F, 60);
				this.setTarget(INDEX_DOORS, 0F, 3F, 60);
			} else if(this.erectorUp() && this.rotorUp() && this.doorsClosed()) {
				this.animationDelay = 20;
				this.setTarget(INDEX_ERECTOR, 25F, 2F, 60);
			} else if(this.erectorCenter() && this.rotorUp() && this.doorsClosed()) {
				this.erected = true;
				this.markChanged();
			} else {
				// oops, we've gone off-script, just run everything back
				this.erecting = false;
				this.markChanged();
			}
		}
		
		// return erector when countdown hits T-1
		if(this.erected && this.countdown <= 20 && this.countdown > 0) {
			this.erecting = false;
			this.markChanged();
		}
		
		// return erector post launch
		if(finishedAllMoving() && !this.erecting) {
			
			// Close the silo doors before returning the erector.
			this.setTarget(INDEX_DOORS, 0F, 3F, 60);
			
			if(this.clampsOn()) {
				this.setTarget(INDEX_PISTONS, 0.75F, 0.75F, 20);
			} else if(this.clampsOff() && this.clampsDown()) {
				this.animationDelay = 10;
				this.setTarget(INDEX_CLAMPS, 90F, 90F, 40);
			} else if(this.clampsUp() && !this.erectorDown()) {
				this.animationDelay = 20;
				this.setTarget(INDEX_ERECTOR, 0F, 25F, 100);
			} else if(this.erectorDown() && this.doorsClosed()) {
				
				if(this.hasRocketLoaded()) {
					this.erecting = true;
					this.markChanged();
				}
			}
		}
		
		if(this.erected && this.countdown > 0) {
            if(!canLaunch()) { countdown = 0; markDirty(); return; }
			this.countdown--;
			
			if(this.countdown <= 0) {
				this.markChanged();

				this.liftOff();
			}
		}
	}
	
    public boolean liftOff() {
        if(world.isRemote || !erected || !canLaunch()) return false;
        EntityRocketLambda rocket = new EntityRocketLambda(world);
        rocket.setLocationAndAngles(pos.getX() + .5, pos.getY() + 2, pos.getZ() + .5, 0, 0);
        rocket.setSat(inventory.getStackInSlot(1).copy());
        if(!world.spawnEntity(rocket)) return false;
        tanks[0].setFill(tanks[0].getFill() - 24_000);
        tanks[1].setFill(tanks[1].getFill() - 24_000);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        inventory.setStackInSlot(1, ItemStack.EMPTY);
        erected = false;
        world.playSound(null, pos, HBMSoundHandler.soyuzTakeOff, SoundCategory.BLOCKS, 100F, 1.1F);
        markDirty();
        return true;
    }

	// Mechanical position predicates.
	public boolean doorsClosed() { return this.positions[INDEX_DOORS] == 0F; }
	public boolean doorsOpen() { return this.positions[INDEX_DOORS] == 3F; }
	public boolean erectorDown() { return this.positions[INDEX_ERECTOR] == 0F; }
	public boolean erectorCenter() { return this.positions[INDEX_ERECTOR] == 25F; }
	public boolean erectorUp() { return this.positions[INDEX_ERECTOR] == 27F; }
	public boolean rotorUp() { return this.positions[INDEX_ROTOR] == 0F; }
	public boolean rotorDown() { return this.positions[INDEX_ROTOR] == 180F; }
	public boolean clampsOn() { return this.positions[INDEX_PISTONS] == 0F; }
	public boolean clampsOff() { return this.positions[INDEX_PISTONS] == 0.75F; }
	public boolean clampsUp() { return this.positions[INDEX_CLAMPS] == 90F; }
	public boolean clampsDown() { return this.positions[INDEX_CLAMPS] == 0F; }

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		tanks[0].serialize(buf);
		tanks[1].serialize(buf);
		buf.writeLong(power);
		buf.writeBoolean(erecting);
		buf.writeBoolean(erected);
		buf.writeBoolean(autolaunch);
		buf.writeInt(countdown);

		for(int i = 0; i < this.target.length; i++) buf.writeFloat(this.target[i]);
		for(int i = 0; i < this.positions.length; i++) buf.writeFloat(this.positions[i]);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		tanks[0].deserialize(buf);
		tanks[1].deserialize(buf);
		this.power = buf.readLong();
		this.erecting = buf.readBoolean();
		this.erected = buf.readBoolean();
		this.autolaunch = buf.readBoolean();
		this.countdown = buf.readInt();

		for(int i = 0; i < this.target.length; i++) this.target[i] = buf.readFloat();
		for(int i = 0; i < this.positions.length; i++) {
			float newSync = buf.readFloat();
			if(this.syncPositions[i] != newSync) {
				this.syncPositions[i] = newSync;
				this.turnProgress = 3;
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if(!nbt.hasKey("power")) return;
		this.power = nbt.getLong("power");
		tanks[0].readFromNBT(nbt, "tank0");
		tanks[1].readFromNBT(nbt, "tank1");
		for(int i = 0; i < 5; i++) {
			positions[i] = nbt.getFloat("p" + i);
			speed[i] = nbt.getFloat("s" + i);
			target[i] = nbt.getFloat("t" + i);
		}
		erected = nbt.getBoolean("erected");
		erecting = nbt.getBoolean("erecting");
		animationProgress = nbt.getInteger("animationProgress");
		animationDelay = nbt.getInteger("animationDelay");
		autolaunch = nbt.getBoolean("autolaunch");
		countdown = nbt.getInteger("countdown");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		tanks[0].writeToNBT(nbt, "tank0");
		tanks[1].writeToNBT(nbt, "tank1");
		for(int i = 0; i < 5; i++) {
			nbt.setFloat("p" + i, positions[i]);
			nbt.setFloat("s" + i, speed[i]);
			nbt.setFloat("t" + i, target[i]);
		}
		nbt.setBoolean("erected", erected);
		nbt.setBoolean("erecting", erecting);
		nbt.setInteger("animationProgress", animationProgress);
		nbt.setInteger("animationDelay", animationDelay);
		nbt.setBoolean("autolaunch", autolaunch);
		nbt.setInteger("countdown", countdown);
        return nbt;
	}
	
	public void setTarget(int index, float target, float span, int duration) {
		if(span <= 0) span = 1F;
		this.target[index] = target;
		this.speed[index] = span / duration;
	}
	
	public void move() {
		
		if(this.animationDelay > 0) this.animationDelay--;
		
		for(int i = 0; i < this.positions.length; i++) {
			
			this.prevPositions[i] = this.positions[i];
			if(this.animationDelay > 0) continue;
			
			if(Math.abs(this.positions[i] - this.target[i]) <= this.speed[i]) {
				this.positions[i] = this.target[i];
			} else if(this.positions[i] < this.target[i]) {
				this.positions[i] += this.speed[i];
			} else {
				this.positions[i] -= this.speed[i];
			}
		}
	}
	
	public boolean canLaunch() {
		if(!this.hasRocketLoaded()) return false;
		if(!this.hasAllFuel()) return false;
		if(this.power < this.CONSUMPTION) return false;
		if(inventory.getStackInSlot(1).isEmpty()) return false;
		if(!this.isItemValidForSlot(1, inventory.getStackInSlot(1))) return false;
		if(!acceptsPayload(inventory.getStackInSlot(1))) return false;
		
		return true;
	}

	public boolean hasRocketLoaded() { return !inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(0).getItem() == ModItems.missile_lambda; }
	public boolean finishedMoving(int index) { return this.positions[index] == this.target[index]; }
	public boolean wasMoving(int index) { return this.positions[index] != this.prevPositions[index]; }
	
	public boolean finishedAllMoving() {
		for(int i = 0; i < this.positions.length; i++) if(!finishedMoving(i)) return false;
		return true;
	}
	
	public float getInterpPos(int index, float interp) {
		return prevPositions[index] + (positions[index] - prevPositions[index]) * interp;
	}
	
	public boolean hasAllFuel() {
		return hasJetFuel() && hasOxidizer();
	}

	public boolean hasJetFuel() { return this.tanks[0].getFill() >= 24_000; }
	public boolean hasOxidizer() { return this.tanks[1].getFill() >= 24_000; }

	@Override public long getPower() { return this.power; }
	@Override public void setPower(long power) { this.power = power; }
	@Override public long getMaxPower() { return maxPower; }

	@Override public FluidTankNTM[] getReceivingTanks() { return tanks; }
	@Override public FluidTankNTM[] getAllTanks() { return tanks; }

	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return new ContainerLaunchpadLambda(player.inventory, this); }
	@Override @SideOnly(Side.CLIENT) public net.minecraft.client.gui.GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUILaunchpadLambda(player.inventory, this); }

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {

		if(bb == null) {
			bb = new AxisAlignedBB(
					pos.getX() - 7,
					pos.getY(),
					pos.getZ() - 7,
					pos.getX() + 8,
					pos.getY() + 30,
					pos.getZ() + 8
					);
		}

		return bb;
	}
	
	@Override public boolean isUseableByPlayer(EntityPlayer player) { return world.getTileEntity(pos) == this && player.getDistanceSq(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 24 * 24; }

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return this.isUseableByPlayer(player);
	}

	@Override
	public void receiveControl(NBTTagCompound data) {
		
		if(data.hasKey("auto")) {
			this.autolaunch = data.getBoolean("auto");
			this.markChanged();
		}
		
		if(data.hasKey("launch")) {
			
			if(erected && countdown == 0 && canLaunch()) {
				this.countdown = this.COUNTDOWN_DURATION;
                markDirty();
			}
		}
	}
    public static boolean acceptsPayload(ItemStack stack) {
        if(!SatelliteTypeRegistry.isPayload(stack)) return false;
        SatelliteTypeRegistry.Descriptor type = SatelliteTypeRegistry.byItem(stack);
        return type == null || (type.getLegacyId() != 7 && type.getLegacyId() != 8);
    }
    @Override public void invalidate() { if(world != null && !world.isRemote) { powerPorts.destroy(); fluidPorts.destroy(); } super.invalidate(); }
    @Override public void onChunkUnload() { if(world != null && !world.isRemote) { powerPorts.destroy(); fluidPorts.destroy(); } super.onChunkUnload(); }
    @Override public boolean canExtractItem(int slot, ItemStack stack, int side) { return slot == 0 || slot == 1 || slot == 3 || slot == 5; }
}
