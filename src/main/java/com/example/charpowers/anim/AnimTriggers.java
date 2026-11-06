package com.example.charpowers.anim;

import com.example.charpowers.armour.SuitFlight;
import com.example.charpowers.armour.SuitItems;
import com.example.charpowers.CharPowers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

// Which pose goes with which key of Char's own powers: one table, so a new key is one new line. Moments that aren't a key press
// (a clone appearing, a suit landing) call AnimNet.play where they happen.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class AnimTriggers {
    private enum When {
        ALWAYS,    // pressing it is the gesture, whatever comes of it
        IF_SPENT,  // Green Lantern: only if the ring really did something, so no battery or no target means no gesture
        IF_FLYING  // the suit: only if it's really on its way
    }

    private record Row(String pose, When when) {}

    private static final Map<String, Row> ON_KEY = new HashMap<>();
    private static final Map<String, Row> ON_RELEASE = new HashMap<>();
    private static boolean spent;

    private static void key(String power, String ability, String pose) {
        ON_KEY.put(power + "/" + ability, new Row(pose, When.ALWAYS));
    }

    private static void key(String power, String ability, String pose, When when) {
        ON_KEY.put(power + "/" + ability, new Row(pose, when));
    }

    static {
        // Green Lantern: forming a construct is the ring thrust unless it has its own
        key("green_lantern", "rising_fist", "ring_thrust", When.IF_SPENT);
        key("green_lantern", "backhand_slap", "lantern_slap", When.IF_SPENT);
        key("green_lantern", "hand_slam", "hand_slam", When.IF_SPENT);
        key("green_lantern", "hand_throw", "lantern_grab", When.IF_SPENT);
        key("green_lantern", "construct_sword", "ring_thrust", When.IF_SPENT);
        key("green_lantern", "launch_swords", "ring_raise", When.IF_SPENT);
        ON_RELEASE.put("green_lantern/launch_swords", new Row("ring_thrust", When.IF_SPENT));
        key("green_lantern", "turret", "hands_out", When.IF_SPENT);
        key("green_lantern", "bubble", "hands_out", When.IF_SPENT);
        key("green_lantern", "nuke", "ring_raise", When.IF_SPENT);
        key("green_lantern", "fist_field", "hand_slam", When.IF_SPENT);
        key("green_lantern", "sword_rain", "ring_raise", When.IF_SPENT);
        // Invincible Armour
        // calling and sending away the suit have their own poses (suit_equip / suit_unequip), played by SuitFlight itself
        key("invincible_armour", "throw_mob", "lantern_throw");
        // the others' main keys
        for (String order : new String[] {"stop", "no_jump", "freeze", "kneel", "look_at_me", "attack", "die"}) key("superior_authority", order, "command_point");
        key("mob_control", "command_mob", "command_point");
        key("mob_control", "pet_follower", "whistle");
        key("mob_control", "ride_mob", "whistle");
        key("mob_control", "swap_minds", "temple");
        for (String raise : new String[] {"summon_zombies", "summon_skeletons", "chosen_revival", "undead_riot"}) key("necromancy", raise, "raise_dead");
        key("necromancy", "undead_explosion", "command_point");
        key("necromancy", "point_undead", "command_point");
        for (String clock : new String[] {"slow_down", "hurry_up", "let_time_flow", "time_block", "still_moment"}) key("time_control", clock, "clock_hand");
        key("the_miner", "motherlode", "ground_slam");
        key("the_miner", "strip_mine", "ground_slam");
        key("the_miner", "mine_ball", "ground_slam");
        key("future_sight", "see_through", "temple");
        key("future_sight", "hush", "temple");
        key("book_keeping", "open_book", "reading");
        key("teacher", "teach", "raised_finger");
        key("life_enjoyer", "chest_sorter", "hands_out");
        key("game_player", "gp_fear", "command_point");
    }

    private AnimTriggers() {}

    public static void beforeKey() {
        spent = false;
    }

    // Green Lantern's ring paid for something.
    public static void ringSpent() {
        spent = true;
    }

    public static void afterKey(ServerPlayer p, ResourceLocation power, String ability) {
        send(p, ON_KEY.get(power.getPath() + "/" + ability));
    }

    public static void afterRelease(ServerPlayer p, ResourceLocation power, String ability) {
        send(p, ON_RELEASE.get(power.getPath() + "/" + ability));
    }

    private static void send(ServerPlayer p, Row row) {
        if (row == null) return;
        boolean go = switch (row.when()) {
            case ALWAYS -> true;
            case IF_SPENT -> spent;
            case IF_FLYING -> SuitFlight.busy(p.getUUID());
        };
        if (go) AnimNet.play(p, row.pose());
    }

    // Coming down in the suit: the stomp, if the drop was worth it.
    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (e.getDistance() > 3 && e.getEntity() instanceof ServerPlayer p && SuitItems.fullSuit(p)) AnimNet.play(p, "suit_stomp");
    }
}
