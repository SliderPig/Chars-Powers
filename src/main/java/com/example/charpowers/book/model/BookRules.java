package com.example.charpowers.book.model;

// The numbers behind Book Keeping, with no game code in them.
public final class BookRules {
    public static final int MAX_PAGES = 5, MAX_ENTRIES = 40000, MAX_KILLS = 512, UNDO_PER_TICK = 256, UNLOADED_COST = 32;
    public static final double RING_MIN = 3000, RING_MAX = 30000;

    private BookRules() {}

    // 1 page, then 2 at 25, 3 at 50, 4 at 75, 5 at 100.
    public static int pages(int level) {
        return 1 + Math.max(0, Math.min(level, 100)) / 25;
    }

    // How close a death spot has to be for its items to come back: 2 chunks at the start, 4 at 100. In blocks.
    public static double deathRange(int level) {
        return 32 + (Math.max(1, Math.min(level, 100)) - 1) * 32.0 / 99;
    }

    // Where the one book goes: somewhere in the ring round spawn. Takes two random numbers from 0 to 1, gives x and z.
    public static int[] spotInRing(double a, double b) {
        double angle = a * Math.PI * 2;
        double reach = RING_MIN + b * (RING_MAX - RING_MIN);
        return new int[] {(int) (Math.cos(angle) * reach), (int) (Math.sin(angle) * reach)};
    }

    // The oldest thing in the journal any page still needs. indexes = each written page's place in the journal, -1 if empty.
    public static long oldestNeeded(long[] indexes, long end) {
        long oldest = end;
        for (long index : indexes) {
            if (index >= 0 && index < oldest) oldest = index;
        }
        return oldest;
    }

    // How much to throw away from the old end once the journal's too long: a tenth, so it isn't trimmed every single time.
    public static int toDrop(int size) {
        return size <= MAX_ENTRIES ? 0 : Math.max(size - MAX_ENTRIES, MAX_ENTRIES / 10);
    }

    public static String mark(String owner, long timeline) {
        return owner + ":" + timeline;
    }
}
