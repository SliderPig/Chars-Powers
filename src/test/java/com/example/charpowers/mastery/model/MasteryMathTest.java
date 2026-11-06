package com.example.charpowers.mastery.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MasteryMathTest {
    @Test
    void startsAtLevelOneAndEndsAtOneHundredHours() {
        assertEquals(1, MasteryMath.levelOf(0));
        assertEquals(100, MasteryMath.levelOf(360_000));
        assertEquals(99, MasteryMath.levelOf(359_999));
        assertEquals(100, MasteryMath.levelOf(9_999_999));
    }

    @Test
    void levelsGetMoreExpensive() {
        long last = 0;
        for (int l = 2; l <= 100; l++) {
            long cost = MasteryMath.pointsFor(l) - MasteryMath.pointsFor(l - 1);
            assertTrue(cost >= last, "level " + l);
            last = cost;
        }
    }

    @Test
    void levelAndPointsAgree() {
        for (int l = 1; l <= 100; l++) assertEquals(l, MasteryMath.levelOf(MasteryMath.pointsFor(l)));
    }
}
