package com.example.charpowers.client.power;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.PowerPanels;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.client.screen.power.PowersScreen;
import net.threetag.palladium.client.screen.power.TreePowerTab;

// The wheel in the power menu: over the tabs it slides them along, over the Info card it scrolls the words, over a skill tree it moves the tree.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class PowerMenuInput {
    private PowerMenuInput() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        if (!(e.getScreen() instanceof PowersScreen s) || s.overlayScreen != null) return;
        double mx = e.getMouseX(), my = e.getMouseY(), delta = e.getScrollDelta();
        if (PowerMenu.overRail(s, mx, my)) {
            PowerMenu.railScroll(s, delta);
            e.setCanceled(true);
        } else if (PowerPanels.wheelInfo(mx, my, delta)) {
            e.setCanceled(true);
        } else if (s.selectedTab instanceof TreePowerTab tree) {
            int x = (s.width - PowerMenu.W) / 2 + PowerMenu.INSIDE_X, y = (s.height - PowerMenu.H) / 2 + PowerMenu.INSIDE_Y;
            if (mx >= x && mx < x + PowerMenu.INSIDE_W && my >= y && my < y + PowerMenu.INSIDE_H) {
                tree.scroll(0, delta * 16);
                e.setCanceled(true);
            }
        }
    }
}
