package com.example.charpowers.hidden.model;

import java.util.random.RandomGenerator;

// Small rules about mobs, kept here so they can be tested without Minecraft.
public final class MobRoll {
    // A mob rolls for a power once in its life. Loading it again (chunk reload, restart) keeps what it had.
    public static boolean shouldRoll(boolean alreadyRolled) {
        return !alreadyRolled;
    }

    // Ticks until a mob next uses one of its funny powers: somewhere between min and max seconds.
    public static int nextRandomUse(RandomGenerator r, int minSec, int maxSec) {
        int min = minSec * 20;
        int max = Math.max(min, maxSec * 20);
        return min + (max > min ? r.nextInt(max - min + 1) : 0);
    }

    private MobRoll() {}
}
