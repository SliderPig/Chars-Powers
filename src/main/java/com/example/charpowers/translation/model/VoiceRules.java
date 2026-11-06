package com.example.charpowers.translation.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

// Which voice line to play. Lines come from my resource pack, as sounds named voice.<mob>.<kind>.<name>.
public final class VoiceRules {
    public static final String PREFIX = "voice.";
    public static final int PARROT_SECONDS = 1200;
    public static final int SAD_LEVEL = 25, USEFUL_LEVEL = 50;
    public static final long LINE_GAP_TICKS = 160, SAME_MOB_GAP_TICKS = 600;

    public record Line(String id, String mob, String kind) {}

    private VoiceRules() {}

    // voice.zombie.funny.hello -> the line, or null if the name isn't a voice line.
    public static Line parse(String path) {
        if (!path.startsWith(PREFIX)) return null;
        String[] parts = path.split("\\.");
        if (parts.length < 4 || parts[1].isEmpty() || parts[2].isEmpty()) return null;
        return new Line(path, parts[1], parts[2]);
    }

    // You hear mobs from 8 blocks at level 1, 32 at level 100.
    public static int range(int level) {
        return 8 + Math.max(0, Math.min(99, level - 1)) * 24 / 99;
    }

    public static boolean kindAllowed(String kind, int level) {
        return switch (kind) {
            case "sad" -> level >= SAD_LEVEL;
            case "useful" -> level >= USEFUL_LEVEL;
            default -> true;
        };
    }

    // One of this mob's lines you're allowed to hear, ones you haven't heard yet first. Null = nothing to say.
    public static Line pick(Collection<Line> lines, String mob, int level, Set<String> heard, Random random) {
        List<Line> fresh = new ArrayList<>(), any = new ArrayList<>();
        for (Line l : lines) {
            if (!l.mob().equals(mob) || !kindAllowed(l.kind(), level)) continue;
            any.add(l);
            if (!heard.contains(l.id())) fresh.add(l);
        }
        List<Line> from = fresh.isEmpty() ? any : fresh;
        return from.isEmpty() ? null : from.get(random.nextInt(from.size()));
    }

    // The quest: every line in your folders heard at least once. No lines = nothing to finish.
    public static boolean heardAll(Collection<Line> lines, Set<String> heard) {
        if (lines.isEmpty()) return false;
        for (Line l : lines) {
            if (!heard.contains(l.id())) return false;
        }
        return true;
    }
}
