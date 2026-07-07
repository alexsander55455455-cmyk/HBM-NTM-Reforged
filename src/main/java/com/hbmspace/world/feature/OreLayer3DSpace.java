package com.hbmspace.world.feature;

import com.hbmspace.dim.WorldProviderCelestial;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

public class OreLayer3DSpace {
    private static final int MIN_Y = 6;
    private static final int MAX_Y = 64;
    private static final int HEIGHT = MAX_Y - MIN_Y + 1;

    public static int counter = 0;
    public int id;
    private long lastSeed = Long.MIN_VALUE;
    private NoiseGeneratorPerlin noiseX;
    private NoiseGeneratorPerlin noiseY;
    private NoiseGeneratorPerlin noiseZ;
    private double scaleH;
    private double scaleV;
    private double threshold;
    private Block block;
    private int meta;
    private int dim = 0;
    boolean allCelestials = false;
    private double[][] cacheX;
    private double[][] cacheZ;
    private double[][] cacheY;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public OreLayer3DSpace(Block block, int meta) {
        this.block = block;
        this.meta = meta;
        MinecraftForge.EVENT_BUS.register(this);
        this.id = counter++;
    }

    public OreLayer3DSpace setDimension(int dim) {
        this.dim = dim;
        return this;
    }

    // If enabled, this vein will spawn on all celestial bodies
    public OreLayer3DSpace setGlobal(boolean value) {
        this.allCelestials = value;
        return this;
    }

    public OreLayer3DSpace setScaleH(double scale) {
        this.scaleH = scale;
        return this;
    }

    public OreLayer3DSpace setScaleV(double scale) {
        this.scaleV = scale;
        return this;
    }

    public OreLayer3DSpace setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

    @SubscribeEvent
    public void onDecorate(DecorateBiomeEvent.Pre event) {
        World world = event.getWorld();
        if (world == null || world.provider == null || world.isRemote) return;

        Block replace = Blocks.STONE;
        if(world.provider instanceof WorldProviderCelestial) {
            replace = ((WorldProviderCelestial)world.provider).getStone();
        }

        if(allCelestials) {
            if(!(world.provider instanceof WorldProviderCelestial) && world.provider.getDimension() != 0) return;
        } else {
            if(world.provider.getDimension() != this.dim) return;
        }

        final BlockMatcher replaceMatcher = BlockMatcher.forBlock(replace);

        long seed = world.getSeed();
        if (noiseX == null || seed != lastSeed) {
            noiseX = new NoiseGeneratorPerlin(new Random(seed + 101L + (long)this.id), 4);
            noiseY = new NoiseGeneratorPerlin(new Random(seed + 102L + (long)this.id), 4);
            noiseZ = new NoiseGeneratorPerlin(new Random(seed + 103L + (long)this.id), 4);
            lastSeed = seed;
            cacheX = new double[16][HEIGHT];
            cacheZ = new double[16][HEIGHT];
            cacheY = new double[16][16];
        }

        int cX = event.getPos().getX();
        int cZ = event.getPos().getZ();
        int startX = cX + 8;
        int startZ = cZ + 8;

        for (int zOff = 0; zOff < 16; zOff++) {
            int worldZ = startZ + zOff;
            for (int yIndex = 0; yIndex < HEIGHT; yIndex++) {
                int y = MAX_Y - yIndex;
                cacheX[zOff][yIndex] = noiseX.getValue((double)y * scaleV, (double)worldZ * scaleH);
            }
        }

        for (int xOff = 0; xOff < 16; xOff++) {
            int worldX = startX + xOff;
            for (int yIndex = 0; yIndex < HEIGHT; yIndex++) {
                int y = MAX_Y - yIndex;
                cacheZ[xOff][yIndex] = noiseZ.getValue((double)worldX * scaleH, (double)y * scaleV);
            }
        }

        for (int xOff = 0; xOff < 16; xOff++) {
            int worldX = startX + xOff;
            for (int zOff = 0; zOff < 16; zOff++) {
                int worldZ = startZ + zOff;
                cacheY[xOff][zOff] = noiseY.getValue((double)worldX * scaleH, (double)worldZ * scaleH);
            }
        }

        for (int xOff = 0; xOff < 16; xOff++) {
            int worldX = startX + xOff;

            for (int zOff = 0; zOff < 16; zOff++) {
                int worldZ = startZ + zOff;
                double nY = cacheY[xOff][zOff];

                for (int yIndex = 0; yIndex < HEIGHT; yIndex++) {
                    int y = MAX_Y - yIndex;
                    double nX = cacheX[zOff][yIndex];
                    double nZ = cacheZ[xOff][yIndex];

                    if (nX * nY * nZ <= threshold) continue;

                    pos.setPos(worldX, y, worldZ);
                    IBlockState state = world.getBlockState(pos);
                    Block target = state.getBlock();
                    if (target.isNormalCube(state, world, pos) && state.getMaterial() == Material.ROCK && target.isReplaceableOreGen(state, world, pos, replaceMatcher)) {
                        world.setBlockState(pos, this.block.getStateFromMeta(this.meta), 2 | 16);
                    }
                }
            }
        }
    }
}