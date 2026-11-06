package com.example.charpowers.hidden.fx;

// The extras a hit picks up from the attacker's always-on boost powers (Iron Fist, Execute, Berserker...).
// Flat extras are added first, then the multipliers (model/HitMath.boosted). They work on normal hits AND on every
// hit a key power lands (user: "if they go through the effort of dying to a mob with them they deserve it").
public final class Boost {
    public final long press;         // the tick the attacker's current key press started (a plain hit: now)
    public final boolean fromPower;  // true when a key power landed this hit
    public float flatHearts;
    public float multiplier = 1f;

    public Boost(long press, boolean fromPower) {
        this.press = press;
        this.fromPower = fromPower;
    }

    public void add(float hearts) { flatHearts += hearts; }

    public void times(float m) { multiplier *= m; }
}
