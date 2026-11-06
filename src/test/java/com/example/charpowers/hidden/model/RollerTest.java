package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class RollerTest {
    static final Map<Rank, Integer> WEIGHTS = Map.of(Rank.F, 400, Rank.E, 300, Rank.D, 178, Rank.C, 100, Rank.B, 20, Rank.A, 2);

    static List<PowerDef> fPowers(int n) {
        List<PowerDef> out = new ArrayList<>();
        for (int i = 1; i <= n; i++) out.add(new PowerDef("p" + i, i, Rank.F, Kind.KEY, MobRule.RANDOM, 200));
        return out;
    }

    // A random source whose first nextInt answer is fixed (the "1 in N" draw); the rest come from a seeded random.
    static RandomGenerator firstDraw(int first) {
        SplittableRandom rest = new SplittableRandom(7);
        return new RandomGenerator() {
            boolean used;
            @Override public long nextLong() { return rest.nextLong(); }
            @Override public int nextInt(int bound) {
                if (!used) { used = true; return first; }
                return rest.nextInt(bound);
            }
        };
    }

    @Test
    void animalsOneIn100() {
        Roller r = new Roller(100, 20, WEIGHTS);
        assertTrue(r.roll(true, fPowers(4), firstDraw(5)).isEmpty());
        assertTrue(r.roll(true, fPowers(4), firstDraw(0)).isPresent());
    }

    @Test
    void otherOneIn20() {
        Roller r = new Roller(100, 20, WEIGHTS);
        assertTrue(r.roll(false, fPowers(4), firstDraw(19)).isEmpty());
        assertTrue(r.roll(false, fPowers(4), firstDraw(0)).isPresent());
    }

    @Test
    void onlyFExistsSoAlwaysF() {
        Roller r = new Roller(1, 1, WEIGHTS);
        SplittableRandom rnd = new SplittableRandom(3);
        for (int i = 0; i < 200; i++) assertEquals(Rank.F, r.roll(false, fPowers(4), rnd).orElseThrow().rank());
    }

    @Test
    void evenWithinRank() {
        Roller r = new Roller(1, 1, WEIGHTS);
        SplittableRandom rnd = new SplittableRandom(1);
        Map<String, Integer> count = new HashMap<>();
        for (int i = 0; i < 10000; i++) count.merge(r.roll(true, fPowers(4), rnd).orElseThrow().id(), 1, Integer::sum);
        assertEquals(4, count.size());
        count.values().forEach(c -> assertTrue(Math.abs(c - 2500) <= 250, "count " + c));
    }
}
