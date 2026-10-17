package com.hbm.tileentity.machine;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerMachineRockMill;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GUIMachineRockMill;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.modules.machine.ModuleMachineRockMill;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Vec3NT;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@com.hbm.interfaces.AutoRegister
public class TileEntityMachineRockMill extends TileEntityMachineBase implements net.minecraft.util.ITickable, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver, IGUIProvider {

	public FluidTankNTM[] inputTanks;
	public FluidTankNTM[] outputTanks;
	
	public long power;
	public long maxPower = 2_500;
	public boolean didProcess = false;
	
	public float rotation;
	public float prevRotation;
	
	public float rotationSpeed = 0F;
	public static final float ACCELERATION = 0.1F;
	public static final float MAX_SPEED = 15F;
	
	public boolean frame = false;

	public ModuleMachineRockMill rockMillModule;
	private final com.hbm.module.portmanager.ModulePortManPower powerPorts = new com.hbm.module.portmanager.ModulePortManPower(this).onlyInput();
	private com.hbm.module.portmanager.ModulePortManFluidAdaptive fluidPorts;

	public TileEntityMachineRockMill() {
		super(8, true, true);
		
		this.inputTanks = new FluidTankNTM[1];
		this.outputTanks = new FluidTankNTM[1];
		
		this.inputTanks[0] = new FluidTankNTM(Fluids.NONE, 4_000).withOwner(this);
		this.outputTanks[0] = new FluidTankNTM(Fluids.NONE, 4_000).withOwner(this);
		
		this.rockMillModule = new ModuleMachineRockMill(0, this, inventory)
				.itemInput(2).itemOutput(5)
				.fluidInput(inputTanks[0]).fluidOutput(outputTanks[0]);
		this.fluidPorts = new com.hbm.module.portmanager.ModulePortManFluidAdaptive(this).setInputTanks(inputTanks).setOutputTanks(outputTanks);
	}
	
	protected PortDef[] cachedPorts;

	public PortDef[] getPorts() {
		if(cachedPorts == null) {
			cachedPorts = new PortDef[] {
					PortDef.make(pos.getX() + 2, pos.getY(), pos.getZ() + 1, Library.POS_X),
					PortDef.make(pos.getX() + 2, pos.getY(), pos.getZ() - 1, Library.POS_X),
					PortDef.make(pos.getX() - 2, pos.getY(), pos.getZ() + 1, Library.NEG_X),
					PortDef.make(pos.getX() - 2, pos.getY(), pos.getZ() - 1, Library.NEG_X),
					PortDef.make(pos.getX() + 1, pos.getY(), pos.getZ() + 2, Library.POS_Z),
					PortDef.make(pos.getX() - 1, pos.getY(), pos.getZ() + 2, Library.POS_Z),
					PortDef.make(pos.getX() + 1, pos.getY(), pos.getZ() - 2, Library.NEG_Z),
					PortDef.make(pos.getX() - 1, pos.getY(), pos.getZ() - 2, Library.NEG_Z),
			};
		}
		return cachedPorts;
	}

	@Override
	public String getDefaultName() {
		return "container.machineRockMill";
	}

