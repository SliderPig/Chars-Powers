package com.example.charpowers.client.anim.model;

import java.util.Arrays;

// One part's keyframes: at these ticks these three numbers, each stretch shaped by the ease of the keyframe it starts from.
public final class Track {
    private final double[] ticks;
    private final double[][] values;
    private final Ease[] eases;

    public Track(double[] ticks, double[][] values, Ease[] eases) {
        if (ticks.length == 0 || ticks.length != values.length || ticks.length != eases.length) throw new IllegalArgumentException("keyframes don't line up");
        for (int i = 1; i < ticks.length; i++) {
            if (ticks[i] <= ticks[i - 1]) throw new IllegalArgumentException("keyframes must go forward in time");
        }
        this.ticks = ticks;
        this.values = values;
        this.eases = eases;
    }

    public double firstTick() {
        return ticks[0];
    }

    public double lastTick() {
        return ticks[ticks.length - 1];
    }

    // The three numbers at this moment, written into out. Before the first keyframe and after the last it stays put.
    public void sample(double tick, double[] out) {
        int n = ticks.length;
        if (tick <= ticks[0]) {
            copy(values[0], out);
            return;
        }
        if (tick >= ticks[n - 1]) {
            copy(values[n - 1], out);
            return;
        }
        int i = Arrays.binarySearch(ticks, tick);
        if (i >= 0) {
            copy(values[i], out);
            return;
        }
        int next = -i - 1, from = next - 1;
        double t = eases[from].at((tick - ticks[from]) / (ticks[next] - ticks[from]));
        for (int k = 0; k < 3; k++) out[k] = values[from][k] + (values[next][k] - values[from][k]) * t;
    }

    private static void copy(double[] from, double[] to) {
        to[0] = from[0];
        to[1] = from[1];
        to[2] = from[2];
    }
}
