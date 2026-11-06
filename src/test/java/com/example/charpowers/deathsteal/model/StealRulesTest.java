package com.example.charpowers.deathsteal.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StealRulesTest {
    @Test
    void onlyOncePerKillerUntilNinety() {
        assertTrue(StealRules.canStealFrom(0, 1));
        assertFalse(StealRules.canStealFrom(1, 89));
        assertTrue(StealRules.canStealFrom(1, 90));
        assertFalse(StealRules.canStealFrom(2, 100));
    }

    @Test
    void someThingsCantBeTaken() {
        assertTrue(StealRules.stealable("eop:pyrokinesis"));
        assertFalse(StealRules.stealable("charpowers:death_steal"));
        assertFalse(StealRules.stealable("charpowers:teacher"));
        assertFalse(StealRules.stealable("heroes:base_power"));
    }

    private static int facing(int level, int count, long tick) {
        int n = 0;
        for (int i = 0; i < count; i++) if (StealRules.faces(level, i, count, tick)) n++;
        return n;
    }

    @Test
    void iconsShowThemselvesWithMastery() {
        for (long t = 0; t < 400; t++) assertEquals(0, facing(24, 5, t));
        assertEquals(1, facing(25, 5, 0));
        assertEquals(0, facing(25, 5, 10));
        assertEquals(2, facing(75, 5, 0));
        assertEquals(0, facing(75, 5, 30));
        assertEquals(5, facing(100, 5, 7));
        // with only one icon, the two at 75 are the same one
        assertEquals(1, facing(75, 1, 0));
    }

    @Test
    void podiumsLineUpInRows() {
        assertArrayEquals(new int[] {0, 5}, StealRules.spot(0, 1));
        assertArrayEquals(new int[] {-3, 5}, StealRules.spot(0, 3));
        assertArrayEquals(new int[] {3, 5}, StealRules.spot(2, 3));
        assertEquals(9, StealRules.spot(10, 12)[1]);
    }
}
