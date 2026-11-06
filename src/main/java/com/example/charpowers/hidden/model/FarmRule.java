package com.example.charpowers.hidden.model;

import java.util.random.RandomGenerator;

// A mob that won its roll for a hidden power, but seems to be in a farm, usually loses it again.
public final class FarmRule {
    // The mob already won at 1 in normalOneIn. This makes the whole chance 1 in farmOneIn. 0 means never.
    public static boolean keeps(int normalOneIn, int farmOneIn, RandomGenerator r) {
        if (farmOneIn <= 0) return false;
        return r.nextInt(farmOneIn) < normalOneIn;
    }

    private FarmRule() {}
}
