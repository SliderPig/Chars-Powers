package com.example.charpowers.client.ui;

import com.example.charpowers.client.Soft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

// The family's tooltip: a small floating rounded card that keeps clear of the mouse and always stays on the screen.
public final class Tooltip {
    private static final float MOST_WIDE = 170, PAD_X = 7, PAD_Y = 5;

    private Tooltip() {}

    // Drawn at the mouse, on top of everything.
    public static void draw(GuiGraphics g, List<Component> lines, float mx, float my, float alpha) {
        drawAt(g, lines, mx, my, 0f, 0f, alpha);
    }

    // originX, originY: where the pose's zero is on the screen, when Palladium has moved it (its tabs' tooltips are drawn relative to the window).
    public static void drawAt(GuiGraphics g, List<Component> lines, float mx, float my, float originX, float originY, float alpha) {
        if (lines.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth(), screenH = mc.getWindow().getGuiScaledHeight();
        float size = Ui.S, step = Ui.step(size);
        List<FormattedCharSequence> wrapped = Ui.lines(lines, Math.min(MOST_WIDE, screenW - 20), size);
        float w = 0;
        for (FormattedCharSequence l : wrapped) w = Math.max(w, Ui.widthOf(l, size));
        float h = wrapped.size() * step + PAD_Y * 2, bw = w + PAD_X * 2;
        // beside the mouse; the other side if there's no room, then pushed back onto the screen
        float x = mx + originX + 11, y = my + originY - 8;
        if (x + bw > screenW - 3) x = mx + originX - bw - 9;
        x = Math.max(3, Math.min(x, screenW - bw - 3));
        y = Math.max(3, Math.min(y, screenH - h - 3));
        int fade = (int) (Math.min(1f, alpha) * 255);
        if (fade < 8) return;
        final float bx = x - originX, by = y - originY, width = bw;
        g.pose().pushPose();
        g.pose().translate(0f, 0f, 420f);
        g.drawManaged(() -> {
            Soft.shadow(g, bx, by + 2, bx + width, by + h + 2, 7, 8, Soft.alpha(0xC0000000, alpha), true);
            Soft.round(g, bx, by, bx + width, by + h, 7, Soft.alpha(0xFF2C2036, alpha), Soft.alpha(0xFF1E1626, alpha));
            Soft.ring(g, bx, by, bx + width, by + h, 7, 1f, Soft.alpha(0x60FF88C8, alpha), Soft.alpha(0x30FF88C8, alpha));
            float ty = by + PAD_Y;
            for (FormattedCharSequence l : wrapped) {
                Ui.line(g, l, bx + PAD_X, ty, Soft.alpha(Ui.TEXT, alpha), size);
                ty += step;
            }
        });
        g.pose().popPose();
    }
}
