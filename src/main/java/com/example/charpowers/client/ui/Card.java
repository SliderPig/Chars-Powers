package com.example.charpowers.client.ui;

import com.example.charpowers.client.Soft;
import net.minecraft.client.gui.GuiGraphics;

// Layered rounded panels: a card floats on a soft shadow, a well is sunk into its card, a row sits on a well and lifts when the mouse is on it.
public final class Card {
    private Card() {}

    // The biggest layer: a soft shadow, a gentle top-to-bottom fade, a hairline edge lit along the top.
    public static void main(GuiGraphics g, float x0, float y0, float x1, float y1) {
        main(g, x0, y0, x1, y1, Ui.R_CARD, Ui.CARD_TOP, Ui.CARD_BOTTOM);
    }

    public static void main(GuiGraphics g, float x0, float y0, float x1, float y1, float r, int top, int bottom) {
        Soft.shadow(g, x0, y0 + 3, x1, y1 + 4, r, 14, Ui.SHADOW, true);
        Soft.round(g, x0, y0, x1, y1, r, top, bottom);
        Soft.ring(g, x0, y0, x1, y1, r, 1f, Ui.EDGE_LIT, Ui.EDGE);
    }

    // A layer behind the main card: a bit darker, a lighter shadow.
    public static void side(GuiGraphics g, float x0, float y0, float x1, float y1) {
        Soft.shadow(g, x0, y0 + 2, x1, y1 + 3, 10, 10, 0x70000000, true);
        Soft.round(g, x0, y0, x1, y1, 10, Ui.SIDE_TOP, Ui.SIDE_BOTTOM);
        Soft.ring(g, x0, y0, x1, y1, 10, 1f, Ui.EDGE, 0x14FFFFFF);
    }

    // Sunk into the card: dark, shaded along its top, with a faint lit lip along the bottom.
    public static void well(GuiGraphics g, float x0, float y0, float x1, float y1, float r) {
        Soft.round(g, x0, y0, x1, y1, r, Ui.WELL_TOP, Ui.WELL_BOTTOM);
        Soft.ring(g, x0, y0, x1, y1, r, 1f, Ui.EDGE_DARK, Ui.EDGE);
    }

    // A row on a well. lift goes 0 to 1 with the mouse.
    public static void row(GuiGraphics g, float x0, float y0, float x1, float y1, float r, float lift, boolean selected) {
        int top = Soft.mix(Ui.ROW_TOP, Ui.ROW_LIT_TOP, lift), bottom = Soft.mix(Ui.ROW_BOTTOM, Ui.ROW_LIT_BOTTOM, lift);
        if (lift > 0.01f) Soft.shadow(g, x0, y0 + 1, x1, y1 + 2, r, 5, Soft.alpha(0x60000000, lift), false);
        Soft.round(g, x0, y0, x1, y1, r, top, bottom);
        Soft.ring(g, x0, y0, x1, y1, r, 1f, selected ? Ui.PINK : Soft.mix(0x18FFFFFF, Ui.EDGE_LIT, lift), selected ? Ui.PINK_DEEP : 0x10FFFFFF);
    }

    // A small heading in the family style.
    public static void heading(GuiGraphics g, String text, float x, float y) {
        Ui.text(g, text.toUpperCase(), x, y, Ui.SOFT, Ui.XS);
    }
}
