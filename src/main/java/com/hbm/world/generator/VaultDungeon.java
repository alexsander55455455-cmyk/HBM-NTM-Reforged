package com.hbm.world.generator;

import com.hbm.blocks.ModBlocks;
import com.hbm.world.generator.room.VaultDungeonRoom;
import com.hbm.world.generator.room.VaultDungeonRoomElevator;
import com.hbm.world.phased.AbstractPhasedStructure;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VaultDungeon extends CellularDungeon {

    public boolean hasElevator = false;
    public int eX, eZ;
    public VaultDungeonRoom elevatorRoom;
    public final List<int[]> deferredTreePositions = new ArrayList<>();

    public VaultDungeon(int width, int height, int dimX, int dimZ, int tries, int branches) {
        super(width, height, dimX, dimZ, tries, branches);

        this.floor.add(ModBlocks.brick_compound.getDefaultState());
        this.wall.add(ModBlocks.concrete_smooth.getDefaultState());
        this.ceiling.add(ModBlocks.ducrete.getDefaultState());
    }

    public void resetGenerationState() {
        hasElevator = false;
        elevatorRoom = null;
        eX = 0;
        eZ = 0;
        deferredTreePositions.clear();
    }

    @Override
    public void generate(World world, int x, int y, int z, Random rand) {
        resetGenerationState();

        if (world.isRemote)
            return;

        x -= dimX * width / 2;
        z -= dimZ * width / 2;

        compose(rand);
        generateRooms(VaultDungeonPlacer.forWorld(world), x, y, z);
        placeElevatorLoot(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    public void generate(AbstractPhasedStructure.LegacyBuilder world, int x, int y, int z, Random rand) {
        resetGenerationState();

        x -= dimX * width / 2;
        z -= dimZ * width / 2;

        compose(rand);
        generateRooms(VaultDungeonPlacer.forBuilder(world), x, y, z);
        placeElevatorLoot(VaultDungeonPlacer.forBuilder(world), x, y, z);
    }

    private void generateRooms(VaultDungeonPlacer placer, int x, int y, int z) {
        for (int[] coord : order) {
            if (coord == null || coord.length != 2)
                continue;

            int dx = coord[0];
            int dz = coord[1];

            if (cells[dx][dz] instanceof VaultDungeonRoom room) {
                room.generate(placer, x + dx * (width - 1), y, z + dz * (width - 1), doors[dx][dz]);
            }
        }
    }

    private void placeElevatorLoot(VaultDungeonPlacer placer, int x, int y, int z) {
        for (int[] coord : order) {
            if (coord == null || coord.length != 2)
                continue;

            int dx = coord[0];
            int dz = coord[1];

            if (cells[dx][dz] instanceof VaultDungeonRoomElevator elevator) {
                elevator.placeLoot(placer, x + dx * (width - 1), y, z + dz * (width - 1));
            }
        }
    }
}