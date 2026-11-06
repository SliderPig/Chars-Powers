package com.example.charpowers.client.power;

import com.example.charpowers.client.PowerPanels;
import com.example.charpowers.client.Soft;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.Ui;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.threetag.palladium.client.screen.power.ListPowerTab;
import net.threetag.palladium.client.screen.power.PowerTab;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.Power;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.context.DataContext;

import javax.annotation.Nullable;
import java.util.List;

// A power shown as a list instead of a tree: the same well, with soft rows. The list itself (scrolling, clicks) stays Palladium's.
public final class ListPainter {
    private static final int W = PowerMenu.INSIDE_W, H = PowerMenu.INSIDE_H;
    private static AbilityInstance hovered;

    private ListPainter() {}

    @Nullable
    public static AbilityInstance hovered() {
        return hovered;
    }

    public static void draw(PowerTab tab, ListPowerTab.AbilityList list, GuiGraphics g, int x, int y, int mx, int my, float partial) {
        IPowerHolder holder = ((com.example.charpowers.mixin.PowerTabAccessor) tab).charpowers$holder();
        Power power = holder.getPower();
        hovered = null;
        g.enableScissor(x, y, x + W, y + H);
        ResourceLocation texture = power.getBackground() == null ? null : power.getBackground().getTexture(DataContext.forPower(Minecraft.getInstance().player, holder));
        if (texture != null && !TreePainter.OWN_TILE.equals(texture)) {
            for (int i = 0; i <= W / 16; i++) {
                for (int j = 0; j <= H / 16; j++) {
                    g.blit(texture, x + 16 * i, y + 16 * j, 0f, 0f, 16, 16, 16, 16);
                }
            }
            g.drawManaged(() -> Soft.round(g, x, y, x + W, y + H, 0, 0x90120D17, 0xB0120D17));
        }
        g.disableScissor();
        if (list == null) return;
        boolean overlay = tab.getScreen().overlayScreen != null;
        list.render(g, overlay ? -9999 : mx, overlay ? -9999 : my, partial);
        // Palladium's list ends with a square scrollbar: this paints a slim pill over it
        int most = list.getMaxScroll();
        if (most > 0) {
            int bar = list.getRowLeft() - 2 + list.getRowWidth() - 9;
            g.pose().pushPose();
            g.pose().translate(0f, 0f, 200f);
            g.drawManaged(() -> {
                g.fill(bar, y, bar + 6, y + H, Soft.mix(Ui.WELL_TOP, Ui.WELL_BOTTOM, 0.5f));
                float total = most + H, thumb = Math.max(14f, H * H / total), top = y + 1 + (H - 2 - thumb) * ((float) list.getScrollAmount() / most);
                Soft.round(g, bar + 2, y + 2, bar + 4, y + H - 2, 1f, 0x20FFFFFF);
                Soft.round(g, bar + 2, top, bar + 4, top + thumb, 1f, 0xC0FF88C8);
            });
            g.pose().popPose();
        }
    }

    public static void row(AbilityInstance ability, GuiGraphics g, int top, int left, int width, int height, boolean over) {
        if (over) hovered = ability;
        float lift = PowerMenu.ui.hover(ability, over);
        boolean unlocked = ability.isUnlocked();
        float x0 = left + 2, x1 = left + width - 14, y0 = top, y1 = top + 26;
        g.drawManaged(() -> {
            Card.row(g, x0, y0, x1, y1, Ui.R_ROW, lift, false);
            Card.well(g, x0 + 3, y0 + 3, x0 + 23, y0 + 23, 5);
            if (unlocked) Soft.ring(g, x0 + 3, y0 + 3, x0 + 23, y0 + 23, 5, 1f, 0x90FF88C8, 0x40FF88C8);
            else Glyphs.lock(g, x0 + 13, y0 + 12.5f, 1f, Ui.SOFT);
            float room = x1 - x0 - 34 - (ability.getConfiguration().isBuyable() && !unlocked ? 12 : 0);
            var name = ability.getConfiguration().getDisplayName();
            float size = Ui.fit(name, room, Ui.M, Ui.S, Ui.XS);
            List<FormattedCharSequence> lines = Ui.wrap(name, room, size);
            float ty = (y0 + y1) / 2f - lines.size() * Ui.step(size) / 2f + 0.5f;
            for (FormattedCharSequence line : lines) {
                Ui.line(g, line, x0 + 28, ty, unlocked ? Soft.mix(Ui.TEXT, 0xFFFFFFFF, lift) : Soft.mix(Ui.FAINT, Ui.SOFT, lift), size);
                ty += Ui.step(size);
            }
            if (ability.getConfiguration().isBuyable() && !unlocked) Glyphs.buyable(g, x1 - 9, (y0 + y1) / 2f, 4f, Ui.PINK, Ui.ON_PINK);
        });
        if (unlocked) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            Minecraft mc = Minecraft.getInstance();
            ability.getProperty(Ability.ICON).draw(mc, g, DataContext.forAbility(mc.player, ability), (int) x0 + 5, (int) y0 + 5);
            g.flush();
        }
    }

    public static void tooltips(PowerTab tab, GuiGraphics g, boolean overlay) {
        TreePainter.dim(tab, g, overlay);
        if (overlay || PowerPanels.infoShowing() || hovered == null) return;
        TreePainter.showTip(hovered, hovered);
    }
}
