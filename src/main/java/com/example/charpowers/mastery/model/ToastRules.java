package com.example.charpowers.mastery.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// What the mastery toast says and when it may appear, with no game code. One toast every ten seconds at the most:
// level-ups that happen in between are gathered and shown together.
public final class ToastRules {
    public static final long GAP_MS = 10_000, SHOW_MS = 4500, SLIDE_MS = 300;
    public static final int FALLBACK_STEP = 25;

    public record Unlock(int level, String name) {}

    public record Next(String name, int level, int prevLevel, int toGo) {}

    private ToastRules() {}

    // Gathers level-ups while waiting: the latest level per power, in the order they happened.
    public static final class Batch {
        private final Map<String, Integer> levels = new LinkedHashMap<>();

        public void add(String power, int level) {
            levels.remove(power);
            levels.put(power, level);
        }

        public boolean isEmpty() {
            return levels.isEmpty();
        }

        public Map<String, Integer> take() {
            Map<String, Integer> out = new LinkedHashMap<>(levels);
            levels.clear();
            return out;
        }
    }

    public static boolean mayShow(long now, long lastShown, boolean enabled, boolean pending) {
        return enabled && pending && now - lastShown >= GAP_MS;
    }

    // The next thing this level has not reached yet: the nearest listed unlock above it, else the next multiple of 25.
    // null at 100 (nothing left).
    public static Next next(List<Unlock> unlocks, int level) {
        Unlock best = null;
        int prev = 0;
        for (Unlock u : unlocks) {
            if (u.level() <= level) prev = Math.max(prev, u.level());
            else if (best == null || u.level() < best.level()) best = u;
        }
        if (best != null) return new Next(best.name(), best.level(), prev, best.level() - level);
        if (level >= MasteryMath.MAX_LEVEL) return null;
        int step = (level / FALLBACK_STEP + 1) * FALLBACK_STEP;
        int before = (level / FALLBACK_STEP) * FALLBACK_STEP;
        return new Next(step >= MasteryMath.MAX_LEVEL ? "Mastery 100" : "Mastery " + step, step, Math.max(before, prev), step - level);
    }

    public static String togo(int n) {
        return n == 1 ? "1 level to go" : n + " levels to go";
    }

    // The lines of one toast. powers = name -> new level. A single power names its next unlock; several are listed.
    public static List<String> lines(Map<String, Integer> powers, Next next) {
        List<String> out = new ArrayList<>();
        if (powers.size() == 1) {
            Map.Entry<String, Integer> e = powers.entrySet().iterator().next();
            out.add(e.getKey() + " is mastery " + e.getValue());
            out.add(next == null ? "You have mastered it" : "Next: " + next.name() + " (" + togo(next.toGo()) + ")");
            return out;
        }
        out.add(powers.size() + " powers levelled up");
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Map.Entry<String, Integer> e : powers.entrySet()) {
            if (shown == 3) break;
            if (shown > 0) sb.append(", ");
            sb.append(e.getKey()).append(' ').append(e.getValue());
            shown++;
        }
        if (powers.size() > shown) sb.append(" and ").append(powers.size() - shown).append(" more");
        out.add(sb.toString());
        return out;
    }

    // 0..1 how far the toast is slid in at this moment (0 = hidden at the edge, 1 = fully shown).
    public static float slide(long sinceShown) {
        if (sinceShown < 0) return 0f;
        if (sinceShown < SLIDE_MS) return ease(sinceShown / (float) SLIDE_MS);
        if (sinceShown < SHOW_MS - SLIDE_MS) return 1f;
        if (sinceShown < SHOW_MS) return ease((SHOW_MS - sinceShown) / (float) SLIDE_MS);
        return 0f;
    }

    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3 - 2 * t);
    }

    // The thin bar under the text: how far from the last unlock to the next one.
    public static float progress(Next next, int level) {
        if (next == null) return 1f;
        int span = Math.max(1, next.level() - next.prevLevel());
        return Math.max(0f, Math.min(1f, (level - next.prevLevel()) / (float) span));
    }
}
