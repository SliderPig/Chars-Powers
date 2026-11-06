package com.example.charpowers.life.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LifeRulesTest {
    @Test
    void masteryGrowsTheNumbers() {
        assertEquals(4, LifeRules.cropRadius(1));
        assertEquals(16, LifeRules.cropRadius(100));
        assertEquals(5 * 1200, LifeRules.restedTicks(1, false));
        assertEquals(20 * 1200, LifeRules.restedTicks(100, false));
        assertEquals(2f, LifeRules.petHeal(49, false));
        assertEquals(4f, LifeRules.petHeal(50, false));
        assertEquals(8f, LifeRules.petHeal(50, true));
    }

    @Test
    void cropTicksStayCheap() {
        assertEquals(64, LifeRules.cropTicksPerSecond(16, 7, false));
        assertEquals(128, LifeRules.cropTicksPerSecond(16, 7, true));
        assertTrue(LifeRules.cropTicksPerSecond(4, 7, false) >= 4);
    }

    @Test
    void facingAllowsSomeSlack() {
        assertTrue(LifeRules.facing(90f, LifeRules.WEST_YAW));
        assertTrue(LifeRules.facing(140f, LifeRules.WEST_YAW));
        assertFalse(LifeRules.facing(0f, LifeRules.WEST_YAW));
        assertTrue(LifeRules.facing(270f, LifeRules.EAST_YAW));
        assertTrue(LifeRules.facing(-450f, LifeRules.EAST_YAW));
    }

    @Test
    void theSunsetWindow() {
        assertFalse(LifeRules.sunsetCanStart(6000));
        assertTrue(LifeRules.sunsetCanStart(11500));
        assertFalse(LifeRules.sunsetCanStart(12500));
        assertTrue(LifeRules.sunIsGone(13000));
        assertTrue(LifeRules.isSunrise(23500));
        assertTrue(LifeRules.isSunrise(100));
        assertFalse(LifeRules.isSunrise(1000));
        assertEquals(LifeRules.EAST_YAW, LifeRules.questFacing(23500));
        assertEquals(LifeRules.EAST_YAW, LifeRules.questFacing(5000));
        assertEquals(LifeRules.WEST_YAW, LifeRules.questFacing(7000));
    }
}
