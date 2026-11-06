package com.example.charpowers.items.model;

// Who drops what. Roll is a random number from 0 to 1.
public final class DropRules {
    public static final double FROST_KING_STAFF = 0.35;
    public static final double DARK_ELF_DAGGER = 0.07;
    public static final double SOLGO_KATANA_AFTER_FIRST = 0.25;

    private DropRules() {}

    public static boolean iceStaff(double roll) {
        return roll < FROST_KING_STAFF;
    }

    public static boolean dagger(double roll) {
        return roll < DARK_ELF_DAGGER;
    }

    // The first time a player kills him they always get it. After that it's one in four.
    public static boolean katana(boolean firstKill, double roll) {
        return firstKill || roll < SOLGO_KATANA_AFTER_FIRST;
    }
}
