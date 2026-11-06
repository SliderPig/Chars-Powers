package com.example.charpowers.client;

import com.example.charpowers.Net;
import com.example.charpowers.client.power.PowerMenu;
import com.example.charpowers.client.ui.Frame;
import com.example.charpowers.client.ui.ScrollList;
import com.example.charpowers.client.ui.TextBox;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.hidden.HiddenIcons;
import com.example.charpowers.hidden.HiddenPowers;
import com.example.charpowers.hidden.model.Kind;
import com.example.charpowers.hidden.model.Loadout;
import com.example.charpowers.hidden.model.PowerDef;
import com.example.charpowers.hidden.model.PowerList;
import com.example.charpowers.hidden.model.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.threetag.palladium.client.screen.power.PowerTab;
import net.threetag.palladium.client.screen.power.PowerTabType;
import net.threetag.palladium.client.screen.power.PowersScreen;
import net.threetag.palladium.power.IPowerHolder;

import java.util.ArrayList;
import java.util.List;

// The "Hidden Powers" tab in Palladium's power menu.
// Left: every hidden power you own as a smoothly scrolling list (icon, name, rank letter), with a sort pill and a search box. Right: the 5 key
// slots and 10 always-on slots. Click a power, then a slot of the right kind to put it there; click a filled slot (nothing picked) to take it off.
public class HiddenPowersTab extends PowerTab {
    private static final int LIST_X = 5, LIST_W = 124, TOP = 5, BOTTOM = 164;
    private static final int SEARCH_Y = 21, SEARCH_H = 14, ROWS_Y = 39, ROW = 20;
    private static final int SIDE_X = 132, SIDE_W = 97, CELL = 17, STEP = 18;
    private static final int KEYS_Y = 21, ALWAYS_Y = 67;
    private static final int PANEL_TOP = 0xFF281F31, PANEL_BOTTOM = 0xFF211829;

    private static PowerList.Sort sort = PowerList.Sort.RANK_DOWN; // remembered while the game is open

    private String picked = null;
    private final ScrollList list = new ScrollList();
    private int insideX, insideY; // where the inside of the window is on screen (from the last draw)
    private TextBox search;

    // the list as shown, rebuilt only when something changed
    private List<PowerList.Entry> shown = List.of();
    private List<String> shownFrom = null;
    private PowerList.Sort shownSort = null;
    private String shownSearch = null;

    public HiddenPowersTab(Minecraft mc, PowersScreen screen, PowerTabType type, int index, IPowerHolder holder) {
        super(mc, screen, type, index, holder);
    }

    @Override
    public void populate() {}

    private List<PowerList.Entry> rows() {
        List<String> owned = ClientHidden.owned();
        String text = search == null ? "" : search.getValue();
        if (owned != shownFrom || sort != shownSort || !text.equals(shownSearch)) {
            List<PowerList.Entry> all = new ArrayList<>(owned.size());
            for (String id : owned) {
                PowerDef def = HiddenPowers.def(id).orElse(null);
                if (def == null) continue;
                all.add(new PowerList.Entry(id, HiddenPowers.name(id).getString(), def.rank(), def.kind() == Kind.KEY));
            }
            shown = PowerList.view(all, sort, text);
            shownFrom = owned;
            shownSort = sort;
            shownSearch = text;
        }
        return shown;
    }

    static int rankColour(Rank r) {
        return switch (r) {
            case F -> 0xFF8A8A8A;
            case E -> 0xFF6CC070;
            case D -> 0xFF5AA0E6;
            case C -> 0xFFA77BEA;
            case B -> 0xFFFF88C8;
            case A -> 0xFFF2C14E;
            case S -> 0xFFFF6B6B;
            case Z -> 0xFFFFFFFF;
        };
    }

    // One small place that draws a power's picture.
    private static void drawIcon(GuiGraphics g, String id, int x, int y) {
        if (HiddenIcons.drawn(id)) g.blit(HiddenIcons.texture(id), x, y, 0, 0, 16, 16, 16, 16); // the power's own picture
        else g.renderItem(new ItemStack(HiddenPowers.icon(id)), x, y);
    }

    private int listBottom() {
        return BOTTOM - 4;
    }

    // The sort pill: its words, with the arrow drawn as a shape.
    private String sortWords() {
        return sort.label().replace(" ▼", "").replace(" ▲", "");
    }

    private float sortWidth() {
        boolean arrow = !sortWords().equals(sort.label());
        return Ui.width(sortWords(), Ui.XS) + (arrow ? 18 : 12);
    }

