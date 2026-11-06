package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WreckQueueTest {
    @Test void nearestFirstAndCapped() {
        WreckQueue<Integer> q = new WreckQueue<>();
        q.add(List.of(5, 1, 3), i -> i);
        assertEquals(List.of(1, 3), q.take(2, i -> true));
        assertEquals(List.of(5), q.take(2, i -> true));
        assertTrue(q.isEmpty());
    }

    @Test void unusableAreDropped() {
        WreckQueue<Integer> q = new WreckQueue<>();
        q.add(List.of(1, 2, 3), i -> i);
        assertEquals(List.of(1, 3), q.take(3, i -> i != 2));
        assertTrue(q.isEmpty());
    }

    @Test void laterBatchesWaitTheirTurn() {
        WreckQueue<Integer> q = new WreckQueue<>();
        q.add(List.of(9, 8), i -> i);
        q.add(List.of(1), i -> i);
        assertEquals(List.of(8, 9, 1), q.take(10, i -> true));
    }
}
