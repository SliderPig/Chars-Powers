package com.example.charpowers.hidden;

import com.example.charpowers.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Who is lying flat right now (Play Dead, Flop). The server tells everyone nearby, so each PC also knows,
// and PlayerPoseMixin keeps those players lying down until the time is up.
public final class Lying {
    // entity id -> game time when they get back up. One map per side (server and the PC's own world).
    private static final Map<Integer, Long> SERVER = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> CLIENT = new ConcurrentHashMap<>();

    public static void start(ServerPlayer p, int ticks) {
        long until = p.level().getGameTime() + ticks;
        SERVER.put(p.getId(), until);
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new Net.LieDown(p.getId(), ticks));
    }

    // Called on the PC when the server says someone lies down.
    public static void startOnClient(Player p, int ticks) {
        CLIENT.put(p.getId(), p.level().getGameTime() + ticks);
    }

    public static boolean isLying(Player p) {
        Map<Integer, Long> map = p.level().isClientSide ? CLIENT : SERVER;
        Long until = map.get(p.getId());
        if (until == null) return false;
        if (p.level().getGameTime() >= until || !p.isAlive()) {
            map.remove(p.getId());
            return false;
        }
        return true;
    }

    private Lying() {}
}
