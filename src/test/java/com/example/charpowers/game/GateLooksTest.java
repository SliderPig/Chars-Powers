package com.example.charpowers.game;

import com.example.charpowers.game.model.BossRules;
import com.example.charpowers.game.model.GatePlan;
import com.example.charpowers.game.model.MobMove;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Every creature has a model with the humanoid skeleton, a picture and a glow picture, a loot table, a name and at least two moves; cubes sit inside the picture.
class GateLooksTest {
    private static final String[] SKELETON = {"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"};
    private static final Path ASSETS = Path.of("src/main/resources/assets/charpowers");
    private static final String[] BOSSES = {"goblin_king", "hobgoblin_warlord", "elf_mage", "ant_queen", "wolf_king", "demon_lord", "lich_lord", "snake_queen", "frost_king", "war_mech", "tomb_king", "young_dragon"};

    private static List<String> kinds() throws Exception {
        List<String> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(ASSETS.resolve("gate_models"))) {
            files.forEach(f -> out.add(f.getFileName().toString().replace(".json", "")));
        }
        return out;
    }

    @Test
    void everyKindIsComplete() throws Exception {
        List<String> all = kinds();
        assertTrue(all.size() >= 40, "only " + all.size());
        String lang = Files.readString(ASSETS.resolve("lang/en_us.json"));
        for (String k : all) {
            JsonObject json = JsonParser.parseString(Files.readString(ASSETS.resolve("gate_models/" + k + ".json"))).getAsJsonObject();
            JsonObject parts = json.getAsJsonObject("parts");
            for (String name : SKELETON) assertTrue(parts.has(name), k + " lacks " + name);
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/gate/" + k + ".png")), k);
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/gate/" + k + "_glow.png")), k);
            assertTrue(Files.exists(Path.of("src/main/resources/data/charpowers/loot_tables/entities/" + k + ".json")), k);
            assertTrue(lang.contains("\"entity.charpowers." + k + "\""), k + " has no name");
            assertTrue(lang.contains("\"item.charpowers." + k + "_spawn_egg\""), k + " has no egg name");
            checkCubes(k, parts, json.getAsJsonArray("tex").get(0).getAsInt());
        }
    }

    @Test
    void lootTablesAreValidJson() throws Exception {
        for (String k : kinds()) {
            JsonObject t = JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/charpowers/loot_tables/entities/" + k + ".json"))).getAsJsonObject();
            assertEquals("minecraft:entity", t.get("type").getAsString());
            assertTrue(t.getAsJsonArray("pools").size() >= 1, k);
        }
    }

    @Test
    void everyCreatureHasTwoMovesAndEveryBossToo() throws Exception {
        for (String k : kinds()) {
            if (java.util.Arrays.asList(BOSSES).contains(k)) {
                int most = 0;
                for (double d = 0; d < 40; d++) most = Math.max(most, BossRules.allowed(k.toUpperCase(), 3, d, 0).size());
                assertTrue(most >= 2, k + " has " + most + " attacks");
            } else {
                assertTrue(MobMove.of(k).size() >= 2, k + " needs at least two moves");
                for (MobMove m : MobMove.of(k)) assertTrue(m.tell >= 4 && m.cooldown >= 40, k + " " + m);
            }
        }
    }

    @Test
    void noSeriesLeftovers() throws Exception {
        String banned = "lycan|igris|baran|naga|statue|archmage|crimson|blood-red|blood_red|monarch|rift_overseer|xeno|tarantula|double dungeon|temple";
        for (String k : kinds()) assertFalse(k.matches(".*(" + banned + ").*"), k);
        String lang = Files.readString(ASSETS.resolve("lang/en_us.json")).toLowerCase();
        for (String line : lang.split("\n")) {
            if (line.contains("entity.charpowers.") || line.contains("_spawn_egg")) assertFalse(line.matches(".*(" + banned + ").*"), line);
        }
    }

    @Test
    void everyThemeMobExists() throws Exception {
        List<String> known = kinds();
        for (GatePlan.Theme t : GatePlan.Theme.values()) {
            for (int rank = 0; rank < 6; rank++) {
                for (String id : GatePlan.mobs(t, rank)) assertTrue(!id.startsWith("charpowers:") || known.contains(id.substring(11)), id);
                String boss = GatePlan.boss(t, rank, new Random(rank));
                assertTrue(known.contains(boss.substring(11)), boss);
                assertTrue(known.contains(GatePlan.leader(t).substring(11)), t.toString());
            }
        }
        for (int rank = 0; rank < 6; rank++) {
            for (String id : GatePlan.general(rank)) assertTrue(known.contains(id.substring(11)), id);
        }
    }

    private void checkCubes(String id, JsonObject parts, int size) {
        for (var e : parts.entrySet()) {
            JsonObject part = e.getValue().getAsJsonObject();
            for (var c : part.getAsJsonArray("cubes")) {
                JsonObject cube = c.getAsJsonObject();
                int u = cube.getAsJsonArray("tex").get(0).getAsInt(), v = cube.getAsJsonArray("tex").get(1).getAsInt();
                double w = cube.getAsJsonArray("size").get(0).getAsDouble(), hh = cube.getAsJsonArray("size").get(1).getAsDouble(), d = cube.getAsJsonArray("size").get(2).getAsDouble();
                assertTrue(u + 2 * (d + w) <= size && v + d + hh <= size, id + "/" + e.getKey() + " leaves the picture");
            }
            checkCubes(id, part.getAsJsonObject("children"), size);
        }
    }
}
