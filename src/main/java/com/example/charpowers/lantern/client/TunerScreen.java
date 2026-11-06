package com.example.charpowers.lantern.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.PillButton;
import com.example.charpowers.client.ui.ScrollList;
import com.example.charpowers.client.ui.Slider;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.client.ui.UiScreen;
import com.example.charpowers.lantern.ConstructEntity;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

// /constructs: a small tuner. Pick a construct and an animation, it plays over and over beside you, nudge its numbers and see it
// change at once. Everything saves to a file by itself and real constructs use it straight away. The card sits to one side and
// scrolls, so the construct stays in view at any window size.
public class TunerScreen extends UiScreen {
    private static final String[] KINDS = {"fist", "slap", "slam", "throw", "greatsword", "dagger", "minigun", "turret"};
    private static final int PREVIEW_ID = -870001;
    private static final int MOST_W = 216, MARGIN = 6;

    private List<String> animations = new ArrayList<>();
    private String kind = "fist", animation = "";
    private ConstructEntity preview;
    private double length = 40;
    private int age;
    private final ScrollList list = new ScrollList();
    private float contentH = 200;

    public TunerScreen() {
        super(Component.literal("Construct Tuner"));
    }

    @Override
    protected void init() {
        pick(kind);
    }

    private static boolean shift() {
        return hasShiftDown();
    }

    private BakedAnimations baked() {
        return GeckoLibCache.getBakedAnimations().get(new ResourceLocation(CharPowers.MODID, "animations/construct/" + kind + ".animation.json"));
    }

    private void pick(String which) {
        kind = which;
        animations = new ArrayList<>();
        BakedAnimations all = baked();
        if (all != null) animations.addAll(all.animations().keySet());
        animation = animations.isEmpty() ? "" : animations.get(0);
        replay();
    }

    private void replay() {
        age = 0;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (mc.level == null || p == null) return;
        removePreview();
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 at = p.position().add(look.scale(7)).add(look.cross(new Vec3(0, 1, 0)).normalize().scale(2));
        preview = ConstructEntity.preview(mc.level, kind, animation, at.x, at.y, at.z, p.getYRot());
        mc.level.putNonPlayerEntity(PREVIEW_ID, preview);
        BakedAnimations all = baked();
        var baked = all == null ? null : all.getAnimation(animation);
        length = baked == null ? 40 : baked.length() / Math.max(0.05, ConstructTweaks.speed(kind, animation));
    }

    private void removePreview() {
        Minecraft mc = Minecraft.getInstance();
        if (preview != null && mc.level != null) mc.level.removeEntity(PREVIEW_ID, Entity.RemovalReason.DISCARDED);
        preview = null;
    }

    @Override
    public void tick() {
        if (++age > length + 15) replay();
    }

    @Override
    public void removed() {
        removePreview();
    }

    // No dim over the world: the whole point is to watch the construct.
    @Override
    public void renderBackground(GuiGraphics g) {}

    private void save() {
        ConstructTweaks.save();
        length = Math.max(1, length);
    }

    private float row(float x0, float x1, float y, String id, String name, DoubleSupplier value, DoubleConsumer set, double step, double bigStep, double low, double sliderLow, double sliderHigh) {
        GuiGraphics g = ui.g;
        double v = value.getAsDouble();
        String shown = String.format("%.2f", v);
        Ui.text(g, name, x0, y, Ui.SOFT, Ui.S);
        Ui.right(g, shown, x1 - 34, y, Ui.TEXT, Ui.S);
        float mid = y + 5.5f;
        PillButton.round(ui, id + "-", x1 - 24, mid, 6, '-', true, () -> {
            set.accept(Math.max(low, value.getAsDouble() - (shift() ? bigStep : step)));
            save();
        });
        PillButton.round(ui, id + "+", x1 - 7, mid, 6, '+', true, () -> {
            set.accept(value.getAsDouble() + (shift() ? bigStep : step));
            save();
        });
        Slider.draw(ui, id, x0, y + 12, x1 - x0, 9, v, sliderLow, sliderHigh, step, d -> {
            set.accept(Math.max(low, d));
            save();
        });
        return 25;
    }

