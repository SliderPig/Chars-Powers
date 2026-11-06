package com.example.charpowers.sound;

import com.example.charpowers.CharPowers;
import com.example.charpowers.game.client.GameScreen;
import com.example.charpowers.game.client.HoloScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Game Player's windows bleep open and shut. Only when you go from no window to a window or back, so moving between
// the Status Window and the hologram inventory stays quiet.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class HoloSounds {
    private static boolean wasOpen;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        boolean open = mc.screen instanceof GameScreen || mc.screen instanceof HoloScreen;
        if (open == wasOpen) return;
        wasOpen = open;
        mc.getSoundManager().play(SimpleSoundInstance.forUI((open ? PowerSounds.HOLO_OPEN : PowerSounds.HOLO_CLOSE).get(), 1f, 0.45f));
    }

    private HoloSounds() {}
}
