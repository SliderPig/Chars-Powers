package com.example.charpowers.client.ui;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.Soft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// The top-left corner of the screen: the Suit bar, the ring's bar and the hidden power lines each take the next free spot, so they never sit on each other.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class HudStack {
    private static final float TOP = 6, GAP = 3;
    private static float next = TOP;

    private HudStack() {}

    @SubscribeEvent
    public static void onStart(RenderGuiEvent.Pre e) {
        next = TOP;
    }

    // They only clutter the view under a menu. Chat is the one screen you want them with.
    public static boolean menuOpen() {
        var screen = net.minecraft.client.Minecraft.getInstance().screen;
        return screen != null && !(screen instanceof net.minecraft.client.gui.screens.ChatScreen);
    }

    // Reserves a strip this tall and says where its top is.
    public static float take(float height) {
        float y = next;
        next += height + GAP;
        return y;
    }

    // A soft floating pill for HUD things.
    public static void chip(GuiGraphics g, float x, float y, float w, float h, int colour) {
        float r = Math.min(9f, h / 2f);
        Soft.shadow(g, x, y + 1, x + w, y + h + 1, r, 4, 0x50000000, false);
        Soft.round(g, x, y, x + w, y + h, r, Soft.mix(colour, 0xFFFFFFFF, 0.07f), colour);
        Soft.ring(g, x, y, x + w, y + h, r, 1f, 0x28FFFFFF, 0x10FFFFFF);
    }

    // A slim bar, part full.
    public static void bar(GuiGraphics g, float x, float y, float w, float h, float part, int colour) {
        Soft.round(g, x, y, x + w, y + h, h / 2f, 0x60000000);
        float fill = w * Math.max(0f, Math.min(1f, part));
        if (fill > 0.5f) Soft.round(g, x, y, x + Math.max(fill, h), y + h, h / 2f, Soft.mix(colour, 0xFFFFFFFF, 0.15f), colour);
    }
}
