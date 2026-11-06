package com.example.charpowers.game.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.client.OwnClient;
import com.example.charpowers.client.Soft;
import com.example.charpowers.game.GameBossMob;
import com.example.charpowers.game.GameEntities;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.sound.PowerSounds;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// The screen only you can see. A see-through hologram over the world, like Solo Leveling: the game keeps running behind it, and it's
// drawn in real screen pixels with thin lines and the thin Unicode font, so it stays clean at any GUI scale. There's a plain Minecraft look too.
public class GameScreen extends Screen {
    private static final ResourceLocation FRAME = new ResourceLocation(CharPowers.MODID, "textures/gui/status_window.png");
    private static final ResourceLocation NOTE = new ResourceLocation(CharPowers.MODID, "textures/gui/status_note.png");
    private static final int W = 560, H = 430, OPEN_TICKS = 5, NOTE_TICKS = 90;
    private static final String[] TABS = {"STATUS", "QUESTS", "SKILLS", "GATES", "SHOP"}, TITLES = {"STATUS", "QUEST INFO", "SKILLS", "GATES", "SHOP"};
    private static final String[] RANKS = {"E", "D", "C", "B", "A", "S"};
    private static final int SKILLS_PAGE = 6, SHOP_PAGE = 7;
    private static final KeyMapping KEY = new KeyMapping("key.charpowers.status_window", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.charpowers");

    private record Look(boolean plain, int text, int soft, int accent, int green, int row, int rowEdge, int edge, int fillOn, int textOn) {}

    private static final Look BLUE = new Look(false, 0xFFEAF8FF, 0xFF8FC6E4, 0xFF7FE3FF, 0xFF6BFF7A, 0x50081C36, 0x5035C6FF, 0xC07FE3FF, 0xF0DDF5FF, 0xFF061A30);
    private static final Look PLAIN = new Look(true, 0xFF303030, 0xFF5A5A5A, 0xFF1E5E8E, 0xFF1E6B1E, 0xFF8B8B8B, 0xFF555555, 0xFF3A3A3A, 0xFF4A4A4A, 0xFFFFFFFF);

    private record Hit(int x, int y, int w, int h, Runnable what) {}

    private static boolean doubleJump, plainLook;
    private static final Deque<String> NOTES = new ArrayDeque<>();
    private static int noteAge;

    private final List<Hit> hits = new ArrayList<>();
    private CompoundTag state;
    private int tab, age, skillPage, shopPage;
    private double unit;
    private int ox, oy, realX, realY;
    private GuiGraphics g;

    private GameScreen(CompoundTag state) {
        super(Component.literal("Status Window"));
        this.state = state;
    }

    private static void receive(CompoundTag tag) {
        if (tag.contains("note")) {
            NOTES.add(tag.getString("note"));
            return;
        }
        doubleJump = tag.getBoolean("doubleJump");
        plainLook = tag.getBoolean("normal");
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof GameScreen open) open.state = tag;
        else if (tag.getBoolean("open") && mc.screen == null) mc.setScreen(new GameScreen(tag));
    }

    static boolean plainLook() {
        return plainLook;
    }

    static KeyMapping statusKey() {
        return KEY;
    }

    static void send(String action) {
        Net.CHANNEL.sendToServer(new Net.OwnToServer(Mastery.GAME_PLAYER.toString(), action));
    }

    private Look look() {
        return state.getBoolean("normal") ? PLAIN : BLUE;
    }

    // ---- drawing, in real screen pixels: design units times `unit`

    private int X(double d) {
        return ox + (int) Math.round(d * unit);
    }

    private int Y(double d) {
        return oy + (int) Math.round(d * unit);
    }

    private void box(double x, double y, double w, double h, int colour) {
        g.fill(X(x), Y(y), X(x + w), Y(y + h), colour);
    }

    private void round(double x, double y, double w, double h, double radius, int colour) {
        Soft.fill(g, X(x), Y(y), X(x + w), Y(y + h), (int) Math.round(radius * unit), colour);
    }

    private void well(double x, double y, double w, double h, double radius) {
        Soft.well(g, X(x), Y(y), X(x + w), Y(y + h), (int) Math.round(radius * unit), look().row() | (look().plain() ? 0 : 0x60000000), Math.max(2, (int) Math.round(unit)));
    }

