package com.hbm.tileentity.machine;
import java.util.ArrayList;
import java.util.List;
import com.hbm.entity.missile.EntityRocketSoyuz;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerLaunchpadSoyuz;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GUILaunchpadSoyuz;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;
import com.hbm.module.portmanager.ModulePortManPower;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.EnumUtil;
import com.hbm.util.Vec3NT;
import com.hbm.saveddata.satellites.SatelliteTypeRegistry;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.item.IDesignatorItem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
@com.hbm.interfaces.AutoRegister
public class TileEntityLaunchpadSoyuz extends TileEntityMachineBase implements ITickable, IEnergyReceiverMK2, IFluidStandardReceiverMK2, IGUIProvider, IControlReceiver {

	public long power;
	public static final long maxPower = 1_000_000;
	public static final long CONSUMPTION = 10_000;
	public FluidTankNTM[] tanks;

	public static final int INDEX_STRUT1	= 0; // struts 1-5 on the support tower
	public static final int INDEX_STRUT2	= 1;
	public static final int INDEX_STRUT3	= 2;
	public static final int INDEX_STRUT4	= 3;
	public static final int INDEX_STRUT5	= 4;
	public static final int INDEX_CARRIAGE	= 5; // delivery carriage
	public static final int INDEX_ROTOR		= 6; // carriage deploy progress
	public static final int INDEX_TILT		= 7; // carriage tilt after ramming the buffer stops

	public float[] positions		= new float[8];
	public float[] prevPositions	= new float[8];
	public float[] speed			= new float[8];
	public float[] target			= new float[8];
	public float[] syncPositions	= new float[8];
	
	protected int turnProgress;

	public SoyuzStatus soyuzStatus = SoyuzStatus.ABSENT;
	public ComponentStatus strutStatus = ComponentStatus.RETRACT;
	public ComponentStatus carriageStatus = ComponentStatus.RETRACT;
	public ComponentStatus rotorStatus = ComponentStatus.RETRACT;
	
	public boolean cargoMode = false;
	public int loadedType = -1;
	public int fuelCountdown = 0;
	public static final int FUEL_DURATION = 15 * 20;

	public static final int COUNTDOWN_DURATION = 600;
	public int countdown;
	
	private AudioWrapper[] audios;

	protected ModulePortManFluidAdaptive moduleFluidPorts;
	protected ModulePortManPower modulePowerPorts;

	public TileEntityLaunchpadSoyuz() {
		super(27, true, true);
		tanks = new FluidTankNTM[2];
		tanks[0] = new FluidTankNTM(Fluids.KEROSENE_REFORM, 128_000).withOwner(this);
		tanks[1] = new FluidTankNTM(Fluids.OXYGEN, 128_000).withOwner(this);
		
		this.audios = new AudioWrapper[3];

		moduleFluidPorts = new ModulePortManFluidAdaptive(this).setInputTanks(getReceivingTanks());
		modulePowerPorts = new ModulePortManPower(this).onlyInput();
	}

	protected PortDef[] cachedPorts;

