package com.example.charpowers.cat.model;

// How your cat behaves, with no game code in it. It trails you, sits when you stop, naps if you stay put a long while,
// purrs now and then and hops when you level up. It never does anything else.
public final class CatRules {
    public static final double FOLLOW_FROM = 4.0, HURRY_FROM = 9.0, TELEPORT_FROM = 16.0, SETTLE_WITHIN = 3.5;
    public static final int SIT_AFTER_STILL = 80, NAP_AFTER_SITTING = 400, PURR_MIN = 300, PURR_SPREAD = 600, MIN_BETWEEN_HOPS = 6;

    public enum Pose { STAND, SIT, NAP }

    private CatRules() {}

    // Owner still for a few seconds and the cat is beside them: sit. Further along: maybe a nap.
    public static Pose pose(double distance, int ownerStillTicks, int sittingTicks, double napRoll, Pose now) {
        if (distance > SETTLE_WITHIN + 1.5 || ownerStillTicks < SIT_AFTER_STILL) return Pose.STAND;
        if (now == Pose.NAP) return Pose.NAP;
        if (sittingTicks >= NAP_AFTER_SITTING && napRoll < 0.02) return Pose.NAP;
        return Pose.SIT;
    }

    // 0 = stay put, 1 = walk, 2 = hurry, 3 = jump over to you.
    public static int travel(double distance) {
        if (distance > TELEPORT_FROM) return 3;
        if (distance > HURRY_FROM) return 2;
        if (distance > FOLLOW_FROM) return 1;
        return 0;
    }

    // Ticks until the next purr: 15 to 45 seconds.
    public static int nextPurr(double roll) {
        return PURR_MIN + (int) (Math.max(0, Math.min(1, roll)) * PURR_SPREAD);
    }

    // The cat is wanted only when you chose it and own one of Char's powers.
    public static boolean wanted(boolean switchedOn, boolean ownsAPower, boolean alive, boolean spectator) {
        return switchedOn && ownsAPower && alive && !spectator;
    }

    public static boolean ownerStill(double speedSqr) {
        return speedSqr < 0.0025;
    }
}