    @Override
    public void drawContents(GuiGraphics g, int x, int y, int mouseX, int mouseY, float partial) {
        insideX = x;
        insideY = y;
        if (search == null) search = new TextBox(minecraft.font, 32, Component.translatable("gui.charpowers.search").getString());
        Frame ui = PowerMenu.ui;
        g.drawManaged(() -> {
            leftPanel(ui, g, x, y);
            rightPanel(ui, g, x, y);
        });
    }

    private void panel(GuiGraphics g, float x0, float y0, float x1, float y1) {
        Soft.round(g, x0, y0, x1, y1, Ui.R_ROW + 1, PANEL_TOP, PANEL_BOTTOM);
        Soft.ring(g, x0, y0, x1, y1, Ui.R_ROW + 1, 1f, Ui.EDGE, 0x10FFFFFF);
    }

    private void leftPanel(Frame ui, GuiGraphics g, int x, int y) {
        float x0 = x + LIST_X, x1 = x0 + LIST_W;
        panel(g, x0, y + TOP, x1, y + BOTTOM);
        List<PowerList.Entry> rows = rows();
        Ui.text(g, Component.translatable("gui.charpowers.list_title").getString() + " · " + ClientHidden.owned().size(), x0 + 7, Ui.middle(y + TOP + 9, Ui.XS), Ui.SOFT, Ui.XS);
        // the sort pill
        String label = sortWords();
        float cw = sortWidth(), cx = x1 - 4 - cw, cy = y + TOP + 3;
        boolean overChip = ui.over(cx, cy, cx + cw, cy + 12);
        float chipLift = ui.hover("hiddenSort", overChip);
        Soft.round(g, cx, cy, cx + cw, cy + 12, 6, Soft.mix(0xFF3A2C47, 0xFF5A4270, chipLift), Soft.mix(0xFF2C2138, 0xFF49355C, chipLift));
        Soft.ring(g, cx, cy, cx + cw, cy + 12, 6, 1f, Soft.mix(0x30FF88C8, 0xA0FF88C8, chipLift));
        Ui.text(g, label, cx + 6, Ui.middle(cy + 6f, Ui.XS), Ui.PINK, Ui.XS);
        if (sort.label().endsWith("▼")) Soft.chevron(g, cx + cw - 8, cy + 6.5f, 1.8f, 0, 1, 1.1f, Ui.PINK);
        else if (sort.label().endsWith("▲")) Soft.chevron(g, cx + cw - 8, cy + 5.5f, 1.8f, 0, -1, 1.1f, Ui.PINK);

        search.draw(ui, x0 + 4, y + SEARCH_Y, LIST_W - 8, SEARCH_H);

        float rowsTop = y + ROWS_Y, rowsBottom = y + listBottom();
        if (rows.isEmpty()) {
            Component none = Component.translatable(ClientHidden.owned().isEmpty() ? "gui.charpowers.none_yet" : "gui.charpowers.no_match");
            Ui.paraCentre(g, none, x0 + LIST_W / 2f, rowsTop + 20, LIST_W - 16, Ui.SOFT, Ui.S);
        }
        list.begin(ui, x0 + 3, rowsTop, x1 - 2, rowsBottom, rows.size() * ROW);
        float off = list.offset(), right = x1 - 2 - ScrollList.barWidth();
        for (int i = 0; i < rows.size(); i++) {
            float top = rowsTop + i * ROW - off;
            if (top + ROW < rowsTop || top > rowsBottom) continue;
            PowerList.Entry e = rows.get(i);
            boolean mine = e.id().equals(picked);
            boolean over = ui.over(x0 + 3, top, right, top + ROW - 1);
            float lift = ui.hover(e.id(), over);
            if (mine || lift > 0.01f) {
                Soft.round(g, x0 + 3, top, right, top + ROW - 1, Ui.R_ROW, Soft.alpha(mine ? 0xFF4A305C : 0xFF3F2F4E, mine ? 1f : lift), Soft.alpha(mine ? 0xFF3B2B4B : 0xFF33273F, mine ? 1f : lift));
                if (mine) Soft.ring(g, x0 + 3, top, right, top + ROW - 1, Ui.R_ROW, 1f, Ui.PINK, Ui.PINK_DEEP);
            }
            String rank = e.rank().name();
            float rankW = Ui.width(rank, Ui.S);
            float room = right - x0 - 3 - 21 - rankW - 5;
            // a long name goes onto two lines rather than losing its end
            List<FormattedCharSequence> nameLines = Ui.wrap(e.name(), room, Ui.S);
            float lineStep = Math.min(Ui.step(Ui.S), 9f);
            float nameY = top + (ROW - 1) / 2f - (nameLines.size() - 1) * lineStep / 2f;
            for (FormattedCharSequence nameLine : nameLines) {
                Ui.line(g, nameLine, x0 + 3 + 20, Ui.middle(nameY, Ui.S), Soft.mix(Ui.TEXT, 0xFFFFFFFF, lift), Ui.S);
                nameY += lineStep;
            }
            Ui.text(g, rank, right - 4 - rankW, Ui.middle(top + (ROW - 1) / 2f, Ui.S), rankColour(e.rank()), Ui.S);
            drawIcon(g, e.id(), (int) (x0 + 3 + 2), Math.round(top + 2));
        }
        list.end(ui, PANEL_BOTTOM);
    }

