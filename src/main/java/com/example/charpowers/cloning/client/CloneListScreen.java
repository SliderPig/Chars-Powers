package com.example.charpowers.cloning.client;

import com.example.charpowers.Net;
import com.example.charpowers.client.Soft;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.PillButton;
import com.example.charpowers.client.ui.ScrollList;
import com.example.charpowers.client.ui.TextBox;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.client.ui.UiScreen;
import com.example.charpowers.cloning.CloneInfo;
import com.example.charpowers.cloning.model.CloneRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// The clones menu (Clones button on Cloning's power page): every clone slot on the left, with a switch for whether the
// Clone key brings it, and for the one you pick: its nickname, job, health, food, where it is, its powers and everything
// it's carrying. Refreshes once a second.
public class CloneListScreen extends UiScreen {
    private static final int MOST_W = 390, MOST_H = 252, MARGIN = 6, ROW = 30, CELL = 17;
    private static final int ENTER = 257, KEYPAD_ENTER = 335;

    @Nullable
    private final Screen back;
    private List<CloneInfo> clones = new ArrayList<>();
    private int selected = 0;
    private int ticks = 0;
    private TextBox nameBox;
    private boolean chosen, nameWasFocused;
    private final ScrollList list = new ScrollList(), page = new ScrollList();
    private float pageH = 200;

    public CloneListScreen(@Nullable Screen back) {
        super(Component.literal("Your clones"));
        this.back = back;
    }

    public void update(List<CloneInfo> list) {
        clones = list;
        if (selected >= clones.size()) selected = 0;
        // until you've picked one yourself, start on one that's out
        if (!chosen && selected < clones.size() && !clones.get(selected).out()) {
            for (int i = 0; i < clones.size(); i++) {
                if (clones.get(i).out()) {
                    selected = i;
                    break;
                }
            }
        }
        if (nameBox != null && !nameBox.isFocused()) showName();
    }

    // Puts the picked clone's nickname in the box.
    private void showName() {
        CloneInfo c = current();
        if (c == null) return;
        nameBox.hintText("Clone " + (c.slot() + 1));
        if (!nameBox.getValue().equals(c.name())) nameBox.setValue(c.name());
    }

    private void saveName() {
        CloneInfo c = current();
        if (c == null || nameBox == null) return;
        String typed = CloneRules.cleanNickname(nameBox.getValue());
        if (!typed.equals(c.name())) Net.CHANNEL.sendToServer(new Net.CloneOrder(c.slot(), "name:" + typed));
    }

    private void leaveNameBox() {
        saveName();
        nameBox.setFocused(false);
        setFocused(null);
    }

    @Nullable
    private CloneInfo current() {
        return selected < clones.size() ? clones.get(selected) : null;
    }

    @Override
    protected void init() {
        nameBox = addWidget(new TextBox(font, CloneRules.NICK_MAX, "Nickname"));
        showName();
        Net.CHANNEL.sendToServer(new Net.RequestClones());
    }

    private void openOrders() {
        CloneInfo c = current();
        if (c != null && c.out() && minecraft != null) {
            saveName();
            minecraft.setScreen(new CloneScreen(c.slot(), c.label(), c.state(), c.health(), c.maxHealth(), c.food(), c.chest()));
        }
    }

