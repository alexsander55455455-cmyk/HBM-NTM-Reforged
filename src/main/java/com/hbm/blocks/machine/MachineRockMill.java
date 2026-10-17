package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineRockMill;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MachineRockMill extends BlockDummyable {
    public MachineRockMill(String name) { super(Material.IRON, name); }

    @Override public TileEntity createNewTileEntity(World world, int meta) {
        if (meta >= 12) return new TileEntityMachineRockMill();
        if (meta >= 6) return new TileEntityProxyCombo().inventory().power().fluid();
        return null;
    }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                               EnumHand hand, EnumFacing face, float x, float y, float z) {
        return standardOpenBehavior(world, pos.getX(), pos.getY(), pos.getZ(), player, 0);
    }

    @Override public int[] getDimensions() { return new int[] {2, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }

    @Override protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(world, x, y, z, dir, offset);
        x += dir.offsetX * offset;
        z += dir.offsetZ * offset;
        // The eight original side ports form a square, invariant under quarter turns.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if ((Math.abs(dx) == 2 && Math.abs(dz) == 1) || (Math.abs(dx) == 1 && Math.abs(dz) == 2)) {
                makeExtra(world, x + dx, y, z + dz);
            }
        }
    }
}
