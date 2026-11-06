package com.example.charpowers.armour.client;

import com.example.charpowers.client.Soft;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.PillButton;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.client.ui.UiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

// The visor's settings: a switch for every part, and three presets to start from. Opens with /suitvisor.
public class VisorScreen extends UiScreen {
    private static final float ROW = 17;

    public VisorScreen() {
        super(Component.literal("Suit Visor"));
    }

    @Override
    protected void paint(GuiGraphics g, int mx, int my, float partial) {
        List<VisorSettings.Part> parts = VisorSettings.PARTS;
        int perColumn = (parts.size() + 1) / 2;
        float w = Math.min(330, width - 16), h = Math.min(62 + perColumn * ROW + 14, height - 12);
        float x0 = (width - w) / 2f, y0 = (height - h) / 2f, x1 = x0 + w, y1 = y0 + h;
        Card.main(g, x0, y0, x1, y1);
        Ui.centre(g, "Suit Visor", (x0 + x1) / 2f, Ui.middle(y0 + 12, Ui.L), Ui.PINK, Ui.L);
        // presets
        String now = VisorSettings.currentPreset();
        float bw = 64, bh = 15, by = y0 + 25, bx = (x0 + x1) / 2f - (bw * 3 + 8) / 2f;
        String[] names = {"minimal", "standard", "full"};
        for (int i = 0; i < 3; i++) {
            String n = names[i];
            PillButton.draw(ui, "preset" + n, bx + i * (bw + 4), by, bw, bh, n.substring(0, 1).toUpperCase() + n.substring(1), n.equals(now) ? PillButton.Kind.PRIMARY : PillButton.Kind.NORMAL, true, () -> VisorSettings.preset(n));
        }
        float top = by + bh + 8;
        Card.well(g, x0 + 8, top, x1 - 8, y1 - 8, Ui.R_INNER);
        float colW = (w - 16) / 2f;
        for (int i = 0; i < parts.size(); i++) {
            VisorSettings.Part p = parts.get(i);
            int col = i / perColumn, row = i % perColumn;
            float rx = x0 + 8 + col * colW + 6, ry = top + 4 + row * ROW;
            float rw = colW - 12;
            boolean over = ui.over(rx, ry, rx + rw, ry + ROW - 2);
            float lift = ui.hover("row" + p.key(), over);
            if (lift > 0.01f) Soft.round(g, rx, ry, rx + rw, ry + ROW - 2, Ui.R_ROW, Soft.alpha(0xFF47335A, lift), Soft.alpha(0xFF3B2B4B, lift));
            float mid = ry + (ROW - 2) / 2f;
            Ui.text(g, p.label(), rx + 5, Ui.middle(mid, Ui.S), VisorSettings.has(p.key()) ? Ui.TEXT : Ui.FAINT, Ui.fit(p.label(), rw - 38, Ui.S, Ui.XS));
            PillButton.toggle(ui, "t" + p.key(), rx + rw - 24, mid - 5, 20, 10, VisorSettings.has(p.key()), () -> VisorSettings.set(p.key(), !VisorSettings.has(p.key())));
            if (over) ui.tooltip("tip" + p.key(), Component.literal(p.what()));
        }
    }
}
