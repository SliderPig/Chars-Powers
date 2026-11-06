package com.example.charpowers.own;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Who is on whose team. Uses the optional mod Char's Teams (by reflection, so it is never needed); without it falls back on the vanilla
// scoreboard team; with neither, nobody is anybody's teammate and nothing crashes.
public final class TeamBridge {
    private static boolean looked;
    private static Method teammates, sameTeam;

    private TeamBridge() {}

    private static void find() {
        if (looked) return;
        looked = true;
        try {
            Class<?> api = Class.forName("com.example.charteams.api.CharTeams");
            teammates = api.getMethod("teammates", UUID.class);
            sameTeam = api.getMethod("sameTeam", UUID.class, UUID.class);
        } catch (Throwable ignored) {
            teammates = null;
            sameTeam = null;
        }
    }

    // Everyone on this player's team, the player included. Empty when there is no team.
    @SuppressWarnings("unchecked")
    public static Set<UUID> teammates(UUID player) {
        find();
        if (teammates != null) {
            try {
                Set<UUID> out = new HashSet<>((Set<UUID>) teammates.invoke(null, player));
                if (!out.isEmpty()) out.add(player);
                return out;
            } catch (Throwable ignored) {
                // fall through to the scoreboard
            }
        }
        Set<UUID> out = new HashSet<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer me = server == null ? null : server.getPlayerList().getPlayer(player);
        if (me == null || !(me.getTeam() instanceof PlayerTeam team)) return out;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other.getTeam() == team) out.add(other.getUUID());
        }
        return out;
    }

    public static boolean sameTeam(UUID a, UUID b) {
        if (a.equals(b)) return false;
        find();
        if (sameTeam != null) {
            try {
                return (Boolean) sameTeam.invoke(null, a, b);
            } catch (Throwable ignored) {
                // fall through
            }
        }
        return teammates(a).contains(b);
    }
}
