package com.example.charpowers.hidden.model;

// Damage maths for the attack powers. Powers are written in hearts (1 heart = 2 health).
public final class HitMath {
    // A power's own damage, times the attackDamageScale setting.
    public static float powerDamage(float hearts, double scale) {
        return (float) (hearts * 2 * scale);
    }

    // Boosts on a hit: flat extras first (in hearts), then the multipliers (user: no cap).
    public static float boosted(float amount, float flatHearts, float multiplier) {
        return (amount + flatHearts * 2) * multiplier;
    }

    private HitMath() {}
}
