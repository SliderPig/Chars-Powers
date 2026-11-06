package com.example.charpowers.hidden.model;

// Is this hit a real close-up punch/swing by the attacker? Not a power's own hit, not an arrow or trident (the
// direct hitter is the projectile), not thorns (a THORNS hit, not a player/mob attack).
// Used by One Punch, Explosive Fist, Counter, Double Tap and the boost phase.
public final class MeleeRule {
    public static boolean isPunch(boolean powerHit, boolean directIsAttacker, boolean meleeDamageType) {
        return !powerHit && directIsAttacker && meleeDamageType;
    }

    private MeleeRule() {}
}
