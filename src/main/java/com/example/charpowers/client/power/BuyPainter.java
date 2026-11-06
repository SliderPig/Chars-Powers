package com.example.charpowers.client.power;

import com.example.charpowers.client.Soft;
import com.example.charpowers.client.ui.Anims;
import com.example.charpowers.client.ui.Card;
import com.example.charpowers.client.ui.PillButton;
import com.example.charpowers.client.ui.Tooltip;
import com.example.charpowers.client.ui.Ui;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.threetag.palladium.client.screen.power.BuyAbilityScreen;
import net.threetag.palladium.util.context.DataContext;

import java.util.ArrayList;
import java.util.List;

// Palladium's "buy this skill?" box, painted in the family look. Its real buttons stay underneath, so what they do doesn't change.
public final class BuyPainter {
    private static final Anims ANIMS = new Anims();

    private BuyPainter() {}

    public static void draw(BuyAbilityScreen screen, Button confirm, Component text, GuiGraphics g, int mx, int my) {
        ANIMS.frame();
        Minecraft mc = Minecraft.getInstance();
        int x = (screen.width - 202) / 2, y = (screen.height - 60) / 2;
        List<AbstractButton> buttons = new ArrayList<>();
        for (GuiEventListener child : screen.children()) if (child instanceof AbstractButton b) buttons.add(b);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        boolean[] overConfirm = new boolean[1];
        g.drawManaged(() -> {
            Card.main(g, x, y, x + 202, y + 60, 12, 0xFF3A2C47, 0xFF281E31);
            Ui.paraCentre(g, text, x + 101, y + 8, 180, Ui.TEXT, Ui.M);
            for (AbstractButton b : buttons) {
                boolean over = mx >= b.getX() && mx < b.getX() + b.getWidth() && my >= b.getY() && my < b.getY() + b.getHeight();
                float lift = ANIMS.to(b, over && b.active ? 1f : 0f, 16f);
                if (b == confirm) {
                    overConfirm[0] = over;
                    PillButton.body(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), PillButton.Kind.PRIMARY, b.active, lift);
                    String amount = screen.unlockData.amount + "x";
                    float w = Ui.width(amount, Ui.M) + 18, tx = b.getX() + (b.getWidth() - w) / 2f;
                    Ui.text(g, amount, tx, Ui.middle(b.getY() + b.getHeight() / 2f, Ui.M), b.active ? Ui.ON_PINK : Ui.FAINT, Ui.M);
                } else if (b.getWidth() <= 8) {
                    float cx = b.getX() + b.getWidth() / 2f, cy = b.getY() + b.getHeight() / 2f;
                    if (lift > 0.01f) Soft.disc(g, cx, cy, 5.5f, Soft.alpha(0x40FFFFFF, lift));
                    Glyphs.cross(g, cx, cy, 2.2f, 1.2f, Soft.mix(Ui.SOFT, Ui.TEXT, lift));
                } else {
                    PillButton.paint(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.getMessage().getString(), PillButton.Kind.NORMAL, b.active, lift);
                }
            }
        });
        if (confirm != null) {
            String amount = screen.unlockData.amount + "x";
            float w = Ui.width(amount, Ui.M) + 18, ix = confirm.getX() + (confirm.getWidth() - w) / 2f + w - 16;
            screen.unlockData.icon.draw(mc, g, DataContext.forEntity(mc.player), Math.round(ix), confirm.getY() + (confirm.getHeight() - 16) / 2);
            g.flush();
            if (overConfirm[0]) {
                Component tip = Component.literal(screen.unlockData.amount + "x ").append(screen.unlockData.description);
                Tooltip.draw(g, List.of(tip), mx, my, 1f);
            }
        }
    }
}
