package com.example.charpowers.client.anim.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

class KeyframesTest {
    private static final double E = 1e-9;

    @Test
    void everyEaseStartsAtZeroAndEndsAtOne() {
        for (Ease ease : Ease.values()) {
            assertEquals(0, ease.at(0), 1e-6, ease.name());
            assertEquals(1, ease.at(1), 1e-6, ease.name());
            assertEquals(0, ease.at(-3), 1e-6, ease.name() + " before the start stays put");
            assertEquals(1, ease.at(4), 1e-6, ease.name() + " after the end stays put");
        }
    }

    @Test
    void easesHaveTheirShapes() {
        assertEquals(0.5, Ease.LINEAR.at(0.5), E);
        assertEquals(0.5, Ease.SINE.at(0.5), E);
        assertTrue(Ease.IN.at(0.5) < 0.5, "starts slow");
        assertTrue(Ease.OUT.at(0.5) > 0.5, "starts fast");
        assertTrue(Ease.IN3.at(0.5) < Ease.IN.at(0.5), "later and harder");
        assertEquals(0, Ease.HOLD.at(0.99), E);
        double most = 0;
        for (double t = 0; t <= 1; t += 0.01) most = Math.max(most, Ease.BACK.at(t));
        assertTrue(most > 1.05, "follow-through goes past the mark");
        double least = 1;
        for (double t = 0; t <= 1; t += 0.01) least = Math.min(least, Ease.ANTICIPATE.at(t));
        assertTrue(least < -0.05, "pulls back first");
        assertTrue(Ease.SETTLE.at(0.8) < 1 && Ease.SETTLE.at(0.6) > 0.99, "lands, then bounces");
    }

    @Test
    void unknownEaseNamesFallBackToGentle() {
        assertEquals(Ease.BACK, Ease.named("back"));
        assertEquals(Ease.SINE, Ease.named("wobbly"));
        assertEquals(Ease.SINE, Ease.named(null));
    }

    private static Track track() {
        return new Track(new double[] {2, 6, 10},
                new double[][] {{0, 10, 20}, {4, 10, 0}, {8, -10, 0}},
                new Ease[] {Ease.LINEAR, Ease.IN, Ease.LINEAR});
    }

    @Test
    void aTrackInterpolatesAndHoldsAtTheEnds() {
        double[] out = new double[3];
        Track t = track();
        t.sample(0, out);
        assertEquals(0, out[0], E);
        assertEquals(20, out[2], E);
        t.sample(4, out);
        assertEquals(2, out[0], E);
        assertEquals(10, out[1], E);
        assertEquals(10, out[2], E);
        t.sample(6, out);
        assertEquals(4, out[0], E);
        t.sample(8, out);
        // IN at halfway is a quarter of the way
        assertEquals(5, out[0], E);
        assertEquals(5, out[1], E);
        t.sample(50, out);
        assertEquals(8, out[0], E);
        assertEquals(-10, out[1], E);
    }

    @Test
    void keyframesMustGoForward() {
        assertThrows(IllegalArgumentException.class, () -> new Track(new double[] {3, 3}, new double[][] {{0, 0, 0}, {1, 1, 1}}, new Ease[] {Ease.SINE, Ease.SINE}));
        assertThrows(IllegalArgumentException.class, () -> new Track(new double[0], new double[0][], new Ease[0]));
    }

    private static PoseDef pose(double length, double in, double out, double loopFrom, double loopTo) {
        return new PoseDef("x", length, in, out, loopFrom, loopTo, 1, true, false, false, false, true, Map.of());
    }

    @Test
    void aPoseFadesInAndOut() {
        PoseDef p = pose(20, 4, 10, 0, 0);
        assertEquals(0, p.weight(0, -1), E);
        assertEquals(0.5, p.weight(2, -1), E);
        assertEquals(1, p.weight(4, -1), E);
        assertEquals(1, p.weight(20, -1), E);
        assertEquals(0.5, p.weight(25, -1), E);
        assertEquals(0, p.weight(30, -1), E);
        assertEquals(0, p.weight(-1, -1), E);
        assertFalse(p.finished(29, -1));
        assertTrue(p.finished(30, -1));
    }