    private void line(double x, double y, double w, double h, double radius, int colour) {
        Soft.outline(g, X(x), Y(y), X(x + w), Y(y + h), (int) Math.round(radius * unit), colour);
    }

    private Component styled(String s) {
        return Component.literal(s);
    }

    // size 1 = body text. Scales keep to steps that land on whole pixels of the font, so letters stay sharp.
    private float scale(double size) {
        return com.example.charpowers.client.ui.Ui.snapRealScale((float) (size * unit));
    }

    private double width(String s, double size) {
        return font.width(styled(s)) * scale(size) / unit;
    }

    private void text(String s, double x, double y, double size, int colour, int align, boolean glow) {
        Component c = styled(s);
        float k = scale(size);
        float px = X(x) - (align == 0 ? font.width(c) * k / 2 : align > 0 ? font.width(c) * k : 0);
        g.pose().pushPose();
        g.pose().translate(px, Y(y), 0);
        g.pose().scale(k, k, 1);
        if (glow && !look().plain()) {
            float step = 1.5f / k;
            int soft = 0x40000000 | (look().accent() & 0xFFFFFF);
            for (float[] at : new float[][] {{-step, 0}, {step, 0}, {0, -step}, {0, step}}) {
                g.pose().pushPose();
                g.pose().translate(at[0], at[1], 0);
                g.drawString(font, c, 0, 0, soft, false);
                g.pose().popPose();
            }
        }
        g.drawString(font, c, 0, 0, colour, false);
        g.pose().popPose();
    }

    private void button(double x, double y, double w, double h, String label, boolean active, boolean selected, Runnable what) {
        Look l = look();
        int x0 = X(x), y0 = Y(y), x1 = X(x + w), y1 = Y(y + h);
        boolean over = active && realX >= x0 && realX < x1 && realY >= y0 && realY < y1;
        int r = (y1 - y0) / 2, lift = Math.max(2, (int) Math.round(unit));
        int face = selected ? l.fillOn() : l.plain() ? 0xFF6F6F6F : !active ? 0xFF0F2438 : over ? 0xFF1E6A9A : 0xFF134668;
        Soft.plate(g, x0, y0, x1, y1, r, face, lift);
        int colour = selected ? l.textOn() : !active ? l.soft() : l.plain() ? 0xFFFFFFFF : l.text();
        text(label, x + w / 2, y + (h - 9 * scale(1) / unit) / 2, 1, colour, 0, false);
        if (active) hits.add(new Hit(x0, y0, x1 - x0, y1 - y0, what));
    }

    @Override
    public void tick() {
        age++;
        if (tab == 3 && age % 20 == 0) send("refresh");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        g = graphics;
        hits.clear();
        Window window = Minecraft.getInstance().getWindow();
        double gui = window.getGuiScale();
        int realW = window.getWidth(), realH = window.getHeight();
        unit = Math.min(2.4, Math.min(realW / (W + 8.0), realH / (H + 8.0)));
        ox = (int) ((realW - W * unit) / 2);
        oy = (int) ((realH - H * unit) / 2);
        realX = (int) (mouseX * gui);
        realY = (int) (mouseY * gui);
        Look l = look();
        g.pose().pushPose();
        g.pose().scale((float) (1 / gui), (float) (1 / gui), 1);
        // it opens out from a line across the middle, the way the show's windows do
        float open = Math.min(1f, (age + partial) / OPEN_TICKS);
        open = 1 - (1 - open) * (1 - open);
        g.pose().translate(0, realH / 2f, 0);
        g.pose().scale(1, Math.max(0.02f, open), 1);
        g.pose().translate(0, -realH / 2f, 0);
        if (l.plain()) {
            round(34, 22, W - 68, H - 44, 10, 0xFF555555);
            round(35, 23, W - 70, H - 46, 10, 0xFFC6C6C6);
        } else {
            RenderSystem.enableBlend();
            Minecraft.getInstance().getTextureManager().getTexture(FRAME).setFilter(true, false);
            g.blit(FRAME, ox, oy, (int) (W * unit), (int) (H * unit), 0f, 0f, W, H, W, H);
        }
        g.drawManaged(this::paint);
        g.pose().popPose();
    }

