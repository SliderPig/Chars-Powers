package com.example.charpowers.client.power;

import com.example.charpowers.client.PowerPanels;
import com.example.charpowers.client.Soft;
import com.example.charpowers.client.power.model.MenuLayout;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.Frame;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.mixin.PowerTabAccessor;
import com.example.charpowers.mixin.PowersScreenAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.threetag.palladium.client.screen.power.PowerTab;
import net.threetag.palladium.client.screen.power.PowersScreen;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.icon.IIcon;

import java.util.List;

// Palladium's power menu, repainted: one rounded card with a real header, the tabs tucked in behind its top edge, the content in a well
// sunk into it. Palladium keeps doing all the thinking (tabs, buying, scrolling): this only paints, and places the tabs in a rail that
// scrolls instead of spilling round the window. The window stays where Palladium centres it, because other mods draw relative to that.
public final class PowerMenu {
    public static final int W = 252, H = 196, INSIDE_X = 9, INSIDE_Y = 18, INSIDE_W = 234, INSIDE_H = 169;
    private static final int TAB_W = MenuLayout.TAB_W, TAB_STEP = MenuLayout.TAB_STEP, TAB_H = MenuLayout.TAB_H, RAIL_INSET = MenuLayout.RAIL_INSET, ARROW = MenuLayout.ARROW;

    public static final Frame ui = new Frame();
    private static PowersScreen current;
    private static long opened;
    private static float railTarget;
    private static PowerTab lastSelected;

    private PowerMenu() {}

    public static void init(PowersScreen screen) {
        if (current != screen) {
            current = screen;
            opened = Util.getMillis();
            railTarget = 0;
            lastSelected = null;
            ui.anims.clear();
        }
    }

    private static List<PowerTab> tabs(PowersScreen s) {
        return ((PowersScreenAccessor) s).charpowers$tabs();
    }

    // ---- the tab rail

    private static float railRoom(PowersScreen s) {
        return MenuLayout.railRoom(tabs(s).size());
    }

    private static boolean railOverflows(PowersScreen s) {
        return MenuLayout.railOverflows(tabs(s).size());
    }

    private static float railMost(PowersScreen s) {
        return MenuLayout.railMost(tabs(s).size());
    }

    private static float railLeft(PowersScreen s, int x) {
        return x + RAIL_INSET + (railOverflows(s) ? ARROW + 2 : 0);
    }

    private static float railShown() {
        return ui.anims.to("rail", railTarget, 16f);
    }

    public static void railScroll(PowersScreen s, double delta) {
        railTarget = Mth.clamp(railTarget - (float) delta * TAB_STEP, 0, railMost(s));
    }

    public static boolean overRail(PowersScreen s, double mx, double my) {
        int x = (s.width - W) / 2, y = (s.height - H) / 2;
        return mx >= x && mx < x + W && my >= y - TAB_H - 2 && my < y;
    }

    // Is the mouse on this tab? (Palladium asks, for clicks and hover.)
    public static boolean overTab(PowersScreen s, PowerTab tab, int x, int y, double mx, double my) {
        if (tabs(s).size() < 2) return false;
        int i = tabs(s).indexOf(tab);
        if (i < 0) return false;
        float left = railLeft(s, x), tx = left + i * TAB_STEP - railShown();
        return mx >= Math.max(tx, left) && mx < Math.min(tx + TAB_W, left + railRoom(s)) && my >= y - TAB_H - 1 && my < y;
    }

    private static void keepSelectedInView(PowersScreen s) {
        if (s.selectedTab == lastSelected) return;
        lastSelected = s.selectedTab;
        int i = tabs(s).indexOf(s.selectedTab);
        if (i < 0) return;
        railTarget = MenuLayout.railToShow(railTarget, i, tabs(s).size());
    }

    // The ones not picked sit behind the card; the mouse lifts them. The picked one comes out of the card, with soft flares where they meet.
    private static void railBack(PowersScreen s, GuiGraphics g, int x, int y, int mx, int my) {
        List<PowerTab> tabs = tabs(s);
        if (tabs.size() < 2) return;
        boolean clip = railOverflows(s);
        float left = railLeft(s, x), shown = railShown();
        if (clip) g.enableScissor((int) left, y - TAB_H - 6, (int) (left + railRoom(s)), y + 4);
        for (int i = 0; i < tabs.size(); i++) {
            PowerTab tab = tabs.get(i);
            if (tab == s.selectedTab) continue;
            float tx = left + i * TAB_STEP - shown;
            float lift = ui.hover(tab, overTab(s, tab, x, y, mx, my));
            float top = y - TAB_H + 3 - lift * 2f;
            Soft.shape(g, tx, top, tx + TAB_W, y + 6, 7, 7, 0, 0, Soft.mix(0xFF251C2E, 0xFF3E2F4D, lift), Soft.mix(0xFF1B1422, 0xFF2F2439, lift));
            Soft.shape(g, tx, top, tx + TAB_W, y + 6, 7, 7, 0, 0, Ui.EDGE, 0x00FFFFFF);
        }
        if (clip) g.disableScissor();
    }

