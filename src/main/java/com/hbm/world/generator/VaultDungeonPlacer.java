package com.hbm.world.generator;

import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.hbm.world.phased.AbstractPhasedStructure;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/** Routes vault dungeon block placement to either live world or phased layout builder. */
public final class VaultDungeonPlacer {

    private final World world;
    private final AbstractPhasedStructure.LegacyBuilder builder;

    private VaultDungeonPlacer(World world, AbstractPhasedStructure.LegacyBuilder builder) {
        this.world = world;
        this.builder = builder;
    }

    public static VaultDungeonPlacer forWorld(World world) {
        return new VaultDungeonPlacer(world, null);
    }

    public static VaultDungeonPlacer forBuilder(AbstractPhasedStructure.LegacyBuilder builder) {
        return new VaultDungeonPlacer(null, builder);
    }

    public boolean isPhased() {
        return builder != null;
    }

    public World world() {
        return world;
    }

    public Random rand() {
        return builder != null ? builder.rand : world.rand;
    }

    public void generateBox(int x, int y, int z, int sx, int sy, int sz, IBlockState block) {
        if (builder != null) {
            DungeonToolbox.generateBox(builder, x, y, z, sx, sy, sz, block);
        } else {
            DungeonToolbox.generateBox(world, x, y, z, sx, sy, sz, block);
        }
    }

    public void generateBox(int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {
        if (builder != null) {
            DungeonToolbox.generateBox(builder, x, y, z, sx, sy, sz, blocks);
        } else {
            DungeonToolbox.generateBox(world, x, y, z, sx, sy, sz, blocks);
        }
    }

    public void generateBoxNoReplace(int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {
        if (builder != null) {
            DungeonToolbox.generateBoxNoReplace(builder, x, y, z, sx, sy, sz, blocks);
        } else {
            DungeonToolbox.generateBoxNoReplace(world, x, y, z, sx, sy, sz, blocks);
        }
    }

    public void generateWalls(int x, int y, int z, int sx, int sy, int sz, IBlockState block) {
        if (builder != null) {
            DungeonToolbox.generateWalls(builder, x, y, z, sx, sy, sz, block);
        } else {
            DungeonToolbox.generateWalls(world, x, y, z, sx, sy, sz, block);
        }
    }

    public void generateWalls(int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {
        if (builder != null) {
            DungeonToolbox.generateWalls(builder, x, y, z, sx, sy, sz, blocks);
        } else {
            DungeonToolbox.generateWalls(world, x, y, z, sx, sy, sz, blocks);
        }
    }

    public void setBlockState(BlockPos pos, IBlockState state) {
        if (builder != null) {
            builder.setBlockState(pos, state);
        } else {
            world.setBlockState(pos, state, 2 | 16);
        }
    }

    public void placeLootChest(BlockPos pos, EnumFacing dir, WeightedRandomChestContentFrom1710[] pool, int rolls) {
        if (builder != null) {
            WeightedRandomChestContentFrom1710.placeLootChest(builder, pos, dir, pool, rolls);
        } else {
            WeightedRandomChestContentFrom1710.placeLootChest(world, pos, dir, pool, rolls);
        }
    }

}