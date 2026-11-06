package com.example.charpowers.cloning.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

// How a clone picks its next move in a fight, with no game code in it so it can be unit tested.
public final class BrainRules {
    public enum Role { RANGED, MELEE, HEAL, DEFEND, MOVE, BUFF, UNKNOWN }

    public record Option(String key, Role role, int range) {}

    // distance to the target, health from 0 to 1, and whether a shot would reach it without hitting a friend
    public record Situation(double distance, double health, boolean clearShot) {}

    public static final int MELEE_RANGE = 5;
    public static final int FAR = 8;
    public static final double HURT = 0.35;

    private BrainRules() {}

    public static Option pick(List<Option> options, Situation s, IntUnaryOperator random) {
        if (s.health() < HURT) {
            Option save = any(options, Role.HEAL, random);
            if (save == null) save = any(options, Role.DEFEND, random);
            if (save != null) return save;
        }
        Option buff = any(options, Role.BUFF, random);
        if (buff != null) return buff;
        Option attack = s.distance() <= MELEE_RANGE ? any(options, Role.MELEE, random) : null;
        if (attack == null && s.clearShot()) attack = ranged(options, s.distance(), random);
        if (attack != null) return attack;
        if (s.distance() > FAR) {
            Option move = any(options, Role.MOVE, random);
            if (move != null) return move;
        }
        // nothing it knows fits: a good moment to try something it doesn't know yet
        return s.clearShot() ? any(options, Role.UNKNOWN, random) : null;
    }

    private static Option ranged(List<Option> options, double distance, IntUnaryOperator random) {
        List<Option> fits = new ArrayList<>();
        for (Option o : options) if (o.role() == Role.RANGED && o.range() >= distance) fits.add(o);
        return fits.isEmpty() ? null : fits.get(random.applyAsInt(fits.size()));
    }

    private static Option any(List<Option> options, Role role, IntUnaryOperator random) {
        List<Option> fits = new ArrayList<>();
        for (Option o : options) if (o.role() == role) fits.add(o);
        return fits.isEmpty() ? null : fits.get(random.applyAsInt(fits.size()));
    }

    // Trying an unknown ability: what it did decides what it is. null = nothing that could be seen.
    public static Role judge(double targetLost, double selfGained) {
        if (targetLost >= 1.0) return Role.RANGED;
        if (selfGained >= 2.0) return Role.HEAL;
        return null;
    }

    // Where to aim a shot so it arrives where the target is going. A beam needs no lead (speed 0).
    public static double leadTicks(double distance, double shotSpeed) {
        return shotSpeed <= 0 ? 0 : Math.min(20.0, distance / shotSpeed);
    }
}
