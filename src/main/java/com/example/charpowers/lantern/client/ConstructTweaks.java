package com.example.charpowers.lantern.client;

import com.example.charpowers.CharPowers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

// What /constructs changes: where each construct sits, how it's turned, its size, and each animation's speed. It's a plain file on
// the PC, read at start and saved on every change, so the game shows it straight away and the numbers can be built in.
public final class ConstructTweaks {
    public static final class Tweak {
        public double x, y, z, turn, tilt, roll, size = 1;
    }

    private static final class Saved {
        Map<String, Tweak> kinds = new TreeMap<>();
        Map<String, Double> speeds = new TreeMap<>();
        int version;
    }

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("charpowers/construct_tweaks.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Tweak NONE = new Tweak();
    private static Saved saved = new Saved();

    private ConstructTweaks() {}

    public static Path file() {
        return FILE;
    }

    static void load() {
        try {
            if (!Files.exists(FILE)) return;
            Saved read = GSON.fromJson(Files.readString(FILE), Saved.class);
            if (read == null) return;
            if (read.kinds == null) read.kinds = new TreeMap<>();
            if (read.speeds == null) read.speeds = new TreeMap<>();
            // A greatsword turned round by 160 degrees in an early tuning is what made it swing back towards you: put it right once
            if (read.version < 1) {
                Tweak sword = read.kinds.get("greatsword");
                if (sword != null && Math.abs(sword.turn - 160) < 0.5) sword.turn = 0;
                read.version = 1;
                saved = read;
                save();
            }
            saved = read;
        } catch (IOException | RuntimeException e) {
            CharPowers.LOG.warn("Couldn't read {}", FILE, e);
        }
    }

    static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(saved));
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't write {}", FILE, e);
        }
    }

    static Tweak of(String kind) {
        return saved.kinds.getOrDefault(kind, NONE);
    }

    static Tweak edit(String kind) {
        return saved.kinds.computeIfAbsent(kind, k -> new Tweak());
    }

    static double speed(String kind, String animation) {
        return saved.speeds.getOrDefault(kind + "/" + animation, 1.0);
    }

    static void setSpeed(String kind, String animation, double speed) {
        saved.speeds.put(kind + "/" + animation, speed);
    }

    static void reset(String kind) {
        saved.kinds.remove(kind);
        saved.speeds.keySet().removeIf(key -> key.startsWith(kind + "/"));
    }
}
