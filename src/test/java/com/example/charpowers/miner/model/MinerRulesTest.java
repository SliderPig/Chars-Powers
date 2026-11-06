package com.example.charpowers.miner.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Set;

class MinerRulesTest {
    @Test
    void efficiencyEveryTwentyFiveLevels() {
        assertEquals(0, MinerRules.efficiency(24));
        assertEquals(1, MinerRules.efficiency(25));
        assertEquals(4, MinerRules.efficiency(100));
        assertEquals(0, MinerRules.efficiencySpeed(0));
        assertEquals(17, MinerRules.efficiencySpeed(4));
    }

    @Test
    void chunksFormASquare() {
        long a = MinerRules.chunk(-1, -1), b = MinerRules.chunk(0, -1), c = MinerRules.chunk(-1, 0), d = MinerRules.chunk(0, 0);
        assertTrue(MinerRules.twoByTwo(Set.of(a, b, c, d)));
        assertFalse(MinerRules.twoByTwo(Set.of(a, b, c)));
        assertFalse(MinerRules.twoByTwo(Set.of(a, b, c, MinerRules.chunk(5, 5))));
    }

    @Test
    void junkStaysOnTheGroundAndOresDoNot() {
        assertTrue(MinerRules.isJunk("dirt"));
        assertTrue(MinerRules.isJunk("cobbled_deepslate"));
        assertFalse(MinerRules.isJunk("diamond"));
        assertFalse(MinerRules.isJunk("raw_iron"));
    }

    @Test
    void rarerOresComeFirst() {
        assertTrue(MinerRules.oreRank("diamond_ore") > MinerRules.oreRank("copper_ore"));
        assertTrue(MinerRules.oreRank("ancient_debris") > MinerRules.oreRank("diamond_ore"));
        // a far diamond beats a near copper; same kind goes nearest first
        assertTrue(MinerRules.compareRarity(8, 900, 2, 1) < 0);
        assertTrue(MinerRules.compareRarity(2, 4, 2, 9) < 0);
    }
}
