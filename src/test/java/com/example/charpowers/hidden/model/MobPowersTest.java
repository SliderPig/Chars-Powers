package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class MobPowersTest {
    @Test
    void rollsOnlyOnce() {
        assertTrue(MobRoll.shouldRoll(false), "a mob that never rolled rolls");
        assertFalse(MobRoll.shouldRoll(true), "a mob loaded again keeps what it had");
    }

    @Test
    void timerInRange() {
        SplittableRandom r = new SplittableRandom(5);
        for (int i = 0; i < 1000; i++) {
            int t = MobRoll.nextRandomUse(r, 60, 120);
            assertTrue(t >= 1200 && t <= 2400, "ticks " + t);
        }
    }

    @Test
    void timerCopesWithMinAboveMax() {
        assertEquals(1200, MobRoll.nextRandomUse(new SplittableRandom(1), 60, 30));
    }
}
