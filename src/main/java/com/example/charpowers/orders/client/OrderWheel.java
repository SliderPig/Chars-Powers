package com.example.charpowers.orders.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.Soft;
import com.example.charpowers.orders.OrderNet;
import com.example.charpowers.orders.model.OrderRules;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

// Hold the key to open a round wheel of orders for everything you have out (clones, pets, minions), move the mouse towards
// one, let go to give it. It is not a screen: you keep walking, your hotbar works, and only the mouse is borrowed to pick.
// Drawn in real screen pixels out of circles, so nothing on it is a box.
public final class OrderWheel {
    private static final KeyMapping KEY = new KeyMapping("key.charpowers.order_wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.charpowers");
    private static final long OPEN_MS = 170, CLOSE_MS = 130;
    private static final int PINK = 0xFF8FCB, LILAC = 0xB99CFF, VIOLET = 0x8E6BFF, PLUM = 0x241022;

    private static int clones, pets, minions;
    private static List<OrderRules.Order> orders = List.of();
    private static boolean open, closing, forTest;
    private static long openedAt, closedAt, lastFrame;
    private static double curX, curY;
    private static int picked = -1;
    private static float[] hover = new float[0];

    private OrderWheel() {}

    // From the server: what you have out right now.
    public static void have(int c, int p, int m) {
        clones = c;
        pets = p;
        minions = m;
        orders = OrderRules.available(c, p, m);
        if (open && orders.isEmpty()) forget();
    }

    public static boolean holdingMouse() {
        return open && !closing;
    }

    // Mouse movement while the wheel is open, in real pixels.
    public static void pull(double dx, double dy) {
        double sc = scale();
        double[] c = OrderRules.clamp(curX + dx / sc * 0.9, curY + dy / sc * 0.9);
        curX = c[0];
        curY = c[1];
    }

    private static double scale() {
        Minecraft mc = Minecraft.getInstance();
        return Math.max(0.8, Math.min(1.7, mc.getWindow().getHeight() / 540.0));
    }

    private static void forget() {
        open = false;
        closing = false;
        picked = -1;
    }

    private static void openNow(long now) {
        open = true;
        closing = false;
        openedAt = now;
        curX = 0;
        curY = 0;
        picked = -1;
        hover = new float[orders.size()];
    }

    private static void closeNow(long now, boolean give) {
        if (!open || closing) return;
        int sel = OrderRules.slice(curX, curY, orders.size());
        if (give && sel >= 0 && sel < orders.size()) {
            OrderRules.Order o = orders.get(sel);
            if (!forTest) OrderNet.CHANNEL.sendToServer(new OrderNet.Give(o.group(), o.action()));
            picked = sel;
        } else {
            picked = -1;
        }
        closing = true;
        closedAt = now;
    }

    // For the real-game check: show the wheel as if it had been open a while, with the mouse pulled this far.
    public static void showForTest(int c, int p, int m, double dx, double dy) {
        have(c, p, m);
        forTest = true;
        openNow(System.currentTimeMillis() - 1000);
        curX = dx;
        curY = dy;
        int sel = OrderRules.slice(dx, dy, orders.size());
        for (int i = 0; i < hover.length; i++) hover[i] = i == sel ? 1f : 0f;
    }

