package com.example.charpowers.game;

import com.example.charpowers.game.model.BossRules;
import com.example.charpowers.game.model.Elite;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossRulesTest {
    private static final String[] BOSSES = {"GOBLIN_KING", "HOBGOBLIN_WARLORD", "ELF_MAGE", "ANT_QUEEN", "WOLF_KING", "DEMON_LORD", "LICH_LORD", "SNAKE_QUEEN", "FROST_KING", "WAR_MECH", "TOMB_KING", "YOUNG_DRAGON"};

    @Test
    void phasesFollowHealth() {
        assertEquals(1, BossRules.phase(1.0));
        assertEquals(2, BossRules.phase(0.6));
        assertEquals(3, BossRules.phase(0.2));
    }

    @Test
    void everyBossHasTwoOrThreeAttacksToStartAndMoreLater() {
        for (String b : BOSSES) {
            int early = 0, late = 0;
            for (double d = 0; d < 40; d += 1) {
                early = Math.max(early, BossRules.allowed(b, 1, d, 0).size());
                late = Math.max(late, BossRules.allowed(b, 3, d, 0).size());
            }
            assertTrue(early >= 2, b);
            assertTrue(late >= early && late <= 4, b);
        }
    }

    @Test
    void someAttackAlwaysFitsUpClose() {
        for (String b : BOSSES) assertFalse(BossRules.allowed(b, 1, 3, 0).isEmpty(), b);
    }

    @Test
    void noEndlessCrowds() {
        assertFalse(BossRules.allowed("GOBLIN_KING", 3, 10, 6).contains(BossRules.Move.SUMMON));
        assertNull(BossRules.pick("NOBODY", 1, 3, 0, n -> 0));
    }

    @Test
    void partiesGetTougherBosses() {
        assertTrue(BossRules.health(260, 2, 4) > BossRules.health(260, 2, 1) * 2.5);
        assertTrue(BossRules.damage(10, 2, 4) > BossRules.damage(10, 2, 1));
        assertEquals(BossRules.health(260, 2, 1), BossRules.health(260, 2, 0));
    }

    @Test
    void elitesGetCommonerInHigherRanks() {
        assertTrue(Elite.chance(5) > Elite.chance(0));
        int seen = 0;
        Random r = new Random(1);
        for (int i = 0; i < 2000; i++) {
            if (Elite.roll(5, r) != null) seen++;
        }
        assertTrue(seen > 150 && seen < 450, "seen " + seen);
    }
}
