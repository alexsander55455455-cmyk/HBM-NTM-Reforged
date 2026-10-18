package com.hbm.blocks.machine;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityLaunchpadLambda;
import com.hbm.lib.ForgeDirection;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class LaunchpadLambda extends BlockDummyable {

	public LaunchpadLambda(String name) {
		super(Material.IRON, name);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityLaunchpadLambda();
		if(meta >= 6) return new TileEntityProxyCombo().inventory().power().fluid();
		return null;
	}

	public int[][] getAllDimensions() {
		return new int[][] {
			new int[] {1, 0, 7, 7, 7, 7},
			new int[] {2, -2, 7, -6, 7, 7},
			new int[] {2, -2, -6, 7, 7, 7},
			new int[] {2, -2, 7, 7, 7, -6},
			new int[] {2, -2, 7, 7, -6, 7},
		};
	}

	@Override public int[] getDimensions() { return new int[] {1, 0, 7, 7, 7, 7}; }
	@Override public int getOffset() { return 7; }
    public int[][] getAllPorts(ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int[][] ports = new int[5][4];
        for(int i = 0; i < ports.length; i++) ports[i] = new int[] {dir.offsetX * 7 - rot.offsetX * (i + 2), 1, dir.offsetZ * 7 - rot.offsetZ * (i + 2), dir.ordinal()};
        return ports;
    }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float x, float y, float z) {
        return standardOpenBehavior(world, pos, player, 0);
    }

	@Override
	public boolean checkRequirement(World world, int x, int y, int z, ForgeDirection dir, int o) {
		if(!super.checkRequirement(world, x, y, z, dir, o)) return false;
		
		x += dir.offsetX * o;
		z += dir.offsetZ * o;

		if(!MultiblockHandlerXR.checkSpace(world, x, y, z, new int[] {2, -2, 7, -6, 7, 7}, x, y, z, dir)) return false;
		if(!MultiblockHandlerXR.checkSpace(world, x, y, z, new int[] {2, -2, -6, 7, 7, 7}, x, y, z, dir)) return false;
		if(!MultiblockHandlerXR.checkSpace(world, x, y, z, new int[] {2, -2, 7, 7, 7, -6}, x, y, z, dir)) return false;
		if(!MultiblockHandlerXR.checkSpace(world, x, y, z, new int[] {2, -2, 7, 7, -6, 7}, x, y, z, dir)) return false;
		
		return true;
	}

	@Override
	public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);
		
		x += dir.offsetX * o;
		z += dir.offsetZ * o;

		MultiblockHandlerXR.fillSpace(world, x, y, z, new int[] {2, -2, 7, -6, 7, 7}, this, dir);
		MultiblockHandlerXR.fillSpace(world, x, y, z, new int[] {2, -2, -6, 7, 7, 7}, this, dir);
		MultiblockHandlerXR.fillSpace(world, x, y, z, new int[] {2, -2, 7, 7, 7, -6}, this, dir);
		MultiblockHandlerXR.fillSpace(world, x, y, z, new int[] {2, -2, 7, 7, -6, 7}, this, dir);
		
		for(int[] offset : getAllPorts(dir)) {
			this.makeExtra(world, x + offset[0], y + offset[1], z + offset[2]);
		}
	}
}
