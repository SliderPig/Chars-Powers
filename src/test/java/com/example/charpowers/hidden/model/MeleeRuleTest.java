package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeleeRuleTest {
    // powerHit, directIsAttacker, meleeDamageType
    @Test void aRealPunch()         { assertTrue(MeleeRule.isPunch(false, true, true)); }
    @Test void notAPowersHit()      { assertFalse(MeleeRule.isPunch(true, true, true)); }
    @Test void notAnArrow()         { assertFalse(MeleeRule.isPunch(false, false, true)); } // arrow: direct = the arrow
    @Test void notThorns()          { assertFalse(MeleeRule.isPunch(false, true, false)); } // thorns: direct = holder, type THORNS
}
