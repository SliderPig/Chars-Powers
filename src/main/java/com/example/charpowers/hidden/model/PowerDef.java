package com.example.charpowers.hidden.model;

// The fixed facts about one hidden power. `id` has no namespace, e.g. "loud_meow".
// `number` is its place in my list (1-301). `cooldownTicks` only matters for KEY powers.
public record PowerDef(String id, int number, Rank rank, Kind kind, MobRule mobRule, int cooldownTicks) {
    public static final int DEFAULT_KEY_COOLDOWN = 40; // 2 seconds (user: 10 s felt far too long)

    public static PowerDef key(String id, int number, Rank rank, MobRule rule) {
        return new PowerDef(id, number, rank, Kind.KEY, rule, DEFAULT_KEY_COOLDOWN);
    }

    public static PowerDef key(String id, int number, Rank rank, MobRule rule, int cooldownTicks) {
        return new PowerDef(id, number, rank, Kind.KEY, rule, cooldownTicks);
    }

    public static PowerDef always(String id, int number, Rank rank, MobRule rule) {
        return new PowerDef(id, number, rank, Kind.ALWAYS_ON, rule, 0);
    }
}
