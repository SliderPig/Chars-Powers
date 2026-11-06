package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HitMathTest {
    @Test void groundSlamIs100Health() { assertEquals(100f, HitMath.powerDamage(50, 1.0), 1e-4); }
    @Test void scaleHalves()           { assertEquals(50f, HitMath.powerDamage(50, 0.5), 1e-4); }
    // One Punch (100) + Berserker (+5 hearts), then Execute x2 and Blood Pact x1.5
    @Test void flatThenMultiply()      { assertEquals(330f, HitMath.boosted(100f, 5f, 3f), 1e-4); }
    @Test void noBoostsChangeNothing() { assertEquals(7f, HitMath.boosted(7f, 0f, 1f), 1e-4); }
}
