package com.example.charpowers.client.power.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuLayoutTest {
    // Every width the game can give: its GUI scale never lets a window go under 320 units unless the window itself is tiny.
    @Test
    void sideCardsNeverRunOffTheScreenOrUnderTheWindow() {
        for (int keepClear : new int[] {0, 34}) {
            for (int w = 320; w <= 2000; w++) {
                int[] wide = MenuLayout.sideCards(w, keepClear);
                int x = MenuLayout.windowX(w);
                if (wide[0] > 0) assertTrue(x - wide[0] >= MenuLayout.SIDE_EDGE, "left card off screen at " + w);
                if (wide[1] > 0) assertTrue(x + MenuLayout.WINDOW_W + keepClear + wide[1] <= w - MenuLayout.SIDE_EDGE, "right card off screen at " + w);
                for (int card : wide) assertTrue(card == 0 || (card >= MenuLayout.SIDE_LEAST && card <= MenuLayout.SIDE_MOST), "card size " + card + " at " + w);
            }
        }
    }

    @Test
    void cardsFoldAwayWhenNarrowAndMatchWhenWide() {
        assertEquals(0, MenuLayout.sideCards(320, 0)[0]);
        assertEquals(0, MenuLayout.sideCards(320, 0)[1]);
        int[] small = MenuLayout.sideCards(427, 0);
        assertTrue(small[0] > 0);
        assertEquals(small[0], small[1]);
        assertEquals(MenuLayout.SIDE_MOST, MenuLayout.sideCards(960, 0)[0]);
    }

    @Test
    void anotherModsBarOnlyFoldsTheRightCard() {
        int[] wide = MenuLayout.sideCards(427, 34);
        assertTrue(wide[0] > 0);
        assertEquals(0, wide[1]);
    }

    @Test
    void theWindowAndItsTabsFitTheSmallestHeight() {
        assertTrue(MenuLayout.windowY(240) >= MenuLayout.TAB_H, "the tabs would go off the top");
        assertTrue(MenuLayout.windowY(240) + MenuLayout.WINDOW_H <= 240);
    }

    @Test
    void tabsStayInsideTheRail() {
        assertFalse(MenuLayout.railOverflows(8));
        assertTrue(MenuLayout.railOverflows(9));
        assertEquals(0, MenuLayout.railMost(8));
        for (int tabs = 1; tabs <= 26; tabs++) {
            for (int tab = 0; tab < tabs; tab++) {
                float shown = MenuLayout.railToShow(0, tab, tabs);
                float left = tab * MenuLayout.TAB_STEP - shown;
                assertTrue(left >= 0, "tab " + tab + " of " + tabs + " starts before the rail");
                assertTrue(left + MenuLayout.TAB_W <= MenuLayout.railRoom(tabs), "tab " + tab + " of " + tabs + " ends past the rail");
            }
        }
    }
}
