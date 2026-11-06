package com.example.charpowers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmokeTest {
    @Test
    void modIdIsCharpowers() {
        assertEquals("charpowers", CharPowers.MODID);
    }
}
