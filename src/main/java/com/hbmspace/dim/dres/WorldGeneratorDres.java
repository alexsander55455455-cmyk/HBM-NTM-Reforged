package com.hbmspace.dim.dres;

import com.hbm.blocks.ModBlocks;
import com.hbm.world.gen.nbt.JigsawPiece;
import com.hbm.world.gen.nbt.JigsawPool;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import com.hbmspace.blocks.ModBlocksSpace;
import com.hbm.config.GeneralConfig;
import com.hbmspace.blocks.generic.BlockOre;
import com.hbmspace.config.SpaceConfig;
import com.hbm.config.WorldConfig;
import com.hbmspace.dim.CelestialBody;
import com.hbmspace.dim.SolarSystem;
import com.hbmspace.dim.WorldGeneratorCelestial;
import com.hbmspace.dim.WorldProviderCelestial;
import com.hbmspace.dim.dres.biome.BiomeGenBaseDres;
import com.hbmspace.main.StructureManagerSpace;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WorldGeneratorDres implements IWorldGenerator {
    
    public WorldGeneratorDres(){
        Map<Block, StructureComponent.BlockSelector> tiles = new HashMap<>() {{
            put(ModBlocks.tile_lab, new StructureComponent.BlockSelector() {
                @Override
                public void selectBlocks(Random rand, int posX, int posY, int posZ, boolean notInterior) {
                    float chance = rand.nextFloat();
                    if (chance < 0.5F) {
                        this.blockstate = ModBlocks.tile_lab.getDefaultState();
                    } else if (chance < 0.9F) {
                        this.blockstate = ModBlocks.tile_lab_cracked.getDefaultState();
                    } else {
                        this.blockstate = ModBlocks.tile_lab_broken.getDefaultState();
                    }
                }
            });
        }};

        NBTStructure.registerStructure(SpaceConfig.dresDimension, new SpawnCondition("dres_rbmk") {{
            spawnWeight = 8;
            minHeight = 40;
            maxHeight = 40;
            sizeLimit = 128;
            rangeLimit = 64;
            canSpawn = biome -> biome == BiomeGenBaseDres.dresPlains;
            startPool = "start";
            pools = new HashMap<>() {{
                put("start", new JigsawPool() {{
                    add(new JigsawPiece("dres_core", StructureManagerSpace.dres_core) {{ blockTable = tiles; }}, 1);
                }});
                put("default", new JigsawPool() {{
                    add(new JigsawPiece("dres_t", StructureManagerSpace.dres_t) {{ blockTable = tiles; }}, 1);
                    add(new JigsawPiece("dres_airlock", StructureManagerSpace.dres_airlock) {{ blockTable = tiles; }}, 1);
                    add(new JigsawPiece("dres_dome", StructureManagerSpace.dres_dome) {{ blockTable = tiles; }}, 1);
                    add(new JigsawPiece("dres_pool", StructureManagerSpace.dres_pool) {{ blockTable = tiles; }}, 1);
                    fallback = "inback";
                }});
                put("outside", new JigsawPool() {{
                    add(new JigsawPiece("dres_balcony", StructureManagerSpace.dres_balcony) {{ blockTable = tiles; }}, 1);
                    add(new JigsawPiece("dres_pad", StructureManagerSpace.dres_pad) {{ blockTable = tiles; }}, 1);
                    fallback = "outback";
                }});
                put("reactor", new JigsawPool() {{
                    add(new JigsawPiece("dres_hall_starbmk", StructureManagerSpace.dres_hall_starbmk) {{ blockTable = tiles; }}, 5);
                    add(new JigsawPiece("dres_hall_breeder", StructureManagerSpace.dres_hall_breeder) {{ blockTable = tiles; }}, 1);
                }});
                put("inback", new JigsawPool() {{
                    add(new JigsawPiece("dres_incap", StructureManagerSpace.dres_incap) {{ blockTable = tiles; }}, 1);
                }});
                put("outback", new JigsawPool() {{
                    add(new JigsawPiece("dres_outcap", StructureManagerSpace.dres_outcap) {{ blockTable = tiles; }}, 1);
                }});
            }};
        }});

        NBTStructure.registerNullWeight(SpaceConfig.dresDimension, 16);

        BlockOre.addValidBody(ModBlocksSpace.ore_shale, SolarSystem.Body.DRES);
        BlockOre.addValidBody(ModBlocksSpace.ore_lanthanium, SolarSystem.Body.DRES);
        BlockOre.addValidBody(ModBlocksSpace.ore_niobium, SolarSystem.Body.DRES);
        BlockOre.addValidBody(ModBlocks.ore_coltan, SolarSystem.Body.DRES);
        BlockOre.addValidBody(ModBlocksSpace.ore_lanthanium, SolarSystem.Body.DRES);
    }

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
		if(world.provider.getDimension() == SpaceConfig.dresDimension) {
			generateDres(world, random, chunkX * 16, chunkZ * 16);
		}
	}

	private void generateDres(World world, Random rand, int i, int j) {
        int meta = CelestialBody.getMeta(world);
        Block stone = ((WorldProviderCelestial) world.provider).getStone();

        WorldGeneratorCelestial.generateOre(world, rand, i, j, WorldConfig.cobaltSpawn, 4, 3, 22, ModBlocksSpace.ore_cobalt.getStateFromMeta(meta), stone);
        WorldGeneratorCelestial.generateOre(world, rand, i, j, WorldConfig.copperSpawn, 9, 4, 27, ModBlocksSpace.ore_iron.getStateFromMeta(meta), stone);
        WorldGeneratorCelestial.generateOre(world, rand, i, j, 12,  8, 1, 33, ModBlocksSpace.ore_niobium.getStateFromMeta(meta), stone);
        WorldGeneratorCelestial.generateOre(world, rand, i, j, GeneralConfig.coltanRate, 4, 15, 40, ModBlocks.ore_coltan.getStateFromMeta(meta), stone);
        WorldGeneratorCelestial.generateOre(world, rand, i, j, 1, 6, 4, 64, ModBlocksSpace.ore_lanthanium.getStateFromMeta(meta), stone);

        WorldGeneratorCelestial.generateOre(world, rand, i, j, 1, 12, 8, 32, ModBlocksSpace.ore_shale.getStateFromMeta(meta), stone);
	}
}