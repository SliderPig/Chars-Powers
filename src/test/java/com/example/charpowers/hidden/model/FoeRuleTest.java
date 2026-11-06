package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FoeRuleTest {
    // living, self, alive, armourStand, creativeOrSpectator, mine, allied, alliedOwnersPet
    @Test void otherPlayerIsHit()   { assertTrue(FoeRule.hittable(new FoeRule.Facts(true, false, true, false, false, false, false, false))); }
    @Test void teammateIsNot()      { assertFalse(FoeRule.hittable(new FoeRule.Facts(true, false, true, false, false, false, true, false))); }
    @Test void teammatesPetIsNot()  { assertFalse(FoeRule.hittable(new FoeRule.Facts(true, false, true, false, false, false, false, true))); }
    @Test void ownHelperIsNot()     { assertFalse(FoeRule.hittable(new FoeRule.Facts(true, false, true, false, false, true, false, false))); }
    @Test void creativeIsNot()      { assertFalse(FoeRule.hittable(new FoeRule.Facts(true, false, true, false, true, false, false, false))); }
    @Test void selfAndStandsAreNot() {
        assertFalse(FoeRule.hittable(new FoeRule.Facts(true, true, true, false, false, false, false, false)));
        assertFalse(FoeRule.hittable(new FoeRule.Facts(true, false, true, true, false, false, false, false)));
    }
    @Test void deadOrNotLivingIsNot() {
        assertFalse(FoeRule.hittable(new FoeRule.Facts(true, false, false, false, false, false, false, false)));
        assertFalse(FoeRule.hittable(new FoeRule.Facts(false, false, true, false, false, false, false, false)));
    }
}