	public PortDef[] getPorts() {
		if(cachedPorts == null) {
			
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
			ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
			
			cachedPorts = new PortDef[] {
					PortDef.make(pos.getX() + dir.offsetX * 2 + rot.offsetX * 1, pos.getY() + 1, pos.getZ() + dir.offsetZ * 2 + rot.offsetZ * 1, dir),
					PortDef.make(pos.getX() + dir.offsetX * 2 - rot.offsetX * 1, pos.getY() + 1, pos.getZ() + dir.offsetZ * 2 - rot.offsetZ * 1, dir),
					PortDef.make(pos.getX() + dir.offsetX * 2 - rot.offsetX * 7, pos.getY() + 1, pos.getZ() + dir.offsetZ * 2 - rot.offsetZ * 7, dir),
					PortDef.make(pos.getX() + dir.offsetX * 2 - rot.offsetX * 9, pos.getY() + 1, pos.getZ() + dir.offsetZ * 2 - rot.offsetZ * 9, dir),
					PortDef.make(pos.getX() - dir.offsetX * 10 + rot.offsetX * 1, pos.getY() + 1, pos.getZ() - dir.offsetZ * 10 + rot.offsetZ * 1, dir.getOpposite()),
					PortDef.make(pos.getX() - dir.offsetX * 10 - rot.offsetX * 1, pos.getY() + 1, pos.getZ() - dir.offsetZ * 10 - rot.offsetZ * 1, dir.getOpposite()),
					PortDef.make(pos.getX() - dir.offsetX * 10 - rot.offsetX * 7, pos.getY() + 1, pos.getZ() - dir.offsetZ * 10 - rot.offsetZ * 7, dir.getOpposite()),
					PortDef.make(pos.getX() - dir.offsetX * 10 - rot.offsetX * 9, pos.getY() + 1, pos.getZ() - dir.offsetZ * 10 - rot.offsetZ * 9, dir.getOpposite()),

					PortDef.make(pos.getX() + dir.offsetX * 1 - rot.offsetX * 10, pos.getY() + 1, pos.getZ() + dir.offsetZ * 1 - rot.offsetZ * 10, rot.getOpposite()),
					PortDef.make(pos.getX() - dir.offsetX * 1 - rot.offsetX * 10, pos.getY() + 1, pos.getZ() - dir.offsetZ * 1 - rot.offsetZ * 10, rot.getOpposite()),
					PortDef.make(pos.getX() - dir.offsetX * 7 - rot.offsetX * 10, pos.getY() + 1, pos.getZ() - dir.offsetZ * 7 - rot.offsetZ * 10, rot.getOpposite()),
					PortDef.make(pos.getX() - dir.offsetX * 9 - rot.offsetX * 10, pos.getY() + 1, pos.getZ() - dir.offsetZ * 9 - rot.offsetZ * 10, rot.getOpposite()),
			};
		}
		return cachedPorts;
	}

	@Override
	public String getDefaultName() {
		return "container.launchpadSoyuz";
	}

