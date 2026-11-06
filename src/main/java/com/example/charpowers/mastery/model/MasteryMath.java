package com.example.charpowers.mastery.model;

// The sums behind mastery, with no game code in them so they can be unit tested.
// 1 point = 1 second of use. Level 100 takes about 100 hours, and each level costs a bit more than the one before.
public final class MasteryMath {
    public static final int MAX_LEVEL = 100;
    public static final long POINTS_TO_MAX = 360_000L;
    // cost of a level is proportional to the level: 1 + 2 + ... + 99 = 4950 parts in total
    private static final long PARTS = 4950L;

    private MasteryMath() {}

    // Total points needed to stand on this level (level 1 = 0 points).
    public static long pointsFor(int level) {
        int l = Math.max(1, Math.min(MAX_LEVEL, level));
        return POINTS_TO_MAX * ((long) (l - 1) * l / 2) / PARTS;
    }

    public static int levelOf(long points) {
        int level = 1;
        while (level < MAX_LEVEL && points >= pointsFor(level + 1)) level++;
        return level;
    }
}