    private void paint() {
        Look l = look();
        double cx = W / 2.0;
        String title = TITLES[tab];
        double titleWidth = width(title, 1.7);
        line(cx - titleWidth / 2 - 22, 25, 18, 18, 9, l.edge());
        text("!", cx - titleWidth / 2 - 13, 28, 1, l.accent(), 0, true);
        text(title, cx + 4, 24, 1.5, l.plain() ? l.text() : 0xFFFFFFFF, 0, true);
        int tabs = state.getBoolean("awakened") ? 5 : 4;
        double tabW = (W - 120 - tabs * 6) / (double) (tabs + 1);
        for (int i = 0; i < tabs; i++) {
            int which = i;
            button(60 + i * (tabW + 6), 60, tabW, 20, TABS[i], true, tab == i, () -> tab = which);
        }
        button(60 + tabs * (tabW + 6), 60, tabW, 20, "ITEMS", true, false, this::openItems);
        switch (tab) {
            case 0 -> status();
            case 1 -> quests();
            case 2 -> skills();
            case 3 -> gates();
            default -> shop();
        }
        // along the bottom: mastery, the look switch, and what you have to spend
        text("LV. " + state.getInt("level"), 60, 381, 1, l.soft(), -1, false);
        button(112, 377, 74, 17, l.plain() ? "HOLOGRAM" : "PLAIN", true, false, () -> send("look"));
        button(190, 377, 60, 17, "CLOSE", true, false, this::onClose);
        String money = tab == 4 ? state.getInt("coins") + " COINS" : state.getInt("points") + " POINTS";
        well(W - 60 - 130, 376, 130, 19, 9.5);
        text(money, W - 68, 381, 1, l.plain() ? l.text() : 0xFFFFFFFF, 1, true);
    }

    private void status() {
        Look l = look();
        ListTag stats = state.getList("stats", Tag.TAG_COMPOUND);
        double y = 90;
        for (int i = 0; i < stats.size(); i++) {
            CompoundTag s = stats.getCompound(i);
            int stat = i, cost = s.getInt("cost");
            well(60, y, W - 120, 48, 10);
            text(s.getString("name").toUpperCase(), 72, y + 8, 1.2, l.text(), -1, false);
            text(s.getString("words"), 72, y + 28, 1, l.soft(), -1, false);
            text(String.valueOf(s.getInt("points")), 330, y + 15, 1.4, l.accent(), 1, true);
            button(W - 60 - 104, y + 13, 94, 22, "+1  (" + cost + ")", state.getInt("points") >= cost, false, () -> send("stat:" + stat));
            y += 56;
        }
    }

    private void quests() {
        Look l = look();
        ListTag quests = state.getList("quests", Tag.TAG_COMPOUND);
        int dailies = 0;
        for (int i = 0; i < quests.size(); i++) {
            if (!quests.getCompound(i).getString("text").startsWith("Chapter") && !quests.getCompound(i).getString("text").startsWith("Final") && !quests.getCompound(i).getString("text").startsWith("Side")
                    && !quests.getCompound(i).getString("text").startsWith("Special") && !quests.getCompound(i).getString("text").startsWith("The ")) dailies++;
        }
        double y = 88;
        text("DAILY QUEST", W / 2.0, y, 1, 0xFFFFFFFF, 0, false);
        y += 22;
        text("GOALS", W / 2.0, y, 1, l.green(), 0, true);
        y += 24;
        for (int i = 0; i < quests.size(); i++) {
            if (i == dailies) {
                y += 6;
                text("THE STORY", W / 2.0, y, 1, 0xFFFFFFFF, 0, false);
                y += 24;
            }
            CompoundTag q = quests.getCompound(i);
            int have = q.getInt("have"), need = Math.max(1, q.getInt("need"));
            boolean done = have >= need;
            text("-" + q.getString("text"), 76, y, 1, done ? l.green() : l.text(), -1, false);
            String reward = q.getString("reward");
            if (!reward.isEmpty()) text(reward, W - 170, y, 1, l.soft(), 1, false);
            text("[" + have + "/" + need + "]", W - 76, y, 1, done ? l.green() : l.accent(), 1, false);
            y += 24;
        }
    }

