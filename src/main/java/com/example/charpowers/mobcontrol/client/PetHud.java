package com.example.charpowers.mobcontrol.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.Soft;
import com.example.charpowers.mobcontrol.PetNet;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

// Your pets on your own screen: a 3D ping arrow hovering above each one, and a squad panel down the left edge with a face, name,
// health and what it is doing. Only the owner is ever told about the pets, so nobody else sees any of it.
public final class PetHud {
    private static final ResourceLocation PING = new ResourceLocation(CharPowers.MODID, "textures/gui/pet_ping.png");
    private static final int MAX_ROWS = 5;
    private static final int PLUM = 0xFF241022, PLUM_LIGHT = 0xFF3A1B38, PINK = 0xFFFF8FCB, PINK_DARK = 0xFFC2457F, CREAM = 0xFFFFE6F3, MUTED = 0xFFC9A6C0;

    // 0 = panel and pings, 1 = pings only, 2 = nothing
    private static int mode = load();
    private static List<PetNet.Entry> pets = List.of();
    private static Set<Integer> ids = Set.of();
    private static final KeyMapping KEY = new KeyMapping("key.charpowers.pet_panel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_COMMA, "key.categories.charpowers");

    private PetHud() {}

    // From the server, once a second. An empty list means no pets.
    public static void receive(List<PetNet.Entry> list) {
        pets = list;
        Set<Integer> now = new HashSet<>();
        for (PetNet.Entry e : list) now.add(e.id());
        ids = now;
    }

    public static List<PetNet.Entry> pets() {
        return pets;
    }

    // for the real-game check: 0 panel + pings, 1 pings, 2 off
    public static void setMode(int m) {
        mode = m;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("charpowers").resolve("pet_hud.properties");
    }

    private static int load() {
        try (InputStream in = Files.newInputStream(file())) {
            Properties p = new Properties();
            p.load(in);
            return Math.max(0, Math.min(2, Integer.parseInt(p.getProperty("mode", "0").trim())));
        } catch (IOException | RuntimeException e) {
            return 0;
        }
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            Properties p = new Properties();
            p.setProperty("mode", Integer.toString(mode));
            try (OutputStream out = Files.newOutputStream(file())) {
                p.store(out, "Pet panel: 0 = panel and pings, 1 = pings only, 2 = nothing (the key cycles it)");
            }
        } catch (IOException ignored) {
            // it just forgets next time
        }
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(KEY);
        }

