package com.example.charpowers.cloning.model;

import java.util.function.DoubleSupplier;

// Cloning's numbers from the powers list, with no game code in them so they can be unit tested.
public final class CloneRules {
    public static final int SLOTS = 5;
    public static final int AWAKENED_CAP = 5;
    public static final double AWAKENED_DAMAGE = 3.0;
    public static final int LEVEL_TWO_CLONES = 20;
    public static final int LEVEL_DOG_PILE = 60;
    public static final int LEVEL_POWER_CLONE = 80;
    public static final int LEVEL_MAX = 100;
    public static final long DAY_TICKS = 24_000L;
    public static final int NICK_MAX = 12;

    private CloneRules() {}

    public static int cap(int mastery, boolean awakened) {
        return awakened ? AWAKENED_CAP : cap(mastery);
    }

    public static int toSummon(int mastery, boolean awakened, int alreadyOut) {
        return awakened ? Math.max(0, AWAKENED_CAP - alreadyOut) : toSummon(mastery, alreadyOut);
    }

    public static double maxHealth(int mastery, boolean awakened) {
        return awakened ? maxHealth(mastery) * 1.5 : maxHealth(mastery);
    }

    // How many clones you can have out at once.
    public static int cap(int mastery) {
        if (mastery >= LEVEL_DOG_PILE) return 3;
        return mastery >= LEVEL_TWO_CLONES ? 2 : 1;
    }

    // How many one press of Clone tries to summon, given how many are out already.
    public static int toSummon(int mastery, int alreadyOut) {
        int free = Math.max(0, cap(mastery) - alreadyOut);
        return mastery >= LEVEL_DOG_PILE ? free : Math.min(1, free);
    }

    // What's left of a typed nickname: no colour codes or control characters, and short. Empty = no nickname.
    public static String cleanNickname(String typed) {
        StringBuilder kept = new StringBuilder();
        for (char ch : typed.toCharArray()) {
            if (ch >= 32 && ch != 127 && ch != '\u00a7') kept.append(ch);
        }
        String name = kept.toString().trim();
        return name.length() > NICK_MAX ? name.substring(0, NICK_MAX).trim() : name;
    }

    // How long a dead clone's slot stays locked.
    public static long lockTicks(int mastery) {
        return mastery >= LEVEL_MAX ? DAY_TICKS / 2 : DAY_TICKS;
    }

    public static double rogueChance(int mastery) {
        return mastery >= LEVEL_MAX ? 0.05 : 0.20;
    }

    // The one rogue check a clone gets when it gains a power.
    public static boolean rollsRogue(int mastery, DoubleSupplier random) {
        return random.getAsDouble() < rogueChance(mastery);
    }

    // A little more health every level: 20 HP at level 1, 39.8 at level 100.
    public static double maxHealth(int mastery) {
        return 20.0 + 0.2 * (Math.max(1, mastery) - 1);
    }
}
