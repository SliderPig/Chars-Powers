package com.example.charpowers.cat;

import com.example.charpowers.cat.model.CatRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatRulesTest {
    @Test
    void sitsWhenYouStopAndNapsLater() {
        assertEquals(CatRules.Pose.STAND, CatRules.pose(2, 10, 0, 0.5, CatRules.Pose.STAND));
        assertEquals(CatRules.Pose.SIT, CatRules.pose(2, 100, 0, 0.5, CatRules.Pose.STAND));
        assertEquals(CatRules.Pose.STAND, CatRules.pose(8, 500, 0, 0.0, CatRules.Pose.SIT));
        assertEquals(CatRules.Pose.SIT, CatRules.pose(2, 500, 100, 0.0, CatRules.Pose.SIT));
        assertEquals(CatRules.Pose.NAP, CatRules.pose(2, 500, 500, 0.0, CatRules.Pose.SIT));
        assertEquals(CatRules.Pose.SIT, CatRules.pose(2, 500, 500, 0.9, CatRules.Pose.SIT));
        assertEquals(CatRules.Pose.NAP, CatRules.pose(2, 500, 0, 0.9, CatRules.Pose.NAP));
        assertEquals(CatRules.Pose.STAND, CatRules.pose(2, 3, 0, 0.0, CatRules.Pose.NAP));
    }

    @Test
    void travelByDistance() {
        assertEquals(0, CatRules.travel(2));
        assertEquals(1, CatRules.travel(6));
        assertEquals(2, CatRules.travel(12));
        assertEquals(3, CatRules.travel(30));
    }

    @Test
    void purrsAndWanting() {
        assertEquals(CatRules.PURR_MIN, CatRules.nextPurr(0));
        assertEquals(CatRules.PURR_MIN + CatRules.PURR_SPREAD, CatRules.nextPurr(1));
        assertTrue(CatRules.wanted(true, true, true, false));
        assertFalse(CatRules.wanted(false, true, true, false));
        assertFalse(CatRules.wanted(true, false, true, false));
        assertFalse(CatRules.wanted(true, true, false, false));
        assertFalse(CatRules.wanted(true, true, true, true));
        assertTrue(CatRules.ownerStill(0.0));
        assertFalse(CatRules.ownerStill(0.04));
    }
}
