package com.example.charpowers.house.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

// Is this pile of blocks really a castle? Pure maths on which cells are solid, seen as columns from above.
// A castle has: walls at least 5 high on at least 2 sides or round a courtyard, open space in it (it's not a solid lump),
// at least 2 towers taller than the walls, and a doorway in a wall. Doors and gates count as open.
public final class CastleRule {
    public static final int MIN_WALL = 5, TOWERS = 2, TOWER_OVER_WALL = 2, MIN_YARD = 9;

    private final int sx, sz;
    private final BitSet[] columns;

    public CastleRule(int sx, int sz) {
        this.sx = sx;
        this.sz = sz;
        this.columns = new BitSet[sx * sz];
    }

    public void solid(int x, int y, int z) {
        int i = x + z * sx;
        if (columns[i] == null) columns[i] = new BitSet();
        columns[i].set(y);
    }

    private BitSet col(int x, int z) {
        return x < 0 || z < 0 || x >= sx || z >= sz ? null : columns[x + z * sx];
    }

    private int height(int x, int z) {
        BitSet c = col(x, z);
        return c == null || c.isEmpty() ? 0 : c.length() - c.nextSetBit(0);
    }

    // Most of the way up is filled in: windows are fine, a floor with a far-off roof above it isn't a wall.
    private boolean isWall(int x, int z) {
        BitSet c = col(x, z);
        if (c == null) return false;
        int h = height(x, z);
        return h >= MIN_WALL && c.cardinality() * 10 >= h * 6;
    }

    // Everything still missing, in plain words. Empty = it's a castle.
    public List<String> missing() {
        List<String> out = new ArrayList<>();
        int walls = 0, x0 = sx, x1 = -1, z0 = sz, z1 = -1;
        int[] heights = new int[sx * sz];
        for (int z = 0; z < sz; z++) {
            for (int x = 0; x < sx; x++) {
                if (isWall(x, z)) {
                    heights[walls++] = height(x, z);
                    x0 = Math.min(x0, x);
                    x1 = Math.max(x1, x);
                    z0 = Math.min(z0, z);
                    z1 = Math.max(z1, z);
                }
            }
        }
        if (walls < 8) {
            out.add("It needs walls at least " + MIN_WALL + " blocks high.");
            out.add("It needs " + TOWERS + " more towers, taller than the walls.");
            out.add("It needs a doorway in a wall.");
            return out;
        }
        int[] sorted = Arrays.copyOf(heights, walls);
        Arrays.sort(sorted);
        int wallHeight = sorted[walls / 4]; // towers are the tall minority, so look low

        if (sides(x0, x1, z0, z1) < 2 && enclosedYard(x0, x1, z0, z1) < MIN_YARD) {
            out.add("The walls need to run along at least 2 sides, or right round a courtyard.");
        }
        if (openColumns(x0, x1, z0, z1) < MIN_YARD) {
            out.add("It's a solid lump: a castle needs rooms or a courtyard inside.");
        }
        int towers = towers(wallHeight);
        if (towers < TOWERS) {
            int more = TOWERS - towers;
            out.add("It needs " + more + " more tower" + (more == 1 ? "" : "s") + ", at least " + TOWER_OVER_WALL + " blocks taller than the walls and 2 wide.");
        }
        if (!hasDoorway()) out.add("It needs a doorway in a wall, at least 2 high, with a way through.");
        return out;
    }

