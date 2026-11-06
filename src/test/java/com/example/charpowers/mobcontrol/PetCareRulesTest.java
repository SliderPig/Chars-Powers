package com.example.charpowers.mobcontrol;

import com.example.charpowers.cloning.model.PostRules;
import com.example.charpowers.mobcontrol.model.PetCareRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PetCareRulesTest {
    @Test
    void meatEatersTakeMeatAndPlantEatersPlants() {
        // zombie pet: meat yes, bread no
        assertTrue(PetCareRules.accepts("zombie", false, false, true, true, false));
        assertFalse(PetCareRules.accepts("zombie", false, false, true, false, false));
        // cow: wheat (its breeding food) yes, carrot (plant) yes, beef no
        assertTrue(PetCareRules.accepts("cow", true, true, false, false, false));
        assertTrue(PetCareRules.accepts("cow", true, false, true, false, false));
        assertFalse(PetCareRules.accepts("cow", true, false, true, true, false));
        // wolf: meat yes, apple no, fish allowed
        assertTrue(PetCareRules.accepts("wolf", true, false, true, true, false));
        assertFalse(PetCareRules.accepts("wolf", true, false, true, false, false));
        assertTrue(PetCareRules.accepts("cat", true, false, true, false, true));
        // not food at all
        assertFalse(PetCareRules.accepts("zombie", false, false, false, false, false));
    }

    @Test
    void healingAndRiding() {
        assertEquals(2f, PetCareRules.heal(1));
        assertEquals(8f, PetCareRules.heal(8));
        assertTrue(PetCareRules.ridable("ravager", false, false));
        assertTrue(PetCareRules.ridable("horse", true, true));
        assertFalse(PetCareRules.ridable("horse", true, false));
        assertFalse(PetCareRules.ridable("wolf", false, false));
    }

    @Test
    void guardAndFarmNumbers() {
        assertTrue(PostRules.worthChasing(25, 100));
        assertFalse(PostRules.worthChasing(200, 4));
        assertFalse(PostRules.worthChasing(25, 400));
        assertFalse(PostRules.shouldReturn(4));
        assertTrue(PostRules.shouldReturn(30));
        assertTrue(PostRules.inField(12, 3, -12));
        assertFalse(PostRules.inField(13, 0, 0));
        assertTrue(PostRules.ripe(7, 7));
        assertFalse(PostRules.ripe(6, 7));
        assertFalse(PostRules.ripe(0, 0));
    }
}
