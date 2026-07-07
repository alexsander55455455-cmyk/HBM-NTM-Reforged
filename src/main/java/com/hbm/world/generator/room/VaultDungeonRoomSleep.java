package com.hbm.world.generator.room;

import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsComponent;
import com.hbm.itempool.ItemPoolsSingle;
import com.hbm.world.generator.CellularDungeon;
import com.hbm.world.generator.DungeonToolbox;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VaultDungeonRoomSleep extends VaultDungeonRoomElevator {
    public VaultDungeonRoomSleep(CellularDungeon parent) {
        super(parent, 15);
    }

    @Override
    public void generateRoom(World world, int x, int y, int z) {
        DungeonToolbox.generateBox(world, x + getW() / 2 - 2, y + 2, z + 2, 1, getH() - 4, getW() - 4, parent.wall);
        DungeonToolbox.generateBox(world, x + getW() / 2 - 2, y + 3, z + 2, 1, 1, getW() - 4, getLine(x, z));

        DungeonToolbox.generateBox(world, x + getW() / 2 + 2, y + 2, z + 2, 1, getH() - 4, getW() - 4, parent.wall);
        DungeonToolbox.generateBox(world, x + getW() / 2 + 2, y + 3, z + 2, 1, 1, getW() - 4, getLine(x, z));

        DungeonToolbox.generateBox(world, x + getW() / 2 - 2, y + 2, z + getW() / 2 - 1, 5, 3, 3, air);
    }

    @Override
    public void placeLoot(World world, int x, int y, int z) {
        WeightedRandomChestContentFrom1710[] lockerLoot = ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LOCKERS);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(x + 4, y + 2, z + 4), EnumFacing.EAST, lockerLoot, 8);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(x + getW() - 5, y + 2, z + getW() - 5), EnumFacing.WEST, lockerLoot, 8);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(x + getW() / 2, y + 2, z + getW() / 2), EnumFacing.NORTH, lockerLoot, 7);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(x + 4, y + 2, z + getW() - 5), EnumFacing.SOUTH,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_RUSTY), 8);
    }
}