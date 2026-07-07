package com.hbm.world.generator.room;

import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsSingle;
import com.hbm.world.generator.CellularDungeon;
import com.hbm.world.generator.DungeonToolbox;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VaultDungeonRoomControl extends VaultDungeonRoomElevator {
    public VaultDungeonRoomControl(CellularDungeon parent) {
        super(parent, 0);
    }

    @Override
    public void generateRoom(World world, int x, int y, int z) {
        int centerX = x + getW() / 2;
        int centerZ = z + getW() / 2;

        DungeonToolbox.generateWalls(world, centerX - 3, y + 2, centerZ - 3, 7, getH() - 4, 7, parent.wall);
        DungeonToolbox.generateWalls(world, centerX - 3, y + 3, centerZ - 3, 7, 1, 7, line);

        DungeonToolbox.generateBox(world, centerX, y + 2, centerZ - 3, 1, 2, 1, air);
    }

    @Override
    public void placeLoot(World world, int x, int y, int z) {
        int centerX = x + getW() / 2;
        int centerZ = z + getW() / 2;

        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(centerX - 2, y + 2, centerZ), EnumFacing.EAST,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_REINFORCED), 12);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(centerX + 2, y + 2, centerZ), EnumFacing.WEST,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_UNBREAKABLE), 10);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(centerX, y + 2, centerZ + 2), EnumFacing.SOUTH,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_STANDARD), 10);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(centerX, y + 2, centerZ - 2), EnumFacing.NORTH,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_DUNGEON), 12);
    }
}