	@Override
	public void update() {
		
		if(!world.isRemote) {

			moduleFluidPorts.update(getPorts());
			modulePowerPorts.update(getPorts());
			
			this.power = Library.chargeTEFromItems(inventory, 8, power, maxPower);
			
			tanks[0].loadTank(4, 5, inventory);
			tanks[1].loadTank(6, 7, inventory);
			
			if(!hasRocketLoaded()) {
				
				boolean isMoving = false;
				for(int i = 0; i < this.positions.length; i++) {
					if(!this.finishedMoving(i)) {
						isMoving = true;
						break;
					}
				}
				
				// if all parts are currently idle, set the status back to absent and prepare new loading procedure
				if(!isMoving) this.soyuzStatus = SoyuzStatus.ABSENT;
				
				this.loadedType = -1;
			} else {
				this.loadedType = this.inventory.getStackInSlot(0).getMetadata();
			}
			
			if(this.power >= CONSUMPTION) {
				this.updateStates();
				this.move();
				this.power -= CONSUMPTION;
                markDirty();
			}
			
			for(int i = 0; i <= INDEX_STRUT5; i++) {
				
				if(this.finishedMoving(i) && this.wasMoving(i)) {
					ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
					ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
					double x = pos.getX() + 0.5 - dir.offsetX * 4 + rot.offsetX * 2;
					double z = pos.getZ() + 0.5 - dir.offsetZ * 4 + rot.offsetZ * 2;
					world.playSound(null, x, pos.getY() + 25, z, HBMSoundHandler.sliding_seal_stop, SoundCategory.BLOCKS, 35F, 0.75F);
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
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
			ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
			
			double x = pos.getX() + 0.5 - dir.offsetX * 4 - rot.offsetX * 4;
			double z = pos.getZ() + 0.5 - dir.offsetZ * 4 - rot.offsetZ * 4;
			
			if((this.soyuzStatus == SoyuzStatus.FUELING || this.soyuzStatus == SoyuzStatus.READY || this.soyuzStatus == SoyuzStatus.LAUNCHING) &&
					this.hasOxidizer() && MainRegistry.proxy.me() != null && MainRegistry.proxy.me().getDistanceSq(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5) <= 100 * 100) {

				NBTTagCompound data = new NBTTagCompound();
				data.setString("type", "tower");
				data.setFloat("lift", 0F);
				data.setFloat("base", 0.5F);
				data.setFloat("max", 2F);
				data.setInteger("life", 70 + world.rand.nextInt(30));
				data.setDouble("posX", x + world.rand.nextGaussian() * 0.75);
				data.setDouble("posZ", z + world.rand.nextGaussian() * 0.75);
				data.setDouble("posY", pos.getY() + 4);
				data.setBoolean("noWind", true);
				data.setFloat("alphaMod", 2F);
				data.setFloat("strafe", 0.075F);
				for(int i = 0; i < 3; i++) MainRegistry.proxy.effectNT(data);
			}
			
			List<EntityRocketSoyuz> entities = world.getEntitiesWithinAABB(EntityRocketSoyuz.class, new AxisAlignedBB(x - 1, pos.getY() + 4, z - 1, x + 1, pos.getY() + 14, z + 1));
			
			if(!entities.isEmpty() || (this.soyuzStatus == SoyuzStatus.LAUNCHING && this.countdown <= 20)) {
				
				NBTTagCompound data = new NBTTagCompound();
				data.setString("type", "smoke");
				data.setString("mode", "shockRand");
				data.setInteger("count", 50);
				data.setDouble("strength", world.rand.nextGaussian() * 3 + 6);
				data.setDouble("posX", x);
				data.setDouble("posY", pos.getY() + 1);
				data.setDouble("posZ", z);
				
				MainRegistry.proxy.effectNT(data);
			}
			
			handleSound(0, this.soyuzStatus == SoyuzStatus.LAUNCHING);
			handleSound(1, this.power >= CONSUMPTION && this.positions[INDEX_CARRIAGE] > 0 && this.positions[INDEX_CARRIAGE] < 1);
			handleSound(2, this.power >= CONSUMPTION && this.positions[INDEX_ROTOR] > 0 && this.positions[INDEX_ROTOR] < 1);
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

				Vec3NT pos = getSoundPosition(index);
				if(index == 1) MainRegistry.proxy.playSoundClient((float) pos.x, (float) pos.y, (float) pos.z, HBMSoundHandler.garage_stop, SoundCategory.BLOCKS, 35F, 1F);
				if(index == 2) MainRegistry.proxy.playSoundClient((float) pos.x, (float) pos.y, (float) pos.z, HBMSoundHandler.wgh_big_stop, SoundCategory.BLOCKS, 35F, 0.75F);
			}
		}
	}
	
	protected AudioWrapper createSound(int index) {
		
		Vec3NT pos = getSoundPosition(index);
		
		if(index == 0) return MainRegistry.proxy.getLoopedSound(HBMSoundHandler.soyuzReady, SoundCategory.BLOCKS, (float) pos.x, (float) pos.y, (float) pos.z, 2.0F, 100F, 1.0F, 10);
		if(index == 1) return MainRegistry.proxy.getLoopedSound(HBMSoundHandler.garage, SoundCategory.BLOCKS, (float) pos.x, (float) pos.y, (float) pos.z, 2.0F, 35F, 1.0F, 10);
		if(index == 2) return MainRegistry.proxy.getLoopedSound(HBMSoundHandler.wgh_big_start, SoundCategory.BLOCKS, (float) pos.x, (float) pos.y, (float) pos.z, 2.0F, 50F, 0.75F, 10);
		
		return null;
	}
	
	protected Vec3NT getSoundPosition(int index) {

		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
		
		double x = pos.getX() + 0.5 - dir.offsetX * 4 - rot.offsetX * 4;
		double y = pos.getY();
		double z = pos.getZ() + 0.5 - dir.offsetZ * 4 - rot.offsetZ * 4;
		
		if(index == 1) {
			double dist = 24 + 20.5 * (1 - this.positions[INDEX_CARRIAGE]);
			x -= rot.offsetX * dist;
			z -= rot.offsetZ * dist;
		}
		
		if(index == 2) {
			double dist = 18 + 20.5 * (1 - this.positions[INDEX_CARRIAGE]);
			x -= rot.offsetX * dist;
			z -= rot.offsetZ * dist;
		}
		
		return new Vec3NT(x, y + 4, z);
	}
	
	public void updateStates() {
		
		if(this.soyuzStatus == SoyuzStatus.ABSENT) {
			
			/// RETURN BEHAVIOR ///
			
			// retract all struts
			if(this.strutStatus == ComponentStatus.DEPLOY) {
				this.strutStatus = ComponentStatus.RETRACT;
				
				for(int i = 0; i <= INDEX_STRUT5; i++) {
					setTarget(i, false, 60 + world.rand.nextInt(21)); // 3-4 seconds
				}
			}
			
			// first send away the carriage
			if(this.carriageStatus == ComponentStatus.DEPLOY) {
				this.carriageStatus = ComponentStatus.RETRACT;
				setTarget(INDEX_CARRIAGE, false, 100); // 5 seconds
			}
			
			// once carriage has stopped, tilt, then retract rotor
			if(this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
				// if the carriage has hit the buffer stops, tilt
				if(wasMoving(INDEX_CARRIAGE)) setTarget(INDEX_TILT, true, 3);
				
				// after tilt has returned
				if(this.target[INDEX_TILT] == 0 && this.rotorStatus == ComponentStatus.DEPLOY) {
					this.rotorStatus = ComponentStatus.RETRACT;
					setTarget(INDEX_ROTOR, false, 100); // 5 seconds
				}
			}

			// return tilt
			if(this.target[INDEX_TILT] > 0 && this.finishedMoving(INDEX_TILT)) {
				setTarget(INDEX_TILT, false, 3);
			}
			
			/// DEPLOY BEHAVIOR ///
			
			// start if rocket is loaded and all relevant components are fully retracted
			if(this.hasRocketLoaded() &&
					this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE) &&
					this.rotorStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_ROTOR)) {
				
				// deploy carriage
				this.carriageStatus = ComponentStatus.DEPLOY;
				setTarget(INDEX_CARRIAGE, true, 200); // 10 seconds

				this.soyuzStatus = SoyuzStatus.LOADING;
				return; // always return on status change
			}
		}
		
