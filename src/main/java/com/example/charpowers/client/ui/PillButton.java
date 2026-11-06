package com.example.charpowers.client.ui;

import com.example.charpowers.client.Soft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;

// A soft pill. Never a straight-sided box. PRIMARY is the pink one for the main thing to press, NORMAL a plum one, GHOST only shows when the mouse is on it.
public final class PillButton {
    public enum Kind { PRIMARY, NORMAL, GHOST }

    private PillButton() {}

    // Paints one and registers its click. id only has to be different from its neighbours.
    public static void draw(Frame ui, Object id, float x, float y, float w, float h, String label, Kind kind, boolean enabled, Runnable action) {
        boolean over = enabled && ui.over(x, y, x + w, y + h);
        float lift = ui.hover(id, over);
        paint(ui.g, x, y, w, h, label, kind, enabled, lift);
        if (enabled) ui.hit(x, y, x + w, y + h, action);
    }

    // Just the look.
    public static void paint(GuiGraphics g, float x, float y, float w, float h, String label, Kind kind, boolean enabled, float lift) {
        body(g, x, y, w, h, kind, enabled, lift);
        float size = Ui.fit(label, w - 5f, Ui.M, Ui.S, Ui.XS);
        int colour = !enabled ? Ui.FAINT : kind == Kind.PRIMARY ? Ui.ON_PINK : Soft.mix(Ui.TEXT, 0xFFFFFFFF, lift);
        Ui.centre(g, label, x + w / 2f, Ui.middle(y + h / 2f, size), colour, size);
    }

    public static void body(GuiGraphics g, float x, float y, float w, float h, Kind kind, boolean enabled, float lift) {
        float r = h / 2f, x1 = x + w, y1 = y + h;
        if (!enabled) {
            Soft.round(g, x, y, x1, y1, r, 0x60332740, 0x602A2035);
            Soft.ring(g, x, y, x1, y1, r, 1f, 0x14FFFFFF);
            return;
        }
        switch (kind) {
            case PRIMARY -> {
                if (lift > 0.01f) Soft.shadow(g, x, y, x1, y1, r, 7, Soft.alpha(Ui.PINK_GLOW, lift), false);
                Soft.shadow(g, x, y + 1, x1, y1 + 1, r, 3, 0x50000000, false);
                Soft.round(g, x, y, x1, y1, r, Soft.mix(Ui.PINK, 0xFFFFA8D8, lift), Soft.mix(Ui.PINK_DEEP, Ui.PINK, lift));
                Soft.ring(g, x, y, x1, y1, r, 1f, 0x60FFFFFF, 0x20FFFFFF);
            }
            case NORMAL -> {
                if (lift > 0.01f) Soft.shadow(g, x, y, x1, y1, r, 6, Soft.alpha(0x50FF88C8, lift), false);
                Soft.shadow(g, x, y + 1, x1, y1 + 1, r, 3, 0x50000000, false);
                Soft.round(g, x, y, x1, y1, r, Soft.mix(0xFF3E2F4D, 0xFF5A4270, lift), Soft.mix(0xFF2F2439, 0xFF49355C, lift));
                Soft.ring(g, x, y, x1, y1, r, 1f, Soft.mix(Ui.EDGE_LIT, 0xA0FF88C8, lift), Soft.mix(0x18FFFFFF, 0x60FF88C8, lift));
            }
            case GHOST -> {
                if (lift > 0.01f) {
                    Soft.round(g, x, y, x1, y1, r, Soft.alpha(0x40FFFFFF, lift));
                    Soft.ring(g, x, y, x1, y1, r, 1f, Soft.alpha(0x60FF88C8, lift));
                }
            }
        }
    }

    // A little on/off switch whose knob slides.
    public static void toggle(Frame ui, Object id, float x, float y, float w, float h, boolean on, Runnable action) {
        GuiGraphics g = ui.g;
        boolean over = ui.over(x, y, x + w, y + h);
        float lift = ui.hover(id + "#h", over);
        float at = ui.glide(id + "#t", on ? 1f : 0f);
        float r = h / 2f;
        if (lift > 0.01f) Soft.shadow(g, x, y, x + w, y + h, r, 5, Soft.alpha(Ui.PINK_GLOW, lift * (0.4f + 0.6f * at)), false);
        Soft.round(g, x, y, x + w, y + h, r, Soft.mix(0xFF1A1320, Ui.PINK, at), Soft.mix(0xFF231A2B, Ui.PINK_DEEP, at));
        Soft.ring(g, x, y, x + w, y + h, r, 1f, Soft.mix(Ui.EDGE_DARK, 0x50FFFFFF, at), Soft.mix(Ui.EDGE, 0x20FFFFFF, at));
        float knob = r - 1.5f, kx = x + r + at * (w - h);
        Soft.shadow(g, kx - knob, y + r - knob + 0.5f, kx + knob, y + r + knob + 0.5f, knob, 2, 0x50000000, false);
        Soft.disc(g, kx, y + r, knob, Soft.mix(0xFFBCA9CC, 0xFFFFFFFF, Math.max(at, lift)));
        ui.hit(x, y, x + w, y + h, action);
    }

    // A small round button with a shape on it: minus, plus, or an arrow (way -1 or 1).
    public static void round(Frame ui, Object id, float cx, float cy, float r, char mark, boolean enabled, Runnable action) {
        GuiGraphics g = ui.g;
        boolean over = enabled && ui.over(cx - r, cy - r, cx + r, cy + r);
        float lift = ui.hover(id, over);
        body(g, cx - r, cy - r, r * 2f, r * 2f, Kind.NORMAL, enabled, lift);
        int colour = !enabled ? Ui.FAINT : Soft.mix(Ui.TEXT, Ui.PINK, lift);
        float arm = r * 0.42f, thick = Math.max(1f, r * 0.2f);
        switch (mark) {
            case '-' -> Soft.line(g, cx - arm, cy, cx + arm, cy, thick, colour);
            case '+' -> {
                Soft.line(g, cx - arm, cy, cx + arm, cy, thick, colour);
                Soft.line(g, cx, cy - arm, cx, cy + arm, thick, colour);
            }
            case '<' -> Soft.chevron(g, cx - 0.5f, cy, r * 0.32f, -1, 0, thick, colour);
            case '>' -> Soft.chevron(g, cx + 0.5f, cy, r * 0.32f, 1, 0, thick, colour);
            default -> {}
        }
        if (enabled) ui.hit(cx - r, cy - r, cx + r, cy + r, action);
    }

    // Paints over a real button of someone else's (Palladium's), keeping its clicks.
    public static void skin(GuiGraphics g, AbstractButton b, String label, Kind kind, float lift) {
        paint(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), label, kind, b.active, lift);
    }
}
