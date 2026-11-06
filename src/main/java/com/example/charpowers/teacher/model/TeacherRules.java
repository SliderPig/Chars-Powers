package com.example.charpowers.teacher.model;

// How you earn Teacher: raise 50 villagers all the way up to Master.
public final class TeacherRules {
    public static final int VILLAGERS_NEEDED = 50;
    public static final int MASTER_LEVEL = 5;
    public static final long CONFIRM_TICKS = 200;

    private TeacherRules() {}

    // True when this trade fills the villager's last bar before Master.
    public static boolean tradeMakesMaster(int level, int xp, int xpForNextLevel) {
        return level == MASTER_LEVEL - 1 && xp >= xpForNextLevel;
    }

    // A second crouch + click soon after the first is the yes. Becoming a Teacher and quitting both ask twice.
    public static boolean confirms(long firstClick, long now) {
        return firstClick > 0 && now - firstClick <= CONFIRM_TICKS;
    }
}
