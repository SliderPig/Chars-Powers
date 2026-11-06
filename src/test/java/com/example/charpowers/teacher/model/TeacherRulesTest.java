package com.example.charpowers.teacher.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TeacherRulesTest {
    @Test
    void onlyTheTradeThatFillsExpertCounts() {
        assertTrue(TeacherRules.tradeMakesMaster(4, 250, 250));
        assertTrue(TeacherRules.tradeMakesMaster(4, 300, 250));
        assertFalse(TeacherRules.tradeMakesMaster(4, 249, 250));
        assertFalse(TeacherRules.tradeMakesMaster(3, 400, 150));
        assertFalse(TeacherRules.tradeMakesMaster(5, 400, 0));
    }

    @Test
    void theSecondClickMustComeSoon() {
        assertFalse(TeacherRules.confirms(0, 1000));
        assertTrue(TeacherRules.confirms(900, 1000));
        assertFalse(TeacherRules.confirms(500, 1000));
    }
}
