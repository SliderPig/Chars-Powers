package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrantRuleTest {
    @Test
    void removedPowerWipes() {
        // something took the Palladium power away (DNA Normalizer, GabeUp vial, /superpower remove): lose everything
        assertEquals(GrantRule.Action.WIPE, GrantRule.onCheck(false, false));
    }

    @Test
    void normalCasesDoNothing() {
        assertEquals(GrantRule.Action.NOTHING, GrantRule.onCheck(true, false));
        assertEquals(GrantRule.Action.NOTHING, GrantRule.onCheck(false, true), "never adds the power back by itself");
        assertEquals(GrantRule.Action.NOTHING, GrantRule.onCheck(true, true));
    }
}
