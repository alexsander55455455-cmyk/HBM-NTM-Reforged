package com.hbm.world.generator.room;

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

public class VaultDungeonRoomArmory extends VaultDungeonRoomElevator {
    public VaultDungeonRoomArmory(CellularDungeon parent) {
        super(parent, 7);
    }

    @Override
    public void generateRoom(World world, int x, int y, int z) {
        generateRoom(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    public void generateRoom(VaultDungeonPlacer placer, int x, int y, int z) {
        placer.generateBox(x + 5, y + 2, z + getW() / 2 - 2, getW() - 7, getH() - 4, 1, parent.wall);
        placer.generateBox(x + 5, y + 3, z + getW() / 2 - 2, getW() - 7, 1, 1, getLine(x, z));

        placer.generateBox(x + 2, y + 2, z + getW() / 2 + 2, getW() - 7, getH() - 4, 1, parent.wall);
        placer.generateBox(x + 2, y + 3, z + getW() / 2 + 2, getW() - 7, 1, 1, getLine(x, z));
    }

    @Override
    public void placeLoot(World world, int x, int y, int z) {
        placeLoot(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    public void placeLoot(VaultDungeonPlacer placer, int x, int y, int z) {
        WeightedRandomChestContentFrom1710[] lockerLoot = ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LOCKERS);
        placer.placeLootChest(new BlockPos(x + 4, y + 2, z + 4), EnumFacing.EAST, lockerLoot, 9);
        placer.placeLootChest(new BlockPos(x + getW() - 5, y + 2, z + getW() - 5), EnumFacing.WEST, lockerLoot, 10);
        placer.placeLootChest(new BlockPos(x + getW() / 2, y + 2, z + 4), EnumFacing.SOUTH,
                ItemPool.getPool(ItemPoolsLegacy.POOL_EXPENSIVE), 10);
        placer.placeLootChest(new BlockPos(x + 4, y + 2, z + getW() - 5), EnumFacing.NORTH,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_STANDARD), 8);
        placer.placeLootChest(new BlockPos(x + getW() - 5, y + 2, z + 4), EnumFacing.EAST,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_DUNGEON), 12);
    }
}