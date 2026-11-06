package com.example.charpowers.items.model;

// The Spray Bottle: what it can hold and how much. Only lava hurts: nobody's getting hurt by a squirt of water or milk.
public final class SprayRules {
    public enum Liquid {
        EMPTY(0, 0, 0xffffff), WATER(48, 16, 0x4aa3ff), LAVA(16, 16, 0xff7a1a), MILK(24, 24, 0xf4f4ee);

        public final int full, perFill, barColour;

        Liquid(int full, int perFill, int barColour) {
            this.full = full;
            this.perFill = perFill;
            this.barColour = barColour;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Liquid of(String id) {
            for (Liquid l : values()) {
                if (l.id().equals(id)) return l;
            }
            return EMPTY;
        }
    }

    // one squirt every this many ticks while you hold the button
    public static final int TICKS_PER_SPRAY = 3;
    public static final double RANGE = 6.5;
    public static final double CONE_DEGREES = 18;
    public static final float LAVA_DAMAGE = 1.5f;
    public static final int LAVA_BURN_SECONDS = 4;

    private SprayRules() {}

    public static boolean hurts(Liquid l) {
        return l == Liquid.LAVA;
    }

    // Can this go in a bottle holding that much of that?
    public static boolean canFill(Liquid in, int sprays, Liquid adding) {
        if (adding == Liquid.EMPTY) return false;
        if (in == Liquid.EMPTY || sprays <= 0) return true;
        return in == adding && sprays < adding.full;
    }

    // How many sprays it holds after one fill.
    public static int afterFill(Liquid in, int sprays, Liquid adding) {
        int have = in == adding ? sprays : 0;
        return Math.min(adding.full, have + adding.perFill);
    }

    public static int barWidth(int sprays, Liquid l) {
        if (l == Liquid.EMPTY || l.full == 0) return 0;
        return Math.round(13f * Math.max(0, Math.min(sprays, l.full)) / l.full);
    }
}
