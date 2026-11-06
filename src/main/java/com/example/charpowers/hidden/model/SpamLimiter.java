package com.example.charpowers.hidden.model;

import java.util.*;

// Stops mobs' funny powers from turning into noise spam: each player gets at most N in any 1-minute window.
public final class SpamLimiter {
    private static final long WINDOW = 1200; // ticks = 1 minute
    private final int perMinute;
    private final Map<UUID, ArrayDeque<Long>> recent = new HashMap<>();

    public SpamLimiter(int perMinute) { this.perMinute = perMinute; }

    public boolean allow(UUID player, long gameTime) {
        ArrayDeque<Long> q = recent.computeIfAbsent(player, k -> new ArrayDeque<>());
        while (!q.isEmpty() && gameTime - q.peekFirst() >= WINDOW) q.pollFirst();
        if (q.size() >= perMinute) return false;
        q.addLast(gameTime);
        return true;
    }

    public void forget(UUID player) { recent.remove(player); }
}
