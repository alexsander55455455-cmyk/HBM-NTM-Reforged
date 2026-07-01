package com.hbm.tileentity;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.interfaces.IKeypadHandler;
import com.hbm.interfaces.Spaghetti;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntitySlidingBlastDoor;
import com.hbm.util.Keypad;
import com.hbm.util.KeypadClient;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

@Spaghetti("Weird stuff to make it work property client side")
@AutoRegister
public class TileEntitySlidingBlastDoorKeypad extends TileEntityKeypadBase {

	private static final String NBT_CORE_X = "linkedCoreX";
	private static final String NBT_CORE_Y = "linkedCoreY";
	private static final String NBT_CORE_Z = "linkedCoreZ";

	public boolean foundCore = false;
	private BlockPos linkedCore = null;

	public void setLinkedCore(BlockPos core) {
		this.linkedCore = core == null ? null : core.toImmutable();
		this.foundCore = false;
	}

	@org.jetbrains.annotations.Nullable
	public BlockPos getLinkedCore() {
		return linkedCore;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void setupKeypadClient() {
	}
	
	@SideOnly(Side.CLIENT)
	public void setupKeypadClient(BlockPos corePos, int meta){
		ForgeDirection dir = ForgeDirection.getOrientation(meta);
		if(((BlockDummyable)getBlockType()).hasExtra(getBlockMetadata())){
			dir = dir.getOpposite();
		}
		float rot = dir.getRotationRadians();
		Matrix4f mat = new Matrix4f();
		mat.rotate(rot, new Vector3f(0, 1, 0));
		mat.translate(new Vector3f(-0.03125F, 0.27812F, -0.46875F));
		mat.scale(new Vector3f(0.35F, 0.4125F, 0.25F));
		keypad = new KeypadClient(this, mat);
	}
	
	@Override
	public void update() {
		super.update();
		if (world.isRemote) {
			if (world.getBlockState(pos).getBlock() != ModBlocks.sliding_blast_door_keypad) {
				foundCore = false;
				return;
			}
			if (!foundCore) {
				BlockPos corePos = linkedCore;
				if (corePos == null) {
					int[] found = ((BlockDummyable) this.getBlockType()).findCore(world, pos.getX(), pos.getY(), pos.getZ());
					if (found == null) {
						return;
					}
					corePos = new BlockPos(found[0], found[1], found[2]);
				}
				if (world.getBlockState(corePos).getBlock() != ModBlocks.sliding_blast_door_2) {
					return;
				}
				int meta = world.getBlockState(corePos).getValue(BlockDummyable.META) - BlockDummyable.offset;
				setupKeypadClient(corePos, meta);
				foundCore = true;
			}
		}
	}

	@Override
	public void invalidate() {
		foundCore = false;
		super.invalidate();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		if (linkedCore != null) {
			compound.setInteger(NBT_CORE_X, linkedCore.getX());
			compound.setInteger(NBT_CORE_Y, linkedCore.getY());
			compound.setInteger(NBT_CORE_Z, linkedCore.getZ());
		}
		return super.writeToNBT(compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		if (compound.hasKey(NBT_CORE_X)) {
			linkedCore = new BlockPos(compound.getInteger(NBT_CORE_X), compound.getInteger(NBT_CORE_Y), compound.getInteger(NBT_CORE_Z));
		} else {
			linkedCore = null;
		}
		foundCore = false;
	}
	
	@Override
	public void keypadActivated() {
		Block b = this.getBlockType();
		if(b instanceof BlockDummyable){
			int[] corePos = ((BlockDummyable) b).findCore(world, pos.getX(), pos.getY(), pos.getZ());
			TileEntity core = world.getTileEntity(new BlockPos(corePos[0], corePos[1], corePos[2]));
			if(core instanceof TileEntitySlidingBlastDoor){
				((TileEntitySlidingBlastDoor) core).toggle();
			}
		}
	}
	
	@Override
	public void passwordSet() {
		Block b = this.getBlockType();
		if(b instanceof BlockDummyable){
			int[] corePos = ((BlockDummyable) b).findCore(world, pos.getX(), pos.getY(), pos.getZ());
			TileEntity core = world.getTileEntity(new BlockPos(corePos[0], corePos[1], corePos[2]));
			if(core instanceof TileEntitySlidingBlastDoor){
				((TileEntitySlidingBlastDoor) core).keypadLocked = true;
				BlockPos otherPad = this.pos.subtract(new BlockPos(corePos[0], corePos[1], corePos[2]));
				otherPad = new BlockPos(-otherPad.getX(), otherPad.getY(), -otherPad.getZ()).add(new BlockPos(corePos[0], corePos[1], corePos[2]));
				if(world.getTileEntity(otherPad) instanceof IKeypadHandler){
					Keypad pad = ((IKeypadHandler)world.getTileEntity(otherPad)).getKeypad();
					pad.clearCode();
					pad.isSettingCode = false;
					pad.storedCode = this.keypad.storedCode;
				}
			}
		}
	}
}
