package com.example.charpowers.hidden.model;

import java.util.function.Predicate;

// Where to land when teleporting behind someone: the first open spot of the caller's list (behind, left, right,
// front...), or null when every one is blocked, so you never end up inside a wall.
public final class SpotRule {
    public static int[] pick(int[][] candidates, Predicate<int[]> open) {
        for (int[] c : candidates) if (open.test(c)) return c;
        return null;
    }

    private SpotRule() {}
}