        // registered above everything, but the game still hides overlays with F1
        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("pet_panel", (gui, g, partial, w, h) -> draw(g, w, h));
        }
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            while (KEY.consumeClick()) {
                mode = (mode + 1) % 3;
                save();
                var p = Minecraft.getInstance().player;
                if (p != null) p.displayClientMessage(Component.literal(mode == 0 ? "Pet panel and pings on" : mode == 1 ? "Pet pings only" : "Pet panel off"), true);
            }
        }

        @SubscribeEvent
        public static void ping(RenderLivingEvent.Post<?, ?> e) {
            if (mode == 2 || !ids.contains(e.getEntity().getId())) return;
            drawPing(e.getEntity(), e.getPoseStack(), e.getMultiBufferSource());
        }
    }

    // ---- the ping above the head: a chunky 3D arrow that points down at your pet, slowly spinning, with a ripple round its tip.
    // Pink while it follows you, hot orange while it fights.

    private static final float[][] CORNERS = {{-1, 1}, {1, 1}, {1, -1}, {-1, -1}}; // round the outside, so faces wind the right way

    private static void drawPing(LivingEntity pet, PoseStack pose, MultiBufferSource buffers) {
        Minecraft mc = Minecraft.getInstance();
        double dist = Math.sqrt(mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(pet.getX(), pet.getY(), pet.getZ()));
        float alpha = (float) Math.max(0, Math.min(1, (48 - dist) / 24.0));
        if (dist < 2.5) alpha *= (float) Math.max(0, (dist - 1.0) / 1.5);
        if (alpha <= 0.04f) return;
        float ticks = pet.tickCount + mc.getFrameTime();
        float spin = ticks * 0.045f;
        float size = 0.20f + (float) Math.min(0.20, dist * 0.010); // small: you can tell it is yours, but it stays out of your face
        boolean fights = false;
        for (PetNet.Entry e : pets) if (e.id() == pet.getId()) fights = e.state() == 1;
        int[] main = fights ? new int[]{255, 112, 44} : new int[]{255, 118, 186};
        int[] top = fights ? new int[]{255, 204, 120} : new int[]{255, 198, 230};
        int[] edge = fights ? new int[]{84, 26, 18} : new int[]{58, 24, 66};
        int a = (int) (alpha * 255);

        pose.pushPose();
        pose.translate(0, pet.getBbHeight() + 0.22 + Math.sin(ticks * 0.11f) * 0.04, 0);
        pose.scale(size, size, size);
        VertexConsumer v = buffers.getBuffer(RenderType.text(PING));

        // the ripple, in the flat of the tip: it spreads out and fades, again and again
        float phase = (ticks * 0.022f) % 1f;
        ring(v, pose.last().pose(), 0.12f + phase * 0.95f, 0.06f, -0.04f, main, (int) (a * (1 - phase) * 0.75f));

        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(spin));
        Matrix4f m = pose.last().pose();
        // a dark shell first (only its inside faces show), which draws a clean outline round the arrow on any background
        arrow(v, m, 1.2f, true, edge, edge, spin, a);
        arrow(v, m, 1f, false, main, top, spin, a);
        pose.popPose();
        pose.popPose();
    }

    // Tip at the bottom (y 0), the head up to 0.52, then the shaft up to 1.08.
    private static void arrow(VertexConsumer v, Matrix4f m, float grow, boolean inside, int[] main, int[] top, float spin, int a) {
        float head = 0.46f * grow, shaft = 0.19f * grow;
        float mid = 0.52f, tipY = 0.52f - 0.52f * (inside ? 1.3f : 1f), topY = 0.52f + 0.56f * (inside ? 1.12f : 1f);
        float bottom = mid - (inside ? 0.05f : 0f);
        for (int i = 0; i < 4; i++) {
            float[] c0 = CORNERS[i], c1 = CORNERS[(i + 1) % 4];
            int[] col = lit(main, spin + i * (float) (Math.PI / 2));
            // head side, a triangle from the tip
            quad(v, m, inside, col, a, 0, tipY, 0, c1[0] * head, bottom, c1[1] * head, c0[0] * head, bottom, c0[1] * head, c0[0] * head, bottom, c0[1] * head);
            // shaft side
            quad(v, m, inside, col, a, c0[0] * shaft, bottom, c0[1] * shaft, c1[0] * shaft, bottom, c1[1] * shaft, c1[0] * shaft, topY, c1[1] * shaft, c0[0] * shaft, topY, c0[1] * shaft);
        }
        // the top of the head round the shaft, and the top of the shaft
        float[][] c = CORNERS;
        quad(v, m, inside, top, a, c[0][0] * head, bottom, c[0][1] * head, c[1][0] * head, bottom, c[1][1] * head, c[2][0] * head, bottom, c[2][1] * head, c[3][0] * head, bottom, c[3][1] * head);
        quad(v, m, inside, top, a, c[0][0] * shaft, topY, c[0][1] * shaft, c[1][0] * shaft, topY, c[1][1] * shaft, c[2][0] * shaft, topY, c[2][1] * shaft, c[3][0] * shaft, topY, c[3][1] * shaft);
    }

    // A flat ring, drawn from above and below.
    private static void ring(VertexConsumer v, Matrix4f m, float radius, float width, float y, int[] col, int a) {
        if (a <= 2) return;
        int steps = 20;
        for (int i = 0; i < steps; i++) {
            double t0 = i * Math.PI * 2 / steps, t1 = (i + 1) * Math.PI * 2 / steps;
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0), c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            float in = radius - width, out = radius + width;
            quad(v, m, false, col, a, c0 * in, y, s0 * in, c0 * out, y, s0 * out, c1 * out, y, s1 * out, c1 * in, y, s1 * in);
            quad(v, m, true, col, a, c0 * in, y, s0 * in, c0 * out, y, s0 * out, c1 * out, y, s1 * out, c1 * in, y, s1 * in);
        }
    }

    // Sides facing the light are brighter, which is what makes it read as solid while it turns.
    private static int[] lit(int[] col, float angle) {
        float k = 0.8f + 0.2f * Math.max(0f, (float) Math.cos(angle - 0.7f));
        return new int[]{(int) (col[0] * k), (int) (col[1] * k), (int) (col[2] * k)};
    }

    private static void quad(VertexConsumer v, Matrix4f m, boolean flip, int[] c, int a, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
        if (flip) {
            corner(v, m, c, a, x3, y3, z3);
            corner(v, m, c, a, x2, y2, z2);
            corner(v, m, c, a, x1, y1, z1);
            corner(v, m, c, a, x0, y0, z0);
        } else {
            corner(v, m, c, a, x0, y0, z0);
            corner(v, m, c, a, x1, y1, z1);
            corner(v, m, c, a, x2, y2, z2);
            corner(v, m, c, a, x3, y3, z3);
        }
    }

    private static void corner(VertexConsumer v, Matrix4f m, int[] c, int a, float x, float y, float z) {
        v.vertex(m, x, y, z).color(c[0], c[1], c[2], a).uv(0.5f, 0.5f).uv2(0xF000F0).endVertex();
    }

    // ---- the squad panel

    private static Component styled(String s) {
        return Component.literal(s);
    }

    private static void text(GuiGraphics g, String s, float x, float y, float k, int colour) {
        k = com.example.charpowers.client.ui.Ui.snapRealScale(k);
        g.pose().pushPose();
        g.pose().translate(Math.round(x), Math.round(y), 0);
        g.pose().scale(k, k, 1);
        g.drawString(Minecraft.getInstance().font, styled(s), 0, 0, colour, false);
        g.pose().popPose();
    }

    private static int width(String s, float k) {
        return (int) (Minecraft.getInstance().font.width(styled(s)) * com.example.charpowers.client.ui.Ui.snapRealScale(k));
    }

    static void draw(GuiGraphics g, int guiW, int guiH) {
        Minecraft mc = Minecraft.getInstance();
        if (mode != 0 || pets.isEmpty() || mc.player == null || mc.screen != null) return;
        double gui = mc.getWindow().getGuiScale();
        int realW = mc.getWindow().getWidth(), realH = mc.getWindow().getHeight();
        // one unit of the layout below, in real pixels: grows with the window, never tiny
        float u = (float) Math.max(1.2, Math.min(3.0, realH / 400.0));
        float tk = realH >= 1800 ? 4f : 2f; // whole multiples of the font stay sharp
        int rows = Math.min(MAX_ROWS, pets.size());
        int extra = pets.size() - rows;
        int rowW = Math.max(236, (int) (132 * u)), rowH = Math.max(50, (int) (30 * u)), gap = (int) (4 * u), faceS = rowH - (int) (6 * u);
        int total = rows * (rowH + gap) + (extra > 0 ? (int) (14 * u) : 0);
        int x0 = (int) (6 * u);
        // a little above the middle of the left edge: clear of the hotbar, chat, and the ability bar at the bottom
        int y0 = Math.max((int) (30 * u), realH / 2 - total / 2 - (int) (30 * u));
        g.pose().pushPose();
        g.pose().scale((float) (1 / gui), (float) (1 / gui), 1);
        for (int i = 0; i < rows; i++) {
            PetNet.Entry e = pets.get(i);
            int y = y0 + i * (rowH + gap);
            row(g, mc, e, x0, y, rowW, rowH, faceS, u, tk, gui);
        }
        if (extra > 0) {
            String more = "+" + extra + " more";
            int y = y0 + rows * (rowH + gap);
            int w = width(more, tk) + (int) (14 * u);
            Soft.fill(g, x0, y, x0 + w, y + (int) (12 * u), (int) (6 * u), 0xB0241022);
            text(g, more, x0 + 7 * u, y + (12 * u - 9 * tk) / 2f, tk, MUTED);
        }
        g.pose().popPose();
    }

    private static void row(GuiGraphics g, Minecraft mc, PetNet.Entry e, int x, int y, int w, int h, int faceS, float u, float tk, double gui) {
        int r = (int) (9 * u);
        Soft.fill(g, x + 1, y + 2, x + w + 1, y + h + 2, r, 0x50000000);
        Soft.fill(g, x, y, x + w, y + h, r, 0xD0241022);
        boolean fights = e.state() == 1;
        Soft.outline(g, x, y, x + w, y + h, r, fights ? 0xFFFF6FA8 : 0x90B25A93);
        // face
        int pad = (h - faceS) / 2;
        int fx = x + pad, fy = y + pad;
        Soft.fill(g, fx, fy, fx + faceS, fy + faceS, faceS / 2, PLUM_LIGHT);
        Entity live = mc.level == null ? null : mc.level.getEntity(e.id());
        int inset = Math.max(1, faceS / 8);
        g.enableScissor((int) ((fx + inset) / gui), (int) ((fy + inset) / gui), (int) Math.ceil((fx + faceS - inset) / gui), (int) Math.ceil((fy + faceS - inset) / gui));
        if (live instanceof LivingEntity le) {
            float big = Math.max(le.getBbHeight(), le.getBbWidth() * 1.2f);
            int scale = (int) Math.max(4, (faceS * 0.78f) / Math.max(0.4f, big));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, fx + faceS / 2, fy + faceS - inset - 1, scale, fx + faceS / 2f, fy + faceS * 0.2f, le);
        } else {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(e.type()));
            SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
            if (egg != null) {
                float k = faceS * 0.7f / 16f;
                g.pose().pushPose();
                g.pose().translate(fx + faceS * 0.15f, fy + faceS * 0.15f, 0);
                g.pose().scale(k, k, 1);
                g.renderItem(new ItemStack(egg), 0, 0);
                g.pose().popPose();
            }
        }
        g.disableScissor();
        Soft.outline(g, fx, fy, fx + faceS, fy + faceS, faceS / 2, 0xFFFF8FCB);
        // name, health bar and number
        int tx = fx + faceS + pad;
        int right = x + w - pad;
        String name = e.name();
        while (name.length() > 3 && width(name, tk) > right - tx - (int) (4 * u)) name = name.substring(0, name.length() - 2) + ".";
        text(g, name, tx, y + 4 * u, tk, CREAM);
        int bh = Math.max(3, (int) (6 * u));
        int by = y + h - pad - bh - (int) (1 * u);
        int bw = right - tx;
        float ratio = e.maxHealth() <= 0 ? 0 : Math.max(0f, Math.min(1f, e.health() / e.maxHealth()));
        Soft.fill(g, tx, by, tx + bw, by + bh, bh / 2, 0xFF120810);
        if (ratio > 0) {
            int fill = Math.max(bh, (int) (bw * ratio));
            int colour = ratio > 0.5f ? PINK : ratio > 0.25f ? 0xFFFFB35A : 0xFFFF5A6E;
            Soft.fill(g, tx, by, tx + fill, by + bh, bh / 2, colour);
            Soft.fill(g, tx + bh / 3, by + 1, tx + Math.max(bh / 3 + 2, fill - bh / 3), by + Math.max(2, bh / 3), Math.max(1, bh / 6), 0x55FFFFFF);
        }
        String hp = Math.round(e.health()) + "/" + Math.round(e.maxHealth());
        float small = tk;
        text(g, hp, tx, by - 9 * small - 1 * u, small, MUTED);
        String state = fights ? "Fighting" : e.state() == 2 ? "Staying" : e.state() == 3 ? "Guarding" : "Following";
        text(g, state, right - width(state, small), by - 9 * small - 1 * u, small, fights ? 0xFFFF8FB5 : MUTED);
    }
}
