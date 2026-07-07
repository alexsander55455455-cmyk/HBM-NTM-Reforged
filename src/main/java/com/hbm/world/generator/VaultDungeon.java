package com.hbm.world.generator;

import com.hbm.blocks.ModBlocks;
import com.hbm.world.generator.room.VaultDungeonRoom;
import com.hbm.world.generator.room.VaultDungeonRoomElevator;
import net.minecraft.world.World;

import java.util.Random;

public class VaultDungeon extends CellularDungeon {

    public boolean hasElevator = false;
    public int eX, eZ;
    public VaultDungeonRoom elevatorRoom;

    public VaultDungeon(int width, int height, int dimX, int dimZ, int tries, int branches) {
        super(width, height, dimX, dimZ, tries, branches);

        this.floor.add(ModBlocks.brick_compound.getDefaultState());
        this.wall.add(ModBlocks.concrete_smooth.getDefaultState());
        this.ceiling.add(ModBlocks.ducrete.getDefaultState());
    }

    @Override
    public void generate(World world, int x, int y, int z, Random rand) {
        hasElevator = false;
        elevatorRoom = null;

        if(world.isRemote)
            return;

        x -= dimX * width / 2;
        z -= dimZ * width / 2;

        compose(rand);

        for(int[] coord : order) {
            if(coord == null || coord.length != 2)
                continue;

            int dx = coord[0];
            int dz = coord[1];

            if(cells[dx][dz] != null) {
                cells[dx][dz].generate(world, x + dx * (width - 1), y, z + dz * (width - 1), doors[dx][dz]);
            }
        }

        for(int[] coord : order) {
            if(coord == null || coord.length != 2)
                continue;

            int dx = coord[0];
            int dz = coord[1];

            if(cells[dx][dz] instanceof VaultDungeonRoomElevator) {
                ((VaultDungeonRoomElevator) cells[dx][dz]).placeLoot(world, x + dx * (width - 1), y, z + dz * (width - 1));
            }
        }
    }
}