    @Override
    public void tick() {
        if (++ticks % 20 == 0) Net.CHANNEL.sendToServer(new Net.RequestClones());
        nameBox.tick();
        // clicked somewhere else in the menu: that counts as finished typing
        if (nameWasFocused && !nameBox.isFocused()) saveName();
        nameWasFocused = nameBox.isFocused();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((key == ENTER || key == KEYPAD_ENTER) && nameBox.isFocused()) {
            leaveNameBox();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        saveName();
        if (minecraft != null) minecraft.setScreen(back);
    }

    @Override
    protected void paint(GuiGraphics g, int mx, int my, float partial) {
        float w = Math.min(MOST_W, width - MARGIN * 2), h = Math.min(MOST_H, height - MARGIN * 2);
        float x0 = (width - w) / 2f, y0 = (height - h) / 2f, x1 = x0 + w, y1 = y0 + h;
        Card.main(g, x0, y0, x1, y1);
        Ui.text(g, title, x0 + 12, Ui.middle(y0 + 11, Ui.L), Ui.PINK, Ui.L);
        String hint = "The switch: comes with your Clone key";
        float hintW = w - 24 - Ui.width(title, Ui.L) - 8;
        if (Ui.width(hint, Ui.XS) <= hintW) Ui.right(g, hint, x1 - 12, Ui.middle(y0 + 11, Ui.XS), Ui.FAINT, Ui.XS);
        float top = y0 + 22, bottom = y1 - 7;
        float listW = Math.max(92, Math.min(112, w * 0.3f));
        // left: the clones, then the buttons
        float buttons = 38;
        Card.well(g, x0 + 7, top, x0 + 7 + listW, bottom - buttons, Ui.R_INNER);
        list.begin(ui, x0 + 7, top + 2, x0 + 7 + listW, bottom - buttons - 2, clones.size() * ROW + 4);
        float rowW = listW - 6 - (list.needsBar() ? ScrollList.barWidth() : 0);
        for (int i = 0; i < clones.size(); i++) {
            cloneRow(g, i, clones.get(i), x0 + 10, top + 3 + i * ROW - list.offset(), rowW);
        }
        list.end(ui, Ui.WELL_TOP, Ui.WELL_BOTTOM);
        CloneInfo c = current();
        PillButton.draw(ui, "orders", x0 + 7, bottom - buttons + 4, listW, 15, "Orders", PillButton.Kind.PRIMARY, c != null && c.out(), this::openOrders);
        PillButton.draw(ui, "back", x0 + 7, bottom - 15, listW, 15, "Back", PillButton.Kind.NORMAL, true, this::onClose);
        // right: everything about the one you picked
        float px0 = x0 + 7 + listW + 5, px1 = x1 - 7;
        Card.well(g, px0, top, px1, bottom, Ui.R_INNER);
        page.begin(ui, px0, top + 2, px1, bottom - 2, pageH);
        float inner0 = px0 + 7, inner1 = px1 - 6 - (page.needsBar() ? ScrollList.barWidth() : 0);
        float start = top + 6 - page.offset();
        pageH = details(g, c, inner0, inner1, start) - start + 8;
        page.end(ui, Ui.WELL_TOP, Ui.WELL_BOTTOM);
    }

    private void cloneRow(GuiGraphics g, int i, CloneInfo c, float x, float y, float w) {
        float h = ROW - 3;
        boolean over = ui.over(x, y, x + w, y + h);
        float lift = ui.hover("clone" + i, over);
        Card.row(g, x, y, x + w, y + h, Ui.R_ROW, lift, i == selected);
        float room = w - 34;
        String label = c.label();
        Ui.text(g, label, x + 6, y + 3, c.out() ? Ui.TEXT : Ui.SOFT, Ui.fit(label, room, Ui.S, Ui.XS));
        String state = c.state();
        Ui.text(g, state, x + 6, y + 14, c.out() ? Ui.PINK : Ui.FAINT, Ui.fit(state, w - 10, Ui.XS));
        ui.hit(x, y, x + w, y + h, () -> {
            leaveNameBox();
            selected = i;
            chosen = true;
            page.toTop();
            showName();
        });
        PillButton.toggle(ui, "pick" + i, x + w - 24, y + 3, 20, 10, c.picked(), () -> Net.CHANNEL.sendToServer(new Net.CloneOrder(c.slot(), "pick")));
    }

    private float details(GuiGraphics g, @Nullable CloneInfo c, float x0, float x1, float y) {
        if (c == null) {
            Ui.text(g, "Nobody here yet", x0, y, Ui.FAINT, Ui.S);
            return y + 12;
        }
        float label = 36, wide = x1 - x0 - label;
        Ui.text(g, "Name", x0, Ui.middle(y + 7, Ui.S), Ui.SOFT, Ui.S);
        nameBox.draw(ui, x0 + label, y, Math.min(110, wide), 14);
        if (nameBox.isFocused() && wide > 160) Ui.text(g, "Enter saves", x0 + label + 114, Ui.middle(y + 7, Ui.XS), Ui.FAINT, Ui.XS);
        y += 18;
        if (!c.out()) {
            return Ui.para(g, Component.literal(c.state()), x0, y, x1 - x0, Ui.SOFT, Ui.S, -1);
        }
        y = line(g, "Job", c.task(), x0, y, label, wide);
        y = line(g, "Health", Math.round(c.health()) + "/" + Math.round(c.maxHealth()) + "    Food " + c.food() + "/20", x0, y, label, wide);
        y = line(g, "At", c.where(), x0, y, label, wide);
        y = line(g, "Chest", c.chest(), x0, y, label, wide);
        Ui.text(g, "Powers", x0, y, Ui.SOFT, Ui.S);
        MutableComponent powers = Component.empty();
        if (c.powers().isEmpty()) powers.append("None");
        for (int i = 0; i < c.powers().size(); i++) {
            if (i > 0) powers.append(", ");
            powers.append(c.powers().get(i));
        }
        y = Ui.para(g, powers, x0 + label, y, wide, Ui.TEXT, Ui.S, -1) + 4;
        return inventory(g, c, x0, y, x1);
    }

    private float line(GuiGraphics g, String name, String value, float x0, float y, float label, float wide) {
        Ui.text(g, name, x0, y, Ui.SOFT, Ui.S);
        return Ui.para(g, Component.literal(value), x0 + label, y, wide, Ui.TEXT, Ui.S, -1);
    }

    private float inventory(GuiGraphics g, CloneInfo c, float x, float y, float x1) {
        // armour and off hand along the top, then pockets, then the hotbar a little apart
        int[] worn = {39, 38, 37, 36, 40};
        for (int i = 0; i < worn.length; i++) slot(g, c, worn[i], x + i * CELL, y);
        float gy = y + CELL + 4;
        for (int i = 9; i < 36; i++) slot(g, c, i, x + ((i - 9) % 9) * CELL, gy + ((i - 9) / 9) * CELL);
        for (int i = 0; i < 9; i++) slot(g, c, i, x + i * CELL, gy + 3 * CELL + 4);
        return gy + 4 * CELL + 4;
    }

    private void slot(GuiGraphics g, CloneInfo c, int index, float x, float y) {
        float size = CELL - 1;
        boolean over = ui.over(x, y, x + size, y + size);
        float lift = ui.hover("s" + index, over);
        Soft.round(g, x, y, x + size, y + size, 4, Soft.mix(0xFF231A2B, 0xFF3F2F4E, lift), Soft.mix(0xFF2A2035, 0xFF33273F, lift));
        Soft.ring(g, x, y, x + size, y + size, 4, 1f, Soft.mix(0x18FFFFFF, 0xA0FF88C8, lift));
        ItemStack stack = index < c.items().size() ? c.items().get(index) : ItemStack.EMPTY;
        if (!stack.isEmpty()) {
            int ix = Math.round(x), iy = Math.round(y);
            g.renderItem(stack, ix, iy);
            g.renderItemDecorations(font, stack, ix, iy);
            if (over) ui.tooltipItem(stack);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (nameBox != null && button == 0 && nameBox.clickAt(mx, my)) {
            setFocused(nameBox);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return list.wheel(mx, my, delta, ROW) || page.wheel(mx, my, delta, CELL) || super.mouseScrolled(mx, my, delta);
    }
}