    private void skills() {
        Look l = look();
        text("SKILL SLOTS  " + state.getInt("slotsUsed") + " / " + state.getInt("slots"), W / 2.0, 86, 1, l.soft(), 0, false);
        ListTag skills = state.getList("skills", Tag.TAG_COMPOUND);
        double y = 106;
        int pages = Math.max(1, (skills.size() + SKILLS_PAGE - 1) / SKILLS_PAGE);
        skillPage = Math.min(skillPage, pages - 1);
        for (int i = skillPage * SKILLS_PAGE; i < skills.size() && i < (skillPage + 1) * SKILLS_PAGE; i++) {
            CompoundTag s = skills.getCompound(i);
            int skill = i;
            boolean owned = s.getBoolean("owned"), on = s.getBoolean("on");
            well(60, y, W - 120, 40, 10);
            if (on) line(60, y, W - 120, 40, 10, l.edge());
            text(s.getString("name"), 72, y + 5, 1, on ? l.accent() : l.text(), -1, on);
            text(s.getString("about"), 72, y + 22, 1, l.soft(), -1, false);
            String label = !owned ? "BUY  " + s.getInt("cost") : on ? "ON" : "OFF";
            button(W - 60 - 84, y + 10, 74, 20, label, owned || state.getInt("points") >= s.getInt("cost"), on, () -> send("skill:" + skill));
            y += 44;
        }
        pager(pages, skillPage, next -> skillPage = next);
    }

    private void pager(int pages, int page, java.util.function.IntConsumer go) {
        if (pages <= 1) return;
        button(256, 377, 22, 17, "<", page > 0, false, () -> go.accept(page - 1));
        text((page + 1) + "/" + pages, 296, 381, 1, look().soft(), 0, false);
        button(314, 377, 22, 17, ">", page < pages - 1, false, () -> go.accept(page + 1));
    }

    private void gates() {
        Look l = look();
        text("OPEN A GATE", W / 2.0, 88, 1, 0xFFFFFFFF, 0, false);
        int top = state.getInt("topRank"), wait = state.getInt("openWait");
        double pill = 60, gap = 8, start = (W - (RANKS.length * pill + (RANKS.length - 1) * gap)) / 2;
        for (int i = 0; i < RANKS.length; i++) {
            int rank = i;
            button(start + i * (pill + gap), 106, pill, 26, RANKS[i], i <= top && wait == 0, false, () -> send("gate:" + rank));
        }
        text(wait > 0 ? "You can open another in " + wait + " s." : "Higher ranks come with mastery. It appears right in front of you.", W / 2.0, 140, 1, l.soft(), 0, false);
        text("GATES NEARBY", W / 2.0, 166, 1, 0xFFFFFFFF, 0, false);
        ListTag gates = state.getList("gates", Tag.TAG_COMPOUND);
        if (gates.isEmpty()) text("None yet. They open near you as you explore.", W / 2.0, 190, 1, l.soft(), 0, false);
        double y = 186;
        for (int i = 0; i < gates.size(); i++) {
            CompoundTag gate = gates.getCompound(i);
            int which = i;
            well(60, y, W - 120, 44, 10);
            text((gate.getBoolean("sealed") ? "SEALED " : "") + gate.getString("label"), 72, y + 10, 1, gate.getBoolean("sealed") ? 0xFFFFC857 : l.accent(), -1, true);
            text(gate.getInt("far") + " blocks " + gate.getString("way"), 72, y + 23, 1, l.text(), -1, false);
            if (!gate.getString("warn").isEmpty()) text(gate.getString("warn"), 72, y + 35, 1, l.soft(), -1, false);
            button(W - 60 - 104, y + 12, 94, 20, gate.getBoolean("tracked") ? "TRACKING" : "TRACK", true, gate.getBoolean("tracked"), () -> send("track:" + which));
            y += 48;
        }
        invites(l);
    }

    // Teammates who are online: click to ask them along, click again to take it back. They go in with you when you walk into a gate.
    private void invites(Look l) {
        ListTag mates = state.getList("mates", Tag.TAG_COMPOUND);
        text("RAID WITH FRIENDS", W / 2.0, 330, 1, 0xFFFFFFFF, 0, false);
        if (mates.isEmpty()) {
            text("Teammates who are online show up here.", W / 2.0, 350, 1, l.soft(), 0, false);
            return;
        }
        double w = 128, gap = 8, total = Math.min(4, mates.size()) * (w + gap) - gap, x = (W - total) / 2;
        for (int i = 0; i < mates.size() && i < 4; i++) {
            CompoundTag m = mates.getCompound(i);
            String st = m.getString("state"), id = m.getString("id");
            String label = m.getString("name") + (st.equals("joined") ? " - IN" : st.equals("invited") ? " - ASKED" : st.equals("busy") ? " - BUSY" : "");
            button(x + i * (w + gap), 344, w, 22, label, !st.equals("busy"), st.equals("joined") || st.equals("invited"), () -> send((st.isEmpty() ? "invite:" : "uninvite:") + id));
        }
    }

