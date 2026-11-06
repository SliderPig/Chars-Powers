package com.example.charpowers.necromancy.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UndeadRulesTest {
    @Test
    void masteryMakesThemStronger() {
        assertEquals(10, UndeadRules.health(1), 0.001);
        assertTrue(UndeadRules.health(100) > 29);
        assertTrue(UndeadRules.damageScale(100) > UndeadRules.damageScale(1));
    }

    @Test
    void lightChangesTheirStrength() {
        assertEquals(0.25, UndeadRules.lightScale(true, false, 74));
        assertEquals(0.75, UndeadRules.lightScale(true, false, 75));
        assertEquals(1.5, UndeadRules.lightScale(false, true, 1));
        assertEquals(1.0, UndeadRules.lightScale(false, false, 1));
    }

    @Test
    void limits() {
        assertEquals(1, UndeadRules.revivalsAtOnce(49));
        assertEquals(2, UndeadRules.revivalsAtOnce(50));
        assertEquals(2400, UndeadRules.riotTicks(99));
        assertEquals(4800, UndeadRules.riotTicks(100));
        assertEquals(5, UndeadRules.room(0, 5));
        assertEquals(2, UndeadRules.room(28, 5));
        assertEquals(0, UndeadRules.room(30, 5));
    }
}
