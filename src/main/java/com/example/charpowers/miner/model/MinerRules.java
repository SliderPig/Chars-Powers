package com.example.charpowers.miner.model;

import com.example.charpowers.cloning.model.OreRules;

import java.util.Set;

// The numbers behind The Miner, with no game code in them.
public final class MinerRules {
    public static final long BLOCKS_FOR_QUEST = 1_000_000L;
    public static final double EXTRA_ORE_CHANCE = 0.5;
    public static final int FIST_SPEED = 9; // a netherite pickaxe
    public static final int STAIR_LEVELS = 30, STAIR_TALL = 3, BALL_RADIUS = 15, MOTHERLODE_RADIUS = 15;
    public static final int STAIR_PER_TICK = 8, BALL_PER_TICK = 150, STRIP_PER_TICK = 170;
    public static final int CHUNK_CHECK_AFTER = 20_000, CHUNK_CHECK_EVERY = 1000, CHUNK_LEFTOVERS = 200, COUNTED_CHUNKS = 64;
    public static final int RUBBLE_STACKS = 64;

    private MinerRules() {}

    // Every 25 levels: another efficiency level (1 at 25, 4 at 100).
    public static int efficiency(int level) {
        return Math.min(4, Math.max(0, level) / 25);
    }

    // What vanilla adds to a tool's speed for that efficiency level.
    public static int efficiencySpeed(int efficiency) {
        return efficiency <= 0 ? 0 : efficiency * efficiency + 1;
    }

    // A pack long holds chunk x and z, the same way the game's ChunkPos does.
    public static long chunk(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    // 4 mined-out chunks that make a 2x2 square.
    public static boolean twoByTwo(Set<Long> done) {
        for (long c : done) {
            int x = (int) c, z = (int) (c >> 32);
            if (done.contains(chunk(x + 1, z)) && done.contains(chunk(x, z + 1)) && done.contains(chunk(x + 1, z + 1))) return true;
        }
        return false;
    }

    // Plain stuff you never want in your bag: it stays on the ground as a normal drop.
    private static final Set<String> JUNK = Set.of("dirt", "coarse_dirt", "rooted_dirt", "grass_block", "podzol", "mycelium", "mud", "gravel",
            "sand", "red_sand", "cobblestone", "cobbled_deepslate", "stone", "deepslate", "netherrack", "andesite", "diorite", "granite", "tuff",
            "calcite", "basalt", "smooth_basalt", "blackstone", "soul_sand", "soul_soil", "end_stone", "dripstone_block", "pointed_dripstone",
            "sandstone", "red_sandstone", "terracotta", "clay_ball", "snowball", "flint", "bone_meal", "moss_block", "magma_block", "crimson_nylium", "warped_nylium");

    public static boolean isJunk(String itemPath) {
        return JUNK.contains(itemPath);
    }

    // How rare this ore is (higher = rarer). Same ranking the clones use.
    public static int oreRank(String blockPath) {
        return OreRules.rank(blockPath);
    }

    // Rarest first, then nearest. Used to sort lists of ore spots.
    public static int compareRarity(int rankA, double distA, int rankB, double distB) {
        if (rankA != rankB) return Integer.compare(rankB, rankA);
        return Double.compare(distA, distB);
    }

    // The most fluid blocks Motherlode clears in one go, so it never drains an ocean.
    public static final int FLUID_CAP = 1500, FLUID_REACH = 2;
}
