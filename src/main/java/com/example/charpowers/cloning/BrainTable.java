package com.example.charpowers.cloning;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.model.BrainRules.Role;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// What the clones know about every key ability: data/charpowers/clone_brain.json (from tools/make_brain_table.py),
// plus what they've found out for themselves, saved in config/charpowers/clone_brain_learned.json.
final class BrainTable {
    private static final int GIVE_UP_AFTER = 3;
    private static final String SKIP = "SKIP";
    private static JsonObject table;
    private static JsonObject learned;

    private BrainTable() {}

    private static JsonObject table() {
        if (table == null) {
            table = new JsonObject();
            try (InputStream in = BrainTable.class.getResourceAsStream("/data/charpowers/clone_brain.json")) {
                if (in != null) table = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            } catch (IOException e) {
                CharPowers.LOG.warn("Couldn't read the clones' ability table", e);
            }
        }
        return table;
    }

    private static Path learnedFile() {
        return FMLPaths.CONFIGDIR.get().resolve("charpowers").resolve("clone_brain_learned.json");
    }

    private static JsonObject learned() {
        if (learned == null) {
            learned = new JsonObject();
            Path file = learnedFile();
            try {
                if (Files.exists(file)) {
                    JsonObject read = new Gson().fromJson(Files.readString(file), JsonObject.class);
                    if (read != null) learned = read;
                }
            } catch (Exception e) {
                CharPowers.LOG.warn("Couldn't read {}", file, e);
            }
        }
        return learned;
    }

    @Nullable
    static JsonObject entry(String power, String ability) {
        JsonObject forPower = table().getAsJsonObject(power);
        return forPower == null ? null : forPower.getAsJsonObject(ability);
    }

    // The role to use, counting what clones have learned. null = leave it alone.
    @Nullable
    static Role role(String power, String ability, JsonObject entry) {
        Role role;
        try {
            role = Role.valueOf(entry.get("r").getAsString());
        } catch (RuntimeException e) {
            return null;
        }
        if (role != Role.UNKNOWN) return role;
        JsonObject known = learned().getAsJsonObject(power + "|" + ability);
        if (known == null || !known.has("role")) return role;
        String saved = known.get("role").getAsString();
        return SKIP.equals(saved) ? null : Role.valueOf(saved);
    }

    // What a try showed. Nothing seen three times = leave it alone from now on.
    static void record(String power, String ability, @Nullable Role seen) {
        JsonObject all = learned();
        String key = power + "|" + ability;
        JsonObject known = all.getAsJsonObject(key);
        if (known == null) {
            known = new JsonObject();
            all.add(key, known);
        }
        if (seen != null) {
            known.addProperty("role", seen.name());
        } else {
            int tries = known.has("tries") ? known.get("tries").getAsInt() + 1 : 1;
            known.addProperty("tries", tries);
            if (tries >= GIVE_UP_AFTER) known.addProperty("role", SKIP);
        }
        try {
            Files.createDirectories(learnedFile().getParent());
            Files.writeString(learnedFile(), all.toString());
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't save what the clones learned", e);
        }
    }

    static Map<String, Integer> props(JsonObject entry) {
        Map<String, Integer> out = new HashMap<>();
        JsonObject s = entry.getAsJsonObject("s");
        if (s != null) s.entrySet().forEach(e -> out.put(e.getKey(), e.getValue().getAsInt()));
        return out;
    }
}