    private void rightPanel(Frame ui, GuiGraphics g, int x, int y) {
        float x0 = x + SIDE_X, x1 = x0 + SIDE_W;
        panel(g, x0, y + TOP, x1, y + BOTTOM);
        Kind pickedKind = picked == null ? null : HiddenPowers.def(picked).map(PowerDef::kind).orElse(null);
        float cells = x0 + (SIDE_W - (STEP * 5 - 1)) / 2f;
        Ui.text(g, Component.translatable("gui.charpowers.keys").getString().toUpperCase(), cells + 1, y + KEYS_Y - 12, Ui.SOFT, Ui.XS);
        for (int i = 0; i < Loadout.KEY_SLOTS; i++) {
            float cx = cells + i * STEP;
            slot(ui, g, "k" + i, cx, y + KEYS_Y, ClientHidden.key(i), pickedKind == Kind.ALWAYS_ON, pickedKind == Kind.KEY);
            Ui.centre(g, String.valueOf(i + 1), cx + CELL / 2f, y + KEYS_Y + CELL + 2, Ui.FAINT, Ui.XS);
        }
        Ui.text(g, Component.translatable("gui.charpowers.always").getString().toUpperCase(), cells + 1, y + ALWAYS_Y - 12, Ui.SOFT, Ui.XS);
        for (int i = 0; i < Loadout.ALWAYS_SLOTS; i++) {
            slot(ui, g, "a" + i, cells + (i % 5) * STEP, y + ALWAYS_Y + (i / 5) * STEP, ClientHidden.always(i), pickedKind == Kind.KEY, pickedKind == Kind.ALWAYS_ON);
        }
        // a little help line under the slots
        Component help = Component.translatable(picked == null ? "gui.charpowers.how" : "gui.charpowers.pick_slot");
        Ui.paraCentre(g, help, x0 + SIDE_W / 2f, y + ALWAYS_Y + 2 * STEP + 8, SIDE_W - 12, picked == null ? Ui.FAINT : Ui.PINK, Ui.XS);
    }

    // A slot: a pink ring when the picked power can go in it, dimmed when it can't.
    private void slot(Frame ui, GuiGraphics g, String key, float x, float y, String id, boolean dim, boolean wanted) {
        boolean over = ui.over(x, y, x + CELL, y + CELL);
        float lift = ui.hover(key, over);
        float pulse = wanted ? 0.6f + 0.4f * (float) Math.sin(net.minecraft.Util.getMillis() / 220.0) : 0f;
        if (wanted) Soft.shadow(g, x, y, x + CELL, y + CELL, 5, 5, Soft.alpha(Ui.PINK_GLOW, 0.4f + 0.4f * pulse), false);
        Soft.round(g, x, y, x + CELL, y + CELL, 5, Soft.mix(Ui.WELL_TOP, 0xFF2B2036, lift), Soft.mix(Ui.WELL_BOTTOM, 0xFF322640, lift));
        Soft.ring(g, x, y, x + CELL, y + CELL, 5, 1f, wanted ? Ui.PINK : Soft.mix(Ui.EDGE_DARK, 0xA0FF88C8, lift), wanted ? Ui.PINK_DEEP : Soft.mix(Ui.EDGE, 0x60FF88C8, lift));
        if (!id.isEmpty()) drawIcon(g, id, Math.round(x), Math.round(y));
        if (dim) {
            g.pose().pushPose();
            g.pose().translate(0f, 0f, 200f);
            Soft.round(g, x, y, x + CELL, y + CELL, 5, 0xB0120D17);
            g.pose().popPose();
        }
    }

