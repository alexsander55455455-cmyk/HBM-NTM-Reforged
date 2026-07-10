package com.hbm.world.generator.room;

import com.hbm.world.generator.CellularDungeon;
import com.hbm.world.generator.VaultDungeon;
import com.hbm.world.generator.VaultDungeonPlacer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenTrees;

import java.util.ArrayList;
import java.util.List;

public class VaultDungeonRoomWiese extends VaultDungeonRoomElevator {

    WorldGenTrees tree;
    public List<IBlockState> wiese = new ArrayList<>();

    public VaultDungeonRoomWiese(CellularDungeon parent) {
        super(parent, 13);
        wiese.add(null);
        for (int i = 0; i < 16; i++) wiese.add(Blocks.TALLGRASS.getStateFromMeta(1));
        wiese.add(Blocks.TALLGRASS.getStateFromMeta(2));
        tree = new WorldGenTrees(false);
    }

    @Override
    public void generateRoom(World world, int x, int y, int z) {
        generateRoom(VaultDungeonPlacer.forWorld(world), x, y, z);
    }

    @Override
    public void generateRoom(VaultDungeonPlacer placer, int x, int y, int z) {
        placer.generateBox(x + 2, y + 1, z + 2, getW() - 4, 1, getW() - 4, Blocks.GRASS.getDefaultState());

        if (placer.isPhased() && parent instanceof VaultDungeon vault) {
            int treeCount = placer.rand().nextInt(2) + 3;
            for (int i = 0; i < treeCount; i++) {
                int treeX = x + 2 + placer.rand().nextInt(getW() - 4);
                int treeY = y + 2;
                int treeZ = z + 2 + placer.rand().nextInt(getW() - 4);
                vault.deferredTreePositions.add(new int[]{treeX, treeY, treeZ});
            }
        } else {
            for (int i = 0; i < placer.rand().nextInt(2) + 3; i++) {
                tree.generate(placer.world(), placer.rand(), new net.minecraft.util.math.BlockPos(
                        x + 2 + placer.rand().nextInt(getW() - 4), y + 2, z + 2 + placer.rand().nextInt(getW() - 4)));
            }
        }

        placer.generateBoxNoReplace(x + 2, y + 2, z + 2, getW() - 4, 1, getW() - 4, wiese);
    }

    @Override
    public int getH() {
        return parent.height + 3;
    }
}