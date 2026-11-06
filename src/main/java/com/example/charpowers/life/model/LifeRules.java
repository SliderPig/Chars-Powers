package com.example.charpowers.life.model;

// The numbers behind Life Enjoyer, with no game code in them.
public final class LifeRules {
    public static final int HEAL_DOUBLE_LEVEL = 50, GRASS_LANDING_LEVEL = 100;
    public static final int MAGNET_RANGE = 10, MAGNET_RANGE_AWAKE = 16, STRANGER_DROP_WAIT = 100, OWN_DROP_WAIT = 60;
    public static final int SORT_RANGE = 8, SORT_RANGE_AWAKE = 16, SORT_MAX_CHESTS = 64;
    public static final int VEIN_MAX = 64, VEIN_MAX_AWAKE = 256, TREE_MAX = 128, TREE_PER_TICK = 16, TREE_LEAVES_NEEDED = 4;
    public static final int FACING_SLACK_DEGREES = 60;
    public static final long SUN_GOES_DOWN = 11800, SUN_IS_GONE = 13000, LAST_SIT_FOR_SUNSET = 12300;
    public static final long SUNRISE_FROM = 23000, SUNRISE_UNTIL = 300, NOON = 6000, QUEST_SUNSET = 12500;
    public static final float EAST_YAW = -90f, WEST_YAW = 90f;

    private LifeRules() {}

    private static int steps(int level) {
        return Math.max(0, Math.min(99, level - 1));
    }

    // Crops grow faster in a bigger area: 4 blocks at level 1, 16 at level 100.
    public static int cropRadius(int level) {
        return 4 + steps(level) * 12 / 99;
    }

    // How many random spots to tick a second. Vanilla gives each block about 1.5% a second, this adds about 0.85%.
    public static int cropTicksPerSecond(int radius, int height, boolean awakened) {
        long volume = (2L * radius + 1) * (2L * radius + 1) * height;
        int n = (int) Math.max(4, volume * 85 / 10000);
        return awakened ? n * 2 : n;
    }

    public static int restedTicks(int level, boolean awakened) {
        int minutes = 5 + steps(level) * 15 / 99;
        return (awakened ? minutes + minutes / 2 : minutes) * 1200;
    }

    public static float petHeal(int level, boolean awakened) {
        float heal = level >= HEAL_DOUBLE_LEVEL ? 4f : 2f;
        return awakened ? heal * 2 : heal;
    }

    public static boolean facing(float yaw, float wanted) {
        float diff = ((yaw - wanted) % 360 + 540) % 360 - 180;
        return Math.abs(diff) <= FACING_SLACK_DEGREES;
    }

    // Sitting down in time to watch the whole sunset.
    public static boolean sunsetCanStart(long dayTime) {
        return dayTime >= SUN_GOES_DOWN - 800 && dayTime <= LAST_SIT_FOR_SUNSET;
    }

    public static boolean sunIsGone(long dayTime) {
        return dayTime >= SUN_IS_GONE;
    }

    public static boolean isSunrise(long dayTime) {
        return dayTime >= SUNRISE_FROM || dayTime <= SUNRISE_UNTIL;
    }

    // The quest: the morning faces east, the afternoon faces west.
    public static float questFacing(long dayTime) {
        return dayTime >= SUNRISE_FROM || dayTime < NOON ? EAST_YAW : WEST_YAW;
    }
}
