package com.example.charpowers.mobcontrol.model;

import static org.junit.jupiter.api.Assertions.*;

import com.example.charpowers.mobcontrol.model.MobRules.Tier;
import org.junit.jupiter.api.Test;

class MobRulesTest {
    @Test
    void biggerMobsLeaveYouAloneLater() {
        assertEquals(Tier.NORMAL, MobRules.tier("zombie", false));
        assertEquals(Tier.TOUGH, MobRules.tier("ravager", false));
        assertEquals(Tier.TOUGH, MobRules.tier("warden", false));
        assertEquals(Tier.BOSS, MobRules.tier("wither", false));
        assertEquals(Tier.BOSS, MobRules.tier("some_modded_boss", true));
        assertTrue(MobRules.leavesYouAlone(Tier.NORMAL, 1));
        assertFalse(MobRules.leavesYouAlone(Tier.TOUGH, 49));
        assertTrue(MobRules.leavesYouAlone(Tier.TOUGH, 50));
        assertFalse(MobRules.leavesYouAlone(Tier.BOSS, 99));
        assertTrue(MobRules.leavesYouAlone(Tier.BOSS, 100));
    }

    @Test
    void masteryGrowsTheNumbers() {
        assertEquals(10, MobRules.commandRange(1));
        assertEquals(50, MobRules.commandRange(100));
        assertEquals(1, MobRules.maxPets(1));
        assertEquals(5, MobRules.maxPets(100));
        assertEquals(600, MobRules.swapTicks(1));
        assertEquals(3600, MobRules.swapTicks(100));
    }

    @Test
    void ridingRules() {
        assertTrue(MobRules.canRide(Tier.NORMAL, false, 1, false));
        assertFalse(MobRules.canRide(Tier.NORMAL, true, 49, false));
        assertTrue(MobRules.canRide(Tier.NORMAL, true, 50, false));
        assertFalse(MobRules.canRide(Tier.BOSS, false, 100, false));
        assertTrue(MobRules.canRide(Tier.BOSS, false, 100, true));
        assertFalse(MobRules.canKeep(Tier.BOSS, false));
        assertFalse(MobRules.canKeep("warden", Tier.TOUGH, 49, false));
        assertTrue(MobRules.canKeep("warden", Tier.TOUGH, 50, false));
        assertFalse(MobRules.canKeep("ender_dragon", Tier.BOSS, 100, true));
        assertTrue(MobRules.canKeep("wither", Tier.BOSS, 100, true));
        assertTrue(MobRules.canKeep(Tier.BOSS, true));
    }
}
