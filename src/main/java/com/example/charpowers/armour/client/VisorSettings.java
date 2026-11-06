package com.example.charpowers.armour.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Which parts of the suit's visor you see, in config/charpowers/suit_visor.json (your PC only). Three presets to start from.
public final class VisorSettings {
    public record Part(String key, String label, String what) {}

    public static final List<Part> PARTS = List.of(
            new Part("visor", "Visor", "The whole visor. Off and none of the rest shows."),
            new Part("frame", "Visor frame", "A faint curved frame round the edges of the screen."),
            new Part("heading", "Heading strip", "A compass strip along the top."),
            new Part("battery", "Battery", "The suit's charge."),
            new Part("modules", "Modules", "An icon for each module, lit when it is working."),
            new Part("state", "Suit state", "Normal, or Going Crazy with a red pulse."),
            new Part("station", "Station arrow", "Which way and how far your charging station is, when it is far or the battery is low."),
            new Part("mastery", "Mastery", "Your level and how far to the next unlock."),
            new Part("speed", "Speed and height", "How fast you are going and how high you are."),
            new Part("target", "Target box", "Name and health of what you are looking at."),
            new Part("threats", "Threats", "The three nearest hostile mobs and how far they are."),
            new Part("scanner", "Scanner markers", "With the scanner module: mobs marked on the heading strip."),
            new Part("armour", "Armour and hits", "Armour points, and a red flash on the side you were hit from."),
            new Part("oxygen", "Oxygen", "Your air, only when you are under water."),
            new Part("quest", "Quest", "The nuke quest, once you have reached mastery 100."),
            new Part("clock", "Time and place", "Time of day and your coordinates in one small line."));

    private static final Set<String> MINIMAL = Set.of("visor", "frame", "heading", "battery");
    private static final Set<String> STANDARD = Set.of("visor", "frame", "heading", "battery", "modules", "state", "station", "mastery", "target", "armour", "oxygen", "speed");

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("charpowers/suit_visor.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, Boolean> on;

    private VisorSettings() {}

    private static void load() {
        on = new LinkedHashMap<>();
        for (Part p : PARTS) on.put(p.key(), STANDARD.contains(p.key()));
        try (Reader r = Files.newBufferedReader(FILE)) {
            JsonObject o = GSON.fromJson(r, JsonObject.class);
            if (o != null) for (Part p : PARTS) if (o.has(p.key())) on.put(p.key(), o.get(p.key()).getAsBoolean());
        } catch (Exception ignored) {
            // no file yet, or one we can't read: the standard preset
        }
    }

    public static boolean has(String key) {
        if (on == null) load();
        return on.getOrDefault(key, false);
    }

    public static void set(String key, boolean value) {
        if (on == null) load();
        on.put(key, value);
        save();
    }

    public static void preset(String name) {
        if (on == null) load();
        for (Part p : PARTS) {
            boolean v = switch (name) {
                case "minimal" -> MINIMAL.contains(p.key());
                case "standard" -> STANDARD.contains(p.key());
                default -> true;
            };
            on.put(p.key(), v);
        }
        save();
    }

    // Which preset the current choice is exactly, or "" when it is a mix.
    public static String currentPreset() {
        for (String name : new String[] {"minimal", "standard", "full"}) {
            boolean same = true;
            for (Part p : PARTS) {
                boolean want = switch (name) {
                    case "minimal" -> MINIMAL.contains(p.key());
                    case "standard" -> STANDARD.contains(p.key());
                    default -> true;
                };
                if (has(p.key()) != want) same = false;
            }
            if (same) return name;
        }
        return "";
    }

    private static void save() {
        JsonObject o = new JsonObject();
        for (Map.Entry<String, Boolean> e : on.entrySet()) o.addProperty(e.getKey(), e.getValue());
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(o, w);
            }
        } catch (Exception ignored) {
            // a setting that can't be saved is just not remembered
        }
    }
}
