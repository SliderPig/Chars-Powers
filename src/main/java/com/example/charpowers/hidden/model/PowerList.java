package com.example.charpowers.hidden.model;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// The hidden powers list in the menu: which powers show (search) and in what order (sort).
public final class PowerList {
    // The sort button cycles through these (list menu, 3.1).
    public enum Sort {
        RANK_DOWN("Rank ▼"), RANK_UP("Rank ▲"), NAME("A-Z"), KEYS_FIRST("Keys");

        private final String label;

        Sort(String label) { this.label = label; }

        public String label() { return label; }

        public Sort next() { return values()[(ordinal() + 1) % values().length]; }
    }

    // One row: `key` = a key power (else always on).
    public record Entry(String id, String name, Rank rank, boolean key) {}

    private static final Comparator<Entry> BY_NAME =
            Comparator.comparing((Entry e) -> e.name().toLowerCase(Locale.ROOT)).thenComparing(Entry::id);
    private static final Comparator<Entry> RANK_DOWN =
            Comparator.comparing((Entry e) -> e.rank().ordinal(), Comparator.reverseOrder()).thenComparing(BY_NAME);

    // The rows to show: those whose name contains `search` (any case), in `sort` order. Ties go by name.
    public static List<Entry> view(List<Entry> all, Sort sort, String search) {
        String s = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Comparator<Entry> order = switch (sort) {
            case RANK_DOWN -> RANK_DOWN;
            case RANK_UP -> Comparator.comparing((Entry e) -> e.rank().ordinal()).thenComparing(BY_NAME);
            case NAME -> BY_NAME;
            case KEYS_FIRST -> Comparator.comparing((Entry e) -> !e.key()).thenComparing(RANK_DOWN);
        };
        return all.stream().filter(e -> s.isEmpty() || e.name().toLowerCase(Locale.ROOT).contains(s)).sorted(order).toList();
    }

    // Keeps the scroll inside the list: 0 .. rows - visible (0 when everything fits).
    public static int clampScroll(int offset, int rows, int visible) {
        return Math.max(0, Math.min(offset, rows - visible));
    }

    private PowerList() {}
}
