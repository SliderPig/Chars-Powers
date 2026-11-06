package com.example.charpowers.client.ui;

import com.example.charpowers.client.Soft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

// A clipped area that scrolls smoothly, with a slim pill for a scrollbar that only shows when there's more than fits.
// begin, then draw your rows starting at y - offset(), then end. Rows outside the area are clipped, and their clicks too.
public final class ScrollList {
    private float target, shown;
    private float x0, y0, x1, y1, content;
    private final Object id = new Object();

    // The value of content is how tall everything inside is.
    public void begin(Frame ui, float x0, float y0, float x1, float y1, float content) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.content = content;
        target = Mth.clamp(target, 0f, most());
        // dragging the bar
        if (ui.held == id) {
            float thumb = thumbSize(), track = y1 - y0 - thumb;
            if (track > 0) target = Mth.clamp((ui.my - y0 - thumb / 2f) / track, 0f, 1f) * most();
        }
        shown = ui.anims.to(id, target, 18f);
        shown = Mth.clamp(shown, 0f, most());
        ui.g.flush();
        ui.g.enableScissor((int) Math.floor(x0), (int) Math.floor(y0), (int) Math.ceil(x1), (int) Math.ceil(y1));
        ui.clip(x0, y0, x1, y1);
    }

    public float offset() {
        return shown;
    }

    private float most() {
        return Math.max(0f, content - (y1 - y0));
    }

    private float thumbSize() {
        float view = y1 - y0;
        return Math.max(14f, view * view / Math.max(view, content));
    }

    public boolean needsBar() {
        return most() > 0.5f;
    }

    // How wide the bar is, to leave room for it.
    public static float barWidth() {
        return 5f;
    }

    // Soft fades at the edges where rows slide under, painted in the colour behind the list.
    public void end(Frame ui, int behindTop, int behindBottom) {
        GuiGraphics g = ui.g;
        g.flush();
        g.disableScissor();
        ui.noClip();
        if (!needsBar()) return;
        float fade = 7f;
        g.pose().pushPose();
        g.pose().translate(0f, 0f, 200f);
        if (shown > 0.5f) Soft.round(g, x0, y0, x1 - barWidth(), y0 + fade, 0f, behindTop, Soft.alpha(behindTop, 0f));
        if (shown < most() - 0.5f) Soft.round(g, x0, y1 - fade, x1 - barWidth(), y1, 0f, Soft.alpha(behindBottom, 0f), behindBottom);
        float thumb = thumbSize(), ty = y0 + (y1 - y0 - thumb) * (shown / most());
        float bx = x1 - 3f;
        boolean over = ui.over(bx - 3, y0, x1 + 1, y1);
        float lift = ui.hover(id.hashCode() + "b", over || ui.held == id);
        Soft.round(g, bx, y0 + 1, bx + 2, y1 - 1, 1f, 0x20FFFFFF);
        Soft.round(g, bx - lift * 0.5f, ty, bx + 2 + lift * 0.5f, ty + thumb, 1.5f, Soft.mix(0xB0FF88C8, Ui.PINK, lift));
        g.pose().popPose();
        ui.hit(bx - 3, y0, x1 + 1, y1, () -> ui.held = id);
    }

    public void end(Frame ui, int behind) {
        end(ui, behind, behind);
    }

    // True if the wheel was over this list.
    public boolean wheel(double mx, double my, double delta, float stepSize) {
        if (mx < x0 || mx >= x1 || my < y0 || my >= y1 || !needsBar()) return false;
        target = Mth.clamp(target - (float) delta * stepSize, 0f, most());
        return true;
    }

    public void toTop() {
        target = 0f;
    }
}
