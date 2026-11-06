package com.example.charpowers.authority.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.authority.Orders;
import com.example.charpowers.authority.model.AuthorityRules;
import com.example.charpowers.client.OwnClient;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// When someone's command reaches you, it takes your movement keys on your own PC. It wears off by itself.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class ObeyClient {
    private static final int LASTS_TICKS = 60;

    static {
        OwnClient.on(Mastery.SUPERIOR_AUTHORITY.toString(), ObeyClient::receive);
    }

    private ObeyClient() {}

    private static void receive(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Orders.clientMask = tag.getInt("mask");
        Orders.clientUntil = mc.level.getGameTime() + LASTS_TICKS;
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        int mask = Orders.clientMask;
        if (mask == 0) return;
        if (e.getEntity().level().getGameTime() >= Orders.clientUntil) {
            Orders.clientMask = 0;
            return;
        }
        Input in = e.getInput();
        if ((mask & AuthorityRules.FREEZE) != 0) {
            in.forwardImpulse = 0;
            in.leftImpulse = 0;
            in.up = in.down = in.left = in.right = false;
            in.jumping = false;
            in.shiftKeyDown = false;
        }
        if ((mask & AuthorityRules.NO_JUMP) != 0) in.jumping = false;
        if ((mask & AuthorityRules.KNEEL) != 0) in.shiftKeyDown = true;
        if ((mask & AuthorityRules.ATTACK) != 0) {
            in.forwardImpulse = 1;
            in.up = true;
            in.jumping = e.getEntity().horizontalCollision;
        }
    }
}
