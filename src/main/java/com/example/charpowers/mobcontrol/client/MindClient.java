package com.example.charpowers.mobcontrol.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.client.OwnClient;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Swap minds on the PC: your view moves to the mob, your own body stands still, and your movement keys go to the mob.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class MindClient {
    private static boolean swapped;

    static {
        OwnClient.on(Mastery.MOB_CONTROL.toString(), MindClient::receive);
    }

    private MindClient() {}

    private static void receive(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !tag.contains("swap")) return;
        Entity mob = mc.level.getEntity(tag.getInt("swap"));
        swapped = mob != null;
        mc.setCameraEntity(mob != null ? mob : mc.player);
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (!swapped) return;
        Input in = e.getInput();
        Net.CHANNEL.sendToServer(new Net.OwnToServer(Mastery.MOB_CONTROL.toString(), "steer:" + in.forwardImpulse + ":" + in.leftImpulse + ":" + (in.jumping ? "1" : "0")));
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }
}
