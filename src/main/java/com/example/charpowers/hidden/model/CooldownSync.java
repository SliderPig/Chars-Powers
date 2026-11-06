package com.example.charpowers.hidden.model;

// Palladium's bar keeps its own cooldown per key SLOT (and won't let the key be pressed while it runs), but a hidden
// power's cooldown belongs to the POWER. So the slot's bar is set again whenever the two disagree: a power swapped
// into the slot, a cooldown that started later (Ground Slam throwing itself), or one given back.
public final class CooldownSync {
    public static final int SLACK = 2; // ticks; both count down together, so only a real change is bigger

    public static boolean needsSync(int barLeft, int powerLeft) {
        return Math.abs(barLeft - powerLeft) > SLACK;
    }

    private CooldownSync() {}
}
