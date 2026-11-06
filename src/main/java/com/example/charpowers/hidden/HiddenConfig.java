package com.example.charpowers.hidden;

import com.example.charpowers.hidden.model.Rank;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.Map;

// Settings in config/charpowers-common.toml. Change a number there and restart (or reload the world).
public final class HiddenConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue ANIMAL_ONE_IN;
    public static final ForgeConfigSpec.IntValue OTHER_ONE_IN;
    public static final ForgeConfigSpec.IntValue FARM_ONE_IN;
    public static final ForgeConfigSpec.IntValue FARM_CROWD;
    public static final ForgeConfigSpec.BooleanValue MOBS_USE_CARRIED;
    public static final ForgeConfigSpec.IntValue MOB_RANDOM_MIN_SEC;
    public static final ForgeConfigSpec.IntValue MOB_RANDOM_MAX_SEC;
    public static final ForgeConfigSpec.IntValue MOB_RANDOM_RANGE;
    public static final ForgeConfigSpec.IntValue PLAYER_HEARS_PER_MINUTE;
    public static final ForgeConfigSpec.DoubleValue ATTACK_DAMAGE_SCALE;
    public static final ForgeConfigSpec.BooleanValue ATTACK_BREAKS_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue HIDDEN_POWERS_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_CLONES_ON_SERVER;
    public static final ForgeConfigSpec.BooleanValue CLONES_GET_SKILLS;
    public static final ForgeConfigSpec.BooleanValue CLONES_BREAK_BLOCKS;
    private static final Map<Rank, ForgeConfigSpec.IntValue> WEIGHTS = new EnumMap<>(Rank.class);

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("hidden_powers");
        ANIMAL_ONE_IN = b.comment("Animals (cows, fish, villagers...): 1 in this many spawn with a hidden power")
                .defineInRange("animalOneIn", 100, 1, 1_000_000);
        OTHER_ONE_IN = b.comment("Every other mob: 1 in this many spawn with a hidden power")
                .defineInRange("otherOneIn", 20, 1, 1_000_000);
        FARM_ONE_IN = b.comment("Mobs that seem to be in a farm (born into a crowd): 1 in this many spawn with a hidden power. 0 = never. Spawner, bred and villager-made mobs never do.")
                .defineInRange("farmOneIn", 200, 0, 1_000_000);
        FARM_CROWD = b.comment("A mob counts as in a crowd with this many others of its sort within 16 blocks")
                .defineInRange("farmCrowd", 8, 1, 1000);
        MOBS_USE_CARRIED = b.comment("Mobs use the powers written for players, in their own way (leap, throw, heal, sense you through walls...)")
                .define("mobsUseCarriedPowers", true);
        MOB_RANDOM_MIN_SEC = b.comment("Mobs use their funny powers every this many seconds (shortest)")
                .defineInRange("mobRandomMinSeconds", 60, 5, 3600);
        MOB_RANDOM_MAX_SEC = b.comment("... and longest")
                .defineInRange("mobRandomMaxSeconds", 120, 5, 3600);
        MOB_RANDOM_RANGE = b.comment("A player has to be this close (blocks) for a mob to use a funny power")
                .defineInRange("mobRandomRange", 16, 1, 128);
        PLAYER_HEARS_PER_MINUTE = b.comment("Most mob funny powers one player hears in a minute (stops spam)")
                .defineInRange("playerHearsPerMinute", 3, 0, 100);
        ATTACK_DAMAGE_SCALE = b.comment("Multiplies the damage of the attack powers (#302-381). 0.5 = half as hard, 2 = twice")
                .defineInRange("attackDamageScale", 1.0, 0.0, 100.0);
        ATTACK_BREAKS_BLOCKS = b.comment("Can the attack powers wreck the ground (craters, tunnels...)? Players' and mobs' alike.",
                "Chests and other blocks that hold things, obsidian and unbreakable blocks are never broken,",
                "and land protected by a claim plugin or spawn protection is skipped.")
                .define("attackPowersBreakBlocks", true);
        HIDDEN_POWERS_ENABLED = b.comment("Do mobs get hidden powers? Off by default on a dedicated server, so installing this mod for Cloning",
                "doesn't also switch on the hidden mob powers, which haven't been tried on a server yet.")
                .define("hiddenPowersEnabled", net.minecraftforge.fml.loading.FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER);
        MAX_CLONES_ON_SERVER = b.comment("Cloning: how many real-player clones can be out at once on the whole server.",
                "Each one costs about as much as a real player, so turn this down if the server lags.")
                .defineInRange("maxClonesOnServer", 9, 0, 100);
        CLONES_GET_SKILLS = b.comment("Cloning: a clone that gains a power also gets all that power's buyable skills for free.",
                "Without this most FSang and Echoes abilities stay locked on clones, as nothing can spend their skill points.")
                .define("clonesGetSkills", true);
        CLONES_BREAK_BLOCKS = b.comment("Cloning: may clones use abilities that look like they break blocks?")
                .define("clonesBreakBlocks", false);
        b.comment("How likely each rank is when a mob gets a power (only ranks that exist count).",
                "From my rarity table (out of all mobs): F 1 in 50, E 1 in 67, D 1 in 110, C 1 in 200,",
                "B 1 in 1,000, A 1 in 10,000, S 1 in 100,000, Z 1 in 10,000,000 (and only one Z mob in the world).",
                "Only the sizes compared to each other matter. (Worlds made before version 2.0 keep their old numbers,",
                "where S and Z were 0 = never: change them here, or delete this file to get these.)").push("rankWeights");
        int[] defaults = {400000, 300000, 178000, 100000, 20000, 2000, 200, 2};
        for (Rank r : Rank.values()) WEIGHTS.put(r, b.defineInRange(r.name(), defaults[r.ordinal()], 0, 1_000_000));
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    public static Map<Rank, Integer> weights() {
        Map<Rank, Integer> out = new EnumMap<>(Rank.class);
        WEIGHTS.forEach((r, v) -> out.put(r, v.get()));
        return out;
    }

    private HiddenConfig() {}
}
