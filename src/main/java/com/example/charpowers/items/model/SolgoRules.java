package com.example.charpowers.items.model;

// Solgo's rules with no game code in them. He gets faster when you aren't looking, freezes for a moment when you first look at him,
// and every move he makes has a clear tell.
public final class SolgoRules {
    public enum Move {IDLE, WALK, WINDUP, SLASH, CROUCH, LUNGE, VANISH, ARRIVE, SCREAM, STARED, RUN, DIE}

    public static final double SLASH_RANGE = 9.5, LUNGE_MIN = 9, LUNGE_MAX = 24, SCREAM_RANGE = 14;
    public static final int STARE_FREEZE = 22, STARE_COOLDOWN = 140;
    public static final int SLASH_COOLDOWN = 34, LUNGE_COOLDOWN = 130, SCREAM_COOLDOWN = 360, FLICKER_COOLDOWN = 240;
    public static final int UNSEEN_BEFORE_FLICKER = 70;

    private SolgoRules() {}

    // ticks for each stage of a move: phase two is quicker
    public static int windup(boolean phaseTwo) {
        return phaseTwo ? 13 : 18;
    }

    public static int crouch(boolean phaseTwo) {
        return phaseTwo ? 11 : 16;
    }

    public static boolean phaseTwo(float health, float max) {
        return health <= max * 0.5f;
    }

    // Blocks a tick.
    public static double speed(boolean phaseTwo, boolean looked) {
        double base = phaseTwo ? 0.36 : 0.17;
        return looked ? base * 0.62 : base * 1.5;
    }

    // Is someone looking at him? view = where they look, to = from their eyes to him. Neither needs to be normal.
    public static boolean lookingAt(double vx, double vy, double vz, double tx, double ty, double tz) {
        double lv = Math.sqrt(vx * vx + vy * vy + vz * vz), lt = Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (lv < 1e-6 || lt < 1e-6) return false;
        // he's tall, so anywhere near the crosshair counts; closer in you see more of him
        double wide = lt < 12 ? 0.72 : 0.86;
        return (vx * tx + vy * ty + vz * tz) / (lv * lt) > wide;
    }

    // What he does next when he's free to choose. Cooldowns are ticks left. Pick is a random number from 0 to 1. Null means just keep walking.
    public static Move choose(double dist, boolean phaseTwo, boolean looked, int unseenTicks, int slashCd, int lungeCd, int screamCd, int flickerCd, double pick) {
        if (!looked && unseenTicks >= UNSEEN_BEFORE_FLICKER && flickerCd <= 0 && dist > 6 && dist < 48) return Move.VANISH;
        if (dist <= SLASH_RANGE && slashCd <= 0) {
            if (screamCd <= 0 && pick < (phaseTwo ? 0.3 : 0.15)) return Move.SCREAM;
            return Move.WINDUP;
        }
        if (dist >= LUNGE_MIN && dist <= LUNGE_MAX && lungeCd <= 0 && pick < 0.4) return Move.CROUCH;
        if (dist <= SCREAM_RANGE && screamCd <= 0 && pick < 0.05) return Move.SCREAM;
        return null;
    }

    public static float slashDamage(boolean phaseTwo) {
        return phaseTwo ? 15f : 13f;
    }

    public static float lungeDamage(boolean phaseTwo) {
        return phaseTwo ? 18f : 16f;
    }

    // Inside a gate he follows the same idea as the other bosses: every player past the first adds a third, each rank above A a quarter.
    public static double scaledHealth(double base, int rankAboveA, int party) {
        return base * (1 + 0.25 * Math.max(0, rankAboveA)) * (1 + 0.34 * Math.max(0, party - 1));
    }

    // Natural spawns: one check every 30 seconds at night in his woods.
    public static final int CHECK_TICKS = 600;
    public static final double CHECK_CHANCE = 0.02;

    public static boolean nightTime(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= 13000 && t <= 23000;
    }
}
