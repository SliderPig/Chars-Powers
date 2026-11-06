package com.example.charpowers.futuresight.model;

// The words and numbers behind Future Sight, with no game code in them.
public final class SightRules {
    public static final int ARROWS_NEEDED = 1000, RAIDS_NEEDED = 10;
    public static final int RANK_LEVEL = 50, GLOW_LEVEL = 75, COUNT_LEVEL = 100;
    public static final long HINT_GAP_TICKS = 200, WARN_GAP_TICKS = 100;

    private SightRules() {}

    public static int oreRange(int level) {
        return 8 + clamp(level) * 24 / 99;
    }

    public static double warnSeconds(int level) {
        return 1.0 + clamp(level) * 2.0 / 99.0;
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(99, level - 1));
    }

    // "below and to the left". yaw is the player's facing in degrees: 0 = south, 90 = west.
    public static String direction(double dx, double dy, double dz, float yaw) {
        double rad = Math.toRadians(yaw), sin = Math.sin(rad), cos = Math.cos(rad);
        double forward = -dx * sin + dz * cos, right = -dx * cos - dz * sin;
        String up = dy <= -2 ? "below" : dy >= 2 ? "above" : "";
        String side = "";
        if (Math.max(Math.abs(forward), Math.abs(right)) > 1.5) {
            if (Math.abs(forward) >= Math.abs(right)) side = forward > 0 ? "ahead" : "behind you";
            else side = right > 0 ? "to the right" : "to the left";
        }
        if (up.isEmpty()) return side.isEmpty() ? "right here" : side;
        return side.isEmpty() ? up : up + " and " + side;
    }

    // diamond_ore -> diamonds, deepslate_iron_ore -> iron, ancient_debris -> ancient debris
    public static String oreName(String blockPath) {
        String n = com.example.charpowers.cloning.model.OreRules.family(blockPath).replace('_', ' ');
        return n.equals("diamond") || n.equals("emerald") ? n + "s" : n;
    }

    public static String oreHint(String name, String direction, int count, int level) {
        String how = level >= COUNT_LEVEL ? count + " " + name : name;
        return how + "... " + direction;
    }

    // How long until a mob walking at this speed is close enough to hurt you.
    public static double secondsAway(double distance, double blocksPerSecond, double reach) {
        if (distance <= reach) return 0;
        return (distance - reach) / Math.max(0.5, blocksPerSecond);
    }

    public static String threat(String mob, String direction, double seconds) {
        if (seconds < 1) return "a " + mob + " " + direction + "!";
        return "a " + mob + " " + direction + " in " + (int) Math.ceil(seconds) + "...";
    }
}
