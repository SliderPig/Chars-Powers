package com.example.charpowers.game.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GateRulesTest {
    @Test
    void higherRanksAreBiggerAndPayMore() {
        for (int rank = 1; rank < GateRules.RANKS.length; rank++) {
            assertTrue(GateRules.rooms(rank) > GateRules.rooms(rank - 1));
            assertTrue(GateRules.mobsInRoom(rank, 0) > GateRules.mobsInRoom(rank - 1, 0));
            assertTrue(GateRules.bossHealth(rank) > GateRules.bossHealth(rank - 1));
            assertTrue(GateRules.coins(rank) > GateRules.coins(rank - 1));
            assertTrue(GateRules.points(rank) > GateRules.points(rank - 1));
        }
    }

    @Test
    void masteryUnlocksRanks() {
        assertEquals(0, GateRules.topRank(1));
        assertEquals(1, GateRules.topRank(15));
        assertEquals(5, GateRules.topRank(100));
    }

    @Test
    void everyRankHasMobs() {
        for (int rank = 0; rank < GateRules.RANKS.length; rank++) assertFalse(GateRules.mobs(rank).isEmpty());
        assertEquals(GateRules.mobs(5), GateRules.mobs(99));
    }

    @Test
    void theCompassPointsTheRightWay() {
        assertEquals("north", GateRules.compass(0, -10));
        assertEquals("south", GateRules.compass(0, 10));
        assertEquals("east", GateRules.compass(10, 0));
        assertEquals("west", GateRules.compass(-10, 0));
        assertEquals("north-east", GateRules.compass(10, -10));
    }
}
