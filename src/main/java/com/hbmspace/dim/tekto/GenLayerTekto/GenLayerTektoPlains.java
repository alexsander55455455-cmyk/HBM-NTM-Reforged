package com.hbmspace.dim.tekto.GenLayerTekto;

import com.hbmspace.dim.tekto.biome.BiomeGenBaseTekto;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import org.jetbrains.annotations.NotNull;

public class GenLayerTektoPlains extends GenLayer {

	private static final int polyvinylPlains = Biome.getIdForBiome(BiomeGenBaseTekto.polyvinylPlains);
	private static final int halogenHills = Biome.getIdForBiome(BiomeGenBaseTekto.halogenHills);

	public GenLayerTektoPlains(long seed, GenLayer parent) {
		super(seed);
		this.parent = parent;
	}

	@Override
	public int @NotNull [] getInts(int x, int z, int width, int height) {
		int i = x - 1;
		int j = z - 1;
		int k = 1 + width + 1;
		int l = 1 + height + 1;
		int[] input = this.parent.getInts(i, j, k, l);
		int[] output = IntCache.getIntCache(width * height);

		for (int zOut = 0; zOut < height; ++zOut) {
			for (int xOut = 0; xOut < width; ++xOut) {
				this.initChunkSeed((long) (xOut + x), (long) (zOut + z));
				int center = input[xOut + 1 + (zOut + 1) * k];

				if (center == halogenHills) {
					int north = input[xOut + 1 + (zOut + 1 - 1) * k];
					int east = input[xOut + 1 + 1 + (zOut + 1) * k];
					int west = input[xOut + 1 - 1 + (zOut + 1) * k];
					int south = input[xOut + 1 + (zOut + 1 + 1) * k];
					boolean nearPlains = north == polyvinylPlains || east == polyvinylPlains || west == polyvinylPlains || south == polyvinylPlains;
					if (nearPlains) {
						center = halogenHills;
					}
				}

				output[xOut + zOut * width] = center;
			}
		}

		return output;
	}
}