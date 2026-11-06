package com.example.charpowers.client;

import com.example.charpowers.client.power.PowerMenu;
import com.example.charpowers.client.power.model.MenuLayout;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.Frame;
import com.example.charpowers.client.ui.PillButton;
import com.example.charpowers.client.ui.ScrollList;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.cloning.client.CloneClient;
import com.example.charpowers.cloning.client.CloneListScreen;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.mastery.MasteryCondition;
import com.example.charpowers.mastery.model.MasteryMath;
import com.example.charpowers.mixin.PowerTabAccessor;
import com.example.charpowers.client.power.Glyphs;
import com.example.charpowers.client.power.ListPainter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.fml.ModList;
import net.threetag.palladium.client.screen.power.ListPowerTab;
import net.threetag.palladium.client.screen.power.PowerTab;
import net.threetag.palladium.client.screen.power.PowersScreen;
import net.threetag.palladium.client.screen.power.TreeAbilityWidget;
import net.threetag.palladium.client.screen.power.TreePowerTab;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityDescription;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.icon.IIcon;

import javax.annotation.Nullable;
import java.util.List;

// The two cards tucked in behind the power menu when one of Char's own powers is open: mastery and the next unlock on the left, the skill
// your mouse is on (or was last on) on the right. They slide out from behind the main card. With no room for one, mastery folds into a
// pill in the header and the skill's words become a tooltip.
public final class PowerPanels {
    private static final int TUCK = 12, TOP = 12, BOTTOM = 184;
    // FSang's mod draws a bar beside the window, so with it around the right card starts past it
    private static final int FSANG_BAR = 34;

    private static AbilityInstance shown;
    private static IPowerHolder shownFrom;
    private static final ScrollList INFO = new ScrollList();
    private static float infoX0 = -1, infoY0, infoX1, infoY1;
    private static boolean infoCard, masteryCard;

    private PowerPanels() {}

    private static int fsangBar() {
        return ModList.get() != null && ModList.get().isLoaded("fsang") ? FSANG_BAR : 0;
    }

    @Nullable
    private static IPowerHolder holder(PowersScreen s) {
        PowerTab tab = s.selectedTab;
        if (tab == null) return null;
        IPowerHolder holder = ((PowerTabAccessor) tab).charpowers$holder();
        return holder == null || holder.getPower() == null ? null : holder;
    }

    @Nullable
    private static Integer level(IPowerHolder holder) {
        return CloneClient.masteryLevel(holder.getPower().getId().toString());
    }

    // True when the Info card is showing, so hovering a skill needs no tooltip.
    public static boolean infoShowing() {
        return infoCard;
    }

    // Drawn before the main card, so its shadow falls on these.
    public static void behind(Frame ui, PowersScreen s, int x, int y, float open) {
        infoCard = false;
        masteryCard = false;
        infoX0 = -1;
        IPowerHolder holder = holder(s);
        if (holder == null) return;
        Integer level = level(holder);
        if (level == null) return;
        int[] wide = MenuLayout.sideCards(s.width, fsangBar());
        int wideL = wide[0], wideR = wide[1];
        if (wideL > 0) {
            float out = wideL * (1f - open);
            masteryCard = true;
            left(ui, s, holder, level, x - wideL + out, y, wideL);
        }
        if (wideR > 0) {
            float out = wideR * (1f - open);
            infoCard = true;
            right(ui, s, holder, x + PowerMenu.W + fsangBar() - out, y, wideR);
        }
    }

    // ---- mastery card

    private static void left(Frame ui, PowersScreen s, IPowerHolder holder, int level, float x, int y, int wide) {
        GuiGraphics g = ui.g;
        float top = y + TOP, bottom = y + BOTTOM, x1 = x + wide, mid = x + wide / 2f;
        Card.side(g, x, top, x1 + TUCK, bottom);
        Ui.centre(g, "MASTERY", mid, top + 7, Ui.SOFT, Ui.XS);
        float cy = top + 40, r = Math.min(23f, wide / 2f - 8f);
        float done = ui.anims.from("masteryRing", 0f, level / (float) MasteryMath.MAX_LEVEL, 7f);
        Soft.arc(g, mid, cy, r, 3.5f, 0f, 0.9999f, 0x24FFFFFF);
        if (done > 0.001f) {
            Soft.arc(g, mid, cy, r, 3.5f, 0f, done, Ui.PINK);
        }
        String number = String.valueOf(level);
        Ui.centre(g, number, mid, Ui.middle(cy - 4, Ui.XL), Ui.TEXT, Ui.XL);
        Ui.centre(g, "/" + MasteryMath.MAX_LEVEL, mid, cy + 5, Ui.FAINT, Ui.XS);
        float yy = cy + r + 7;
        Ui.divider(g, x + 8, x1 - 6, yy);
        yy += 5;
        Ui.centre(g, "NEXT", mid, yy, Ui.SOFT, Ui.XS);
        yy += 11;
        boolean cloning = Mastery.CLONING.equals(holder.getPower().getId());
        float end = bottom - (cloning ? 26 : 6);
        Component next = nextUnlock(holder, level);
        float size = Ui.wrap(next, wide - 14, Ui.S).size() * Ui.step(Ui.S) <= end - yy ? Ui.S : Ui.XS;
        for (FormattedCharSequence line : Ui.wrap(next, wide - 14, size)) {
            if (yy + Ui.step(size) > end + 1) break;
            Ui.line(g, line, mid - Ui.widthOf(line, size) / 2f, yy, Ui.TEXT, size);
            yy += Ui.step(size);
        }
        if (cloning) {
            PillButton.draw(ui, "clones", x + 8, bottom - 23, wide - 14, 16, "Clones", PillButton.Kind.NORMAL, true, () -> Minecraft.getInstance().setScreen(new CloneListScreen(s)));
        }
    }

