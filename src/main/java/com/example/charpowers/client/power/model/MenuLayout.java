package com.example.charpowers.client.power.model;

// The sums behind the power menu's layout, kept apart so they can be tested without the game. All in GUI units.
public final class MenuLayout {
    public static final int WINDOW_W = 252, WINDOW_H = 196;
    public static final int SIDE_EDGE = 6, SIDE_MOST = 124, SIDE_LEAST = 78;
    public static final int TAB_W = 26, TAB_STEP = 28, TAB_H = 21, RAIL_INSET = 14, ARROW = 13;

    private MenuLayout() {}

    public static int windowX(int screenW) {
        return (screenW - WINDOW_W) / 2;
    }

    public static int windowY(int screenH) {
        return (screenH - WINDOW_H) / 2;
    }

    // How wide the two side cards are (0 = no room, it folds away). Both the same width when both fit, so the menu stays balanced.
    public static int[] sideCards(int screenW, int rightKeepClear) {
        int x = windowX(screenW);
        int roomLeft = x - SIDE_EDGE, roomRight = screenW - x - WINDOW_W - SIDE_EDGE - rightKeepClear;
        boolean left = roomLeft >= SIDE_LEAST, right = roomRight >= SIDE_LEAST;
        if (left && right) {
            int wide = Math.min(SIDE_MOST, Math.min(roomLeft, roomRight));
            return new int[] {wide, wide};
        }
        return new int[] {left ? Math.min(SIDE_MOST, roomLeft) : 0, right ? Math.min(SIDE_MOST, roomRight) : 0};
    }

    // Do the tabs need arrows and a sliding rail?
    public static boolean railOverflows(int tabs) {
        return tabs * TAB_STEP - (TAB_STEP - TAB_W) > WINDOW_W - 2 * RAIL_INSET;
    }

    // How much room the tabs have along the top of the card.
    public static int railRoom(int tabs) {
        return WINDOW_W - 2 * RAIL_INSET - (railOverflows(tabs) ? 2 * (ARROW + 2) : 0);
    }

    // How far the rail can slide.
    public static int railMost(int tabs) {
        return Math.max(0, tabs * TAB_STEP - (TAB_STEP - TAB_W) - railRoom(tabs));
    }

    // Where the rail must slide to, from now, to bring this tab into full view.
    public static float railToShow(float now, int tab, int tabs) {
        float at = tab * TAB_STEP, room = railRoom(tabs);
        if (at < now) now = at;
        else if (at + TAB_W > now + room) now = at + TAB_W - room;
        return Math.max(0, Math.min(railMost(tabs), now));
    }
}
