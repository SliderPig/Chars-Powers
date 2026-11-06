package com.example.charpowers.game.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.Soft;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.mixin.SlotAccessor;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The Player's own inventory: E opens this instead of the normal one. They're the real slots, so nothing can go wrong with your items,
// just arranged on a see-through hologram over the running game. Pages: Inventory, Equipment (your figure, with lines to what you're
// wearing), and Storage, a private stash nobody else can reach.
public class HoloScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    public static final String STORAGE_KEY = "container.charpowers.storage";
    private static final ResourceLocation FRAME = new ResourceLocation(CharPowers.MODID, "textures/gui/status_window.png");
    private static final int W = 264, H = 232, GRID_X = (W - 162) / 2 + 1, CELL = 18, OFF = -10000;
    private static final int TEXT = 0xFFEAF8FF, SOFT = 0xFF8FC6E4, ACCENT = 0xFF7FE3FF, GREEN = 0xFF6BFF7A, EDGE = 0xC07FE3FF, ON = 0xF0DDF5FF, ON_TEXT = 0xFF061A30;
    private static final int HEAD = 5, CHEST = 6, LEGS = 7, FEET = 8, OFF_HAND = 45, CRAFT_Y = 58;

    private enum Page { INVENTORY, EQUIPMENT, STORAGE }

    private record Hit(int x, int y, int w, int h, Runnable what) {}

    private static Page last = Page.INVENTORY;

    private final List<Hit> hits = new ArrayList<>();
    private final int[] homeX, homeY;
    private final boolean inventory;
    private Page page;
    private double gui = 2;
    private int realX, realY;
    private GuiGraphics g;

    public HoloScreen(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = W;
        imageHeight = H;
        slotColor = 0x5535C6FF;
        inventory = menu instanceof InventoryMenu;
        page = inventory ? (last == Page.STORAGE ? Page.INVENTORY : last) : Page.STORAGE;
        homeX = new int[menu.slots.size()];
        homeY = new int[menu.slots.size()];
        for (int i = 0; i < homeX.length; i++) {
            homeX[i] = menu.slots.get(i).x;
            homeY[i] = menu.slots.get(i).y;
        }
    }

    private void place(int index, int x, int y) {
        Slot slot = menu.slots.get(index);
        ((SlotAccessor) slot).charpowers$setX(x);
        ((SlotAccessor) slot).charpowers$setY(y);
    }

    private void row(int from, int count, int y) {
        for (int i = 0; i < count; i++) place(from + i, GRID_X + i * CELL, y);
    }

    private void layout() {
        for (int i = 0; i < menu.slots.size(); i++) place(i, OFF, OFF);
        switch (page) {
            case INVENTORY -> {
                place(1, GRID_X + 18, CRAFT_Y);
                place(2, GRID_X + 36, CRAFT_Y);
                place(3, GRID_X + 18, CRAFT_Y + CELL);
                place(4, GRID_X + 36, CRAFT_Y + CELL);
                place(0, GRID_X + 90, CRAFT_Y + 9);
                place(OFF_HAND, GRID_X + 144, CRAFT_Y + 9);
                for (int r = 0; r < 3; r++) row(9 + r * 9, 9, CRAFT_Y + 52 + r * CELL);
                row(36, 9, CRAFT_Y + 52 + 3 * CELL + 8);
            }
            case EQUIPMENT -> {
                place(HEAD, 22, 48);
                place(CHEST, 22, 92);
                place(OFF_HAND, 22, 136);
                place(LEGS, W - 38, 48);
                place(FEET, W - 38, 92);
                row(36, 9, 200);
            }
            case STORAGE -> {
                for (int r = 0; r < 6; r++) row(r * 9, 9, 36 + r * CELL);
                for (int r = 0; r < 3; r++) row(54 + r * 9, 9, 146 + r * CELL);
                row(81, 9, 202);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        layout();
    }

    // The normal screens need the slots back where they were.
    @Override
    public void removed() {
        for (int i = 0; i < homeX.length; i++) place(i, homeX[i], homeY[i]);
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    // ---- drawing, in real screen pixels: thin lines stay thin at any GUI scale

    private int R(double guiUnits) {
        return (int) Math.round(guiUnits * gui);
    }

    private void text(String s, double x, double y, double size, int colour, int align, boolean glow) {
        Component c = Component.literal(s);
        float k = com.example.charpowers.client.ui.Ui.snapRealScale((float) (size * gui));
        float px = R(x) - (align == 0 ? font.width(c) * k / 2 : align > 0 ? font.width(c) * k : 0);
        g.pose().pushPose();
        g.pose().translate(px, R(y), 0);
        g.pose().scale(k, k, 1);
        if (glow) {
            int soft = 0x40000000 | (ACCENT & 0xFFFFFF);
            float step = 1.5f / k;
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

    private void pill(double x, double y, double w, double h, String label, boolean selected, Runnable what) {
        int x0 = R(x), y0 = R(y), x1 = R(x + w), y1 = R(y + h), r = (y1 - y0) / 2;
        boolean over = realX >= x0 && realX < x1 && realY >= y0 && realY < y1;
        Soft.plate(g, x0, y0, x1, y1, r, selected ? ON : over ? 0xFF1E6A9A : 0xFF134668, Math.max(1, (int) Math.round(gui / 2)));
        text(label, x + w / 2, y + (h - 8) / 2, 1, selected ? ON_TEXT : TEXT, 0, false);
        hits.add(new Hit(x0, y0, x1 - x0, y1 - y0, what));
    }

    // A thin line at any angle, one real pixel at a time.
    private void line(double x0, double y0, double x1, double y1, int colour) {
        int ax = R(x0), ay = R(y0), bx = R(x1), by = R(y1);
        int steps = Math.max(Math.abs(bx - ax), Math.abs(by - ay));
        for (int i = 0; i <= steps; i++) {
            int x = ax + (bx - ax) * i / Math.max(1, steps), y = ay + (by - ay) * i / Math.max(1, steps);
            g.fill(x, y, x + 1, y + 1, colour);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        g = graphics;
        hits.clear();
        Minecraft mc = Minecraft.getInstance();
        gui = mc.getWindow().getGuiScale();
        realX = (int) (mouseX * gui);
        realY = (int) (mouseY * gui);
        RenderSystem.enableBlend();
        mc.getTextureManager().getTexture(FRAME).setFilter(true, false);
        g.blit(FRAME, leftPos, topPos, W, H, 0f, 0f, 560, 430, 560, 430);
        g.pose().pushPose();
        g.pose().scale((float) (1 / gui), (float) (1 / gui), 1);
        g.pose().translate(R(leftPos), R(topPos), 0);
        realX -= R(leftPos);
        realY -= R(topPos);
        g.drawManaged(this::paint);
        g.pose().popPose();
        if (page == Page.EQUIPMENT && mc.player != null) {
            int cx = leftPos + W / 2, base = topPos + 182;
            RenderSystem.setShaderColor(0.62f, 0.9f, 1f, 1f);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, cx, base, 62, cx - mouseX, base - 90 - mouseY, mc.player);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    private void paint() {
        text(page.name(), W / 2.0, 6, 1, 0xFFFFFFFF, 0, true);
        double tabW = 58;
        double tabX = (W - 4 * tabW - 3 * 3) / 2.0;
        pill(tabX, 20, tabW, 12, "INVENTORY", page == Page.INVENTORY, () -> go(Page.INVENTORY));
        pill(tabX + tabW + 3, 20, tabW, 12, "EQUIPMENT", page == Page.EQUIPMENT, () -> go(Page.EQUIPMENT));
        pill(tabX + 2 * (tabW + 3), 20, tabW, 12, "STORAGE", page == Page.STORAGE, () -> go(Page.STORAGE));
        pill(tabX + 3 * (tabW + 3), 20, tabW, 12, "STATUS", false, this::toStatus);
        for (Slot slot : menu.slots) {
            if (slot.x == OFF) continue;
            int x0 = R(slot.x - 1), y0 = R(slot.y - 1), size = R(CELL) - 2;
            Soft.well(g, x0 + 1, y0 + 1, x0 + 1 + size, y0 + 1 + size, R(3), 0xB00A2036, Math.max(2, (int) Math.round(gui)));
        }
        if (page == Page.INVENTORY) {
            text("CRAFT", GRID_X + 14, CRAFT_Y + 14, 1, SOFT, 1, false);
            text(">>", GRID_X + 72, CRAFT_Y + 14, 1, ACCENT, 0, true);
            text("OFF HAND", GRID_X + 152, CRAFT_Y - 3, 1, SOFT, 0, false);
        } else if (page == Page.EQUIPMENT) {
            equipmentLabels();
        }
    }

    private void equipmentLabels() {
        double cx = W / 2.0;
        label(HEAD, EquipmentSlot.HEAD, "HEAD", true, cx - 8, 70);
        label(CHEST, EquipmentSlot.CHEST, "BODY", true, cx - 14, 100);
        label(OFF_HAND, EquipmentSlot.OFFHAND, "OFF HAND", true, cx - 20, 118);
        label(LEGS, EquipmentSlot.LEGS, "LEGS", false, cx + 8, 136);
        label(FEET, EquipmentSlot.FEET, "FEET", false, cx + 8, 168);
    }

    // What's in a slot and what it gives, with a line from it to that part of your figure.
    private void label(int index, EquipmentSlot where, String empty, boolean left, double toX, double toY) {
        Slot slot = menu.slots.get(index);
        ItemStack stack = slot.getItem();
        double top = slot.y - 3, edge = left ? slot.x + 17 : slot.x - 1, elbow = left ? W / 2.0 - 34 : W / 2.0 + 34;
        line(edge, top, elbow, top, EDGE);
        line(elbow, top, toX, toY, EDGE);
        double textX = left ? slot.x + 20 : slot.x - 4;
        int align = left ? -1 : 1;
        if (stack.isEmpty()) {
            text(empty, textX, top + 3, 1, SOFT, align, false);
            return;
        }
        String name = font.plainSubstrByWidth(stack.getHoverName().getString(), 90);
        text(name, textX, top + 3, 1, TEXT, align, false);
        double y = top + 13;
        Multimap<Attribute, AttributeModifier> gives = stack.getAttributeModifiers(where);
        int shown = 0;
        for (Map.Entry<Attribute, AttributeModifier> e : gives.entries()) {
            if (shown++ == 2) break;
            double amount = e.getValue().getAmount();
            String what = Component.translatable(e.getKey().getDescriptionId()).getString();
            text((amount >= 0 ? "+" : "") + String.format("%.1f ", amount) + font.plainSubstrByWidth(what, 40), textX, y, 1, GREEN, align, false);
            y += 9;
        }
    }

    // ---- moving between pages

    private void go(Page next) {
        if (next == page) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !menu.getCarried().isEmpty()) return;
        last = next;
        if (next == Page.STORAGE) {
            GameScreen.send("storage");
        } else if (inventory) {
            page = next;
            layout();
        } else {
            p.closeContainer();
            mc.setScreen(new HoloScreen(p.inventoryMenu, p.getInventory(), Component.literal("Inventory")));
        }
    }

    private void toStatus() {
        if (!menu.getCarried().isEmpty()) return;
        onClose();
        GameScreen.send("open");
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int rx = (int) (mx * gui) - R(leftPos), ry = (int) (my * gui) - R(topPos);
            for (Hit h : new ArrayList<>(hits)) {
                if (rx >= h.x() && rx < h.x() + h.w() && ry >= h.y() && ry < h.y() + h.h()) {
                    h.what().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (GameScreen.statusKey().matches(key, scan)) {
            toStatus();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
    public static final class Swap {
        private Swap() {}

        // Only for the Player, and not if they've chosen the plain Minecraft look.
        @SubscribeEvent
        public static void onOpening(ScreenEvent.Opening e) {
            LocalPlayer p = Minecraft.getInstance().player;
            Screen next = e.getNewScreen();
            if (p == null || !(next instanceof InventoryScreen || next instanceof ContainerScreen) || !AbilityUtil.hasPower(p, Mastery.GAME_PLAYER)) return;
            if (next instanceof InventoryScreen screen) {
                if (!GameScreen.plainLook()) e.setNewScreen(new HoloScreen(screen.getMenu(), p.getInventory(), next.getTitle()));
            } else if (next instanceof ContainerScreen screen && next.getTitle().getContents() instanceof TranslatableContents title && STORAGE_KEY.equals(title.getKey())) {
                e.setNewScreen(new HoloScreen(screen.getMenu(), p.getInventory(), next.getTitle()));
            }
        }

        @SubscribeEvent
        public static void onTooltip(RenderTooltipEvent.Color e) {
            Screen open = Minecraft.getInstance().screen;
            if (open instanceof HoloScreen || open instanceof GameScreen) {
                e.setBackground(0xF0061726);
                e.setBorderStart(0xFF7FE3FF);
                e.setBorderEnd(0x807FE3FF);
            }
        }
    }
}
