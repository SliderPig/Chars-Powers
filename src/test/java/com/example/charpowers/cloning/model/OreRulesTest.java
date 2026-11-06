package com.example.charpowers.cloning.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OreRulesTest {
    @Test
    void wantsRarerOresEvenIfFurtherAway() {
        assertTrue(OreRules.rank("minecraft:deepslate_diamond_ore") > OreRules.rank("minecraft:gold_ore"));
        assertTrue(OreRules.rank("minecraft:ancient_debris") > OreRules.rank("minecraft:diamond_ore"));
        assertTrue(OreRules.rank("minecraft:iron_ore") > OreRules.rank("minecraft:coal_ore"));
        assertTrue(OreRules.worth(8, 8) > OreRules.worth(1, 1));
        assertTrue(OreRules.worth(8, 1) > OreRules.worth(8, 8));
    }
}
