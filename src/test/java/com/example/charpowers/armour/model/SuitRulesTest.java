package com.example.charpowers.armour.model;

import static org.junit.jupiter.api.Assertions.*;

import com.example.charpowers.armour.model.SuitRules.Stage;
import org.junit.jupiter.api.Test;

class SuitRulesTest {
    @Test
    void batteryDrainsSlowerWithMastery() {
        assertEquals(1.0, SuitRules.drainPerMinute(1), 0.001);
        assertEquals(0.5, SuitRules.drainPerMinute(100), 0.001);
    }

    @Test
    void moduleSlotsByMastery() {
        assertEquals(1, SuitRules.moduleSlots(24));
        assertEquals(2, SuitRules.moduleSlots(25));
        assertEquals(3, SuitRules.moduleSlots(50));
        assertEquals(4, SuitRules.moduleSlots(75));
    }

    @Test
    void goingCrazyByHours() {
        assertEquals(Stage.FINE, SuitRules.stage(3599, 1));
        assertEquals(Stage.FLICKER, SuitRules.stage(3600, 1));
        assertEquals(Stage.WHISPERS, SuitRules.stage(7200, 1));
        assertEquals(Stage.FAKE_MOBS, SuitRules.stage(10800, 1));
        assertEquals(Stage.OWN_MIND, SuitRules.stage(14400, 1));
        // mastery 100 takes twice as long
        assertEquals(Stage.FINE, SuitRules.stage(7199, 100));
        assertEquals(Stage.FLICKER, SuitRules.stage(7200, 100));
        assertTrue(SuitRules.atLeast(Stage.WHISPERS, Stage.FLICKER));
        assertFalse(SuitRules.atLeast(Stage.FLICKER, Stage.WHISPERS));
    }
}
