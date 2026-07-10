package com.hbm.world.generator.room;

import com.hbm.blocks.ModBlocks;
import com.hbm.world.generator.CellularDungeon;
import com.hbm.world.generator.CellularDungeonRoom;
import com.hbm.world.generator.DungeonToolbox;
import com.hbm.world.generator.VaultDungeonPlacer;
import com.hbm.world.phased.AbstractPhasedStructure;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VaultDungeonRoom extends CellularDungeonRoom {

    public static IBlockState shielding = Blocks.BEDROCK.getDefaultState();
    public static IBlockState tunnel = ModBlocks.ducrete_smooth.getDefaultState();
    public static IBlockState air = Blocks.AIR.getDefaultState();
    public int lineColor;
    public IBlockState line;
    public static IBlockState light = ModBlocks.reinforced_light.getDefaultState();
    public static IBlockState cableLine = ModBlocks.reinforced_ducrete.getDefaultState();

    public VaultDungeonRoom(CellularDungeon parent, int lineColor) {
        super(parent);
        this.line = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.byMetadata(lineColor));
        this.lineColor = lineColor;
    }

    public void resetLine() {
        this.line = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.byMetadata(this.lineColor));
    }

    @Override
    public void generate(World world, int x, int y, int z, EnumFacing door) {
        generate(VaultDungeonPlacer.forWorld(world), x, y, z, door);
    }

    @Override
    public void generate(AbstractPhasedStructure.LegacyBuilder world, int x, int y, int z, EnumFacing door) {
        generate(VaultDungeonPlacer.forBuilder(world), x, y, z, door);
    }

    public void generate(VaultDungeonPlacer placer, int x, int y, int z, EnumFacing door) {
        generateMain(placer, x, y, z);
        for (int i = 2; i < 6; i++) {
            EnumFacing dir = EnumFacing.byIndex(i);
            generateWall(placer, x, y, z, dir, dir == door);
        }
    }

    public void generateMain(World world, int x, int y, int z, int w, int h) {
        generateMain(VaultDungeonPlacer.forWorld(world), x, y, z, w, h);
    }

    protected void generateMain(VaultDungeonPlacer placer, int x, int y, int z, int w, int h) {
        placer.generateBox(x, y, z, w, h, w, shielding);
        placer.generateBox(x + 1, y + 1, z + 1, w - 2, 1, w - 2, parent.floor);
        placer.generateBox(x + 1, y + h - 2, z + 1, w - 2, 1, w - 2, parent.ceiling);
        placer.generateBox(x + 1, y + 2, z + 1, w - 2, h - 4, w - 2, air);
    }

    @Override
    public void generateMain(World world, int x, int y, int z) {
        generateMain(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    protected void generateMain(VaultDungeonPlacer placer, int x, int y, int z) {
        generateMain(placer, x, y, z, getW(), getH());
        for (int dx = -4; dx < 5; dx += 2) {
            for (int dz = -4; dz < 5; dz += 2) {
                placer.setBlockState(new BlockPos(x + getW() / 2 + dx, y + getH() - 2, z + getW() / 2 + dz), light);
            }
        }
    }

    public int getW() {
        return parent.width;
    }

    public int getH() {
        return parent.height;
    }

    public IBlockState getLine(int x, int z) {
        return line;
    }

    @Override
    public void generateWall(World world, int x, int y, int z, EnumFacing wall, boolean door) {
        generateWall(VaultDungeonPlacer.forWorld(world), x, y, z, wall, door);
    }

    protected void generateWall(VaultDungeonPlacer placer, int x, int y, int z, EnumFacing wall, boolean door) {
        if (wall == EnumFacing.NORTH) {
            placer.generateBox(x + 1, y + 2, z + 1, getW() - 2, getH() - 4, 1, parent.wall);
            placer.generateBox(x + 1, y + 3, z + 1, getW() - 2, 1, 1, getLine(x, z));

            if (door) {
                placer.generateBox(x + getW() / 2 - 2, y + 2, z, 5, 4, 1, tunnel);
                placer.generateBox(x + getW() / 2 - 2, y + 1, z, 5, 1, 1, parent.floor);
                placer.generateBox(x + getW() / 2 - 1, y + 2, z - 1, 3, 3, 3, air);
                placer.generateBox(x + 1, y + getH() - 2, z, getW() - 2, 1, 1, cableLine);
            }
        } else if (wall == EnumFacing.SOUTH) {
            placer.generateBox(x + 1, y + 2, z + getW() - 2, getW() - 2, getH() - 4, 1, parent.wall);
            placer.generateBox(x + 1, y + 3, z + getW() - 2, getW() - 2, 1, 1, getLine(x, z));

            if (door) {
                placer.generateBox(x + getW() / 2 - 2, y + 2, z + getW() - 1, 5, 4, 1, tunnel);
                placer.generateBox(x + getW() / 2 - 2, y + 1, z + getW() - 1, 5, 1, 1, parent.floor);
                placer.generateBox(x + getW() / 2 - 1, y + 2, z + getW() - 2, 3, 3, 3, air);
                placer.generateBox(x + 1, y + getH() - 2, z + getW() - 1, getW() - 2, 1, 1, cableLine);
            }
        } else if (wall == EnumFacing.WEST) {
            placer.generateBox(x + 1, y + 2, z + 1, 1, getH() - 4, getW() - 2, parent.wall);
            placer.generateBox(x + 1, y + 3, z + 1, 1, 1, getW() - 2, getLine(x, z));

            if (door) {
                placer.generateBox(x, y + 2, z + getW() / 2 - 2, 1, 4, 5, tunnel);
                placer.generateBox(x, y + 1, z + getW() / 2 - 2, 1, 1, 5, parent.floor);
                placer.generateBox(x - 1, y + 2, z + getW() / 2 - 1, 3, 3, 3, air);
                placer.generateBox(x, y + getH() - 2, z + 1, 1, 1, getW() - 2, cableLine);
            }
        } else if (wall == EnumFacing.EAST) {
            placer.generateBox(x + getW() - 2, y + 2, z + 1, 1, getH() - 4, getW() - 2, parent.wall);
            placer.generateBox(x + getW() - 2, y + 3, z + 1, 1, 1, getW() - 2, getLine(x, z));

            if (door) {
                placer.generateBox(x + getW() - 1, y + 2, z + getW() / 2 - 2, 1, 4, 5, tunnel);
                placer.generateBox(x + getW() - 1, y + 1, z + getW() / 2 - 2, 1, 1, 5, parent.floor);
                placer.generateBox(x + getW() - 2, y + 2, z + getW() / 2 - 1, 3, 3, 3, air);
                placer.generateBox(x + getW() - 1, y + getH() - 2, z + 1, 1, 1, getW() - 2, cableLine);
            }
        }
    }
}