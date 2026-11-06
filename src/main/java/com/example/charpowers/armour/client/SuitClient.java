package com.example.charpowers.armour.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.SuitItems;
import com.example.charpowers.client.ui.HudStack;
import com.example.charpowers.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

// While you're in the suit: its battery, and which modules are in it, in the corner of the screen.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class SuitClient {
    private static final int PANEL = 0xFF23282D, SILVER = 0xFFB8C2CA, LIME = 0xFF96FF1E, RED = 0xFFFF5A5A, TEXT = 0xFFF4F8FA;

    private SuitClient() {}

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || HudStack.menuOpen() || !SuitItems.fullSuit(mc.player) || SuitVisor.active()) return;
        ItemStack suit = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        double battery = SuitItems.battery(suit);
        GuiGraphics g = e.getGuiGraphics();
        float w = 92, h = 22, x = 6, y = HudStack.take(h);
        List<String> modules = SuitItems.modulesOn(suit);
        g.drawManaged(() -> {
            HudStack.chip(g, x, y, w, h, PANEL);
            Ui.text(g, "SUIT", x + 10, y + 4, SILVER, Ui.XS);
            Ui.right(g, Math.round(battery) + "%", x + w - 10, y + 4, TEXT, Ui.XS);
            HudStack.bar(g, x + 10, y + 14, w - 20, 3, (float) (battery / 100), battery <= 15 ? RED : LIME);
            // the modules in it, each a little round button
            float d = 13, mx = x + w + 3;
            for (String m : modules) {
                HudStack.chip(g, mx, y, d, d, PANEL);
                Ui.centre(g, m.substring(0, 1).toUpperCase(), mx + d / 2f, Ui.middle(y + d / 2f, Ui.XS), LIME, Ui.XS);
                mx += d + 2;
            }
        });
    }
}
