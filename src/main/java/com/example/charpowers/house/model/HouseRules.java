package com.example.charpowers.house.model;

import com.example.charpowers.mastery.model.MasteryMath;

// The numbers behind House Builder, with no game code in them so they can be unit tested.
public final class HouseRules {
    public static final int UNLOCK_BLOCKS = 10_000;
    public static final int MASTERY_BLOCKS = 100_000;
    public static final int COOLDOWN_TICKS = 12_000; // 10 minutes, one for the whole power
    public static final int CASTLE_BLOCKS = 3_200; // 50 stacks
    public static final int REMEMBER_FROM_LEVEL = 90; // placed blocks are remembered for the castle from here on
    public static final int SCAN_CAP = 40_000;
    public static final int MAX_CUT = 8; // terrain this much above the floor gets cut away, anything higher is a hill
    public static final int MAX_DROP = 12; // foundations go down this far, anything lower is a cliff
    public static final int REACH = 96;
    public static final double MAX_WET_SHARE = 0.15;
    public static final int MAX_PER_TICK = 150; // a huge file from the config folder just takes longer, nobody's server stutters for it

    private HouseRules() {}

    // Mastery points for the block number `placedBefore + 1` since you got the power. They add up to exactly
    // MasteryMath.POINTS_TO_MAX at MASTERY_BLOCKS, whatever the rounding on the way.
    public static long masteryGain(long placedBefore) {
        return pointsAfter(placedBefore + 1) - pointsAfter(placedBefore);
    }

    private static long pointsAfter(long placed) {
        return Math.min(placed, MASTERY_BLOCKS) * MasteryMath.POINTS_TO_MAX / MASTERY_BLOCKS;
    }

    // How long a house takes: 10 to 30 s, bigger ones longer.
    public static int secondsFor(int blocks) {
        return Math.max(10, Math.min(30, blocks / 200));
    }

    public static int perTick(int blocks, double speedPercent) {
        int perTick = (int) Math.ceil(blocks / (secondsFor(blocks) * 20.0) * speedPercent / 100.0);
        return Math.max(1, Math.min(MAX_PER_TICK, perTick));
    }

    public static String clock(long ticks) {
        long seconds = Math.max(0, (ticks + 19) / 20);
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    // A house you may build now? Locked ones say what they're waiting for: null = open.
    public static String lockReason(int needLevel, boolean needsAwakening, int level, boolean awakened) {
        if (needsAwakening && !awakened) return "Awakening";
        if (level < needLevel) return "Mastery " + needLevel;
        return null;
    }
}
