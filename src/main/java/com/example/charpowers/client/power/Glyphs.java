package com.example.charpowers.client.power;

import com.example.charpowers.client.Soft;
import net.minecraft.client.gui.GuiGraphics;

// Small smooth pictures drawn from shapes, so they're never blurry or blocky at any GUI scale.
public final class Glyphs {
    private Glyphs() {}

    // A padlock centred on cx, cy. size 1 = about 10 units tall.
    public static void lock(GuiGraphics g, float cx, float cy, float size, int colour) {
        float w = 4.2f * size, top = cy - 0.8f * size, bottom = cy + 4.6f * size, loop = 2.8f * size, thick = Math.max(1f, 1.2f * size);
        Soft.arc(g, cx, top, loop, thick, 0.75f, 1.25f, colour);
        Soft.line(g, cx - loop + thick / 2f, top, cx - loop + thick / 2f, top + 1f * size, thick, colour);
        Soft.line(g, cx + loop - thick / 2f, top, cx + loop - thick / 2f, top + 1f * size, thick, colour);
        Soft.round(g, cx - w, top, cx + w, bottom, 1.4f * size, colour);
    }

    // A little plus on a pink dot: this can be bought.
    public static void buyable(GuiGraphics g, float cx, float cy, float r, int dot, int mark) {
        Soft.disc(g, cx, cy, r, dot);
        float arm = r * 0.5f, thick = Math.max(0.8f, r * 0.34f);
        Soft.line(g, cx - arm, cy, cx + arm, cy, thick, mark);
        Soft.line(g, cx, cy - arm, cx, cy + arm, thick, mark);
    }

    // A cross, for closing things.
    public static void cross(GuiGraphics g, float cx, float cy, float size, float thick, int colour) {
        Soft.line(g, cx - size, cy - size, cx + size, cy + size, thick, colour);
        Soft.line(g, cx - size, cy + size, cx + size, cy - size, thick, colour);
    }

    // A tick.
    public static void tick(GuiGraphics g, float cx, float cy, float size, float thick, int colour) {
        Soft.line(g, cx - size, cy, cx - size * 0.25f, cy + size * 0.75f, thick, colour);
        Soft.line(g, cx - size * 0.25f, cy + size * 0.75f, cx + size, cy - size * 0.7f, thick, colour);
    }

    // The black cat: a round head with two pointed ears and a pair of pink eyes, centred on cx, cy. size 1 = about 12 units wide.
    public static void cat(GuiGraphics g, float cx, float cy, float size, int fur, int eyes) {
        float r = 5f * size;
        Soft.polygon(g, new float[] {cx - r * 0.95f, cx - r * 0.85f, cx - r * 0.1f}, new float[] {cy - r * 0.2f, cy - r * 1.35f, cy - r * 0.8f}, 3, fur);
        Soft.polygon(g, new float[] {cx + r * 0.1f, cx + r * 0.85f, cx + r * 0.95f}, new float[] {cy - r * 0.8f, cy - r * 1.35f, cy - r * 0.2f}, 3, fur);
        Soft.round(g, cx - r, cy - r * 0.85f, cx + r, cy + r * 0.8f, r * 0.82f, fur);
        Soft.disc(g, cx - r * 0.42f, cy - r * 0.05f, r * 0.18f, eyes);
        Soft.disc(g, cx + r * 0.42f, cy - r * 0.05f, r * 0.18f, eyes);
    }
}
