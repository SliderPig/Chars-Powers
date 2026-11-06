package com.example.charpowers.client.anim.model;

// Where every part of the player model is this frame: x, y, z of its pivot in model pixels, then xRot, yRot, zRot in radians.
// It starts as whatever the game worked out, and the poses change it.
public final class Body {
    public static final int HEAD = 0, CHEST = 1, RIGHT_ARM = 2, LEFT_ARM = 3, RIGHT_LEG = 4, LEFT_LEG = 5, PARTS = 6;
    public final double[][] part = new double[PARTS][6];
    public final double[] lift = new double[3]; // the whole body moved: left, down, back, in model pixels
    public final double[] tip = new double[3];  // the whole body tipped: forward, turning right, leaning left
    public int changed; // one bit per part
    public boolean rootChanged;

    public void set(int which, double x, double y, double z, double xr, double yr, double zr) {
        double[] p = part[which];
        p[0] = x;
        p[1] = y;
        p[2] = z;
        p[3] = xr;
        p[4] = yr;
        p[5] = zr;
    }

    // A person standing still, for when the game has no model to ask: first person.
    public void rest() {
        set(HEAD, 0, 0, 0, 0, 0, 0);
        set(CHEST, 0, 0, 0, 0, 0, 0);
        set(RIGHT_ARM, -5, 2, 0, 0, 0, 0);
        set(LEFT_ARM, 5, 2, 0, 0, 0, 0);
        set(RIGHT_LEG, -1.9, 12, 0, 0, 0, 0);
        set(LEFT_LEG, 1.9, 12, 0, 0, 0, 0);
        changed = 0;
        rootChanged = false;
        lift[0] = lift[1] = lift[2] = 0;
        tip[0] = tip[1] = tip[2] = 0;
    }

    public boolean didChange(int which) {
        return (changed & (1 << which)) != 0;
    }

    void mark(int which) {
        changed |= 1 << which;
    }
}
