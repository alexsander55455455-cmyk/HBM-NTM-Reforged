package com.hbm.tileentity.machine;

import com.hbm.api.fluid.IFluidStandardReceiver;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.container.ContainerAMSEmitter;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GUIAMSEmitter;
import com.hbm.items.ModItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

@AutoRegister
public class TileEntityAMSEmitter extends TileEntityMachineBase implements ITickable, IFluidStandardReceiver, IGUIProvider {

	public long power = 0;
	public static final long maxPower = 100000000;
	public int efficiency = 0;
	public static final int maxEfficiency = 100;
	public int heat = 0;
	public static final int maxHeat = 2500;
	public int warning = 0;
	public boolean locked = false;
	public FluidTankNTM tank;

	private final Random rand = new Random();

	public TileEntityAMSEmitter() {
		super(4, true, false);
		tank = new FluidTankNTM(Fluids.COOLANT, 16000).withOwner(this);
	}

	@Override
	public String getDefaultName() {
		return "container.amsEmitter";
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		power = compound.getLong("power");
		tank.readFromNBT(compound, "tank");
		efficiency = compound.getInteger("efficiency");
		heat = compound.getInteger("heat");
		locked = compound.getBoolean("locked");
		warning = compound.getInteger("warning");
	}

	@NotNull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("power", power);
		tank.writeToNBT(compound, "tank");
		compound.setInteger("efficiency", efficiency);
		compound.setInteger("heat", heat);
		compound.setBoolean("locked", locked);
		compound.setInteger("warning", warning);
		return super.writeToNBT(compound);
	}

	@Override
	public void update() {
		if(!world.isRemote) {
			tank.loadTank(0, 1, inventory);

			if(!locked) {
				if(power > 0) {
					efficiency = Math.round(calcEffect(power, heat - (maxHeat / 2)) * 100);
					power -= (long) Math.ceil(power * 0.025);
					warning = 0;
				} else {
					efficiency = 0;
					warning = 1;
				}

				FluidType tankType = tank.getTankType();

				if(tankType == Fluids.CRYOGEL) {
					if(tank.getFill() >= 15) {
						if(heat > 0) {
							tank.setFill(tank.getFill() - 15);
						}

						if(heat <= maxHeat / 2) {
							if(efficiency > 0)
								heat += efficiency;
							else
								for(int i = 0; i < 10; i++)
									if(heat > 0)
										heat--;
						}

						for(int i = 0; i < 10; i++)
							if(heat > maxHeat / 2)
								heat--;
					} else {
						heat += efficiency;
					}
				} else if(tankType == Fluids.COOLANT) {
					if(tank.getFill() >= 15) {
						if(heat > 0) {
							tank.setFill(tank.getFill() - 15);
						}

						if(heat <= maxHeat / 4) {
							if(efficiency > 0)
								heat += efficiency;
							else
								for(int i = 0; i < 5; i++)
									if(heat > 0)
										heat--;
						}

						for(int i = 0; i < 5; i++)
							if(heat > maxHeat / 4)
								heat--;
					} else {
						heat += efficiency;
					}
				} else if(tankType == Fluids.WATER) {
					if(tank.getFill() >= 45) {
						if(heat > 0) {
							tank.setFill(tank.getFill() - 45);
						}

						if(heat <= maxHeat * 0.85) {
							if(efficiency > 0)
								heat += efficiency;
							else
								for(int i = 0; i < 2; i++)
									if(heat > 0)
										heat--;
						}

						for(int i = 0; i < 2; i++)
							if(heat > maxHeat * 0.85)
								heat--;
					} else {
						heat += efficiency;
					}
				} else {
					heat += efficiency;
					warning = 2;
				}

				if(!inventory.getStackInSlot(2).isEmpty()) {
					if(inventory.getStackInSlot(2).getItem() != ModItems.ams_muzzle) {
						this.efficiency = 0;
						this.warning = 2;
					}
				} else {
					this.efficiency = 0;
					this.warning = 2;
				}

				if(tank.getFill() <= 5 || heat > maxHeat * 0.9)
					warning = 2;

				if(heat > maxHeat) {
					heat = maxHeat;
					locked = true;
					ExplosionLarge.spawnBurst(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 36, 3);
					ExplosionLarge.spawnBurst(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 36, 2.5);
					ExplosionLarge.spawnBurst(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 36, 2);
					ExplosionLarge.spawnBurst(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 36, 1.5);
					ExplosionLarge.spawnBurst(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 36, 1);
					this.world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HBMSoundHandler.oldExplosion, SoundCategory.BLOCKS, 10.0F, 1);
					this.world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HBMSoundHandler.shutdown, SoundCategory.BLOCKS, 10.0F, 1.0F);
				}

				power = Library.chargeTEFromItems(inventory, 3, power, maxPower);
			} else {
				ExplosionLarge.spawnBurst(world, pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5, rand.nextInt(10), 1);

				efficiency = 0;
				power = 0;
				warning = 3;
			}

			networkPackNT(15);
		}
	}

	private float gauss(float a, float x) {
		double amplifier = 0.10;
		return (float) ((1 / Math.sqrt(a * Math.PI)) * Math.pow(Math.E, -1 * Math.pow(x, 2) / amplifier));
	}

	private float calcEffect(float a, float x) {
		return (float) (gauss(1 / a, x / maxHeat) * Math.sqrt(Math.PI * 2) / (Math.sqrt(2) * Math.sqrt(maxPower)));
	}

	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}

	public int getEfficiencyScaled(int i) {
		return (efficiency * i) / maxEfficiency;
	}

	public int getHeatScaled(int i) {
		return (heat * i) / maxHeat;
	}

	public boolean isValidFluid(FluidType type) {
		return type == Fluids.WATER || type == Fluids.COOLANT || type == Fluids.CRYOGEL;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public FluidTankNTM[] getReceivingTanks() {
		return new FluidTankNTM[] { tank };
	}

	@Override
	public FluidTankNTM[] getAllTanks() {
		return new FluidTankNTM[] { tank };
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeInt(efficiency);
		buf.writeInt(heat);
		buf.writeBoolean(locked);
		buf.writeInt(warning);
		tank.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		power = buf.readLong();
		efficiency = buf.readInt();
		heat = buf.readInt();
		locked = buf.readBoolean();
		warning = buf.readInt();
		tank.deserialize(buf);
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerAMSEmitter(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIAMSEmitter(player.inventory, this);
	}
}