    private static void railPicked(PowersScreen s, GuiGraphics g, int x, int y) {
        List<PowerTab> tabs = tabs(s);
        if (tabs.size() < 2 || s.selectedTab == null) return;
        int i = tabs.indexOf(s.selectedTab);
        float left = railLeft(s, x), tx = left + i * TAB_STEP - ui.glide("railPicked", railShown());
        if (tx + TAB_W < left || tx > left + railRoom(s)) return;
        boolean clip = railOverflows(s);
        if (clip) g.enableScissor((int) left - 4, y - TAB_H - 6, (int) (left + railRoom(s)) + 4, y + 4);
        float top = y - TAB_H, px = 1f / Ui.guiScale();
        Soft.shape(g, tx, top, tx + TAB_W, y + 2, 7, 7, 0, 0, Ui.EDGE_LIT, Ui.EDGE);
        Soft.shape(g, tx + px, top + px, tx + TAB_W - px, y + 2, 7, 7, 0, 0, 0xFF3A2C47, Ui.CARD_TOP);
        Soft.cornerOutside(g, tx + px, y + 1, 4, -1, -1, Ui.CARD_TOP);
        Soft.cornerOutside(g, tx + TAB_W - px, y + 1, 4, 1, -1, Ui.CARD_TOP);
        if (clip) g.disableScissor();
    }

    private static void railArrows(PowersScreen s, GuiGraphics g, int x, int y) {
        if (!railOverflows(s)) return;
        float mid = y - TAB_H / 2f + 1;
        arrow(g, "railL", x + RAIL_INSET, mid, -1, railTarget > 0, () -> railTarget = Mth.clamp(railTarget - TAB_STEP * 3, 0, railMost(s)));
        arrow(g, "railR", x + W - RAIL_INSET - ARROW, mid, 1, railTarget < railMost(s), () -> railTarget = Mth.clamp(railTarget + TAB_STEP * 3, 0, railMost(s)));
    }

    private static void arrow(GuiGraphics g, String id, float x, float mid, int way, boolean enabled, Runnable what) {
        float y = mid - ARROW / 2f;
        boolean over = enabled && ui.over(x, y, x + ARROW, y + ARROW);
        float lift = ui.hover(id, over);
        Soft.round(g, x, y, x + ARROW, y + ARROW, ARROW / 2f, Soft.mix(0xFF2F2439, 0xFF5A4270, lift), Soft.mix(0xFF251C2E, 0xFF49355C, lift));
        Soft.ring(g, x, y, x + ARROW, y + ARROW, ARROW / 2f, 1f, Ui.EDGE_LIT, Ui.EDGE);
        Soft.chevron(g, x + ARROW / 2f, mid, 2.2f, way, 0, 1.3f, enabled ? Soft.mix(Ui.TEXT, Ui.PINK, lift) : Ui.FAINT);
        if (enabled) ui.hit(x, y, x + ARROW, y + ARROW, what);
    }

