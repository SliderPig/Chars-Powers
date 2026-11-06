package com.example.charpowers.hidden.model;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

// Blocks waiting to be wrecked, so a huge crater is spread over several ticks instead of freezing the server.
// Each batch is sorted nearest first and waits behind the batches added before it.
public final class WreckQueue<T> {
    public static final int BREAKS_PER_TICK = 500;

    private final ArrayDeque<T> items = new ArrayDeque<>();

    public void add(Collection<T> batch, ToDoubleFunction<T> distance) {
        List<T> sorted = new ArrayList<>(batch);
        sorted.sort(Comparator.comparingDouble(distance));
        items.addAll(sorted);
    }

    // Takes up to `max` from the front; the ones that aren't usable (any more) are just dropped.
    public List<T> take(int max, Predicate<T> usable) {
        List<T> out = new ArrayList<>();
        for (int i = 0; i < max && !items.isEmpty(); i++) {
            T t = items.poll();
            if (usable.test(t)) out.add(t);
        }
        return out;
    }

    public boolean isEmpty() { return items.isEmpty(); }
}
