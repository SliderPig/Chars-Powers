package com.example.charpowers.mastery;

import com.example.charpowers.mastery.model.ToastRules;
import com.example.charpowers.mastery.model.ToastRules.Unlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToastRulesTest {
    private static final List<Unlock> UNLOCKS = List.of(new Unlock(10, "Raise"), new Unlock(50, "Big Move"), new Unlock(25, "Tracking"));

    @Test
    void nextUnlockIsTheNearestAbove() {
        ToastRules.Next n = ToastRules.next(UNLOCKS, 12);
        assertEquals("Tracking", n.name());
        assertEquals(13, n.toGo());
        assertEquals(10, n.prevLevel());
        assertEquals("Big Move", ToastRules.next(UNLOCKS, 25).name());
        assertEquals(0.0f, ToastRules.progress(ToastRules.next(UNLOCKS, 10), 10), 1e-6);
    }

    @Test
    void withoutAListItFallsBackToTheNextQuarter() {
        ToastRules.Next n = ToastRules.next(List.of(), 31);
        assertEquals("Mastery 50", n.name());
        assertEquals(19, n.toGo());
        assertEquals("Mastery 100", ToastRules.next(List.of(), 80).name());
        assertNull(ToastRules.next(List.of(), 100));
        assertNull(ToastRules.next(UNLOCKS, 100));
    }

    @Test
    void oneToastEveryTenSeconds() {
        assertFalse(ToastRules.mayShow(5000, 0, true, true));
        assertTrue(ToastRules.mayShow(10_000, 0, true, true));
        assertFalse(ToastRules.mayShow(20_000, 0, false, true));
        assertFalse(ToastRules.mayShow(20_000, 0, true, false));
    }

    @Test
    void batchKeepsTheLatestLevelPerPower() {
        ToastRules.Batch b = new ToastRules.Batch();
        assertTrue(b.isEmpty());
        b.add("Cloning", 5);
        b.add("Necromancy", 3);
        b.add("Cloning", 6);
        Map<String, Integer> got = b.take();
        assertEquals(2, got.size());
        assertEquals(6, got.get("Cloning"));
        assertTrue(b.isEmpty());
    }

    @Test
    void linesForOneAndMany() {
        List<String> one = ToastRules.lines(Map.of("Cloning", 12), ToastRules.next(UNLOCKS, 12));
        assertEquals("Cloning is mastery 12", one.get(0));
        assertEquals("Next: Tracking (13 levels to go)", one.get(1));
        assertEquals("Next: Mastery 100 (1 level to go)", ToastRules.lines(Map.of("A", 99), ToastRules.next(List.of(), 99)).get(1));
        Map<String, Integer> many = new java.util.LinkedHashMap<>();
        many.put("A", 1);
        many.put("B", 2);
        many.put("C", 3);
        many.put("D", 4);
        List<String> lines = ToastRules.lines(many, null);
        assertEquals("4 powers levelled up", lines.get(0));
        assertEquals("A 1, B 2, C 3 and 1 more", lines.get(1));
    }

    @Test
    void slidesInAndOut() {
        assertEquals(0f, ToastRules.slide(-5));
        assertEquals(1f, ToastRules.slide(1000));
        assertTrue(ToastRules.slide(100) > 0f && ToastRules.slide(100) < 1f);
        assertEquals(0f, ToastRules.slide(ToastRules.SHOW_MS + 1));
        assertTrue(ToastRules.slide(ToastRules.SHOW_MS - 100) < 1f);
    }
}
