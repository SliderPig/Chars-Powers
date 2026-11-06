package com.example.charpowers.cloning.model;

import com.example.charpowers.cloning.model.RoutePlanner.Cell;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

// The rest of the vein a clone is part way through, so it finishes that before wandering off to something rarer.
// No game code here, so it can be unit tested.
public final class Vein {
    private final Set<Cell> left = new HashSet<>();

    // That ore just broke: any ore touching it, corners too, is the same vein.
    public void broke(Cell at, Predicate<Cell> isOre) {
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            Cell near = new Cell(at.x() + x, at.y() + y, at.z() + z);
            if (isOre.test(near)) left.add(near);
        }
    }

    // The nearest part still there. Null = this vein is finished.
    public Cell next(Cell from, Predicate<Cell> isOre) {
        left.removeIf(isOre.negate());
        Cell best = null;
        int bestFar = Integer.MAX_VALUE;
        for (Cell cell : left) {
            int dx = cell.x() - from.x(), dy = cell.y() - from.y(), dz = cell.z() - from.z();
            int far = dx * dx + dy * dy + dz * dz;
            if (far < bestFar) {
                bestFar = far;
                best = cell;
            }
        }
        return best;
    }

    public boolean contains(Cell cell) {
        return left.contains(cell);
    }

    public boolean isEmpty() {
        return left.isEmpty();
    }

    public int size() {
        return left.size();
    }
}
