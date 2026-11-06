package com.example.charpowers.hidden.model;

import java.util.*;
import java.util.random.RandomGenerator;

// Decides whether a new mob gets a hidden power, and which one.
// First the "1 in N" chance, then a rank by weight (only ranks that have powers), then any power of that rank.
public final class Roller {
    private final int animalOneIn;
    private final int otherOneIn;
    private final Map<Rank, Integer> weights;

    public Roller(int animalOneIn, int otherOneIn, Map<Rank, Integer> weights) {
        this.animalOneIn = Math.max(1, animalOneIn);
        this.otherOneIn = Math.max(1, otherOneIn);
        this.weights = new EnumMap<>(Rank.class);
        this.weights.putAll(weights);
    }

    public Optional<PowerDef> roll(boolean animal, List<PowerDef> all, RandomGenerator r) {
        int oneIn = animal ? animalOneIn : otherOneIn;
        if (r.nextInt(oneIn) != 0) return Optional.empty();

        Map<Rank, List<PowerDef>> byRank = new EnumMap<>(Rank.class);
        for (PowerDef p : all) byRank.computeIfAbsent(p.rank(), k -> new ArrayList<>()).add(p);
        if (byRank.isEmpty()) return Optional.empty();

        int total = 0;
        for (Rank rank : byRank.keySet()) total += Math.max(0, weights.getOrDefault(rank, 0));

        Rank picked = byRank.keySet().iterator().next(); // used when no weights are set at all
        if (total > 0) {
            int n = r.nextInt(total);
            for (Rank rank : byRank.keySet()) {
                n -= Math.max(0, weights.getOrDefault(rank, 0));
                if (n < 0) { picked = rank; break; }
            }
        }
        List<PowerDef> pool = byRank.get(picked);
        return Optional.of(pool.get(r.nextInt(pool.size())));
    }
}