    @Override
    protected void paint(GuiGraphics g, int mx, int my, float partial) {
        float w = Math.min(MOST_W, width - MARGIN * 2), x0 = MARGIN, y0 = MARGIN, x1 = x0 + w;
        float h = Math.min(30 + contentH + 8, height - MARGIN * 2), y1 = y0 + h;
        Card.main(g, x0, y0, x1, y1);
        Ui.centre(g, "Construct Tuner", (x0 + x1) / 2f, Ui.middle(y0 + 12, Ui.L), Ui.PINK, Ui.L);
        float top = y0 + 23, bottom = y1 - 8;
        Card.well(g, x0 + 7, top, x1 - 7, bottom, Ui.R_INNER);
        list.begin(ui, x0 + 7, top + 2, x1 - 7, bottom - 2, contentH);
        float inner0 = x0 + 13, inner1 = x1 - 13 - (list.needsBar() ? ScrollList.barWidth() : 0);
        float start = top + 5 - list.offset();
        contentH = rows(g, inner0, inner1, start) - start + 6;
        list.end(ui, Ui.WELL_TOP, Ui.WELL_BOTTOM);
    }

    // Pills that wrap onto as many rows as they need.
    private float pills(float x0, float x1, float y, List<String> labels, String picked, java.util.function.Consumer<String> onPick) {
        float gap = 3, h = 15, x = x0;
        for (String label : labels) {
            float pw = Ui.width(label, Ui.S) + 14;
            if (x + pw > x1 && x > x0) {
                x = x0;
                y += h + gap;
            }
            boolean mine = label.equals(picked);
            PillButton.draw(ui, "p" + label, x, y, pw, h, label, mine ? PillButton.Kind.PRIMARY : PillButton.Kind.NORMAL, true, () -> onPick.accept(label));
            x += pw + gap;
        }
        return y + h + gap;
    }

    private float rows(GuiGraphics g, float x0, float x1, float y) {
        y = pills(x0, x1, y, List.of(KINDS), kind, this::pick) + 3;
        Ui.text(g, "ANIMATION", x0, y, Ui.SOFT, Ui.XS);
        y += 11;
        y = pills(x0, x1, y, animations, animation, which -> {
            animation = which;
            replay();
        }) + 3;
        String k = kind;
        ConstructTweaks.Tweak t = ConstructTweaks.of(k);
        y += row(x0, x1, y, "z", "Forward / back", () -> t.z, v -> ConstructTweaks.edit(k).z = v, 0.05, 0.5, -99, -5, 5);
        y += row(x0, x1, y, "x", "Left / right", () -> t.x, v -> ConstructTweaks.edit(k).x = v, 0.05, 0.5, -99, -5, 5);
        y += row(x0, x1, y, "y", "Up / down", () -> t.y, v -> ConstructTweaks.edit(k).y = v, 0.05, 0.5, -99, -5, 5);
        y += row(x0, x1, y, "turn", "Turn", () -> t.turn, v -> ConstructTweaks.edit(k).turn = v, 5, 45, -360, -180, 180);
        y += row(x0, x1, y, "tilt", "Tilt", () -> t.tilt, v -> ConstructTweaks.edit(k).tilt = v, 5, 45, -360, -180, 180);
        y += row(x0, x1, y, "roll", "Roll", () -> t.roll, v -> ConstructTweaks.edit(k).roll = v, 5, 45, -360, -180, 180);
        y += row(x0, x1, y, "size", "Size", () -> t.size, v -> ConstructTweaks.edit(k).size = v, 0.05, 0.25, 0.05, 0.05, 4);
        y += row(x0, x1, y, "speed", "Speed of this one", () -> ConstructTweaks.speed(k, animation), v -> ConstructTweaks.setSpeed(k, animation, v), 0.05, 0.25, 0.05, 0.05, 4);
        y += 2;
        float each = (x1 - x0 - 6) / 3f;
        PillButton.draw(ui, "replay", x0, y, each, 15, "Replay", PillButton.Kind.PRIMARY, true, this::replay);
        PillButton.draw(ui, "reset", x0 + each + 3, y, each, 15, "Reset", PillButton.Kind.NORMAL, true, () -> {
            ConstructTweaks.reset(kind);
            ConstructTweaks.save();
            replay();
        });
        PillButton.draw(ui, "folder", x0 + (each + 3) * 2, y, each, 15, "Folder", PillButton.Kind.NORMAL, true, () -> Util.getPlatform().openFile(ConstructTweaks.file().getParent().toFile()));
        y += 19;
        y = Ui.para(g, Component.literal("Hold Shift for bigger steps. It saves by itself, and real constructs use it now. The numbers are in:"), x0, y, x1 - x0, Ui.SOFT, Ui.XS, -1);
        return Ui.para(g, Component.literal("config/charpowers/construct_tweaks.json"), x0, y, x1 - x0, Ui.TEXT, Ui.XS, -1);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return list.wheel(mx, my, delta, 20) || super.mouseScrolled(mx, my, delta);
    }
}
