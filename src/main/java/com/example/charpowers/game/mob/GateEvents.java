package com.example.charpowers.game.mob;

import com.example.charpowers.CharPowers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Rules that cut across the gate creatures: nothing they throw hurts another of them, and a curse halves healing.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class GateEvents {
    private GateEvents() {}

    private static boolean ours(Entity e) {
        return e instanceof GateMob || e instanceof GateBoss;
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent e) {
        if (!(e.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)) return;
        if (ours(e.getProjectile().getOwner()) && ours(hit.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent e) {
        if (e.getEntity() instanceof Player p && p.getPersistentData().getLong(GateMob.CURSE) > p.level().getGameTime()) e.setAmount(e.getAmount() * 0.5f);
    }
}
