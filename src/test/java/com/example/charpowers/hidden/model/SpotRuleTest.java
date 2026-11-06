package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpotRuleTest {
    @Test void behindWhenOpen()   { assertArrayEquals(new int[]{0, 0, -1}, SpotRule.pick(new int[][]{{0, 0, -1}, {1, 0, 0}}, s -> true)); }
    @Test void besideWhenWalled() { assertArrayEquals(new int[]{1, 0, 0}, SpotRule.pick(new int[][]{{0, 0, -1}, {1, 0, 0}}, s -> s[0] == 1)); }
    @Test void noneWhenBoxedIn()  { assertNull(SpotRule.pick(new int[][]{{0, 0, -1}}, s -> false)); }
}
