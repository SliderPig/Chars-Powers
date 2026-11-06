package com.example.charpowers.authority.model;

// The numbers behind Superior Authority, with no game code in them.
public final class AuthorityRules {
    public static final int STOP = 1, NO_JUMP = 2, FREEZE = 4, KNEEL = 8, LOOK = 16, ATTACK = 32;
    public static final int DIE_RADIUS = 100, DIE_PER_TICK = 16, MOB_CAP = 256, DIE_COOLDOWN = 6000;

    private AuthorityRules() {}

    // +100 blocks every 10 levels, 1,000 at level 100.
    public static int range(int level) {
        return Math.max(100, 100 * (Math.min(100, level) / 10));
    }

    // 10 s at level 1, 60 s at level 100. The cooldown is the same.
    public static int ticks(int level) {
        return 200 + Math.max(0, Math.min(99, level - 1)) * 1000 / 99;
    }

    public static int mask(String ability) {
        return switch (ability) {
            case "stop" -> STOP;
            case "no_jump" -> NO_JUMP;
            case "freeze" -> FREEZE;
            case "kneel" -> KNEEL;
            case "look_at_me" -> LOOK;
            case "attack" -> ATTACK;
            default -> 0;
        };
    }

    public static String word(String ability) {
        return switch (ability) {
            case "stop" -> "Stop";
            case "no_jump" -> "No jumping, no flying";
            case "freeze" -> "Freeze";
            case "kneel" -> "Kneel";
            case "look_at_me" -> "Look at me";
            case "attack" -> "Attack";
            default -> ability;
        };
    }
}
