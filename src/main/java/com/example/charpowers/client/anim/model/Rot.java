package com.example.charpowers.client.anim.model;

// A turn in the player model's space: x is the player's left, y is down, z is backwards, angles in radians.
// The game turns a model part by Z then Y then X (Rz * Ry * Rx), so that's what "euler" means here.
public final class Rot {
    public static final Rot NONE = new Rot(1, 0, 0, 0, 1, 0, 0, 0, 1);
    private final double m00, m01, m02, m10, m11, m12, m20, m21, m22;

    private Rot(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    public static Rot x(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Rot(1, 0, 0, 0, c, -s, 0, s, c);
    }

    public static Rot y(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Rot(c, 0, s, 0, 1, 0, -s, 0, c);
    }

    public static Rot z(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Rot(c, -s, 0, s, c, 0, 0, 0, 1);
    }

    // The same order a model part uses.
    public static Rot euler(double xRot, double yRot, double zRot) {
        return z(zRot).then(y(yRot)).then(x(xRot));
    }

    // This turn, with `inner` done first.
    public Rot then(Rot inner) {
        return new Rot(
                m00 * inner.m00 + m01 * inner.m10 + m02 * inner.m20,
                m00 * inner.m01 + m01 * inner.m11 + m02 * inner.m21,
                m00 * inner.m02 + m01 * inner.m12 + m02 * inner.m22,
                m10 * inner.m00 + m11 * inner.m10 + m12 * inner.m20,
                m10 * inner.m01 + m11 * inner.m11 + m12 * inner.m21,
                m10 * inner.m02 + m11 * inner.m12 + m12 * inner.m22,
                m20 * inner.m00 + m21 * inner.m10 + m22 * inner.m20,
                m20 * inner.m01 + m21 * inner.m11 + m22 * inner.m21,
                m20 * inner.m02 + m21 * inner.m12 + m22 * inner.m22);
    }

    public double[] apply(double x, double y, double z) {
        return new double[] {m00 * x + m01 * y + m02 * z, m10 * x + m11 * y + m12 * z, m20 * x + m21 * y + m22 * z};
    }

    // The undoing of this turn.
    public Rot inverse() {
        return new Rot(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }

    // {xRot, yRot, zRot} for a model part. When it can't tell Z and X apart (yRot at a quarter turn) it puts it all in xRot.
    public double[] toEuler() {
        double sy = -m20;
        if (Math.abs(sy) > 0.99999) {
            // yRot is +-90 degrees: Rz * Ry * Rx only leaves the difference of x and z, and zRot = 0 keeps that
            return new double[] {Math.atan2(sy > 0 ? m01 : -m01, m11), Math.copySign(Math.PI / 2, sy), 0};
        }
        return new double[] {Math.atan2(m21, m22), Math.asin(sy), Math.atan2(m10, m00)};
    }

    // Quaternion {x, y, z, w} of this turn, for the game's own pose stack.
    public float[] toQuaternion() {
        double t = m00 + m11 + m22;
        double x, y, z, w;
        if (t > 0) {
            double s = Math.sqrt(t + 1) * 2;
            w = s / 4;
            x = (m21 - m12) / s;
            y = (m02 - m20) / s;
            z = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            double s = Math.sqrt(1 + m00 - m11 - m22) * 2;
            w = (m21 - m12) / s;
            x = s / 4;
            y = (m01 + m10) / s;
            z = (m02 + m20) / s;
        } else if (m11 > m22) {
            double s = Math.sqrt(1 + m11 - m00 - m22) * 2;
            w = (m02 - m20) / s;
            x = (m01 + m10) / s;
            y = s / 4;
            z = (m12 + m21) / s;
        } else {
            double s = Math.sqrt(1 + m22 - m00 - m11) * 2;
            w = (m10 - m01) / s;
            x = (m02 + m20) / s;
            y = (m12 + m21) / s;
            z = s / 4;
        }
        return new float[] {(float) x, (float) y, (float) z, (float) w};
    }

    // The smallest turn that swings an arm (which hangs along +y) to point along this direction, without twisting it.
    public static Rot pointing(double dx, double dy, double dz) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) return NONE;
        dx /= len;
        dy /= len;
        dz /= len;
        // straight up has no one answer: swing it through the front
        if (dy < -0.99999) return x(-Math.PI);
        // the turn about (dz, 0, -dx) that takes +y to d
        double k = 1 / (1 + dy);
        return new Rot(
                1 - dx * dx * k, dx, -dx * dz * k,
                -dx, dy, -dz,
                -dx * dz * k, dz, 1 - dz * dz * k);
    }
}
