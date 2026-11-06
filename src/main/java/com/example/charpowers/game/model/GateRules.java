package com.example.charpowers.game.model;

import java.util.List;

// How gates work, with no game code in them: ranks E to S, how big each dungeon is, what's in it and what it pays.
public final class GateRules {
    public static final String[] RANKS = {"E", "D", "C", "B", "A", "S"};
    private static final int[] UNLOCK_LEVEL = {1, 15, 30, 50, 70, 90};
    private static final List<List<String>> MOBS = List.of(
            List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider"),
            List.of("minecraft:husk", "minecraft:stray", "minecraft:cave_spider", "minecraft:zombie"),
            List.of("minecraft:vindicator", "minecraft:pillager", "minecraft:witch", "minecraft:blaze"),
            List.of("minecraft:wither_skeleton", "minecraft:piglin_brute", "minecraft:vindicator", "minecraft:blaze"),
            List.of("minecraft:wither_skeleton", "minecraft:piglin_brute", "minecraft:evoker", "minecraft:ravager"),
            List.of("minecraft:piglin_brute", "minecraft:evoker", "minecraft:ravager", "minecraft:wither_skeleton"));

    private GateRules() {}

    public static int clampRank(int rank) {
        return Math.max(0, Math.min(RANKS.length - 1, rank));
    }

    // The highest rank you can open yourself at this mastery.
    public static int topRank(int level) {
        int top = 0;
        for (int i = 0; i < UNLOCK_LEVEL.length; i++) {
            if (level >= UNLOCK_LEVEL[i]) top = i;
        }
        return top;
    }

    public static int rooms(int rank) {
        return 2 + clampRank(rank);
    }

    public static int mobsInRoom(int rank, int room) {
        return 3 + clampRank(rank) + Math.max(0, room);
    }

    public static List<String> mobs(int rank) {
        return MOBS.get(clampRank(rank));
    }

    public static double mobHealth(int rank) {
        return 1 + clampRank(rank) * 0.5;
    }

    public static double mobDamage(int rank) {
        return 1 + clampRank(rank) * 0.25;
    }

    public static double bossHealth(int rank) {
        return 80 + clampRank(rank) * 90;
    }

    public static double bossDamage(int rank) {
        return 6 + clampRank(rank) * 2.5;
    }

    public static int points(int rank) {
        return 15 + clampRank(rank) * 25;
    }

    private static final int[] COINS = {6, 14, 28, 48, 78, 120};

    // Coins a cleared gate pays: grows faster than the rank, because the shop is priced in clears (see ShopPrices).
    public static int coins(int rank) {
        return COINS[clampRank(rank)];
    }

    // Percent chance a cleared gate leaves one of the Player's own weapons.
    public static int weaponChance(int rank) {
        return 5 + clampRank(rank) * 15;
    }

    // Which way something is from you, as a compass word. +z is south in this game.
    public static String compass(double dx, double dz) {
        String[] words = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        return words[Math.floorMod((int) Math.round(angle / 45), 8)];
    }
}
