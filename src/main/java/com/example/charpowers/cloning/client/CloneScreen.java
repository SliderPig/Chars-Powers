package com.example.charpowers.cloning.client;

import com.example.charpowers.Net;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.PillButton;
import com.example.charpowers.client.ui.Tabs;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.client.ui.UiScreen;
import com.example.charpowers.cloning.jobs.Jobs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

// The menu you get from crouch + right-clicking your clone.
public class CloneScreen extends UiScreen {
    private static final int W = 230;
    private final int slot;
    private final String job;
    private final float health;
    private final float maxHealth;
    private final int food;
    private final String chest;

    CloneScreen(int slot, String name, String job, float health, float maxHealth, int food, String chest) {
        super(Component.literal(name));
        this.slot = slot;
        this.job = job;
        this.health = health;
        this.maxHealth = maxHealth;
        this.food = food;
        this.chest = chest;
    }

    private void order(String action) {
        Net.CHANNEL.sendToServer(new Net.CloneOrder(slot, action));
        onClose();
    }

    @Override
    protected void paint(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float w = Math.min(W, width - 16), inner = w - 20;
        float h = 56 + 19 + 2 + 19 + 21 + 6, x0 = (width - w) / 2f, y0 = (height - h) / 2f;
        Card.main(g, x0, y0, x0 + w, y0 + h);
        float x = x0 + 10, y = y0 + 7;
        Ui.text(g, title.getString(), x, y, Ui.PINK, Ui.fit(title, inner, Ui.L, Ui.M, Ui.S));
        y += 16;
        Ui.text(g, "Job: " + job, x, y, Ui.TEXT, Ui.S);
        y += 11;
        Ui.text(g, "Health " + Math.round(health) + "/" + Math.round(maxHealth) + "   Food " + food + "/20", x, y, Ui.TEXT, Ui.S);
        y += 11;
        String where = "Chest: " + chest;
        Ui.text(g, where, x, y, Ui.TEXT, Ui.fit(where, inner, Ui.S, Ui.XS));
        y += 15;
        List<String> labels = new ArrayList<>();
        for (String name : Jobs.NAMES) labels.add(name.substring(0, 1).toUpperCase() + name.substring(1));
        Tabs.draw(ui, "jobs", x, y, inner, 16, labels, Jobs.NAMES.indexOf(job), i -> {
            if (!Jobs.NAMES.get(i).equals(job)) order("job:" + Jobs.NAMES.get(i));
        });
        y += 21;
        float half = (inner - 3) / 2f;
        PillButton.draw(ui, "mark", x, y, half, 16, "Mark chest", PillButton.Kind.NORMAL, true, () -> order("mark"));
        PillButton.draw(ui, "dismiss", x + half + 3, y, half, 16, "Dismiss", PillButton.Kind.NORMAL, true, () -> order("dismiss"));
        y += 19;
        PillButton.draw(ui, "close", x, y, inner, 16, "Close", PillButton.Kind.GHOST, true, this::onClose);
    }
}
