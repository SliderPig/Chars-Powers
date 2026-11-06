package com.example.charpowers.client.ui;

import com.example.charpowers.client.Soft;

import java.util.List;
import java.util.function.IntConsumer;

// A row of labels in one soft trough, with a pink pill that slides to the one that's picked.
public final class Tabs {
    private Tabs() {}

    public static void draw(Frame ui, Object id, float x, float y, float w, float h, List<String> labels, int picked, IntConsumer onPick) {
        if (labels.isEmpty()) return;
        var g = ui.g;
        float r = h / 2f, each = w / labels.size();
        Soft.round(g, x, y, x + w, y + h, r, Ui.WELL_TOP, Ui.WELL_BOTTOM);
        Soft.ring(g, x, y, x + w, y + h, r, 1f, Ui.EDGE_DARK, Ui.EDGE);
        // picked can be -1: nothing is picked, so no pink pill
        float slide = picked < 0 ? -9f : ui.glide(id, picked);
        if (picked >= 0) {
            float px = x + slide * each;
            Soft.shadow(g, px + 1, y + 1, px + each - 1, y + h - 1, r - 1, 5, 0x40FF88C8, false);
            Soft.round(g, px + 1, y + 1, px + each - 1, y + h - 1, r - 1, Ui.PINK, Ui.PINK_DEEP);
        }
        for (int i = 0; i < labels.size(); i++) {
            float x0 = x + i * each;
            boolean over = i != picked && ui.over(x0, y, x0 + each, y + h);
            float lift = ui.hover(id + "#" + i, over);
            if (lift > 0.01f) Soft.round(g, x0 + 2, y + 2, x0 + each - 2, y + h - 2, r - 2, Soft.alpha(0x30FFFFFF, lift));
            String label = labels.get(i);
            float size = Ui.fit(label, each - 6, Ui.S, Ui.XS);
            float on = 1f - Math.min(1f, Math.abs(slide - i));
            Ui.centre(g, label, x0 + each / 2f, Ui.middle(y + h / 2f, size), Soft.mix(Soft.mix(Ui.SOFT, Ui.TEXT, lift), Ui.ON_PINK, on), size);
            int index = i;
            ui.hit(x0, y, x0 + each, y + h, () -> onPick.accept(index));
        }
    }
}
