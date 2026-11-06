package com.example.charpowers.cloning.model;

import static org.junit.jupiter.api.Assertions.*;

import com.example.charpowers.cloning.model.RoutePlanner.Cell;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class VeinTest {
    private static Set<Cell> ores(Cell... cells) {
        return new HashSet<>(List.of(cells));
    }

    @Test
    void anOreTouchingTheOneThatBrokeIsNext() {
        Set<Cell> world = ores(new Cell(1, 0, 0));
        Vein vein = new Vein();
        vein.broke(new Cell(0, 0, 0), world::contains);
        assertEquals(new Cell(1, 0, 0), vein.next(new Cell(0, 0, 0), world::contains));
    }

    @Test
    void cornersCountAsTouching() {
        Set<Cell> world = ores(new Cell(1, 1, 1));
        Vein vein = new Vein();
        vein.broke(new Cell(0, 0, 0), world::contains);
        assertEquals(new Cell(1, 1, 1), vein.next(new Cell(0, 0, 0), world::contains));
    }

    @Test
    void anOreThatDoesNotTouchIsAnotherVein() {
        Set<Cell> world = ores(new Cell(2, 0, 0));
        Vein vein = new Vein();
        vein.broke(new Cell(0, 0, 0), world::contains);
        assertNull(vein.next(new Cell(0, 0, 0), world::contains));
        assertTrue(vein.isEmpty());
    }

    @Test
    void takesTheNearestPartFirst() {
        Set<Cell> world = ores(new Cell(1, 0, 0), new Cell(-1, 0, 0));
        Vein vein = new Vein();
        vein.broke(new Cell(0, 0, 0), world::contains);
        assertEquals(new Cell(-1, 0, 0), vein.next(new Cell(-3, 0, 0), world::contains));
    }

    @Test
    void followsAVeinRightToItsEndHoweverFarItGoes() {
        Set<Cell> world = new HashSet<>();
        for (int x = 1; x <= 30; x++) world.add(new Cell(x, 0, 0));
        Vein vein = new Vein();
        Cell broke = new Cell(0, 0, 0);
        int mined = 0;
        while (true) {
            vein.broke(broke, world::contains);
            Cell next = vein.next(broke, world::contains);
            if (next == null) break;
            world.remove(next);
            broke = next;
            mined++;
        }
        assertEquals(30, mined);
        assertTrue(world.isEmpty());
    }

    @Test
    void forgetsPartsThatAreGoneOrItWasToldToLeave() {
        Set<Cell> world = ores(new Cell(1, 0, 0), new Cell(-1, 0, 0));
        Vein vein = new Vein();
        vein.broke(new Cell(0, 0, 0), world::contains);
        world.remove(new Cell(1, 0, 0));
        assertEquals(new Cell(-1, 0, 0), vein.next(new Cell(1, 0, 0), world::contains));
        world.remove(new Cell(-1, 0, 0));
        assertNull(vein.next(new Cell(1, 0, 0), world::contains));
        assertTrue(vein.isEmpty());
    }
}
