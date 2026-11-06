package com.example.charpowers.futuresight.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SightRulesTest {
    @Test
    void rangesGrowWithMastery() {
        assertEquals(8, SightRules.oreRange(1));
        assertEquals(32, SightRules.oreRange(100));
        assertEquals(1.0, SightRules.warnSeconds(1), 0.001);
        assertEquals(3.0, SightRules.warnSeconds(100), 0.001);
    }

    @Test
    void directionsMatchWhereYouFace() {
        // facing south (yaw 0): +z is ahead, -x is to the right
        assertEquals("ahead", SightRules.direction(0, 0, 5, 0));
        assertEquals("behind you", SightRules.direction(0, 0, -5, 0));
        assertEquals("to the right", SightRules.direction(-5, 0, 0, 0));
        assertEquals("below and to the left", SightRules.direction(5, -4, 0, 0));
        // facing west (yaw 90): -x is ahead
        assertEquals("ahead", SightRules.direction(-5, 0, 0, 90));
        assertEquals("right here", SightRules.direction(0, 0, 0, 0));
        assertEquals("above", SightRules.direction(0, 5, 0, 0));
    }

    @Test
    void oreNamesReadNicely() {
        assertEquals("diamonds", SightRules.oreName("deepslate_diamond_ore"));
        assertEquals("iron", SightRules.oreName("iron_ore"));
        assertEquals("gold", SightRules.oreName("nether_gold_ore"));
        assertEquals("ancient debris", SightRules.oreName("ancient_debris"));
    }

    @Test
    void theExactCountWaitsForOneHundred() {
        assertEquals("diamonds... below", SightRules.oreHint("diamonds", "below", 4, 99));
        assertEquals("4 diamonds... below", SightRules.oreHint("diamonds", "below", 4, 100));
    }

    @Test
    void warningsCountDown() {
        assertEquals(2.0, SightRules.secondsAway(7, 2.5, 2), 0.001);
        assertEquals(0, SightRules.secondsAway(1, 2.5, 2));
        assertEquals("a creeper behind you in 3...", SightRules.threat("creeper", "behind you", 2.5));
        assertEquals("a creeper behind you!", SightRules.threat("creeper", "behind you", 0.5));
    }
}
