package com.hbm.world.generator.room;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsComponent;
import com.hbm.itempool.ItemPoolsSingle;
import com.hbm.world.generator.CellularDungeon;
import com.hbm.world.generator.DungeonToolbox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class VaultDungeonRoomMedical extends VaultDungeonRoomElevator {

    public static List<IBlockState> lab = new ArrayList<>();

    public VaultDungeonRoomMedical(CellularDungeon parent) {
        super(parent, 14);
        lab.add(ModBlocks.tile_lab.getDefaultState());
        lab.add(ModBlocks.tile_lab_broken.getDefaultState());
        lab.add(ModBlocks.tile_lab_cracked.getDefaultState());
    }

    @Override
    public void generateRoom(World world, int x, int y, int z) {
        DungeonToolbox.generateBox(world, x + 2, y + 1, z + 2, getW() - 4, 1, getW() - 4, lab);

        DungeonToolbox.generateWalls(world, x + 1, y + 2, z + 1, getW() / 2 - 2, getH() - 4, getW() / 2 - 2, parent.wall);
        DungeonToolbox.generateWalls(world, x + getW() / 2 + 2, y + 2, z + 1, getW() / 2 - 2, getH() - 4, getW() / 2 - 2, parent.wall);
        DungeonToolbox.generateWalls(world, x + 1, y + 2, z + getW() / 2 + 2, getW() / 2 - 2, getH() - 4, getW() / 2 - 2, parent.wall);
        DungeonToolbox.generateWalls(world, x + getW() / 2 + 2, y + 2, z + getW() / 2 + 2, getW() / 2 - 2, getH() - 4, getW() / 2 - 2, parent.wall);

        DungeonToolbox.generateWalls(world, x + 1, y + 3, z + 1, getW() / 2 - 2, 1, getW() / 2 - 2, getLine(x, z));
        DungeonToolbox.generateWalls(world, x + getW() / 2 + 2, y + 3, z + 1, getW() / 2 - 2, 1, getW() / 2 - 2, getLine(x, z));
        DungeonToolbox.generateWalls(world, x + 1, y + 3, z + getW() / 2 + 2, getW() / 2 - 2, 1, getW() / 2 - 2, getLine(x, z));
        DungeonToolbox.generateWalls(world, x + getW() / 2 + 2, y + 3, z + getW() / 2 + 2, getW() / 2 - 2, 1, getW() / 2 - 2, getLine(x, z));

        DungeonToolbox.generateBox(world, x + 4, y + 2, z + getW() / 2 - 2, 1, 2, 5, air);
        DungeonToolbox.generateBox(world, x + getW() - 5, y + 2, z + getW() / 2 - 2, 1, 2, 5, air);
    }

    @Override
    public void placeLoot(World world, int x, int y, int z) {
        WeightedRandomChestContentFrom1710[] labLoot = ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LAB);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(x + 4, y + 2, z + 4), EnumFacing.EAST, labLoot, 10);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(x + getW() - 5, y + 2, z + 4), EnumFacing.WEST, labLoot, 11);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(x + 4, y + 2, z + getW() - 5), EnumFacing.EAST, labLoot, 11);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(x + getW() - 5, y + 2, z + getW() - 5), EnumFacing.WEST, labLoot, 12);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(x + getW() / 2, y + 2, z + getW() / 2), EnumFacing.NORTH,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_REINFORCED), 8);
        WeightedRandomChestContentFrom1710.placeLootChest(world, new BlockPos(x + getW() / 2 - 3, y + 2, z + getW() / 2), EnumFacing.EAST,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_DUNGEON), 10);
    }
}