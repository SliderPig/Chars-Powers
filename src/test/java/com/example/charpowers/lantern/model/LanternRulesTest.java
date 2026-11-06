package com.example.charpowers.lantern.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LanternRulesTest {
    @Test
    void masteryGrowsTheNumbers() {
        assertEquals(100, LanternRules.maxBattery(1), 0.001);
        assertEquals(200, LanternRules.maxBattery(100), 0.001);
        assertEquals(10, LanternRules.maxDaggers(24));
        assertEquals(15, LanternRules.maxDaggers(25));
        assertEquals(10, LanternRules.bubbleRadius(49));
        assertEquals(15, LanternRules.bubbleRadius(50));
        assertEquals(1, LanternRules.maxTurrets(74, false));
        assertEquals(2, LanternRules.maxTurrets(75, false));
        assertEquals(3, LanternRules.maxTurrets(100, true));
        assertEquals(50, LanternRules.nukeCost(99));
        assertEquals(30, LanternRules.nukeCost(100));
        assertTrue(LanternRules.damage(10, 100) > LanternRules.damage(10, 1));
    }

    @Test
    void theOathForgivesCapitalsAndPunctuation() {
        assertTrue(LanternRules.oathSaid("in biggest day on maccas night no big mac shall escape my sight", LanternRules.DEFAULT_OATH));
        assertTrue(LanternRules.oathSaid("  IN BIGGEST DAY, ON MACCAS NIGHT,  NO BIG MAC SHALL ESCAPE MY SIGHT!!", LanternRules.DEFAULT_OATH));
        assertFalse(LanternRules.oathSaid("in brightest day", LanternRules.DEFAULT_OATH));
    }

    @Test
    void theNukeFallsOffWithDistance() {
        assertTrue(LanternRules.nukeDamage(0) > LanternRules.nukeDamage(50));
        assertTrue(LanternRules.nukeDamage(100) > 0);
    }

    @Test
    void swordsFanOutEvenly() {
        assertEquals(0, LanternRules.fan(0, 1)[0], 0.001);
        assertEquals(-LanternRules.fan(0, 5)[0], LanternRules.fan(4, 5)[0], 0.001);
        assertEquals(0, LanternRules.fan(2, 5)[0], 0.001);
    }
}
