package com.example.charpowers.authority.model;

// Whether the blocks around a seat make a throne, in any of the 4 directions.
public final class ThroneRule {
    public interface Blocks {
        boolean gold(int x, int y, int z);

        boolean diamond(int x, int y, int z);
    }

    // Which way the back is from the seat, for each answer below.
    public static final int[][] BACK = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private ThroneRule() {}

    // The gold throne: a 3x3 base, an armrest each side of the seat, and a back 3 blocks tall. 14 blocks.
    // Returns which way its back is (an index into BACK), or -1 if it isn't one.
    public static int gold(Blocks b, int x, int y, int z) {
        if (!base(b, x, y, z, 1)) return -1;
        for (int d = 0; d < 4; d++) {
            int bx = BACK[d][0], bz = BACK[d][1], px = -bz, pz = bx;
            if (b.gold(x + px, y + 1, z + pz) && b.gold(x - px, y + 1, z - pz) && column(b, x + bx, y + 1, z + bz, 3)) return d;
        }
        return -1;
    }

    // The superior throne (the list only says bigger and more decorated): a 5x5 base, armrests 2 tall,
    // a back 5 tall with a diamond block on top, and a 3 tall wing each side of the back. 40 gold blocks and a diamond.
    public static int superior(Blocks b, int x, int y, int z) {
        if (!base(b, x, y, z, 2)) return -1;
        for (int d = 0; d < 4; d++) {
            int bx = BACK[d][0], bz = BACK[d][1], px = -bz, pz = bx;
            if (column(b, x + px, y + 1, z + pz, 2) && column(b, x - px, y + 1, z - pz, 2)
                    && column(b, x + bx, y + 1, z + bz, 5) && b.diamond(x + bx, y + 6, z + bz)
                    && column(b, x + bx + px, y + 1, z + bz + pz, 3) && column(b, x + bx - px, y + 1, z + bz - pz, 3)) return d;
        }
        return -1;
    }

    private static boolean base(Blocks b, int x, int y, int z, int reach) {
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (!b.gold(x + dx, y, z + dz)) return false;
            }
        }
        return true;
    }

    private static boolean column(Blocks b, int x, int y, int z, int height) {
        for (int i = 0; i < height; i++) {
            if (!b.gold(x, y + i, z)) return false;
        }
        return true;
    }
}
