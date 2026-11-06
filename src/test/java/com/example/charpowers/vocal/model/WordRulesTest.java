package com.example.charpowers.vocal.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WordRulesTest {
    @Test
    void masteryGrowsTheNumbers() {
        assertEquals(5, WordRules.maxLetters(1));
        assertEquals(30, WordRules.maxLetters(100));
        assertEquals(200, WordRules.lifeTicks(1));
        assertEquals(6000, WordRules.lifeTicks(100));
        assertEquals(20, WordRules.cost(5, 1));
        assertEquals(10, WordRules.cost(5, 100));
        assertEquals(1, WordRules.wordsAtOnce(99));
        assertEquals(2, WordRules.wordsAtOnce(100));
    }

    @Test
    void textIsCleanedAndCut() {
        assertEquals("HELLO", WordRules.clean("hello world", 5));
        assertEquals("HI YOU", WordRules.clean("  hi~ you  ", 5));
        assertEquals("", WordRules.clean("~~~", 5));
        assertEquals(4, WordRules.letters("HI YO"));
        assertEquals(29, WordRules.width("HELLO"));
    }

    @Test
    void lettersAreDrawnRight() {
        // T: the whole top row, then just the middle
        for (int x = 0; x < 5; x++) assertTrue(WordRules.ink("T", x, 0));
        assertTrue(WordRules.ink("T", 2, 3));
        assertFalse(WordRules.ink("T", 0, 3));
        // the gap between letters is empty, and the second letter starts after it
        assertFalse(WordRules.ink("TT", 5, 0));
        assertTrue(WordRules.ink("TT", 6, 0));
        assertFalse(WordRules.ink("T", 99, 0));
        assertFalse(WordRules.ink(" ", 2, 3));
    }

    @Test
    void everyAwakenedWordCanBeDrawn() {
        for (String w : WordRules.TRUE_WORDS) {
            assertTrue(WordRules.isTrueWord(w));
            assertTrue(WordRules.clean(w, 30).equals(w), w);
        }
        assertFalse(WordRules.isTrueWord("HELLO"));
    }
}
