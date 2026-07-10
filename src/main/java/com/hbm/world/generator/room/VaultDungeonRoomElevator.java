package com.hbm.world.generator.room;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockNTMLadder;
import com.hbm.world.generator.CellularDungeon;
import com.hbm.world.generator.DungeonToolbox;
import com.hbm.world.generator.VaultDungeon;
import com.hbm.world.generator.VaultDungeonPlacer;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class VaultDungeonRoomElevator extends VaultDungeonRoom {

    public static int doorRoomX = 19;
    public static int doorRoomY = 9;
    public static int doorRoomZ = 17;

    public static List<IBlockState> bricks = new ArrayList<>();
    public static IBlockState haz = ModBlocks.concrete_hazard.getDefaultState();
    public static IBlockState decoSteel = ModBlocks.deco_steel.getDefaultState();
    public static IBlockState ducRef = ModBlocks.reinforced_ducrete.getDefaultState();
    public static IBlockState plating = ModBlocks.deco_tungsten.getDefaultState();
    public static IBlockState dark = ModBlocks.concrete_gray.getDefaultState();
    public static IBlockState pillar = ModBlocks.concrete_pillar.getDefaultState();
    public static IBlockState grate = ModBlocks.steel_grate.getStateFromMeta(7);
    public static IBlockState ladderE = ModBlocks.ladder_steel.getDefaultState().withProperty(BlockNTMLadder.FACING, EnumFacing.WEST);
    public static IBlockState ladderW = ModBlocks.ladder_steel.getDefaultState().withProperty(BlockNTMLadder.FACING, EnumFacing.EAST);
    public static IBlockState railing = ModBlocks.railing_normal.getStateFromMeta(5);

    public VaultDungeonRoomElevator(CellularDungeon parent, int lineColor) {
        super(parent, lineColor);
        bricks.add(ModBlocks.brick_concrete.getDefaultState());
        bricks.add(ModBlocks.brick_concrete_broken.getDefaultState());
        bricks.add(ModBlocks.brick_concrete_cracked.getDefaultState());
        bricks.add(ModBlocks.brick_concrete_mossy.getDefaultState());
    }

    @Override
    public IBlockState getLine(int x, int z) {
        if (parent instanceof VaultDungeon vault && vault.hasElevator && vault.elevatorRoom == this) {
            if (vault.eX != x + parent.width / 2 || vault.eZ != z + parent.width / 2) {
                resetLine();
            }
        }
        return line;
    }

    public boolean spawnGlow() {
        return true;
    }

    @Override
    public void generateMain(World world, int x, int y, int z) {
        generateMain(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    protected void generateMain(VaultDungeonPlacer placer, int x, int y, int z) {
        super.generateMain(placer, x, y, z);

        if (parent instanceof VaultDungeon vault && !vault.hasElevator) {
            vault.eX = x + parent.width / 2;
            vault.eZ = z + parent.width / 2;
            vault.hasElevator = true;
            this.line = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.YELLOW);
            vault.elevatorRoom = this;
            if (placer.isPhased()) {
                // Door room, shaft, and surface exit are deferred to VaultDungeonStructure.postGenerate.
            } else {
                World world = placer.world();
                int h = Math.max(world.getHeight(vault.eX, vault.eZ) - 25, y + parent.height + 10);
                generateVaultDoorRoom(world, vault.eX, h, vault.eZ);
                generateElevator(world, vault.eX, y, vault.eZ, h, doorRoomY - 2);
            }
        } else {
            generateRoom(placer, x, y, z);
            if (spawnGlow()) {
                placer.setBlockState(new BlockPos(x + parent.width / 2, y + 2, z + parent.width / 2), ModBlocks.glow_spawner.getDefaultState());
            }
        }
    }

    public void generateRoom(World world, int x, int y, int z) {
        generateRoom(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    public void generateRoom(VaultDungeonPlacer placer, int x, int y, int z) {
    }

    public void placeLoot(World world, int x, int y, int z) {
        placeLoot(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    public void placeLoot(VaultDungeonPlacer placer, int x, int y, int z) {
    }

    public void generateVaultDoorRoom(World world, int x, int y, int z) {
        DungeonToolbox.generateHollowBox(world, x - 5, y - 1, z - doorRoomZ / 2 - 1, doorRoomX + 2, doorRoomY + 2, doorRoomZ + 2, parent.wall);
        DungeonToolbox.generateHollowBox(world, x - 4, y, z - doorRoomZ / 2, doorRoomX, doorRoomY, doorRoomZ, shielding);
        DungeonToolbox.generateBox(world, x - 3, y + 1, z - doorRoomZ / 2 + 1, doorRoomX - 2, 1, doorRoomZ - 2, parent.floor);
        DungeonToolbox.generateWalls(world, x - 3, y + 2, z - doorRoomZ / 2 + 1, doorRoomX - 2, doorRoomY - 4, doorRoomZ - 2, dark);
        DungeonToolbox.generateBox(world, x - 3, y + doorRoomY - 2, z - doorRoomZ / 2 + 1, doorRoomX - 2, 1, doorRoomZ - 2, parent.ceiling);
        DungeonToolbox.generateBox(world, x - 2, y + 2, z - doorRoomZ / 2 + 2, doorRoomX - 4, doorRoomY - 4, doorRoomZ - 4, air);

        DungeonToolbox.generateBox(world, x + doorRoomX - 5, y + 1, z - 5, 1, 7, 7, ducRef);
        DungeonToolbox.generateBox(world, x + doorRoomX - 6, y + 2, z - 4, 3, 5, 5, air);

        DungeonToolbox.placeVaultDoor(world, x + doorRoomX - 6, y + 2, z - 2, EnumFacing.EAST);
        world.setBlockState(new BlockPos(x + doorRoomX - 7, y + 4, z - 5), Blocks.LEVER.getStateFromMeta(2), 2 | 16);
        world.setBlockState(new BlockPos(x + doorRoomX - 5, y + 4, z - 4), Blocks.STONE_BUTTON.getStateFromMeta(3), 2 | 16);

        DungeonToolbox.generateBox(world, x + doorRoomX - 8, y + 1, z - doorRoomZ / 2 + 2, 1, 1, doorRoomZ - 4, haz);
        DungeonToolbox.generateBox(world, x + doorRoomX - 8, y + 2, z - doorRoomZ / 2 + 2, 1, 1, doorRoomZ - 4, railing);
        DungeonToolbox.generateBox(world, x + doorRoomX - 6, y + 4, z + 1, 1, 1, 6, decoSteel);
        DungeonToolbox.generateBox(world, x + doorRoomX - 11, y + 2, z - 3, 4, 1, 3, grate);

        world.setBlockState(new BlockPos(x + doorRoomX - 8, y + 3, z - 3), ModBlocks.railing_end_flipped_self.getStateFromMeta(3), 2 | 16);
        world.setBlockState(new BlockPos(x + doorRoomX - 9, y + 3, z - 3), ModBlocks.railing_normal.getStateFromMeta(3), 2 | 16);
        world.setBlockState(new BlockPos(x + doorRoomX - 10, y + 3, z - 3), ModBlocks.railing_normal.getStateFromMeta(3), 2 | 16);
        world.setBlockState(new BlockPos(x + doorRoomX - 11, y + 3, z - 3), ModBlocks.railing_end_self.getStateFromMeta(3), 2 | 16);

        world.setBlockState(new BlockPos(x + doorRoomX - 8, y + 3, z - 1), ModBlocks.railing_end_self.getStateFromMeta(2), 2 | 16);
        world.setBlockState(new BlockPos(x + doorRoomX - 9, y + 3, z - 1), ModBlocks.railing_normal.getStateFromMeta(2), 2 | 16);
        world.setBlockState(new BlockPos(x + doorRoomX - 10, y + 3, z - 1), ModBlocks.railing_normal.getStateFromMeta(2), 2 | 16);
        world.setBlockState(new BlockPos(x + doorRoomX - 11, y + 3, z - 1), ModBlocks.railing_end_flipped_self.getStateFromMeta(2), 2 | 16);

        DungeonToolbox.generateBox(world, x + doorRoomX - 11, y + 2, z - 4, 3, 1, 1, ModBlocks.concrete_stairs.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH));
        DungeonToolbox.generateBox(world, x + doorRoomX - 11, y + 2, z, 3, 1, 1, ModBlocks.concrete_stairs.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
        DungeonToolbox.generateBox(world, x + doorRoomX - 12, y + 2, z - 4, 1, 1, 5, ModBlocks.concrete_stairs.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.EAST));

        int exitY = world.getHeight(x + doorRoomX + 8, z - 2) + 1;
        DungeonToolbox.generateHollowBox(world, x + doorRoomX - 3, y, z - 6, 10, 9, 9, bricks);
        DungeonToolbox.generateHollowBox(world, x + doorRoomX + 6, y, z - 4, 5, exitY - y, 5, bricks);
        DungeonToolbox.generateBox(world, x + doorRoomX + 7, y + 1, z - 3, 3, exitY - y, 3, air);
        DungeonToolbox.generateBox(world, x + doorRoomX - 4, y + 1, z - 5, 10, 7, 7, air);
        DungeonToolbox.generateBox(world, x + doorRoomX + 6, y + 1, z - 3, 1, 3, 3, air);

        for (int dx = -6; dx < 6; dx += 2) {
            for (int dz = -4; dz < 5; dz += 2) {
                world.setBlockState(new BlockPos(x + 6 + dx, y + doorRoomY - 2, z - doorRoomZ / 2 + 8 + dz), light, 2 | 16);
            }
        }
    }

    public void generateElevator(World world, int x, int y, int z, int h, int ladderOffset) {
        int baseY = y + 2;
        int baseH = h - y - 2 + ladderOffset;
        DungeonToolbox.generateWalls(world, x - 3, y + getH(), z - 3, 7, h - getH() - y, 7, shielding);
        DungeonToolbox.generateWalls(world, x - 2, baseY, z - 2, 5, baseH, 5, plating);
        DungeonToolbox.generateBox(world, x - 2, baseY, z, 1, baseH, 1, pillar);
        DungeonToolbox.generateBox(world, x + 2, baseY, z, 1, baseH, 1, pillar);
        DungeonToolbox.generateBox(world, x, baseY, z - 2, 1, baseH, 1, pillar);
        DungeonToolbox.generateBox(world, x, baseY, z + 2, 1, baseH, 1, pillar);
        DungeonToolbox.generateBox(world, x - 1, baseY, z - 2, 3, 3, 5, air);
        DungeonToolbox.generateBox(world, x - 1, baseY, z - 1, 1, baseH, 3, ladderW);
        DungeonToolbox.generateBox(world, x, baseY, z - 1, 1, baseH, 3, air);
        DungeonToolbox.generateBox(world, x + 1, baseY, z - 1, 1, baseH, 3, ladderE);

        DungeonToolbox.generateBox(world, x - 1, h + 2, z - 2, 3, 5, 1, air);
        DungeonToolbox.generateBox(world, x - 1, h + 2, z + 2, 3, 5, 1, air);
        world.setBlockState(new BlockPos(x, h + 1, z + 2), plating, 2 | 16);
        world.setBlockState(new BlockPos(x, h + 1, z - 2), plating, 2 | 16);
        world.setBlockState(new BlockPos(x, y + 5, z + 2), plating, 2 | 16);
        world.setBlockState(new BlockPos(x, y + 5, z - 2), plating, 2 | 16);
    }
}