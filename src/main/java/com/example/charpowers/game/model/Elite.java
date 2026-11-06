package com.example.charpowers.game.model;

import java.util.Random;

// An elite creature: a glowing name and one extra edge. Higher ranks meet more of them.
public enum Elite {
    SWIFT("Swift"), ARMOURED("Armoured"), BERSERK("Berserk"), REGENERATING("Regenerating");

    public final String title;

    Elite(String title) {
        this.title = title;
    }

    // Percent chance that a creature in a gate of this rank is elite.
    public static int chance(int rank) {
        return 6 + 3 * GateRules.clampRank(rank);
    }

    // The elite kind this roll gives, or null for an ordinary creature.
    public static Elite roll(int rank, Random r) {
        return r.nextInt(100) < chance(rank) ? values()[r.nextInt(values().length)] : null;
    }
}
