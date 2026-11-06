package com.example.charpowers.game.model;

// The Player's shop. Prices are worked out in clears: what a cleared gate of a fitting rank pays (GateRules.coins).
// A diamond is about two Rank C gates, a netherite ingot about sixteen Rank B ones, an elytra about fourteen Rank A ones.
// Lines are "item count price". Anything of your own goes in config/charpowers/game_shop.txt.
public final class ShopPrices {
    public static final String OLD_HEADER = "# Adds to the Player's shop: item, how many, price in coins. Anything from any mod works.";
    public static final String HEADER = OLD_HEADER + "\n# Built-in items and prices are in the mod (a gate of Rank C pays 28 coins, B 48, A 78, S 120). Lines here are added to the shop.\n";

    public static final String[] BUILT_IN = {
            "charpowers:knight_killer 1 400", "charpowers:crimson_edge 1 700", "charpowers:gravity_blade 1 950", "charpowers:grand_spear 1 900", "charpowers:shadow_scythe 1 1400", "charpowers:cocytus_hammer 1 1900",
            "charpowers:power_syringe 1 150", "minecraft:diamond 1 60", "minecraft:emerald 8 60", "minecraft:gold_ingot 8 40", "minecraft:iron_ingot 16 40", "minecraft:golden_apple 1 90",
            "minecraft:enchanted_golden_apple 1 700", "minecraft:cooked_beef 16 30", "minecraft:ender_pearl 4 70", "minecraft:blaze_rod 4 90", "minecraft:experience_bottle 16 120", "minecraft:firework_rocket 16 50",
            "minecraft:netherite_scrap 1 220", "minecraft:netherite_ingot 1 750", "minecraft:ancient_debris 1 260", "minecraft:shulker_shell 2 200", "minecraft:totem_of_undying 1 450", "minecraft:trident 1 500",
            "minecraft:elytra 1 1100", "minecraft:nether_star 1 1300", "minecraft:beacon 1 1600", "minecraft:diamond_block 1 600", "palladium:vibranium_ingot 1 160", "palladium:titanium_ingot 1 90"};

    private ShopPrices() {}

    public static int priceOf(String line) {
        return Integer.parseInt(line.trim().split("\s+")[2]);
    }

    // How many cleared gates of this rank an item costs.
    public static double clears(String line, int rank) {
        return priceOf(line) / (double) GateRules.coins(rank);
    }
}
