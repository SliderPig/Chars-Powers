package com.example.charpowers.client.anim;

import com.example.charpowers.CharPowers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

// Your own PC's choice: player animations on or off, in config/charpowers/player_animations.json. Off means everyone on your screen
// keeps the game's plain poses; other people's screens don't change.
public final class AnimSettings {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("charpowers/player_animations.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Boolean on;

    private AnimSettings() {}

    public static boolean on() {
        if (on == null) load();
        return on;
    }

    public static void set(boolean value) {
        on = value;
        JsonObject o = new JsonObject();
        o.addProperty("enabled", value);
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(o, w);
            }
        } catch (Exception e) {
            CharPowers.LOG.error("Couldn't save {}", FILE, e);
        }
    }

    private static void load() {
        on = true;
        if (!Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE)) {
            JsonObject o = GSON.fromJson(r, JsonObject.class);
            if (o != null && o.has("enabled")) on = o.get("enabled").getAsBoolean();
        } catch (Exception e) {
            CharPowers.LOG.error("Couldn't read {}", FILE, e);
        }
    }
}
