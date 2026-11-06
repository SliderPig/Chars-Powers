package com.example.charpowers.items.model;

// Which set of moves a weapon swings with, how long each move lasts, and at what point in it the hit lands. No game code in here.
// Moves 0 .. lights-1 take turns as you keep swinging; heavy is for a crouching swing, dash for a sprinting one, idle plays now and then when you stand still.
public enum WeaponStyle {
    //        two hands, other hand up the grip, flat picture, move lengths in ticks, where the hit lands (0 to 1), lights, heavy, dash, idle
    GOLD(true, 0.14f, false, ticks(10, 10, 14, 16), at(0.4, 0.4, 0.57, 0.375), 4, -1, -1, -1),
    KATANA(true, 0.13f, false, ticks(9, 9, 18, 34), at(0.4, 0.4, 0.48, 0), 2, 2, -1, 3),
    DAGGER(false, 0f, false, ticks(6, 6, 10), at(0.42, 0.42, 0.45), 2, -1, 2, -1),
    HAMMER(true, 0.3f, false, ticks(18, 16), at(0.62, 0.55), 2, -1, -1, -1),
    STAFF(true, 0.3f, false, ticks(10, 22), at(0.4, 0.72), 1, 1, -1, -1),
    FORK(false, 0f, false, ticks(6, 6, 26), at(0.45, 0.45, 0), 2, -1, -1, 2),
    SPRAY(false, 0f, false, ticks(6), at(0), 1, -1, -1, -1),
    KNIGHT(false, 0f, true, ticks(7, 7, 8), at(0.4, 0.4, 0.45), 3, -1, -1, -1),
    CRIMSON(false, 0f, true, ticks(9, 9, 15), at(0.4, 0.4, 0.62), 2, 2, -1, -1),
    GRAVITY(true, 0.13f, true, ticks(14, 16), at(0.55, 0.62), 1, 1, -1, -1),
    SPEAR(true, 0.42f, true, ticks(11, 11, 16), at(0.42, 0.42, 0.55), 2, 2, -1, -1),
    SCYTHE(true, 0.36f, true, ticks(16, 16, 18), at(0.5, 0.5, 0.62), 2, 2, -1, -1),
    COCYTUS(true, 0.3f, true, ticks(18, 16), at(0.62, 0.55), 2, -1, -1, -1);

    public final boolean twoHanded, flat;
    // how far up the grip the other hand holds it, in blocks, in first person
    public final float otherHand;
    private final int[] lengths;
    private final float[] hits;
    public final int lights, heavy, dash, idle;

    WeaponStyle(boolean twoHanded, float otherHand, boolean flat, int[] lengths, float[] hits, int lights, int heavy, int dash, int idle) {
        this.twoHanded = twoHanded;
        this.otherHand = otherHand;
        this.flat = flat;
        this.lengths = lengths;
        this.hits = hits;
        this.lights = lights;
        this.heavy = heavy;
        this.dash = dash;
        this.idle = idle;
    }

    private static int[] ticks(int... t) {
        return t;
    }

    private static float[] at(double... a) {
        float[] f = new float[a.length];
        for (int i = 0; i < a.length; i++) f[i] = (float) a[i];
        return f;
    }

    public int moves() {
        return lengths.length;
    }

    public int length(int step) {
        return lengths[Math.floorMod(step, lengths.length)];
    }

    // Ticks after the swing starts that the blow lands: when the blade passes through.
    public int hitTick(int step) {
        int s = Math.floorMod(step, lengths.length);
        return Math.round(hits[s] * lengths[s]);
    }

    // The next move. prev = the move before, ticksSince = how long ago it started.
    public int pick(int prev, float ticksSince, boolean crouching, boolean sprinting) {
        if (sprinting && dash >= 0) return dash;
        if (crouching && heavy >= 0) return heavy;
        if (prev >= 0 && prev < lights && ticksSince < length(prev) + 20) return (prev + 1) % lights;
        return 0;
    }
}
