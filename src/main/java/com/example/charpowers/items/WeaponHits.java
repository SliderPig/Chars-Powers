package com.example.charpowers.items;

import com.example.charpowers.CharPowers;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

// Hits that land when the swing gets there instead of the moment you click. The PC starts the move, the server waits the move's own number
// of ticks and then makes the normal attack, so enchantments, the weapons' own effects, teams and protections all work as usual.
// Also runs small things weapons do a moment later (the Ice Staff's spin cast).
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class WeaponHits {
    private static final int MOST_WAIT = 14;
    private static final List<Pending> PENDING = new ArrayList<>();

    private record Pending(UUID who, int target, long at, WeaponStyle style, Runnable later) {}

    private WeaponHits() {}

    public static void request(ServerPlayer p, int target, int step) {
        if (p.isSpectator()) return;
        WeaponStyle style = WeaponStyles.of(p.getMainHandItem().getItem());
        if (!WeaponStyles.timedHits(style)) return;
        // one blow on its way at a time: clicking faster than the weapon swings does nothing more
        for (Pending x : PENDING) {
            if (x.who.equals(p.getUUID()) && x.later == null) return;
        }
        int wait = Math.max(0, Math.min(MOST_WAIT, style.hitTick(step)));
        PENDING.add(new Pending(p.getUUID(), target, p.serverLevel().getGameTime() + wait, style, null));
        ItemNet.toWatchers(p, new ItemNet.Show(p.getId(), style.ordinal(), step));
        if (step == style.dash && style.dash >= 0 && p.onGround()) {
            // the dash stab carries you in
            Vec3 look = p.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() > 1e-4) {
                look = look.normalize();
                p.setDeltaMovement(look.x * 0.75, 0.12, look.z * 0.75);
                p.hurtMarked = true;
            }
        }
    }

    // Something to do a few ticks from now, if the player's still about.
    public static void later(ServerPlayer p, int ticks, Runnable what) {
        PENDING.add(new Pending(p.getUUID(), -1, p.serverLevel().getGameTime() + ticks, null, what));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        List<Pending> due = new ArrayList<>();
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending x = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(x.who);
            if (p == null) {
                it.remove();
                continue;
            }
            if (p.serverLevel().getGameTime() < x.at) continue;
            it.remove();
            due.add(x);
        }
        for (Pending x : due) {
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(x.who);
            if (p == null) continue;
            if (x.later != null) {
                x.later.run();
                continue;
            }
            land(p, x);
        }
    }

    private static void land(ServerPlayer p, Pending x) {
        if (WeaponStyles.of(p.getMainHandItem().getItem()) != x.style || !p.isAlive()) return;
        Entity target = p.serverLevel().getEntity(x.target);
        if (target == null || !target.isAlive() || !target.isAttackable() || target == p) return;
        double reach = p.getEntityReach() + 1.2;
        if (p.distanceToSqr(target) > reach * reach + target.getBbWidth() * target.getBbWidth()) return;
        if (target instanceof LivingEntity l && !p.hasLineOfSight(l)) return;
        p.attack(target);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        PENDING.removeIf(x -> x.who.equals(e.getEntity().getUUID()));
    }
}
