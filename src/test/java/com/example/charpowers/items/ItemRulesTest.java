package com.example.charpowers.items;

import com.example.charpowers.items.model.ComboRules;
import com.example.charpowers.items.model.DropRules;
import com.example.charpowers.items.model.RockShape;
import com.example.charpowers.items.model.SolgoRules;
import com.example.charpowers.items.model.SprayRules;
import com.example.charpowers.items.model.SprayRules.Liquid;
import com.example.charpowers.items.model.TrailMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemRulesTest {
    // Plays a whole chain with a click right after each move ends.
    private static ComboRules.State onBeat() {
        ComboRules.State s = new ComboRules.State();
        long t = 100;
        for (int want = 0; want < 4; want++) {
            assertEquals(ComboRules.Outcome.STARTED, ComboRules.press(s, t));
            assertEquals(want, s.step);
            t += ComboRules.length(want) + 2;
        }
        return s;
    }

    @Test
    void cleanTimingReachesTheFinisher() {
        ComboRules.State s = onBeat();
        assertEquals(ComboRules.FINISHER, s.step);
    }

    @Test
    void afterTheFinisherYouWait() {
        ComboRules.State s = onBeat();
        long end = s.start + ComboRules.length(ComboRules.FINISHER);
        assertEquals(ComboRules.Outcome.IGNORED, ComboRules.press(s, end + 5));
        assertEquals(ComboRules.Outcome.STARTED, ComboRules.press(s, end + ComboRules.FINISHER_WAIT + 1));
        assertEquals(ComboRules.HORIZONTAL, s.step);
    }

    @Test
    void tooSlowStartsAgain() {
        ComboRules.State s = new ComboRules.State();
        ComboRules.press(s, 100);
        long late = 100 + ComboRules.length(0) + ComboRules.WINDOW + 1;
        assertEquals(ComboRules.Outcome.STARTED, ComboRules.press(s, late));
        assertEquals(ComboRules.HORIZONTAL, s.step);
    }

    @Test
    void clickJustBeforeTheEndIsSavedAndFiresWhenTheMoveEnds() {
        ComboRules.State s = new ComboRules.State();
        ComboRules.press(s, 100);
        long end = 100 + ComboRules.length(0);
        assertEquals(ComboRules.Outcome.QUEUED, ComboRules.press(s, end - 1));
        assertEquals(-1, ComboRules.tick(s, end - 1));
        assertEquals(ComboRules.REVERSE, ComboRules.tick(s, end));
        assertEquals(end, s.start);
        assertEquals(-1, ComboRules.tick(s, end + 1));
    }

    @Test
    void spammingNeverReachesTheFinisher() {
        ComboRules.State s = new ComboRules.State();
        boolean finisher = false;
        for (long t = 0; t < 400; t++) {
            ComboRules.press(s, t);
            ComboRules.tick(s, t);
            finisher |= s.step == ComboRules.FINISHER;
        }
        assertFalse(finisher);
        assertTrue(s.step >= 0);
    }

    @Test
    void arcsAreSlicesOfACircle() {
        assertTrue(ComboRules.inArc(3, 0, 1, 0, 4, 60));
        assertFalse(ComboRules.inArc(5, 0, 1, 0, 4, 60));
        assertFalse(ComboRules.inArc(-3, 0, 1, 0, 4, 60));
        assertTrue(ComboRules.inArc(2, 2, 1, 0, 4, 60));
        assertFalse(ComboRules.inArc(1, 3, 1, 0, 4, 60));
        assertTrue(ComboRules.inArc(0.1, -0.1, 1, 0, 4, 10));
    }

    @Test
    void sprayFillsAndOnlyLavaHurts() {
        assertTrue(SprayRules.canFill(Liquid.EMPTY, 0, Liquid.WATER));
        assertTrue(SprayRules.canFill(Liquid.WATER, 3, Liquid.WATER));
        assertFalse(SprayRules.canFill(Liquid.WATER, 3, Liquid.LAVA));
        assertFalse(SprayRules.canFill(Liquid.WATER, Liquid.WATER.full, Liquid.WATER));
        assertTrue(SprayRules.canFill(Liquid.WATER, 0, Liquid.MILK));
        assertEquals(Liquid.MILK.perFill, SprayRules.afterFill(Liquid.WATER, 0, Liquid.MILK));
        assertEquals(Liquid.WATER.full, SprayRules.afterFill(Liquid.WATER, Liquid.WATER.full - 1, Liquid.WATER));
        assertFalse(SprayRules.hurts(Liquid.WATER));
        assertFalse(SprayRules.hurts(Liquid.MILK));
        assertTrue(SprayRules.hurts(Liquid.LAVA));
        assertTrue(Liquid.LAVA.full < Liquid.WATER.full);
        assertEquals(13, SprayRules.barWidth(Liquid.LAVA.full, Liquid.LAVA));
        assertEquals(0, SprayRules.barWidth(0, Liquid.LAVA));
        assertEquals(Liquid.MILK, Liquid.of("milk"));
    }

    @Test
    void dropChances() {
        assertTrue(DropRules.katana(true, 0.99));
        assertFalse(DropRules.katana(false, 0.3));
        assertTrue(DropRules.katana(false, 0.2));
        assertTrue(DropRules.iceStaff(0.3));
        assertFalse(DropRules.iceStaff(0.4));
        assertTrue(DropRules.dagger(0.05));
        assertFalse(DropRules.dagger(0.09));
    }

    @Test
    void solgoIsFasterUnseenAndChoosesFairly() {
        assertTrue(SolgoRules.speed(false, false) > SolgoRules.speed(false, true));
        assertTrue(SolgoRules.speed(true, true) > SolgoRules.speed(false, true));
        assertTrue(SolgoRules.lookingAt(1, 0, 0, 10, 0, 0));
        assertFalse(SolgoRules.lookingAt(-1, 0, 0, 10, 0, 0));
        assertEquals(SolgoRules.Move.WINDUP, SolgoRules.choose(6, false, true, 0, 0, 0, 100, 0, 0.9));
        assertEquals(SolgoRules.Move.VANISH, SolgoRules.choose(20, false, false, SolgoRules.UNSEEN_BEFORE_FLICKER, 0, 0, 0, 0, 0.9));
        assertNull(SolgoRules.choose(60, false, true, 0, 0, 0, 0, 0, 0.1));
        assertTrue(SolgoRules.windup(true) < SolgoRules.windup(false));
        assertTrue(SolgoRules.phaseTwo(50, 100));
        assertFalse(SolgoRules.phaseTwo(51, 100));
        assertTrue(SolgoRules.nightTime(18000));
        assertFalse(SolgoRules.nightTime(6000));
        assertTrue(SolgoRules.scaledHealth(100, 1, 2) > 100);
    }

    @Test
    void kingsRockIsARockNotASphere() {
        for (long seed = 1; seed < 12; seed++) {
            RockShape rock = new RockShape(seed);
            int solid = 0, gold = 0, moss = 0, minX = 99, maxX = -99, minZ = 99, maxZ = -99, maxY = 0;
            int r = rock.reach();
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    for (int y = 0; y <= rock.top(); y++) {
                        RockShape.Kind k = rock.kind(x, y, z);
                        if (k == RockShape.Kind.AIR) continue;
                        solid++;
                        if (k == RockShape.Kind.GOLD || k == RockShape.Kind.RAW_GOLD) gold++;
                        if (k == RockShape.Kind.MOSS) moss++;
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minZ = Math.min(minZ, z);
                        maxZ = Math.max(maxZ, z);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            int across = Math.max(maxX - minX, maxZ - minZ) + 1;
            assertTrue(across >= 12 && across <= 26, "seed " + seed + " is " + across + " across");
            assertTrue(maxY >= 9, "seed " + seed + " only " + maxY + " high");
            assertTrue(gold > 0, "no gold in seed " + seed);
            assertTrue(gold < solid * 0.12, "too much gold in seed " + seed);
            assertTrue(moss > 20, "not enough moss in seed " + seed);
            int[] peak = rock.peak();
            assertTrue(rock.solid(peak[0], peak[1], peak[2]));
            assertFalse(rock.solid(peak[0], peak[1] + 1, peak[2]));
            assertTrue(peak[1] >= 8, "sword spot too low on seed " + seed);
        }
    }

    @Test
    void trailCurvePassesThroughItsPoints() {
        assertEquals(2.0, TrailMath.catmullRom(1, 2, 3, 4, 0), 1e-9);
        assertEquals(3.0, TrailMath.catmullRom(1, 2, 3, 4, 1), 1e-9);
        assertEquals(1.0, TrailMath.fade(0, 10), 1e-9);
        assertEquals(0.0, TrailMath.fade(10, 10), 1e-9);
        assertTrue(TrailMath.fade(2, 10) > TrailMath.fade(6, 10));
    }
}
