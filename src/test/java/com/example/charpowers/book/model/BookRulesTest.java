package com.example.charpowers.book.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BookRulesTest {
    @Test
    void pagesComeWithMastery() {
        assertEquals(1, BookRules.pages(1));
        assertEquals(1, BookRules.pages(24));
        assertEquals(2, BookRules.pages(25));
        assertEquals(3, BookRules.pages(50));
        assertEquals(4, BookRules.pages(75));
        assertEquals(5, BookRules.pages(100));
    }

    @Test
    void deathsComeBackFromFurtherAway() {
        assertEquals(32, BookRules.deathRange(1), 0.001);
        assertEquals(64, BookRules.deathRange(100), 0.001);
    }

    @Test
    void theBookIsAlwaysInTheRing() {
        for (double a = 0; a <= 1; a += 0.1) {
            for (double b = 0; b <= 1; b += 0.1) {
                int[] spot = BookRules.spotInRing(a, b);
                double far = Math.hypot(spot[0], spot[1]);
                assertTrue(far >= 2999 && far <= 30001, "at " + far);
            }
        }
    }

    @Test
    void onlyWhatNoPageNeedsCanGo() {
        assertEquals(10, BookRules.oldestNeeded(new long[] {-1, 10, 40, -1, -1}, 50));
        assertEquals(50, BookRules.oldestNeeded(new long[] {-1, -1, -1, -1, -1}, 50));
    }

    @Test
    void theJournalIsTrimmedInLumps() {
        assertEquals(0, BookRules.toDrop(BookRules.MAX_ENTRIES));
        assertEquals(BookRules.MAX_ENTRIES / 10, BookRules.toDrop(BookRules.MAX_ENTRIES + 1));
    }
}
