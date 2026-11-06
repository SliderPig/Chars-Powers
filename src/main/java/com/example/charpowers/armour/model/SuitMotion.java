package com.example.charpowers.armour.model;

// The numbers behind the suit flying to you, going away again, and walking in it. No game code in here.
public final class SuitMotion {
    // The suit arrives as one folded-up bundle and rises behind you (FLY), hovers there and opens like a shell (OPEN), glides round you while
    // its plates swing shut (CLOSE), and last the helmet drops onto your head (DROP) and bounces once (SETTLE). Sending it away is the same
    // timeline played backwards, so every number below is "ticks into putting it on".
    public static final int FLY = 30, OPEN = 12, CLOSE = 16, DROP = 6, SETTLE = 8;
    public static final int OPEN_END = FLY + OPEN, CLOSE_END = OPEN_END + CLOSE, LAND = CLOSE_END + DROP, TOTAL = LAND + SETTLE;
    public static final double HELMET_HOVER = 16, LEAN = 68;
    public static final double STEP_BLOCKS = 1.1;

    private SuitMotion() {}

    // The time on the putting-on timeline for this many ticks since the entity started (age 0 = just made).
    public static double timeline(int age, boolean out) {
        double t = Math.max(0, Math.min(TOTAL, age));
        return out ? TOTAL - t : t;
    }

    // 0..1 along the flight; 1 once it hovers.
    public static double flight(double t) {
        return Math.max(0, Math.min(1, t / FLY));
    }

    // How tilted the flying bundle is: head first at 68 degrees, upright by the time it arrives behind you.
    public static double lean(double t) {
        return LEAN * (1 - ease((flight(t) - 0.6) / 0.4));
    }

    // How far open a group of plates is, 0 shut to 1 wide. Groups close one after another: 0 legs, 1 chest and back plate, 2 arms.
    public static double open(int group, double t) {
        if (t <= FLY) return 0;
        if (t < OPEN_END) return ease((t - FLY) / OPEN);
        return 1 - ease((t - (OPEN_END + 4 * group)) / 6);
    }

    // 0 hovering behind you, 1 on top of you.
    public static double glide(double t) {
        return t < OPEN_END ? 0 : ease((t - OPEN_END) / CLOSE);
    }

    // How high above your head the helmet is, in model pixels: it lifts off while the suit opens, waits, then drops and bounces once.
    public static double helmet(double t) {
        if (t <= FLY) return 0;
        if (t < OPEN_END) return HELMET_HOVER * ease((t - FLY) / OPEN);
        if (t < CLOSE_END) return HELMET_HOVER;
        if (t < LAND) {
            double u = (t - CLOSE_END) / DROP;
            return HELMET_HOVER * (1 - u * u);
        }
        double after = t - LAND;
        return 2.4 * Math.exp(-after * 0.45) * Math.abs(Math.sin(after * 1.2));
    }

    public enum Beat { OPEN_SERVO, CLANK_LEGS, CLANK_CHEST, CLANK_ARMS, HELMET }

    // The sounds (and the shake) that belong to the moments between two times, whichever way the time is running.
    public static java.util.List<Beat> beats(double from, double to) {
        double lo = Math.min(from, to), hi = Math.max(from, to);
        java.util.List<Beat> out = new java.util.ArrayList<>();
        if (lo < FLY && hi >= FLY) out.add(Beat.OPEN_SERVO);
        if (lo < OPEN_END + 6 && hi >= OPEN_END + 6) out.add(Beat.CLANK_LEGS);
        if (lo < OPEN_END + 10 && hi >= OPEN_END + 10) out.add(Beat.CLANK_CHEST);
        if (lo < OPEN_END + 14 && hi >= OPEN_END + 14) out.add(Beat.CLANK_ARMS);
        if (lo < LAND && hi >= LAND) out.add(Beat.HELMET);
        return out;
    }

    public static double ease(double t) {
        double c = Math.max(0, Math.min(1, t));
        return c * c * (3 - 2 * c);
    }

    // A smooth curve from a to b that bows up to c: {x, y, z}.
    public static double[] curve(double[] a, double[] c, double[] b, double t) {
        double u = 1 - t;
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) out[i] = u * u * a[i] + 2 * u * t * c[i] + t * t * b[i];
        return out;
    }

    // The bend of the arc: half way along and higher up, higher the further it has to go.
    public static double[] bow(double[] a, double[] b) {
        double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        double lift = Math.max(4, Math.min(18, Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.35));
        return new double[] {a[0] + dx / 2, Math.max(a[1], b[1]) + lift, a[2] + dz / 2};
    }

    // Slowness level in the suit: V (amplifier 4) at mastery 1, down to I (amplifier 0) at mastery 100, evenly in between.
    public static int slownessAmplifier(int mastery) {
        int level = Math.max(1, Math.min(100, mastery));
        return (int) Math.round(4.0 * (100 - level) / 99.0);
    }

    public enum Step { NONE, STEP, LAND }

    // Works out when a foot comes down: every STEP_BLOCKS walked, and a heavy landing after a drop.
    public static final class Walk {
        private double walked, peak;
        private int air;
        private boolean started;
        public double drop;

        public void reset() {
            walked = 0;
            air = 0;
            started = false;
        }

        public Step update(double movedFlat, boolean onGround, double y) {
            if (!started) {
                started = true;
                peak = y;
            }
            if (!onGround) {
                air++;
                peak = air == 1 ? y : Math.max(peak, y);
                walked = 0;
                return Step.NONE;
            }
            boolean landed = air >= 3;
            drop = peak - y;
            air = 0;
            peak = y;
            if (landed) {
                walked = 0;
                return Step.LAND;
            }
            walked += movedFlat;
            if (walked >= STEP_BLOCKS) {
                walked = 0;
                return Step.STEP;
            }
            return Step.NONE;
        }
    }
}
