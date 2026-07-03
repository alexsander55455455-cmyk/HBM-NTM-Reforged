package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluid.IFluidStandardTransceiver;
import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.container.ContainerMachineUUCreator;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GUIMachineUUCreator;
import com.hbm.inventory.slot.SlotBattery;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.IConnectionAnchors;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

@AutoRegister
public class TileEntityMachineUUCreator extends TileEntityMachineBase implements ITickable, IEnergyReceiverMK2, IFluidStandardTransceiver, IGUIProvider, IFluidCopiable, IConnectionAnchors {

	public int[] log = new int[20];
	public static final long rfPerMbOfUU = 1_000_000L;
	public FluidTankNTM tank;
	public long power;
	public static final long maxPower = 5_000_000_000_000_000L;
	public double producedmb = 0;
	public boolean isOn;

	public TileEntityMachineUUCreator() {
		super(4, true, true);
		tank = new FluidTankNTM(Fluids.UU_MATTER, 2_000_000_000).withOwner(this);
	}

	@Override
	public String getDefaultName() {
		return "container.uuCreator";
	}

	@Override
	public void update() {
		if(!world.isRemote) {
			if(world.getTotalWorldTime() % 10 == 0)
				updateConnections();

			power = Library.chargeTEFromItems(inventory, 0, power, maxPower);
			tank.unloadTank(2, 3, inventory);

			int loggedProducedMB = 0;
			if(isOn && power >= rfPerMbOfUU && tank.getFill() < tank.getMaxFill()) {
				int producedUUmB = (int)Math.min(power / rfPerMbOfUU, tank.getMaxFill() - tank.getFill());
				if(producedUUmB > 0) {
					tank.setFill(tank.getFill() + producedUUmB);
					power -= producedUUmB * rfPerMbOfUU;
					loggedProducedMB = producedUUmB;
					markDirty();
				}
			}

			for(int i = 1; i < this.log.length; i++)
				this.log[i - 1] = this.log[i];
			this.log[this.log.length - 1] = loggedProducedMB;
			producedmb = getAvgUU();

			networkPackNT(250);
		}
	}

	public double getAvgUU() {
		long sum = 0;
		for(int i = 0; i < this.log.length; i++)
			sum += this.log[i];
		return sum / (double)this.log.length;
	}

	@Override
	public void handleButtonPacket(int value, int meta) {
		if(meta == 0)
			this.isOn = !this.isOn;
	}

	public long getPowerScaled(long i) {
		if(maxPower <= 0)
			return 0;
		return (power * i) / maxPower;
	}

	private void updateConnections() {
		for(DirPos pos : getConPos()) {
			this.trySubscribe(world, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir());
			if(tank.getFill() > 0)
				this.sendFluid(tank, world, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir());
		}
	}

	@Override
	public DirPos[] getConPos() {
		return new DirPos[] {
				new DirPos(pos.getX(), pos.getY() + 3, pos.getZ(), Library.POS_Y),
				new DirPos(pos.getX() + 2, pos.getY() + 3, pos.getZ(), Library.POS_Y),
				new DirPos(pos.getX() - 2, pos.getY() + 3, pos.getZ(), Library.POS_Y),
				new DirPos(pos.getX(), pos.getY() + 3, pos.getZ() + 2, Library.POS_Y),
				new DirPos(pos.getX(), pos.getY() + 3, pos.getZ() - 2, Library.POS_Y),
				new DirPos(pos.getX(), pos.getY() - 1, pos.getZ(), Library.NEG_Y),
				new DirPos(pos.getX() + 2, pos.getY() - 1, pos.getZ(), Library.NEG_Y),
				new DirPos(pos.getX() - 2, pos.getY() - 1, pos.getZ(), Library.NEG_Y),
				new DirPos(pos.getX(), pos.getY() - 1, pos.getZ() + 2, Library.NEG_Y),
				new DirPos(pos.getX(), pos.getY() - 1, pos.getZ() - 2, Library.NEG_Y),
		};
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(
				pos.getX() - 3,
				pos.getY(),
				pos.getZ() - 3,
				pos.getX() + 4,
				pos.getY() + 3,
				pos.getZ() + 4
		);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeBoolean(isOn);
		buf.writeLong(power);
		buf.writeDouble(producedmb);
		tank.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		isOn = buf.readBoolean();
		power = buf.readLong();
		producedmb = buf.readDouble();
		tank.deserialize(buf);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		isOn = nbt.getBoolean("isOn");
		power = nbt.getLong("power");
		tank.readFromNBT(nbt, "tank");
	}

	@Override
	public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("isOn", isOn);
		nbt.setLong("power", power);
		tank.writeToNBT(nbt, "tank");
		return nbt;
	}

	@Override
	public void setPower(long i) {
		power = i;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

	@Override
	public FluidTankNTM[] getAllTanks() {
		return new FluidTankNTM[] {tank};
	}

	@Override
	public FluidTankNTM[] getReceivingTanks() {
		return new FluidTankNTM[0];
	}

	@Override
	public FluidTankNTM[] getSendingTanks() {
		return new FluidTankNTM[] {tank};
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMachineUUCreator(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineUUCreator(player.inventory, this);
	}
}