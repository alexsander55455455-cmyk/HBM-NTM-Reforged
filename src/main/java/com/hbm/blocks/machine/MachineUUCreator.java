package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineUUCreator;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import org.jetbrains.annotations.NotNull;

public class MachineUUCreator extends BlockDummyable {

	public MachineUUCreator(Material mat, String s) {
		super(mat, s);
	}

	@Override
	public TileEntity createNewTileEntity(@NotNull World world, int meta) {
		if(meta >= 12)
			return new TileEntityMachineUUCreator();
		if(meta >= 6)
			return new TileEntityProxyCombo(true, true, true);
		return null;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(world.isRemote)
			return true;
		if(player.isSneaking())
			return false;

		int[] core = this.findCore(world, pos.getX(), pos.getY(), pos.getZ());
		if(core == null)
			return false;

		FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, core[0], core[1], core[2]);
		return true;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {2, 0, 3, 3, 1, 1};
	}

	@Override
	public int getOffset() {
		return 3;
	}

	@Override
	protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);
		MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{2, 0, 2, 2, 2, -2}, this, dir);
		MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{2, 0, 2, 2, -2, 2}, this, dir);
		MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{2, 0, 1, 1, 3, -3}, this, dir);
		MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{2, 0, 1, 1, -3, 3}, this, dir);

		x += dir.offsetX * o;
		z += dir.offsetZ * o;

		this.makeExtra(world, x + 2, y, z);
		this.makeExtra(world, x - 2, y, z);
		this.makeExtra(world, x, y, z + 2);
		this.makeExtra(world, x, y, z - 2);
		this.makeExtra(world, x + 2, y + 2, z);
		this.makeExtra(world, x - 2, y + 2, z);
		this.makeExtra(world, x, y + 2, z + 2);
		this.makeExtra(world, x, y + 2, z - 2);
		this.makeExtra(world, x, y + 2, z);
	}
}