    public static void hideForTest() {
        forTest = false;
        forget();
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(KEY);
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("order_wheel", (gui, g, partial, w, h) -> draw(g));
        }
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            long now = System.currentTimeMillis();
            if (forTest) return;
            if (mc.player == null || mc.level == null) {
                forget();
                return;
            }
            boolean held = mc.screen == null && KEY.isDown();
            if (open && closing && now - closedAt > CLOSE_MS) forget();
            if (!open && held && !orders.isEmpty()) {
                openNow(now);
            } else if (open && !closing && (!held || orders.isEmpty())) {
                closeNow(now, held == false && mc.screen == null);
            }
        }
    }

    private static int tint(String group) {
        return switch (group) {
            case OrderRules.CLONES -> PINK;
            case OrderRules.PETS -> LILAC;
            default -> VIOLET;
        };
    }

    private static Item icon(OrderRules.Order o) {
        return switch (o.group() + ":" + o.action()) {
            case "clones:follow", "pets:follow" -> Items.LEAD;
            case "clones:mine" -> Items.IRON_PICKAXE;
            case "clones:chop" -> Items.IRON_AXE;
            case "clones:cook" -> Items.COOKED_BEEF;
            case "clones:guard", "pets:guard" -> Items.SHIELD;
            case "clones:farm" -> Items.WHEAT;
            case "clones:home" -> Items.RED_BED;
            case "pets:stay", "minions:stay" -> Items.OAK_SIGN;
            case "pets:attack", "minions:attack" -> Items.IRON_SWORD;
            case "minions:follow" -> Items.ROTTEN_FLESH;
            default -> Items.BARRIER;
        };
    }

    private static int argb(int rgb, float alpha) {
        return ((int) (Math.max(0f, Math.min(1f, alpha)) * 255) << 24) | rgb;
    }

    static void draw(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!open || orders.isEmpty() || mc.player == null || (mc.screen != null && !forTest)) return;
        long now = System.currentTimeMillis();
        float t = closing ? 1f - (now - closedAt) / (float) CLOSE_MS : (now - openedAt) / (float) OPEN_MS;
        float ease = OrderRules.ease(t);
        if (ease <= 0f) return;
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        int n = orders.size();
        double gui = mc.getWindow().getGuiScale();
        int realW = mc.getWindow().getWidth(), realH = mc.getWindow().getHeight();
        double sc = scale();
        int sel = closing ? picked : OrderRules.slice(curX, curY, n);
        if (hover.length != n) hover = new float[n];
        for (int i = 0; i < n; i++) {
            float goal = i == sel ? 1f : 0f;
            hover[i] += (goal - hover[i]) * Math.min(1f, dt * 16f);
        }
        int cx = realW / 2, cy = realH / 2;
        double ring = 94 * sc * (0.55 + 0.45 * ease);
        double bigR = 25 * sc;
        double base = OrderRules.bubbleRadius(n, 94 * sc, bigR);
        int text = Math.max(1, (int) Math.round(realH / 560.0));

        g.pose().pushPose();
        g.pose().scale((float) (1 / gui), (float) (1 / gui), 1f);

        // a soft dark pool behind the wheel so it reads on any ground
        int pool = (int) ((94 * sc + bigR + 22 * sc) * (0.7 + 0.3 * ease));
        Soft.fill(g, cx - pool, cy - pool, cx + pool, cy + pool, pool, argb(0x0A0610, 0.42f * ease));
        Soft.fill(g, cx - pool + (int) (10 * sc), cy - pool + (int) (10 * sc), cx + pool - (int) (10 * sc), cy + pool - (int) (10 * sc), pool, argb(0x0A0610, 0.22f * ease));

        for (int i = 0; i < n; i++) {
            OrderRules.Order o = orders.get(i);
            double[] pos = OrderRules.position(i, n, ring);
            double r = base * (1 + 0.28 * hover[i]);
            int bx = cx + (int) Math.round(pos[0]), by = cy + (int) Math.round(pos[1]);
            int ri = (int) Math.round(r);
            int tone = tint(o.group());
            float h = hover[i];
            Soft.fill(g, bx - ri + 2, by - ri + 3, bx + ri + 2, by + ri + 3, ri, argb(0x000000, 0.35f * ease));
            Soft.fill(g, bx - ri, by - ri, bx + ri, by + ri, ri, argb(PLUM, 0.92f * ease));
            if (h > 0.02f) Soft.fill(g, bx - ri, by - ri, bx + ri, by + ri, ri, argb(tone, 0.45f * h * ease));
            int rim = Math.max(1, (int) Math.round(2 * sc * (1 + h)));
            for (int k = 0; k < rim; k++) Soft.outline(g, bx - ri + k, by - ri + k, bx + ri - k, by + ri - k, Math.max(1, ri - k), argb(tone, (0.55f + 0.45f * h) * ease));
            // a light edge on the top, like the other rounded things in the mod
            Soft.fill(g, bx - ri / 2, by - ri + rim + 1, bx + ri / 2, by - ri + rim + 2 + Math.max(1, ri / 10), 1, argb(0xFFFFFF, 0.18f * ease));
            float k = (float) (r * 1.25 / 16.0);
            g.pose().pushPose();
            g.pose().translate(bx - 8 * k, by - 8 * k, 0);
            g.pose().scale(k, k, 1f);
            if (ease > 0.35f) g.renderItem(new ItemStack(icon(o)), 0, 0);
            g.pose().popPose();
        }

        // middle: what you are pointing at
        int mid = (int) (36 * sc * (0.6 + 0.4 * ease));
        Soft.fill(g, cx - mid + 2, cy - mid + 3, cx + mid + 2, cy + mid + 3, mid, argb(0x000000, 0.3f * ease));
        Soft.fill(g, cx - mid, cy - mid, cx + mid, cy + mid, mid, argb(PLUM, 0.94f * ease));
        Soft.outline(g, cx - mid, cy - mid, cx + mid, cy + mid, mid, argb(sel >= 0 ? tint(orders.get(sel).group()) : 0x7A5A78, 0.9f * ease));
        if (ease > 0.6f) {
            String top = sel >= 0 ? groupName(orders.get(sel).group()) : "Orders";
            String main = sel >= 0 ? orders.get(sel).label() : "let go to cancel";
            centered(g, top, cx, cy - 9 * text - 2, text, sel >= 0 ? argb(tint(orders.get(sel).group()), 1f) : 0xFFC9A6C0);
            centered(g, main, cx, cy + 2, text, 0xFFFFE6F3);
        }

        // where the mouse has gone: a small pink bead and a faint trail to it
        int px = cx + (int) Math.round(curX * sc), py = cy + (int) Math.round(curY * sc);
        int steps = 6;
        for (int s = 1; s <= steps; s++) {
            int dx = (int) Math.round((px - cx) * s / (double) (steps + 1)), dy = (int) Math.round((py - cy) * s / (double) (steps + 1));
            int rr = Math.max(1, (int) Math.round(1.5 * sc));
            Soft.fill(g, cx + dx - rr, cy + dy - rr, cx + dx + rr, cy + dy + rr, rr, argb(PINK, 0.12f * ease));
        }
        int bead = Math.max(3, (int) Math.round(5 * sc));
        Soft.fill(g, px - bead - 2, py - bead - 2, px + bead + 2, py + bead + 2, bead + 2, argb(PINK, 0.25f * ease));
        Soft.fill(g, px - bead, py - bead, px + bead, py + bead, bead, argb(0xFFD0EA, ease));
        g.pose().popPose();
    }

    private static String groupName(String group) {
        return switch (group) {
            case OrderRules.CLONES -> "Clones";
            case OrderRules.PETS -> "Pets";
            default -> "Minions";
        };
    }

    private static void centered(GuiGraphics g, String s, int x, int y, int k, int colour) {
        var font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(x - font.width(s) * k / 2f, y, 0);
        g.pose().scale(k, k, 1f);
        g.drawString(font, s, 0, 0, colour, false);
        g.pose().popPose();
    }
}
