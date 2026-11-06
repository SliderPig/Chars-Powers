package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpamLimiterTest {
    @Test
    void threePerMinute() {
        SpamLimiter s = new SpamLimiter(3);
        UUID p = UUID.randomUUID();
        assertTrue(s.allow(p, 0));
        assertTrue(s.allow(p, 10));
        assertTrue(s.allow(p, 20));
        assertFalse(s.allow(p, 30));
        assertTrue(s.allow(p, 1201));
        assertTrue(s.allow(UUID.randomUUID(), 30), "other players have their own count");
    }
}
