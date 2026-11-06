package com.example.charpowers.mastery.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.Soft;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.mastery.model.ToastRules;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.OwnPowers;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// A small rounded toast in the top right when a power's mastery goes up: which power, and what comes next and how far off it is.
// At most one every ten seconds (level-ups in between are gathered into it), and it can be switched off with /chartoast.
public final class MasteryToast {
    private static final int PLUM = 0x241022, PINK = 0xFF8FCB, CREAM = 0xFFE6F3, MUTED = 0xFFC9A6C0;
    private static boolean enabled = load();
    private static final ToastRules.Batch PENDING = new ToastRules.Batch();
    private static Map<String, List<ToastRules.Unlock>> unlocks;
    private static long lastShown = -ToastRules.GAP_MS, shownAt = -1;
    private static List<String> lines = List.of();
    private static Item icon = Items.EXPERIENCE_BOTTLE;
    private static float bar;

    private MasteryToast() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("charpowers").resolve("toast.properties");
    }

    private static boolean load() {
        try (InputStream in = Files.newInputStream(file())) {
            Properties p = new Properties();
            p.load(in);
            return Boolean.parseBoolean(p.getProperty("masteryToast", "true").trim());
        } catch (IOException | RuntimeException e) {
            return true;
        }
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            Properties p = new Properties();
            p.setProperty("masteryToast", Boolean.toString(enabled));
            try (OutputStream out = Files.newOutputStream(file())) {
                p.store(out, "Char's Powers: the mastery toast");
            }
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't save the toast setting", e);
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    // power = "charpowers:cloning"
    public static void levelUp(String power, int level) {
        if (enabled) PENDING.add(power, level);
    }

    private static Map<String, List<ToastRules.Unlock>> unlocks() {
        if (unlocks != null) return unlocks;
        Map<String, List<ToastRules.Unlock>> out = new HashMap<>();
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(CharPowers.MODID, "mastery_unlocks.json")).orElseThrow().open();
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                List<ToastRules.Unlock> list = new ArrayList<>();
                for (JsonElement u : (JsonArray) e.getValue()) {
                    JsonObject o = u.getAsJsonObject();
                    list.add(new ToastRules.Unlock(o.get("level").getAsInt(), o.get("name").getAsString()));
                }
                out.put(CharPowers.MODID + ":" + e.getKey(), list);
            }
        } catch (IOException | RuntimeException e) {
            CharPowers.LOG.warn("Couldn't read the mastery unlock list", e);
        }
        unlocks = out;
        return out;
    }

    private static String nameOf(String power) {
        String path = power.substring(power.indexOf(':') + 1);
        String key = "power.charpowers." + path;
        return I18n.exists(key) ? I18n.get(key) : path;
    }

    private static Item iconOf(String power) {
        for (OwnPower p : OwnPowers.all()) if (p.id.toString().equals(power)) return p.icon;
        return Items.EXPERIENCE_BOTTLE;
    }

    private static void make(Map<String, Integer> levels, long now) {
        Map<String, Integer> named = new java.util.LinkedHashMap<>();
        String first = null;
        int firstLevel = 0;
        for (Map.Entry<String, Integer> e : levels.entrySet()) {
            if (first == null) {
                first = e.getKey();
                firstLevel = e.getValue();
            }
            named.put(nameOf(e.getKey()), e.getValue());
        }
        ToastRules.Next next = named.size() == 1 ? ToastRules.next(unlocks().getOrDefault(first, List.of()), firstLevel) : null;
        List<String> text = ToastRules.lines(named, next);
        Item it = named.size() == 1 ? iconOf(first) : Items.EXPERIENCE_BOTTLE;
        float fill = named.size() == 1 ? ToastRules.progress(next, firstLevel) : -1f;
        lastShown = now;
        Minecraft.getInstance().getToasts().addToast(new Card(text, it, fill));
    }

    // for the real-game check
    public static void showForTest(String power, int level) {
        make(Map.of(power, level), System.currentTimeMillis());
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void client(FMLClientSetupEvent e) {
            Mastery.clientLevelUp = MasteryToast::levelUp;
        }
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            long now = System.currentTimeMillis();
            if (ToastRules.mayShow(now, lastShown, enabled, !PENDING.isEmpty()) && Minecraft.getInstance().player != null) make(PENDING.take(), now);
        }

        @SubscribeEvent
        public static void leaving(ClientPlayerNetworkEvent.LoggingOut e) {
            PENDING.take();
            Mastery.setClientLevels(Map.of());
        }

        @SubscribeEvent
        public static void commands(RegisterClientCommandsEvent e) {
            e.getDispatcher().register(Commands.literal("chartoast")
                    .executes(c -> set(!enabled))
                    .then(Commands.argument("state", StringArgumentType.word()).executes(c -> set("on".equalsIgnoreCase(StringArgumentType.getString(c, "state"))))));
        }
    }

    private static int set(boolean on) {
        enabled = on;
        if (!on) {
            PENDING.take();
        }
        save();
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal(on ? "Mastery toasts are on." : "Mastery toasts are off."));
        return 1;
    }

    private static int argb(int rgb, float alpha) {
        return ((int) (Math.max(0f, Math.min(1f, alpha)) * 255) << 24) | rgb;
    }

    // The toast itself: vanilla queues and stacks it with the advancement and recipe toasts and slides it in and out. The look
    // (rounded plum card, round icon plate, normal font, a thin progress bar) is drawn in real screen pixels inside its space.
    private static final class Card implements Toast {
        private final List<String> lines;
        private final Item icon;
        private final float bar;
        private final double gui;
        private final int text, pad, plate, lineH, wReal, hReal, gap;
        private long first = -1;

        Card(List<String> lines, Item icon, float bar) {
            Minecraft mc = Minecraft.getInstance();
            this.lines = lines;
            this.icon = icon;
            this.bar = bar;
            gui = mc.getWindow().getGuiScale();
            int realH = mc.getWindow().getHeight();
            double sc = Math.max(0.8, Math.min(1.7, realH / 540.0));
            text = Math.max(1, (int) Math.round(realH / 560.0));
            pad = (int) Math.round(12 * sc);
            plate = (int) Math.round(34 * sc);
            lineH = 9 * text + 3;
            gap = (int) Math.round(4 * gui);
            int w = 0;
            for (String l : lines) w = Math.max(w, mc.font.width(l) * text);
            wReal = Math.max((int) Math.round(170 * sc), w + plate + pad * 3);
            hReal = Math.max(plate + pad, lines.size() * lineH + pad * 2 + (bar >= 0 ? (int) Math.round(8 * sc) : 0));
        }

        @Override
        public int width() {
            return (int) Math.ceil((wReal + gap * 2) / gui);
        }

        @Override
        public int height() {
            return (int) Math.ceil((hReal + gap * 2) / gui);
        }

        @Override
        public Visibility render(GuiGraphics g, ToastComponent toasts, long sinceVisible) {
            Minecraft mc = Minecraft.getInstance();
            if (first < 0) first = sinceVisible;
            double sc = Math.max(0.8, Math.min(1.7, mc.getWindow().getHeight() / 540.0));
            int x = gap, y = gap, w = wReal, h = hReal, r = h / 2;
            g.pose().pushPose();
            g.pose().scale((float) (1 / gui), (float) (1 / gui), 1f);
            Soft.fill(g, x + 2, y + 4, x + w + 2, y + h + 4, r, argb(0x000000, 0.35f));
            Soft.fill(g, x, y, x + w, y + h, r, argb(PLUM, 0.96f));
            Soft.outline(g, x, y, x + w, y + h, r, argb(0xFF8FCB, 0.9f));
            int ix = x + pad, iy = y + (h - plate) / 2;
            Soft.fill(g, ix, iy, ix + plate, iy + plate, plate / 2, argb(0x4A2048, 1f));
            Soft.outline(g, ix, iy, ix + plate, iy + plate, plate / 2, argb(0xFF8FCB, 0.55f));
            float k = plate * 0.58f / 16f;
            g.pose().pushPose();
            g.pose().translate(ix + plate / 2f - 8 * k, iy + plate / 2f - 8 * k, 0);
            g.pose().scale(k, k, 1f);
            g.renderItem(new ItemStack(icon), 0, 0);
            g.pose().popPose();
            int barSpace = bar >= 0 ? (int) Math.round(8 * sc) : 0;
            int tx = ix + plate + pad;
            int ty = y + pad - 1 + (h - pad * 2 - lines.size() * lineH - barSpace) / 2;
            for (int i = 0; i < lines.size(); i++) {
                g.pose().pushPose();
                g.pose().translate(tx, ty + i * lineH, 0);
                g.pose().scale(text, text, 1f);
                g.drawString(mc.font, lines.get(i), 0, 0, i == 0 ? PINK : MUTED, false);
                g.pose().popPose();
            }
            if (bar >= 0) {
                int bh = Math.max(3, (int) Math.round(4 * sc)), bw = w - (tx - x) - pad, by = y + h - pad - bh + 1;
                Soft.fill(g, tx, by, tx + bw, by + bh, bh / 2, argb(0x000000, 0.45f));
                Soft.fill(g, tx, by, tx + Math.max(bh, (int) (bw * bar)), by + bh, bh / 2, argb(0xFF8FCB, 1f));
            }
            g.pose().popPose();
            return sinceVisible - first < ToastRules.SHOW_MS ? Visibility.SHOW : Visibility.HIDE;
        }
    }
}