		if(this.soyuzStatus == SoyuzStatus.LOADING) {
			
			// once carriage has returned, start rotor and lift the rocket onto the launch pad
			if(this.rotorStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
				this.rotorStatus = ComponentStatus.DEPLOY;
				setTarget(INDEX_ROTOR, true, 200); // 10 seconds
			}
			
			if(this.carriageStatus == ComponentStatus.DEPLOY && this.finishedMoving(INDEX_CARRIAGE) &&
					this.rotorStatus == ComponentStatus.DEPLOY && this.finishedMoving(INDEX_ROTOR)) {

				// once carriage and rotor are deployed, extend struts
				if(this.strutStatus == ComponentStatus.RETRACT) {
					this.strutStatus = ComponentStatus.DEPLOY;
					
					for(int i = 0; i <= INDEX_STRUT5; i++) {
						setTarget(i, true, 60 + world.rand.nextInt(21)); // 3-4 seconds
					}
				} else {

					// if struts are fully deployed, the rocket is in place, start fueling
					boolean strutsDeployed = true;
					for(int i = 0; i <= INDEX_STRUT5; i++) {
						if(!this.finishedMoving(i)) strutsDeployed = false;
					}
					
					if(strutsDeployed) {
						this.fuelCountdown = FUEL_DURATION;
						this.soyuzStatus = SoyuzStatus.FUELING;
						return; // always return on status change
					}
				}
			}
		}
		