    private void shop() {
        Look l = look();
        ListTag shop = state.getList("shop", Tag.TAG_COMPOUND);
        double y = 90;
        int pages = Math.max(1, (shop.size() + SHOP_PAGE - 1) / SHOP_PAGE);
        shopPage = Math.min(shopPage, pages - 1);
        pager(pages, shopPage, next -> shopPage = next);
        for (int i = shopPage * SHOP_PAGE; i < shop.size() && i < (shopPage + 1) * SHOP_PAGE; i++) {
            CompoundTag s = shop.getCompound(i);
            int item = i, price = s.getInt("price");
            Item found = ForgeRegistries.ITEMS.getValue(new ResourceLocation(s.getString("item")));
            ItemStack stack = new ItemStack(found == null ? Items.BARRIER : found, s.getInt("count"));
            well(60, y, W - 120, 36, 10);
            well(66, y + 4, 28, 28, 7);
            line(66, y + 4, 28, 28, 7, l.rowEdge());
            float k = (float) (unit * 24 / 16);
            g.pose().pushPose();
            g.pose().translate(X(68), Y(y + 6), 0);
            g.pose().scale(k, k, 1);
            g.renderItem(stack, 0, 0);
            g.renderItemDecorations(font, stack, 0, 0);
            g.pose().popPose();
            text(stack.getHoverName().getString(), 104, y + 11, 1, l.text(), -1, false);
            button(W - 60 - 104, y + 7, 94, 22, price + " COINS", state.getInt("coins") >= price, false, () -> send("shop:" + item));
            y += 40;
        }
    }

