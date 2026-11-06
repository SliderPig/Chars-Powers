package com.example.charpowers.client.ui;

import com.example.charpowers.client.Soft;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;

// A slim track with a round knob you drag. Calls back with the new value only when it really changes.
public final class Slider {
    private Slider() {}

    public static void draw(Frame ui, Object id, float x, float y, float w, float h, double value, double low, double high, double step, DoubleConsumer onChange) {
        var g = ui.g;
        float mid = y + h / 2f, knob = Math.min(h, 10f) / 2f;
        float left = x + knob, track = w - knob * 2f;
        // the knob follows the mouse while it's held
        if (ui.held == id && track > 0) {
            double t = Mth.clamp((ui.mx - left) / track, 0f, 1f), wanted = low + (high - low) * t;
            if (step > 0) wanted = Math.round(wanted / step) * step;
            if (Math.abs(wanted - value) > 1e-6) {
                value = wanted;
                onChange.accept(value);
            }
        }
        float at = high > low ? (float) Mth.clamp((value - low) / (high - low), 0.0, 1.0) : 0f;
        float shown = ui.held == id ? ui.anims.to(id, at, 60f) : ui.anims.to(id, at, 18f);
        float kx = left + shown * track;
        boolean over = ui.over(x, y, x + w, y + h);
        float lift = ui.hover(id + "#k", over || ui.held == id);
        Soft.round(g, x, mid - 1.5f, x + w, mid + 1.5f, 1.5f, 0xFF120D17, 0xFF1A1320);
        Soft.ring(g, x, mid - 1.5f, x + w, mid + 1.5f, 1.5f, 1f, 0x14FFFFFF);
        Soft.round(g, x, mid - 1.5f, kx, mid + 1.5f, 1.5f, Ui.PINK, Ui.PINK_DEEP);
        if (lift > 0.01f) Soft.shadow(g, kx - knob, mid - knob, kx + knob, mid + knob, knob, 5, Soft.alpha(Ui.PINK_GLOW, lift), false);
        Soft.shadow(g, kx - knob, mid - knob + 1, kx + knob, mid + knob + 1, knob, 3, 0x60000000, false);
        Soft.disc(g, kx, mid, knob, Soft.mix(0xFFF3E9F8, 0xFFFFFFFF, lift));
        ui.hit(x, y, x + w, y + h, () -> ui.held = id);
    }
}
