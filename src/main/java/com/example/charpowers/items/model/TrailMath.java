package com.example.charpowers.items.model;

// Smooth curves for the swing trails. No game code in here.
public final class TrailMath {
    private TrailMath() {}

    // The usual Catmull-Rom curve between p1 and p2, one number at a time. t runs from 0 (p1) to 1 (p2).
    public static double catmullRom(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    // How strong a point of a trail is: 1 at the fresh end, 0 by the time it has lived `life` ticks, easing out so the tail thins away softly.
    public static double fade(double age, double life) {
        if (age <= 0) return 1;
        if (age >= life) return 0;
        double u = 1 - age / life;
        return u * u * (3 - 2 * u);
    }
}
