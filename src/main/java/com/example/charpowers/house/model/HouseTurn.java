package com.example.charpowers.house.model;

// Turning a house round its centre, quarter turns clockwise seen from above. In the file every front faces south (+z).
// Same maths as the game's Rotation: one clockwise turn takes (x, z) to (-z, x).
public final class HouseTurn {
    private HouseTurn() {}

    public static int x(int dx, int dz, int turns) {
        return switch (turns & 3) {
            case 1 -> -dz;
            case 2 -> -dx;
            case 3 -> dz;
            default -> dx;
        };
    }

    public static int z(int dx, int dz, int turns) {
        return switch (turns & 3) {
            case 1 -> dx;
            case 2 -> -dz;
            case 3 -> -dx;
            default -> dz;
        };
    }

    // Where a cell of the file ends up, with the middle of the house on (cx, cz).
    public static int worldX(int fileX, int fileZ, int w, int d, int cx, int turns) {
        return cx + x(fileX - w / 2, fileZ - d / 2, turns);
    }

    public static int worldZ(int fileX, int fileZ, int w, int d, int cz, int turns) {
        return cz + z(fileX - w / 2, fileZ - d / 2, turns);
    }

    // {minX, minZ, maxX, maxZ} of the whole plot once turned.
    public static int[] rect(int w, int d, int cx, int cz, int turns) {
        int x0 = worldX(0, 0, w, d, cx, turns), x1 = worldX(w - 1, d - 1, w, d, cx, turns);
        int z0 = worldZ(0, 0, w, d, cz, turns), z1 = worldZ(w - 1, d - 1, w, d, cz, turns);
        return new int[] {Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1)};
    }

    // The player looks in direction `lookIndex` (0 south, 1 west, 2 north, 3 east: the game's 2D data value):
    // the first guess is the turn that points the front door back at them.
    public static int facingPlayer(int lookIndex) {
        return (lookIndex + 2) & 3;
    }
}
