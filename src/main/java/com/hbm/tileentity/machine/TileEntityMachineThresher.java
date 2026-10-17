package com.hbm.tileentity.machine;

import java.util.List;
import com.hbm.blocks.generic.BlockTallPlant;
import com.hbm.blocks.PlantEnums.EnumTallPlantType;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageSource;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.util.TrackerUtil;
import com.hbm.util.Vec3NT;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@com.hbm.interfaces.AutoRegister
public class TileEntityMachineThresher extends TileEntityLoadedBase implements IBufPacketReceiver, IFluidStandardReceiverMK2, IFluidCopiable, ITickable {

	public FluidTankNTM tank;
    private final ModulePortManFluidAdaptive fluidPorts;

	public boolean isOn;
	public boolean isSuspended;
	public int delay;

	private int turnProgress;
	public float syncAngle;
	public float angle;
	public float prevAngle;

	// 0: waiting, 1: extending, 2: retracting
	private int state = 0;

	public float spin;
	public float lastSpin;
	private AudioWrapper audio;

	public TileEntityMachineThresher() {
		this.tank = new FluidTankNTM(Fluids.WOODOIL, 100).withOwner(this);
        this.fluidPorts = new ModulePortManFluidAdaptive(this).setInputTanks(tank);
	}

	protected PortDef[] cachedPorts;

