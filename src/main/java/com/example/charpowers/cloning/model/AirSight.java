package com.example.charpowers.cloning.model;

import com.example.charpowers.cloning.model.RoutePlanner.Cell;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// What a player standing here could see: the open air that connects to them, and any ore touching that air.
// An ore sitting inside solid rock is not seen. Spread over several ticks (a budget of cells per call). No game code.
public final class AirSight {
    public interface Space {
        boolean open(int x, int y, int z);

        boolean ore(int x, int y, int z);

        // lava next to it: leave that ore alone
        boolean lavaNear(int x, int y, int z);

        // could you stand here (open, with something to stand on)?
        boolean standable(int x, int y, int z);

        // open to the sky, as opposed to being in a cave
        boolean sky(int x, int y, int z);
    }

    private final Space space;
    private final Cell start;
    private final int radius, below, above, maxCells;
    private final ArrayDeque<Cell> queue = new ArrayDeque<>();
    private final Set<Cell> seen = new HashSet<>();
    private final Set<Cell> oreSet = new HashSet<>();
    private final List<Cell> ores = new ArrayList<>();
    private final List<Cell> caveFloors = new ArrayList<>();
    private int visited;

    private static final int[][] SIDES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    public AirSight(Space space, Cell start, int radius, int below, int above, int maxCells) {
        this.space = space;
        this.start = start;
        this.radius = radius;
        this.below = below;
        this.above = above;
        this.maxCells = maxCells;
        queue.add(start);
        seen.add(start);
        // the start may be a feet cell: the head cell above it is open air too
        Cell head = new Cell(start.x(), start.y() + 1, start.z());
        queue.add(head);
        seen.add(head);
    }

    // True once it has looked at everything it's going to.
    public boolean step(int budget) {
        while (budget-- > 0) {
            Cell at = queue.poll();
            if (at == null || visited >= maxCells) {
                queue.clear();
                return true;
            }
            visited++;
            if (!space.sky(at.x(), at.y(), at.z()) && space.standable(at.x(), at.y(), at.z())) caveFloors.add(at);
            for (int[] d : SIDES) {
                Cell n = new Cell(at.x() + d[0], at.y() + d[1], at.z() + d[2]);
                if (Math.abs(n.x() - start.x()) > radius || Math.abs(n.z() - start.z()) > radius
                        || n.y() < start.y() - below || n.y() > start.y() + above) continue;
                if (space.open(n.x(), n.y(), n.z())) {
                    if (seen.add(n)) queue.add(n);
                } else if (space.ore(n.x(), n.y(), n.z()) && !oreSet.contains(n) && !space.lavaNear(n.x(), n.y(), n.z())) {
                    oreSet.add(n);
                    ores.add(n);
                }
            }
        }
        return false;
    }

    public boolean done() {
        return queue.isEmpty();
    }

    // In the order they were found: nearest through the air first.
    public List<Cell> ores() {
        return ores;
    }

    public int visited() {
        return visited;
    }

    // The deepest place in a cave it can stand, at least `minAway` blocks from where it started, skipping ones it has
    // been to. Null = no cave in sight.
    public Cell deepestCave(int minAway, Set<Cell> beenTo, int beenWithin) {
        Cell best = null;
        for (Cell c : caveFloors) {
            int away = Math.abs(c.x() - start.x()) + Math.abs(c.z() - start.z()) + Math.abs(c.y() - start.y());
            if (away < minAway) continue;
            boolean old = false;
            for (Cell b : beenTo) {
                if (Math.abs(b.x() - c.x()) <= beenWithin && Math.abs(b.y() - c.y()) <= beenWithin && Math.abs(b.z() - c.z()) <= beenWithin) {
                    old = true;
                    break;
                }
            }
            if (old) continue;
            if (best == null || c.y() < best.y() || (c.y() == best.y() && away > distance(best))) best = c;
        }
        return best;
    }

    private int distance(Cell c) {
        return Math.abs(c.x() - start.x()) + Math.abs(c.z() - start.z()) + Math.abs(c.y() - start.y());
    }
}
