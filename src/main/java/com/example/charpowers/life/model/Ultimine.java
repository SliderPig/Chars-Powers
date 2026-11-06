package com.example.charpowers.life.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

// Which blocks a hold-and-mine hits for each shape, with no game code in it. The same maths runs on the server (to break)
// and on the PC (to draw the outline), so they agree.
public final class Ultimine {
    public record P(int x, int y, int z) {
        P plus(int dx, int dy, int dz) {
            return new P(x + dx, y + dy, z + dz);
        }
    }

    public enum Shape {
        SHAPELESS("shapeless"), SMALL_TUNNEL("small_tunnel"), LARGE_TUNNEL("large_tunnel"), MINING_TUNNEL("mining_tunnel"),
        ESCAPE_TUNNEL("escape_tunnel"), DIAGONAL_TUNNEL("diagonal_tunnel"), SMALL_SQUARE("small_square"), SQUARE("square");

        public final String id;

        Shape(String id) {
            this.id = id;
        }

        public static Shape of(int index) {
            Shape[] all = values();
            return all[Math.floorMod(index, all.length)];
        }
    }

    // Face and facing numbers are Minecraft's own: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.
    private static final int[][] NORMAL = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
    public static final int MAX_DEPTH = 64;

    private Ultimine() {}

    // face = the side of the block you are looking at. facing = which way you face (2..5). feetY = your feet height.
    // The first block in the answer is always the one you hit. At most `max` blocks, all of them breakable.
    public static List<P> cells(Shape shape, P origin, int face, int facing, int feetY, int max, Predicate<P> breakable, Predicate<P> sameKind) {
        Set<P> out = new LinkedHashSet<>();
        if (max < 1 || !breakable.test(origin)) return new ArrayList<>();
        out.add(origin);
        int[] into = {-NORMAL[face][0], -NORMAL[face][1], -NORMAL[face][2]};
        int[][] axes = across(face);
        switch (shape) {
            case SHAPELESS -> shapeless(out, origin, max, breakable, sameKind);
            case SMALL_SQUARE -> flat(out, origin, axes, 1, max, breakable);
            case SQUARE -> flat(out, origin, axes, 2, max, breakable);
            case SMALL_TUNNEL -> tunnel(out, origin, into, axes, 0, max, breakable);
            case LARGE_TUNNEL -> tunnel(out, origin, into, axes, 1, max, breakable);
            case MINING_TUNNEL -> mining(out, origin, into, axes, face, feetY, max, breakable);
            case ESCAPE_TUNNEL -> stairs(out, origin, facing, 1, max, breakable);
            case DIAGONAL_TUNNEL -> stairs(out, origin, facing, -1, max, breakable);
        }
        return new ArrayList<>(out);
    }

    // The two directions across the face you hit.
    private static int[][] across(int face) {
        return switch (face) {
            case 0, 1 -> new int[][] {{1, 0, 0}, {0, 0, 1}};
            case 2, 3 -> new int[][] {{1, 0, 0}, {0, 1, 0}};
            default -> new int[][] {{0, 0, 1}, {0, 1, 0}};
        };
    }

    private static P at(P o, int[] into, int depth, int[][] axes, int a, int b) {
        return o.plus(into[0] * depth + axes[0][0] * a + axes[1][0] * b,
                into[1] * depth + axes[0][1] * a + axes[1][1] * b,
                into[2] * depth + axes[0][2] * a + axes[1][2] * b);
    }

    private static boolean full(Set<P> out, int max) {
        return out.size() >= max;
    }

    private static void add(Set<P> out, P p, int max, Predicate<P> breakable) {
        if (!full(out, max) && breakable.test(p)) out.add(p);
    }

    // Middle first, so a small limit still takes the middle.
    private static List<int[]> square(int r) {
        List<int[]> list = new ArrayList<>();
        for (int a = -r; a <= r; a++) for (int b = -r; b <= r; b++) list.add(new int[] {a, b});
        list.sort((p, q) -> Integer.compare(p[0] * p[0] + p[1] * p[1], q[0] * q[0] + q[1] * q[1]));
        return list;
    }

    private static void flat(Set<P> out, P o, int[][] axes, int r, int max, Predicate<P> breakable) {
        for (int[] ab : square(r)) add(out, at(o, new int[] {0, 0, 0}, 0, axes, ab[0], ab[1]), max, breakable);
    }

    private static void tunnel(Set<P> out, P o, int[] into, int[][] axes, int r, int max, Predicate<P> breakable) {
        List<int[]> slice = square(r);
        for (int depth = 0; depth < MAX_DEPTH && !full(out, max); depth++) {
            for (int[] ab : slice) add(out, at(o, into, depth, axes, ab[0], ab[1]), max, breakable);
        }
    }

    // One wide, two tall, the way you walk through it: the second block is below if you hit a high one, above if you hit a low one.
    private static void mining(Set<P> out, P o, int[] into, int[][] axes, int face, int feetY, int max, Predicate<P> breakable) {
        if (face <= 1) {
            tunnel(out, o, into, axes, 0, max, breakable);
            return;
        }
        int other = o.y() - feetY >= 1 ? -1 : 1;
        for (int depth = 0; depth < MAX_DEPTH && !full(out, max); depth++) {
            add(out, at(o, into, depth, axes, 0, 0), max, breakable);
            add(out, at(o, into, depth, axes, 0, other), max, breakable);
        }
    }

    // A staircase up (+1) or down (-1) in the way you face, three tall so you can walk it.
    private static void stairs(Set<P> out, P o, int facing, int up, int max, Predicate<P> breakable) {
        int fx = NORMAL[facing][0], fz = NORMAL[facing][2];
        if (facing < 2) {
            fx = 0;
            fz = 1;
        }
        for (int k = 0; k < MAX_DEPTH && !full(out, max); k++) {
            P base = o.plus(fx * k, up * k, fz * k);
            for (int h = 0; h < 3; h++) add(out, base.plus(0, h, 0), max, breakable);
        }
    }

    // Everything of the same kind joined up to the block, corners included, nearest first.
    private static void shapeless(Set<P> out, P o, int max, Predicate<P> breakable, Predicate<P> sameKind) {
        Set<P> seen = new java.util.HashSet<>();
        Deque<P> queue = new ArrayDeque<>();
        seen.add(o);
        queue.add(o);
        while (!queue.isEmpty() && !full(out, max)) {
            P at = queue.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                P next = at.plus(dx, dy, dz);
                if (!seen.add(next) || !sameKind.test(next) || !breakable.test(next)) continue;
                out.add(next);
                queue.add(next);
                if (full(out, max)) return;
            }
        }
    }
}
