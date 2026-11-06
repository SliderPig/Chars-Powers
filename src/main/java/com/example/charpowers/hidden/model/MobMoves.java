package com.example.charpowers.hidden.model;

import java.util.HashMap;
import java.util.Map;

// Powers that only make sense for a player ("bread fills you twice as much") can't simply be pressed by a mob. Each gets the nearest
// thing a mob can really do with it, and MobBrain carries that out.
public final class MobMoves {
    public enum Move { LEAP, CLIMB, SOFT_FALL, FLOAT, RUN, SENSE, STALK, HEAL, THROW, DISARM, RALLY, BLINK, FROST, GATHER, SHOUT }

    private static final Map<String, Move> MOVES = new HashMap<>();

    private static void moves(Move move, String... ids) {
        for (String id : ids) MOVES.put(id, move);
    }

    static {
        moves(Move.LEAP, "frog_legs", "bounce_back", "bounce_pad", "roll", "wall_jump", "grapple_rod", "copycat");
        moves(Move.CLIMB, "climbing_hands", "wall_run", "ladder_monkey");
        moves(Move.SOFT_FALL, "glide", "umbrella", "wall_slide");
        moves(Move.FLOAT, "water_walker", "lava_skipper", "rubber_ducky", "bubble_burp", "air_pocket", "heavy_boots");
        moves(Move.RUN, "moonwalk", "clean_paws", "honey_paws", "sticky_feet", "snack_on_the_run", "mole");
        moves(Move.SENSE, "echolocation", "soul_sight", "danger_heartbeat", "creeper_ears", "trap_sense", "cave_eyes", "moon_glow", "pumpkin_vision", "health_peek", "ore_chime",
                "sniffer_nose", "debris_nose", "light_meter", "horse_whisperer", "weather_nose", "no_phantoms", "breadcrumbs", "pocket_light", "backwards_compass");
        moves(Move.STALK, "low_profile");
        moves(Move.HEAL, "home_sweet_home", "haybale_hugger", "snow_angel", "good_sleeper", "breakfast_bonus", "milk_mustache", "bread_lover", "soup_slurper", "raw_eater", "iron_stomach",
                "second_breakfast", "chefs_kiss", "pet_medic", "green_thumb", "tree_hugger", "noisy_eater");
        moves(Move.THROW, "pet_rock", "sneaky_snack", "ice_cube", "fortune_cookie", "spoon", "pocket_lint", "lucky_penny", "lucky_dip", "pebble_collector", "sling", "spare_arrow", "mob_bait");
        moves(Move.DISARM, "pickpocket", "magnet_hands", "long_arms");
        moves(Move.RALLY, "taunt", "meat_shield", "animal_tamer", "bird_brain", "bee_keeper", "good_haggler");
        moves(Move.BLINK, "afterimage", "recall", "mark_return", "ender_pocket");
        moves(Move.FROST, "ice_breath");
        moves(Move.GATHER, "chain_miner", "lucky_miner", "fortune_finger", "glass_hands", "timber", "double_xp", "xp_magnet", "seed_finder", "apple_luck", "sapling_luck", "forager",
                "lucky_clover", "treasure_fisher", "fish_whisperer", "bare_hand_fisher", "quick_shears", "wool_grower", "silkworm", "copper_polish", "pocket_table");
        moves(Move.SHOUT, "echo", "narrator", "snail_mail", "sticky_note", "rumbly_tummy", "paper_cut", "microwave_ding");
    }

    public static Move of(String id) {
        return MOVES.getOrDefault(id, Move.GATHER);
    }

    public static boolean known(String id) {
        return MOVES.containsKey(id);
    }

    public static int count() {
        return MOVES.size();
    }

    private MobMoves() {}
}