		if(this.soyuzStatus == SoyuzStatus.FUELING) {
			
			if(this.hasAllFuel()) {
				if(this.fuelCountdown > 0) {
					this.fuelCountdown--;
				} else {
					this.soyuzStatus = SoyuzStatus.READY;
					return; // always return on status change
				}
			}
		}
		
		if(this.soyuzStatus == SoyuzStatus.READY) {
			
			// should the fuel somehow not be present during this phase, reset phase back to fueling
			if(!this.hasAllFuel()) {
				this.fuelCountdown = FUEL_DURATION;
				this.soyuzStatus = SoyuzStatus.FUELING;
				return; // always return on status change
			}
		}
		
		if(this.soyuzStatus == SoyuzStatus.LAUNCHING) {
            if(!canLaunch()) { soyuzStatus = hasRocketLoaded() ? SoyuzStatus.READY : SoyuzStatus.ABSENT; countdown = 0; return; }

			// return carriage
			if(this.carriageStatus == ComponentStatus.DEPLOY) {
				this.carriageStatus = ComponentStatus.RETRACT;
				this.setTarget(INDEX_CARRIAGE, false, 100); // 5 seconds
				
			// return rotor
			}
			
			if(this.carriageStatus == ComponentStatus.RETRACT && this.finishedMoving(INDEX_CARRIAGE)) {
				if(wasMoving(INDEX_CARRIAGE)) setTarget(INDEX_TILT, true, 3);
				
				if(this.target[INDEX_TILT] == 0 && this.rotorStatus == ComponentStatus.DEPLOY) {
					this.rotorStatus = ComponentStatus.RETRACT;
					setTarget(INDEX_ROTOR, false, 100); // 5 seconds
				}
			}

			if(this.target[INDEX_TILT] > 0 && this.finishedMoving(INDEX_TILT)) {
				setTarget(INDEX_TILT, false, 3);
			}
			
			if(this.countdown > 0) {
				this.countdown--;
				
				if(countdown % 100 == 0 && countdown > 0) world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HBMSoundHandler.alarmHatch, SoundCategory.BLOCKS, 100F, 1.1F);

				if(countdown == 20) world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HBMSoundHandler.soyuzTakeOff, SoundCategory.BLOCKS, 100F, 1.1F);
				
			} else {
				
				if(!liftOff()) { soyuzStatus = SoyuzStatus.READY; return; }
				
				for(int i = 0; i <= INDEX_STRUT5; i++) {
					setTarget(i, false, 18 + world.rand.nextInt(8)); //1 second
				}
			}
		}
	}
	
	public boolean hasRocketLoaded() { return !inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(0).getItem() == ModItems.missile_soyuz; }
	public boolean finishedMoving(int index) { return this.positions[index] == this.target[index]; }
	public boolean wasMoving(int index) { return this.positions[index] != this.prevPositions[index]; }
	
	public boolean hasAllFuel() {
		return hasJetFuel() && hasOxidizer();
	}

	public boolean hasJetFuel() { return this.tanks[0].getFill() >= 100_000; }
	public boolean hasOxidizer() { return this.tanks[1].getFill() >= 100_000; }
	
	public void setTarget(int index, boolean deploy, int duration) {
		this.target[index] = deploy ? 1F : 0F;
		this.speed[index] = 1F / duration;
	}
	
	public void move() {
		
		for(int i = 0; i < this.positions.length; i++) {
			
			this.prevPositions[i] = this.positions[i];
			
			if(Math.abs(this.positions[i] - this.target[i]) <= this.speed[i]) {
				this.positions[i] = this.target[i];
			} else if(this.positions[i] < this.target[i]) {
				this.positions[i] += this.speed[i];
			} else {
				this.positions[i] -= this.speed[i];
			}
		}
	}
	
	public float getInterpPos(int index, float interp) {
		return prevPositions[index] + (positions[index] - prevPositions[index]) * interp;
	}
	
	public boolean canLaunch() {
		
		// prerequisites for all modes
		if(!hasRocketLoaded()) return false;
		if(!this.hasAllFuel()) return false;
		if(this.power < this.CONSUMPTION) return false;
		
		// at least one cargo slot must be occupied
		if(this.cargoMode) {
			if(getCargoTarget() == null) return false;
			for(int i = 9; i < 27; i++) {
				if(!inventory.getStackInSlot(i).isEmpty()) return true;
			}
			
			return false;
			
		// checks for satellite and optional orbital module
		} else {
			
			if(this.orbital() == 1) return false;
			if(!acceptsPayload(inventory.getStackInSlot(2))) return false;
			
			return true;
		}
	}
	
    public boolean liftOff() {
        if(world.isRemote || !canLaunch()) return false;
        ForgeDirection dir = ForgeDirection.getOrientation(getBlockMetadata() - 10);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        EntityRocketSoyuz rocket = new EntityRocketSoyuz(world);
        rocket.setSkin(inventory.getStackInSlot(0).getMetadata());
        rocket.mode = cargoMode ? 1 : 0;
        rocket.setLocationAndAngles(pos.getX() + .5 - dir.offsetX * 4 - rot.offsetX * 4, pos.getY() + 4, pos.getZ() + .5 - dir.offsetZ * 4 - rot.offsetZ * 4, 0, 0);
        if(cargoMode) {
            Vec3d target = getCargoTarget();
            if(target == null) return false;
            rocket.targetX = MathHelper.floor(target.x);
            rocket.targetZ = MathHelper.floor(target.z);
            List<ItemStack> payload = new ArrayList<>();
            for(int i = 9; i < 27; i++) payload.add(inventory.getStackInSlot(i).copy());
            rocket.setPayload(payload);
        } else {
            rocket.setSat(inventory.getStackInSlot(2));
        }
        if(!world.spawnEntity(rocket)) return false;
        tanks[0].setFill(tanks[0].getFill() - 100_000);
        tanks[1].setFill(tanks[1].getFill() - 100_000);
        if(cargoMode) {
            for(int i = 9; i < 27; i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
        } else {
            if(orbital() == 2) inventory.setStackInSlot(3, ItemStack.EMPTY);
            inventory.setStackInSlot(2, ItemStack.EMPTY);
        }
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        loadedType = -1;
        soyuzStatus = SoyuzStatus.ABSENT;
        countdown = 0;
        markDirty();
        return true;
    }

    private Vec3d getCargoTarget() {
        ItemStack stack = inventory.getStackInSlot(1);
        if(stack.isEmpty() || !(stack.getItem() instanceof IDesignatorItem)) return null;
        IDesignatorItem designator = (IDesignatorItem) stack.getItem();
        if(!designator.isReady(world, stack, pos.getX(), pos.getY(), pos.getZ())) return null;
        Vec3d target = designator.getCoords(world, stack, pos.getX(), pos.getY(), pos.getZ());
        return target != null && Double.isFinite(target.x) && Double.isFinite(target.z) ? target : null;
    }

	/** Returns 0 if no orbital module is required, 1 if it is and it's missing and 2 if the orbital module is required and loaded */
	public int orbital() {
		if(this.cargoMode) return 0;
		
		if(needsOrbiter(inventory.getStackInSlot(2))) {
			if(!inventory.getStackInSlot(3).isEmpty() && inventory.getStackInSlot(3).getItem() == ModItems.missile_soyuz_lander) return 2;
			return 1;
		}
		return 0;
	}
	
	public static boolean needsOrbiter(ItemStack stack) {
		SatelliteTypeRegistry.Descriptor type = SatelliteTypeRegistry.byItem(stack);
        return type != null && (type.getLegacyId() == 7 || type.getLegacyId() == 8);
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == 0) return stack.getItem() == ModItems.missile_soyuz;
		if(slot == 1) return stack.getItem() instanceof IDesignatorItem;
		if(slot == 2) return acceptsPayload(stack) && !cargoMode;
		if(slot == 3) return stack.getItem() == ModItems.missile_soyuz_lander && !cargoMode;
		if(slot == 4) return Library.isStackDrainableForTank(stack, tanks[0]);
		if(slot == 5) return false;
		if(slot == 6) return Library.isStackDrainableForTank(stack, tanks[1]);
		if(slot == 7) return false;
		if(slot == 8) return Library.isBattery(stack);
		if(slot > 8) {
			if(!cargoMode) return false;
            if(stack.getItem() == ModItems.missile_soyuz || stack.getItem() instanceof IDesignatorItem) return false;
			return true;
		}
		return true;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing side) {
		return new int[] {
				0, 2, 3,	// soyuz, satellite, orbital module
				9, 10, 11,	// cargo
				12, 13, 14,
				15, 16, 17,
				18, 19, 20,
				21, 22, 23,
				24, 25, 26
		};
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		tanks[0].serialize(buf);
		tanks[1].serialize(buf);
		buf.writeLong(power);
		buf.writeInt(loadedType);
		buf.writeBoolean(cargoMode);
		buf.writeInt(countdown);
        buf.writeInt(fuelCountdown);
        buf.writeByte(strutStatus.ordinal()); buf.writeByte(carriageStatus.ordinal()); buf.writeByte(rotorStatus.ordinal());
		buf.writeByte((byte) this.soyuzStatus.ordinal());
		
		for(int i = 0; i < this.positions.length; i++) {
			buf.writeFloat(this.positions[i]);
            buf.writeFloat(speed[i]); buf.writeFloat(target[i]);
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		tanks[0].deserialize(buf);
		tanks[1].deserialize(buf);
		this.power = buf.readLong();
		this.loadedType = buf.readInt();
		this.cargoMode = buf.readBoolean();
		this.countdown = buf.readInt();
        fuelCountdown = buf.readInt();
        strutStatus = EnumUtil.grabEnumSafely(ComponentStatus.class, buf.readByte());
        carriageStatus = EnumUtil.grabEnumSafely(ComponentStatus.class, buf.readByte());
        rotorStatus = EnumUtil.grabEnumSafely(ComponentStatus.class, buf.readByte());
		this.soyuzStatus = EnumUtil.grabEnumSafely(SoyuzStatus.class, buf.readByte());

		for(int i = 0; i < this.positions.length; i++) {
			float newSync = buf.readFloat();
            speed[i] = buf.readFloat(); target[i] = buf.readFloat();
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
		for(int i = 0; i < 8; i++) {
			positions[i] = nbt.getFloat("p" + i);
            prevPositions[i] = syncPositions[i] = positions[i];
			speed[i] = nbt.getFloat("s" + i);
			target[i] = nbt.getFloat("t" + i);
		}
		soyuzStatus = EnumUtil.grabEnumSafely(SoyuzStatus.class, nbt.getInteger("soyuzStatus"));
		strutStatus = EnumUtil.grabEnumSafely(ComponentStatus.class, nbt.getInteger("strutStatus"));
		carriageStatus = EnumUtil.grabEnumSafely(ComponentStatus.class, nbt.getInteger("carriageStatus"));
		rotorStatus = EnumUtil.grabEnumSafely(ComponentStatus.class, nbt.getInteger("rotorStatus"));
		cargoMode = nbt.getBoolean("cargoMode");
		fuelCountdown = nbt.getInteger("fuelCountdown");
		countdown = nbt.getInteger("countdown");
        loadedType = hasRocketLoaded() ? inventory.getStackInSlot(0).getMetadata() : -1;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		tanks[0].writeToNBT(nbt, "tank0");
		tanks[1].writeToNBT(nbt, "tank1");
		for(int i = 0; i < 8; i++) {
			nbt.setFloat("p" + i, positions[i]);
			nbt.setFloat("s" + i, speed[i]);
			nbt.setFloat("t" + i, target[i]);
		}
		nbt.setInteger("soyuzStatus", soyuzStatus.ordinal());
		nbt.setInteger("strutStatus", strutStatus.ordinal());
		nbt.setInteger("carriageStatus", carriageStatus.ordinal());
		nbt.setInteger("rotorStatus", rotorStatus.ordinal());
		nbt.setBoolean("cargoMode", cargoMode);
		nbt.setInteger("fuelCountdown", fuelCountdown);
		nbt.setInteger("countdown", countdown);
        nbt.setInteger("loadedType", loadedType);
        return nbt;
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if(world != null && !world.isRemote) { modulePowerPorts.destroy(); moduleFluidPorts.destroy(); }
        for(int i = 0; i < audios.length; i++) if(audios[i] != null) { audios[i].stopSound(); audios[i] = null; }
	}
	
	@Override
	public void invalidate() {
		super.invalidate();
		if(world != null && !world.isRemote) { modulePowerPorts.destroy(); moduleFluidPorts.destroy(); }
        for(int i = 0; i < audios.length; i++) if(audios[i] != null) { audios[i].stopSound(); audios[i] = null; }
	}

	@Override public long getPower() { return this.power; }
	@Override public void setPower(long power) { this.power = power; }
	@Override public long getMaxPower() { return maxPower; }

	@Override public FluidTankNTM[] getReceivingTanks() { return tanks; }
	@Override public FluidTankNTM[] getAllTanks() { return tanks; }

	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return new ContainerLaunchpadSoyuz(player.inventory, this); }
	@Override @SideOnly(Side.CLIENT) public net.minecraft.client.gui.GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUILaunchpadSoyuz(player.inventory, this); }
	
	public static enum SoyuzStatus {
		ABSENT,		// no rocket is present, return all components to null position
		LOADING,	// rocket is moved to launch pad
		FUELING,	// rocket is on the launch pad, cooldown is active
		READY,		// rocket is ready to launch
		LAUNCHING	// countdown is active
	}
	
	public static enum ComponentStatus {
		DEPLOY, RETRACT
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override public boolean isUseableByPlayer(EntityPlayer player) { return world.getTileEntity(pos) == this && player.getDistanceSq(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 50 * 50; }
	@Override public boolean hasPermission(EntityPlayer player) { return this.isUseableByPlayer(player); }

	@Override
	public void receiveControl(NBTTagCompound data) {
		
		if(data.hasKey("cargo")) {
			this.cargoMode = data.getBoolean("cargo");
            if(soyuzStatus == SoyuzStatus.LAUNCHING) { soyuzStatus = SoyuzStatus.READY; countdown = 0; }
			this.markChanged();
		}
		
		if(data.hasKey("launch")) {
			if(this.soyuzStatus == SoyuzStatus.READY && canLaunch()) {
				this.soyuzStatus = SoyuzStatus.LAUNCHING;
				this.countdown = this.COUNTDOWN_DURATION;
				this.markChanged();
			}
		}
	}
    public static boolean acceptsPayload(ItemStack stack) {
        return SatelliteTypeRegistry.isPayload(stack) || (!stack.isEmpty() && stack.getItem() == ModItems.flame_pony);
    }
    @Override public boolean canExtractItem(int slot, ItemStack stack, int side) { return slot != 4 && slot != 6 && slot != 8; }
}
