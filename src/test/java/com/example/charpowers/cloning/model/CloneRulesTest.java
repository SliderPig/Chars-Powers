package com.example.charpowers.cloning.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CloneRulesTest {
    @Test
    void capFollowsThePowersList() {
        assertEquals(1, CloneRules.cap(1));
        assertEquals(1, CloneRules.cap(19));
        assertEquals(2, CloneRules.cap(20));
        assertEquals(2, CloneRules.cap(59));
        assertEquals(3, CloneRules.cap(60));
    }

    @Test
    void onePressSummonsOneUntilDogPile() {
        assertEquals(1, CloneRules.toSummon(20, 0));
        assertEquals(1, CloneRules.toSummon(20, 1));
        assertEquals(0, CloneRules.toSummon(20, 2));
        assertEquals(3, CloneRules.toSummon(60, 0));
        assertEquals(2, CloneRules.toSummon(60, 1));
        assertEquals(0, CloneRules.toSummon(60, 3));
    }

    @Test
    void levelOneHundredHalvesTheLockAndCutsTheRogueRisk() {
        assertEquals(24_000, CloneRules.lockTicks(99));
        assertEquals(12_000, CloneRules.lockTicks(100));
        assertTrue(CloneRules.rollsRogue(99, () -> 0.19));
        assertFalse(CloneRules.rollsRogue(99, () -> 0.20));
        assertTrue(CloneRules.rollsRogue(100, () -> 0.04));
        assertFalse(CloneRules.rollsRogue(100, () -> 0.05));
    }

    @Test
    void awakeningMeansFiveStrongerClones() {
        assertEquals(5, CloneRules.cap(100, true));
        assertEquals(3, CloneRules.cap(100, false));
        assertEquals(5, CloneRules.toSummon(100, true, 0));
        assertEquals(1, CloneRules.toSummon(100, true, 4));
        assertEquals(0, CloneRules.toSummon(100, true, 5));
        assertEquals(59.7, CloneRules.maxHealth(100, true), 0.001);
    }

    @Test
    void nicknamesAreKeptShortAndPlain() {
        assertEquals("SliderM", CloneRules.cleanNickname("  SliderM "));
        assertEquals("Miner", CloneRules.cleanNickname("\u00a7Miner\n"));
        assertEquals("", CloneRules.cleanNickname("   "));
        assertEquals("ABCDEFGHIJKL", CloneRules.cleanNickname("ABCDEFGHIJKLMNOP"));
    }

    @Test
    void healthGrowsALittle() {
        assertEquals(20.0, CloneRules.maxHealth(1), 0.001);
        assertEquals(39.8, CloneRules.maxHealth(100), 0.001);
    }

    @Test
    void betterMaterialBeatsEnchantedWorseMaterial() {
        assertTrue(GearRank.armor(3, 0, 12) < GearRank.armor(4, 0, 0));
        assertTrue(GearRank.tool(2, 30) < GearRank.tool(3, 0));
        assertTrue(GearRank.weapon(6, 5) > GearRank.weapon(6, 0));
    }
}
