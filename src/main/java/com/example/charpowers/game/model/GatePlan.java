package com.example.charpowers.game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// What a gate is made of, with no game code in it: its theme, the rooms in a row (each with its own size, shape and goal), who lives there
// and what it pays. GateRun builds this out of blocks. Higher ranks see more themes and tougher creatures; the weakest themes fade out.
public final class GatePlan {
    public enum Theme { GOBLIN_CAVE, ELF_RUIN, ICE_KEEP, ANT_NEST, DRAGON_KEEP, WOLF_FOREST, DEMON_CASTLE, CRYPT, MARSH, DESERT_TOMB, STATION }

    // ARENA = kill everything; WAVES = survive the waves; SHAMAN = kill the glowing leader and the pack loses heart; TRAP = cross a hall of arrow slits,
    // then beat the ambush; KEY = a fight where a marked carrier holds the key to the loot alcove; TERMINAL = stand at the terminal until it is done while
    // things come for you; PHYLACTERY = smash the soul jars that keep the dead coming; MAZE = wind through walls to the beast at the end; BOSS = the last room.
    public enum Kind { ARENA, WAVES, SHAMAN, TRAP, KEY, TERMINAL, PHYLACTERY, MAZE, BOSS }

    public enum Shape { SQUARE, ROUND, CROSS, PILLARS, HALL }

    // For a TERMINAL room, waves is the seconds to hold; for a PHYLACTERY room, how many jars.
    public record Room(Kind kind, Shape shape, int halfX, int halfZ, int height, int waves, boolean loot) {}

    public static final int CORRIDOR = 5;

    private GatePlan() {}

    public static String title(Theme t) {
        return switch (t) {
            case GOBLIN_CAVE -> "Goblin Cave";
            case ELF_RUIN -> "Elf Ruin";
            case ICE_KEEP -> "Ice Keep";
            case ANT_NEST -> "Ant Nest";
            case DRAGON_KEEP -> "Dragon Keep";
            case WOLF_FOREST -> "Wolf Forest";
            case DEMON_CASTLE -> "Demon Castle";
            case CRYPT -> "Crypt";
            case MARSH -> "Marsh";
            case DESERT_TOMB -> "Desert Tomb";
            case STATION -> "Derelict Station";
        };
    }

    // The plain system-style warning shown under a gate on the GATES tab.
    public static String warning(Theme t) {
        return switch (t) {
            case GOBLIN_CAVE -> "Warning: packs ambush in the dark.";
            case ELF_RUIN -> "Warning: archers and mages hold the high ground.";
            case ICE_KEEP -> "Warning: freezing cold. Hits slow you down.";
            case ANT_NEST -> "Warning: acid and poison. Bring antidotes.";
            case DRAGON_KEEP -> "Warning: a dragon guards the keep. Bring fire protection.";
            case WOLF_FOREST -> "Warning: fast packs. Keep your back to a wall.";
            case DEMON_CASTLE -> "Warning: fire and heavy charges ahead.";
            case CRYPT -> "Warning: the dead rise again. Smash what anchors them.";
            case MARSH -> "Warning: poison and webs punish the careless.";
            case DESERT_TOMB -> "Warning: curses, traps and sandstorms.";
            case STATION -> "Warning: lasers and shielded machines. Hold the terminal.";
        };
    }

    // The lowest and highest rank a theme turns up in (rank 0 = E ... 5 = S).
    public static int minRank(Theme t) {
        return switch (t) {
            case GOBLIN_CAVE, ANT_NEST, WOLF_FOREST -> 0;
            case MARSH, ELF_RUIN, CRYPT -> 1;
            case DESERT_TOMB, ICE_KEEP -> 2;
            case STATION, DEMON_CASTLE -> 3;
            case DRAGON_KEEP -> 4;
        };
    }

    public static int maxRank(Theme t) {
        return switch (t) {
            case GOBLIN_CAVE, ANT_NEST -> 3;
            case WOLF_FOREST, MARSH -> 4;
            default -> 5;
        };
    }

    public static List<Theme> eligible(int rank) {
        rank = GateRules.clampRank(rank);
        List<Theme> out = new ArrayList<>();
        for (Theme t : Theme.values()) {
            if (rank >= minRank(t) && rank <= maxRank(t)) out.add(t);
        }
        return out;
    }

    // A sealed gate (the one you cannot leave) is a hard one: from rank A it is the dragon's keep.
    public static Theme themeFor(int rank, Random r, boolean sealed) {
        rank = GateRules.clampRank(rank);
        if (sealed && rank >= 4) return Theme.DRAGON_KEEP;
        List<Theme> pool = eligible(rank);
        final int floor = Math.max(0, rank - 2);
        if (sealed) pool.removeIf(t -> minRank(t) < floor);
        if (pool.isEmpty()) pool = eligible(rank);
        return pool.get(r.nextInt(pool.size()));
    }