    private static void railIcons(PowersScreen s, GuiGraphics g, int x, int y) {
        List<PowerTab> tabs = tabs(s);
        if (tabs.size() < 2) return;
        Minecraft mc = Minecraft.getInstance();
        boolean clip = railOverflows(s);
        float left = railLeft(s, x), shown = railShown();
        if (clip) g.enableScissor((int) left, y - TAB_H - 6, (int) (left + railRoom(s)), y);
        for (int i = 0; i < tabs.size(); i++) {
            PowerTab tab = tabs.get(i);
            IPowerHolder holder = ((PowerTabAccessor) tab).charpowers$holder();
            float tx = left + i * TAB_STEP - shown;
            if (tx + TAB_W < left || tx > left + railRoom(s) || holder == null || holder.getPower() == null) continue;
            boolean picked = tab == s.selectedTab;
            float lift = picked ? 0f : ui.hover(tab, overTab(s, tab, x, y, ui.mx, ui.my));
            int iy = picked ? y - TAB_H + 3 : y - TAB_H + 6 - Math.round(lift * 2f);
            if (!picked) RenderSystem.setShaderColor(1f, 1f, 1f, 0.72f + 0.28f * lift);
            IIcon icon = holder.getPower().getIcon();
            icon.draw(mc, g, DataContext.forPower(mc.player, holder), Math.round(tx) + 5, iy);
            g.flush();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        if (clip) g.disableScissor();
    }

    // ---- what Palladium's own draws turn into

    // Before the tab draws its contents.
    public static void back(PowersScreen s, GuiGraphics g, int x, int y, int mx, int my, float partial) {
        init(s);
        boolean modal = s.overlayScreen != null;
        ui.begin(g, modal ? -9999 : mx, modal ? -9999 : my, partial);
        keepSelectedInView(s);
        float open = Soft.easeOut((Util.getMillis() - opened) / 260f);
        int fx = ui.mx, fy = ui.my;
        g.drawManaged(() -> {
            PowerPanels.behind(ui, s, x, y, open);
            railBack(s, g, x, y, fx, fy);
            Card.main(g, x, y, x + W, y + H);
            railPicked(s, g, x, y);
            header(s, g, x, y);
            Card.well(g, x + INSIDE_X, y + INSIDE_Y, x + INSIDE_X + INSIDE_W, y + INSIDE_Y + INSIDE_H, Ui.R_INNER);
        });
    }

    private static void header(PowersScreen s, GuiGraphics g, int x, int y) {
        PowerTab tab = s.selectedTab;
        Component title = tab == null ? Component.translatable("gui.palladium.powers") : tab.getTitle();
        float right = PowerPanels.inHeader(ui, s, x, y);
        if (right >= x + W - INSIDE_X - 4) {
            Glyphs.cat(g, x + W - 20, y + INSIDE_Y / 2f + 0.5f, 0.82f, 0xFF0E0912, Ui.PINK);
            right -= 16;
        }
        float room = right - (x + INSIDE_X + 3) - 4;
        float size = Ui.fit(title, room, Ui.M, Ui.S, Ui.XS);
        Ui.text(g, title, x + INSIDE_X + 3, Ui.middle(y + INSIDE_Y / 2f + 0.5f, size), Ui.TEXT, size);
    }

    // After the contents: rounds them off where they meet the card, then the tab icons on top.
    public static void window(PowersScreen s, GuiGraphics g, int x, int y) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        int ix = x + INSIDE_X, iy = y + INSIDE_Y, ex = ix + INSIDE_W, ey = iy + INSIDE_H, r = Ui.R_INNER;
        g.pose().pushPose();
        g.pose().translate(0f, 0f, 300f);
        g.drawManaged(() -> {
            int top = Soft.mix(Ui.CARD_TOP, Ui.CARD_BOTTOM, INSIDE_Y / (float) H), bottom = Soft.mix(Ui.CARD_TOP, Ui.CARD_BOTTOM, (INSIDE_Y + INSIDE_H) / (float) H);
            Soft.cornerOutside(g, ix, iy, r, 1, 1, top);
            Soft.cornerOutside(g, ex, iy, r, -1, 1, top);
            Soft.cornerOutside(g, ix, ey, r, 1, -1, bottom);
            Soft.cornerOutside(g, ex, ey, r, -1, -1, bottom);
            Soft.ring(g, ix, iy, ex, ey, r, 1f, Ui.EDGE_DARK, Ui.EDGE);
            railArrows(s, g, x, y);
        });
        g.pose().popPose();
        railIcons(s, g, x, y);
        PowerPanels.icons(ui, s, x, y);
    }

    // After that: the tab's tooltips, and the names of the tabs.
    public static void tooltips(PowersScreen s, GuiGraphics g, int mx, int my, int x, int y, float partial) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        PowerTab tab = s.selectedTab;
        boolean modal = s.overlayScreen != null;
        if (tab != null) {
            g.pose().pushPose();
            g.pose().translate(x + INSIDE_X, y + INSIDE_Y, 400f);
            tab.drawTooltips(g, mx - x - INSIDE_X, my - y - INSIDE_Y, x, y, partial, modal);
            g.pose().popPose();
        }
        if (!modal) {
            List<PowerTab> tabs = tabs(s);
            if (tabs.size() > 1) {
                for (PowerTab t : tabs) {
                    if (overTab(s, t, x, y, mx, my)) {
                        ui.tooltip(t, t.getTitle());
                        break;
                    }
                }
            }
        }
        ui.end();
    }

    public static boolean click(PowersScreen s, double mx, double my, int button) {
        return s.overlayScreen == null && button == 0 && ui.click(mx, my);
    }
}
