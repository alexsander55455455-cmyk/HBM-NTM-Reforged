package com.hbmspace.dim.moon;

import com.hbm.world.gen.nbt.JigsawPiece;
import com.hbm.world.gen.nbt.JigsawPool;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import com.hbmspace.blocks.ModBlocksSpace;
import com.hbmspace.config.SpaceConfig;
import com.hbm.config.WorldConfig;
import com.hbmspace.blocks.generic.BlockOre;
import com.hbmspace.dim.CelestialBody;
import com.hbmspace.dim.SolarSystem;
import com.hbmspace.dim.WorldGeneratorCelestial;
import com.hbmspace.main.StructureManagerSpace;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.HashMap;
import java.util.Random;

public class WorldGeneratorMoon implements IWorldGenerator {

	public WorldGeneratorMoon() {
		JigsawPiece munBase = new JigsawPiece("mun_base", StructureManagerSpace.mun_base) {{ alignToTerrain = true; heightOffset = -1; }};

		NBTStructure.registerStructure(SpaceConfig.moonDimension, new SpawnCondition("mun_base") {{
			spawnWeight = 6;
			sizeLimit = 32;
			startPool = "start";
			pools = new HashMap<String, JigsawPool>() {{
				put("start", new JigsawPool() {{
					add(munBase, 1);
				}});
				put("default", new JigsawPool() {{
					add(munBase, 1);
					add(new JigsawPiece("mun_flag", StructureManagerSpace.mun_flag) {{ alignToTerrain = true; heightOffset = -1; }}, 2);
					add(new JigsawPiece("mun_panels", StructureManagerSpace.mun_panels) {{ alignToTerrain = true; heightOffset = -1; }}, 6);
					add(new JigsawPiece("mun_stardar", StructureManagerSpace.mun_stardar) {{ alignToTerrain = true; heightOffset = -1; }}, 1);
					add(new JigsawPiece("mun_tanks", StructureManagerSpace.mun_tanks) {{ alignToTerrain = true; heightOffset = -1; }}, 6);
				}});
				put("connect", new JigsawPool() {{
					add(new JigsawPiece("mun_connector_1", StructureManagerSpace.mun_connector_1), 1);
					add(new JigsawPiece("mun_connector_2", StructureManagerSpace.mun_connector_2), 1);
					add(new JigsawPiece("mun_connector_3", StructureManagerSpace.mun_connector_3), 1);
					add(new JigsawPiece("mun_connector_s", StructureManagerSpace.mun_connector_s), 1);
					add(new JigsawPiece("mun_connector_l", StructureManagerSpace.mun_connector_l), 1);
					add(new JigsawPiece("mun_connector_t", StructureManagerSpace.mun_connector_t), 1);
				}});
			}};
		}});

        NBTStructure.registerNullWeight(SpaceConfig.moonDimension, 18);

        BlockOre.addValidBody(ModBlocksSpace.ore_lithium, SolarSystem.Body.MUN);
		BlockOre.addValidBody(ModBlocksSpace.ore_quartz, SolarSystem.Body.MUN);
		BlockOre.addValidBody(ModBlocksSpace.ore_shale, SolarSystem.Body.MUN);

		BlockOre.addValidBody(ModBlocksSpace.ore_brine, SolarSystem.Body.MUN);
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
		if(world.provider.getDimension() == SpaceConfig.moonDimension) {
			generateMoon(world, random, chunkX * 16, chunkZ * 16); 
		}
	}

	private void generateMoon(World world, Random rand, int i, int j) {
		int meta = CelestialBody.getMeta(world);

		WorldGeneratorCelestial.generateOre(world, rand, i, j, WorldConfig.lithiumSpawn,  6, 4, 8, ModBlocksSpace.ore_lithium.getStateFromMeta(meta), ModBlocksSpace.moon_rock);
		WorldGeneratorCelestial.generateOre(world, rand, i, j, WorldConfig.aluminiumSpawn,  6, 5, 40, ModBlocksSpace.ore_aluminium.getStateFromMeta(meta), ModBlocksSpace.moon_rock);
        WorldGeneratorCelestial.generateOre(world, rand, i, j, WorldConfig.fluoriteSpawn, 4, 5, 45, ModBlocksSpace.ore_fluorite.getStateFromMeta(meta), ModBlocksSpace.moon_rock);
        WorldGeneratorCelestial.generateOre(world, rand, i, j, 10, 13, 5, 64, ModBlocksSpace.ore_quartz.getStateFromMeta(meta), ModBlocksSpace.moon_rock);

        WorldGeneratorCelestial.generateOre(world, rand, i, j, 1, 12, 8, 32, ModBlocksSpace.ore_shale.getStateFromMeta(meta), ModBlocksSpace.moon_rock);
	}
}