    // Folded mastery in the header, when the card has no room. Returns where the title has to stop.
    public static float inHeader(Frame ui, PowersScreen s, int x, int y) {
        float right = x + PowerMenu.W - PowerMenu.INSIDE_X - 3;
        if (masteryCard) return right;
        IPowerHolder holder = holder(s);
        if (holder == null) return right;
        Integer level = level(holder);
        if (level == null) return right;
        GuiGraphics g = ui.g;
        float h = 12, top = y + (PowerMenu.INSIDE_Y - h) / 2f + 0.5f;
        String label = level + "/" + MasteryMath.MAX_LEVEL;
        float textW = Ui.width(label, Ui.XS), w = textW + 30, x0 = right - w;
        Soft.round(g, x0, top, right, top + h, h / 2f, Ui.WELL_TOP, Ui.WELL_BOTTOM);
        Soft.ring(g, x0, top, right, top + h, h / 2f, 1f, Ui.EDGE_DARK, Ui.EDGE);
        float barX = x0 + 5, barW = 18, barY = top + h / 2f - 1.5f;
        Soft.round(g, barX, barY, barX + barW, barY + 3, 1.5f, 0x30FFFFFF);
        if (level > 0) Soft.round(g, barX, barY, barX + Math.max(3f, barW * level / MasteryMath.MAX_LEVEL), barY + 3, 1.5f, Ui.PINK);
        Ui.text(g, label, barX + barW + 3, Ui.middle(top + h / 2f, Ui.XS), Ui.TEXT, Ui.XS);
        if (ui.over(x0, top, right, top + h)) {
            ui.tooltip("mastery", Component.literal("Mastery " + label).withStyle(st -> st.withColor(Ui.PINK & 0xFFFFFF)), Component.literal("Next: ").append(nextUnlock(holder, level)));
        }
        if (Mastery.CLONING.equals(holder.getPower().getId())) {
            float w2 = 38;
            PillButton.draw(ui, "clones", x0 - w2 - 4, top, w2, h, "Clones", PillButton.Kind.NORMAL, true, () -> Minecraft.getInstance().setScreen(new CloneListScreen(s)));
            x0 -= w2 + 4;
        }
        return x0;
    }

    private static Component nextUnlock(IPowerHolder holder, int level) {
        AbilityInstance next = null;
        int nextLevel = Integer.MAX_VALUE;
        for (AbilityInstance a : holder.getAbilities().values()) {
            for (Condition c : a.getConfiguration().getUnlockingConditions()) {
                if (c instanceof MasteryCondition m && m.level() > level && m.level() < nextLevel) {
                    next = a;
                    nextLevel = m.level();
                }
            }
        }
        if (next == null) {
            String id = holder.getPower().getId().toString();
            if (CloneClient.masteryLevel(id + "#awakened") != null) return Component.literal("Awakened");
            return Component.translatable("power.charpowers." + holder.getPower().getId().getPath() + ".quest_hint");
        }
        return Component.empty().append(next.getConfiguration().getDisplayName()).append(" at mastery " + nextLevel);
    }

    // ---- info card

    private static float iconX, iconY;
    private static AbilityInstance iconFor;

