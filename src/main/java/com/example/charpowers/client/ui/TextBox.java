package com.example.charpowers.client.ui;

import com.example.charpowers.client.Soft;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

// A rounded text box in the family look. The typing itself is the game's own box (cursor keys, selection, copy and paste);
// only the drawing is ours, so the text matches the rest of the menu and stays in its well.
public final class TextBox extends EditBox {
    private String hintText = "";
    private float shownFrom;
    private float focusGlow;

    public TextBox(Font font, int most, String hint) {
        super(font, 0, 0, 100, 16, Component.literal(hint));
        setMaxLength(most);
        setBordered(false);
        hintText = hint;
    }

    public void hintText(String hint) {
        hintText = hint;
    }

    // Game draws nothing by itself: call draw from the menu's paint.
    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float partial) {}

    public void draw(Frame ui, float x, float y, float w, float h) {
        setX((int) x);
        setY((int) y);
        setWidth((int) w);
        setHeight((int) h);
        GuiGraphics g = ui.g;
        boolean over = ui.over(x, y, x + w, y + h);
        focusGlow = ui.hover(this, isFocused() || over);
        float r = h / 2f;
        Soft.round(g, x, y, x + w, y + h, r, Ui.WELL_TOP, Ui.WELL_BOTTOM);
        Soft.ring(g, x, y, x + w, y + h, r, 1f, Soft.mix(Ui.EDGE_DARK, isFocused() ? Ui.PINK : 0x80FF88C8, focusGlow), Soft.mix(Ui.EDGE, isFocused() ? Ui.PINK_DEEP : 0x60FF88C8, focusGlow));
        float pad = Math.max(5f, r * 0.7f), inside = w - pad * 2f, size = Ui.S, ty = Ui.middle(y + h / 2f, size);
        String text = getValue();
        if (text.isEmpty() && !isFocused()) {
            Ui.text(g, hintText, x + pad, ty, Ui.FAINT, size);
            return;
        }
        // keeps the cursor in view: slides the text left when it runs past the end
        int cursor = Math.min(getCursorPosition(), text.length());
        float cursorAt = Ui.width(text.substring(0, cursor), size);
        if (cursorAt - shownFrom > inside - 1) shownFrom = cursorAt - inside + 1;
        if (cursorAt < shownFrom) shownFrom = cursorAt;
        shownFrom = Math.max(0f, Math.min(shownFrom, Math.max(0f, Ui.width(text, size) - inside + 1)));
        g.flush();
        g.enableScissor((int) (x + pad), (int) y, (int) (x + w - pad) + 1, (int) (y + h));
        // the highlighted part, if any
        String picked = getHighlighted();
        if (!picked.isEmpty() && isFocused()) {
            int a = text.indexOf(picked);
            if (a >= 0) {
                float from = x + pad - shownFrom + Ui.width(text.substring(0, a), size), to = from + Ui.width(picked, size);
                Soft.round(g, from, y + 2, to, y + h - 2, 2f, 0x70FF88C8);
            }
        }
        Ui.text(g, text, x + pad - shownFrom, ty, Ui.TEXT, size);
        if (isFocused() && (Util.getMillis() / 500) % 2 == 0) {
            float cx = x + pad - shownFrom + cursorAt;
            Soft.round(g, cx, y + 3, cx + 1, y + h - 3, 0.5f, Ui.PINK);
        }
        g.flush();
        g.disableScissor();
    }

    // Focus and the cursor from a click. True if it was on the box.
    public boolean clickAt(double mx, double my) {
        boolean on = mx >= getX() && mx < getX() + getWidth() && my >= getY() && my < getY() + getHeight();
        setFocused(on);
        if (on) {
            float pad = Math.max(5f, getHeight() / 2f * 0.7f), wanted = (float) mx - getX() - pad + shownFrom;
            String text = getValue();
            int best = text.length();
            for (int i = 0; i <= text.length(); i++) {
                if (Ui.width(text.substring(0, i), Ui.S) >= wanted) {
                    best = i;
                    break;
                }
            }
            setCursorPosition(best);
            setHighlightPos(best);
        }
        return on;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return button == 0 && clickAt(mx, my);
    }
}
