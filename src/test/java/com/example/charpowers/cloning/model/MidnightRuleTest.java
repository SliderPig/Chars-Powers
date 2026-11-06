package com.example.charpowers.cloning.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MidnightRuleTest {
    private static boolean run(MidnightRule r, long start, int seconds) {
        boolean done = false;
        for (int i = 0; i < seconds && !done; i++) done = r.step(true, start + i * 20L);
        return done;
    }

    @Test
    void startingAtMidnightTakesTheWholeMinute() {
        MidnightRule r = new MidnightRule();
        assertFalse(run(r, 18_000, 59));
        assertTrue(r.step(true, 18_000 + 59 * 20L));
    }

    @Test
    void startingEarlierAlsoCounts() {
        assertTrue(run(new MidnightRule(), 17_000, 60 + 50));
    }

    @Test
    void aMinuteThatMissesMidnightDoesNot() {
        assertFalse(run(new MidnightRule(), 6_000, 120));
        assertFalse(run(new MidnightRule(), 19_100, 40));
    }

    @Test
    void lookingAwayStartsAgain() {
        MidnightRule r = new MidnightRule();
        run(r, 17_990, 30);
        r.step(false, 18_590);
        assertFalse(run(r, 6_000, 100));
        assertFalse(r.sawMidnight());
    }

    @Test
    void timeSetMidnightWorks() {
        MidnightRule r = new MidnightRule();
        for (int i = 0; i < 59; i++) assertFalse(r.step(true, 18_000));
        assertTrue(r.step(true, 18_000));
    }

    @Test
    void arrivingMidWindowWorks() {
        assertTrue(run(new MidnightRule(), 18_700, 60));
    }

    @Test
    void midnightMayEndDuringTheMinute() {
        assertTrue(run(new MidnightRule(), 18_950, 60));
    }

    @Test
    void windowEdges() {
        assertTrue(MidnightRule.isMidnight(24_000L * 3 + 17_000));
        assertFalse(MidnightRule.isMidnight(16_999));
        assertFalse(MidnightRule.isMidnight(19_001));
    }

    @Test
    void worksOnLaterNights() {
        assertTrue(run(new MidnightRule(), 24_000 * 5 + 18_000, 60));
    }
}
