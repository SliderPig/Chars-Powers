package com.example.charpowers.items.model;

import java.util.Random;

// The shape of a King's Rock. Works out whether a spot, measured from the middle of the rock's base, is rock, and what kind. It's a lump with noise
// carved out of it, leaning to one side, with ledges in it and gold running through in thin veins. No game code in here.
public final class RockShape {
    public enum Kind {AIR, STONE, ANDESITE, TUFF, COBBLE, MOSSY, MOSS, GOLD, RAW_GOLD}

    public final double radiusX, radiusZ, height;
    private final double leanX, leanZ, angle;
    private final long seed;

    public RockShape(long seed) {
        this.seed = seed;
        Random r = new Random(seed);
        radiusX = 7.5 + r.nextDouble() * 2.5;
        radiusZ = 6 + r.nextDouble() * 2.5;
        height = 13 + r.nextDouble() * 4;
        angle = r.nextDouble() * Math.PI * 2;
        double lean = 0.12 + r.nextDouble() * 0.16;
        double leanAngle = r.nextDouble() * Math.PI * 2;
        leanX = Math.cos(leanAngle) * lean;
        leanZ = Math.sin(leanAngle) * lean;
    }

    // The most any part of the rock can reach sideways from the middle. Anything further out is always air.
    public int reach() {
        return (int) Math.ceil(Math.max(radiusX, radiusZ) * 1.35 + Math.max(Math.abs(leanX), Math.abs(leanZ)) * height) + 1;
    }

    public int top() {
        return (int) Math.ceil(height * 1.3) + 1;
    }

    // Positive inside the rock.
    private double field(double x, double y, double z) {
        double cx = x - leanX * Math.max(y, 0), cz = z - leanZ * Math.max(y, 0);
        double a = cx * Math.cos(angle) + cz * Math.sin(angle), b = -cx * Math.sin(angle) + cz * Math.cos(angle);
        // wide at the bottom, narrowing to the top, with a heavier shoulder on one side
        double up = Math.max(y, 0) / height;
        double taper = 1.05 - 0.5 * up * up;
        double shoulder = 1 + 0.22 * Math.sin(up * 3.1 + angle);
        double rx = radiusX * taper * shoulder, rz = radiusZ * taper;
        double round = (a * a) / (rx * rx) + (b * b) / (rz * rz) + (y > 0 ? (y * y) / (height * height) : 0);
        double big = fractal(x * 0.13, y * 0.17, z * 0.13, 3, 11) * 0.62;
        // ledges: stone splits along flat layers
        double ledge = Math.sin(y * 1.15 + fractal(x * 0.09, 0, z * 0.09, 2, 23) * 3) * 0.05;
        return 1 - round + big + ledge;
    }

    public boolean solid(int x, int y, int z) {
        if (Math.abs(x) > reach() || Math.abs(z) > reach() || y > top()) return false;
        return field(x, y, z) > 0;
    }

    public Kind kind(int x, int y, int z) {
        if (!solid(x, y, z)) return Kind.AIR;
        boolean open = !solid(x, y + 1, z);
        double wet = fractal(x * 0.21, y * 0.21, z * 0.21, 2, 41);
        if (open && wet > -0.08) return Kind.MOSS;
        boolean surface = open || !solid(x + 1, y, z) || !solid(x - 1, y, z) || !solid(x, y, z + 1) || !solid(x, y, z - 1);
        double vein = Math.abs(fractal(x * 0.19 + 7, y * 0.24, z * 0.19 - 3, 2, 57));
        if (vein < 0.022) {
            double core = fractal(x * 0.5, y * 0.5, z * 0.5, 1, 71);
            return core > 0.45 ? Kind.RAW_GOLD : Kind.GOLD;
        }
        if (surface && wet > 0.1) return Kind.MOSSY;
        double layer = fractal(x * 0.08, y * 0.45, z * 0.08, 2, 83);
        if (layer > 0.28) return Kind.ANDESITE;
        if (layer < -0.32) return Kind.TUFF;
        if (surface && wet < -0.34) return Kind.COBBLE;
        return Kind.STONE;
    }

    // Where the sword goes: the highest ground close to the middle, as {x, y, z}, y being the rock block itself.
    public int[] peak() {
        int bestX = 0, bestZ = 0, bestY = 0;
        double best = -1e9;
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = top(); y > 0; y--) {
                    if (solid(x, y, z)) {
                        double score = y - Math.sqrt(x * x + z * z) * 0.35;
                        if (score > best) {
                            best = score;
                            bestX = x;
                            bestY = y;
                            bestZ = z;
                        }
                        break;
                    }
                }
            }
        }
        return new int[]{bestX, bestY, bestZ};
    }

    // Smooth noise from about -1 to 1.
    private double fractal(double x, double y, double z, int octaves, int salt) {
        double total = 0, amp = 1, sum = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise(x, y, z, salt + i) * amp;
            sum += amp;
            amp *= 0.5;
            x *= 2.03;
            y *= 2.03;
            z *= 2.03;
        }
        return total / sum;
    }

    private double noise(double x, double y, double z, int salt) {
        int ix = floor(x), iy = floor(y), iz = floor(z);
        double fx = fade(x - ix), fy = fade(y - iy), fz = fade(z - iz);
        double c00 = lerp(fx, hash(ix, iy, iz, salt), hash(ix + 1, iy, iz, salt));
        double c10 = lerp(fx, hash(ix, iy + 1, iz, salt), hash(ix + 1, iy + 1, iz, salt));
        double c01 = lerp(fx, hash(ix, iy, iz + 1, salt), hash(ix + 1, iy, iz + 1, salt));
        double c11 = lerp(fx, hash(ix, iy + 1, iz + 1, salt), hash(ix + 1, iy + 1, iz + 1, salt));
        return lerp(fz, lerp(fy, c00, c10), lerp(fy, c01, c11));
    }

    private double hash(int x, int y, int z, int salt) {
        long h = seed * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL;
        h ^= x * 0x165667B19E3779F9L;
        h = Long.rotateLeft(h, 27) * 0x85EBCA77C2B2AE63L;
        h ^= y * 0x27D4EB2F165667C5L;
        h = Long.rotateLeft(h, 31) * 0x9E3779B97F4A7C15L;
        h ^= z * 0x85EBCA6B9E3779B1L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return ((h & 0xFFFFFFL) / (double) 0xFFFFFFL) * 2 - 1;
    }

    private static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    private static double fade(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double t, double a, double b) {
        return a + (b - a) * t;
    }
}
