package com.example.charpowers.lantern.model;

import java.util.Locale;

// The numbers behind Green Lantern (from its design), with no game code in them.
public final class LanternRules {
    public static final double HAND_COST = 5, SWORD_COST = 10, GUN_COST = 10, GUN_PER_SECOND = 5, TURRET_COST = 20, CARRY_PER_SECOND = 1, BUBBLE_PER_SECOND = 1;
    public static final int FIST_HIT = 12, FIST_LIFE = 28, SLAP_HIT = 20, SLAP_LIFE = 31, SLAM_HIT = 17, SLAM_LIFE = 28, GRAB_HIT = 9, THROW_RELEASE = 9, THROW_LIFE = 21;
    public static final int SWEEP_HIT = 4, OVERHEAD_HIT = 8, SWING_GAP = 12, DAGGER_EVERY = 4, SPIN_UP = 10, NUKE_FUSE = 60, NUKE_RADIUS = 100, NUKE_PER_TICK = 16;
    public static final int TURRET_RANGE = 16, TRIAL_WALK = 30, LOOK_RANGE = 32, GUN_RANGE = 48;
    public static final float FIST_DAMAGE = 12, SLAP_DAMAGE = 14, SLAM_DAMAGE = 14, THROW_DAMAGE = 8, SWORD_DAMAGE = 8, DAGGER_DAMAGE = 7, GUN_TICK_DAMAGE = 0.6f, TURRET_TICK_DAMAGE = 0.7f, NUKE_DAMAGE = 80;
    public static final float TRIAL_HEALTH = 10;
    public static final int MIDDLE_FINGER_ONE_IN = 10;
    public static final float SINESTRO_HEALTH = 400, SINESTRO_DAMAGE = 9;
    public static final int SINESTRO_GAP = 70, RAIN_SWORDS = 24;
    public static final double AWAKENED_COST = 30;
    public static final String DEFAULT_OATH = "In biggest day, on maccas night, no big mac shall escape my sight.";

    private LanternRules() {}

    private static int steps(int level) {
        return Math.max(0, Math.min(99, level - 1));
    }

    // Every level: the battery gets bigger (100 at level 1, 200 at level 100) and constructs hit a bit harder.
    public static double maxBattery(int level) {
        return 100 + steps(level) * 100.0 / 99;
    }

    public static float damage(float base, int level) {
        return base * (1 + steps(level) * 0.5f / 99);
    }

    // 10: hands hit a bigger area.
    public static double handRadius(int level) {
        return level >= 10 ? 4 : 3;
    }

    // 25: launch up to 15 swords.
    public static int maxDaggers(int level) {
        return level >= 25 ? 15 : 10;
    }

    public static double daggerCost(int count) {
        return 10 + count;
    }

    // 50: the bubble is up to 30 blocks wide. Given as the radius.
    public static double bubbleRadius(int level) {
        return level >= 50 ? 15 : 10;
    }

    // 75: two turrets. Awakened: one more.
    public static int maxTurrets(int level, boolean awakened) {
        return (level >= 75 ? 2 : 1) + (awakened ? 1 : 0);
    }

    // 100: the nuke costs 30.
    public static double nukeCost(int level) {
        return level >= 100 ? 30 : 50;
    }

    public static float nukeDamage(double distance) {
        return (float) (NUKE_DAMAGE * Math.max(0.05, 1 - distance / NUKE_RADIUS));
    }

    // Ignoring capitals and punctuation, so the oath doesn't have to be typed perfectly.
    public static boolean oathSaid(String said, String oath) {
        return plain(said).equals(plain(oath));
    }

    private static String plain(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").trim().replaceAll(" +", " ");
    }

    // Where the Nth of the swords floats: fanned out behind you. Sideways and up, in blocks.
    public static double[] fan(int index, int count) {
        double spread = count <= 1 ? 0 : (index / (double) (count - 1) - 0.5) * 2;
        return new double[] {spread * 2.2, 1.6 + (1 - spread * spread) * 0.9};
    }
}
