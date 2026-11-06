package com.example.charpowers.client.power;

import com.example.charpowers.client.PowerPanels;
import com.example.charpowers.client.Soft;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.mixin.TreePowerTabAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.advancements.FrameType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.threetag.palladium.client.screen.power.TreeAbilityWidget;
import net.threetag.palladium.client.screen.power.TreePowerTab;
import net.threetag.palladium.power.Power;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityDescription;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.context.DataContext;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

// A power's skill tree, painted: faint dots on the well, soft round-ended lines between the skills, every skill's picture in a rounded well
// that glows when the mouse comes near. Scrolling, clicks and buying stay Palladium's.
public final class TreePainter {
    private static final int W = PowerMenu.INSIDE_W, H = PowerMenu.INSIDE_H, GRID = 22;
    // Palladium's colours for a power that doesn't choose its own: then ours are used
    private static final Color DEFAULT_ON = new Color(210, 112, 49);
    private static final int LINE_OFF = 0x60FFFFFF;
    // our own powers' plain tile: the clean well looks better than it does
    static final ResourceLocation OWN_TILE = new ResourceLocation("charpowers", "textures/gui/powers/background.png");

    private TreePainter() {}

    // Centres the tree in the well the first time it opens (Palladium's numbers left it high up, with an empty bottom half).
    private static void centre(TreePowerTabAccessor a, TreePowerTab tab) {
        if (a.charpowers$centered()) return;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (TreeAbilityWidget w : a.charpowers$entries()) {
            minX = Math.min(minX, w.getX() + 3);
            maxX = Math.max(maxX, w.getX() + 29);
            minY = Math.min(minY, w.getY());
            maxY = Math.max(maxY, w.getY() + 26);
        }
        if (minX == Integer.MAX_VALUE) return;
        a.charpowers$setScrollX(W / 2.0 - (minX + maxX) / 2.0);
        a.charpowers$setScrollY(H / 2.0 - (minY + maxY) / 2.0);
        a.charpowers$setCentered(true);
    }

    public static void draw(TreePowerTab tab, GuiGraphics g, int x, int y, int mx, int my, float partial) {
        TreePowerTabAccessor a = (TreePowerTabAccessor) tab;
        centre(a, tab);
        Power power = ((com.example.charpowers.mixin.PowerTabAccessor) tab).charpowers$holder().getPower();
        int sx = (int) Math.floor(a.charpowers$scrollX()), sy = (int) Math.floor(a.charpowers$scrollY());
        int ox = x + sx, oy = y + sy;
        boolean overlay = tab.getScreen().overlayScreen != null;
        int fmx = overlay ? -9999 : mx, fmy = overlay ? -9999 : my;

        boolean tile = power.getBackground() != null && !OWN_TILE.equals(power.getBackground().getTexture(DataContext.forPower(Minecraft.getInstance().player, ((com.example.charpowers.mixin.PowerTabAccessor) tab).charpowers$holder())));
        g.enableScissor(x, y, x + W, y + H);
        if (tile) {
            Minecraft mc = Minecraft.getInstance();
            ResourceLocation texture = power.getBackground().getTexture(DataContext.forPower(mc.player, ((com.example.charpowers.mixin.PowerTabAccessor) tab).charpowers$holder()));
            int k = Math.floorMod(sx, 16), l = Math.floorMod(sy, 16);
            for (int i = -1; i <= W / 16; i++) {
                for (int j = -1; j <= H / 16; j++) {
                    g.blit(texture, x + k + 16 * i, y + l + 16 * j, 0f, 0f, 16, 16, 16, 16);
                }
            }
        }
        g.drawManaged(() -> {
            if (tile) {
                Soft.round(g, x, y, x + W, y + H, 0, 0x90120D17, 0xB0120D17);
            } else {
                int dx = Math.floorMod(sx, GRID), dy = Math.floorMod(sy, GRID);
                for (int i = 0; i <= W / GRID + 1; i++) {
                    for (int j = 0; j <= H / GRID + 1; j++) {
                        Soft.round(g, x + dx + i * GRID - 0.75f, y + dy + j * GRID - 0.75f, x + dx + i * GRID + 0.75f, y + dy + j * GRID + 0.75f, 0.75f, 0x12FFFFFF);
                    }
                }
            }
            lines(a, power, g, ox, oy);
            for (TreeAbilityWidget w : a.charpowers$entries()) node(tab, w, g, ox + w.getX() + 16, oy + w.getY() + 13, fmx, fmy, sx, sy, x, y);
        });
        // the pictures go on after the batch (they're blits and items)
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        for (TreeAbilityWidget w : a.charpowers$entries()) {
            AbilityInstance ability = w.abilityInstance;
            if (!ability.isUnlocked()) continue;
            ability.getProperty(Ability.ICON).draw(mc, g, DataContext.forAbility(mc.player, ability), ox + w.getX() + 8, oy + w.getY() + 5);
            g.flush();
        }
        g.disableScissor();
    }

