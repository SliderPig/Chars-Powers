package com.example.charpowers.house.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.Soft;
import com.example.charpowers.house.model.HouseRules;
import com.example.charpowers.house.model.IsoThumb;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

// The small rounded picker: a card per house with its picture, locked ones grey with what they're waiting for.
// Drawn in real screen pixels so the corners stay smooth at any GUI scale.
public final class HousePickerScreen extends Screen {
    private static final ResourceLocation CAT = new ResourceLocation(CharPowers.MODID, "textures/gui/houses/cat.png");
    private static final int BACK = 0xF4231727, CARD = 0xFF38233F, CARD_HOT = 0xFF4E3058, WELL = 0xFF1B1120, LOCK = 0xC0170F1C,
            PINK = 0xFFFFA8D8, TEXT = 0xFFF6E8F4, MUTED = 0xFFB7A0BA, RED = 0xFFFF8A8A, EDGE = 0x55FFA8D8;
    private static final int THUMB_W = 180, THUMB_H = 130, COLS = 5;

    private record House(String id, String name, int w, int h, int d, int blocks, boolean custom, boolean awk, String lock, byte[] thumb) {}

    private static final int PER_PAGE = COLS * 3;
    private static final java.util.Map<String, Object[]> SENT_PICTURES = new java.util.HashMap<>();

    private final List<House> houses = new ArrayList<>();
    private final int level;
    private final boolean awakened;
    private final long cooldownAtOpen;
    private int age, page;
    private double unit = 1;
    private GuiGraphics g;
    private int realX, realY, panelX, panelY, panelW, panelH;

