package com.example.charpowers.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// What a menu needs while it paints itself: the mouse, the things you can click this frame, the little animations, and a tooltip
// to draw last so it goes on top of everything.
public final class Frame {
    private record Hit(float x0, float y0, float x1, float y1, Runnable what) {}

    public final Anims anims = new Anims();
    public GuiGraphics g;
    public int mx, my;
    public float partial;
    // shapes that follow the mouse while it's held (slider knobs, scrollbars)
    @Nullable
    public Object held;

    private final List<Hit> hits = new ArrayList<>();
    private final List<Hit> painting = new ArrayList<>();
    private float clipX0 = -1e6f, clipY0 = -1e6f, clipX1 = 1e6f, clipY1 = 1e6f;
    private List<Component> tipLines;
    private ItemStack tipItem;
    private Object tipKey;

    public void begin(GuiGraphics g, int mx, int my, float partial) {
        this.g = g;
        this.mx = mx;
        this.my = my;
        this.partial = partial;
        anims.frame();
        painting.clear();
        tipLines = null;
        tipItem = null;
        tipKey = null;
        noClip();
        if (!Minecraft.getInstance().mouseHandler.isLeftPressed()) held = null;
    }

    // This frame's click zones replace last frame's.
    public void end() {
        hits.clear();
        hits.addAll(painting);
        drawTooltip();
    }

    public void clip(float x0, float y0, float x1, float y1) {
        clipX0 = x0;
        clipY0 = y0;
        clipX1 = x1;
        clipY1 = y1;
    }

    public void noClip() {
        clip(-1e6f, -1e6f, 1e6f, 1e6f);
    }

    public boolean over(float x0, float y0, float x1, float y1) {
        return mx >= x0 && mx < x1 && my >= y0 && my < y1 && mx >= clipX0 && mx < clipX1 && my >= clipY0 && my < clipY1;
    }

    // A click zone. Later ones are on top.
    public void hit(float x0, float y0, float x1, float y1, Runnable what) {
        painting.add(new Hit(Math.max(x0, clipX0), Math.max(y0, clipY0), Math.min(x1, clipX1), Math.min(y1, clipY1), what));
    }

    public boolean click(double x, double y) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (x >= h.x0 && x < h.x1 && y >= h.y0 && y < h.y1) {
                h.what.run();
                return true;
            }
        }
        return false;
    }

    // ---- hover helpers: a value from 0 to 1 that glides in and out

    public float hover(Object key, boolean over) {
        return anims.to(key, over ? 1f : 0f, 16f);
    }

    public float glide(Object key, float target) {
        return anims.to(key, target, 14f);
    }

    public void tooltip(Object key, Component... lines) {
        tipLines = List.of(lines);
        tipKey = key;
    }

    public void tooltip(Object key, List<Component> lines) {
        tipLines = new ArrayList<>(lines);
        tipKey = key;
    }

    public void tooltipItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        tipItem = stack;
        tipKey = "item";
    }

    private void drawTooltip() {
        float wanted = tipLines != null || tipItem != null ? 1f : 0f;
        float fade = anims.from("tooltip", 0f, wanted, 22f);
        if (tipKey == null) return;
        List<Component> lines = tipItem != null
                ? net.minecraft.client.gui.screens.Screen.getTooltipFromItem(Minecraft.getInstance(), tipItem)
                : tipLines;
        Tooltip.draw(g, lines, mx, my, Math.max(0.3f, fade));
    }
}
