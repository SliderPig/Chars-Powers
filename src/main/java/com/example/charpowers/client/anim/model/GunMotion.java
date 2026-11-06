package com.example.charpowers.client.anim.model;

// The little movements of a heavy gun in living hands: breathing, the bob of walking, the shake of firing. All worked out from
// the clock, so nothing has to be remembered: x left, y down, z back in model pixels, then a small turn in radians.
public final class GunMotion {
    public static final GunMotion STILL = new GunMotion(0, 0, 0, 0, 0, 0);
    public final double dx, dy, dz, rx, ry, rz;

    public GunMotion(double dx, double dy, double dz, double rx, double ry, double rz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
    }

    // time in ticks. fire: 0 not firing, 1 flat out. walkPosition and walkSpeed as the game's walk animation has them.
    public static GunMotion at(double t, double fire, double walkPosition, double walkSpeed) {
        double dx = 0.22 * Math.sin(t * 0.05 + 2);
        double dy = 0.3 * Math.sin(t * 0.07) + 0.12 * Math.sin(t * 0.16 + 1);
        double dz = 0.15 * Math.sin(t * 0.06 + 4);
        double rx = 0.007 * Math.sin(t * 0.06 + 0.5);
        double ry = 0.006 * Math.sin(t * 0.045);
        double rz = 0.005 * Math.sin(t * 0.03 + 1);
        double walk = Math.min(1, walkSpeed);
        if (walk > 0) {
            dy += walk * 0.75 * Math.sin(walkPosition * 0.6662 * 2);
            dx += walk * 0.45 * Math.cos(walkPosition * 0.6662);
            rz += walk * 0.018 * Math.cos(walkPosition * 0.6662);
            rx += walk * 0.012 * Math.sin(walkPosition * 0.6662 * 2);
        }
        if (fire > 0) {
            // always pushed back a bit, with a fast rattle on top, the barrel climbing a little
            dz += fire * (0.75 + 0.45 * Math.sin(t * 5.1) + 0.3 * Math.sin(t * 7.3 + 1));
            dy += fire * 0.25 * Math.sin(t * 6.1 + 2);
            dx += fire * 0.25 * Math.sin(t * 4.7);
            rx += fire * (-0.022 + 0.016 * Math.sin(t * 5.9));
            ry += fire * 0.012 * Math.sin(t * 4.3 + 1);
            rz += fire * 0.014 * Math.sin(t * 3.7);
        }
        return new GunMotion(dx, dy, dz, rx, ry, rz);
    }
}
