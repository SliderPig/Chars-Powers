package com.example.charpowers.own;

import com.example.charpowers.CharPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Several quests are "win a whole raid, and...". A power calls step() once a second for a player on such a quest. It
// joins them to a raid they're in from the start, notes if they got hit or hit anything, and says how it ended.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class RaidWatch {
    private static final Map<UUID, Watch> WATCHING = new HashMap<>();

    public static final class Watch {
        final Raid raid;
        public boolean started, over, wasHit, didHit;

        Watch(Raid raid) {
            this.raid = raid;
        }

        public boolean won() {
            return over && raid.isVictory();
        }
    }

    private RaidWatch() {}

    // Null = not in a raid. started is true only the first time, over only the last.
    public static Watch step(ServerPlayer p) {
        Watch w = WATCHING.get(p.getUUID());
        if (w == null) {
            Raid raid = p.serverLevel().getRaidAt(p.blockPosition());
            if (raid == null || raid.isOver() || raid.isStopped() || raid.getGroupsSpawned() > 1) return null;
            w = new Watch(raid);
            w.started = true;
            WATCHING.put(p.getUUID(), w);
            return w;
        }
        w.started = false;
        if (w.raid.isOver() || w.raid.isStopped()) {
            w.over = true;
            WATCHING.remove(p.getUUID());
        }
        return w;
    }

    // This player has failed. They can join again only if the raid is still on its first wave.
    public static void drop(ServerPlayer p) {
        WATCHING.remove(p.getUUID());
    }

    public static boolean inOne(ServerPlayer p) {
        return WATCHING.containsKey(p.getUUID());
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (WATCHING.isEmpty()) return;
        Watch hurt = WATCHING.get(e.getEntity().getUUID());
        if (hurt != null) hurt.wasHit = true;
        if (e.getSource().getEntity() instanceof ServerPlayer by) {
            Watch hitter = WATCHING.get(by.getUUID());
            if (hitter != null) hitter.didHit = true;
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        WATCHING.remove(e.getEntity().getUUID());
    }
}
