package com.hbmspace.dim.tekto.GenLayerTekto;

import com.hbmspace.dim.tekto.biome.BiomeGenBaseTekto;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import org.jetbrains.annotations.NotNull;

public class GenLayerDiversifyTekto extends GenLayer {

	private static final Biome[] biomes = new Biome[] {
			BiomeGenBaseTekto.polyvinylPlains,
			BiomeGenBaseTekto.halogenHills,
			BiomeGenBaseTekto.tetrachloricRiver,
			BiomeGenBaseTekto.vinylsands
	};

	public GenLayerDiversifyTekto(long seed, GenLayer parent) {
		super(seed);
		this.parent = parent;
	}

	@Override
	public int @NotNull [] getInts(int x, int z, int width, int depth) {
		int[] input = this.parent.getInts(x, z, width, depth);
		int[] output = IntCache.getIntCache(width * depth);

		for (int zOut = 0; zOut < depth; zOut++) {
			for (int xOut = 0; xOut < width; xOut++) {
				int i = xOut + zOut * width;
				int center = input[i];
				initChunkSeed(xOut + x, zOut + z);
				if (nextInt(2) == 0) {
					output[i] = Biome.getIdForBiome(biomes[nextInt(biomes.length)]);
				} else {
					output[i] = center;
				}
			}
		}

		return output;
	}
}