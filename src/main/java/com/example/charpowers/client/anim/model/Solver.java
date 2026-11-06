package com.example.charpowers.client.anim.model;

import java.util.Map;

// Puts a pose on a body. Pure sums, no game code. The chest, head and shoulders all hang off a spine that turns at the hips, so when
// you lean or twist the arms go along with the chest and stay attached. Arms keep to their keyframes unless a gun gives them
// somewhere to be: then each one points at its grip, and the shoulder gives a pixel or two so the palm lands on it.
public final class Solver {
    public static final double PALM = 9;      // from the shoulder pivot to the middle of the hand, in model pixels
    public static final double SHRUG = 3;     // how far a shoulder may give to make the reach

    // What the body is doing besides the pose.
    public static final class View {
        public double netYaw;   // radians: where you look against where your legs point
        public double pitch;    // radians: looking down is positive
        public double armY = 2; // how far down the shoulders are: 2 standing, 5 crouching
        public double walk;     // 0 standing still, 1 walking or running: own leg keyframes fade out
        public double shakeLean, shakeTwist; // radians: the kick of a gun going off, for whichever pose is holding it
        public boolean firstPerson; // the arms on a gun come from below the view, not from real shoulders
    }

    private final double[] sample = new double[3];

    // weight 0 changes nothing, 1 is all pose. mirror is for the left-handed.
    public void apply(PoseDef def, double running, double weight, View view, Body body, boolean mirror, GunPose gun, GunNumbers numbers) {
        if (weight <= 0) return;
        double key = def.keyTime(running);
        Map<PoseDef.Part, Track> tracks = def.tracks;
        double lean = 0, twist = 0, side = 0;
        Track spine = tracks.get(PoseDef.Part.SPINE);
        if (spine != null) {
            spine.sample(key, sample);
            lean = sample[0];
            twist = mirror ? -sample[1] : sample[1];
            side = mirror ? -sample[2] : sample[2];
        }
        if (gun != null) {
            lean += view.shakeLean;
            twist += view.shakeTwist;
        }
        if (def.additive) {
            additive(def, key, weight, body, mirror, lean, twist, side, view);
        } else {
            replace(def, key, weight, view, body, mirror, lean, twist, side, gun, numbers);
        }
        root(def, key, weight, body, mirror, view);
    }

    private void replace(PoseDef def, double key, double weight, View view, Body body, boolean mirror, double lean, double twist, double side, GunPose gun, GunNumbers numbers) {
        Rot spine = Rot.y(twist + def.follow * view.netYaw).then(Rot.x(lean)).then(Rot.z(side));
        double hipY = body.part[Body.RIGHT_LEG][1], hipZ = body.part[Body.RIGHT_LEG][2];
        // the chest
        double[] chest = body.part[Body.CHEST];
        double[] at = off(spine, chest[0], chest[1], chest[2], hipY, hipZ);
        double[] turn = spine.then(Rot.x(chest[3])).toEuler();
        blend(body, Body.CHEST, weight, at[0], at[1], at[2], turn[0], turn[1], turn[2]);
        // the head goes with the chest but keeps looking where you look
        double[] head = body.part[Body.HEAD];
        at = off(spine, head[0], head[1], head[2], hipY, hipZ);
        double hx = 0, hy = 0, hz = 0;
        Track headTrack = def.tracks.get(PoseDef.Part.HEAD);
        if (headTrack != null) {
            headTrack.sample(key, sample);
            hx = sample[0];
            hy = mirror ? -sample[1] : sample[1];
            hz = mirror ? -sample[2] : sample[2];
        }
        blend(body, Body.HEAD, weight, at[0], at[1], at[2], head[3] + hx, head[4] + hy, head[5] + hz);
        // the arms
        arm(def, key, weight, view, body, mirror, spine, hipY, hipZ, Body.RIGHT_ARM, -5, gun, numbers == null ? null : numbers.gripRight, numbers == null ? null : numbers.firstPersonRight);
        arm(def, key, weight, view, body, mirror, spine, hipY, hipZ, Body.LEFT_ARM, 5, gun, numbers == null ? null : numbers.gripLeft, numbers == null ? null : numbers.firstPersonLeft);
        // the legs, unless you're walking
        double legs = weight * (1 - view.walk);
        leg(def, key, legs, body, mirror, Body.RIGHT_LEG, PoseDef.Part.RIGHT_LEG, PoseDef.Part.LEFT_LEG);
        leg(def, key, legs, body, mirror, Body.LEFT_LEG, PoseDef.Part.LEFT_LEG, PoseDef.Part.RIGHT_LEG);
    }