    private void openItems() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.setScreen(new HoloScreen(mc.player.inventoryMenu, mc.player.getInventory(), Component.literal("Inventory")));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int mouseButton) {
        if (mouseButton == 0) {
            double gui = Minecraft.getInstance().getWindow().getGuiScale();
            int rx = (int) (mx * gui), ry = (int) (my * gui);
            for (Hit h : new ArrayList<>(hits)) {
                if (rx >= h.x() && rx < h.x() + h.w() && ry >= h.y() && ry < h.y() + h.h()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.8f, 0.25f));
                    h.what().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, mouseButton);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (KEY.matches(key, scan)) {
            onClose();
            return true;
        }
        if (Minecraft.getInstance().options.keyInventory.matches(key, scan)) {
            openItems();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- notifications: a small hologram at the top of the screen instead of a line in chat

    // How many real pixels are free to the left of a window (they are centred, so the right has the same).
    private static int noteRoom(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> open, Window window) {
        return (int) (open.getGuiLeft() * window.getGuiScale());
    }

    // Too little room to read it beside the open window: it waits until that is shut. Other menus get left alone too.
    private static boolean noteWaits(Minecraft mc) {
        if (mc.screen == null) return false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> open) return noteRoom(open, mc.getWindow()) < 260;
        return true;
    }

    private static void drawNote(GuiGraphics g, Minecraft mc, float partial) {
        String note = NOTES.peek();
        if (note == null || mc.options.hideGui) return;
        Window window = mc.getWindow();
        double gui = window.getGuiScale();
        float age = noteAge + partial;
        float end = NOTES.size() > 1 ? NOTE_TICKS / 2f : NOTE_TICKS;
        float fade = Math.min(1f, Math.min(age / 6f, (end - age) / 10f));
        if (fade <= 0) return;
        double unit = Math.min(1.8, window.getWidth() / 900.0);
        int w = (int) (400 * unit), h = (int) (140 * unit), x = (window.getWidth() - w) / 2, y = (int) (10 * unit);
        // with a window open, it goes in the free space beside that window, a bit smaller if it has to
        if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> open) {
            int room = noteRoom(open, window);
            unit = Math.min(unit, (room - 16) / 400.0);
            w = (int) (400 * unit);
            h = (int) (140 * unit);
            x = (int) (open.getGuiLeft() * gui - w) / 2;
            y = (int) (10 * unit);
        }
        g.flush();
        g.pose().pushPose();
        g.pose().scale((float) (1 / gui), (float) (1 / gui), 1);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, fade);
        mc.getTextureManager().getTexture(NOTE).setFilter(true, false);
        g.blit(NOTE, x, y, w, h, 0f, 0f, 400, 140, 400, 140);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int alpha = Math.max(4, (int) (fade * 255)) << 24;
        float k = com.example.charpowers.client.ui.Ui.snapRealScale((float) (unit * 1.7f));
        Component head = Component.literal("NOTIFICATION");
        float titleScale = com.example.charpowers.client.ui.Ui.snapRealScale(k * 1.5f);
        float titleWidth = mc.font.width(head) * titleScale;
        float mid = x + w / 2f;
        int ring = (int) (24 * unit), ringX = (int) (mid - titleWidth / 2 - ring / 2f - 7 * unit), ringY = (int) (y + 20 * unit);
        Soft.outline(g, ringX, ringY, ringX + ring, ringY + ring, ring / 2, alpha | 0x7FE3FF);
        Component bang = Component.literal("!");
        g.pose().pushPose();
        g.pose().translate(ringX + ring / 2f - mc.font.width(bang) * k / 2, ringY + ring / 2f - 4 * k, 0);
        g.pose().scale(k, k, 1);
        g.drawString(mc.font, bang, 0, 0, alpha | 0xBFF0FF, false);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(mid - titleWidth / 2 + ring / 2f + 7 * unit, y + 24 * unit, 0);
        g.pose().scale(titleScale, titleScale, 1);
        g.drawString(mc.font, head, 0, 0, alpha | 0xBFF0FF, false);
        g.pose().popPose();
        List<FormattedCharSequence> lines = mc.font.split(Component.literal(note), (int) ((w - 90 * unit) / k));
        float lineY = (float) (y + 54 * unit);
        for (FormattedCharSequence line : lines) {
            g.pose().pushPose();
            g.pose().translate(mid - mc.font.width(line) * k / 2, lineY, 0);
            g.pose().scale(k, k, 1);
            g.drawString(mc.font, line, 0, 0, alpha | 0xFFFFFF, false);
            g.pose().popPose();
            lineY += 10 * k;
        }
        g.pose().popPose();
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
    public static final class Events {
        private static boolean wasDown, used;

        private Events() {}

        @SubscribeEvent
        public static void onHud(RenderGuiEvent.Post e) {
            if (!NOTES.isEmpty() && !noteWaits(Minecraft.getInstance())) drawNote(e.getGuiGraphics(), Minecraft.getInstance(), e.getPartialTick());
        }

        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent e) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = mc.player;
            if (e.phase != TickEvent.Phase.END || p == null) return;
            while (KEY.consumeClick()) {
                if (mc.screen == null) send("open");
            }
            if (!NOTES.isEmpty() && !noteWaits(mc)) { // they wait for you to shut the window, not sit on top of it
                if (noteAge == 0) mc.getSoundManager().play(SimpleSoundInstance.forUI(PowerSounds.HOLO_PING.get(), 1f, 0.5f));
                // when there are more waiting, move along a bit sooner
                if (++noteAge >= (NOTES.size() > 1 ? NOTE_TICKS / 2 : NOTE_TICKS)) {
                    NOTES.poll();
                    noteAge = 0;
                }
            }
            if (!doubleJump) return;
            // Double Jump is felt on your own PC: a jump press in mid-air, once, until you land
            boolean down = mc.options.keyJump.isDown();
            if (p.onGround()) used = false;
            else if (down && !wasDown && !used && !p.getAbilities().flying && !p.isInWater() && !p.onClimbable() && mc.screen == null) {
                used = true;
                p.setDeltaMovement(p.getDeltaMovement().x, 0.6, p.getDeltaMovement().z);
                send("double_jump");
            }
            wasDown = down;
        }
    }

    public static final class BossRenderer extends HumanoidMobRenderer<GameBossMob, PlayerModel<GameBossMob>> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(CharPowers.MODID, "textures/entity/game_boss.png");

        public BossRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(GameBossMob e) {
            return TEXTURE;
        }
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent e) {
            OwnClient.on(Mastery.GAME_PLAYER.toString(), GameScreen::receive);
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(KEY);
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(GameEntities.BOSS.get(), BossRenderer::new);
        }
    }
}
