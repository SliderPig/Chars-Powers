package com.example.charpowers.game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Every quest there is. A quest counts one kind of thing, and the filter narrows it down by name ("zombie", "_ore").
public final class QuestBook {
    public record Quest(String kind, String filter, int count, int points, String text) {
        public boolean matches(String doing, String what) {
            return kind.equals(doing) && (filter.isEmpty() || what.contains(filter));
        }
    }

    private static Quest q(String kind, String filter, int count, int points, String text) {
        return new Quest(kind, filter, count, points, text);
    }

    public static final List<Quest> DAILY = List.of(
            q("kill", "", 15, 6, "Defeat 15 mobs"),
            q("kill", "zombie", 8, 5, "Defeat 8 zombies"),
            q("kill", "skeleton", 6, 5, "Defeat 6 skeletons"),
            q("kill", "creeper", 3, 6, "Defeat 3 creepers"),
            q("kill", "spider", 5, 5, "Defeat 5 spiders"),
            q("mine", "", 64, 4, "Break 64 blocks"),
            q("mine", "_ore", 10, 6, "Mine 10 ores"),
            q("mine", "_log", 32, 4, "Chop 32 logs"),
            q("craft", "", 16, 3, "Craft 16 things"),
            q("smelt", "", 16, 4, "Smelt 16 things"),
            q("eat", "", 5, 3, "Eat 5 times"),
            q("walk", "", 500, 4, "Walk 500 blocks"),
            q("fish", "", 3, 6, "Catch 3 things fishing"),
            q("breed", "", 2, 5, "Breed 2 animals"),
            q("trade", "", 3, 5, "Trade with a villager 3 times"),
            q("sleep", "", 1, 3, "Sleep in a bed"));

    public static final List<Quest> STORY = List.of(
            q("mine", "", 20, 10, "Chapter 1: Break 20 blocks"),
            q("craft", "", 10, 10, "Chapter 2: Craft 10 things"),
            q("kill", "zombie", 5, 15, "Chapter 3: Defeat 5 zombies"),
            q("walk", "", 1000, 15, "Chapter 4: Walk 1,000 blocks"),
            q("mine", "_ore", 20, 20, "Chapter 5: Mine 20 ores"),
            q("kill", "skeleton", 15, 20, "Chapter 6: Defeat 15 skeletons"),
            q("eat", "", 20, 15, "Chapter 7: Eat 20 times"),
            q("kill", "creeper", 15, 25, "Chapter 8: Defeat 15 creepers"),
            q("smelt", "", 64, 25, "Chapter 9: Smelt 64 things"),
            q("trade", "", 10, 25, "Chapter 10: Trade with villagers 10 times"),
            q("fish", "", 10, 30, "Chapter 11: Catch 10 things fishing"),
            q("kill", "enderman", 10, 35, "Chapter 12: Defeat 10 endermen"),
            q("kill", "blaze", 20, 40, "Chapter 13: Defeat 20 blazes"),
            q("mine", "ancient_debris", 4, 50, "Chapter 14: Dig up 4 ancient debris"),
            q("kill", "wither_skeleton", 25, 50, "Chapter 15: Defeat 25 wither skeletons"),
            q("kill", "elder_guardian", 1, 75, "Chapter 16: Defeat an elder guardian"),
            q("kill", "minecraft:wither", 1, 100, "Chapter 17: Defeat the Wither"),
            q("kill", "warden", 1, 100, "Chapter 18: Defeat a Warden"),
            q("kill", "ender_dragon", 1, 150, "Final chapter: Defeat the Ender Dragon"));

    public static final List<Quest> SECOND_STORY = List.of(
            q("kill", "", 200, 40, "Side story 1: Defeat 200 mobs"),
            q("mine", "", 2000, 40, "Side story 2: Break 2,000 blocks"),
            q("walk", "", 20000, 50, "Side story 3: Walk 20,000 blocks"),
            q("mine", "diamond", 32, 60, "Side story 4: Mine 32 diamond ore"),
            q("breed", "", 50, 60, "Side story 5: Breed 50 animals"),
            q("kill", "piglin_brute", 10, 70, "Side story 6: Defeat 10 piglin brutes"),
            q("kill", "shulker", 30, 80, "Side story 7: Defeat 30 shulkers"),
            q("kill", "warden", 3, 150, "Side story 8: Defeat 3 Wardens"));

    // Once awakened: one a day, and these pay in shop coins.
    public static final List<Quest> SPECIAL = List.of(
            q("kill", "", 50, 10, "Special: Defeat 50 mobs"),
            q("mine", "_ore", 40, 10, "Special: Mine 40 ores"),
            q("walk", "", 3000, 8, "Special: Walk 3,000 blocks"),
            q("kill", "wither_skeleton", 10, 15, "Special: Defeat 10 wither skeletons"),
            q("kill", "enderman", 15, 12, "Special: Defeat 15 endermen"),
            q("trade", "", 15, 10, "Special: Trade 15 times"),
            q("fish", "", 15, 10, "Special: Catch 15 things fishing"));

    private QuestBook() {}

    // Today's quests: the same all day, different the next. seed = the player and the day together.
    public static List<Quest> dailies(long seed, int count) {
        List<Quest> pool = new ArrayList<>(DAILY);
        Collections.shuffle(pool, new Random(seed));
        return pool.subList(0, Math.min(count, pool.size()));
    }

    public static Quest special(long seed) {
        return SPECIAL.get(new Random(seed).nextInt(SPECIAL.size()));
    }
}
