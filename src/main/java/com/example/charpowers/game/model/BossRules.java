package com.example.charpowers.game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

// What gate bosses do, with no game code in it: phases by health, which special attacks each boss has and when each fits, and the scaling by gate rank and party size.
public final class BossRules {
    // A special attack and how many ticks its tell lasts before it lands.
    public enum Move {
        LEAP(24), ROAR(30), SUMMON(36), CHARGE(30), CLEAVE(20), WARCRY(36), VOLLEY(22), NOVA(36), BLINK(14), METEORS(34), SPIT(26), BITE(24), SLASH3(14), WAVE(26), DASH(16),
        BREATH(34), GUST(24), TAILSWEEP(18), SWOOP(28), SANDSTORM(34), CURSE_ALL(30), SHIELD(20);
        public final int windup;

        Move(int windup) {
            this.windup = windup;
        }
    }

    private record Entry(Move move, int fromPhase, double minDist, double maxDist) {}

    private BossRules() {}

    private static List<Entry> moves(String kind) {
        return switch (kind) {
            case "GOBLIN_KING" -> List.of(new Entry(Move.LEAP, 1, 5, 16), new Entry(Move.ROAR, 1, 0, 8), new Entry(Move.SUMMON, 2, 0, 99));
            case "HOBGOBLIN_WARLORD" -> List.of(new Entry(Move.CHARGE, 1, 5, 20), new Entry(Move.CLEAVE, 1, 0, 5.5), new Entry(Move.WARCRY, 2, 0, 99));
            case "ELF_MAGE" -> List.of(new Entry(Move.VOLLEY, 1, 4, 30), new Entry(Move.NOVA, 1, 0, 7), new Entry(Move.BLINK, 2, 0, 99), new Entry(Move.METEORS, 3, 0, 99));
            case "ANT_QUEEN" -> List.of(new Entry(Move.SPIT, 1, 0, 30), new Entry(Move.BITE, 1, 4, 14), new Entry(Move.SUMMON, 2, 0, 99));
            case "WOLF_KING" -> List.of(new Entry(Move.LEAP, 1, 4, 14), new Entry(Move.SLASH3, 1, 0, 5), new Entry(Move.ROAR, 1, 0, 8), new Entry(Move.SUMMON, 2, 0, 99));
            case "DEMON_LORD" -> List.of(new Entry(Move.WAVE, 1, 3, 14), new Entry(Move.DASH, 1, 3, 30), new Entry(Move.METEORS, 2, 0, 99), new Entry(Move.SUMMON, 3, 0, 99));
            case "LICH_LORD" -> List.of(new Entry(Move.VOLLEY, 1, 4, 30), new Entry(Move.NOVA, 1, 0, 7), new Entry(Move.BLINK, 2, 0, 99), new Entry(Move.SUMMON, 2, 0, 99));
            case "SNAKE_QUEEN" -> List.of(new Entry(Move.SPIT, 1, 0, 30), new Entry(Move.BITE, 1, 4, 14), new Entry(Move.WAVE, 2, 3, 14), new Entry(Move.SUMMON, 2, 0, 99));
            case "FROST_KING" -> List.of(new Entry(Move.NOVA, 1, 0, 7), new Entry(Move.CHARGE, 1, 5, 20), new Entry(Move.VOLLEY, 2, 4, 30), new Entry(Move.METEORS, 3, 0, 99));
            case "WAR_MECH" -> List.of(new Entry(Move.VOLLEY, 1, 4, 30), new Entry(Move.ROAR, 1, 0, 8), new Entry(Move.SHIELD, 2, 0, 99), new Entry(Move.METEORS, 3, 0, 99));
            case "TOMB_KING" -> List.of(new Entry(Move.SANDSTORM, 1, 0, 12), new Entry(Move.CLEAVE, 1, 0, 5.5), new Entry(Move.CURSE_ALL, 2, 0, 12), new Entry(Move.SUMMON, 1, 0, 99));
            case "YOUNG_DRAGON" -> List.of(new Entry(Move.BREATH, 1, 3, 14), new Entry(Move.GUST, 1, 0, 9), new Entry(Move.TAILSWEEP, 1, 0, 5), new Entry(Move.SWOOP, 2, 6, 30));
            default -> List.of();
        };
    }

    public static List<Move> allowed(String kind, int phase, double distance, int minions) {
        List<Move> out = new ArrayList<>();
        for (Entry e : moves(kind)) {
            if (phase < e.fromPhase || distance < e.minDist || distance > e.maxDist) continue;
            if ((e.move == Move.SUMMON || e.move == Move.WARCRY || e.move == Move.SHIELD) && minions >= 6) continue; // no endless crowds
            out.add(e.move);
        }
        return out;
    }

    // One of the attacks that fits right now, or null (then it just walks closer).
    public static Move pick(String kind, int phase, double distance, int minions, IntUnaryOperator random) {
        List<Move> fit = allowed(kind, phase, distance, minions);
        return fit.isEmpty() ? null : fit.get(random.applyAsInt(fit.size()));
    }

    // 1 above two thirds health, 2 above one third, then 3.
    public static int phase(double healthFraction) {
        return healthFraction > 2.0 / 3 ? 1 : healthFraction > 1.0 / 3 ? 2 : 3;
    }

    public static double partyHealth(int party) {
        return 1 + 0.7 * (Math.max(1, party) - 1);
    }

    public static double partyDamage(int party) {
        return 1 + 0.1 * (Math.max(1, party) - 1);
    }

    // kindHealth is the boss's own health in GateKind (260 = an average boss).
    public static double health(double kindHealth, int rank, int party) {
        return GateRules.bossHealth(rank) * kindHealth / 260 * partyHealth(party);
    }

    public static double damage(double kindDamage, int rank, int party) {
        return GateRules.bossDamage(rank) * kindDamage / 10 * partyDamage(party);
    }
}
