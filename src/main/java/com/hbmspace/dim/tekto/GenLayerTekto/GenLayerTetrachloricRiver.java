package com.hbmspace.dim.tekto.GenLayerTekto;

import com.hbmspace.dim.tekto.biome.BiomeGenBaseTekto;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import org.jetbrains.annotations.NotNull;

public class GenLayerTetrachloricRiver extends GenLayer {

	private static final int tetrachloricRiver = Biome.getIdForBiome(BiomeGenBaseTekto.tetrachloricRiver);

	public GenLayerTetrachloricRiver(long seed, GenLayer parent) {
		super(seed);
		this.parent = parent;
	}

	@Override
	public int @NotNull [] getInts(int x, int z, int width, int height) {
		int i = x - 1;
		int j = z - 1;
		int k = width + 2;
		int l = height + 2;
		int[] input = this.parent.getInts(i, j, k, l);
		int[] output = IntCache.getIntCache(width * height);

		for (int zOut = 0; zOut < height; ++zOut) {
			for (int xOut = 0; xOut < width; ++xOut) {
				int west = normalizeRiver(input[xOut + 0 + (zOut + 1) * k]);
				int east = normalizeRiver(input[xOut + 2 + (zOut + 1) * k]);
				int north = normalizeRiver(input[xOut + 1 + (zOut + 0) * k]);
				int south = normalizeRiver(input[xOut + 1 + (zOut + 2) * k]);
				int center = normalizeRiver(input[xOut + 1 + (zOut + 1) * k]);

				if (center == west && center == north && center == east && center == south) {
					output[xOut + zOut * width] = -1;
				} else {
					output[xOut + zOut * width] = tetrachloricRiver;
				}
			}
		}

		return output;
	}

	private int normalizeRiver(int biomeId) {
		return biomeId >= 2 ? 2 + (biomeId & 1) : biomeId;
	}
}