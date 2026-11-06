package com.example.charpowers.cloning.model;

import static org.junit.jupiter.api.Assertions.*;

import com.example.charpowers.cloning.model.BrainRules.Option;
import com.example.charpowers.cloning.model.BrainRules.Role;
import com.example.charpowers.cloning.model.BrainRules.Situation;
import org.junit.jupiter.api.Test;

import java.util.List;

class BrainRulesTest {
    private static final Option BEAM = new Option("beam", Role.RANGED, 10);
    private static final Option PUNCH = new Option("punch", Role.MELEE, 32);
    private static final Option HEAL = new Option("heal", Role.HEAL, 32);
    private static final Option SHIELD = new Option("shield", Role.DEFEND, 32);
    private static final Option DASH = new Option("dash", Role.MOVE, 32);
    private static final Option RAGE = new Option("rage", Role.BUFF, 32);
    private static final Option ODD = new Option("odd", Role.UNKNOWN, 32);

    private static Option pick(Situation s, Option... options) {
        return BrainRules.pick(List.of(options), s, n -> 0);
    }

    @Test
    void healsThenBlocksWhenHurt() {
        assertEquals(HEAL, pick(new Situation(3, 0.2, true), PUNCH, SHIELD, HEAL));
        assertEquals(SHIELD, pick(new Situation(3, 0.2, true), PUNCH, SHIELD));
        assertEquals(PUNCH, pick(new Situation(3, 0.9, true), PUNCH, HEAL, SHIELD));
    }

    @Test
    void meleeUpCloseBeamsFromAfar() {
        assertEquals(PUNCH, pick(new Situation(3, 1, true), PUNCH, BEAM));
        assertEquals(BEAM, pick(new Situation(8, 1, true), PUNCH, BEAM));
        assertEquals(BEAM, pick(new Situation(3, 1, true), BEAM));
    }

    @Test
    void neverShootsWithoutAClearShotOrOutOfRange() {
        assertNull(pick(new Situation(8, 1, false), BEAM));
        assertNull(pick(new Situation(11, 1, true), BEAM) == BEAM ? BEAM : null);
        assertEquals(PUNCH, pick(new Situation(3, 1, false), BEAM, PUNCH));
    }

    @Test
    void dashesWhenFarWithNoShot() {
        assertEquals(DASH, pick(new Situation(20, 1, true), BEAM, DASH));
        assertNull(pick(new Situation(6, 1, false), DASH));
    }

    @Test
    void buffsFirstThenTriesTheUnknownLast() {
        assertEquals(RAGE, pick(new Situation(3, 1, true), PUNCH, RAGE));
        assertEquals(PUNCH, pick(new Situation(3, 1, true), PUNCH, ODD));
        assertEquals(ODD, pick(new Situation(3, 1, true), ODD));
        assertNull(pick(new Situation(3, 1, false), ODD));
    }

    @Test
    void judgesWhatAnUnknownAbilityDid() {
        assertEquals(Role.RANGED, BrainRules.judge(4, 0));
        assertEquals(Role.HEAL, BrainRules.judge(0, 3));
        assertNull(BrainRules.judge(0.5, 1));
    }

    @Test
    void leadsShotsButNotBeams() {
        assertEquals(0, BrainRules.leadTicks(10, 0));
        assertEquals(5, BrainRules.leadTicks(10, 2));
        assertEquals(20, BrainRules.leadTicks(1000, 2));
    }
}
