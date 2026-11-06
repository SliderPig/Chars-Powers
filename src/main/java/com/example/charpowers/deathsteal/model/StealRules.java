package com.example.charpowers.deathsteal.model;

import java.util.Set;

// The rules behind Death Steal, with no game code in them.
public final class StealRules {
    public static final int DAILY_ONE_IN = 1000, ROW = 10, MAX_OPTIONS = 30, TIME_LIMIT_TICKS = 6000, POINTS_A_STEAL = 3600;
    public static final int ONE_ICON_LEVEL = 25, TWO_ICONS_LEVEL = 75, TWICE_LEVEL = 90, ALL_ICONS_LEVEL = 100;
    private static final int SPLIT_SECOND = 4, ONE_EVERY = 100, MOMENT = 20, TWO_EVERY = 80;
    private static final Set<String> NEVER = Set.of("charpowers:death_steal", "charpowers:teacher", "charpowers:hidden_powers");

    private StealRules() {}

    // Only once per person or mob. Twice from mastery 90.
    public static boolean canStealFrom(int alreadyTaken, int level) {
        return alreadyTaken < (level >= TWICE_LEVEL ? 2 : 1);
    }

    // Not worth moving: this power itself, the joke one, the holder for hidden powers (they go one by one), and base
    // powers like the Heroes one that a whole other mod hangs off.
    public static boolean stealable(String powerId) {
        return !NEVER.contains(powerId) && !powerId.endsWith(":base_power");
    }

    // Does this icon turn to face you right now? They never do at first. At 25 one flashes for a split second, at 75 two
    // for a moment, at 100 they all do.
    public static boolean faces(int level, int index, int count, long tick) {
        if (level >= ALL_ICONS_LEVEL) return true;
        if (level >= TWO_ICONS_LEVEL) {
            if (tick % TWO_EVERY >= MOMENT) return false;
            long turn = tick / TWO_EVERY;
            return index == turn % count || index == (turn + 1) % count;
        }
        if (level >= ONE_ICON_LEVEL) {
            if (tick % ONE_EVERY >= SPLIT_SECOND) return false;
            return index == (tick / ONE_EVERY) % count;
        }
        return false;
    }

    // Where a podium stands: rows of up to 10 in front of you, 3 blocks apart. Sideways, then how far ahead.
    public static int[] spot(int index, int count) {
        int row = index / ROW, inRow = Math.min(ROW, count - row * ROW);
        int place = index % ROW;
        return new int[] {place * 3 - (inRow - 1) * 3 / 2, 5 + row * 4};
    }
}
