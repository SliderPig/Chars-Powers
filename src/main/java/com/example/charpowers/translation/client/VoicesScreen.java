package com.example.charpowers.translation.client;

import com.example.charpowers.client.Soft;
import com.example.charpowers.client.power.Glyphs;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.PillButton;
import com.example.charpowers.client.ui.ScrollList;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.client.ui.UiScreen;
import com.example.charpowers.translation.model.VoiceRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The list in the sounds menu: every mob in your voice folders, how many of its lines you've heard (the quest), and
// once awakened, a switch for whether that mob gets to speak.
public class VoicesScreen extends UiScreen {
    private static final int MOST_W = 260, MOST_H = 230, ROW = 18;

    private final Screen back;
    private final List<String> mobs = new ArrayList<>();
    private final Map<String, int[]> counts = new LinkedHashMap<>();
    private final ScrollList list = new ScrollList();

    public VoicesScreen(Screen back) {
        super(Component.literal("Mob Voices"));
        this.back = back;
    }

    @Override
    protected void init() {
        VoiceClient.reload();
        Set<String> heard = VoiceClient.heard();
        counts.clear();
        for (VoiceRules.Line line : VoiceClient.lines()) {
            int[] c = counts.computeIfAbsent(line.mob(), k -> new int[2]);
            c[1]++;
            if (heard.contains(line.id())) c[0]++;
        }
        mobs.clear();
        mobs.addAll(counts.keySet());
    }

    @Override
    protected void paint(GuiGraphics g, int mx, int my, float partial) {
        float w = Math.min(MOST_W, width - 16), h = Math.min(mobs.isEmpty() ? 104 : MOST_H, height - 16);
        float x0 = (width - w) / 2f, y0 = (height - h) / 2f, x1 = x0 + w, y1 = y0 + h;
        Card.main(g, x0, y0, x1, y1);
        Ui.centre(g, "Mob Voices", (x0 + x1) / 2f, Ui.middle(y0 + 12, Ui.L), Ui.PINK, Ui.L);
        float top = y0 + 23, bottom = y1 - 8;
        Card.well(g, x0 + 8, top, x1 - 8, bottom, Ui.R_INNER);
        if (mobs.isEmpty()) {
            float mid = (x0 + x1) / 2f, y = (top + bottom) / 2f - 20;
            y = Ui.paraCentre(g, Component.literal("No voice lines yet."), mid, y, w - 32, Ui.TEXT, Ui.M) + 2;
            Ui.paraCentre(g, Component.literal("They come from your own resource pack: docs/voice-pack.md in the mod's folder."), mid, y, w - 32, Ui.SOFT, Ui.S);
            return;
        }
        boolean awake = VoiceClient.awakened();
        list.begin(ui, x0 + 8, top + 2, x1 - 8, bottom - 2, mobs.size() * ROW + 4);
        float rowX = x0 + 12, rowW = w - 24 - (list.needsBar() ? ScrollList.barWidth() : 0);
        for (int i = 0; i < mobs.size(); i++) {
            String mob = mobs.get(i);
            int[] c = counts.get(mob);
            float y = top + 4 + i * ROW - list.offset();
            if (y + ROW < top || y > bottom) continue;
            boolean over = awake && ui.over(rowX, y, rowX + rowW, y + ROW - 2);
            float lift = ui.hover(mob, over);
            if (lift > 0.01f) Soft.round(g, rowX, y, rowX + rowW, y + ROW - 2, Ui.R_ROW, Soft.alpha(0xFF47335A, lift), Soft.alpha(0xFF3B2B4B, lift));
            boolean done = c[0] >= c[1];
            float my0 = y + (ROW - 2) / 2f;
            Soft.round(g, rowX + 3, my0 - 5, rowX + 13, my0 + 5, 3, done ? Ui.PINK : 0xFF2A2035, done ? Ui.PINK_DEEP : 0xFF2A2035);
            if (done) Glyphs.tick(g, rowX + 8, my0, 2.6f, 1.2f, Ui.ON_PINK);
            else Soft.ring(g, rowX + 3, my0 - 5, rowX + 13, my0 + 5, 3, 1f, Ui.EDGE_LIT);
            String name = mob.replace('_', ' ');
            String count = c[0] + "/" + c[1];
            float countW = Ui.width(count, Ui.S), right = rowX + rowW - (awake ? 32 : 5);
            Ui.text(g, name, rowX + 18, Ui.middle(my0, Ui.S), done ? Ui.PINK : Ui.TEXT, Ui.fit(name, right - countW - rowX - 24, Ui.S, Ui.XS));
            Ui.right(g, count, right, Ui.middle(my0, Ui.S), Ui.SOFT, Ui.S);
            if (awake) PillButton.toggle(ui, "v" + mob, rowX + rowW - 26, my0 - 5, 20, 10, !VoiceClient.isOff(mob), () -> VoiceClient.setOff(mob, !VoiceClient.isOff(mob)));
        }
        list.end(ui, Ui.WELL_TOP, Ui.WELL_BOTTOM);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return list.wheel(mx, my, delta, ROW) || super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(back);
    }
}
