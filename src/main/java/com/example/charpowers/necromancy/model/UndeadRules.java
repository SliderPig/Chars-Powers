package com.example.charpowers.necromancy.model;

// The numbers behind Necromancy, with no game code in them.
public final class UndeadRules {
    public static final int ZOMBIES_NEEDED = 100;
    public static final int ZOMBIES_A_SUMMON = 5, SKELETONS_A_SUMMON = 3, MAX_SUMMONS = 30;
    public static final int GEAR_LEVEL = 10, FAST_ARROWS_LEVEL = 25, TWO_REVIVALS_LEVEL = 50, SUN_LEVEL = 75, LONG_RIOT_LEVEL = 100;
    public static final int RIOT_SIZE = 50, RIOT_RADIUS = 32, RIOT_SPAWN_EVERY = 4, EXPLODE_PER_TICK = 8, REVIVE_COOLDOWN = 2400;
    public static final long REVIVAL_WINDOW = 600;
    public static final double REVIVAL_CHANCE = 0.25;
    public static final float EXPLOSION = 2.5f;

    private UndeadRules() {}

    private static int steps(int level) {
        return Math.max(0, Math.min(99, level - 1));
    }

    // Weak to start with, a little stronger every level.
    public static double health(int level) {
        return 10 + steps(level) * 0.2;
    }

    public static double damageScale(int level) {
        return 0.5 + steps(level) * 0.015;
    }

    // Stronger at night or in the dark, very weak in sunlight. At mastery 75 the sun only weakens them a little.
    public static double lightScale(boolean sun, boolean dark, int level) {
        if (sun) return level >= SUN_LEVEL ? 0.75 : 0.25;
        return dark ? 1.5 : 1.0;
    }

    public static int revivalsAtOnce(int level) {
        return level >= TWO_REVIVALS_LEVEL ? 2 : 1;
    }

    public static int riotTicks(int level) {
        return (level >= LONG_RIOT_LEVEL ? 4 : 2) * 1200;
    }

    // Room for these many more ordinary summons. The riot's 50 don't count, they're gone in a couple of minutes.
    public static int room(int have, int wanted) {
        return Math.max(0, Math.min(wanted, MAX_SUMMONS - have));
    }
}
