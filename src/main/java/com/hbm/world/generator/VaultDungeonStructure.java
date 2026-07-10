package com.hbm.world.generator;

import com.hbm.lib.Library;
import com.hbm.world.generator.room.VaultDungeonRoomElevator;
import com.hbm.world.phased.AbstractPhasedStructure;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenTrees;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class VaultDungeonStructure extends AbstractPhasedStructure {

    public static final VaultDungeonStructure INSTANCE = new VaultDungeonStructure();

    private VaultDungeonStructure() {
    }

    private static int horizontalRadius() {
        return CellularDungeonFactory.vault.getRadius() + VaultDungeonRoomElevator.doorRoomX + 8 + 10;
    }

    @Override
    protected boolean isCacheable() {
        return false;
    }

    @Override
    protected int getGenerationHeightOffset() {
        return 16;
    }

    @Override
    public LongArrayList getWatchedChunkOffsets(long origin) {
        return collectChunkOffsetsByRadius(horizontalRadius());
    }

    @Override
    protected void buildStructure(@NotNull LegacyBuilder builder, @NotNull Random rand) {
        CellularDungeonFactory.vault.generate(builder, 0, 0, 0, rand);
    }

    @Override
    public void postGenerate(@NotNull World world, @NotNull Random rand, long finalOrigin) {
        VaultDungeon vault = (VaultDungeon) CellularDungeonFactory.vault;
        if (!vault.hasElevator || !(vault.elevatorRoom instanceof VaultDungeonRoomElevator elevator)) {
            return;
        }

        int ox = Library.getBlockPosX(finalOrigin);
        int oy = Library.getBlockPosY(finalOrigin);
        int oz = Library.getBlockPosZ(finalOrigin);
        int absEX = ox + vault.eX;
        int absEZ = oz + vault.eZ;

        int surfaceY = world.getHeight(absEX, absEZ);
        int h = Math.max(surfaceY - 25, oy + vault.height + 10);
        elevator.generateVaultDoorRoom(world, absEX, h, absEZ);
        elevator.generateElevator(world, absEX, oy, absEZ, h, VaultDungeonRoomElevator.doorRoomY - 2);

        WorldGenTrees tree = new WorldGenTrees(false);
        for (int[] rel : vault.deferredTreePositions) {
            tree.generate(world, rand, new BlockPos(ox + rel[0], oy + rel[1], oz + rel[2]));
        }
    }
}