    // How many of the four edges of the wall outline are mostly wall.
    private int sides(int x0, int x1, int z0, int z1) {
        int w = x1 - x0 + 1, d = z1 - z0 + 1;
        if (w < 5 || d < 5) return 0;
        int band = Math.max(2, Math.min(w, d) / 4), sides = 0;
        boolean[] lines = new boolean[Math.max(w, d)];
        for (int side = 0; side < 4; side++) {
            Arrays.fill(lines, false);
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    boolean in = switch (side) {
                        case 0 -> z < z0 + band;
                        case 1 -> z > z1 - band;
                        case 2 -> x < x0 + band;
                        default -> x > x1 - band;
                    };
                    if (in && isWall(x, z)) lines[side < 2 ? x - x0 : z - z0] = true;
                }
            }
            int covered = 0;
            for (boolean b : lines) if (b) covered++;
            if (covered * 2 >= (side < 2 ? w : d)) sides++;
        }
        return sides;
    }

    private int openColumns(int x0, int x1, int z0, int z1) {
        int open = 0;
        for (int z = z0 + 1; z < z1; z++) {
            for (int x = x0 + 1; x < x1; x++) {
                if (!isWall(x, z)) open++;
            }
        }
        return open;
    }

    // Columns inside the outline you can't reach from outside without crossing a wall.
    private int enclosedYard(int x0, int x1, int z0, int z1) {
        int w = x1 - x0 + 3, d = z1 - z0 + 3;
        boolean[] seen = new boolean[w * d];
        ArrayDeque<int[]> todo = new ArrayDeque<>();
        todo.add(new int[] {0, 0});
        seen[0] = true;
        int reached = 0;
        while (!todo.isEmpty()) {
            int[] at = todo.poll();
            reached++;
            for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = at[0] + step[0], nz = at[1] + step[1];
                if (nx < 0 || nz < 0 || nx >= w || nz >= d || seen[nx + nz * w]) continue;
                if (isWall(nx + x0 - 1, nz + z0 - 1)) continue;
                seen[nx + nz * w] = true;
                todo.add(new int[] {nx, nz});
            }
        }
        int wallCount = 0;
        for (int z = z0; z <= z1; z++) {
            for (int x = x0; x <= x1; x++) {
                if (isWall(x, z)) wallCount++;
            }
        }
        return w * d - reached - wallCount;
    }

    // Groups of columns taller than the walls that are at least 2 wide each way. A long raised wall isn't a tower.
    private int towers(int wallHeight) {
        boolean[] seen = new boolean[sx * sz];
        int count = 0;
        for (int z = 0; z < sz; z++) {
            for (int x = 0; x < sx; x++) {
                if (seen[x + z * sx] || !tall(x, z, wallHeight)) continue;
                int minX = x, maxX = x, minZ = z, maxZ = z, size = 0;
                ArrayDeque<int[]> todo = new ArrayDeque<>();
                todo.add(new int[] {x, z});
                seen[x + z * sx] = true;
                while (!todo.isEmpty()) {
                    int[] at = todo.poll();
                    size++;
                    minX = Math.min(minX, at[0]);
                    maxX = Math.max(maxX, at[0]);
                    minZ = Math.min(minZ, at[1]);
                    maxZ = Math.max(maxZ, at[1]);
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = at[0] + dx, nz = at[1] + dz;
                            if (nx < 0 || nz < 0 || nx >= sx || nz >= sz || seen[nx + nz * sx] || !tall(nx, nz, wallHeight)) continue;
                            seen[nx + nz * sx] = true;
                            todo.add(new int[] {nx, nz});
                        }
                    }
                }
                if (size >= 4 && maxX > minX && maxZ > minZ) count++;
            }
        }
        return count;
    }

    private boolean tall(int x, int z, int wallHeight) {
        return isWall(x, z) && height(x, z) >= wallHeight + TOWER_OVER_WALL;
    }

    private boolean hasDoorway() {
        for (int z = 0; z < sz; z++) {
            for (int x = 0; x < sx; x++) {
                if (doorwayAt(x, z, 1, 0) || doorwayAt(x, z, 0, 1)) return true;
            }
        }
        return false;
    }

    // Standing at (x, z), wall on both sides along (ax, az) within 2 blocks, and a way through across it.
    private boolean doorwayAt(int x, int z, int ax, int az) {
        for (int a = 1; a <= 2; a++) {
            int lx = x - ax * a, lz = z - az * a;
            if (!isWall(lx, lz)) continue;
            for (int b = 1; b <= 2; b++) {
                int rx = x + ax * b, rz = z + az * b;
                if (!isWall(rx, rz)) continue;
                BitSet left = col(lx, lz), right = col(rx, rz);
                int from = Math.max(left.nextSetBit(0), right.nextSetBit(0));
                for (int y = from; y <= from + 3; y++) {
                    if (left.get(y) && left.get(y + 1) && right.get(y) && right.get(y + 1)
                            && open(x, y, z) && open(x + az, y, z + ax) && open(x - az, y, z - ax)) return true;
                }
            }
        }
        return false;
    }

    private boolean open(int x, int y, int z) {
        BitSet c = col(x, z);
        return c == null || (!c.get(y) && !c.get(y + 1));
    }
}