	@Override
	public void update() {
		
		if(maxPower <= 0) this.maxPower = 2_500;
		
		if(!world.isRemote) {

			this.powerPorts.update(getPorts());
			this.fluidPorts.update(getPorts());
			
			GenericRecipe recipe = rockMillModule.getRecipe();
			if(recipe != null) {
				this.maxPower = recipe.power * 100;
			}
			
			this.maxPower = BobMathUtil.max(this.power, this.maxPower, 2_500);
			this.power = Library.chargeTEFromItems(inventory, 0, power, maxPower);
			
			this.rockMillModule.update(1D, 1D, true, inventory.getStackInSlot(1));
			this.didProcess = this.rockMillModule.didProcess;
			if(this.rockMillModule.markDirty) this.markDirty();
				
			if(this.didProcess && (world.getTotalWorldTime() + pos.toLong()) % 3 == 0) {
				net.minecraft.util.SoundEvent sound = Blocks.STONE.getSoundType().getStepSound();
				
				if(recipe != null && recipe.getIcon().getItem() instanceof ItemBlock && ((ItemBlock) recipe.getIcon().getItem()).getBlock() != null) {
					sound = ((ItemBlock) recipe.getIcon().getItem()).getBlock().getSoundType().getStepSound();
				}
				
				world.playSound(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, sound, net.minecraft.util.SoundCategory.BLOCKS, this.getVolume(1.0F), 0.75F);
			}
			
			this.networkPackNT(100);
			
		} else {
			
			this.prevRotation = this.rotation;
			
			this.rotationSpeed += this.ACCELERATION * (this.didProcess ? 1 : -1);
			this.rotationSpeed = MathHelper.clamp(this.rotationSpeed, 0F, MAX_SPEED);
			
			this.rotation += this.rotationSpeed;
			
			if(this.rotation >= 360F) {
				this.prevRotation -= 360F;
				this.rotation -= 360F;
			}

			if(world.getTotalWorldTime() % 20 == 0) {
				frame = !world.isAirBlock(pos.up(3));
			}
			
			if(this.didProcess && MainRegistry.proxy.me().getDistanceSq(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5) < 35 * 35) {
				
				GenericRecipe recipe = rockMillModule.getRecipe();
				Block block = Blocks.GRAVEL;
				int meta = 0;
				
				if(recipe != null) {
					if(recipe.getIcon().getItem() instanceof ItemBlock) {
						block = Block.getBlockFromItem(recipe.getIcon().getItem());
						meta = recipe.getIcon().getItemDamage();
					}
				}
				
				Vec3NT vec = new Vec3NT(1, 0, 0);
				vec.rotateAroundYDeg(world.rand.nextDouble() * 360);
				
				double speed = 0.125D;
				
				NBTTagCompound data = new NBTTagCompound();
				data.setString("type", "vanillaExt");
				data.setString("mode", "blockdust");
				data.setInteger("block", Block.getIdFromBlock(block));
				data.setByte("meta", (byte) meta);
				data.setDouble("mX", vec.x * speed);
				data.setDouble("mY", vec.y * speed - 0.1D);
				data.setDouble("mZ", vec.z * speed);
				data.setDouble("posX", pos.getX() + 0.5 + vec.x * 2.25);
				data.setDouble("posY", pos.getY() + 1.5);
				data.setDouble("posZ", pos.getZ() + 0.5 + vec.z * 2.25);
				MainRegistry.proxy.effectNT(data);
			}
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		for(FluidTankNTM tank : inputTanks) tank.serialize(buf);
		for(FluidTankNTM tank : outputTanks) tank.serialize(buf);
		buf.writeLong(power);
		buf.writeLong(maxPower);
		buf.writeBoolean(didProcess);
		this.rockMillModule.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		for(FluidTankNTM tank : inputTanks) tank.deserialize(buf);
		for(FluidTankNTM tank : outputTanks) tank.deserialize(buf);
		this.power = buf.readLong();
		this.maxPower = buf.readLong();
		this.didProcess = buf.readBoolean();
		this.rockMillModule.deserialize(buf);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		this.inputTanks[0].readFromNBT(nbt, "i" + 0);
		this.outputTanks[0].readFromNBT(nbt, "o" + 0);

		this.power = nbt.getLong("power");
		this.maxPower = nbt.getLong("maxPower");
		this.rockMillModule.readFromNBT(nbt);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		
		this.inputTanks[0].writeToNBT(nbt, "i" + 0);
		this.outputTanks[0].writeToNBT(nbt, "o" + 0);

		nbt.setLong("power", power);
		nbt.setLong("maxPower", maxPower);
		this.rockMillModule.writeToNBT(nbt);
		return nbt;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == 0) return stack.getItem() instanceof IBatteryItem; // battery
		if(slot == 1 && stack.getItem() == ModItems.blueprints) return true;
		if(this.rockMillModule.isItemValid(slot, stack)) return true; // recipe input crap
		return false;
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int j) {
		return (i >= 5 && i <= 7) || this.rockMillModule.isSlotClogged(i);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(net.minecraft.util.EnumFacing side) {
		return new int[] {2, 3, 4, 5, 6, 7};
	}

	@Override public long getPower() { return power; }
	@Override public void setPower(long power) { this.power = power; }
	@Override public long getMaxPower() { return maxPower; }

	@Override public FluidTankNTM[] getReceivingTanks() { return inputTanks; }
	@Override public FluidTankNTM[] getSendingTanks() { return outputTanks; }
	@Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {inputTanks[0], outputTanks[0]}; }

	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return new ContainerMachineRockMill(player.inventory, this.getCheckedInventory()); }
	@Override @SideOnly(Side.CLIENT) public net.minecraft.client.gui.GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUIMachineRockMill(player.inventory, this); }

	@Override public boolean hasPermission(EntityPlayer player) { return this.isUseableByPlayer(player); }

	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data.hasKey("index") && data.hasKey("selection")) {
			int index = data.getInteger("index");
			String selection = data.getString("selection");
			if(index == 0) {
				this.rockMillModule.setRecipe(selection, false);
				this.markChanged();
			}
		}
	}
	
	AxisAlignedBB bb = null;
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null) bb = new AxisAlignedBB(pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 3, pos.getY() + 3, pos.getZ() + 3);
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
