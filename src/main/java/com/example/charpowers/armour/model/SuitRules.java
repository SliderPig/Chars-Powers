package com.example.charpowers.armour.model;

import java.util.List;

// The numbers behind Invincible Armour, with no game code in them.
public final class SuitRules {
    public static final double FULL = 100, DAMAGE_LEFT = 0.1, NUKE_DAMAGE = 100, MODULE_SPEED = 0.3, MODULE_PUNCH = 2.0;
    public static final int NUKE_ONE_IN = 1000, NUKE_WARNING_TICKS = 100, NUKE_RADIUS = 24, SHIELD_TICKS = 600, CHARGE_EVERY_TICKS = 60, TICK_EVERY_SECONDS = 60;
    public static final int SLIM_COST = 4;
    public static final double SLIM_RUN_SLOWDOWN = -0.15;
    public static final int CALL_RANGE = 64, CALL_ANYWHERE_LEVEL = 100, LIFT_REACH = 4, SCANNER_RANGE = 24;
    public static final List<String> WHISPERS = List.of("take it off", "it's warmer in here", "they can't hear you", "you're still in here, right?",
            "we should keep walking", "don't charge it", "who's wearing who", "listen");

    public enum Stage { FINE, FLICKER, WHISPERS, FAKE_MOBS, OWN_MIND }

    private SuitRules() {}

    private static int steps(int level) {
        return Math.max(0, Math.min(99, level - 1));
    }

    // Battery drains a bit slower with mastery: 1% a minute down to 0.5%.
    public static double drainPerMinute(int level) {
        return 1.0 - steps(level) * 0.5 / 99;
    }

    // 1 module at the start, 2 at 25, 3 at 50, 4 at 75.
    public static int moduleSlots(int level) {
        return level >= 75 ? 4 : level >= 50 ? 3 : level >= 25 ? 2 : 1;
    }

    // Going crazy: hours worn since the last charge, and you go crazy a bit slower with mastery.
    public static Stage stage(long wornSeconds, int level) {
        double hours = wornSeconds / 3600.0 / (1 + steps(level) / 99.0);
        if (hours >= 4) return Stage.OWN_MIND;
        if (hours >= 3) return Stage.FAKE_MOBS;
        if (hours >= 2) return Stage.WHISPERS;
        return hours >= 1 ? Stage.FLICKER : Stage.FINE;
    }

    public static boolean atLeast(Stage now, Stage wanted) {
        return now.ordinal() >= wanted.ordinal();
    }
}
