package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerMachineSuperComputer;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GUIMachineSuperComputer;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachineSuperComputer;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.BobMathUtil;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import com.hbm.lib.ForgeDirection;

@com.hbm.interfaces.AutoRegister
public class TileEntityMachineSuperComputer extends TileEntityMachineBase implements net.minecraft.util.ITickable, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver, IGUIProvider {

	public FluidTankNTM inputTank;
	public FluidTankNTM outputTank;

	public long power;
	public long maxPower = 100_000;
	public boolean didProcess = false;

	public ModuleMachineSuperComputer computerModule;
    private final com.hbm.module.portmanager.ModulePortManPower powerPorts = new com.hbm.module.portmanager.ModulePortManPower(this).onlyInput();
    private com.hbm.module.portmanager.ModulePortManFluidAdaptive fluidPorts;

	public TileEntityMachineSuperComputer() {
		super(8, true, true);
		this.inputTank = new FluidTankNTM(Fluids.NONE, 4_000).withOwner(this);
		this.outputTank = new FluidTankNTM(Fluids.NONE, 4_000).withOwner(this);

		this.computerModule = new ModuleMachineSuperComputer(0, this, inventory)
				.itemInput(2).itemOutput(5)
				.fluidInput(inputTank).fluidOutput(outputTank);
        this.fluidPorts = new com.hbm.module.portmanager.ModulePortManFluidAdaptive(this).setInputTanks(inputTank).setOutputTanks(outputTank);
	}

	protected PortDef[] cachedPorts;

	public PortDef[] getPorts() {
		if(cachedPorts == null) {
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
			ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

			cachedPorts = new PortDef[] {
					PortDef.make(pos.getX() + dir.offsetX * 8, pos.getY(), pos.getZ() + dir.offsetZ * 8, dir),
					PortDef.make(pos.getX() + dir.offsetX * 7 + rot.offsetX, pos.getY(), pos.getZ() + dir.offsetZ * 7 + rot.offsetZ, rot),
					PortDef.make(pos.getX() + dir.offsetX * 7 - rot.offsetX, pos.getY(), pos.getZ() + dir.offsetZ * 7 - rot.offsetZ, rot.getOpposite()),
					PortDef.make(pos.getX() + dir.offsetX * 5 + rot.offsetX, pos.getY(), pos.getZ() + dir.offsetZ * 5 + rot.offsetZ, rot),
					PortDef.make(pos.getX() + dir.offsetX * 5 - rot.offsetX, pos.getY(), pos.getZ() + dir.offsetZ * 5 - rot.offsetZ, rot.getOpposite()),
			};
		}
		return cachedPorts;
	}

	@Override
	public String getDefaultName() {
		return "container.machineSuperComputer";
	}

	@Override
	public void update() {

		if(maxPower <= 0) this.maxPower = 1_000_000;

		if(!world.isRemote) {

			this.powerPorts.update(getPorts());
            this.fluidPorts.update(getPorts());

			GenericRecipe recipe = computerModule.getRecipe();
			if(recipe != null) {
				this.maxPower = recipe.power * 100;
			}
			this.maxPower = BobMathUtil.max(this.power, this.maxPower, 100_000);
			this.power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

			this.computerModule.update(1D, 1D, true, inventory.getStackInSlot(1));
			this.didProcess = this.computerModule.didProcess;
			if(this.computerModule.markDirty) this.markDirty();

			this.networkPackNT(100);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		this.inputTank.serialize(buf);
		this.outputTank.serialize(buf);
		buf.writeLong(power);
		buf.writeLong(maxPower);
		buf.writeBoolean(didProcess);
		this.computerModule.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.inputTank.deserialize(buf);
		this.outputTank.deserialize(buf);
		this.power = buf.readLong();
		this.maxPower = buf.readLong();
		this.didProcess = buf.readBoolean();
		this.computerModule.deserialize(buf);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.inputTank.readFromNBT(nbt, "i");
		this.outputTank.readFromNBT(nbt, "o");
		this.power = nbt.getLong("power");
		this.maxPower = nbt.getLong("maxPower");
		this.computerModule.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		this.inputTank.writeToNBT(nbt, "i");
		this.outputTank.writeToNBT(nbt, "o");
		nbt.setLong("power", power);
		nbt.setLong("maxPower", maxPower);
		this.computerModule.writeToNBT(nbt);
        return nbt;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == 0 && stack.getItem() instanceof IBatteryItem) return true;
		if(slot == 1 && stack.getItem() == ModItems.blueprints) return true;
		if(this.computerModule.isItemValid(slot, stack)) return true; // recipe input crap
		return false;
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int j) {
		return i >= 5 || this.computerModule.isSlotClogged(i);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(net.minecraft.util.EnumFacing side) {
		return new int[] {2, 3, 4, 5, 6, 7};
	}

	@Override public long getPower() { return power; }
	@Override public void setPower(long power) { this.power = power; }
	@Override public long getMaxPower() { return maxPower; }

	@Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {inputTank}; }
	@Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {outputTank}; }
	@Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {inputTank, outputTank}; }

	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return new ContainerMachineSuperComputer(player.inventory, getCheckedInventory()); }
	@Override @SideOnly(Side.CLIENT) public net.minecraft.client.gui.GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUIMachineSuperComputer(player.inventory, this); }

	@Override public boolean hasPermission(EntityPlayer player) { return this.isUseableByPlayer(player); }

	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data.hasKey("index") && data.hasKey("selection")) {
			int index = data.getInteger("index");
			String selection = data.getString("selection");
			if(index == 0) {
				this.computerModule.setRecipe(selection, false);
				this.markChanged();
			}
		}
	}

	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null) bb = new AxisAlignedBB(pos.getX() - 8, pos.getY(), pos.getZ() - 8, pos.getX() + 9, pos.getY() + 9, pos.getZ() + 9);
		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
    @Override public void invalidate() {
        if(world != null && !world.isRemote) { powerPorts.destroy(); fluidPorts.destroy(); }
        super.invalidate();
    }
    @Override public void onChunkUnload() {
        if(world != null && !world.isRemote) { powerPorts.destroy(); fluidPorts.destroy(); }
        super.onChunkUnload();
    }
}
