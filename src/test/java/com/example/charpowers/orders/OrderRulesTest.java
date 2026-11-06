package com.example.charpowers.orders;

import com.example.charpowers.orders.model.OrderRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderRulesTest {
    @Test
    void showsOnlyWhatYouHave() {
        assertTrue(OrderRules.available(0, 0, 0).isEmpty());
        assertEquals(7, OrderRules.available(2, 0, 0).size());
        assertEquals(4, OrderRules.available(0, 1, 0).size());
        assertEquals(14, OrderRules.available(1, 1, 1).size());
    }

    @Test
    void onlyKnownOrdersAreValid() {
        assertTrue(OrderRules.valid("clones", "farm"));
        assertTrue(OrderRules.valid("pets", "stay"));
        assertFalse(OrderRules.valid("pets", "mine"));
        assertFalse(OrderRules.valid("minions", "guard"));
        assertFalse(OrderRules.valid("nope", "follow"));
    }

    @Test
    void slicePointsTheRightWay() {
        assertEquals(-1, OrderRules.slice(3, 3, 4));
        assertEquals(0, OrderRules.slice(0, -40, 4));
        assertEquals(1, OrderRules.slice(40, 0, 4));
        assertEquals(2, OrderRules.slice(0, 40, 4));
        assertEquals(3, OrderRules.slice(-40, 0, 4));
        assertEquals(0, OrderRules.slice(-1, -40, 8));
        assertEquals(-1, OrderRules.slice(50, 0, 0));
    }

    @Test
    void clampAndEase() {
        double[] c = OrderRules.clamp(300, 0);
        assertEquals(OrderRules.MAX_PULL, c[0], 1e-9);
        assertEquals(0f, OrderRules.ease(-1));
        assertEquals(1f, OrderRules.ease(2));
        assertEquals(0.5f, OrderRules.ease(0.5f), 1e-6);
        assertTrue(OrderRules.tooSoon(10, 12));
        assertFalse(OrderRules.tooSoon(10, 20));
        assertTrue(OrderRules.bubbleRadius(14, 90, 22) < 22);
    }
}
