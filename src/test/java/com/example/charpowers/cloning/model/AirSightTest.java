package com.example.charpowers.cloning.model;

import static org.junit.jupiter.api.Assertions.*;

import com.example.charpowers.cloning.model.RoutePlanner.Cell;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class AirSightTest {
    // A world: stone below y=0, anything listed as open or ore is carved out / placed.
    private static AirSight.Space world(Predicate<Cell> carved, Predicate<Cell> ore) {
        return new AirSight.Space() {
            public boolean open(int x, int y, int z) { return y >= 0 ? true : carved.test(new Cell(x, y, z)); }
            public boolean ore(int x, int y, int z) { return ore.test(new Cell(x, y, z)); }
            public boolean lavaNear(int x, int y, int z) { return false; }
            public boolean standable(int x, int y, int z) { return open(x, y, z) && open(x, y + 1, z) && !open(x, y - 1, z); }
            public boolean sky(int x, int y, int z) { return y >= 0 || (x > 100); }
        };
    }

    @Test
    void anOreInsideRockIsNotSeen() {
        AirSight s = new AirSight(world(c -> false, c -> c.equals(new Cell(0, -5, 0))), new Cell(0, 0, 0), 16, 16, 4, 5000);
        while (!s.step(500)) { }
        assertTrue(s.ores().isEmpty());
    }

    @Test
    void anOreOnTheSurfaceIsSeen() {
        AirSight s = new AirSight(world(c -> false, c -> c.equals(new Cell(3, -1, 0))), new Cell(0, 0, 0), 16, 16, 4, 5000);
        while (!s.step(500)) { }
        assertEquals(1, s.ores().size());
    }

    @Test
    void anOreInACaveWallIsSeenAndTheCaveIsFound() {
        // a shaft at x=5 down to y=-6, then a room
        Predicate<Cell> carved = c -> (c.x() == 5 && c.z() == 0 && c.y() >= -6) || (c.y() >= -8 && c.y() <= -6 && c.x() >= 5 && c.x() <= 9 && c.z() == 0);
        AirSight s = new AirSight(world(carved, c -> c.equals(new Cell(10, -7, 0))), new Cell(0, 0, 0), 16, 16, 4, 5000);
        while (!s.step(500)) { }
        assertEquals(Set.of(new Cell(10, -7, 0)), Set.copyOf(s.ores()));
        Cell cave = s.deepestCave(2, Set.of(), 3);
        assertNotNull(cave);
        assertTrue(cave.y() <= -7);
    }

    @Test
    void placesAlreadyVisitedAreSkipped() {
        Predicate<Cell> carved = c -> c.x() == 5 && c.z() == 0 && c.y() >= -6;
        AirSight s = new AirSight(world(carved, c -> false), new Cell(0, 0, 0), 16, 16, 4, 5000);
        while (!s.step(500)) { }
        Cell cave = s.deepestCave(2, Set.of(), 3);
        assertNotNull(cave);
        assertNull(s.deepestCave(2, Set.of(cave), 3));
    }

    @Test
    void itStopsAtTheCellLimit() {
        AirSight s = new AirSight(world(c -> false, c -> false), new Cell(0, 0, 0), 40, 40, 4, 300);
        while (!s.step(100)) { }
        assertTrue(s.visited() <= 300);
    }
}
