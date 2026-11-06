package com.example.charpowers.armour.client;

import com.example.charpowers.CharPowers;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// A very slight shake of the view when the last piece of the suit closes around you.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class SuitShake {
    private static final int LENGTH = 10;
    private static int left;

    private SuitShake() {}

    public static void start() {
        left = LENGTH;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && left > 0 && !Minecraft.getInstance().isPaused()) left--;
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (left <= 0) return;
        float k = left / (float) LENGTH;
        float t = (LENGTH - left + (float) e.getPartialTick()) * 2.6f;
        e.setRoll(e.getRoll() + (float) Math.sin(t) * 1.4f * k);
        e.setPitch(e.getPitch() + (float) Math.cos(t * 1.3) * 0.6f * k);
        e.setYaw(e.getYaw() + (float) Math.sin(t * 0.9) * 0.4f * k);
    }
}
