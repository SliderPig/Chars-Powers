package com.example.charpowers.mobcontrol.model;

import java.util.List;
import java.util.Set;

// The numbers behind Mob Control, with no game code in them.
public final class MobRules {
    public static final List<String> TAMEABLE = List.of("wolf", "cat", "parrot", "horse", "donkey", "mule", "llama");
    public static final int TOUGH_LEVEL = 50, BOSS_LEVEL = 100, FLYING_LEVEL = 50, QUEST_PETS = 5, REACH = 8, BODY_LEASH = 64;

    public enum Tier { NORMAL, TOUGH, BOSS }

    private static final Set<String> TOUGH = Set.of("ravager", "piglin_brute", "elder_guardian", "warden");
    public static final Set<String> NEVER_KEPT = Set.of("ender_dragon");
    private static final Set<String> BOSSES = Set.of("wither", "ender_dragon");

    private MobRules() {}

    private static int steps(int level) {
        return Math.max(0, Math.min(99, level - 1));
    }

    public static Tier tier(String entityPath, boolean bossTag) {
        if (bossTag || BOSSES.contains(entityPath)) return Tier.BOSS;
        return TOUGH.contains(entityPath) ? Tier.TOUGH : Tier.NORMAL;
    }

    // The higher your mastery, the bigger the mobs that leave you alone.
    public static boolean leavesYouAlone(Tier tier, int level) {
        return switch (tier) {
            case NORMAL -> true;
            case TOUGH -> level >= TOUGH_LEVEL;
            case BOSS -> level >= BOSS_LEVEL;
        };
    }

    // Commands reach 10 blocks at level 1, 50 at level 100.
    public static int commandRange(int level) {
        return 10 + steps(level) * 40 / 99;
    }

    // 1 pet at the start, up to 5 at level 100.
    public static int maxPets(int level) {
        return 1 + steps(level) * 4 / 99;
    }

    // Swap minds: 30 s at the start, up to 3 minutes at level 100.
    public static int swapTicks(int level) {
        return 600 + steps(level) * 3000 / 99;
    }

    // Bosses need the awakening, flying mobs need mastery 50.
    public static boolean canRide(Tier tier, boolean flying, int level, boolean awakened) {
        if (tier == Tier.BOSS && !awakened) return false;
        return !flying || level >= FLYING_LEVEL;
    }

    public static boolean canKeep(Tier tier, boolean awakened) {
        return tier != Tier.BOSS || awakened;
    }

    // Tough ones (ravager, brute, warden...) need mastery 50 to keep, bosses the awakening. The dragon follows no one.
    public static boolean canKeep(String entityPath, Tier tier, int level, boolean awakened) {
        if (NEVER_KEPT.contains(entityPath)) return false;
        if (tier == Tier.TOUGH && level < TOUGH_LEVEL) return false;
        return canKeep(tier, awakened);
    }
}
