package com.hbm.world.generator.room;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsComponent;
import com.hbm.itempool.ItemPoolsLegacy;
import com.hbm.itempool.ItemPoolsSingle;
import com.hbm.world.generator.CellularDungeon;
import com.hbm.world.generator.VaultDungeonPlacer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VaultDungeonRoomPower extends VaultDungeonRoomElevator {
    public VaultDungeonRoomPower(CellularDungeon parent) {
        super(parent, 3);
    }

    @Override
    public void generateRoom(World world, int x, int y, int z) {
        generateRoom(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    public void generateRoom(VaultDungeonPlacer placer, int x, int y, int z) {
        int centerX = x + getW() / 2;
        int centerZ = z + getW() / 2;

        placer.setBlockState(new BlockPos(centerX, y + 2, centerZ), ModBlocks.reactor_research.getDefaultState());
        placer.setBlockState(new BlockPos(centerX - 1, y + 2, centerZ), ModBlocks.machine_battery.getDefaultState());
        placer.setBlockState(new BlockPos(centerX + 1, y + 2, centerZ), ModBlocks.machine_battery.getDefaultState());

        placer.generateWalls(centerX - 3, y + 2, centerZ - 3, 7, getH() - 4, 7, parent.wall);
        placer.generateWalls(centerX - 3, y + 3, centerZ - 3, 7, 1, 7, getLine(x, z));

        placer.generateWalls(centerX - 3, y + 2, centerZ, 1, getH() - 4, 1, air);
        placer.generateWalls(centerX, y + 2, centerZ - 3, 1, getH() - 4, 1, air);
        placer.generateWalls(centerX, y + 2, centerZ + 3, 1, getH() - 4, 1, air);
        placer.generateWalls(centerX + 3, y + 2, centerZ, 1, getH() - 4, 1, air);
    }

    @Override
    public boolean spawnGlow() {
        return false;
    }

    @Override
    public void placeLoot(World world, int x, int y, int z) {
        placeLoot(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    public void placeLoot(VaultDungeonPlacer placer, int x, int y, int z) {
        int centerX = x + getW() / 2;
        int centerZ = z + getW() / 2;

        placer.placeLootChest(new BlockPos(centerX, y + 2, centerZ + 2), EnumFacing.SOUTH,
                ItemPool.getPool(ItemPoolsComponent.POOL_MACHINE_PARTS), 10);
        placer.placeLootChest(new BlockPos(centerX - 2, y + 2, centerZ), EnumFacing.EAST,
                ItemPool.getPool(ItemPoolsLegacy.POOL_EXPENSIVE), 9);
        placer.placeLootChest(new BlockPos(centerX + 2, y + 2, centerZ), EnumFacing.WEST,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_STANDARD), 8);
        placer.placeLootChest(new BlockPos(centerX, y + 2, centerZ - 2), EnumFacing.NORTH,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_DUNGEON), 10);
    }
}