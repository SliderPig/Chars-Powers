package com.example.charpowers.game;

import com.example.charpowers.game.model.GateRules;
import com.example.charpowers.game.model.ShopPrices;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPricesTest {
    @Test
    void everythingTakesSeveralClears() {
        for (String line : ShopPrices.BUILT_IN) {
            assertEquals(3, line.trim().split("\s+").length, line);
            assertTrue(ShopPrices.clears(line, 2) >= 1, "too cheap: " + line);
        }
    }

    @Test
    void biggestThingsAreEarned() {
        int top = 0;
        for (String line : ShopPrices.BUILT_IN) top = Math.max(top, ShopPrices.priceOf(line));
        assertTrue(top >= 15 * GateRules.coins(4));
    }

    @Test
    void incomeRisesWithRank() {
        for (int r = 1; r < GateRules.RANKS.length; r++) assertTrue(GateRules.coins(r) > GateRules.coins(r - 1));
    }
}
