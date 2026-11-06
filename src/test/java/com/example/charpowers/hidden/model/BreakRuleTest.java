package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BreakRuleTest {
    // enabled, air, fluidOnly, hasBlockEntity, hardness
    @Test void dirtBreaks()       { assertTrue(BreakRule.canWreck(true, false, false, false, 0.5f)); }
    @Test void chestNever()       { assertFalse(BreakRule.canWreck(true, false, false, true, 2.5f)); }
    @Test void obsidianNever()    { assertFalse(BreakRule.canWreck(true, false, false, false, 50f)); }
    @Test void bedrockNever()     { assertFalse(BreakRule.canWreck(true, false, false, false, -1f)); }
    @Test void waterNever()       { assertFalse(BreakRule.canWreck(true, false, true, false, 100f)); }
    @Test void airNever()         { assertFalse(BreakRule.canWreck(true, true, false, false, 0f)); }
    @Test void switchedOffNever() { assertFalse(BreakRule.canWreck(false, false, false, false, 0.5f)); }
}