	public PortDef[] getPorts() {
		if(cachedPorts == null) {
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata()).getOpposite();
			ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

			cachedPorts = new PortDef[] {
					PortDef.make(pos.getX(), pos.getY(), pos.getZ(), rot, rot.getOpposite())
			};
		}
		return cachedPorts;
	}

	@Override
	public void update() {

		if(!world.isRemote) {

			this.fluidPorts.update(getPorts());

			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata()).getOpposite();
			ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

			if(!isSuspended && world.getTotalWorldTime() % 20 == 0) {
				if(tank.getFill() > 0) {
					tank.setFill(tank.getFill() - 1);
					this.isOn = true;
				} else {
					this.isOn = false;
				}
			}

			if(isOn && !isSuspended) {

				if(this.state == 0) {
					this.delay--;
					if(delay <= 0) this.state = 1;
				}

				if(this.state == 1) {
					this.angle += 82.5F / 60F;

					if(this.angle >= 82.5F) {
						this.angle = 82.5F;
						this.state = 2;
					}
				} else if(this.state == 2) {
					this.angle -= 82.5F / 60F;

					if(this.angle <= 0F) {
						this.angle = 0F;
						this.state = 0;
						this.delay = 200 + world.rand.nextInt(100);
					}
				}

				if(this.angle != 0) {
					Vec3NT pivot = new Vec3NT(pos.getX() + 0.5 - dir.offsetX, pos.getY() + 0.5, pos.getZ() + 0.5 - dir.offsetZ);
					Vec3NT upperArm = new Vec3NT(-dir.offsetX * 4, 0, -dir.offsetZ * 4);
					Vec3NT lowerArm = new Vec3NT(-dir.offsetX * 4, 0, -dir.offsetZ * 4);
					if(dir.offsetZ != 0) {
						upperArm.rotatePitchSelf((float) Math.toRadians(82.5 - angle));
						lowerArm.rotatePitchSelf((float) -Math.toRadians(82.5 - angle));
					}
					if(dir.offsetX != 0) {
						upperArm.rotateRollSelf((float) Math.toRadians(82.5 - angle));
						lowerArm.rotateRollSelf((float) -Math.toRadians(82.5 - angle));
					}
					Vec3NT armTip = new Vec3NT(-dir.offsetX * 2, 0, -dir.offsetZ * 2);

					double endX = pivot.x + upperArm.x + lowerArm.x + armTip.x;
					double endZ = pivot.z + upperArm.z + lowerArm.z + armTip.z;

					//ParticleUtil.spawnGasFlame(world, endX, pos.getY(), endZ, 0, 0.2, 0);

					for(int i = -3; i <= 3; i++) {
						int hitX = (int) Math.floor(endX + rot.offsetX * i);
						int hitZ = (int) Math.floor(endZ + rot.offsetZ * i);

						Block b = blockAt(hitX, pos.getY(), hitZ);
						int meta = metaAt(hitX, pos.getY(), hitZ);

						if(stateAt(hitX, pos.getY(), hitZ).isNormalCube() && !this.canCut(b)) {
							this.state = 2;
							break;
						}

						if(b == Blocks.DOUBLE_PLANT) {
							// sunflower
							if((meta & 7) == 0 && world.rand.nextInt(250) == 0) {
								breakEffect(hitX, pos.getY(), hitZ);
								this.dropItem(new ItemStack(Blocks.DOUBLE_PLANT, 1, 0));
							}
							// tall grass
							if((meta & 7) == 2 && world.rand.nextInt(100) == 0) {
								breakEffect(hitX, pos.getY(), hitZ);
								this.dropItem(new ItemStack(Items.WHEAT_SEEDS, 1, 0));
							}
							continue;
						}

						// NTM tall plants like hemp
						if(b instanceof BlockTallPlant) {
							this.cutTallPlant(b, meta, hitX, pos.getY(), hitZ);
							continue;
						}

						if(b == Blocks.REEDS || b == Blocks.CACTUS) {
							this.cutCane(b, hitX, pos.getY(), hitZ);
							continue;
						}
						// IGrowable also covers anything that accepts bone
						// meal, so we have to handle actual crops last
						if(canCut(b) && !this.shouldIgnore(world, hitX, pos.getY(), hitZ, b, meta)) this.cutCrop(b, meta, hitX, pos.getY(), hitZ);
					}

					List<EntityLivingBase> affected = world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(endX, pos.getY() + 0.5, endZ, endX, pos.getY() + 0.5, endZ).grow(Math.abs(dir.offsetX * 0.5) + Math.abs(rot.offsetX * 4.5), 0.5, Math.abs(dir.offsetZ * 0.5) + Math.abs(rot.offsetZ * 4.5)));

					for(EntityLivingBase e : affected) {
						if(e.isEntityAlive() && e.attackEntityFrom(ModDamageSource.turbofan, 100)) {
							if(e instanceof IMob && !e.isEntityAlive()) this.dropItem(new ItemStack(ModItems.nitra_small));
							world.playSound(null, e.posX, e.posY, e.posZ, SoundEvents.ENTITY_ZOMBIE_BREAK_DOOR_WOOD, SoundCategory.HOSTILE, 2.0F, 0.95F + world.rand.nextFloat() * 0.2F);
							int count = Math.min((int)Math.ceil(e.getMaxHealth() / 4), 250);
							NBTTagCompound data = new NBTTagCompound();
							data.setString("type", "vanillaburst");
							data.setInteger("count", count * 4);
							data.setDouble("motion", 0.1D);
							data.setString("mode", "blockdust");
							data.setInteger("block", Block.getIdFromBlock(Blocks.REDSTONE_BLOCK));
							PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(data, e.posX, e.posY + e.height * 0.5, e.posZ), new TargetPoint(e.dimension, e.posX, e.posY, e.posZ, 50));
						}
					}
				}
			}

			networkPackNT(100);

		} else {

			this.lastSpin = this.spin;

			if(isOn && !isSuspended) {
				if(this.angle > 0) this.spin += 15F;

				ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata()).getOpposite();
				ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

				world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, pos.getX() + 0.5 + dir.offsetX * 0.8125 + rot.offsetX * 0.375, pos.getY() + 1.5625, pos.getZ() + 0.5 + dir.offsetZ * 0.8125 + rot.offsetZ * 0.375, 0, 0, 0);
			}

			if(isOn && !isSuspended && MainRegistry.proxy.me().getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 15 * 15) {
				if(audio == null) {
					audio = createAudioLoop();
					audio.startSound();
				} else if(!audio.isPlaying()) {
					audio = rebootAudio(audio);
				}

				audio.keepAlive();
				audio.updateVolume(this.getVolume(1F));

			} else {
				if(audio != null) {
					audio.stopSound();
					audio = null;
				}
			}

			if(this.spin >= 360F) {
				this.spin -= 360F;
				this.lastSpin -= 360F;
			}

			this.prevAngle = this.angle;

			if(this.turnProgress > 0) {
				double d0 = MathHelper.wrapDegrees(this.syncAngle - (double) this.angle);
				this.angle = (float) ((double) this.angle + d0 / (double) this.turnProgress);
				--this.turnProgress;
			} else {
				this.angle = this.syncAngle;
			}
		}
	}

	@Override
	public AudioWrapper createAudioLoop() {
		return MainRegistry.proxy.getLoopedSound(new SoundEvent(new ResourceLocation("hbm:block.engine")), SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 1.0F, 10F, 1.0F + world.rand.nextFloat() * 0.1F, 10);
	}

	@Override
	public void onChunkUnload() {
        if(world != null && !world.isRemote) fluidPorts.destroy();
        super.onChunkUnload();
		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	@Override
	public void invalidate() {
        if(world != null && !world.isRemote) fluidPorts.destroy();
		super.invalidate();
		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	public static boolean canCut(Block b) {
		if(b instanceof IGrowable) return true;
		if(b == Blocks.NETHER_WART) return true;
		if(b == Blocks.MELON_BLOCK || b == Blocks.PUMPKIN) return true;
		return false;
	}

	public static boolean shouldIgnore(World world, int x, int y, int z, Block b, int meta) {

		if(b instanceof BlockStem) return true;
		if(b == Blocks.NETHER_WART) return meta < 3;

		if((b instanceof IGrowable)) {
			return ((IGrowable) b).canGrow(world, new BlockPos(x, y, z), world.getBlockState(new BlockPos(x, y, z)), world.isRemote);
		}

		return false;
	}

    private IBlockState stateAt(int x, int y, int z) { return world.getBlockState(new BlockPos(x, y, z)); }
    private Block blockAt(int x, int y, int z) { return stateAt(x, y, z).getBlock(); }
    private int metaAt(int x, int y, int z) { IBlockState s = stateAt(x, y, z); return s.getBlock().getMetaFromState(s); }
    private void breakEffect(int x, int y, int z) { world.playEvent(2001, new BlockPos(x, y, z), Block.getStateId(stateAt(x, y, z))); }

    public static boolean acceptsFuel(FluidType type) {
        return type != null && (type == Fluids.WOODOIL || type == Fluids.ETHANOL || type == Fluids.FISHOIL
                || type == Fluids.HEAVYOIL || type == Fluids.COALCREOSOTE);
    }

    protected void cutTallPlant(Block block, int meta, int x, int y, int z) {
        EnumTallPlantType type = EnumTallPlantType.VALUES[meta];
        if(type == EnumTallPlantType.HEMP_LOWER || type == EnumTallPlantType.MUSTARD_WILLOW_4_LOWER) {
            y++;
            if(blockAt(x, y, z) != block) return;
            type = EnumTallPlantType.VALUES[metaAt(x, y, z)];
        }
        if(type != EnumTallPlantType.HEMP_UPPER && type != EnumTallPlantType.MUSTARD_WILLOW_4_UPPER) return;
        BlockPos target = new BlockPos(x, y, z);
        breakEffect(x, y, z);
        for(ItemStack drop : block.getDrops(world, target, stateAt(x, y, z), 0)) dropItem(drop);
        world.setBlockToAir(target);
    }

    protected void cutCane(Block target, int x, int y, int z) {
        int offset = blockAt(x, y - 1, z) == target ? -1 : 0;
        for(int i = 2 + offset; i > offset; i--) {
            BlockPos at = new BlockPos(x, y + i, z);
            IBlockState state = world.getBlockState(at);
            if(state.getBlock() != target) continue;
            breakEffect(x, y + i, z);
            for(ItemStack drop : target.getDrops(world, at, state, 0)) dropItem(drop);
            world.setBlockToAir(at);
        }
    }

    protected void cutCrop(Block block, int meta, int x, int y, int z) {
        BlockPos target = new BlockPos(x, y, z), soilPos = target.down();
        IBlockState soil = world.getBlockState(soilPos), replacement = Blocks.AIR.getDefaultState();
        breakEffect(x, y, z);
        if(!world.isRemote && !world.restoringBlockSnapshots) {
            boolean replanted = false;
            for(ItemStack drop : block.getDrops(world, target, world.getBlockState(target), 0)) {
                if(!replanted && drop.getItem() instanceof IPlantable) {
                    IPlantable seed = (IPlantable) drop.getItem();
                    if(soil.getBlock().canSustainPlant(soil, world, soilPos, EnumFacing.UP, seed)) {
                        replacement = seed.getPlant(world, target);
                        replanted = true;
                        drop.shrink(1);
                    }
                }
                dropItem(drop);
            }
            if(block == Blocks.WHEAT && !replanted) replacement = block.getStateFromMeta(0);
        }
        world.setBlockState(target, replacement, 3);
    }

    protected void dropItem(ItemStack drop) {
        if(drop.isEmpty()) return;
        ForgeDirection dir = ForgeDirection.getOrientation(getBlockMetadata());
        EntityItem item = new EntityItem(world, pos.getX() + 0.5 - dir.offsetX * 0.75, pos.getY(), pos.getZ() + 0.5 - dir.offsetZ * 0.75, drop);
        item.setPickupDelay(10);
        item.motionX = dir.offsetX * -0.2 + 0.2;
        item.motionZ = dir.offsetZ * -0.2;
        world.spawnEntity(item);
        EntityTrackerEntry entry = TrackerUtil.getTrackerEntry((WorldServer) world, item.getEntityId());
        if(entry != null) entry.sendPacketToTrackedPlayers(new SPacketEntityVelocity(item));
    }

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeBoolean(this.isOn);
		buf.writeBoolean(this.isSuspended);
		buf.writeFloat(this.angle);
		this.tank.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.isOn = buf.readBoolean();
		this.isSuspended = buf.readBoolean();
		this.syncAngle = buf.readFloat();
		this.turnProgress = 3; //use 3-ply for extra smoothness
		this.tank.deserialize(buf);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.isOn = nbt.getBoolean("isOn");
		this.isSuspended = nbt.getBoolean("isSuspended");
		this.angle = nbt.getFloat("angle");
		this.state = nbt.getInteger("state");
		this.tank.readFromNBT(nbt, "t");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("isOn", this.isOn);
		nbt.setBoolean("isSuspended", this.isSuspended);
		nbt.setFloat("angle", this.angle);
		nbt.setInteger("state", this.state);
		tank.writeToNBT(nbt, "t");
        return nbt;
	}

	@Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {tank}; }
	@Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {tank}; }

	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {

		if(bb == null) {
			bb = new AxisAlignedBB(
					pos.getX() - 10,
					pos.getY(),
					pos.getZ() - 10,
					pos.getX() + 11,
					pos.getY() + 7,
					pos.getZ() + 11
					);
		}

		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

    private boolean isFluidSide(EnumFacing side) {
        if(side == null) return true;
        ForgeDirection lateral = ForgeDirection.getOrientation(getBlockMetadata()).getOpposite().getRotation(ForgeDirection.UP);
        return side == lateral.toEnumFacing() || side == lateral.getOpposite().toEnumFacing();
    }
    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, EnumFacing side) {
        if(capability == net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && isFluidSide(side)) return true;
        return super.hasCapability(capability, side);
    }
    @Override public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, EnumFacing side) {
        if(capability == net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && isFluidSide(side))
            return net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new com.hbm.capability.NTMFluidHandlerWrapper(this, side == null ? null : pos));
        return super.getCapability(capability, side);
    }

    @Override public FluidTankNTM getTankToPaste() { return tank; }
}