    public HousePickerScreen(CompoundTag tag) {
        super(Component.literal("House Builder"));
        level = tag.getInt("level");
        awakened = tag.getBoolean("awakened");
        cooldownAtOpen = tag.getLong("cooldown");
        ListTag list = tag.getList("houses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag h = list.getCompound(i);
            houses.add(new House(h.getString("id"), h.getString("name"), h.getInt("w"), h.getInt("h"), h.getInt("d"), h.getInt("blocks"),
                    h.getBoolean("custom"), h.getBoolean("awk"), h.contains("lock") ? h.getString("lock") : null, h.contains("thumb") ? h.getByteArray("thumb") : null));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        age++;
    }

    private long cooldown() {
        return Math.max(0, cooldownAtOpen - age);
    }

    private int U(double d) {
        return (int) Math.round(d * unit);
    }

    // The normal Minecraft font, only ever at whole real pixels per font pixel, so it stays sharp.
    private int textScale() {
        return Math.max(1, (int) Math.round(unit * 2));
    }

    private void text(String s, double x, double y, int colour, boolean centre) {
        text(s, x, y, colour, centre, textScale());
    }

    private void text(String s, double x, double y, int colour, boolean centre, int k) {
        int px = (int) Math.round(x - (centre ? font.width(s) * k / 2.0 : 0));
        g.pose().pushPose();
        g.pose().translate(px, (int) Math.round(y), 0);
        g.pose().scale(k, k, 1);
        g.drawString(font, s, 0, 0, colour, false);
        g.pose().popPose();
    }

    private float width(String s) {
        return font.width(s) * textScale();
    }

    // A name too wide for its card breaks at the space nearest the middle.
    private List<String> lines(String name, int k, int max) {
        if (font.width(name) * k <= max) return List.of(name);
        int best = -1;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == ' ' && (best < 0 || Math.abs(i - name.length() / 2) < Math.abs(best - name.length() / 2))) best = i;
        }
        return best < 0 ? List.of(name) : List.of(name.substring(0, best), name.substring(best + 1));
    }

    // The biggest whole scale at which every name fits on its card, so they all match.
    private int nameScale(int max) {
        for (int k = textScale(); k > 1; k--) {
            int finalK = k;
            boolean fits = houses.stream().allMatch(h -> lines(h.name, finalK, max).stream().allMatch(l -> font.width(l) * finalK <= max));
            if (fits) return k;
        }
        return 1;
    }

    private int pages() {
        return Math.max(1, (houses.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private List<House> shown() {
        int from = page * PER_PAGE;
        return houses.subList(from, Math.min(houses.size(), from + PER_PAGE));
    }

    private int rows() {
        return Math.max(2, (shown().size() + COLS - 1) / COLS);
    }

    // A picture the server made from an own file's blocks, turned into a texture the first time it's seen. Null if there's none.
    private ResourceLocation sentPicture(House h) {
        if (h.thumb == null) return null;
        int hash = java.util.Arrays.hashCode(h.thumb);
        Object[] known = SENT_PICTURES.get(h.id);
        if (known != null && (int) known[0] == hash) return (ResourceLocation) known[1];
        int[] pixels = IsoThumb.unpack(h.thumb);
        if (pixels == null) return null;
        NativeImage image = new NativeImage(IsoThumb.W, IsoThumb.H, false);
        for (int y = 0; y < IsoThumb.H; y++) {
            for (int x = 0; x < IsoThumb.W; x++) {
                int argb = pixels[x + y * IsoThumb.W];
                image.setPixelRGBA(x, y, (argb & 0xFF00FF00) | (argb & 0xFF) << 16 | (argb >> 16) & 0xFF);
            }
        }
        ResourceLocation at = new ResourceLocation(CharPowers.MODID, "house_picture/" + h.id);
        Minecraft.getInstance().getTextureManager().register(at, new DynamicTexture(image));
        SENT_PICTURES.put(h.id, new Object[] {hash, at});
        return at;
    }

    private int[] arrow(int side) {
        int size = U(26), y = panelY + panelH - size - U(6);
        int x = side < 0 ? panelX + panelW / 2 - U(70) - size : panelX + panelW / 2 + U(70);
        return new int[] {x, y, x + size, y + size};
    }

    private boolean inside(int[] r) {
        return realX >= r[0] && realX < r[2] && realY >= r[1] && realY < r[3];
    }

    // Where a card goes: {x0, y0, x1, y1} in real pixels.
    private int[] card(int i) {
        int cw = U(172), ch = U(150), gap = U(12);
        int col = i % COLS, row = i / COLS;
        int gridW = COLS * cw + (COLS - 1) * gap;
        int x = panelX + (panelW - gridW) / 2 + col * (cw + gap);
        int y = panelY + U(58) + row * (ch + gap);
        return new int[] {x, y, x + cw, y + ch};
    }

    private int hovered() {
        for (int i = 0; i < shown().size(); i++) {
            int[] c = card(i);
            if (realX >= c[0] && realX < c[2] && realY >= c[1] && realY < c[3]) return i;
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        g = graphics;
        Window window = Minecraft.getInstance().getWindow();
        double gui = window.getGuiScale();
        int sw = window.getWidth(), sh = window.getHeight();
        unit = Math.max(0.5, Math.min(1.5, Math.min(sw / 960.0, sh / (90.0 + (pages() > 1 ? 34 : 0) + rows() * 162.0))));
        realX = (int) (mouseX * gui);
        realY = (int) (mouseY * gui);
        panelW = COLS * U(172) + (COLS - 1) * U(12) + U(36);
        panelH = U(70) + rows() * (U(150) + U(12)) + U(10) + (pages() > 1 ? U(34) : 0);
        panelX = (sw - panelW) / 2;
        panelY = (sh - panelH) / 2;
        int hot = hovered();
        g.pose().pushPose();
        g.pose().scale((float) (1 / gui), (float) (1 / gui), 1);
        g.drawManaged(() -> {
            g.fill(0, 0, sw, sh, 0x66000000);
            Soft.plate(g, panelX, panelY, panelX + panelW, panelY + panelH, U(20), BACK, U(4));
            Soft.outline(g, panelX, panelY, panelX + panelW, panelY + panelH, U(20), EDGE);
            // a pink moon for the black cat to sit in front of
            int moon = U(44);
            Soft.fill(g, panelX + panelW - moon - U(14), panelY + U(6), panelX + panelW - U(14), panelY + U(6) + moon, moon / 2, PINK);
            for (int side : pages() > 1 ? new int[] {-1, 1} : new int[0]) {
                int[] r = arrow(side);
                Soft.fill(g, r[0], r[1], r[2], r[3], (r[3] - r[1]) / 2, inside(r) ? CARD_HOT : CARD);
            }
            for (int i = 0; i < shown().size(); i++) {
                int[] c = card(i);
                House h = shown().get(i);
                boolean open = h.lock == null;
                int up = i == hot && open ? U(2) : 0;
                Soft.plate(g, c[0], c[1] - up, c[2], c[3] - up, U(14), i == hot && open ? CARD_HOT : CARD, U(3));
                Soft.well(g, c[0] + U(6), c[1] + U(6) - up, c[2] - U(6), c[3] - U(46) - up, U(10), WELL, U(2));
                if (i == hot && open) Soft.outline(g, c[0], c[1] - up, c[2], c[3] - up, U(14), PINK);
            }
        });
        // pictures go straight to the screen, so they come after the shapes they sit on and before the text over them
        RenderSystem.enableBlend();
        for (int i = 0; i < shown().size(); i++) {
            House h = shown().get(i);
            int[] c = card(i);
            int up = i == hot && h.lock == null ? U(2) : 0;
            int ax = c[0] + U(9), ay = c[1] + U(8) - up, aw = c[2] - c[0] - U(18), ah = c[3] - c[1] - U(56);
            ResourceLocation pic = h.custom ? sentPicture(h) : new ResourceLocation(CharPowers.MODID, "textures/gui/houses/" + h.id + ".png");
            if (pic != null) {
                float grey = h.lock == null ? 1f : 0.34f;
                RenderSystem.setShaderColor(grey, grey, grey + 0.03f, 1f);
                float fit = Math.min(aw / (float) THUMB_W, ah / (float) THUMB_H);
                int pw = (int) (THUMB_W * fit), ph = (int) (THUMB_H * fit);
                g.blit(pic, ax + (aw - pw) / 2, ay + (ah - ph) / 2, pw, ph, 0, 0, THUMB_W, THUMB_H, THUMB_W, THUMB_H);
            }
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int catSize = U(40);
        g.blit(CAT, panelX + panelW - catSize - U(16), panelY + U(8), catSize, catSize, 0, 0, 32, 32, 32, 32);
        RenderSystem.disableBlend();
        g.drawManaged(() -> {
            text("House Builder", panelX + U(20), panelY + U(12), PINK, false);
            long cd = cooldown();
            String sub = "Mastery " + level + (awakened ? "  ·  Awakened" : "");
            text(sub, panelX + U(20), panelY + U(30), MUTED, false);
            String ready = cd > 0 ? "Next house in " + HouseRules.clock(cd) : "Ready to build";
            text(ready, panelX + panelW - U(66) - width(ready), panelY + U(30), cd > 0 ? RED : TEXT, false);
            int nameMax = U(172) - U(14);
            int nk = nameScale(nameMax);
            if (pages() > 1) {
                int[] l = arrow(-1), r = arrow(1);
                text("<", (l[0] + l[2]) / 2.0, l[1] + (l[3] - l[1] - 8 * textScale()) / 2.0, PINK, true);
                text(">", (r[0] + r[2]) / 2.0, r[1] + (r[3] - r[1] - 8 * textScale()) / 2.0, PINK, true);
                text((page + 1) + " / " + pages(), panelX + panelW / 2.0, l[1] + (l[3] - l[1] - 8 * textScale()) / 2.0, MUTED, true);
            }
            for (int i = 0; i < shown().size(); i++) {
                House h = shown().get(i);
                int[] c = card(i);
                int up = i == hot && h.lock == null ? U(2) : 0;
                int mid = (c[0] + c[2]) / 2;
                List<String> name = lines(h.name, nk, nameMax);
                int lineH = 10 * nk;
                int nameTop = c[3] - U(46) - up + (U(46) - name.size() * lineH) / 2 + nk;
                for (int l = 0; l < name.size(); l++) text(name.get(l), mid, nameTop + l * lineH, h.lock == null ? TEXT : MUTED, true, nk);
                if (h.custom && h.thumb == null) text(h.blocks + " blocks", mid, c[1] + U(44) - up, MUTED, true);
                if (h.lock != null) {
                    int tk = textScale(), pill = 8 * tk + U(10), pw = font.width(h.lock) * tk + U(20), py = c[1] + U(44);
                    Soft.fill(g, mid - pw / 2, py, mid + pw / 2, py + pill, pill / 2, LOCK);
                    text(h.lock, mid, py + (pill - 8 * tk) / 2.0, PINK, true, tk);
                }
            }
        });
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            double gui = Minecraft.getInstance().getWindow().getGuiScale();
            realX = (int) (mx * gui);
            realY = (int) (my * gui);
            if (pages() > 1) {
                if (inside(arrow(-1))) {
                    turnPage(-1);
                    return true;
                }
                if (inside(arrow(1))) {
                    turnPage(1);
                    return true;
                }
            }
            int i = hovered();
            if (i >= 0 && shown().get(i).lock == null) {
                if (cooldown() > 0) return true;
                choose(i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void turnPage(int by) {
        page = (page + by + pages()) % pages();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (pages() > 1) turnPage(delta > 0 ? -1 : 1);
        return true;
    }

    private void choose(int i) {
        House h = shown().get(i);
        onClose();
        HouseClient.start(h.id, h.name, h.w, h.h, h.d);
    }

    // For the walkthrough in the real game.
    public void chooseForCheck(int i) {
        choose(i);
    }
}