    // The rooms in order. The first is a fight, the last the boss, and each odd goal turns up at most once, more often where the theme suits it.
    public static List<Room> plan(int rank, Theme theme, Random r) {
        rank = GateRules.clampRank(rank);
        int n = GateRules.rooms(rank);
        List<Room> out = new ArrayList<>();
        List<Kind> used = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i == n - 1) {
                out.add(bossRoom(theme));
                break;
            }
            Kind kind = i == 0 ? (r.nextInt(10) < 6 ? Kind.ARENA : Kind.WAVES) : pickKind(theme, rank, used, r);
            used.add(kind);
            out.add(room(kind, r, rank));
        }
        return out;
    }

    private static Kind pickKind(Theme theme, int rank, List<Kind> used, Random r) {
        List<Kind> bag = new ArrayList<>();
        add(bag, Kind.ARENA, 3);
        add(bag, Kind.WAVES, 3);
        add(bag, Kind.SHAMAN, 2);
        if (rank >= 1 && !used.contains(Kind.KEY)) add(bag, Kind.KEY, 2);
        if (rank >= 1 && !used.contains(Kind.TRAP)) add(bag, Kind.TRAP, theme == Theme.DESERT_TOMB ? 5 : 1);
        if (rank >= 3 && !used.contains(Kind.MAZE)) add(bag, Kind.MAZE, 1);
        if (rank >= 2 && theme == Theme.STATION && !used.contains(Kind.TERMINAL)) add(bag, Kind.TERMINAL, 6);
        if (rank >= 1 && theme == Theme.CRYPT && !used.contains(Kind.PHYLACTERY)) add(bag, Kind.PHYLACTERY, 6);
        return bag.get(r.nextInt(bag.size()));
    }

    private static void add(List<Kind> bag, Kind k, int weight) {
        for (int i = 0; i < weight; i++) bag.add(k);
    }

    private static Room bossRoom(Theme theme) {
        Shape shape = theme == Theme.ICE_KEEP || theme == Theme.GOBLIN_CAVE || theme == Theme.WOLF_FOREST ? Shape.ROUND : Shape.PILLARS;
        return switch (theme) {
            case DRAGON_KEEP -> new Room(Kind.BOSS, Shape.ROUND, 15, 14, 15, 1, false);
            case STATION -> new Room(Kind.BOSS, Shape.SQUARE, 13, 12, 11, 1, false);
            default -> new Room(Kind.BOSS, shape, 11, 11, 10, 1, false);
        };
    }

    private static Room room(Kind kind, Random r, int rank) {
        return switch (kind) {
            case TRAP -> new Room(kind, Shape.HALL, 13 + r.nextInt(4), 3, 6, 1, false);
            case WAVES -> new Room(kind, r.nextBoolean() ? Shape.ROUND : Shape.SQUARE, 8 + r.nextInt(2), 8 + r.nextInt(2), 8, 2 + r.nextInt(3), false);
            case SHAMAN -> new Room(kind, Shape.PILLARS, 9 + r.nextInt(3), 8 + r.nextInt(3), 8, 1, false);
            case KEY -> new Room(kind, Shape.CROSS, 9 + r.nextInt(2), 9 + r.nextInt(2), 8, 1, true);
            case TERMINAL -> new Room(kind, Shape.SQUARE, 9, 9, 8, 22 + rank * 3, false);
            case PHYLACTERY -> new Room(kind, Shape.ROUND, 10, 10, 9, 3, false);
            case MAZE -> new Room(kind, Shape.SQUARE, 11, 7, 6, 1, false);
            default -> new Room(kind, r.nextBoolean() ? Shape.SQUARE : Shape.ROUND, 6 + r.nextInt(5), 6 + r.nextInt(5), 7 + r.nextInt(2), 1, false);
        };
    }

    // x of each room's middle, relative to the first room's middle, with rooms in a row along +x and CORRIDOR blocks between their doors.
    public static int[] centres(List<Room> rooms) {
        int[] out = new int[rooms.size()];
        for (int i = 1; i < out.length; i++) out[i] = out[i - 1] + rooms.get(i - 1).halfX() + rooms.get(i).halfX() + CORRIDOR + 1;
        return out;
    }

    // Is this spot (relative to the room's middle) floor you can stand on? The door cells at the two ends count, so the way through is always open.
    public static boolean walkable(Room room, int x, int z) {
        int hx = room.halfX(), hz = room.halfZ();
        if (Math.abs(z) <= 1 && Math.abs(x) <= hx) return true; // the lane from door to door
        if (Math.abs(x) >= hx || Math.abs(z) >= hz) return false;
        return switch (room.shape()) {
            case ROUND -> {
                double a = x / (double) (hx - 0.5), b = z / (double) (hz - 0.5);
                yield a * a + b * b <= 1;
            }
            case CROSS -> Math.abs(x) <= hx / 3 || Math.abs(z) <= hz / 3;
            default -> true;
        };
    }

    // A pillar or low block inside a room, as (x, z, tall) relative to the middle, kept off the lane through the middle and off the walls.
    public static List<int[]> cover(Room room, Random r) {
        int count = switch (room.shape()) {
            case HALL -> 0;
            case PILLARS -> 6 + r.nextInt(4);
            case CROSS -> 2 + r.nextInt(3 - 1);
            default -> 2 + r.nextInt(3);
        };
        if (room.kind() == Kind.BOSS) count = 4;
        if (room.kind() == Kind.MAZE || room.kind() == Kind.TERMINAL || room.kind() == Kind.PHYLACTERY) count = 0;
        List<int[]> out = new ArrayList<>();
        for (int tries = 0; tries < 60 && out.size() < count; tries++) {
            int x = r.nextInt(2 * room.halfX() - 7) - room.halfX() + 4, z = r.nextInt(2 * room.halfZ() - 7) - room.halfZ() + 4;
            if (Math.abs(z) <= 2 || !walkable(room, x, z) || !walkable(room, x + 1, z + 1) || !walkable(room, x - 1, z - 1)) continue;
            boolean close = false;
            for (int[] o : out) close |= Math.abs(o[0] - x) < 4 && Math.abs(o[1] - z) < 4;
            if (!close) out.add(new int[]{x, z, r.nextInt(3) == 0 ? 1 : 0});
        }
        return out;
    }

    // The walls of a maze room as {x, zFrom, zTo}: a wall every 4 blocks across the room with a 3-wide gap, alternating ends, so the way winds from side to side.
    public static List<int[]> mazeWalls(Room room) {
        List<int[]> out = new ArrayList<>();
        int hz = room.halfZ(), k = 0;
        for (int x = -room.halfX() + 5; x <= room.halfX() - 5; x += 4, k++) {
            if (k % 2 == 0) out.add(new int[]{x, -hz + 1, hz - 4}); // gap at the +z end
            else out.add(new int[]{x, -hz + 4, hz - 1}); // gap at the -z end
        }
        return out;
    }

    // Entity ids that live in this theme at this rank ("id@lowest rank" in the table). Never empty. Tougher things only turn up higher.
    public static List<String> mobs(Theme theme, int rank) {
        rank = GateRules.clampRank(rank);
        String[] table = switch (theme) {
            case GOBLIN_CAVE -> new String[]{"goblin@0", "goblin@0", "goblin_shaman@0", "hobgoblin@1", "orc@2"};
            case ELF_RUIN -> new String[]{"elf@0", "elf@0", "dark_elf@1", "goblin@1", "gargoyle@3", "stone_golem@3"};
            case ICE_KEEP -> new String[]{"ice_archer@0", "ice_archer@0", "minecraft:stray@0", "ice_bear@2", "ice_bear@3"};
            case ANT_NEST -> new String[]{"ant_soldier@0", "ant_soldier@0", "ant_soldier@0", "minecraft:cave_spider@0", "giant_spider@2"};
            case DRAGON_KEEP -> new String[]{"orc@0", "hobgoblin@0", "skeleton_knight@0", "dark_elf@0", "demon_knight@4", "gargoyle@4"};
            case WOLF_FOREST -> new String[]{"dire_wolf@0", "dire_wolf@0", "dire_wolf@0", "alpha_wolf@1", "mushroom_man@1", "elf@2"};
            case DEMON_CASTLE -> new String[]{"demon@0", "demon@0", "imp@0", "imp@0", "demon_knight@3", "gargoyle@4"};
            case CRYPT -> new String[]{"skeleton_knight@0", "skeleton_knight@0", "ghoul@0", "wraith@2", "lich@3", "ghoul@3"};
            case MARSH -> new String[]{"lizardman@0", "lizardman@0", "snake_warrior@0", "snake_warrior@0", "slime_knight@0", "giant_spider@2"};
            case DESERT_TOMB -> new String[]{"mummy@0", "mummy@0", "scarab@0", "scarab@0", "scarab@0", "skeleton_knight@2", "sand_golem@3"};
            case STATION -> new String[]{"sentry_drone@0", "cyber_soldier@0", "cyber_soldier@0", "alien_crawler@0", "alien_crawler@0", "alien@3", "bulwark_mech@4"};
        };
        List<String> out = new ArrayList<>();
        for (String entry : table) {
            String[] parts = entry.split("@");
            if (rank >= Integer.parseInt(parts[1])) out.add(parts[0].contains(":") ? parts[0] : "charpowers:" + parts[0]);
        }
        return out;
    }

    // Surprises any theme can roll now and then.
    public static List<String> general(int rank) {
        rank = GateRules.clampRank(rank);
        List<String> out = new ArrayList<>(List.of("charpowers:mimic", "charpowers:slime_knight"));
        if (rank >= 1) out.add("charpowers:mushroom_man");
        if (rank >= 2) out.add("charpowers:gargoyle");
        if (rank >= 4) out.add("charpowers:slime_king");
        return out;
    }

    // The marked leader of a SHAMAN room ("kill the glowing leader").
    public static String leader(Theme theme) {
        return switch (theme) {
            case GOBLIN_CAVE -> "charpowers:goblin_shaman";
            case ELF_RUIN -> "charpowers:dark_elf";
            case ICE_KEEP -> "charpowers:ice_bear";
            case ANT_NEST -> "charpowers:ant_soldier";
            case DRAGON_KEEP, DEMON_CASTLE -> "charpowers:demon_knight";
            case WOLF_FOREST -> "charpowers:alpha_wolf";
            case CRYPT -> "charpowers:lich";
            case MARSH -> "charpowers:snake_warrior";
            case DESERT_TOMB -> "charpowers:mummy";
            case STATION -> "charpowers:alien";
        };
    }

    public static String boss(Theme theme, int rank, Random r) {
        return switch (theme) {
            case GOBLIN_CAVE -> rank >= 2 && r.nextBoolean() ? "charpowers:hobgoblin_warlord" : "charpowers:goblin_king";
            case ELF_RUIN -> "charpowers:elf_mage";
            case ICE_KEEP -> "charpowers:frost_king";
            case ANT_NEST -> "charpowers:ant_queen";
            case DRAGON_KEEP -> "charpowers:young_dragon";
            case WOLF_FOREST -> "charpowers:wolf_king";
            case DEMON_CASTLE -> "charpowers:demon_lord";
            case CRYPT -> "charpowers:lich_lord";
            case MARSH -> "charpowers:snake_queen";
            case DESERT_TOMB -> "charpowers:tomb_king";
            case STATION -> "charpowers:war_mech";
        };
    }

    // How many creatures a room holds: more in higher ranks, deeper rooms and bigger parties.
    public static int count(int rank, int roomIndex, int party, Kind kind) {
        double base = GateRules.mobsInRoom(rank, roomIndex);
        if (kind == Kind.WAVES) base *= 0.6; // each of the waves
        if (kind == Kind.SHAMAN || kind == Kind.KEY || kind == Kind.PHYLACTERY) base *= 0.8;
        if (kind == Kind.MAZE) base *= 0.4;
        return Math.max(2, (int) Math.round(base * (1 + 0.6 * (Math.max(1, party) - 1))));
    }

    public static double partyMobHealth(int party) {
        return 1 + 0.25 * (Math.max(1, party) - 1);
    }

    public record Item(String id, int count) {}

    // What a chest in a gate holds: a few stacks, better for higher ranks.
    public static List<Item> chestLoot(int rank, Random r, int stacks) {
        rank = GateRules.clampRank(rank);
        String[][] tiers = {
                {"minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:bread", "minecraft:arrow", "minecraft:coal"},
                {"minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:emerald", "minecraft:cooked_beef", "minecraft:redstone"},
                {"minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot", "minecraft:experience_bottle", "minecraft:golden_apple"},
                {"minecraft:diamond", "minecraft:emerald", "minecraft:experience_bottle", "minecraft:golden_apple", "minecraft:ender_pearl"},
                {"minecraft:diamond", "minecraft:netherite_scrap", "minecraft:experience_bottle", "minecraft:golden_apple", "minecraft:ender_pearl"},
                {"minecraft:diamond", "minecraft:netherite_scrap", "minecraft:enchanted_golden_apple", "minecraft:experience_bottle", "minecraft:totem_of_undying"}};
        int[][] amounts = {{4, 8}, {4, 8}, {2, 5}, {2, 6}, {2, 6}, {2, 5}};
        List<Item> out = new ArrayList<>();
        for (int i = 0; i < stacks; i++) {
            String id = tiers[rank][r.nextInt(tiers[rank].length)];
            int a = amounts[rank][0], b = amounts[rank][1];
            int count = a + r.nextInt(b - a + 1);
            if (id.contains("enchanted_golden") || id.contains("totem") || id.contains("netherite")) count = 1;
            out.add(new Item(id, count));
        }
        return out;
    }
}
