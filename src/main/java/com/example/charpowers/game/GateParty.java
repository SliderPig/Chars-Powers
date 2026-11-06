package com.example.charpowers.game;

import com.example.charpowers.own.Pocket;
import com.example.charpowers.own.TeamBridge;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Raiding with friends. The Player invites teammates from the GATES tab (teammates come from Char's Teams, or the vanilla scoreboard team);
// they get a clickable message, and whoever accepts goes in with the Player, or joins the raid in progress through its entry.
// Friends need no power of their own: they fight and take the loot, but only a Game Player earns points and coins.
@Mod.EventBusSubscriber(modid = com.example.charpowers.CharPowers.MODID)
public final class GateParty {
    private static final long INVITE_TICKS = 20 * 120;
    private record Invite(UUID host, long expires) {}

    private static final Map<UUID, Set<UUID>> accepted = new HashMap<>(); // host -> friends who said yes and wait
    private static final Map<UUID, Invite> pending = new HashMap<>(); // friend -> the invitation

    private GateParty() {}

    private static ServerPlayer find(MinecraftServer server, UUID id) {
        return server == null ? null : server.getPlayerList().getPlayer(id);
    }

    // The INVITE list for the Status Window: teammates who are online, and where each one stands.
    static ListTag rows(ServerPlayer host) {
        ListTag out = new ListTag();
        MinecraftServer server = host.getServer();
        for (UUID id : TeamBridge.teammates(host.getUUID())) {
            ServerPlayer mate = find(server, id);
            if (id.equals(host.getUUID()) || mate == null) continue;
            CompoundTag row = new CompoundTag();
            row.putString("id", id.toString());
            row.putString("name", mate.getGameProfile().getName());
            boolean inRun = GamePlayer.POWER.runOf(id) != null;
            Invite inv = pending.get(id);
            String state = accepted.getOrDefault(host.getUUID(), Set.of()).contains(id) ? "joined"
                    : inRun || Pocket.in(mate) ? "busy" : inv != null && inv.host.equals(host.getUUID()) && inv.expires > host.level().getGameTime() ? "invited" : "";
            row.putString("state", state);
            out.add(row);
        }
        return out;
    }

    static void invite(ServerPlayer host, UUID target) {
        ServerPlayer mate = find(host.getServer(), target);
        if (mate == null || !TeamBridge.sameTeam(host.getUUID(), target)) {
            GamePlayer.POWER.note(host, "They aren't on your team.");
            return;
        }
        if (GamePlayer.POWER.runOf(target) != null || Pocket.in(mate)) {
            GamePlayer.POWER.note(host, mate.getGameProfile().getName() + " is already in a gate.");
            return;
        }
        pending.put(target, new Invite(host.getUUID(), host.level().getGameTime() + INVITE_TICKS));
        String who = host.getGameProfile().getName();
        MutableComponent msg = Component.literal(who + " invites you to raid a gate. ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal("[Accept]").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gateparty accept " + host.getUUID()))))
                .append(Component.literal(" "))
                .append(Component.literal("[Decline]").withStyle(s -> s.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gateparty decline " + host.getUUID()))));
        mate.sendSystemMessage(msg);
        GamePlayer.POWER.noteAll(mate, who + " invites you to a gate. Check your chat.");
        GamePlayer.POWER.note(host, "Invited " + mate.getGameProfile().getName() + ".");
    }

    static void uninvite(ServerPlayer host, UUID target) {
        pending.remove(target);
        Set<UUID> set = accepted.get(host.getUUID());
        if (set != null) set.remove(target);
    }

    private static void accept(ServerPlayer p, UUID hostId) {
        Invite inv = pending.get(p.getUUID());
        ServerPlayer host = find(p.getServer(), hostId);
        if (inv == null || !inv.host.equals(hostId) || host == null || inv.expires < p.level().getGameTime()) {
            p.sendSystemMessage(Component.literal("That invitation has run out.").withStyle(ChatFormatting.GRAY));
            return;
        }
        pending.remove(p.getUUID());
        if (GamePlayer.POWER.runOf(p.getUUID()) != null || Pocket.in(p)) {
            p.sendSystemMessage(Component.literal("You're already in a gate.").withStyle(ChatFormatting.GRAY));
            return;
        }
        GateRun run = GamePlayer.POWER.runOf(hostId);
        if (run != null) {
            GamePlayer.POWER.joinRun(run, p);
            GamePlayer.POWER.noteAll(host, p.getGameProfile().getName() + " joined your raid.");
        } else {
            accepted.computeIfAbsent(hostId, k -> new LinkedHashSet<>()).add(p.getUUID());
            p.sendSystemMessage(Component.literal("You're in. You'll go in when " + host.getGameProfile().getName() + " enters the gate.").withStyle(ChatFormatting.GREEN));
            GamePlayer.POWER.noteAll(host, p.getGameProfile().getName() + " will come along.");
        }
    }

    private static void decline(ServerPlayer p, UUID hostId) {
        pending.remove(p.getUUID());
        ServerPlayer host = find(p.getServer(), hostId);
        if (host != null) GamePlayer.POWER.noteAll(host, p.getGameProfile().getName() + " said no.");
    }

    // Everyone who said yes, ready to go in with the host. Clears the list.
    static List<ServerPlayer> takeAccepted(ServerPlayer host) {
        List<ServerPlayer> out = new ArrayList<>();
        Set<UUID> set = accepted.remove(host.getUUID());
        if (set == null) return out;
        for (UUID id : set) {
            ServerPlayer p = find(host.getServer(), id);
            if (p != null && p.isAlive() && GamePlayer.POWER.runOf(id) == null && !Pocket.in(p)) out.add(p);
        }
        return out;
    }

    static void forget(UUID id) {
        accepted.remove(id);
        pending.remove(id);
        accepted.values().forEach(s -> s.remove(id));
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("gateparty")
                .then(Commands.literal("accept").then(Commands.argument("host", StringArgumentType.word()).executes(c -> {
                    UUID id = parse(StringArgumentType.getString(c, "host"));
                    if (id != null) accept(c.getSource().getPlayerOrException(), id);
                    return 1;
                })))
                .then(Commands.literal("decline").then(Commands.argument("host", StringArgumentType.word()).executes(c -> {
                    UUID id = parse(StringArgumentType.getString(c, "host"));
                    if (id != null) decline(c.getSource().getPlayerOrException(), id);
                    return 1;
                })))
                .then(Commands.literal("leave").executes(c -> {
                    GamePlayer.POWER.leaveRun(c.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static UUID parse(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
