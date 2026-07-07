package com.hbm.world.generator.room;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsComponent;
import com.hbm.itempool.ItemPoolsLegacy;
import com.hbm.itempool.ItemPoolsSingle;
import com.hbm.world.generator.CellularDungeon;
import com.hbm.world.generator.DungeonToolbox;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VaultDungeonRoomPower extends VaultDungeonRoomElevator {
    public VaultDungeonRoomPower(CellularDungeon parent) {
        super(parent, 3);
    }

    @Override
    public void generateRoom(World world, int x, int y, int z) {
        int centerX = x + getW() / 2;
        int centerZ = z + getW() / 2;

        world.setBlockState(new BlockPos(centerX, y + 2, centerZ), ModBlocks.reactor_research.getDefaultState(), 2 | 16);
        world.setBlockState(new BlockPos(centerX - 1, y + 2, centerZ), ModBlocks.machine_battery.getDefaultState(), 2 | 16);
        world.setBlockState(new BlockPos(centerX + 1, y + 2, centerZ), ModBlocks.machine_battery.getDefaultState(), 2 | 16);

        DungeonToolbox.generateWalls(world, centerX - 3, y + 2, centerZ - 3, 7, getH() - 4, 7, parent.wall);
        DungeonToolbox.generateWalls(world, centerX - 3, y + 3, centerZ - 3, 7, 1, 7, getLine(x, z));

        DungeonToolbox.generateWalls(world, centerX - 3, y + 2, centerZ, 1, getH() - 4, 1, air);
        DungeonToolbox.generateWalls(world, centerX, y + 2, centerZ - 3, 1, getH() - 4, 1, air);
        DungeonToolbox.generateWalls(world, centerX, y + 2, centerZ + 3, 1, getH() - 4, 1, air);
        DungeonToolbox.generateWalls(world, centerX + 3, y + 2, centerZ, 1, getH() - 4, 1, air);
    }

    @Override
    public void placeLoot(World world, int x, int y, int z) {
        int centerX = x + getW() / 2;
        int centerZ = z + getW() / 2;

        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(centerX, y + 2, centerZ + 2), EnumFacing.SOUTH,
                ItemPool.getPool(ItemPoolsComponent.POOL_MACHINE_PARTS), 10);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(centerX - 2, y + 2, centerZ), EnumFacing.EAST,
                ItemPool.getPool(ItemPoolsLegacy.POOL_EXPENSIVE), 9);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(centerX + 2, y + 2, centerZ), EnumFacing.WEST,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_STANDARD), 8);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(centerX, y + 2, centerZ - 2), EnumFacing.NORTH,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_DUNGEON), 10);
    }

    @Override
    public boolean spawnGlow() {
        return false;
    }
}