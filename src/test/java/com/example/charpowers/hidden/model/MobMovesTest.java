package com.example.charpowers.hidden.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Random;

class MobMovesTest {
    @Test
    void thePowersWrittenForPlayersEachHaveAMove() {
        assertEquals(114, MobMoves.count());
        assertEquals(MobMoves.Move.LEAP, MobMoves.of("frog_legs"));
        assertEquals(MobMoves.Move.THROW, MobMoves.of("pet_rock"));
        assertEquals(MobMoves.Move.HEAL, MobMoves.of("bread_lover"));
    }

    @Test
    void aPowerWithNoMoveJustGathers() {
        assertFalse(MobMoves.known("brand_new_power"));
        assertEquals(MobMoves.Move.GATHER, MobMoves.of("brand_new_power"));
    }

    @Test
    void farmMobsEndUpAtTheFarmChance() {
        Random r = new Random(5);
        int kept = 0, tries = 200_000;
        for (int i = 0; i < tries; i++) {
            if (FarmRule.keeps(20, 200, r)) kept++;
        }
        assertEquals(0.1, kept / (double) tries, 0.01); // won at 1 in 20, then 1 in 10 of those stay: 1 in 200
    }

    @Test
    void zeroMeansNeverAndARarerNormalChanceIsNotMadeRarer() {
        Random r = new Random(5);
        for (int i = 0; i < 1000; i++) {
            assertFalse(FarmRule.keeps(20, 0, r));
            assertTrue(FarmRule.keeps(500, 200, r));
        }
    }
}