    @Test
    void stoppingEarlyFadesFromThatMoment() {
        PoseDef p = pose(100, 4, 10, 0, 0);
        assertEquals(1, p.weight(10, 10), E);
        assertEquals(0.5, p.weight(15, 10), E);
        assertTrue(p.finished(20, 10));
        // a stop after the natural end changes nothing
        assertTrue(p.finished(110, 500));
    }

    @Test
    void aFadeOnAPoseStillFadingInNeverJumpsUp() {
        PoseDef p = pose(100, 10, 10, 0, 0);
        assertTrue(p.weight(5, 2) <= p.weight(2, 2) + E, "stopped at tick 2, it can only go down from there");
    }

    @Test
    void loopsGoRound() {
        PoseDef p = pose(100, 0, 0, 10, 20);
        assertEquals(5, p.keyTime(5), E);
        assertEquals(20, p.keyTime(20), E);
        assertEquals(11, p.keyTime(21), E);
        assertEquals(15, p.keyTime(35), E);
        PoseDef noLoop = pose(100, 0, 0, 0, 0);
        assertEquals(70, noLoop.keyTime(70), E);
    }

    @Test
    void thePoseFileReads() {
        PoseFile f = PoseFile.read(new StringReader("{\"poses\":{\"Wave\":{\"ticks\":10,\"in\":1,\"out\":2,\"firstPerson\":false,\"loop\":[2,8],\"frames\":["
                + "{\"t\":0,\"ease\":\"LINEAR\",\"ra\":[-90,0,0]},{\"t\":4,\"spine\":[10,0,0]},{\"t\":8,\"ra\":[-180,0,0],\"spine\":[0,0,0],\"lift\":[0,2,0]}]}}}"));
        PoseDef wave = f.poses.get("wave");
        assertEquals(10, wave.length, E);
        assertEquals(1, wave.blendIn, E);
        assertEquals(2, wave.blendOut, E);
        assertFalse(wave.firstPerson);
        assertFalse(f.hasGun);
        double[] out = new double[3];
        // a part left out of a keyframe carries on to the next one that names it
        wave.tracks.get(PoseDef.Part.RIGHT_ARM).sample(4, out);
        assertEquals(Math.toRadians(-135), out[0], 1e-9);
        wave.tracks.get(PoseDef.Part.LIFT).sample(8, out);
        assertEquals(2, out[1], E, "lift is pixels, not degrees");
    }

    @Test
    void theRealPoseFileHasEveryPoseWeUse() throws Exception {
        try (var in = KeyframesTest.class.getResourceAsStream("/assets/charpowers/player_poses.json")) {
            PoseFile f = PoseFile.read(new InputStreamReader(in, StandardCharsets.UTF_8));
            assertTrue(f.hasGun);
            for (String name : new String[] {"minigun_hold", "ring_thrust", "ring_raise", "hand_slam", "lantern_slap", "sword_sweep_left", "sword_sweep_right", "sword_overhead", "hands_out",
                    "lantern_grab", "lantern_throw", "suit_walk", "suit_call", "suit_stomp", "command_point", "whistle", "raise_dead", "clock_hand", "shout", "ground_slam", "reach_pull", "temple",
                    "reading", "hologram_swipe", "split", "sweep_swing", "raised_finger", "hammer_tap"}) {
                assertTrue(f.poses.containsKey(name), name);
                assertTrue(f.poses.get(name).length > 0, name);
            }
            for (PoseDef p : f.poses.values()) {
                // no arm may go round the back of the head and tie itself in a knot
                double[] out = new double[3];
                for (PoseDef.Part part : new PoseDef.Part[] {PoseDef.Part.RIGHT_ARM, PoseDef.Part.LEFT_ARM}) {
                    Track t = p.tracks.get(part);
                    if (t == null) continue;
                    for (double tick = 0; tick < p.length; tick++) {
                        t.sample(tick, out);
                        assertTrue(out[0] > Math.toRadians(-200) && out[0] < Math.toRadians(60), p.name + " at " + tick);
                    }
                }
            }
        }
    }
}
