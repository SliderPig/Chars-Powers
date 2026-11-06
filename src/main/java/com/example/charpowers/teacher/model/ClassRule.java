package com.example.charpowers.teacher.model;

// The awakening quest: stand at a lectern for 3 whole Minecraft days. Counted once a second while near a lectern,
// so sleeping can't skip it. Getting knocked away for a moment is forgiven, walking off starts it again.
public final class ClassRule {
    public static final int SECONDS_PER_DAY = 1200;
    public static final int SECONDS_NEEDED = 3 * SECONDS_PER_DAY;
    public static final int GRACE_SECONDS = 30;

    public enum Result { NOTHING, STARTED, NEW_DAY, WALKED_OUT, DONE }

    public int seconds;
    public int away;

    public ClassRule(int seconds, int away) {
        this.seconds = seconds;
        this.away = away;
    }

    public Result step(boolean nearLectern) {
        if (!nearLectern) {
            if (seconds == 0) return Result.NOTHING;
            if (++away < GRACE_SECONDS) return Result.NOTHING;
            seconds = 0;
            away = 0;
            return Result.WALKED_OUT;
        }
        away = 0;
        seconds++;
        if (seconds >= SECONDS_NEEDED) return Result.DONE;
        if (seconds == 1) return Result.STARTED;
        return seconds % SECONDS_PER_DAY == 0 ? Result.NEW_DAY : Result.NOTHING;
    }
}
