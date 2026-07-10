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

public class VaultDungeonRoomCanteen extends VaultDungeonRoomElevator {
    public VaultDungeonRoomCanteen(CellularDungeon parent) {
        super(parent, 1);
    }

    @Override
    public void generateRoom(World world, int x, int y, int z) {
        generateRoom(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    public void generateRoom(VaultDungeonPlacer placer, int x, int y, int z) {
        int centerX = x + getW() / 2;
        int centerZ = z + getW() / 2;

        placer.generateBox(centerX - 2, y + 2, centerZ - 4, 1, getH() - 4, 9, parent.wall);
        placer.generateBox(centerX - 2, y + 3, centerZ - 4, 1, 1, 9, getLine(x, z));

        placer.generateBox(centerX + 2, y + 2, centerZ - 4, 1, getH() - 4, 9, parent.wall);
        placer.generateBox(centerX + 2, y + 3, centerZ - 4, 1, 1, 9, getLine(x, z));

        placer.generateBox(centerX - 4, y + 2, centerZ - 2, 9, getH() - 4, 1, parent.wall);
        placer.generateBox(centerX - 4, y + 3, centerZ - 2, 9, 1, 1, getLine(x, z));

        placer.generateBox(centerX - 4, y + 2, centerZ + 2, 9, getH() - 4, 1, parent.wall);
        placer.generateBox(centerX - 4, y + 3, centerZ + 2, 9, 1, 1, getLine(x, z));

        placer.generateBox(centerX - 2, y + 2, centerZ - 1, 5, getH() - 4, 3, air);
        placer.generateBox(centerX - 1, y + 2, centerZ - 2, 3, getH() - 4, 5, air);
    }

    @Override
    public void placeLoot(World world, int x, int y, int z) {
        placeLoot(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    public void placeLoot(VaultDungeonPlacer placer, int x, int y, int z) {
        int centerX = x + getW() / 2;
        int centerZ = z + getW() / 2;

        placer.placeLootChest(new BlockPos(centerX - 1, y + 2, centerZ + 3), EnumFacing.SOUTH,
                ItemPool.getPool(ItemPoolsLegacy.POOL_GENERIC), 10);
        placer.placeLootChest(new BlockPos(centerX + 1, y + 2, centerZ - 3), EnumFacing.NORTH,
                ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LOCKERS), 8);
        placer.placeLootChest(new BlockPos(centerX + 3, y + 2, centerZ), EnumFacing.WEST,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_STANDARD), 8);
    }
}