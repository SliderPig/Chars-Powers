package com.example.charpowers.client.anim.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

class SolverTest {
    private static final double E = 1e-6;

    private static double[] handOf(double[] part, double along) {
        double[] p = Rot.euler(part[3], part[4], part[5]).apply(0, along, 0);
        return new double[] {part[0] + p[0], part[1] + p[1], part[2] + p[2]};
    }

    @Test
    void eulerComesBackTheSame() {
        double[][] tries = {{0.3, -0.7, 1.1}, {-2.2, 0.4, 0.2}, {1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {-1.57, 0.2, -0.3}};
        for (double[] t : tries) {
            Rot r = Rot.euler(t[0], t[1], t[2]);
            double[] e = r.toEuler();
            Rot again = Rot.euler(e[0], e[1], e[2]);
            for (double[] v : new double[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}) {
                assertArrayEquals(r.apply(v[0], v[1], v[2]), again.apply(v[0], v[1], v[2]), E);
            }
        }
    }

    @Test
    void aQuarterTurnOnYStillWorks() {
        Rot r = Rot.euler(0.5, Math.PI / 2, 0);
        double[] e = r.toEuler();
        Rot again = Rot.euler(e[0], e[1], e[2]);
        assertArrayEquals(r.apply(1, 2, 3), again.apply(1, 2, 3), 1e-4);
        Rot down = Rot.euler(0.5, -Math.PI / 2, 0);
        e = down.toEuler();
        assertArrayEquals(down.apply(1, 2, 3), Rot.euler(e[0], e[1], e[2]).apply(1, 2, 3), 1e-4);
    }

    @Test
    void turnsFollowTheModelsWay() {
        // an arm raised by -90 points forward, which is -z in the model
        assertArrayEquals(new double[] {0, 0, -1}, Rot.euler(-Math.PI / 2, 0, 0).apply(0, 1, 0), E);
        // and +y on the model swings forward to the player's right, which is -x
        assertArrayEquals(new double[] {-1, 0, 0}, Rot.y(Math.PI / 2).apply(0, 0, -1), E);
    }

    @Test
    void pointingAimsTheArmWithoutTwist() {
        double[][] directions = {{0, 0, -1}, {1, 1, -1}, {-2, 3, -1}, {0, 1, 0}, {0.1, 0.2, 3}, {1, 0, 0}};
        for (double[] d : directions) {
            double len = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            assertArrayEquals(new double[] {d[0] / len, d[1] / len, d[2] / len}, Rot.pointing(d[0], d[1], d[2]).apply(0, 1, 0), E);
        }
        // straight forward is the same as the plain raise: nothing rolled
        assertArrayEquals(Rot.euler(-Math.PI / 2, 0, 0).apply(1, 0, 0), Rot.pointing(0, 0, -1).apply(1, 0, 0), E);
    }

    @Test
    void quaternionMatchesTheTurn() {
        Rot r = Rot.euler(0.4, -1.2, 0.9);
        float[] q = r.toQuaternion();
        // v' = v + 2w(q x v) + 2 q x (q x v)
        double[] v = {0.3, -0.5, 0.8};
        double[] u = {q[0], q[1], q[2]};
        double[] c = cross(u, v), cc = cross(u, c);
        double[] rotated = {v[0] + 2 * q[3] * c[0] + 2 * cc[0], v[1] + 2 * q[3] * c[1] + 2 * cc[1], v[2] + 2 * q[3] * c[2] + 2 * cc[2]};
        assertArrayEquals(r.apply(v[0], v[1], v[2]), rotated, 1e-5);
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static PoseDef pose(String frames, String flags) {
        return PoseFile.read(new StringReader("{\"poses\":{\"p\":{" + flags + "\"frames\":[" + frames + "]}}}")).poses.get("p");
    }

    private static Body rest() {
        Body b = new Body();
        b.rest();
        return b;
    }

    @Test
    void keyframedArmGoesWhereItIsTold() {
        PoseDef p = pose("{\"t\":0,\"ra\":[-90,0,0]}", "\"ticks\":10,\"in\":0,\"out\":0,\"follow\":0,");
        Body b = rest();
        new Solver().apply(p, 5, 1, new Solver.View(), b, false, null, null);
        assertEquals(Math.toRadians(-90), b.part[Body.RIGHT_ARM][3], E);
        assertTrue(b.didChange(Body.RIGHT_ARM));
    }

    @Test
    void weightBlendsFromWhatTheBodyWasDoing() {
        PoseDef p = pose("{\"t\":0,\"ra\":[-90,0,0]}", "\"ticks\":10,\"in\":0,\"out\":0,\"follow\":0,");
        Body b = rest();
        b.part[Body.RIGHT_ARM][3] = Math.toRadians(-30);
        new Solver().apply(p, 5, 0.5, new Solver.View(), b, false, null, null);
        assertEquals(Math.toRadians(-60), b.part[Body.RIGHT_ARM][3], E);
        Body none = rest();
        new Solver().apply(p, 5, 0, new Solver.View(), none, false, null, null);
        assertEquals(0, none.changed);
    }

    @Test
    void leaningKeepsTheShouldersOnTheChest() {
        PoseDef p = pose("{\"t\":0,\"spine\":[30,0,0]}", "\"ticks\":10,\"in\":0,\"out\":0,\"follow\":0,");
        Body b = rest();
        new Solver().apply(p, 5, 1, new Solver.View(), b, false, null, null);
        // the neck moved forward and down round the hips, and the chest's bottom stays on the hips
        assertTrue(b.part[Body.CHEST][2] < -5, "neck forward");
        double[] chestBottom = handOf(b.part[Body.CHEST], 12);
        assertEquals(12, chestBottom[1], E);
        assertEquals(0, chestBottom[2], E);
        assertEquals(b.part[Body.CHEST][2] + 2 * Math.cos(Math.toRadians(30)) * Math.sin(Math.toRadians(30)) * 0, b.part[Body.HEAD][2], E);
        assertTrue(b.part[Body.RIGHT_ARM][2] < -4, "shoulder forward with it");
    }

    @Test
    void twistingMovesTheLeftShoulderForward() {
        PoseDef p = pose("{\"t\":0,\"spine\":[0,30,0]}", "\"ticks\":10,\"in\":0,\"out\":0,\"follow\":0,");
        Body b = rest();
        new Solver().apply(p, 5, 1, new Solver.View(), b, false, null, null);
        assertTrue(b.part[Body.LEFT_ARM][2] < 0, "left forward");
        assertTrue(b.part[Body.RIGHT_ARM][2] > 0, "right back");
    }

    @Test
    void lookingAroundCarriesTheUpperBodyWhenItFollows() {
        PoseDef p = pose("{\"t\":0,\"ra\":[-90,0,0]}", "\"ticks\":10,\"in\":0,\"out\":0,\"follow\":1,");
        Body b = rest();
        Solver.View v = new Solver.View();
        v.netYaw = Math.toRadians(40);
        v.pitch = Math.toRadians(-20);
        new Solver().apply(p, 5, 1, v, b, false, null, null);
        double[] part = b.part[Body.RIGHT_ARM];
        double[] pointing = Rot.euler(part[3], part[4], part[5]).apply(0, 1, 0);
        // the look direction: yaw 40 right, 20 up
        double[] look = Rot.y(v.netYaw).then(Rot.x(v.pitch)).apply(0, 0, -1);
        assertArrayEquals(look, pointing, E);
    }

    @Test
    void walkingFadesLegKeyframesOnly() {
        PoseDef p = pose("{\"t\":0,\"ra\":[-90,0,0],\"rl\":[-40,0,0],\"ll\":[40,0,0]}", "\"ticks\":10,\"in\":0,\"out\":0,\"follow\":0,");
        Body b = rest();
        Solver.View v = new Solver.View();
        v.walk = 1;
        b.part[Body.RIGHT_LEG][3] = 0.7;
        new Solver().apply(p, 5, 1, v, b, false, null, null);
        assertEquals(0.7, b.part[Body.RIGHT_LEG][3], E);
        assertEquals(Math.toRadians(-90), b.part[Body.RIGHT_ARM][3], E);
        assertEquals(0, b.lift[1], E, "feet are the game's while walking");
    }

    @Test
    void standingWithLegsApartDropsTheBodySoFeetStayDown() {
        PoseDef p = pose("{\"t\":0,\"rl\":[-30,0,0],\"ll\":[20,0,0]}", "\"ticks\":10,\"in\":0,\"out\":0,\"follow\":0,");
        Body b = rest();
        new Solver().apply(p, 5, 1, new Solver.View(), b, false, null, null);
        assertEquals(12 * (1 - Math.cos(Math.toRadians(30))), b.lift[1], E);
        assertTrue(b.rootChanged);
    }

    @Test
    void additivePosesAddToWhatsThere() {
        PoseDef p = pose("{\"t\":0,\"ra\":[0,0,10],\"spine\":[0,0,5]}", "\"ticks\":20,\"in\":0,\"out\":0,\"additive\":true,");
        Body b = rest();
        b.part[Body.RIGHT_ARM][3] = 0.5;
        new Solver().apply(p, 5, 1, new Solver.View(), b, false, null, null);
        assertEquals(0.5, b.part[Body.RIGHT_ARM][3], E);
        assertEquals(Math.toRadians(10), b.part[Body.RIGHT_ARM][5], E);
        assertEquals(Math.toRadians(5), b.part[Body.CHEST][5], E);
    }

    @Test
    void leftHandersGetItMirrored() {
        PoseDef p = pose("{\"t\":0,\"ra\":[-90,20,10],\"spine\":[0,15,0]}", "\"ticks\":10,\"in\":0,\"out\":0,\"follow\":0,");
        Body b = rest();
        new Solver().apply(p, 5, 1, new Solver.View(), b, true, null, null);
        Body straight = rest();
        new Solver().apply(p, 5, 1, new Solver.View(), straight, false, null, null);
        double[] mine = b.part[Body.LEFT_ARM], theirs = straight.part[Body.RIGHT_ARM];
        assertEquals(theirs[3], mine[3], E);
        assertEquals(-theirs[4], mine[4], E);
        assertEquals(-theirs[5], mine[5], E);
        assertEquals(-theirs[0], mine[0], E);
        assertEquals(theirs[2], mine[2], E);
    }

    // ---- the minigun

    private static final GunNumbers NUMBERS = GunNumbers.DEFAULTS;

    private static PoseDef holdPose() {
        return pose("{\"t\":0,\"spine\":[6,45,0]}", "\"ticks\":100000,\"in\":0,\"out\":0,");
    }

    private static void assertHandsOnGrips(double netYaw, double pitch, double armY) {
        GunPose gun = GunPose.of(NUMBERS, armY, netYaw, pitch, GunMotion.STILL);
        Body b = rest();
        Solver.View v = new Solver.View();
        v.netYaw = netYaw;
        v.pitch = pitch;
        v.armY = armY;
        new Solver().apply(holdPose(), 5, 1, v, b, false, gun, NUMBERS);
        for (int which : new int[] {Body.RIGHT_ARM, Body.LEFT_ARM}) {
            double[] part = b.part[which];
            double[] hand = handOf(part, Solver.PALM);
            double[] grip = gun.point(which == Body.RIGHT_ARM ? NUMBERS.gripRight : NUMBERS.gripLeft);
            double miss = Math.sqrt(Math.pow(hand[0] - grip[0], 2) + Math.pow(hand[1] - grip[1], 2) + Math.pow(hand[2] - grip[2], 2));
            // the shoulder may only give so much: the arm must still end within a pixel or two of the grip
            assertTrue(miss < 2.5, (which == Body.RIGHT_ARM ? "right" : "left") + " hand misses by " + miss + " at yaw " + netYaw + " pitch " + pitch);
            // whatever the numbers, the arm points right at the grip
            double[] toGrip = {grip[0] - part[0], grip[1] - part[1], grip[2] - part[2]};
            double len = Math.sqrt(toGrip[0] * toGrip[0] + toGrip[1] * toGrip[1] + toGrip[2] * toGrip[2]);
            double[] along = Rot.euler(part[3], part[4], part[5]).apply(0, 1, 0);
            assertArrayEquals(new double[] {toGrip[0] / len, toGrip[1] / len, toGrip[2] / len}, along, 1e-5);
        }
    }

    @Test
    void handsStayOnTheGunHoweverYouLook() {
        for (double pitch : new double[] {-60, -30, 0, 25, 50}) {
            for (double yaw : new double[] {-50, 0, 50}) assertHandsOnGrips(Math.toRadians(yaw), Math.toRadians(pitch), 2);
        }
    }

    @Test
    void handsStayOnTheGunCrouching() {
        assertHandsOnGrips(0, 0, 5);
    }

    @Test
    void theGunStopsTippingPastItsLimits() {
        GunPose flat = GunPose.of(NUMBERS, 2, 0, Math.toRadians(NUMBERS.pitchDown), GunMotion.STILL);
        GunPose past = GunPose.of(NUMBERS, 2, 0, Math.toRadians(89), GunMotion.STILL);
        assertArrayEquals(flat.position, past.position, E);
    }

    @Test
    void theBarrelPointsWhereYouLook() {
        double yaw = Math.toRadians(30), pitch = Math.toRadians(-20);
        GunPose gun = GunPose.of(NUMBERS, 2, yaw, pitch, GunMotion.STILL);
        double[] back = gun.point(new double[] {4, 11, 0}), front = gun.point(new double[] {4, 11, 30});
        double[] way = {front[0] - back[0], front[1] - back[1], front[2] - back[2]};
        double len = Math.sqrt(way[0] * way[0] + way[1] * way[1] + way[2] * way[2]);
        assertArrayEquals(Rot.y(yaw).then(Rot.x(pitch)).apply(0, 0, -1), new double[] {way[0] / len, way[1] / len, way[2] / len}, E);
        assertEquals(30 * gun.size, len, E);
    }

    @Test
    void gunMovementIsSmallAndFiringShakesMore() {
        double still = 0, firing = 0;
        for (double t = 0; t < 400; t += 0.37) {
            GunMotion a = GunMotion.at(t, 0, 0, 0), b = GunMotion.at(t, 1, 0, 0);
            still = Math.max(still, Math.abs(a.dz));
            firing = Math.max(firing, Math.abs(b.dz));
            assertTrue(Math.abs(a.dy) < 0.5 && Math.abs(a.dx) < 0.5);
            assertTrue(Math.abs(b.dy) < 1 && Math.abs(b.dz) < 2, "a shake, not a jump");
        }
        assertTrue(firing > still * 3);
    }
}
