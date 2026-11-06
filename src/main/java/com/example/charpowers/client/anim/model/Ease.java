package com.example.charpowers.client.anim.model;

// How a move speeds up and slows down between two keyframes. The names are what you type in player_poses.json.
public enum Ease {
    LINEAR,
    SINE,        // gentle in and out, the everyday one
    IN,          // starts slow, ends fast: the fall of a heavy swing
    OUT,         // starts fast, ends slow: settling
    IN3,         // like IN but much later and harder
    OUT3,        // like OUT but sharper
    BACK,        // goes a little past the mark and comes back: follow-through
    ANTICIPATE,  // pulls back a little first, then goes
    SETTLE,      // lands, bounces once, stops: weight
    HOLD;        // doesn't move until the next keyframe

    public double at(double t) {
        double x = t < 0 ? 0 : t > 1 ? 1 : t;
        return switch (this) {
            case LINEAR -> x;
            case SINE -> 0.5 - Math.cos(x * Math.PI) / 2;
            case IN -> x * x;
            case OUT -> 1 - (1 - x) * (1 - x);
            case IN3 -> x * x * x;
            case OUT3 -> 1 - Math.pow(1 - x, 3);
            case BACK -> {
                double u = x - 1;
                yield 1 + 2.70158 * u * u * u + 1.70158 * u * u;
            }
            case ANTICIPATE -> 2.70158 * x * x * x - 1.70158 * x * x;
            case SETTLE -> {
                if (x < 0.6) {
                    double u = x / 0.6;
                    yield u * u;
                }
                double u = (x - 0.6) / 0.4;
                yield 1 - 0.09 * Math.sin(u * Math.PI);
            }
            case HOLD -> x >= 1 ? 1 : 0;
        };
    }

    public static Ease named(String name) {
        if (name == null) return SINE;
        try {
            return valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SINE;
        }
    }
}