    private void arm(PoseDef def, double key, double weight, View view, Body body, boolean mirror, Rot spine, double hipY, double hipZ, int which, double shoulderX, GunPose gun, double[] grip, double[] from) {
        // in first person a lean only turns the arm: moving the shoulder too would bring its top end into view
        double[] at = view.firstPerson ? new double[] {shoulderX, view.armY, 0} : off(spine, shoulderX, view.armY, 0, hipY, hipZ);
        if (gun != null && grip != null) {
            double[] hand = gun.point(grip);
            if (view.firstPerson) {
                double dx = hand[0] - from[0], dy = hand[1] - from[1], dz = hand[2] - from[2];
                double along = PALM / Math.sqrt(dx * dx + dy * dy + dz * dz);
                double[] turn = Rot.pointing(dx, dy, dz).toEuler();
                blend(body, which, weight, hand[0] - dx * along, hand[1] - dy * along, hand[2] - dz * along, turn[0], turn[1], turn[2]);
                return;
            }
            double dx = hand[0] - at[0], dy = hand[1] - at[1], dz = hand[2] - at[2];
            double reach = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double give = reach < 1e-6 ? 0 : Math.max(-SHRUG, Math.min(SHRUG, reach - PALM)) / reach;
            double[] turn = Rot.pointing(dx, dy, dz).toEuler();
            blend(body, which, weight, at[0] + dx * give, at[1] + dy * give, at[2] + dz * give, turn[0], turn[1], turn[2]);
            return;
        }
        boolean left = which == Body.LEFT_ARM;
        Track track = def.tracks.get(left != mirror ? PoseDef.Part.LEFT_ARM : PoseDef.Part.RIGHT_ARM);
        double[] now = body.part[which];
        double[] turn;
        if (track != null) {
            track.sample(key, sample);
            Rot keyed = Rot.euler(sample[0], mirror ? -sample[1] : sample[1], mirror ? -sample[2] : sample[2]);
            turn = spine.then(Rot.x(def.follow * view.pitch)).then(keyed).toEuler();
        } else {
            turn = spine.then(Rot.euler(now[3], now[4], now[5])).toEuler();
        }
        blend(body, which, weight, at[0], at[1], at[2], turn[0], turn[1], turn[2]);
    }

    private void leg(PoseDef def, double key, double weight, Body body, boolean mirror, int which, PoseDef.Part own, PoseDef.Part other) {
        if (weight <= 0) return;
        Track track = def.tracks.get(mirror ? other : own);
        if (track == null) return;
        track.sample(key, sample);
        double[] p = body.part[which];
        blend(body, which, weight, p[0], p[1], p[2], sample[0], mirror ? -sample[1] : sample[1], mirror ? -sample[2] : sample[2]);
    }

