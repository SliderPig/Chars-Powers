package com.example.charpowers.game.model;

// The rules of Raise Shadow, with no game code. A creature killed in a gate leaves a shadow body for a few seconds;
// pressing the key near it stands it up on your side until you leave the gate.
public final class ShadowRules {
    public static final int UNLOCK_LEVEL = 25, WINDOW_TICKS = 200, REACH = 10, BOSS_COOLDOWN_TICKS = 6000, MAX_BODIES = 5;
    public static final double HEALTH_SCALE = 0.8;

    public enum Result { OK, LOCKED, NO_BODY, TOO_MANY, BOSS_WAIT }

    private ShadowRules() {}

    // 3 at the unlock, one more every 25 mastery levels.
    public static int maxShadows(int level) {
        if (level < UNLOCK_LEVEL) return 0;
        return Math.min(6, 3 + (level - UNLOCK_LEVEL) / 25);
    }

    // Ordinary creatures always stand up. A boss only sometimes: more often at high mastery, never always.
    public static double chance(boolean boss, int level) {
        if (!boss) return 1.0;
        return Math.min(0.5, 0.12 + level * 0.004);
    }

    // Only a creature killed inside a gate run you are in, never a player, never someone's minion or pet.
    public static boolean fair(boolean inGateRun, boolean victimIsPlayer, boolean victimIsMinion) {
        return inGateRun && !victimIsPlayer && !victimIsMinion;
    }

    // Can the key raise something right now? bodiesInReach counts bodies still fresh and near enough.
    public static Result check(int level, int bodiesInReach, int alreadyRaised, boolean nearestIsBoss, long now, long bossReadyAt) {
        if (level < UNLOCK_LEVEL) return Result.LOCKED;
        if (bodiesInReach <= 0) return Result.NO_BODY;
        if (alreadyRaised >= maxShadows(level)) return Result.TOO_MANY;
        if (nearestIsBoss && now < bossReadyAt) return Result.BOSS_WAIT;
        return Result.OK;
    }

    public static boolean fresh(long now, long expiresAt) {
        return now <= expiresAt;
    }

    // Shadows keep fighting while you are inside the gate and nowhere else.
    public static boolean mayStay(boolean ownerOnline, boolean ownerInGateRun, boolean sameWorld) {
        return ownerOnline && ownerInGateRun && sameWorld;
    }
}
