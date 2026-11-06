package com.example.charpowers.cloning.model;

// Which ores a mining clone wants most. No game code here, so it can be unit tested.
public final class OreRules {
    public static final int TOP_RANK = 9;

    private OreRules() {}

    // Rarer ores first. Anything else tagged as an ore (other mods') counts about as much as gold.
    public static int rank(String blockName) {
        String n = blockName.toLowerCase();
        if (n.contains("ancient_debris") || n.contains("netherite") || n.contains("vibranium")) return 9;
        if (n.contains("diamond")) return 8;
        if (n.contains("emerald")) return 7;
        if (n.contains("gold")) return 6;
        if (n.contains("lapis")) return 5;
        if (n.contains("redstone")) return 4;
        if (n.contains("silver")) return 5;
        if (n.contains("iron") || n.contains("lead") || n.contains("tin") || n.contains("zinc") || n.contains("nickel") || n.contains("alumin")) return 3;
        if (n.contains("copper") || n.contains("quartz")) return 2;
        if (n.contains("coal")) return 1;
        return 6;
    }

    // For ores a mod forgot to tag, and the raw ore blocks in big veins.
    public static boolean looksLikeOre(String blockPath) {
        return blockPath.endsWith("_ore") || blockPath.startsWith("ore_") || blockPath.contains("_ore_") || blockPath.equals("ancient_debris")
                || (blockPath.startsWith("raw_") && blockPath.endsWith("_block"));
    }

    // deepslate_iron_ore and iron_ore are the same ore to anyone but a geologist.
    public static String family(String blockPath) {
        return blockPath.replace("deepslate_", "").replace("nether_", "").replace("_ore", "").replace("ore_", "");
    }

    // Worth = how rare, minus a little for how far. A diamond further off still beats nearby coal.
    public static int worth(int rank, double distance) {
        return rank * 100 - (int) Math.round(distance);
    }
}