    // Added on top of what the body already does: the suit's heavy walk.
    private void additive(PoseDef def, double key, double weight, Body body, boolean mirror, double lean, double twist, double side, View view) {
        Rot spine = Rot.y(twist * weight).then(Rot.x(lean * weight)).then(Rot.z(side * weight));
        double hipY = body.part[Body.RIGHT_LEG][1], hipZ = body.part[Body.RIGHT_LEG][2];
        for (int which : new int[] {Body.CHEST, Body.HEAD, Body.RIGHT_ARM, Body.LEFT_ARM}) {
            double[] p = body.part[which];
            double[] at = off(spine, p[0], p[1], p[2], hipY, hipZ);
            p[0] = at[0];
            p[1] = at[1];
            p[2] = at[2];
            body.mark(which);
        }
        double[] c = body.part[Body.CHEST];
        c[3] += lean * weight;
        c[4] += twist * weight;
        c[5] += side * weight;
        add(def, PoseDef.Part.HEAD, key, weight, body, Body.HEAD, mirror);
        add(def, mirror ? PoseDef.Part.LEFT_ARM : PoseDef.Part.RIGHT_ARM, key, weight, body, Body.RIGHT_ARM, mirror);
        add(def, mirror ? PoseDef.Part.RIGHT_ARM : PoseDef.Part.LEFT_ARM, key, weight, body, Body.LEFT_ARM, mirror);
        add(def, mirror ? PoseDef.Part.LEFT_LEG : PoseDef.Part.RIGHT_LEG, key, weight, body, Body.RIGHT_LEG, mirror);
        add(def, mirror ? PoseDef.Part.RIGHT_LEG : PoseDef.Part.LEFT_LEG, key, weight, body, Body.LEFT_LEG, mirror);
    }

    private void add(PoseDef def, PoseDef.Part part, double key, double weight, Body body, int which, boolean mirror) {
        Track track = def.tracks.get(part);
        if (track == null) return;
        track.sample(key, sample);
        double[] p = body.part[which];
        p[3] += sample[0] * weight;
        p[4] += (mirror ? -sample[1] : sample[1]) * weight;
        p[5] += (mirror ? -sample[2] : sample[2]) * weight;
        body.mark(which);
    }

    private void root(PoseDef def, double key, double weight, Body body, boolean mirror, View view) {
        Track tip = def.tracks.get(PoseDef.Part.ROOT), lift = def.tracks.get(PoseDef.Part.LIFT);
        double drop = 0;
        if (!def.additive) {
            // straight legs apart would lift you off the ground, so the body comes down by what that costs
            drop = plant(def.tracks.get(PoseDef.Part.RIGHT_LEG), key);
            drop = Math.max(drop, plant(def.tracks.get(PoseDef.Part.LEFT_LEG), key)) * (1 - view.walk);
        }
        if (tip == null && lift == null && drop == 0) return;
        body.rootChanged = true;
        if (tip != null) {
            tip.sample(key, sample);
            body.tip[0] += sample[0] * weight;
            body.tip[1] += (mirror ? -sample[1] : sample[1]) * weight;
            body.tip[2] += (mirror ? -sample[2] : sample[2]) * weight;
        }
        if (lift != null) {
            lift.sample(key, sample);
            body.lift[0] += (mirror ? -sample[0] : sample[0]) * weight;
            body.lift[1] += sample[1] * weight;
            body.lift[2] += sample[2] * weight;
        }
        body.lift[1] += drop * weight;
    }

    private double plant(Track leg, double key) {
        if (leg == null) return 0;
        leg.sample(key, sample);
        double angle = Math.sqrt(sample[0] * sample[0] + sample[2] * sample[2]);
        return 12 * (1 - Math.cos(angle));
    }

    private static double[] off(Rot spine, double x, double y, double z, double hipY, double hipZ) {
        double[] p = spine.apply(x, y - hipY, z - hipZ);
        p[1] += hipY;
        p[2] += hipZ;
        return p;
    }

    private static void blend(Body body, int which, double weight, double x, double y, double z, double xr, double yr, double zr) {
        double[] p = body.part[which];
        p[0] += (x - p[0]) * weight;
        p[1] += (y - p[1]) * weight;
        p[2] += (z - p[2]) * weight;
        p[3] = angle(p[3], xr, weight);
        p[4] = angle(p[4], yr, weight);
        p[5] = angle(p[5], zr, weight);
        body.mark(which);
    }

    // The short way round.
    static double angle(double from, double to, double weight) {
        double d = (to - from) % (Math.PI * 2);
        if (d > Math.PI) d -= Math.PI * 2;
        if (d < -Math.PI) d += Math.PI * 2;
        return from + d * weight;
    }
}
