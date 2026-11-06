package com.example.charpowers.cloning.model;

// The numbers behind Guard and Farm: how far from its post a clone goes, and when it comes back. No game code.
public final class PostRules {
    public static final int GUARD_RADIUS = 9, GUARD_LEASH = 14, FARM_AREA = 12, FARM_HEIGHT = 3;

    private PostRules() {}

    // Is something worth going after? It must be near the post, and the clone mustn't be dragged past its leash.
    public static boolean worthChasing(double targetToPostSqr, double cloneToPostSqr) {
        return targetToPostSqr <= (double) GUARD_RADIUS * GUARD_RADIUS && cloneToPostSqr <= (double) GUARD_LEASH * GUARD_LEASH;
    }

    // Idle clones drift back to the post once they are further than this.
    public static boolean shouldReturn(double cloneToPostSqr) {
        return cloneToPostSqr > 9;
    }

    // A crop is inside the field being worked: the square round the post, a few blocks up or down.
    public static boolean inField(int dx, int dy, int dz) {
        return Math.abs(dx) <= FARM_AREA && Math.abs(dz) <= FARM_AREA && Math.abs(dy) <= FARM_HEIGHT;
    }

    // Ripe means fully grown; nether wart tops out at 3, everything else at its own maximum.
    public static boolean ripe(int age, int maxAge) {
        return maxAge > 0 && age >= maxAge;
    }
}
