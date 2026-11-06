package com.example.charpowers.cloning.model;

// Which of two pieces of gear a clone would rather have. Enchantments count, but never beat a whole tier of material.
public final class GearRank {
    private GearRank() {}

    public static int armor(int defense, float toughness, int enchantLevels) {
        return defense * 100 + Math.round(toughness * 10) + enchantLevels;
    }

    public static int weapon(float damage, int enchantLevels) {
        return Math.round(damage * 100) + enchantLevels;
    }

    public static int tool(int tierLevel, int enchantLevels) {
        return tierLevel * 100 + enchantLevels;
    }
}
