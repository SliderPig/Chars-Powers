package com.example.charpowers.orders.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The order wheel's rules with no game code: which orders exist for what you have, which slice a mouse movement points at.
public final class OrderRules {
    public static final String CLONES = "clones", PETS = "pets", MINIONS = "minions";
    public static final int DEAD_ZONE = 18, MAX_PULL = 70, ORDER_GAP_TICKS = 6;

    public record Order(String group, String action, String label) {}

    private static final Map<String, List<Order>> ALL = Map.of(
            CLONES, List.of(o(CLONES, "follow", "Follow"), o(CLONES, "mine", "Mine"), o(CLONES, "chop", "Chop"), o(CLONES, "cook", "Cook"),
                    o(CLONES, "guard", "Guard"), o(CLONES, "farm", "Farm"), o(CLONES, "home", "Come home")),
            PETS, List.of(o(PETS, "follow", "Follow"), o(PETS, "stay", "Stay"), o(PETS, "guard", "Guard"), o(PETS, "attack", "Attack that")),
            MINIONS, List.of(o(MINIONS, "follow", "Follow"), o(MINIONS, "stay", "Stay"), o(MINIONS, "attack", "Attack that")));

    private OrderRules() {}

    private static Order o(String g, String a, String l) {
        return new Order(g, a, l);
    }

    // Only the orders for what you actually have.
    public static List<Order> available(int clones, int pets, int minions) {
        List<Order> out = new ArrayList<>();
        if (clones > 0) out.addAll(ALL.get(CLONES));
        if (pets > 0) out.addAll(ALL.get(PETS));
        if (minions > 0) out.addAll(ALL.get(MINIONS));
        return out;
    }

    public static boolean valid(String group, String action) {
        List<Order> list = ALL.get(group);
        if (list == null) return false;
        for (Order o : list) if (o.action().equals(action)) return true;
        return false;
    }

    public static String labelOf(String group, String action) {
        for (Order o : ALL.getOrDefault(group, List.of())) if (o.action().equals(action)) return o.label();
        return action;
    }

    // Which of n bubbles (the first straight up, going clockwise) the pulled-out mouse points at; -1 = none (middle).
    public static int slice(double dx, double dy, int n) {
        if (n <= 0 || Math.hypot(dx, dy) < DEAD_ZONE) return -1;
        double angle = Math.atan2(dx, -dy);
        if (angle < 0) angle += Math.PI * 2;
        double step = Math.PI * 2 / n;
        return (int) Math.round(angle / step) % n;
    }

    // Where bubble i of n sits on a ring of that radius, relative to the middle.
    public static double[] position(int i, int n, double radius) {
        double a = Math.PI * 2 * i / n;
        return new double[] {Math.sin(a) * radius, -Math.cos(a) * radius};
    }

    // Big enough to read, small enough not to touch its neighbours.
    public static double bubbleRadius(int n, double ring, double biggest) {
        if (n <= 1) return biggest;
        double room = ring * Math.sin(Math.PI / n) * 0.82;
        return Math.max(8, Math.min(biggest, room));
    }

    // Smooth 0..1.
    public static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3 - 2 * t);
    }

    // The pulled-out mouse stays inside the wheel.
    public static double[] clamp(double dx, double dy) {
        double len = Math.hypot(dx, dy);
        if (len <= MAX_PULL) return new double[] {dx, dy};
        return new double[] {dx * MAX_PULL / len, dy * MAX_PULL / len};
    }

    public static boolean tooSoon(long last, long now) {
        return now - last < ORDER_GAP_TICKS;
    }
}
