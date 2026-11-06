package com.example.charpowers.timecontrol.model;

// The numbers behind Time Control, with no game code in them.
public final class TimeRules {
    public static final long END_TICKS_NEEDED = 100L * 24000L;
    public static final int SLOW_RADIUS = 32, SLOW_TICKS = 600;
    public static final double SLOW_AMOUNT = -0.7;
    public static final int HURRY_RADIUS = 40, HURRY_TICKS = 6000, HURRY_MAX_BLOCKS = 64, HURRY_EXTRA = 4, SPAWNER_EXTRA = 1;
    public static final int BLOCK_COOLDOWN = 12000, BLOCK_SIZE = 10;
    public static final int SPEED_MIN = 100, SPEED_STEP = 50;
    public static final int BARRAGE_HITS = 10, MOMENT_RADIUS = 16, MOMENT_TICKS = 100;
    public static final float MOMENT_DAMAGE = 20f;

    private TimeRules() {}

    // Gotta move's top speed: +100% at level 1, up to +1000% at level 100.
    public static int topSpeed(int level) {
        return SPEED_MIN + Math.max(0, Math.min(99, level - 1)) * 900 / 99;
    }

    public static int nudgeSpeed(int speed, boolean up, int level) {
        int next = speed + (up ? SPEED_STEP : -SPEED_STEP);
        return Math.max(SPEED_MIN, Math.min(topSpeed(level), next));
    }

    // Let time flow: 1 chunk at the start, 2 at 75, 3 at 100.
    public static int chunkLimit(int level) {
        return level >= 100 ? 3 : level >= 75 ? 2 : 1;
    }
}
