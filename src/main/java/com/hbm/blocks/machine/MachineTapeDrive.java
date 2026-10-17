package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityMachineTapeDrive;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class MachineTapeDrive extends BlockMachineBase {
    public MachineTapeDrive(String name) { super(Material.IRON, 0, name); }
    @Override protected boolean rotatable() { return true; }
    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileEntityMachineTapeDrive(); }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.ENTITYBLOCK_ANIMATED; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        switch(state.getValue(BlockHorizontal.FACING)) {
            case EAST: return new AxisAlignedBB(0, 0, 0, .75, 1, 1);
            case SOUTH: return new AxisAlignedBB(0, 0, 0, 1, 1, .75);
            case WEST: return new AxisAlignedBB(.25, 0, 0, 1, 1, 1);
            case NORTH: return new AxisAlignedBB(0, 0, .25, 1, 1, 1);
            default: return FULL_BLOCK_AABB;
        }
    }
}
