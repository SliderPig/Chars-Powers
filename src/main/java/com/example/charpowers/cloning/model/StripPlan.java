package com.example.charpowers.cloning.model;

import com.example.charpowers.cloning.model.RoutePlanner.Cell;

import java.util.ArrayList;
import java.util.List;

// Strip mining: one long main tunnel, and every few blocks a branch each side, out and back. No game code.
public final class StripPlan {
    public record Leg(Cell to, boolean branch) {}

    public static final int MAIN = 48, SPACING = 3, BRANCH = 12;

    public final Cell origin;
    public final int dirX, dirZ;
    private final List<Leg> legs = new ArrayList<>();

    // dirX/dirZ: one of the four directions along the ground.
    public StripPlan(Cell origin, int dirX, int dirZ) {
        this.origin = origin;
        this.dirX = dirX;
        this.dirZ = dirZ;
        for (int i = 1; i <= MAIN / SPACING; i++) {
            Cell main = at(i * SPACING, 0);
            legs.add(new Leg(main, false));
            legs.add(new Leg(at(i * SPACING, -BRANCH), true));
            legs.add(new Leg(main, false));
            legs.add(new Leg(at(i * SPACING, BRANCH), true));
            legs.add(new Leg(main, false));
        }
    }

    // along: blocks forwards, side: blocks to the right (negative = left)
    private Cell at(int along, int side) {
        int rx = -dirZ, rz = dirX;
        return new Cell(origin.x() + dirX * along + rx * side, origin.y(), origin.z() + dirZ * along + rz * side);
    }

    public int size() {
        return legs.size();
    }

    public Leg leg(int i) {
        return legs.get(i);
    }

    // Where the next strip starts: beside the branches of this one, so the two don't cut into each other.
    public Cell nextOrigin() {
        return at(0, 2 * BRANCH + SPACING);
    }
}
