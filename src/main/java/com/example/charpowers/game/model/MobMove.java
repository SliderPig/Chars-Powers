package com.example.charpowers.game.model;

import java.util.List;
import java.util.Map;

// The little moves gate creatures have. Every creature has at least two of its own; each shows a short tell before it lands
// (a sound, a raised arm, particles) and has a cooldown. Nothing here knows about the game: GateMob carries them out.
public enum MobMove {
    // name(tell ticks, cooldown ticks, nearest, farthest it fits, what the tell looks like)
    STAB(6, 40, 0, 3.2, Look.HIT), ROCK_THROW(14, 80, 4, 14, Look.CAST), SWARM(16, 200, 0, 99, Look.SELF),
    HEAL_ALLIES(22, 200, 0, 99, Look.SELF), FIRE_BOLT(20, 80, 3, 16, Look.CAST),
    SLAM(22, 120, 0, 4, Look.RING), SHIELD_BLOCK(10, 180, 0, 99, Look.SELF),
    CHARGE(18, 140, 5, 16, Look.LINE), WAR_CRY(22, 260, 0, 99, Look.SELF),
    RAPID_VOLLEY(16, 100, 5, 18, Look.CAST), DODGE_ROLL(5, 100, 0, 6, Look.HIT),
    BLINK_BEHIND(12, 110, 4, 16, Look.CAST), POISON_BLADE(8, 70, 0, 3.2, Look.HIT),
    ACID_SPIT(16, 90, 3, 14, Look.CAST), BURROW(16, 220, 0, 99, Look.RING), BOULDER(24, 140, 5, 16, Look.CAST), STOMP(24, 120, 0, 5, Look.RING),
    LEAP(12, 90, 3, 10, Look.LINE), BLEED_BITE(8, 60, 0, 3, Look.HIT), HOWL(20, 220, 0, 99, Look.SELF), POUNCE(8, 70, 2, 7, Look.LINE),
    SPEAR_THRUST(10, 70, 2, 5, Look.HIT), FIREBALL(18, 90, 4, 18, Look.CAST), DASH(8, 100, 2, 9, Look.LINE), FLAME_SLASH(14, 90, 0, 4, Look.HIT),
    DIVE(14, 130, 4, 14, Look.LINE), STONE_SKIN(12, 240, 0, 99, Look.SELF), SHIELD_BASH(10, 90, 0, 3, Look.HIT), SWEEP(14, 80, 0, 4, Look.RING),
    PHASE(10, 200, 3, 16, Look.SELF), LIFE_DRAIN(18, 100, 0, 6, Look.CAST), FRENZY(10, 100, 0, 3, Look.HIT), SHADOW_BOLT(20, 80, 3, 16, Look.CAST), RAISE_DEAD(30, 300, 0, 99, Look.SELF),
    SPEAR_THROW(16, 100, 4, 14, Look.CAST), TAIL_SWEEP(14, 90, 0, 4, Look.RING), POISON_SPIT(16, 90, 3, 14, Look.CAST), COIL_GRAB(16, 140, 0, 5, Look.HIT),
    WEB_SHOT(14, 100, 3, 14, Look.CAST), VENOM_BITE(8, 70, 0, 3, Look.HIT), FROST_ROAR(22, 160, 0, 7, Look.RING), ICE_ARROW(16, 80, 5, 18, Look.CAST),
    LASER(22, 90, 3, 16, Look.CAST), RETREAT(8, 130, 0, 5, Look.SELF), RIFLE_BURST(18, 90, 4, 16, Look.CAST), GRENADE(26, 160, 4, 14, Look.CAST),
    VOID_BOLT(18, 90, 3, 16, Look.CAST), TELEPORT(12, 120, 0, 99, Look.SELF), TONGUE_PULL(14, 120, 3, 9, Look.LINE), CHOMP(10, 60, 0, 3, Look.HIT),
    SPORE_CLOUD(18, 140, 0, 5, Look.RING), SPORE_SLEEP(20, 180, 0, 5, Look.RING), CHARGE_WALL(20, 160, 5, 16, Look.LINE), AXE_SWEEP(14, 80, 0, 4, Look.RING),
    CURSE(18, 150, 2, 10, Look.CAST), GRAB(12, 110, 0, 3, Look.HIT), SAND_BLAST(16, 100, 0, 8, Look.CAST);