    private static void right(Frame ui, PowersScreen s, IPowerHolder holder, float x, int y, int wide) {
        GuiGraphics g = ui.g;
        float top = y + TOP, bottom = y + BOTTOM, x0 = x + 6, x1 = x + wide, mid = x + 6 + (wide - 6) / 2f;
        Card.side(g, x - TUCK, top, x1, bottom);
        AbilityInstance ability = skillToShow(s, holder, ui);
        iconFor = ability;
        if (ability == null) {
            Ui.paraCentre(g, Component.literal("Point at a skill to see what it does"), mid, top + 60, wide - 16, Ui.FAINT, Ui.S);
            return;
        }
        float well = 26, wy = top + 7;
        iconX = mid - 8;
        iconY = wy + (well - 16) / 2f;
        boolean unlocked = ability.isUnlocked();
        Card.well(g, mid - well / 2f, wy, mid + well / 2f, wy + well, 7);
        if (unlocked) Soft.ring(g, mid - well / 2f, wy, mid + well / 2f, wy + well, 7, 1f, 0xA0FF88C8, 0x50FF88C8);
        float yy = wy + well + 4;
        Component name = ability.getConfiguration().getDisplayName();
        float nameSize = Ui.wrap(name, wide - 14, Ui.M).size() <= 2 ? Ui.M : Ui.S;
        yy = Ui.paraCentre(g, name, mid, yy, wide - 14, Ui.PINK, nameSize);
        // whether you have it, or what it takes
        int needs = masteryNeeded(ability);
        String state = unlocked ? "Unlocked" : needs > 0 ? "Mastery " + needs : "Locked";
        int stateColour = unlocked ? Ui.GOOD : Ui.FAINT;
        float stateW = Ui.width(state, Ui.XS) + (unlocked ? 8 : 12);
        float sx = mid - stateW / 2f;
        if (unlocked) Soft.disc(g, sx + 2, Ui.letterMiddle(yy, Ui.XS), 2f, Ui.GOOD);
        else Glyphs.lock(g, sx + 3, Ui.letterMiddle(yy, Ui.XS) - 0.5f, 0.7f, Ui.FAINT);
        Ui.text(g, state, sx + (unlocked ? 7 : 9), yy, stateColour, Ui.XS);
        yy += 12;
        Ui.divider(g, x0 + 4, x1 - 8, yy);
        yy += 4;
        AbilityDescription description = ability.getProperty(Ability.DESCRIPTION);
        Component text = description == null ? null : description.get(unlocked);
        if (text == null) return;
        // it wraps, picks the smaller size when that fits it all, and scrolls with the wheel if it still doesn't
        float room = bottom - 6 - yy, width = wide - 19;
        float size = Ui.wrap(text, width, Ui.S).size() * Ui.step(Ui.S) <= room ? Ui.S : Ui.XS;
        List<FormattedCharSequence> lines = Ui.wrap(text, width, size);
        float step = Ui.step(size), content = lines.size() * step;
        infoX0 = x0;
        infoY0 = yy;
        infoX1 = x1 - 3;
        infoY1 = yy + room;
        INFO.begin(ui, x0, yy, x1 - 3, yy + room, content);
        float ly = yy - INFO.offset();
        for (FormattedCharSequence line : lines) {
            Ui.line(g, line, x0 + 2, ly, Ui.TEXT, size);
            ly += step;
        }
        INFO.end(ui, Soft.mix(Ui.SIDE_TOP, Ui.SIDE_BOTTOM, 0.7f));
    }

    private static int masteryNeeded(AbilityInstance ability) {
        int level = 0;
        for (Condition c : ability.getConfiguration().getUnlockingConditions()) {
            if (c instanceof MasteryCondition m) level = Math.max(level, m.level());
        }
        return level;
    }

    public static boolean wheelInfo(double mx, double my, double delta) {
        return infoCard && infoX0 >= 0 && mx >= infoX0 && mx < infoX1 && my >= infoY0 && my < infoY1 && INFO.wheel(mx, my, delta, 12f);
    }

    @Nullable
    private static AbilityInstance skillToShow(PowersScreen s, IPowerHolder holder, Frame ui) {
        if (shownFrom != holder) {
            shownFrom = holder;
            shown = null;
            INFO.toTop();
            for (AbilityInstance a : holder.getAbilities().values()) {
                if (!a.getProperty(Ability.HIDDEN_IN_GUI)) {
                    shown = a;
                    break;
                }
            }
        }
        AbilityInstance hovered = null;
        int x = (s.width - PowerMenu.W) / 2, y = (s.height - PowerMenu.H) / 2;
        if (s.selectedTab instanceof TreePowerTab tree) {
            TreeAbilityWidget widget = tree.getAbilityHoveredOver(ui.mx - x - PowerMenu.INSIDE_X, ui.my - y - PowerMenu.INSIDE_Y, x, y);
            if (widget != null) hovered = widget.abilityInstance;
        } else if (s.selectedTab instanceof ListPowerTab) {
            hovered = ListPainter.hovered();
        }
        if (hovered != null && hovered != shown) {
            shown = hovered;
            INFO.toTop();
        }
        return shown;
    }

    // The skill's picture in the Info card goes on after the batch.
    public static void icons(Frame ui, PowersScreen s, int x, int y) {
        // while the card is still sliding out from behind the main card, so is its picture
        if (!infoCard || iconFor == null || iconX < x + PowerMenu.W) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = ui.g;
        if (iconFor.isUnlocked()) {
            IIcon icon = iconFor.getProperty(Ability.ICON);
            icon.draw(mc, g, DataContext.forAbility(mc.player, iconFor), Math.round(iconX), Math.round(iconY));
            g.flush();
        } else {
            g.drawManaged(() -> Glyphs.lock(g, iconX + 8, iconY + 8, 1.3f, Ui.SOFT));
        }
    }
}
