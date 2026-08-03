package com.hbm.world.gen.nbt;

import com.hbm.util.Tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

// A set of pieces with weights
public class JigsawPool {

	// Weighted list of pieces to pick from
	List<Pair<JigsawPiece, Integer>> pieces = new ArrayList<>();
	int totalWeight = 0;

	public String fallback;

	private boolean isClone;

	public void add(JigsawPiece piece, int weight) {
		if(weight <= 0) throw new IllegalStateException("JigsawPool spawn weight must be positive!");
		pieces.add(new Pair<>(piece, weight));
		totalWeight += weight;
	}

	public int getAverageWeight() {
		if(pieces.isEmpty()) return 1;
		return totalWeight / pieces.size();
	}

	protected JigsawPool clone() {
		JigsawPool clone = new JigsawPool();
		clone.pieces = new ArrayList<>(this.pieces);
		clone.fallback = this.fallback;
		clone.totalWeight = this.totalWeight;
		clone.isClone = true;

		return clone;
	}

	// If from a clone, will remove from the pool
	public JigsawPiece get(Random rand) {
		if(totalWeight <= 0) return null;
		int weight = rand.nextInt(totalWeight);

		for(int i = 0; i < pieces.size(); i++) {
			Pair<JigsawPiece, Integer> pair = pieces.get(i);
			weight -= pair.getValue();

			if(weight < 0) {
				if(isClone) {
					pieces.remove(i);
					totalWeight -= pair.getValue();
				}

				return pair.getKey();
			}
		}

		return null;
	}

	/**
	 * Selects a required piece when this pool contains one, otherwise uses normal weighted selection.
	 */
	public JigsawPiece getRequired(Random rand, Set<JigsawPiece> requiredPieces) {
		if(requiredPieces == null || requiredPieces.isEmpty()) return get(rand);

		int requiredWeight = 0;
		for(Pair<JigsawPiece, Integer> pair : pieces) {
			if(requiredPieces.contains(pair.getKey())) requiredWeight += pair.getValue();
		}
		if(requiredWeight <= 0) return get(rand);

		int weight = rand.nextInt(requiredWeight);
		for(int i = 0; i < pieces.size(); i++) {
			Pair<JigsawPiece, Integer> pair = pieces.get(i);
			if(!requiredPieces.contains(pair.getKey())) continue;

			weight -= pair.getValue();
			if(weight < 0) {
				if(isClone) {
					pieces.remove(i);
					totalWeight -= pair.getValue();
				}
				return pair.getKey();
			}
		}
		return null;
	}

	public boolean containsRequired(Set<JigsawPiece> requiredPieces) {
		if(requiredPieces == null || requiredPieces.isEmpty()) return false;
		for(Pair<JigsawPiece, Integer> pair : pieces) {
			if(requiredPieces.contains(pair.getKey())) return true;
		}
		return false;
	}

}
