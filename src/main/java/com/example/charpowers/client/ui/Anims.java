package com.example.charpowers.client.ui;

import net.minecraft.Util;

import java.util.HashMap;
import java.util.Map;

// Little values that glide towards a target, by real time, not frames: hover glows, sliding tabs, smooth scrolling.
public final class Anims {
    private final Map<Object, float[]> values = new HashMap<>();
    private long last = Util.getMillis();
    private float seconds;

    // Once a frame.
    public void frame() {
        long now = Util.getMillis();
        seconds = Math.min(0.1f, (now - last) / 1000f);
        last = now;
    }

    // speed: about how many times a second it closes most of the gap. 12 feels quick and soft.
    public float to(Object key, float target, float speed) {
        float[] v = values.get(key);
        if (v == null) {
            v = new float[] {target};
            values.put(key, v);
            return target;
        }
        v[0] += (target - v[0]) * (1f - (float) Math.exp(-speed * seconds));
        if (Math.abs(target - v[0]) < 0.002f) v[0] = target;
        return v[0];
    }

    // Starts at from the first time this key is seen.
    public float from(Object key, float from, float target, float speed) {
        if (!values.containsKey(key)) values.put(key, new float[] {from});
        return to(key, target, speed);
    }

    public void set(Object key, float value) {
        values.put(key, new float[] {value});
    }

    public void clear() {
        values.clear();
    }
}
