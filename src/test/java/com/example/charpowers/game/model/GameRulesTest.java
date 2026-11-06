package com.example.charpowers.game.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

class GameRulesTest {
    @Test
    void eachStatPointCostsABitMore() {
        assertEquals(5, GameRules.statCost(0));
        assertTrue(GameRules.statCost(10) > GameRules.statCost(9));
    }

    @Test
    void masteryGivesMore() {
        assertEquals(3, GameRules.dailies(24));
        assertEquals(4, GameRules.dailies(25));
        assertEquals(5, GameRules.dailies(50));
        assertFalse(GameRules.secondChain(74));
        assertTrue(GameRules.secondChain(75));
        assertEquals(3, GameRules.slots(99));
        assertEquals(4, GameRules.slots(100));
        assertEquals(10, GameRules.questPoints(10, 1));
        assertEquals(20, GameRules.questPoints(10, 100));
    }

    @Test
    void dailiesAreDifferentThingsAndStayTheSameAllDay() {
        List<QuestBook.Quest> today = QuestBook.dailies(12345L, 5);
        assertEquals(5, today.size());
        assertEquals(5, new HashSet<>(today).size());
        assertEquals(today, QuestBook.dailies(12345L, 5));
        assertNotEquals(today, QuestBook.dailies(12346L, 5));
    }

    @Test
    void questsCountOnlyWhatTheyAskFor() {
        QuestBook.Quest zombies = new QuestBook.Quest("kill", "zombie", 5, 1, "");
        assertTrue(zombies.matches("kill", "minecraft:zombie"));
        assertFalse(zombies.matches("kill", "minecraft:skeleton"));
        assertFalse(zombies.matches("mine", "minecraft:zombie"));
        assertTrue(new QuestBook.Quest("mine", "", 5, 1, "").matches("mine", "minecraft:stone"));
    }

    @Test
    void witherQuestIsNotWitherSkeletons() {
        QuestBook.Quest wither = QuestBook.STORY.stream().filter(q -> q.filter().equals("minecraft:wither")).findFirst().orElseThrow();
        assertTrue(wither.matches("kill", "minecraft:wither"));
    }
}
