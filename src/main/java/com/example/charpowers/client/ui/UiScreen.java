package com.example.charpowers.client.ui;

import com.example.charpowers.client.Soft;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// The family's base menu: fades in, paints everything in one batch (so it stays fast), and takes clicks from whatever paint registered.
public abstract class UiScreen extends Screen {
    protected final Frame ui = new Frame();
    private long opened = -1;

    protected UiScreen(Component title) {
        super(title);
    }

    // Paint here, in GUI units. Items are fine (g.renderItem). Before a blit, call g.flush().
    protected abstract void paint(GuiGraphics g, int mx, int my, float partial);

    // How far the opening has got, 0 to 1.
    protected float openness() {
        return Soft.easeOut((Util.getMillis() - opened) / 200f);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opened < 0) opened = Util.getMillis();
        renderBackground(g);
        ui.begin(g, mx, my, partial);
        float open = openness();
        g.pose().pushPose();
        if (open < 1f) {
            g.setColor(1f, 1f, 1f, Math.max(0.05f, open));
            g.pose().translate(0f, (1f - open) * 6f, 0f);
        }
        g.drawManaged(() -> paint(g, mx, my, partial));
        g.setColor(1f, 1f, 1f, 1f);
        g.pose().popPose();
        ui.end();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && ui.click(mx, my)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
