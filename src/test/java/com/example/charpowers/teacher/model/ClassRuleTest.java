package com.example.charpowers.teacher.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClassRuleTest {
    @Test
    void takesThreeWholeDaysAtTheLectern() {
        ClassRule r = new ClassRule(0, 0);
        assertEquals(ClassRule.Result.STARTED, r.step(true));
        for (int i = 2; i < ClassRule.SECONDS_PER_DAY; i++) assertEquals(ClassRule.Result.NOTHING, r.step(true));
        assertEquals(ClassRule.Result.NEW_DAY, r.step(true));
        for (int i = ClassRule.SECONDS_PER_DAY + 1; i < ClassRule.SECONDS_NEEDED; i++) assertNotEquals(ClassRule.Result.DONE, r.step(true));
        assertEquals(ClassRule.Result.DONE, r.step(true));
    }

    @Test
    void aShortKnockIsForgivenButWalkingOffStartsAgain() {
        ClassRule r = new ClassRule(500, 0);
        for (int i = 1; i < ClassRule.GRACE_SECONDS; i++) assertEquals(ClassRule.Result.NOTHING, r.step(false));
        assertEquals(ClassRule.Result.NOTHING, r.step(true));
        assertEquals(0, r.away);
        for (int i = 1; i < ClassRule.GRACE_SECONDS; i++) r.step(false);
        assertEquals(ClassRule.Result.WALKED_OUT, r.step(false));
        assertEquals(0, r.seconds);
    }

    @Test
    void beingAwayBeforeClassStartsDoesNothing() {
        ClassRule r = new ClassRule(0, 0);
        for (int i = 0; i < 100; i++) assertEquals(ClassRule.Result.NOTHING, r.step(false));
    }
}
