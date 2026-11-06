package com.example.charpowers.cloning.model;

import static org.junit.jupiter.api.Assertions.*;

import com.example.charpowers.cloning.model.RoutePlanner.Cell;
import com.example.charpowers.cloning.model.RoutePlanner.Kind;
import com.example.charpowers.cloning.model.RoutePlanner.Step;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class RoutePlannerTest {
    private static final char AIR = '.', STONE = '#', LAVA = 'L', WATER = 'W', HARD = 'H';

    // A little world: anything not set is stone below y=0 and air from y=0 up.
    private static final class World implements RoutePlanner.Terrain {
        final Map<Long, Character> blocks = new HashMap<>();

        World set(int x, int y, int z, char c) {
            blocks.put(RoutePlanner.key(x, y, z), c);
            return this;
        }

        World box(int x0, int y0, int z0, int x1, int y1, int z1, char c) {
            for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, c);
            return this;
        }

        char at(int x, int y, int z) {
            Character c = blocks.get(RoutePlanner.key(x, y, z));
            return c != null ? c : y < 0 ? STONE : AIR;
        }

        @Override
        public boolean open(int x, int y, int z) {
            char c = at(x, y, z);
            return c == AIR || c == WATER;
        }

        @Override
        public boolean floor(int x, int y, int z) {
            char c = at(x, y, z);
            return c == STONE || c == HARD;
        }

        @Override
        public boolean water(int x, int y, int z) {
            return at(x, y, z) == WATER;
        }

        @Override
        public double digCost(int x, int y, int z) {
            char c = at(x, y, z);
            if (c == AIR || c == WATER) return 0;
            if (c != STONE) return -1;
            for (int[] d : new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                if (at(x + d[0], y + d[1], z + d[2]) == LAVA) return -1;
            }
            return 8;
        }

        @Override
        public boolean canFill(int x, int y, int z) {
            char c = at(x, y, z);
            return c == AIR || c == WATER;
        }
    }

    private static List<Step> route(World w, Cell from, RoutePlanner.Goal goal, boolean blocks) {
        RoutePlanner.Search search = new RoutePlanner.Search(from, goal, w, 20000, blocks, Set.of());
        assertEquals(RoutePlanner.Status.FOUND, search.run(100000));
        assertFalse(search.partial(), "should reach the goal");
        return search.path();
    }

    private static boolean uses(List<Step> path, Kind kind) {
        return path.stream().anyMatch(s -> s.kind() == kind);
    }

    private static int digs(List<Step> path) {
        return path.stream().mapToInt(s -> s.dig().size()).sum();
    }

    private static final Cell START = new Cell(0, 0, 0);

    @Test
    void flatGroundIsJustWalking() {
        List<Step> path = route(new World(), START, RoutePlanner.near(10, 0, 0, 0, 0), false);
        assertEquals(10, path.size());
        assertEquals(0, digs(path));
        assertEquals(new Cell(10, 0, 0), path.get(9).to());
    }

    @Test
    void cutsCornersDiagonallyLikeBaritone() {
        List<Step> path = route(new World(), START, RoutePlanner.near(5, 0, 5, 0, 0), false);
        assertTrue(path.size() <= 6, "diagonals, not 10 straight steps: " + path.size());
        assertTrue(uses(path, Kind.DIAGONAL));
    }

    @Test
    void walksRoundASmallWallInsteadOfDigging() {
        World w = new World().box(3, 0, -2, 3, 2, 2, STONE);
        List<Step> path = route(w, START, RoutePlanner.near(6, 0, 0, 0, 0), false);
        assertEquals(0, digs(path));
    }

    @Test
    void digsThroughWhenGoingRoundIsTooFar() {
        World w = new World().box(3, 0, -40, 3, 4, 40, STONE);
        List<Step> path = route(w, START, RoutePlanner.near(6, 0, 0, 0, 0), false);
        assertTrue(digs(path) >= 2);
    }

    @Test
    void neverDigsNextToLava() {
        World w = new World().box(-20, 0, -20, 20, 2, 20, STONE).box(0, 0, 0, 0, 1, 0, AIR).set(2, 0, 0, LAVA);
        RoutePlanner.Search search = new RoutePlanner.Search(START, RoutePlanner.near(4, 0, 0, 0, 0), w, 20000, false, Set.of());
        search.run(100000);
        for (Step s : search.path()) {
            for (Cell c : s.dig()) assertNotEquals(new Cell(1, 0, 0), c, "that block touches the lava");
        }
    }

    @Test
    void stepsUpAndFallsDownWhatItCan() {
        World w = new World().box(2, 0, -1, 2, 0, 1, STONE);
        assertTrue(uses(route(w, START, RoutePlanner.near(2, 1, 0, 0, 0), false), Kind.UP));
        World pit = new World().box(2, -3, 0, 4, -1, 0, AIR).box(1, 0, -1, 5, 3, -1, HARD).box(1, 0, 1, 5, 3, 1, HARD);
        List<Step> down = route(pit, START, RoutePlanner.near(3, -3, 0, 0, 0), false);
        assertTrue(uses(down, Kind.FALL));
    }

    @Test
    void wontFallFurtherThanIsSafe() {
        World w = new World().box(1, -10, -3, 8, -1, 3, AIR).box(0, -1, 0, 0, -1, 0, HARD);
        RoutePlanner.Search search = new RoutePlanner.Search(START, RoutePlanner.near(3, -10, 0, 0, 0), w, 3000, false, Set.of());
        search.run(100000);
        for (Step s : search.path()) {
            assertFalse(s.kind() == Kind.FALL && s.from().y() - s.to().y() > RoutePlanner.MAX_FALL, "a fall of " + (s.from().y() - s.to().y()));
        }
    }

    @Test
    void fallsAnyDistanceIntoWater() {
        World w = new World().box(1, -10, 0, 1, -1, 0, AIR).set(1, -10, 0, WATER).box(1, -11, 0, 1, -11, 0, STONE);
        List<Step> path = route(w, START, RoutePlanner.near(1, -10, 0, 0, 0), false);
        assertEquals(1, path.size());
        assertEquals(Kind.FALL, path.get(0).kind());
    }

    @Test
    void digsStraightDownWhenThatsQuickest() {
        assertTrue(uses(route(new World(), START, RoutePlanner.near(0, -3, 0, 0, 0), false), Kind.DIG_DOWN));
    }

    @Test
    void pillarsAndBridgesOnlyWithBlocks() {
        RoutePlanner.Goal up = RoutePlanner.near(0, 3, 0, 0, 0);
        assertTrue(uses(route(new World(), START, up, true), Kind.PILLAR));
        RoutePlanner.Search none = new RoutePlanner.Search(START, up, new World(), 3000, false, Set.of());
        none.run(100000);
        assertFalse(none.path().stream().anyMatch(s -> s.kind() == Kind.PILLAR));

        World gap = new World().box(1, -20, -70, 8, -1, 70, AIR); // too wide to walk round
        List<Step> bridge = route(gap, START, RoutePlanner.near(9, 0, 0, 0, 0), true);
        assertEquals(new Cell(1, -1, 0), bridge.stream().filter(s -> s.place() != null).findFirst().get().place());
        assertTrue(bridge.stream().filter(s -> s.place() != null).count() >= 8, "a bridge over a wide gap");
    }

    @Test
    void miningGoalDigsTheOreOutAndPrefersTheCheapestOverall() {
        RoutePlanner.Goal goal = RoutePlanner.anyOf(List.of(new RoutePlanner.Target(3, -1, 0, 0)));
        List<Step> path = route(new World(), START, goal, false);
        Cell end = path.get(path.size() - 1).to();
        assertTrue(end.equals(new Cell(3, -1, 0)) || end.equals(new Cell(3, -2, 0)), "stands where the ore was: " + end);
        assertTrue(path.stream().anyMatch(s -> s.dig().contains(new Cell(3, -1, 0))), "the ore gets dug on the way in");

        // a rarer ore further off beats a common one close by
        RoutePlanner.Goal two = RoutePlanner.anyOf(List.of(new RoutePlanner.Target(2, 0, 0, 200), new RoutePlanner.Target(8, 0, 0, 0)));
        List<Step> toBest = route(new World(), START, two, false);
        assertEquals(new Cell(8, 0, 0), toBest.get(toBest.size() - 1).to());
    }

    @Test
    void runningOutOfSearchGivesPartOfTheWay() {
        RoutePlanner.Search search = new RoutePlanner.Search(START, RoutePlanner.near(500, 0, 0, 0, 0), new World(), 400, false, Set.of());
        assertEquals(RoutePlanner.Status.FOUND, search.run(100000));
        assertTrue(search.partial());
        assertTrue(search.path().size() >= 5);
    }

    @Test
    void boxedInWithNothingDiggableHasNoRoute() {
        World w = new World().box(-1, -1, -1, 1, 2, 1, HARD).box(0, 0, 0, 0, 1, 0, AIR);
        RoutePlanner.Search search = new RoutePlanner.Search(START, RoutePlanner.near(9, 0, 0, 0, 0), w, 3000, false, Set.of());
        assertEquals(RoutePlanner.Status.NO_ROUTE, search.run(100000));
    }

    @Test
    void favoursThePathItIsAlreadyOn() {
        // two equal ways round; the favoured cells win
        World w = new World().box(3, 0, -1, 3, 3, 1, HARD);
        Set<Long> favored = new java.util.HashSet<>();
        for (int x = 0; x <= 6; x++) favored.add(RoutePlanner.key(x, 0, -2));
        RoutePlanner.Search search = new RoutePlanner.Search(START, RoutePlanner.near(6, 0, 0, 0, 0), w, 20000, false, favored);
        search.run(100000);
        assertTrue(search.path().stream().anyMatch(s -> s.to().z() <= -2), "should go the favoured side");
    }

    @Test
    void fallTimesMatchGameGravity() {
        assertEquals(0, RoutePlanner.fallTicks(0));
        assertTrue(RoutePlanner.fallTicks(1) > 2 && RoutePlanner.fallTicks(1) < 8);
        assertTrue(RoutePlanner.fallTicks(10) > RoutePlanner.fallTicks(5));
    }
}
