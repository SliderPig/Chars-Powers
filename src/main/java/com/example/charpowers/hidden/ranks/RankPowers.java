package com.example.charpowers.hidden.ranks;

// Every hidden power above F: E (tiny bit useful) ... Z (beyond legendary). Called once when the mod starts.
public final class RankPowers {
    public static void registerAll() {
        EPowers.registerAll();
        DPowers.registerAll();
        CPowers.registerAll();
        BPowers.registerAll();
        APowers.registerAll();
        SPowers.registerAll();
        com.example.charpowers.hidden.attack.AttackPowers.registerAll();
    }

    private RankPowers() {}
}
