package com.example.charpowers.authority.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class AuthorityTest {
    private static String key(int x, int y, int z) { return x + "," + y + "," + z; }

    private static ThroneRule.Blocks blocks(Set<String> gold, String diamond) {
        return new ThroneRule.Blocks() {
            public boolean gold(int x, int y, int z) { return gold.contains(key(x, y, z)); }

            public boolean diamond(int x, int y, int z) { return key(x, y, z).equals(diamond); }
        };
    }

    // back = which way the back is
    private static Set<String> goldThrone(int bx, int bz) {
        Set<String> s = new HashSet<>();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) s.add(key(x, 0, z));
        s.add(key(-bz, 1, bx));
        s.add(key(bz, 1, -bx));
        for (int y = 1; y <= 3; y++) s.add(key(bx, y, bz));
        return s;
    }

    @Test
    void goldThroneWorksInEveryDirectionAndNeedsAllFourteen() {
        for (int d = 0; d < 4; d++) {
            Set<String> s = goldThrone(ThroneRule.BACK[d][0], ThroneRule.BACK[d][1]);
            assertEquals(14, s.size());
            assertEquals(d, ThroneRule.gold(blocks(s, ""), 0, 0, 0));
        }
        Set<String> missing = goldThrone(0, -1);
        missing.remove(key(0, 3, -1));
        assertEquals(-1, ThroneRule.gold(blocks(missing, ""), 0, 0, 0));
    }

    @Test
    void superiorThroneNeedsItsDiamond() {
        Set<String> s = new HashSet<>();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) s.add(key(x, 0, z));
        for (int y = 1; y <= 2; y++) { s.add(key(1, y, 0)); s.add(key(-1, y, 0)); }
        for (int y = 1; y <= 5; y++) s.add(key(0, y, -1));
        for (int y = 1; y <= 3; y++) { s.add(key(1, y, -1)); s.add(key(-1, y, -1)); }
        assertEquals(40, s.size());
        assertEquals(0, ThroneRule.superior(blocks(s, key(0, 6, -1)), 0, 0, 0));
        assertEquals(-1, ThroneRule.superior(blocks(s, ""), 0, 0, 0));
        assertEquals(-1, ThroneRule.gold(blocks(goldThrone(0, -1), ""), 5, 0, 5));
    }

    @Test
    void rangeAndTimeGrowWithMastery() {
        assertEquals(100, AuthorityRules.range(1));
        assertEquals(100, AuthorityRules.range(10));
        assertEquals(500, AuthorityRules.range(50));
        assertEquals(1000, AuthorityRules.range(100));
        assertEquals(200, AuthorityRules.ticks(1));
        assertEquals(1200, AuthorityRules.ticks(100));
    }
}
