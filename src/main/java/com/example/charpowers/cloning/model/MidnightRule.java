package com.example.charpowers.cloning.model;

// Midnight Reflection: 60 unbroken seconds of staring at the glass, and at least one of those seconds
// has to fall in the midnight window. Checked once a second.
public final class MidnightRule {
    public static final int MIDNIGHT = 18_000;
    public static final int WINDOW_FROM = 17_000;
    public static final int WINDOW_TO = 19_000;
    public static final int SECONDS_NEEDED = 60;

    private int seconds;
    private boolean sawMidnight;

    public static boolean isMidnight(long dayTime) {
        long now = Math.floorMod(dayTime, 24_000L);
        return now >= WINDOW_FROM && now <= WINDOW_TO;
    }

    // Returns true the moment the player has done it.
    public boolean step(boolean staring, long dayTime) {
        if (!staring) {
            reset();
            return false;
        }
        if (isMidnight(dayTime)) sawMidnight = true;
        seconds++;
        return seconds >= SECONDS_NEEDED && sawMidnight;
    }

    public void reset() {
        seconds = 0;
        sawMidnight = false;
    }

    public int seconds() {
        return seconds;
    }

    public boolean sawMidnight() {
        return sawMidnight;
    }
}
