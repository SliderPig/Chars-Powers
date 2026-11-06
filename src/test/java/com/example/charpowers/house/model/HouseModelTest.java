package com.example.charpowers.house.model;

import com.example.charpowers.mastery.model.MasteryMath;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HouseModelTest {
    // ---- rules

    @Test
    void masteryReachesOneHundredAtOneHundredThousandBlocks() {
        long points = 0;
        for (long i = 0; i < HouseRules.MASTERY_BLOCKS; i++) points += HouseRules.masteryGain(i);
        assertEquals(MasteryMath.POINTS_TO_MAX, points);
        assertEquals(100, MasteryMath.levelOf(points));
        assertEquals(0, HouseRules.masteryGain(HouseRules.MASTERY_BLOCKS + 5));
    }

    @Test
    void masteryIsNotThereTooEarly() {
        long points = 0;
        for (long i = 0; i < HouseRules.MASTERY_BLOCKS - 2000; i++) points += HouseRules.masteryGain(i);
        assertTrue(MasteryMath.levelOf(points) < 100);
    }

    @Test
    void housesTakeTenToThirtySeconds() {
        for (int blocks : new int[] {100, 1600, 4000, 8400, 20000}) {
            int seconds = HouseRules.secondsFor(blocks);
            assertTrue(seconds >= 10 && seconds <= 30, blocks + " took " + seconds);
            int ticks = (int) Math.ceil((double) blocks / HouseRules.perTick(blocks, 100));
            assertTrue(ticks <= 30 * 20 + 1, blocks + " takes " + ticks + " ticks");
        }
        assertTrue(HouseRules.perTick(8400, 100) <= 15, "too many blocks a tick");
    }

    @Test
    void locks() {
        assertNull(HouseRules.lockReason(1, false, 1, false));
        assertEquals("Mastery 25", HouseRules.lockReason(25, false, 24, false));
        assertNull(HouseRules.lockReason(25, false, 25, false));
        assertEquals("Awakening", HouseRules.lockReason(1, true, 100, false));
        assertNull(HouseRules.lockReason(1, true, 100, true));
    }

    @Test
    void clockReadsLikeAClock() {
        assertEquals("10:00", HouseRules.clock(12000));
        assertEquals("0:01", HouseRules.clock(1));
        assertEquals("0:00", HouseRules.clock(0));
    }

    // ---- turning

    @Test
    void oneTurnIsClockwiseLikeTheGame() {
        // the game's CLOCKWISE_90: east (1, 0) goes to south (0, 1), south goes to west
        assertEquals(0, HouseTurn.x(1, 0, 1));
        assertEquals(1, HouseTurn.z(1, 0, 1));
        assertEquals(-1, HouseTurn.x(0, 1, 1));
        assertEquals(0, HouseTurn.z(0, 1, 1));
    }

    @Test
    void fourTurnsComeBack() {
        for (int t = 0; t < 4; t++) {
            int x = 7, z = -3;
            for (int i = 0; i < 4; i++) {
                int nx = HouseTurn.x(x, z, 1), nz = HouseTurn.z(x, z, 1);
                x = nx;
                z = nz;
            }
            assertEquals(7, x);
            assertEquals(-3, z);
        }
    }

    @Test
    void everyCellStaysInsideTheRectAndNoneCollide() {
        int w = 7, d = 4, cx = 100, cz = -50;
        for (int turns = 0; turns < 4; turns++) {
            int[] r = HouseTurn.rect(w, d, cx, cz, turns);
            Set<String> seen = new HashSet<>();
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    int wx = HouseTurn.worldX(x, z, w, d, cx, turns), wz = HouseTurn.worldZ(x, z, w, d, cz, turns);
                    assertTrue(wx >= r[0] && wx <= r[2] && wz >= r[1] && wz <= r[3], "outside at turn " + turns);
                    assertTrue(seen.add(wx + "," + wz), "two cells on one spot");
                }
            }
            int rw = r[2] - r[0] + 1, rd = r[3] - r[1] + 1;
            assertEquals(turns % 2 == 0 ? w : d, rw);
            assertEquals(turns % 2 == 0 ? d : w, rd);
        }
    }

    @Test
    void theFrontDoorLooksBackAtYou() {
        // looking south (0) you want the front facing north: two turns from the file's south
        assertEquals(2, HouseTurn.facingPlayer(0));
        // looking north (2): no turn at all
        assertEquals(0, HouseTurn.facingPlayer(2));
        // looking east (3): the front has to face west, one turn clockwise from south
        assertEquals(1, HouseTurn.facingPlayer(3));
        // looking west (1): front faces east
        assertEquals(3, HouseTurn.facingPlayer(1));
    }

    // ---- the catalogue on disk

    @Test
    void everyHouseInTheListHasAFileAndAPicture() throws Exception {
        assertEquals(10, HouseList.BUILT_IN.size());
        long awakened = HouseList.BUILT_IN.stream().filter(HouseList.Entry::awakened).count();
        assertEquals(5, awakened);
        assertEquals(List.of(1, 25, 50, 75, 100), HouseList.BUILT_IN.stream().filter(e -> !e.awakened()).map(HouseList.Entry::level).toList());
        for (HouseList.Entry e : HouseList.BUILT_IN) {
            Path file = Path.of("src/main/resources/data/charpowers/structures/houses/" + e.id() + ".nbt");
            assertTrue(Files.size(file) > 1000, e.id() + " has no file");
            try (var in = new java.util.zip.GZIPInputStream(Files.newInputStream(file))) {
                assertEquals(10, in.read(), e.id() + " isn't an NBT compound");
            }
            assertTrue(Files.exists(Path.of("src/main/resources/assets/charpowers/textures/gui/houses/" + e.id() + ".png")), e.id() + " has no picture");
        }
    }

    // ---- is it a castle?

    private static final int WALL = 8, TOWER = 13;

    // A square of walls `side` long, with towers in the given corners and maybe an arch in the south wall.
    private static CastleRule castle(int side, int towers, boolean door, int wallHigh) {
        CastleRule r = new CastleRule(side, side);
        int mid = side / 2;
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                boolean edge = x == 0 || z == 0 || x == side - 1 || z == side - 1;
                if (!edge) {
                    r.solid(x, 0, z); // the courtyard floor
                    continue;
                }
                for (int y = 0; y < wallHigh; y++) {
                    if (door && x == mid && z == side - 1 && y >= 1 && y <= 3) continue;
                    r.solid(x, y, z);
                }
            }
        }
        int[][] corners = {{0, 0}, {side - 3, 0}, {0, side - 3}, {side - 3, side - 3}};
        for (int i = 0; i < towers; i++) {
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    for (int y = 0; y < TOWER; y++) r.solid(corners[i][0] + dx, y, corners[i][1] + dz);
                }
            }
        }
        return r;
    }

    private static void assertSays(List<String> missing, String word) {
        assertTrue(missing.stream().anyMatch(s -> s.toLowerCase().contains(word)), "expected '" + word + "' in " + missing);
    }

    @Test
    void aProperCastlePasses() {
        assertEquals(List.of(), castle(24, 2, true, WALL).missing());
    }

    @Test
    void noTowersIsToldHowManyAreMissing() {
        List<String> missing = castle(24, 0, false, WALL).missing();
        assertSays(missing, "needs 2 more towers");
    }

    @Test
    void oneTowerNeedsOneMore() {
        assertSays(castle(24, 1, false, WALL).missing(), "needs 1 more tower");
    }

    @Test
    void noDoorIsNoticed() {
        assertSays(castle(24, 2, false, WALL).missing(), "doorway");
    }

    @Test
    void lowWallsAreNotWalls() {
        assertSays(castle(24, 0, false, 3).missing(), "at least 5");
    }

    @Test
    void aSolidCubeIsALump() {
        CastleRule r = new CastleRule(16, 16);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 15; y++) r.solid(x, y, z);
        List<String> missing = r.missing();
        assertSays(missing, "solid lump");
        assertSays(missing, "tower");
    }

    @Test
    void aSingleLongWallIsNotEnough() {
        CastleRule r = new CastleRule(40, 3);
        for (int x = 0; x < 40; x++) for (int y = 0; y < 7; y++) r.solid(x, y, 1);
        assertSays(r.missing(), "2 sides");
    }

    @Test
    void twoSidesWithoutACourtyardCount() {
        // an L of walls, towers at its ends, a door in one arm
        CastleRule r = new CastleRule(20, 20);
        for (int i = 0; i < 20; i++) {
            for (int y = 0; y < 7; y++) {
                if (!(i == 10 && y >= 1 && y <= 3)) r.solid(i, y, 19);
                r.solid(0, y, i);
            }
        }
        for (int[] c : new int[][] {{0, 0}, {18, 18}}) {
            for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) for (int y = 0; y < 11; y++) r.solid(c[0] + dx, y, c[1] + dz);
        }
        assertEquals(List.of(), r.missing());
    }

    @Test
    void aTallLineOfWallIsNotATower() {
        CastleRule r = castle(24, 0, true, WALL);
        for (int x = 3; x < 20; x++) for (int y = 0; y < TOWER; y++) r.solid(x, y, 0);
        assertSays(r.missing(), "tower");
    }
}
