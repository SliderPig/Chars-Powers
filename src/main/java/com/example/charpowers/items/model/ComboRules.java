package com.example.charpowers.items.model;

// The Golden King's Long Sword: which move comes next, decided from when you click. No game code in here.
//
// The chain is horizontal slash, reverse slash, overhead slam, finisher. Click near the end of a move or just after it to carry on. Too slow and
// the chain starts again. Click too early (spamming) and the click does nothing, and that chain won't reach the finisher: it only comes if you kept
// the beat. After the finisher there is a short wait.
public final class ComboRules {
    public static final int HORIZONTAL = 0, REVERSE = 1, OVERHEAD = 2, FINISHER = 3;
    private static final int[] LENGTH = {10, 10, 14, 16};
    private static final int[] HIT_AT = {4, 4, 8, 6};
    private static final float[] DAMAGE = {0.8f, 0.85f, 1.2f, 1.7f};
    // this close to the end of a move a click is saved and the next move starts as soon as this one ends
    public static final int BEAT_BEFORE = 4;
    // how long after a move ends the chain stays open
    public static final int WINDOW = 14;
    public static final int FINISHER_WAIT = 50;

    public enum Outcome {IGNORED, QUEUED, STARTED}

    public static final class State {
        public int step = -1;
        public long start;
        public boolean rushed, queued;
        public long waitUntil;
    }

    private ComboRules() {}

    public static int length(int step) {
        return LENGTH[step];
    }

    public static int hitAt(int step) {
        return HIT_AT[step];
    }

    public static float damage(int step) {
        return DAMAGE[step];
    }

    // A click. STARTED means s.step is now the new move, begun at this tick.
    public static Outcome press(State s, long now) {
        if (now < s.waitUntil) return Outcome.IGNORED;
        if (s.step >= 0) {
            long end = s.start + LENGTH[s.step];
            if (now < end - BEAT_BEFORE) {
                s.rushed = true;
                return Outcome.IGNORED;
            }
            if (now < end) {
                s.queued = true;
                return Outcome.QUEUED;
            }
        }
        begin(s, next(s, now), now);
        return Outcome.STARTED;
    }

    // Every tick. Returns the step that has just started from a saved click, or -1.
    public static int tick(State s, long now) {
        if (!s.queued || s.step < 0 || now < s.start + LENGTH[s.step]) return -1;
        s.queued = false;
        begin(s, next(s, now), now);
        return s.step;
    }

    private static int next(State s, long now) {
        if (s.step < 0 || now > s.start + LENGTH[s.step] + WINDOW) return HORIZONTAL;
        return switch (s.step) {
            case HORIZONTAL -> REVERSE;
            case REVERSE -> OVERHEAD;
            case OVERHEAD -> s.rushed ? HORIZONTAL : FINISHER;
            default -> HORIZONTAL;
        };
    }

    private static void begin(State s, int step, long now) {
        if (step == HORIZONTAL) s.rushed = false;
        s.step = step;
        s.start = now;
        s.queued = false;
        if (step == FINISHER) s.waitUntil = now + LENGTH[FINISHER] + FINISHER_WAIT;
    }

    // Is something at (dx, dz) from you within this slice of a circle round where you look? (lookX, lookZ) needn't be normal.
    public static boolean inArc(double dx, double dz, double lookX, double lookZ, double range, double halfAngleDegrees) {
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > range) return false;
        if (dist < 0.5) return true;
        double len = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (len < 1e-6) return true;
        double cos = (dx * lookX + dz * lookZ) / (dist * len);
        return cos >= Math.cos(Math.toRadians(halfAngleDegrees));
    }
}
