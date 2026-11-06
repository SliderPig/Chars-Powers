package com.example.charpowers.book.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.client.OwnClient;
import com.example.charpowers.client.Soft;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.PillButton;
import com.example.charpowers.client.ui.ScrollList;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.client.ui.UiScreen;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

// The book: a row for each page you have, Write and Restore on each, and below them the deaths it's holding.
// Anything that can't be taken back asks once more first.
public class BookScreen extends UiScreen {
    private static final int MOST_W = 320, MARGIN = 8;

    private CompoundTag state;
    private String asking = "";
    private final ScrollList list = new ScrollList();
    private float contentH = 100;

    private BookScreen(CompoundTag state) {
        super(Component.literal("The Book"));
        this.state = state;
    }

    private static void receive(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BookScreen open) {
            open.state = tag;
            open.asking = "";
        } else if (mc.screen == null) {
            mc.setScreen(new BookScreen(tag));
        }
    }

    private void press(String action, boolean ask) {
        if (ask && !asking.equals(action)) {
            asking = action;
            return;
        }
        Net.CHANNEL.sendToServer(new Net.OwnToServer(Mastery.BOOK_KEEPING.toString(), action));
    }

    @Override
    protected void paint(GuiGraphics g, int mx, int my, float partial) {
        float w = Math.min(MOST_W, width - MARGIN * 2), wanted = 23 + contentH + 8 + 26;
        float h = Math.min(wanted, height - MARGIN * 2);
        float x0 = (width - w) / 2f, y0 = (height - h) / 2f, x1 = x0 + w, y1 = y0 + h;
        Card.main(g, x0, y0, x1, y1);
        Ui.text(g, "The Book", x0 + 12, Ui.middle(y0 + 12, Ui.L), Ui.PINK, Ui.L);
        float top = y0 + 23, bottom = y1 - 28;
        Card.well(g, x0 + 8, top, x1 - 8, bottom, Ui.R_INNER);
        list.begin(ui, x0 + 8, top + 1, x1 - 8, bottom - 1, contentH);
        float inner0 = x0 + 13, inner1 = x1 - 13 - (list.needsBar() ? ScrollList.barWidth() : 0);
        float start = top + 5 - list.offset();
        contentH = rows(g, inner0, inner1, start) - start + 8;
        list.end(ui, Ui.WELL_TOP, Ui.WELL_BOTTOM);
        PillButton.draw(ui, "close", (x0 + x1) / 2f - 40, y1 - 23, 80, 16, "Close", PillButton.Kind.NORMAL, true, this::onClose);
    }

    private float rows(GuiGraphics g, float x0, float x1, float y) {
        int max = state.getInt("max");
        ListTag pages = state.getList("pages", Tag.TAG_COMPOUND);
        boolean busy = state.getBoolean("busy");
        for (int i = 0; i < max; i++) {
            CompoundTag page = pages.getCompound(i);
            boolean written = page.contains("where");
            float h = 34;
            Card.row(g, x0, y, x1, y + h, Ui.R_ROW, 0f, false);
            float room = x1 - x0 - 16;
            Ui.text(g, "Page " + (i + 1), x0 + 8, y + 4, Ui.TEXT, Ui.M);
            String sub = !written ? "blank" : page.getBoolean("short") ? "too old to undo it all" : page.getInt("changes") + " to undo";
            Ui.text(g, sub, x0 + 8 + Ui.width("Page " + (i + 1), Ui.M) + 6, y + 5, Ui.FAINT, Ui.XS);
            if (written) {
                String where = page.getString("where");
                Ui.text(g, where, x0 + 8, y + 18, Ui.SOFT, Ui.fit(where, room - 114, Ui.S, Ui.XS));
            }
            int index = i;
            boolean askWrite = asking.equals("write:" + i), askRestore = asking.equals("restore:" + i);
            float bh = 16, by = y + (h - bh) / 2f;
            PillButton.draw(ui, "w" + i, x1 - 112, by, 50, bh, askWrite ? "Sure?" : "Write", askWrite ? PillButton.Kind.PRIMARY : PillButton.Kind.NORMAL, !busy, () -> press("write:" + index, written));
            PillButton.draw(ui, "r" + i, x1 - 59, by, 54, bh, askRestore ? "Sure?" : "Restore", askRestore ? PillButton.Kind.PRIMARY : PillButton.Kind.NORMAL, written && !busy, () -> press("restore:" + index, true));
            y += h + 3;
        }
        y += 4;
        ListTag deaths = state.getList("deaths", Tag.TAG_COMPOUND);
        Component head = Component.literal(deaths.isEmpty() ? "No deaths held." : "Held deaths: walk within " + (int) state.getDouble("range") + " blocks to get them back");
        y = Ui.para(g, head, x0 + 2, y, x1 - x0 - 4, deaths.isEmpty() ? Ui.FAINT : Ui.SOFT, Ui.S, -1) + 2;
        for (int i = 0; i < deaths.size(); i++) {
            CompoundTag death = deaths.getCompound(i);
            Soft.disc(g, x0 + 6, Ui.letterMiddle(y, Ui.S), 1.8f, Ui.PINK);
            y = Ui.para(g, Component.literal(death.getString("where") + " (" + death.getInt("items") + " items)"), x0 + 12, y, x1 - x0 - 14, Ui.TEXT, Ui.S, -1);
        }
        return y;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return list.wheel(mx, my, delta, 20) || super.mouseScrolled(mx, my, delta);
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent e) {
            OwnClient.on(Mastery.BOOK_KEEPING.toString(), BookScreen::receive);
        }
    }
}
