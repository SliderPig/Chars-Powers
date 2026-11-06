package com.example.charpowers.timecontrol.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.client.PalladiumKeyMappings;
import net.threetag.palladium.power.ability.AbilityUtil;

// Gotta move: hold an ability key and scroll to set your speed. The wheel goes back to the hotbar when you let go.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class TimeClient {
    private TimeClient() {}

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !anyAbilityKeyDown()) return;
        if (!AbilityUtil.isEnabled(mc.player, Mastery.TIME_CONTROL, "gotta_move")) return;
        Net.CHANNEL.sendToServer(new Net.OwnToServer(Mastery.TIME_CONTROL.toString(), e.getScrollDelta() > 0 ? "speed+" : "speed-"));
        e.setCanceled(true);
    }

    private static boolean anyAbilityKeyDown() {
        PalladiumKeyMappings.AbilityKeyMapping[] keys = PalladiumKeyMappings.ABILITY_KEYS;
        if (keys == null) return false;
        for (PalladiumKeyMappings.AbilityKeyMapping key : keys) {
            if (key != null && key.isDown()) return true;
        }
        return false;
    }
}
