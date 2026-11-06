package com.example.charpowers.client.ui;

import com.example.charpowers.client.Soft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

// The family look: colours, spacing and text. All text is the normal Minecraft font. One pixel of that font is one GUI unit, so it only
// stays crisp when a pixel of the font lands on a whole number of screen pixels: every size goes through snapScale, for drawing and measuring alike.
public final class Ui {
    // plum and pink, from darkest to lightest
    public static final int INK = 0xFF0E0912, SHADOW = 0x90000000;
    public static final int CARD_TOP = 0xFF2F2439, CARD_BOTTOM = 0xFF211829, SIDE_TOP = 0xFF2A2033, SIDE_BOTTOM = 0xFF1E1626;
    public static final int WELL_TOP = 0xFF120D17, WELL_BOTTOM = 0xFF1A1320, ROW_TOP = 0xFF332740, ROW_BOTTOM = 0xFF2A2035, ROW_LIT_TOP = 0xFF47335A, ROW_LIT_BOTTOM = 0xFF3B2B4B;
    public static final int EDGE = 0x24FFFFFF, EDGE_LIT = 0x38FFFFFF, EDGE_DARK = 0x60000000;
    public static final int PINK = 0xFFFF88C8, PINK_DEEP = 0xFFE86DB0, PINK_GLOW = 0x70FF88C8, ON_PINK = 0xFF2A1020;
    public static final int TEXT = 0xFFF3E9F8, SOFT = 0xFFBCA9CC, FAINT = 0xFF7E6C8E, GOOD = 0xFF8BE3A2, BAD = 0xFFFF6B7A, GOLD = 0xFFF2C14E;
    // spacing and rounding
    public static final int GAP = 4, PAD = 8, R_CARD = 12, R_INNER = 8, R_ROW = 6;
    // text sizes: M is the game's normal text. The others are as near to their share of that as whole screen pixels allow,
    // so at a small GUI scale XS and S can come out the same as M.
    public static final float TINY = 6, XS = 10, S = 11, M = 12, L = 15, XL = 18;

    private Ui() {}

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static int guiScale() {
        return Math.max(1, (int) Minecraft.getInstance().getWindow().getGuiScale());
    }

    // The mouse in GUI units, for code that isn't handed it.
    public static int mouseX() {
        Minecraft mc = Minecraft.getInstance();
        return (int) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / Math.max(1, mc.getWindow().getScreenWidth()));
    }

    public static int mouseY() {
        Minecraft mc = Minecraft.getInstance();
        return (int) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / Math.max(1, mc.getWindow().getScreenHeight()));
    }

    // A text scale for drawing in GUI units: the nearest one where a pixel of the font is a whole number of screen pixels.
    public static float snapScale(float k) {
        int gui = guiScale();
        return Math.max(1, Math.round(k * gui)) / (float) gui;
    }

    // The same for code that has already scaled the pose down to real screen pixels: whole numbers only.
    public static float snapRealScale(float k) {
        return Math.max(1, Math.round(k));
    }

    private static float scale(float size) {
        return snapScale(size / M);
    }

    // ---- measuring, in GUI units

    public static float width(String s, float size) {
        return font().width(s) * scale(size);
    }

    public static float width(Component c, float size) {
        return font().width(c) * scale(size);
    }

    // A wrapped line has to be measured at the size it was wrapped for.
    public static float widthOf(FormattedCharSequence line, float size) {
        return font().width(line) * scale(size);
    }

    // From one line of text to the next.
    public static float step(float size) {
        int gui = guiScale();
        return Math.round(10f * scale(size) * gui) / (float) gui;
    }

    // Lines that fit a width.
    public static List<FormattedCharSequence> wrap(Component c, float width, float size) {
        return font().split(c, Math.max(1, (int) (width / scale(size))));
    }

    public static List<FormattedCharSequence> wrap(String s, float width, float size) {
        return wrap(Component.literal(s), width, size);
    }

    public static List<FormattedCharSequence> lines(List<Component> text, float width, float size) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component c : text) out.addAll(wrap(c, width, size));
        return out;
    }

    // The biggest of these sizes that fits on one line. If none do, TINY: nothing should run out of its place.
    public static float fit(String s, float width, float... sizes) {
        for (float size : sizes) if (width(s, size) <= width) return size;
        return TINY;
    }

    public static float fit(Component c, float width, float... sizes) {
        for (float size : sizes) if (width(c, size) <= width) return size;
        return TINY;
    }

    // ---- drawing. y is the top of the letters.

    public static void line(GuiGraphics g, FormattedCharSequence line, float x, float y, int colour, float size) {
        float k = scale(size);
        int gui = guiScale();
        g.pose().pushPose();
        g.pose().translate(Math.round(x * gui) / (float) gui, Math.round(y * gui) / (float) gui, 0f);
        g.pose().scale(k, k, 1f);
        g.drawString(font(), line, 0, 0, colour, false);
        g.pose().popPose();
    }

    public static void text(GuiGraphics g, String s, float x, float y, int colour, float size) {
        line(g, Component.literal(s).getVisualOrderText(), x, y, colour, size);
    }

    public static void text(GuiGraphics g, Component c, float x, float y, int colour, float size) {
        line(g, c.getVisualOrderText(), x, y, colour, size);
    }

    public static void centre(GuiGraphics g, String s, float x, float y, int colour, float size) {
        text(g, s, x - width(s, size) / 2f, y, colour, size);
    }

    public static void right(GuiGraphics g, String s, float x, float y, int colour, float size) {
        text(g, s, x - width(s, size), y, colour, size);
    }

    // Wrapped text; returns where the next line would go. bottom: stop before going past this (-1 = no limit).
    public static float para(GuiGraphics g, Component c, float x, float y, float width, int colour, float size, float bottom) {
        float step = step(size);
        for (FormattedCharSequence l : wrap(c, width, size)) {
            if (bottom >= 0 && y + step > bottom + 1) break;
            line(g, l, x, y, colour, size);
            y += step;
        }
        return y;
    }

    public static float paraCentre(GuiGraphics g, Component c, float mid, float y, float width, int colour, float size) {
        float step = step(size);
        for (FormattedCharSequence l : wrap(c, width, size)) {
            line(g, l, mid - widthOf(l, size) / 2f, y, colour, size);
            y += step;
        }
        return y;
    }

    // The y to give a line of this size so its letters sit in the middle of mid.
    public static float middle(float mid, float size) {
        return mid - 3.5f * scale(size);
    }

    // The middle of the letters of a line whose top is at y.
    public static float letterMiddle(float y, float size) {
        return y + 3.5f * scale(size);
    }

    // A thin soft line.
    public static void divider(GuiGraphics g, float x0, float x1, float y) {
        Soft.round(g, x0, y, x1, y + 1, 0.5f, EDGE);
    }
}
