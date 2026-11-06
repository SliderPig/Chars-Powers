package com.example.charpowers.game;

import com.example.charpowers.game.model.GatePlan;
import com.example.charpowers.game.model.GatePlan.Kind;
import com.example.charpowers.game.model.GatePlan.Room;
import com.example.charpowers.game.model.GatePlan.Theme;
import com.example.charpowers.game.model.GateRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatePlanTest {
    @Test
    void everyRankEveryThemeMakesAWalkableChainEndingInABoss() {
        for (int rank = 0; rank < 6; rank++) {
            for (Theme theme : Theme.values()) {
                for (long seed = 0; seed < 30; seed++) {
                    List<Room> rooms = GatePlan.plan(rank, theme, new Random(seed));
                    assertEquals(GateRules.rooms(rank), rooms.size());
                    assertEquals(Kind.BOSS, rooms.get(rooms.size() - 1).kind());
                    assertTrue(rooms.stream().filter(r -> r.kind() == Kind.TRAP).count() <= 1);
                    for (Room room : rooms) {
                        // both doors and the lane between them are open ground
                        for (int x = -room.halfX(); x <= room.halfX(); x++) assertTrue(GatePlan.walkable(room, x, 0), room.toString());
                        assertFalse(GatePlan.walkable(room, room.halfX() + 1, 0));
                        assertTrue(room.halfX() >= 6 && room.halfZ() >= 3);
                    }
                }
            }
        }
    }

    @Test
    void roomsDifferInSizeAndKind() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (long seed = 0; seed < 40; seed++) {
            for (Room r : GatePlan.plan(4, Theme.ELF_RUIN, new Random(seed))) seen.add(r.kind() + "/" + r.shape() + "/" + r.halfX());
        }
        assertTrue(seen.size() >= 15, "only " + seen.size());
    }

    @Test
    void centresLeaveACorridorBetweenDoors() {
        List<Room> rooms = GatePlan.plan(3, Theme.GOBLIN_CAVE, new Random(1));
        int[] c = GatePlan.centres(rooms);
        for (int i = 1; i < c.length; i++) assertEquals(GatePlan.CORRIDOR, (c[i] - rooms.get(i).halfX()) - (c[i - 1] + rooms.get(i - 1).halfX()) - 1);
    }

    @Test
    void everyThemeHasMobsAndABoss() {
        for (Theme t : Theme.values()) {
            for (int rank = 0; rank < 6; rank++) {
                assertFalse(GatePlan.mobs(t, rank).isEmpty());
                assertTrue(GatePlan.boss(t, rank, new Random(rank)).startsWith("charpowers:"));
            }
        }
    }

    @Test
    void coverStaysOffTheLane() {
        for (Room r : GatePlan.plan(3, Theme.ELF_RUIN, new Random(9))) {
            for (int[] c : GatePlan.cover(r, new Random(3))) assertTrue(Math.abs(c[1]) > 2);
        }
    }

    @Test
    void partiesFaceMoreAndLootGetsBetter() {
        assertTrue(GatePlan.count(2, 1, 3, Kind.ARENA) > GatePlan.count(2, 1, 1, Kind.ARENA));
        assertEquals(5, GatePlan.chestLoot(3, new Random(1), 5).size());
        assertTrue(GatePlan.chestLoot(5, new Random(1), 40).stream().anyMatch(i -> i.id().contains("netherite") || i.id().contains("totem")));
    }

    @Test
    void lowRanksNeverMeetTheHardThemes() {
        for (int i = 0; i < 100; i++) {
            Theme t = GatePlan.themeFor(0, new Random(i), false);
            assertTrue(GatePlan.minRank(t) == 0, t.toString());
        }
        assertTrue(GatePlan.eligible(5).contains(Theme.DRAGON_KEEP));
        assertTrue(!GatePlan.eligible(0).contains(Theme.DEMON_CASTLE));
        assertTrue(!GatePlan.eligible(5).contains(Theme.GOBLIN_CAVE));
    }

    @Test
    void sealedGatesAreHardOnes() {
        assertEquals(Theme.DRAGON_KEEP, GatePlan.themeFor(4, new Random(1), true));
        for (int rank = 0; rank < 4; rank++) {
            Theme t = GatePlan.themeFor(rank, new Random(rank), true);
            assertTrue(GatePlan.minRank(t) >= Math.max(0, rank - 2), t.toString());
        }
    }

    @Test
    void everyThemeHasAWarningAndTitle() {
        for (Theme t : Theme.values()) {
            assertTrue(GatePlan.warning(t).startsWith("Warning"));
            assertTrue(GatePlan.title(t).length() > 3);
        }
    }

    @Test
    void mazeAlwaysHasAWayThrough() {
        Room maze = new Room(Kind.MAZE, GatePlan.Shape.SQUARE, 11, 7, 6, 1, false);
        int[] gapSide = {0};
        for (int[] wall : GatePlan.mazeWalls(maze)) {
            boolean gapAtTop = wall[2] < maze.halfZ() - 1; // wall stops short of the +z end
            boolean gapAtBottom = wall[1] > -maze.halfZ() + 1;
            assertTrue(gapAtTop ^ gapAtBottom);
            assertTrue((gapAtTop ? maze.halfZ() - 1 - wall[2] : wall[1] - (-maze.halfZ() + 1)) >= 3);
        }
        assertTrue(GatePlan.mazeWalls(maze).size() >= 3);
    }

    @Test
    void specialRoomsMatchTheirThemes() {
        boolean terminal = false, jars = false;
        for (long seed = 0; seed < 200; seed++) {
            for (Room r : GatePlan.plan(4, Theme.STATION, new Random(seed))) terminal |= r.kind() == Kind.TERMINAL;
            for (Room r : GatePlan.plan(4, Theme.CRYPT, new Random(seed))) jars |= r.kind() == Kind.PHYLACTERY;
            for (Room r : GatePlan.plan(4, Theme.GOBLIN_CAVE, new Random(seed))) assertTrue(r.kind() != Kind.TERMINAL && r.kind() != Kind.PHYLACTERY);
        }
        assertTrue(terminal && jars);
    }
}
