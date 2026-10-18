package com.hbm.blocks.machine;

import java.util.Arrays;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LaunchpadSoyuz extends BlockDummyable {
    public LaunchpadSoyuz(String name) { super(Material.IRON, name); }
    @Override public TileEntity createNewTileEntity(World world, int meta) {
        if(meta >= 12) return new TileEntityLaunchpadSoyuz();
        if(meta >= 6) return new TileEntityProxyCombo().power().fluid().inventory();
        return null;
    }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float x, float y, float z) {
        return standardOpenBehavior(world, pos, player, 0);
    }
    @Override public int getOffset() { return 2; }
    @Override public int[] getDimensions() { return new int[] {2, 0, 2, 2, 2, 2}; }

    // Original filled volumes, shared by clearance and placement. Last three values offset the local anchor.
    public int[][] getAllDimensions() {
        return new int[][] {
            {2, 0, 2, 2, 2, 2}, {3, 0, 2, 1, 1, 2},
            {0, 0, 2, 2, -2, 10, 0, 2, 0}, {0, 0, 10, -2, 2, 10, 0, 2, 0},
            {3, -3, 9, 1, 1, 9},
            {-1, 2, 2, 2, -6, 10, 0, 2, 0}, {-1, 2, 10, -6, 2, 2, 0, 2, 0}, {-1, 2, 10, -6, -6, 10, 0, 2, 0},
            {0, 0, 0, 0, -10, 58, 2, 0, 0}, {1, -1, 0, 0, -56, 58, 2, 0, 0},
            {0, 0, 0, 0, -10, 58, -10, 0, 0}, {1, -1, 0, 0, -56, 58, -10, 0, 0},
            {2, 0, 2, 1, 7, -3}, {2, 0, 2, 1, 7, -3, -7, 0, 0},
            {0, 0, 1, 1, 7, -3, -4, 2, 0}, {1, -1, 5, 5, 7, -3, -4, 2, 0},
            {3, -2, 4, 4, 7, -3, -4, 2, 0}, {6, -4, 3, 3, 7, -3, -4, 2, 0},
            {51, -7, 2, 2, 7, -3, -4, 2, 0}, {7, 0, -2, 3, 2, 2, 0, 0, 5}
        };
    }

    public int[][] getAllPorts(ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int[][] ports = new int[12][4];
        int index = 0;
        for(int forward : new int[] {2, -10}) for(int side : new int[] {1, -1, -7, -9})
            ports[index++] = new int[] {dir.offsetX * forward + rot.offsetX * side, 1, dir.offsetZ * forward + rot.offsetZ * side, (forward == 2 ? dir : dir.getOpposite()).ordinal()};
        for(int forward : new int[] {1, -1, -7, -9})
            ports[index++] = new int[] {dir.offsetX * forward - rot.offsetX * 10, 1, dir.offsetZ * forward - rot.offsetZ * 10, rot.getOpposite().ordinal()};
        return ports;
    }

    @Override public boolean checkRequirement(World world, int x, int y, int z, ForgeDirection dir, int offset) {
        int coreX = x + dir.offsetX * offset, coreZ = z + dir.offsetZ * offset;
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        for(int[] shape : getAllDimensions()) {
            int sx = coreX, sy = y, sz = coreZ;
            if(shape.length == 9) { sx += dir.offsetX * shape[6] + rot.offsetX * shape[8]; sy += shape[7]; sz += dir.offsetZ * shape[6] + rot.offsetZ * shape[8]; }
            if(sy - shape[1] < 0 || sy + shape[0] >= world.getHeight()) return false;
            if(!MultiblockHandlerXR.checkSpace(world, sx, sy, sz, Arrays.copyOf(shape, 6), x, y, z, dir)) return false;
        }
        return true;
    }

    @Override public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int offset) {
        int coreX = x + dir.offsetX * offset, coreZ = z + dir.offsetZ * offset;
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        for(int[] shape : getAllDimensions()) {
            int sx = coreX, sy = y, sz = coreZ;
            if(shape.length == 9) { sx += dir.offsetX * shape[6] + rot.offsetX * shape[8]; sy += shape[7]; sz += dir.offsetZ * shape[6] + rot.offsetZ * shape[8]; }
            MultiblockHandlerXR.fillSpace(world, sx, sy, sz, Arrays.copyOf(shape, 6), this, dir);
        }
        for(int[] port : getAllPorts(dir)) makeExtra(world, coreX + port[0], y + port[1], coreZ + port[2]);
    }
}