    private static void lines(TreePowerTabAccessor a, Power power, GuiGraphics g, int ox, int oy) {
        boolean own = !DEFAULT_ON.equals(power.getPrimaryColor());
        int on = own ? (0xFF000000 | power.getPrimaryColor().getRGB()) : Ui.PINK;
        for (TreePowerTab.Connection c : a.charpowers$connections()) {
            boolean lit = power.getPrimaryColor().equals(c.color);
            int colour = lit ? on : LINE_OFF;
            for (TreePowerTab.ConnectionLine l : c.lines) {
                float x0 = ox + l.startX, y0 = oy + l.startY, x1 = ox + l.endX, y1 = oy + l.endY;
                if (lit) Soft.line(g, x0, y0, x1, y1, 4f, Soft.alpha(colour, 0.22f));
                Soft.line(g, x0, y0, x1, y1, 2f, colour);
            }
        }
    }

    private static void node(TreePowerTab tab, TreeAbilityWidget w, GuiGraphics g, float cx, float cy, int mx, int my, int sx, int sy, int x, int y) {
        AbilityInstance ability = w.abilityInstance;
        boolean unlocked = ability.isUnlocked();
        boolean over = mx >= x && mx < x + W && my >= y && my < y + H && w.isMouseOver(sx, sy, mx - x, my - y);
        float lift = PowerMenu.ui.hover(w, over);
        FrameType frame = ability.getProperty(Ability.GUI_FRAME_TYPE);
        float half = 13f + lift * 0.6f;
        float r = frame == FrameType.GOAL ? half : frame == FrameType.CHALLENGE ? 9f : 7f;
        float x0 = cx - half, y0 = cy - half, x1 = cx + half, y1 = cy + half;
        Soft.shadow(g, x0, y0 + 1.5f, x1, y1 + 1.5f, r, 5f, 0x70000000, false);
        if (lift > 0.01f || unlocked) Soft.shadow(g, x0, y0, x1, y1, r, 4f + 5f * lift, Soft.alpha(Ui.PINK_GLOW, unlocked ? 0.25f + 0.75f * lift : lift * 0.7f), false);
        if (unlocked) {
            Soft.round(g, x0, y0, x1, y1, r, 0xFF413050, 0xFF2C2138);
            Soft.ring(g, x0, y0, x1, y1, r, 1f, Soft.mix(0xD0FF9AD2, 0xFFFFC0E4, lift), Soft.mix(0x90FF88C8, 0xD0FF88C8, lift));
            if (frame == FrameType.CHALLENGE) Soft.ring(g, x0 + 2f, y0 + 2f, x1 - 2f, y1 - 2f, r - 2f, 1f, 0x70FF88C8);
        } else {
            Soft.round(g, x0, y0, x1, y1, r, Soft.mix(0xFF231A2B, 0xFF33263F, lift), Soft.mix(0xFF19131F, 0xFF261C2E, lift));
            Soft.ring(g, x0, y0, x1, y1, r, 1f, Soft.mix(0x30FFFFFF, 0x70FF88C8, lift), Soft.mix(0x18FFFFFF, 0x40FF88C8, lift));
            Glyphs.lock(g, cx, cy - 0.5f, 1.25f, Soft.mix(0xFF6E5C7C, Ui.SOFT, lift));
            if (ability.getConfiguration().isBuyable()) Glyphs.buyable(g, x1 - 3.5f, y0 + 3.5f, 4f, Ui.PINK, Ui.ON_PINK);
        }
    }

    // Palladium drew a banner here. Now: a dim over the well while the buy box is open, and when there's no Info card, a tooltip for the skill under the mouse.
    public static void tooltips(TreePowerTab tab, GuiGraphics g, int mx, int my, int x, int y, boolean overlay) {
        dim(tab, g, overlay);
        if (overlay || PowerPanels.infoShowing()) return;
        TreeAbilityWidget w = tab.getAbilityHoveredOver(mx, my, x, y);
        if (w != null) showTip(w.abilityInstance, w);
    }

    public static void dim(net.threetag.palladium.client.screen.power.PowerTab tab, GuiGraphics g, boolean overlay) {
        if (!overlay) tab.fade = Math.max(0f, tab.fade - 0.04f);
        if (tab.fade <= 0.001f) return;
        g.pose().pushPose();
        g.pose().translate(0f, 0f, -150f);
        int colour = (int) (tab.fade * 255) << 24;
        g.drawManaged(() -> Soft.round(g, 0, 0, W, H, Ui.R_INNER, colour));
        g.pose().popPose();
    }

    static void showTip(AbilityInstance ability, Object key) {
        List<Component> lines = new ArrayList<>();
        lines.add(ability.getConfiguration().getDisplayName().copy().withStyle(st -> st.withColor(Ui.PINK & 0xFFFFFF)));
        AbilityDescription description = ability.getProperty(Ability.DESCRIPTION);
        Component text = description == null ? null : description.get(ability.isUnlocked());
        if (text != null) lines.add(text);
        PowerMenu.ui.tooltip(key, lines);
    }
}