    // What's under the mouse (relative to the inside of the window): a power, or a slot.
    private record Hit(String power, Boolean keySlot, int slot) {}

    private Hit hitAt(int rx, int ry) {
        List<PowerList.Entry> rows = rows();
        float right = LIST_X + LIST_W - 2 - ScrollList.barWidth();
        if (rx >= LIST_X + 3 && rx < right && ry >= ROWS_Y && ry < listBottom()) {
            int n = (int) ((ry - ROWS_Y + list.offset()) / ROW);
            return n >= 0 && n < rows.size() ? new Hit(rows.get(n).id(), null, -1) : null;
        }
        float cells = SIDE_X + (SIDE_W - (STEP * 5 - 1)) / 2f;
        for (int i = 0; i < Loadout.KEY_SLOTS; i++) {
            float sx = cells + i * STEP;
            if (rx >= sx && rx < sx + CELL && ry >= KEYS_Y && ry < KEYS_Y + CELL) return new Hit(null, true, i);
        }
        for (int i = 0; i < Loadout.ALWAYS_SLOTS; i++) {
            float sx = cells + (i % 5) * STEP, sy = ALWAYS_Y + (i / 5) * STEP;
            if (rx >= sx && rx < sx + CELL && ry >= sy && ry < sy + CELL) return new Hit(null, false, i);
        }
        return null;
    }

    @Override
    public void drawTooltips(GuiGraphics g, int mouseX, int mouseY, int x, int y, float alpha, boolean free) {
        Hit h = hitAt(mouseX, mouseY);
        if (h == null) return;
        String id = h.power() != null ? h.power() : (h.keySlot() ? ClientHidden.key(h.slot()) : ClientHidden.always(h.slot()));
        List<Component> lines = new ArrayList<>();
        if (id == null || id.isEmpty()) {
            lines.add(Component.translatable(h.keySlot() ? "gui.charpowers.empty_key" : "gui.charpowers.empty_always", h.slot() + 1));
        } else {
            PowerDef def = HiddenPowers.def(id).orElse(null);
            lines.add(HiddenPowers.name(id).copy().withStyle(st -> st.withColor(Ui.PINK & 0xFFFFFF)));
            if (def != null) {
                lines.add(Component.translatable("gui.charpowers.rank", def.rank().name()).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable(def.kind() == Kind.KEY ? "gui.charpowers.kind_key" : "gui.charpowers.kind_always").withStyle(ChatFormatting.GRAY));
            }
            lines.add(HiddenPowers.description(id));
        }
        PowerMenu.ui.tooltip(id == null ? "empty" : id, lines);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        int rx = (int) mouseX - insideX, ry = (int) mouseY - insideY;
        // search box
        if (search != null && search.clickAt(mouseX, mouseY)) return;
        // sort pill (top right of the list)
        float cw = sortWidth(), cx = LIST_X + LIST_W - 4 - cw;
        if (ry >= TOP + 3 && ry < TOP + 15 && rx >= cx && rx < cx + cw) {
            sort = sort.next();
            list.toTop();
            return;
        }
        Hit h = hitAt(rx, ry);
        if (h == null) { picked = null; return; }
        if (h.power() != null) {
            picked = h.power().equals(picked) ? null : h.power();
            return;
        }
        if (picked != null) {
            Net.CHANNEL.sendToServer(new Net.Equip(h.keySlot(), h.slot(), picked)); // the server checks the kind etc.
            picked = null;
        } else {
            String in = h.keySlot() ? ClientHidden.key(h.slot()) : ClientHidden.always(h.slot());
            if (!in.isEmpty()) Net.CHANNEL.sendToServer(new Net.Equip(h.keySlot(), h.slot(), ""));
        }
    }

    // --- forwarded by HiddenTabInput (Palladium's tabs don't get keys, typing or the mouse wheel) ---

    public boolean searchFocused() { return search != null && search.isFocused(); }

    // True = used (the menu must not also act on it, e.g. E closing the window while typing).
    public boolean keyPressed(int key, int scan, int mods) {
        if (!searchFocused()) return false;
        if (key == 256) { // Esc: leave the search box first
            search.setFocused(false);
            return true;
        }
        search.keyPressed(key, scan, mods);
        return true;
    }

    public boolean charTyped(char c, int mods) {
        if (!searchFocused()) return false;
        search.charTyped(c, mods);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return list.wheel(mouseX, mouseY, delta, ROW);
    }
}
