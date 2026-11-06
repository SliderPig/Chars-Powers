package com.example.charpowers.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.client.power.Glyphs;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.PillButton;
import com.example.charpowers.client.ui.ScrollList;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.client.ui.UiScreen;
import com.example.charpowers.own.OpMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

// The op menu. A list of powers on the left, whatever you pick on the right, both scrolling, so it fits any window. It only shows what
// the server sends and only asks for changes: the server checks you're an op every time.
public class OpScreen extends UiScreen {
    private static final int MOST_W = 470, MOST_H = 330, MARGIN = 8, ROW = 18, GAP = 3, LINE = 20;
    private static final int RIGHT_TOP = Ui.WELL_TOP, RIGHT_BOTTOM = Ui.WELL_BOTTOM;

    private CompoundTag state;
    private String picked = "general";
    private final ScrollList menu = new ScrollList(), page = new ScrollList();
    private float contentH;

    private OpScreen(CompoundTag state) {
        super(Component.literal("Char's Powers: op menu"));
        this.state = state;
    }

    private static void receive(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof OpScreen open) open.state = tag;
        else if (tag.getBoolean("open") && mc.screen == null) mc.setScreen(new OpScreen(tag));
    }

    private static void send(String action) {
        Net.CHANNEL.sendToServer(new Net.OwnToServer(OpMenu.CHANNEL, action));
    }

    private static double bigger(double step) {
        return hasShiftDown() ? step * 10 : step;
    }

    private static String number(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.2f", v);
    }

    @Override
    protected void paint(GuiGraphics g, int mx, int my, float partial) {
        float w = Math.min(MOST_W, width - MARGIN * 2), h = Math.min(MOST_H, height - MARGIN * 2);
        float x0 = (width - w) / 2f, y0 = (height - h) / 2f, x1 = x0 + w, y1 = y0 + h;
        Card.main(g, x0, y0, x1, y1);
        Glyphs.cat(g, x0 + 16, y0 + 12, 0.85f, 0xFF0E0912, Ui.PINK);
        Ui.text(g, "Op menu", x0 + 26, Ui.middle(y0 + 12, Ui.M), Ui.TEXT, Ui.M);
        Ui.right(g, "Char's Powers", x1 - 12, Ui.middle(y0 + 12, Ui.XS), Ui.FAINT, Ui.XS);
        float top = y0 + 23, bottom = y1 - 8;
        float listW = Math.max(92, Math.min(136, w * 0.3f));
        // the list of powers
        Card.well(g, x0 + 8, top, x0 + 8 + listW, bottom, Ui.R_INNER);
        ListTag powers = state.getList("powers", Tag.TAG_COMPOUND);
        menu.begin(ui, x0 + 8, top + 2, x0 + 8 + listW, bottom - 2, 4 + (powers.size() + 1) * (ROW + GAP));
        float y = top + 4 - menu.offset(), rowX = x0 + 11, rowW = listW - 6 - (menu.needsBar() ? ScrollList.barWidth() : 0);
        menuRow("general", "General", true, rowX, y, rowW);
        y += ROW + GAP;
        CompoundTag current = null;
        for (int i = 0; i < powers.size(); i++) {
            CompoundTag row = powers.getCompound(i);
            String path = row.getString("path");
            if (picked.equals(path)) current = row;
            menuRow(path, Component.translatable("power.charpowers." + path).getString(), row.getBoolean("on"), rowX, y, rowW);
            y += ROW + GAP;
        }
        menu.end(ui, Ui.WELL_TOP, Ui.WELL_BOTTOM);
        // what's picked
        float px0 = x0 + 8 + listW + 5, px1 = x1 - 8;
        Card.well(g, px0, top, px1, bottom, Ui.R_INNER);
        page.begin(ui, px0, top + 2, px1, bottom - 2, contentH);
        float inner0 = px0 + 7, inner1 = px1 - 6 - (page.needsBar() ? ScrollList.barWidth() : 0);
        float start = top + 6 - page.offset();
        float end = current == null ? general(inner0, inner1, start) : power(current, inner0, inner1, start);
        contentH = end - start + 10;
        page.end(ui, RIGHT_TOP, RIGHT_BOTTOM);
    }

    private void menuRow(String key, String name, boolean on, float x, float y, float w) {
        boolean mine = picked.equals(key);
        boolean over = ui.over(x, y, x + w, y + ROW);
        float lift = ui.hover("row" + key, over);
        GuiGraphics g = ui.g;
        if (mine) {
            Soft.round(g, x, y, x + w, y + ROW, ROW / 2f, Ui.PINK, Ui.PINK_DEEP);
        } else if (lift > 0.01f) {
            Soft.round(g, x, y, x + w, y + ROW, ROW / 2f, Soft.alpha(0xFF47335A, lift), Soft.alpha(0xFF3B2B4B, lift));
        }
        float room = w - 16 - (on ? 0 : 8);
        float size = Ui.fit(name, room, Ui.S, Ui.XS);
        Ui.text(g, name, x + 8, Ui.middle(y + ROW / 2f, size), mine ? Ui.ON_PINK : on ? Ui.TEXT : Ui.FAINT, size);
        if (!on) Soft.disc(g, x + w - 8, y + ROW / 2f, 2f, mine ? Ui.ON_PINK : Ui.BAD);
        ui.hit(x, y, x + w, y + ROW, () -> {
            picked = key;
            page.toTop();
        });
    }

    // A row: its words wrap, so nothing is ever cut off, and the row grows to fit.
    private float rowBack(String label, float x0, float x1, float y, float room) {
        List<FormattedCharSequence> lines = Ui.wrap(label, room, Ui.S);
        float h = Math.max(LINE, lines.size() * Ui.step(Ui.S) + 7);
        Soft.round(ui.g, x0, y, x1, y + h, Ui.R_ROW, Ui.ROW_TOP, Ui.ROW_BOTTOM);
        Soft.ring(ui.g, x0, y, x1, y + h, Ui.R_ROW, 1f, 0x18FFFFFF, 0x10FFFFFF);
        float ty = y + (h - lines.size() * Ui.step(Ui.S)) / 2f + 0.5f;
        for (FormattedCharSequence line : lines) {
            Ui.line(ui.g, line, x0 + 7, ty, Ui.TEXT, Ui.S);
            ty += Ui.step(Ui.S);
        }
        return h;
    }

    private float numberRow(String id, float x0, float x1, float y, String label, String shown, Runnable less, Runnable more) {
        float valueW = Math.max(28, Ui.width(shown, Ui.S) + 4);
        float h = rowBack(label, x0, x1, y, x1 - x0 - 14 - 34 - valueW);
        float mid = y + h / 2f;
        Ui.right(ui.g, shown, x1 - 40, Ui.middle(mid, Ui.S), Ui.PINK, Ui.S);
        PillButton.round(ui, id + "-", x1 - 28, mid, 7, '-', true, less);
        PillButton.round(ui, id + "+", x1 - 11, mid, 7, '+', true, more);
        return h;
    }

    private float flagRow(String id, float x0, float x1, float y, String label, boolean on, Runnable flip) {
        float h = rowBack(label, x0, x1, y, x1 - x0 - 14 - 30);
        PillButton.toggle(ui, id, x1 - 30, y + h / 2f - 6, 24, 12, on, flip);
        return h;
    }

    private float general(float x0, float x1, float y) {
        GuiGraphics g = ui.g;
        Ui.text(g, "General", x0, y, Ui.PINK, Ui.L);
        y += 17;
        ListTag rows = state.getList("general", Tag.TAG_COMPOUND);
        for (int i = 0; i < rows.size(); i++) {
            CompoundTag row = rows.getCompound(i);
            String key = row.getString("key");
            double step = row.getDouble("step");
            if (row.getBoolean("isFlag")) {
                y += flagRow("g" + key, x0, x1, y, row.getString("label"), row.getBoolean("flag"), () -> send("general:" + key)) + GAP;
            } else {
                y += numberRow("g" + key, x0, x1, y, row.getString("label"), number(row.getDouble("value")), () -> send("general:" + key + ":" + (-bigger(step))), () -> send("general:" + key + ":" + bigger(step))) + GAP;
            }
        }
        y += 3;
        return Ui.para(g, Component.literal("Hold Shift for bigger steps. These are saved in the server's mod config."), x0, y, x1 - x0, Ui.SOFT, Ui.XS, -1);
    }

    private float power(CompoundTag row, float x0, float x1, float y) {
        GuiGraphics g = ui.g;
        String path = row.getString("path");
        Ui.text(g, Component.translatable("power.charpowers." + path).getString(), x0, y, Ui.PINK, Ui.L);
        y += 17;
        y += flagRow(path + "on", x0, x1, y, "Power is on", row.getBoolean("on"), () -> send("flip:" + path)) + GAP;
        y += numberRow(path + "rate", x0, x1, y, "Mastery speed", "x" + number(row.getDouble("rate")), () -> send("rate:" + path + ":-" + bigger(0.1)), () -> send("rate:" + path + ":" + bigger(0.1))) + GAP;
        ListTag numbers = row.getList("numbers", Tag.TAG_COMPOUND);
        for (int i = 0; i < numbers.size(); i++) {
            CompoundTag n = numbers.getCompound(i);
            String key = n.getString("key");
            double step = n.getDouble("step");
            y += numberRow(path + key, x0, x1, y, n.getString("label"), number(n.getDouble("value")), () -> send("add:" + path + ":" + key + ":" + (-bigger(step))), () -> send("add:" + path + ":" + key + ":" + bigger(step))) + GAP;
        }
        y += 2;
        PillButton.draw(ui, path + "reset", x0, y, 70, 16, "Defaults", PillButton.Kind.NORMAL, true, () -> send("reset:" + path));
        if (row.contains("info")) {
            y = Math.max(y + 18, Ui.para(g, Component.literal(row.getString("info")), x0 + 76, y + 2, x1 - x0 - 76, Ui.SOFT, Ui.XS, -1));
        } else {
            y += 18;
        }
        y += 6;
        // someone to do things to
        Ui.divider(g, x0, x1, y);
        y += 6;
        float mid = y + 8;
        PillButton.round(ui, "pick-", x0 + 8, mid, 8, '<', true, () -> send("pick:-1"));
        String who = state.getString("player");
        float whoW = Math.max(50, Ui.width(who, Ui.M));
        Ui.text(g, who, x0 + 20, Ui.middle(mid, Ui.M), Ui.TEXT, Ui.M);
        PillButton.round(ui, "pick+", x0 + 28 + whoW, mid, 8, '>', true, () -> send("pick:1"));
        boolean has = row.getBoolean("has");
        String state = has ? "has it, mastery " + row.getInt("level") + (row.getBoolean("awakened") ? ", awakened" : "") : "doesn't have it";
        float stateX = x0 + 40 + whoW;
        float after = Ui.para(g, Component.literal(state), stateX, Ui.middle(mid, Ui.S), x1 - stateX, has ? Ui.PINK : Ui.SOFT, Ui.S, -1);
        y = Math.max(y + 20, after + 4);
        // buttons that wrap onto more rows when they don't fit
        String[] labels = {has ? "Take" : "Give", "Mastery -5", "Mastery +5", "Awaken", "Trigger", row.contains("button") ? row.getString("button") : null};
        Runnable[] actions = {() -> send((has ? "take:" : "give:") + path), () -> send("level:" + path + ":" + (hasShiftDown() ? -100 : -5)), () -> send("level:" + path + ":" + (hasShiftDown() ? 100 : 5)),
                () -> send("awaken:" + path), () -> send("trigger:" + path), () -> send("free:" + path)};
        float bx = x0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == null) continue;
            float bw = Ui.width(labels[i], Ui.S) + 14;
            if (bx + bw > x1 && bx > x0) {
                bx = x0;
                y += 18;
            }
            PillButton.draw(ui, path + "act" + i, bx, y, bw, 15, labels[i], i == 0 && !has ? PillButton.Kind.PRIMARY : PillButton.Kind.NORMAL, true, actions[i]);
            bx += bw + 3;
        }
        return y + 16;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return menu.wheel(mx, my, delta, ROW) || page.wheel(mx, my, delta, ROW) || super.mouseScrolled(mx, my, delta);
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent e) {
            OwnClient.on(OpMenu.CHANNEL, OpScreen::receive);
            e.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(com.example.charpowers.armour.SuitItems.POWER_SYRINGE.get(), new ResourceLocation(CharPowers.MODID, "filled"),
                    (stack, level, entity, seed) -> com.example.charpowers.own.PowerSyringe.filled(stack) ? 1f : 0f));
        }
    }
}
