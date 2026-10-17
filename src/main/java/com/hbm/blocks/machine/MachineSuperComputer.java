package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineSuperComputer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MachineSuperComputer extends BlockDummyable {
    public MachineSuperComputer(String name) { super(Material.IRON, name); }

    @Override public TileEntity createNewTileEntity(World world, int meta) {
        if(meta >= 12) return new TileEntityMachineSuperComputer();
        if(meta >= 6) return new TileEntityProxyCombo().inventory().power().fluid();
        return null;
    }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float x, float y, float z) {
        return standardOpenBehavior(world, pos, player, 0);
    }
    @Override public int[] getDimensions() { return new int[] {5, 0, 3, 3, 3, 3}; }
    @Override public int getOffset() { return 8; }
    public int[][] getAllDimensions() {
        return new int[][] {getDimensions(), {6, -6, 3, 3, 1, 1}, {6, -6, 1, 1, 3, 3}, {7, -7, 1, 1, 1, 1}, {2, 0, -3, 8, 1, 1}};
    }
    @Override public boolean checkRequirement(World world, int x, int y, int z, ForgeDirection dir, int offset) {
        for(int[] dimensions : getAllDimensions())
            if(!MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * offset, y, z + dir.offsetZ * offset, dimensions, x, y, z, dir)) return false;
        return true;
    }
    @Override protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(world, x, y, z, dir, offset);
        int cx = x + dir.offsetX * offset, cz = z + dir.offsetZ * offset;
        int[][] dimensions = getAllDimensions();
        for(int i = 1; i < dimensions.length; i++) MultiblockHandlerXR.fillSpace(world, cx, y, cz, dimensions[i], this, dir);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(world, cx + dir.offsetX * 8, y, cz + dir.offsetZ * 8);
        for(int distance : new int[] {5, 7}) for(int side : new int[] {-1, 1})
            makeExtra(world, cx + dir.offsetX * distance + rot.offsetX * side, y, cz + dir.offsetZ * distance + rot.offsetZ * side);
    }
}
