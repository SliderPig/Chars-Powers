package com.example.charpowers.translation.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

class VoiceRulesTest {
    private static final VoiceRules.Line FUNNY = VoiceRules.parse("voice.zombie.funny.hello");
    private static final VoiceRules.Line SAD = VoiceRules.parse("voice.zombie.sad.lonely");
    private static final VoiceRules.Line USEFUL = VoiceRules.parse("voice.zombie.useful.behind");

    @Test
    void readsTheSoundName() {
        assertEquals("zombie", FUNNY.mob());
        assertEquals("funny", FUNNY.kind());
        assertNull(VoiceRules.parse("voice.zombie"));
        assertNull(VoiceRules.parse("scream.big.loud.now"));
    }

    @Test
    void rangeGrowsWithMastery() {
        assertEquals(8, VoiceRules.range(1));
        assertEquals(32, VoiceRules.range(100));
    }

    @Test
    void sadAndUsefulLinesWaitForMastery() {
        List<VoiceRules.Line> all = List.of(FUNNY, SAD, USEFUL);
        Random r = new Random(1);
        for (int i = 0; i < 20; i++) assertEquals(FUNNY, VoiceRules.pick(all, "zombie", 24, Set.of(), r));
        assertEquals(SAD, VoiceRules.pick(all, "zombie", 25, Set.of(FUNNY.id()), r));
        assertEquals(USEFUL, VoiceRules.pick(all, "zombie", 50, Set.of(FUNNY.id(), SAD.id()), r));
    }

    @Test
    void newLinesComeFirstThenAnything() {
        assertEquals(FUNNY, VoiceRules.pick(List.of(FUNNY), "zombie", 1, Set.of(FUNNY.id()), new Random(1)));
        assertNull(VoiceRules.pick(List.of(FUNNY), "creeper", 1, Set.of(), new Random(1)));
    }

    @Test
    void theQuestNeedsEveryLineAndAtLeastOne() {
        assertFalse(VoiceRules.heardAll(List.of(), Set.of()));
        assertFalse(VoiceRules.heardAll(List.of(FUNNY, SAD), Set.of(FUNNY.id())));
        assertTrue(VoiceRules.heardAll(List.of(FUNNY, SAD), Set.of(FUNNY.id(), SAD.id())));
    }
}
