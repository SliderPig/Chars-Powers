package com.example.charpowers.game.model;

// The numbers behind Game Player, with no game code in them.
public final class GameRules {
    public static final int ROOM_CHANCE = 1000, QUEST_MASTERY = 300;
    public static final String[] STATS = {"Strength", "Speed", "Health", "Defence", "Luck"};
    // what one point in each stat gives: damage, part of your speed, health, armour, luck. No limit, it just costs more and more.
    private static final double[] PER_POINT = {0.25, 0.005, 0.5, 0.25, 0.1};
    private static final String[] WORDS = {"+%.2f damage", "+%.1f%% speed", "+%.1f health", "+%.2f armour", "+%.1f luck"};

    private GameRules() {}

    public static int statCost(int owned) {
        return 5 + owned;
    }

    public static double statEffect(int stat, int points) {
        return PER_POINT[stat] * points;
    }

    public static String statWords(int stat, int points) {
        double value = statEffect(stat, points);
        return String.format(WORDS[stat], stat == 1 ? value * 100 : value);
    }

    public static int dailies(int level) {
        return level >= 50 ? 5 : level >= 25 ? 4 : 3;
    }

    public static boolean secondChain(int level) {
        return level >= 75;
    }

    public static int slots(int level) {
        return level >= 100 ? 4 : 3;
    }

    // Every level, quests give a few more points: double by level 100.
    public static int questPoints(int base, int level) {
        return base + (int) Math.round(base * (Math.max(1, Math.min(level, 100)) - 1) / 99.0);
    }
}