    // HIT = arm raised, close up; CAST = particles at the hands; RING = a ring spreading from the feet; LINE = a line along the way it will go; SELF = glow round itself
    public enum Look { HIT, CAST, RING, LINE, SELF }

    public final int tell, cooldown;
    public final double near, far;
    public final Look look;

    MobMove(int tell, int cooldown, double near, double far, Look look) {
        this.tell = tell;
        this.cooldown = cooldown;
        this.near = near;
        this.far = far;
        this.look = look;
    }

    // Moves that carry the creature somewhere, so it must not stand still while the tell runs.
    public boolean travels() {
        return this == CHARGE || this == CHARGE_WALL || this == LEAP || this == POUNCE || this == DASH || this == DIVE || this == PHASE;
    }

    private static final Map<String, List<MobMove>> OF = Map.ofEntries(
            Map.entry("goblin", List.of(STAB, ROCK_THROW, SWARM)),
            Map.entry("goblin_shaman", List.of(HEAL_ALLIES, FIRE_BOLT)),
            Map.entry("hobgoblin", List.of(SLAM, SHIELD_BLOCK)),
            Map.entry("orc", List.of(CHARGE, WAR_CRY)),
            Map.entry("elf", List.of(RAPID_VOLLEY, DODGE_ROLL)),
            Map.entry("dark_elf", List.of(BLINK_BEHIND, POISON_BLADE)),
            Map.entry("ant_soldier", List.of(ACID_SPIT, BURROW)),
            Map.entry("stone_golem", List.of(BOULDER, STOMP)),
            Map.entry("dire_wolf", List.of(LEAP, BLEED_BITE, HOWL)),
            Map.entry("alpha_wolf", List.of(LEAP, BLEED_BITE, HOWL)),
            Map.entry("demon", List.of(SPEAR_THRUST, FIRE_BOLT)),
            Map.entry("imp", List.of(FIREBALL, DASH)),
            Map.entry("demon_knight", List.of(CHARGE, FLAME_SLASH)),
            Map.entry("gargoyle", List.of(DIVE, STONE_SKIN)),
            Map.entry("skeleton_knight", List.of(SHIELD_BASH, SWEEP, SHIELD_BLOCK)),
            Map.entry("wraith", List.of(PHASE, LIFE_DRAIN)),
            Map.entry("ghoul", List.of(POUNCE, FRENZY)),
            Map.entry("lich", List.of(SHADOW_BOLT, RAISE_DEAD)),
            Map.entry("lizardman", List.of(SPEAR_THROW, TAIL_SWEEP)),
            Map.entry("snake_warrior", List.of(POISON_SPIT, COIL_GRAB)),
            Map.entry("giant_spider", List.of(WEB_SHOT, VENOM_BITE)),
            Map.entry("ice_bear", List.of(CHARGE, FROST_ROAR)),
            Map.entry("ice_archer", List.of(ICE_ARROW, DODGE_ROLL)),
            Map.entry("sentry_drone", List.of(LASER, RETREAT)),
            Map.entry("cyber_soldier", List.of(RIFLE_BURST, GRENADE)),
            Map.entry("bulwark_mech", List.of(SHIELD_BLOCK, STOMP)),
            Map.entry("alien_crawler", List.of(POUNCE, VENOM_BITE)),
            Map.entry("alien", List.of(TELEPORT, VOID_BOLT)),
            Map.entry("mimic", List.of(TONGUE_PULL, CHOMP)),
            Map.entry("slime_knight", List.of(LEAP, SHIELD_BLOCK)),
            Map.entry("slime_king", List.of(LEAP, SLAM)),
            Map.entry("mushroom_man", List.of(SPORE_CLOUD, SPORE_SLEEP)),
            Map.entry("minotaur", List.of(CHARGE_WALL, AXE_SWEEP)),
            Map.entry("mummy", List.of(GRAB, CURSE)),
            Map.entry("scarab", List.of(POUNCE, BURROW)),
            Map.entry("sand_golem", List.of(SAND_BLAST, BURROW)));

    // The moves of an ordinary creature (bosses have theirs in BossRules). Empty if the id is unknown.
    public static List<MobMove> of(String kindId) {
        return OF.getOrDefault(kindId, List.of());
    }

    public static java.util.Set<String> kinds() {
        return OF.keySet();
    }
}
