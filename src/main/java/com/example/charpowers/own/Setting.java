package com.example.charpowers.own;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.model.SuitRules;
import com.example.charpowers.authority.model.AuthorityRules;
import com.example.charpowers.book.model.BookRules;
import com.example.charpowers.cloning.model.CloneRules;
import com.example.charpowers.deathsteal.model.StealRules;
import com.example.charpowers.futuresight.model.SightRules;
import com.example.charpowers.game.model.GameRules;
import com.example.charpowers.lantern.model.LanternRules;
import com.example.charpowers.life.model.LifeRules;
import com.example.charpowers.miner.model.MinerRules;
import com.example.charpowers.mobcontrol.model.MobRules;
import com.example.charpowers.necromancy.model.UndeadRules;
import com.example.charpowers.teacher.model.TeacherRules;
import com.example.charpowers.timecontrol.model.TimeRules;
import com.example.charpowers.vocal.model.WordRules;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// The numbers an op can change in the menu (/charpower menu). The defaults are each power's own rules. Anything changed is kept in
// config/charpowers/power_settings.json on the server. Every power also has an on/off and a mastery speed.
public final class Setting {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("charpowers/power_settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, List<Setting>> BY_POWER = new TreeMap<>();
    private static Map<String, Double> values = new TreeMap<>();

    static {
        try {
            if (Files.exists(FILE)) {
                Map<String, Double> read = GSON.fromJson(Files.readString(FILE), new TypeToken<TreeMap<String, Double>>() {}.getType());
                if (read != null) values = read;
            }
        } catch (IOException | RuntimeException e) {
            CharPowers.LOG.warn("Couldn't read {}", FILE, e);
        }
    }

    public static final Setting SUIT_NUKE_ONE_IN = of("invincible_armour", "nuke_one_in", "Nuke quest: 1 in (a second)", SuitRules.NUKE_ONE_IN, 1, 100000, 100);
    public static final Setting SUIT_NUKE_DAMAGE = of("invincible_armour", "nuke_damage", "Nuke quest damage", SuitRules.NUKE_DAMAGE, 0, 1000, 10);
    public static final Setting AUTHORITY_DIE_RADIUS = of("superior_authority", "die_radius", "Die order reach (blocks)", AuthorityRules.DIE_RADIUS, 1, 512, 10);
    public static final Setting BOOK_MAX_KILLS = of("book_keeping", "max_kills", "Kills a page can undo", BookRules.MAX_KILLS, 0, 5000, 64);
    public static final Setting STEAL_DAILY_ONE_IN = of("death_steal", "daily_one_in", "Awakens: 1 in (a day)", StealRules.DAILY_ONE_IN, 1, 100000, 100);
    public static final Setting STEAL_TIME_LIMIT = of("death_steal", "time_limit", "Black room time (ticks)", StealRules.TIME_LIMIT_TICKS, 200, 72000, 600);
    public static final Setting SIGHT_GLOW_LEVEL = of("future_sight", "glow_level", "Ores glow from mastery", SightRules.GLOW_LEVEL, 1, 100, 5);
    public static final Setting SIGHT_HINT_GAP = of("future_sight", "hint_gap", "Between ore hints (ticks)", SightRules.HINT_GAP_TICKS, 20, 6000, 20);
    public static final Setting GAME_ROOM_CHANCE = of("game_player", "room_chance", "Gate: 1 in (a dungeon)", GameRules.ROOM_CHANCE, 1, 100000, 100);
    public static final Setting GAME_OPEN_GAP = of("game_player", "open_gap", "Between opening gates (ticks)", 6000, 0, 72000, 600);
    public static final Setting GAME_GATE_GAP = of("game_player", "gate_gap", "Between gates appearing (ticks)", 6000, 200, 72000, 600);
    public static final Setting GAME_POINTS = of("game_player", "points", "Quest points %", 100, 0, 1000, 10);
    public static final Setting LANTERN_DAMAGE = of("green_lantern", "damage", "Construct damage %", 100, 0, 1000, 10);
    public static final Setting LANTERN_COST = of("green_lantern", "cost", "Battery cost %", 100, 0, 1000, 10);
    public static final Setting LANTERN_NUKE_RADIUS = of("green_lantern", "nuke_radius", "Nuke reach (blocks)", LanternRules.NUKE_RADIUS, 1, 512, 10);
    public static final Setting LANTERN_TURRET_RANGE = of("green_lantern", "turret_range", "Turret range (blocks)", LanternRules.TURRET_RANGE, 1, 128, 2);
    public static final Setting LIFE_MAGNET = of("life_enjoyer", "magnet", "Magnet reach (blocks)", LifeRules.MAGNET_RANGE, 1, 64, 1);
    public static final Setting LIFE_VEIN = of("life_enjoyer", "vein", "Vein blocks at once", LifeRules.VEIN_MAX, 1, 1024, 8);
    public static final Setting LIFE_ULTIMINE = of("life_enjoyer", "ultimine", "Ultimine blocks at once", 128, 1, 1024, 8);
    public static final Setting MINER_BALL = of("the_miner", "ball", "Ball of mining radius", MinerRules.BALL_RADIUS, 1, 48, 1);
    public static final Setting MINER_MOTHERLODE = of("the_miner", "motherlode", "Motherlode radius", MinerRules.MOTHERLODE_RADIUS, 1, 64, 1);
    public static final Setting MINER_EXTRA_ORE = of("the_miner", "extra_ore", "Extra ore chance", MinerRules.EXTRA_ORE_CHANCE, 0, 1, 0.05);
    public static final Setting MOB_REACH = of("mob_control", "reach", "Reach (blocks)", MobRules.REACH, 1, 64, 1);
    public static final Setting UNDEAD_REVIVAL = of("necromancy", "revival", "Revival chance", UndeadRules.REVIVAL_CHANCE, 0, 1, 0.05);
    public static final Setting UNDEAD_RIOT = of("necromancy", "riot", "Riot size", UndeadRules.RIOT_SIZE, 1, 500, 5);
    public static final Setting TEACHER_VILLAGERS = of("teacher", "villagers", "Villagers to get it", TeacherRules.VILLAGERS_NEEDED, 1, 1000, 5);
    public static final Setting TIME_SLOW = of("time_control", "slow", "Slow down reach (blocks)", TimeRules.SLOW_RADIUS, 1, 128, 4);
    public static final Setting TIME_HURRY = of("time_control", "hurry", "Hurry up reach (blocks)", TimeRules.HURRY_RADIUS, 1, 128, 4);
    public static final Setting TIME_BLOCK_COOLDOWN = of("time_control", "block_cooldown", "Time Block wait (ticks)", TimeRules.BLOCK_COOLDOWN, 0, 72000, 600);
    public static final Setting VOCAL_MESSAGES = of("vocal_expression", "messages", "Messages to get it", WordRules.MESSAGES_NEEDED, 1, 100000, 50);
    public static final Setting CLONE_AWAKENED_DAMAGE = of("cloning", "awakened_damage", "Awakened clone extra damage", CloneRules.AWAKENED_DAMAGE, 0, 100, 0.5);

    public static final Setting HOUSE_COOLDOWN = of("house_builder", "cooldown", "Between houses (ticks)", com.example.charpowers.house.model.HouseRules.COOLDOWN_TICKS, 0, 72000, 600);
    public static final Setting HOUSE_UNLOCK = of("house_builder", "unlock", "Blocks to get it", com.example.charpowers.house.model.HouseRules.UNLOCK_BLOCKS, 1, 1000000, 500);
    public static final Setting HOUSE_CASTLE = of("house_builder", "castle", "Castle blocks (yours)", com.example.charpowers.house.model.HouseRules.CASTLE_BLOCKS, 1, 40000, 200);
    public static final Setting HOUSE_SPEED = of("house_builder", "speed", "Building speed %", 100, 10, 1000, 10);

    public final String power, key, label;
    public final double def, min, max, step;

    private Setting(String power, String key, String label, double def, double min, double max, double step) {
        this.power = power;
        this.key = key;
        this.label = label;
        this.def = def;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    private static Setting of(String power, String key, String label, double def, double min, double max, double step) {
        Setting s = new Setting(power, key, label, def, min, max, step);
        BY_POWER.computeIfAbsent(power, p -> new ArrayList<>()).add(s);
        return s;
    }

    public static List<Setting> of(String power) {
        return BY_POWER.getOrDefault(power, List.of());
    }

    public double d() {
        return values.getOrDefault(power + "." + key, def);
    }

    public int i() {
        return (int) Math.round(d());
    }

    public long l() {
        return Math.round(d());
    }

    public float f() {
        return (float) d();
    }

    public void set(double value) {
        double clamped = Math.max(min, Math.min(max, Math.round(value * 1000) / 1000.0));
        if (clamped == def) values.remove(power + "." + key);
        else values.put(power + "." + key, clamped);
        save();
    }

    // ---- every power: on or off, and how fast its mastery goes

    public static boolean enabled(ResourceLocation power) {
        return values.getOrDefault(power.getPath() + ".enabled", 1.0) != 0;
    }

    public static void setEnabled(String power, boolean on) {
        if (on) values.remove(power + ".enabled");
        else values.put(power + ".enabled", 0.0);
        save();
    }

    public static double masteryRate(String power) {
        return values.getOrDefault(power + ".mastery_rate", 1.0);
    }

    public static void setMasteryRate(String power, double rate) {
        double clamped = Math.max(0, Math.min(100, Math.round(rate * 100) / 100.0));
        if (clamped == 1) values.remove(power + ".mastery_rate");
        else values.put(power + ".mastery_rate", clamped);
        save();
    }

    public static void reset(String power) {
        values.keySet().removeIf(key -> key.startsWith(power + "."));
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(values));
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't write {}", FILE, e);
        }
    }
}
