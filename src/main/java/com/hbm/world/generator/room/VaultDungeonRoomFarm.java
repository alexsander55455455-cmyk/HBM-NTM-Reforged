package com.hbm.world.generator.room;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsComponent;
import com.hbm.itempool.ItemPoolsSingle;
import com.hbm.world.generator.CellularDungeon;
import com.hbm.world.generator.VaultDungeonPlacer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VaultDungeonRoomFarm extends VaultDungeonRoomElevator {

    public IBlockState farmland = Blocks.FARMLAND.getStateFromMeta(7);
    public IBlockState water = Blocks.WATER.getDefaultState();
    public IBlockState grate = ModBlocks.steel_grate.getStateFromMeta(7);
    public IBlockState pole = ModBlocks.steel_beam.getDefaultState();

    public IBlockState potato = Blocks.POTATOES.getDefaultState();
    public IBlockState carrot = Blocks.CARROTS.getDefaultState();
    public IBlockState beet = Blocks.BEETROOTS.getDefaultState();
    public IBlockState wheat = Blocks.WHEAT.getDefaultState();

    public VaultDungeonRoomFarm(CellularDungeon parent) {
        super(parent, 5);
    }

    @Override
    public void generateRoom(World world, int x, int y, int z) {
        generateRoom(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    public void generateRoom(VaultDungeonPlacer placer, int x, int y, int z) {
        int farmSize = (getW() - 7) / 2;
        placer.generateBox(x + 2, y + 1, z + 2, farmSize, 1, farmSize, farmland);
        placer.generateBox(x + farmSize + 5, y + 1, z + 2, farmSize, 1, farmSize, farmland);
        placer.generateBox(x + 2, y + 1, z + farmSize + 5, farmSize, 1, farmSize, farmland);
        placer.generateBox(x + farmSize + 5, y + 1, z + farmSize + 5, farmSize, 1, farmSize, farmland);

        placer.setBlockState(new BlockPos(x + 2, y + 1, z + 2), water);
        placer.setBlockState(new BlockPos(x + getW() - 3, y + 1, z + 2), water);
        placer.setBlockState(new BlockPos(x + 2, y + 1, z + getW() - 3), water);
        placer.setBlockState(new BlockPos(x + getW() - 3, y + 1, z + getW() - 3), water);

        placer.generateBox(x + 2, y + 2, z + 2, farmSize, 1, farmSize, potato);
        placer.generateBox(x + farmSize + 5, y + 2, z + 2, farmSize, 1, farmSize, carrot);
        placer.generateBox(x + 2, y + 2, z + farmSize + 5, farmSize, 1, farmSize, beet);
        placer.generateBox(x + farmSize + 5, y + 2, z + farmSize + 5, farmSize, 1, farmSize, wheat);

        placer.setBlockState(new BlockPos(x + 2, y + 2, z + 2), light);
        placer.setBlockState(new BlockPos(x + getW() - 3, y + 2, z + 2), light);
        placer.setBlockState(new BlockPos(x + 2, y + 2, z + getW() - 3), light);
        placer.setBlockState(new BlockPos(x + getW() - 3, y + 2, z + getW() - 3), light);

        placer.generateBox(x + 2, y + 3, z + 2, farmSize, 1, farmSize, grate);
        placer.generateBox(x + farmSize + 5, y + 3, z + 2, farmSize, 1, farmSize, grate);
        placer.generateBox(x + 2, y + 3, z + farmSize + 5, farmSize, 1, farmSize, grate);
        placer.generateBox(x + farmSize + 5, y + 3, z + farmSize + 5, farmSize, 1, farmSize, grate);

        placer.generateBox(x + farmSize + 1, y + 4, z + farmSize + 1, 1, getH() - 6, 1, pole);
        placer.generateBox(x + farmSize + 5, y + 4, z + farmSize + 1, 1, getH() - 6, 1, pole);
        placer.generateBox(x + farmSize + 1, y + 4, z + farmSize + 5, 1, getH() - 6, 1, pole);
        placer.generateBox(x + farmSize + 5, y + 4, z + farmSize + 5, 1, getH() - 6, 1, pole);
    }

    @Override
    public void placeLoot(World world, int x, int y, int z) {
        placeLoot(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    public void placeLoot(VaultDungeonPlacer placer, int x, int y, int z) {
        placer.placeLootChest(new BlockPos(x + 5, y + 1, z + 5), EnumFacing.EAST,
                ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LOCKERS), 8);
        placer.placeLootChest(new BlockPos(x + getW() - 6, y + 1, z + getW() - 6), EnumFacing.WEST,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_STANDARD), 8);
        placer.placeLootChest(new BlockPos(x + getW() - 6, y + 1, z + 5), EnumFacing.SOUTH,
                ItemPool.getPool(ItemPoolsSingle.POOL_VAULT_DUNGEON), 10);
    }
}