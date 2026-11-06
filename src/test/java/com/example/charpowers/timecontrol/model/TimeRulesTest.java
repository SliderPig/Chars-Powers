package com.example.charpowers.timecontrol.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TimeRulesTest {
    @Test
    void topSpeedGrowsWithMastery() {
        assertEquals(100, TimeRules.topSpeed(1));
        assertEquals(1000, TimeRules.topSpeed(100));
    }

    @Test
    void speedStaysInsideItsLimits() {
        assertEquals(100, TimeRules.nudgeSpeed(100, false, 100));
        assertEquals(150, TimeRules.nudgeSpeed(100, true, 100));
        assertEquals(100, TimeRules.nudgeSpeed(100, true, 1));
        assertEquals(1000, TimeRules.nudgeSpeed(1000, true, 100));
    }

    @Test
    void chunkLimitByMastery() {
        assertEquals(1, TimeRules.chunkLimit(74));
        assertEquals(2, TimeRules.chunkLimit(75));
        assertEquals(3, TimeRules.chunkLimit(100));
    }

    @Test
    void theEndTakesOneHundredDays() {
        assertEquals(2_400_000L, TimeRules.END_TICKS_NEEDED);
    }
}
