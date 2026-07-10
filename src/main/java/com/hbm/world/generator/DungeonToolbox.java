package com.hbm.world.generator;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.hbm.lib.ForgeDirection;

import com.hbm.world.feature.WorldGenMinableNonCascade;
import com.hbm.world.phased.AbstractPhasedStructure;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenFlowers;

import java.util.List;
import java.util.Random;

public class DungeonToolbox {

    private static final MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();


    static {
        new WorldGenFlowers(Blocks.RED_FLOWER, BlockFlower.EnumFlowerType.ALLIUM);
    }

    public static void generateBox(AbstractPhasedStructure.LegacyBuilder world, int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {

        if (blocks.isEmpty())
            return;
        MutableBlockPos poolPos = mutablePos;

        for (int i = x; i < x + sx; i++) {

            for (int j = y; j < y + sy; j++) {

                for (int k = z; k < z + sz; k++) {

                    IBlockState b = getRandom(blocks, world.rand);
                    if (b == null)
                        b = Blocks.AIR.getDefaultState();
                    world.setBlockState(poolPos.setPos(i, j, k), b, 2);
                }
            }
        }
    }

    public static void generateBox(World world, int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {

        if (blocks.isEmpty())
            return;
        MutableBlockPos pos = mutablePos;

        for (int i = x; i < x + sx; i++) {

            for (int j = y; j < y + sy; j++) {

                for (int k = z; k < z + sz; k++) {

                    IBlockState b = getRandom(blocks, world.rand);
                    if (b == null)
                        b = Blocks.AIR.getDefaultState();
                    world.setBlockState(pos.setPos(i, j, k), b, 2 | 16);
                }
            }
        }
    }

    //i know it's copy paste, but it's a better strat than using a wrapper and generating single-entry lists for no good reason
    public static void generateBox(AbstractPhasedStructure.LegacyBuilder world, int x, int y, int z, int sx, int sy, int sz, IBlockState block) {

        for (int i = x; i < x + sx; i++) {

            for (int j = y; j < y + sy; j++) {

                for (int k = z; k < z + sz; k++) {

                    world.setBlockState(mutablePos.setPos(i, j, k), block, 2);
                }
            }
        }
    }

    public static void generateBox(World world, int x, int y, int z, int sx, int sy, int sz, IBlockState block) {

        for (int i = x; i < x + sx; i++) {

            for (int j = y; j < y + sy; j++) {

                for (int k = z; k < z + sz; k++) {

                    world.setBlockState(mutablePos.setPos(i, j, k), block, 2 | 16);
                }
            }
        }
    }

    public static <T> T getRandom(List<T> list, Random rand) {

        if (list.isEmpty())
            return null;

        return list.get(rand.nextInt(list.size()));
    }

    public static void generateOre(World world, Random rand, int chunkX, int chunkZ, int veinCount, int amount, int minHeight, int variance, Block ore) {
        generateOre(world, rand, chunkX, chunkZ, veinCount, amount, minHeight, variance, ore.getDefaultState(), Blocks.STONE);
    }

    public static void generateOre(World world, Random rand, int chunkX, int chunkZ, int veinCount, int amount, int minHeight, int variance, IBlockState ore) {
        generateOre(world, rand, chunkX, chunkZ, veinCount, amount, minHeight, variance, ore, Blocks.STONE);
    }

    public static void generateOre(World world, Random rand, int chunkX, int chunkZ, int veinCount, int amount, int minHeight, int variance, Block ore, Block target) {
        generateOre(world, rand, chunkX, chunkZ, veinCount, amount, minHeight, variance, ore.getDefaultState(), target);
    }

    public static void generateOre(World world, Random rand, int chunkX, int chunkZ, int veinCount, int amount, int minHeight, int variance, IBlockState ore, Block target) {
        if (veinCount > 0) {
            for (int i = 0; i < veinCount; i++) {

                int x = chunkX + rand.nextInt(16);
                int y = minHeight + (variance > 0 ? rand.nextInt(variance) : 0);
                int z = chunkZ + rand.nextInt(16);

                new WorldGenMinableNonCascade(ore, amount, target).generate(world, rand, mutablePos.setPos(x, y, z));
            }
        }
    }

    public static void generateBoxNoReplace(World world, int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {
        if (blocks.isEmpty())
            return;

        for (int i = x; i < x + sx; i++) {
            for (int j = y; j < y + sy; j++) {
                for (int k = z; k < z + sz; k++) {
                    BlockPos pos = mutablePos.setPos(i, j, k);
                    if (!world.isAirBlock(pos))
                        continue;
                    IBlockState b = getRandom(blocks, world.rand);
                    if (b == null)
                        b = Blocks.AIR.getDefaultState();
                    world.setBlockState(pos, b, 2 | 16);
                }
            }
        }
    }

    public static void generateWalls(World world, int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {
        if (blocks.isEmpty())
            return;

        for (int i = 0; i < sx; i++) {
            for (int j = 0; j < sy; j++) {
                for (int k = 0; k < sz; k++) {
                    if ((i == 0 || i == sx - 1) || (k == 0 || k == sz - 1)) {
                        IBlockState b = getRandom(blocks, world.rand);
                        if (b == null)
                            b = Blocks.AIR.getDefaultState();
                        world.setBlockState(mutablePos.setPos(x + i, y + j, z + k), b, 2 | 16);
                    }
                }
            }
        }
    }

    public static void generateWalls(World world, int x, int y, int z, int sx, int sy, int sz, IBlockState block) {
        for (int i = 0; i < sx; i++) {
            for (int j = 0; j < sy; j++) {
                for (int k = 0; k < sz; k++) {
                    if ((i == 0 || i == sx - 1) || (k == 0 || k == sz - 1))
                        world.setBlockState(mutablePos.setPos(x + i, y + j, z + k), block, 2 | 16);
                }
            }
        }
    }

    public static void generateHollowBox(World world, int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {
        if (blocks.isEmpty())
            return;

        for (int i = 0; i < sx; i++) {
            for (int j = 0; j < sy; j++) {
                for (int k = 0; k < sz; k++) {
                    if ((i == 0 || i == sx - 1) || (j == 0 || j == sy - 1) || (k == 0 || k == sz - 1)) {
                        IBlockState b = getRandom(blocks, world.rand);
                        if (b == null)
                            b = Blocks.AIR.getDefaultState();
                        world.setBlockState(mutablePos.setPos(x + i, y + j, z + k), b, 2 | 16);
                    }
                }
            }
        }
    }

    public static void generateHollowBox(World world, int x, int y, int z, int sx, int sy, int sz, IBlockState block) {
        for (int i = 0; i < sx; i++) {
            for (int j = 0; j < sy; j++) {
                for (int k = 0; k < sz; k++) {
                    if ((i == 0 || i == sx - 1) || (j == 0 || j == sy - 1) || (k == 0 || k == sz - 1))
                        world.setBlockState(mutablePos.setPos(x + i, y + j, z + k), block, 2 | 16);
                }
            }
        }
    }

    public static void generateBoxNoReplace(AbstractPhasedStructure.LegacyBuilder world, int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {
        if (blocks.isEmpty())
            return;

        for (int i = x; i < x + sx; i++) {
            for (int j = y; j < y + sy; j++) {
                for (int k = z; k < z + sz; k++) {
                    if (world.getBlockState(i, j, k).getBlock() != Blocks.AIR)
                        continue;
                    IBlockState b = getRandom(blocks, world.rand);
                    if (b == null)
                        b = Blocks.AIR.getDefaultState();
                    world.setBlockState(mutablePos.setPos(i, j, k), b);
                }
            }
        }
    }

    public static void generateWalls(AbstractPhasedStructure.LegacyBuilder world, int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {
        if (blocks.isEmpty())
            return;

        for (int i = 0; i < sx; i++) {
            for (int j = 0; j < sy; j++) {
                for (int k = 0; k < sz; k++) {
                    if ((i == 0 || i == sx - 1) || (k == 0 || k == sz - 1)) {
                        IBlockState b = getRandom(blocks, world.rand);
                        if (b == null)
                            b = Blocks.AIR.getDefaultState();
                        world.setBlockState(mutablePos.setPos(x + i, y + j, z + k), b);
                    }
                }
            }
        }
    }

    public static void generateWalls(AbstractPhasedStructure.LegacyBuilder world, int x, int y, int z, int sx, int sy, int sz, IBlockState block) {
        for (int i = 0; i < sx; i++) {
            for (int j = 0; j < sy; j++) {
                for (int k = 0; k < sz; k++) {
                    if ((i == 0 || i == sx - 1) || (k == 0 || k == sz - 1))
                        world.setBlockState(mutablePos.setPos(x + i, y + j, z + k), block);
                }
            }
        }
    }

    public static void generateHollowBox(AbstractPhasedStructure.LegacyBuilder world, int x, int y, int z, int sx, int sy, int sz, List<IBlockState> blocks) {
        if (blocks.isEmpty())
            return;

        for (int i = 0; i < sx; i++) {
            for (int j = 0; j < sy; j++) {
                for (int k = 0; k < sz; k++) {
                    if ((i == 0 || i == sx - 1) || (j == 0 || j == sy - 1) || (k == 0 || k == sz - 1)) {
                        IBlockState b = getRandom(blocks, world.rand);
                        if (b == null)
                            b = Blocks.AIR.getDefaultState();
                        world.setBlockState(mutablePos.setPos(x + i, y + j, z + k), b);
                    }
                }
            }
        }
    }

    public static void generateHollowBox(AbstractPhasedStructure.LegacyBuilder world, int x, int y, int z, int sx, int sy, int sz, IBlockState block) {
        for (int i = 0; i < sx; i++) {
            for (int j = 0; j < sy; j++) {
                for (int k = 0; k < sz; k++) {
                    if ((i == 0 || i == sx - 1) || (j == 0 || j == sy - 1) || (k == 0 || k == sz - 1))
                        world.setBlockState(mutablePos.setPos(x + i, y + j, z + k), block);
                }
            }
        }
    }

    public static boolean placeDummyable(World world, int x, int y, int z, BlockDummyable block, EnumFacing facing) {
        EnumFacing coreDir = facing.getOpposite();
        ForgeDirection dir = ForgeDirection.getOrientation(coreDir.ordinal());

        if (!block.checkRequirement(world, x, y, z, dir, 0))
            return false;

        BlockDummyable.safeRem = true;
        world.setBlockState(mutablePos.setPos(x, y, z), block.getDefaultState().withProperty(BlockDummyable.META, coreDir.ordinal() + BlockDummyable.offset), 2 | 16);
        MultiblockHandlerXR.fillSpace(world, x, y, z, block.getDimensions(), block, dir);

        if (block instanceof BlockDoorGeneric door && door.type.getExtraDimensions() != null) {
            for (int[] dims : door.type.getExtraDimensions())
                MultiblockHandlerXR.fillSpace(world, x, y, z, dims, block, dir);
        }

        BlockDummyable.safeRem = false;
        return true;
    }

    /** Places a vault door with the label side facing {@code doorFacing}, matching EE {@code VaultDoor.placeVaultDoor}. */
    public static boolean placeVaultDoor(World world, int x, int y, int z, EnumFacing doorFacing) {
        if (!placeDummyable(world, x, y, z, (BlockDummyable) ModBlocks.vault_door, doorFacing.getOpposite())) {
            return false;
        }

        if (world.getTileEntity(mutablePos.setPos(x, y, z)) instanceof TileEntityDoorGeneric door) {
            door.setSkinIndex((byte) 0);
        }

        return true;
    }

}
