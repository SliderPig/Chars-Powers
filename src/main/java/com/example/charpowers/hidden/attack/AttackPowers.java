package com.example.charpowers.hidden.attack;

// The attack hidden powers (#302-381), F to S. Called once when the mod starts, after the other ranks.
public final class AttackPowers {
    public static void registerAll() {
        AttackF.registerAll();
        AttackE.registerAll();
        AttackD.registerAll();
        AttackC.registerAll();
        AttackB.registerAll();
        AttackA.registerAll();
        AttackS.registerAll();
    }

    // Quitting a world: forget anything still floating (Ground Slam's hunk).
    public static void clearAll() { AttackS.clear(); }

    private AttackPowers() {}
}
