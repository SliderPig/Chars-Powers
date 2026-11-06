package com.example.charpowers.armour.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SuitMotionTest {
    @Test
    void slownessGoesFromVToIEvenly() {
        assertEquals(4, SuitMotion.slownessAmplifier(1));
        assertEquals(0, SuitMotion.slownessAmplifier(100));
        int last = 4;
        for (int level = 1; level <= 100; level++) {
            int now = SuitMotion.slownessAmplifier(level);
            assertTrue(now <= last && last - now <= 1, "level " + level);
            last = now;
        }
        assertEquals(2, SuitMotion.slownessAmplifier(50));
    }

    @Test
    void theSuitOpensClosesAndDropsItsHelmetInOrder() {
        assertEquals(0, SuitMotion.open(0, 10), 1e-9);
        assertEquals(1, SuitMotion.open(1, SuitMotion.OPEN_END), 1e-9);
        // legs shut first, then chest, then arms
        double t = SuitMotion.OPEN_END + 7;
        assertTrue(SuitMotion.open(0, t) < SuitMotion.open(1, t) && SuitMotion.open(1, t) < SuitMotion.open(2, t));
        assertEquals(0, SuitMotion.open(2, SuitMotion.CLOSE_END), 1e-9);
        // the helmet lifts while it opens, waits, drops with a bounce and ends on the head
        assertEquals(0, SuitMotion.helmet(0), 1e-9);
        assertEquals(SuitMotion.HELMET_HOVER, SuitMotion.helmet(SuitMotion.CLOSE_END - 1), 1e-9);
        assertEquals(0, SuitMotion.helmet(SuitMotion.LAND), 1e-9);
        assertTrue(SuitMotion.helmet(SuitMotion.LAND + 1) > 0);
        assertEquals(0, SuitMotion.helmet(SuitMotion.TOTAL), 0.2);
        assertEquals(0, SuitMotion.glide(10), 1e-9);
        assertEquals(1, SuitMotion.glide(SuitMotion.CLOSE_END), 1e-9);
    }

    @Test
    void sendingAwayIsTheSameTimelineBackwards() {
        assertEquals(0, SuitMotion.timeline(0, false), 1e-9);
        assertEquals(SuitMotion.TOTAL, SuitMotion.timeline(0, true), 1e-9);
        assertEquals(0, SuitMotion.timeline(500, true), 1e-9);
        // going away the helmet is the first thing to lift off
        assertTrue(SuitMotion.helmet(SuitMotion.timeline(3, true)) > 0);
        assertEquals(SuitMotion.LEAN, SuitMotion.lean(0), 1e-9);
        assertEquals(0, SuitMotion.lean(SuitMotion.FLY), 1e-9);
    }

    @Test
    void soundsFireOnceWhicheverWayTimeRuns() {
        assertEquals(java.util.List.of(SuitMotion.Beat.HELMET), SuitMotion.beats(SuitMotion.LAND - 1, SuitMotion.LAND));
        assertEquals(java.util.List.of(SuitMotion.Beat.HELMET), SuitMotion.beats(SuitMotion.LAND, SuitMotion.LAND - 1));
        assertTrue(SuitMotion.beats(0, SuitMotion.TOTAL).size() == 5);
        assertTrue(SuitMotion.beats(SuitMotion.LAND, SuitMotion.LAND).isEmpty());
    }

    @Test
    void curveStartsAndEndsWhereItShould() {
        double[] a = {0, 10, 0}, b = {40, 64, 30};
        double[] c = SuitMotion.bow(a, b);
        assertArrayEquals(a, SuitMotion.curve(a, c, b, 0), 1e-9);
        assertArrayEquals(b, SuitMotion.curve(a, c, b, 1), 1e-9);
        assertTrue(SuitMotion.curve(a, c, b, 0.5)[1] > 37);
    }

    @Test
    void walkingStepsAndHeavyLanding() {
        SuitMotion.Walk w = new SuitMotion.Walk();
        int steps = 0;
        for (int i = 0; i < 100; i++) if (w.update(0.1, true, 64) == SuitMotion.Step.STEP) steps++;
        assertEquals(8, steps); // a step every 12 ticks of 0.1
        assertEquals(SuitMotion.Step.NONE, w.update(0, true, 64));
        for (int i = 0; i < 6; i++) w.update(0, false, 70 - i);
        assertEquals(SuitMotion.Step.LAND, w.update(0, true, 64));
        assertTrue(w.drop >= 5);
    }
}
