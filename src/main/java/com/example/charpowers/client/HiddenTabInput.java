package com.example.charpowers.client;

import com.example.charpowers.CharPowers;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.client.screen.power.PowersScreen;

import javax.annotation.Nullable;

// Palladium's power menu tabs only get mouse clicks. The hidden powers tab also needs typing (search box) and the
// mouse wheel (scrolling the list), so they're passed on here while our tab is the open one.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class HiddenTabInput {
    @Nullable
    private static HiddenPowersTab tab(Screen s) {
        return s instanceof PowersScreen ps && ps.selectedTab instanceof HiddenPowersTab t ? t : null;
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre e) {
        HiddenPowersTab t = tab(e.getScreen());
        if (t != null && t.keyPressed(e.getKeyCode(), e.getScanCode(), e.getModifiers())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onChar(ScreenEvent.CharacterTyped.Pre e) {
        HiddenPowersTab t = tab(e.getScreen());
        if (t != null && t.charTyped(e.getCodePoint(), e.getModifiers())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        HiddenPowersTab t = tab(e.getScreen());
        if (t != null && t.mouseScrolled(e.getMouseX(), e.getMouseY(), e.getScrollDelta())) e.setCanceled(true);
    }

    private HiddenTabInput() {}
}
