package com.example.charpowers.orders;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneManager;
import com.example.charpowers.cloning.jobs.CloneJob;
import com.example.charpowers.cloning.jobs.Jobs;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.mobcontrol.PetMinds;
import com.example.charpowers.orders.model.OrderRules;
import com.example.charpowers.own.Minions;
import com.example.charpowers.own.OwnPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The server half of the order wheel: tells your PC what you have out, and carries out the order you picked.
// Every order is checked again here: it must be a known one, for something you really have, and not spammed.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class OrderServer {
    private static final String UNDEAD = "undead", PET = "pet";
    private static final int LOOK_RANGE = 40;
    private static final Map<UUID, int[]> TOLD = new HashMap<>();
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private OrderServer() {}

    static int[] counts(ServerPlayer p) {
        int clones = 0;
        for (CloneEntity c : CloneManager.clonesOf(p.getUUID())) if (!c.rogue) clones++;
        return new int[] {clones, Minions.of(p.getUUID(), PET).size(), Minions.of(p.getUUID(), UNDEAD).size()};
    }

    public static void give(ServerPlayer p, String group, String action) {
        if (!OrderRules.valid(group, action)) return;
        long now = p.level().getGameTime();
        Long last = LAST.get(p.getUUID());
        if (last != null && OrderRules.tooSoon(last, now)) return;
        LAST.put(p.getUUID(), now);
        int[] have = counts(p);
        String said = null;
        if (group.equals(OrderRules.CLONES) && have[0] > 0) said = clones(p, action);
        else if (group.equals(OrderRules.PETS) && have[1] > 0) said = minions(p, PET, action);
        else if (group.equals(OrderRules.MINIONS) && have[2] > 0) said = minions(p, UNDEAD, action);
        if (said != null) OwnPower.whisper(p, said);
    }

    private static String clones(ServerPlayer p, String action) {
        List<CloneEntity> list = CloneManager.clonesOf(p.getUUID());
        int n = 0;
        for (CloneEntity c : list) {
            if (c.rogue) continue;
            if ("home".equals(action)) {
                CloneManager.dismiss(c);
            } else {
                CloneJob job = Jobs.make(action, p.blockPosition());
                if (job == null) continue;
                c.setJob(job);
            }
            n++;
        }
        if (n == 0) return null;
        return "home".equals(action) ? "Clones come home" : "Clones: " + OrderRules.labelOf(OrderRules.CLONES, action);
    }

    // Pets and Necromancy's minions share the same stances. "attack" goes for whatever you are looking at.
    private static String minions(ServerPlayer p, String kind, String action) {
        List<Mob> list = Minions.of(p.getUUID(), kind);
        boolean pets = PET.equals(kind);
        switch (action) {
            case "follow" -> {
                for (Mob m : list) Minions.setStance(m, Minions.FOLLOW);
            }
            case "stay" -> {
                for (Mob m : list) {
                    Minions.setStance(m, Minions.STAY);
                    if (pets) PetMinds.stand(m);
                }
            }
            case "guard" -> {
                if (!pets) return null;
                for (Mob m : list) {
                    Minions.setStance(m, Minions.GUARD);
                    PetMinds.stand(m);
                }
            }
            case "attack" -> {
                LivingEntity target = Fx.lookTarget(p, LOOK_RANGE);
                if (target == null) return "Look at what they should attack.";
                for (Mob m : list) Minions.setStance(m, Minions.FOLLOW);
                if (pets) PetMinds.order(p.getUUID(), target);
                else Minions.pointAt(p.getUUID(), kind, target);
            }
            default -> {
                return null;
            }
        }
        return (pets ? "Pets: " : "Minions: ") + OrderRules.labelOf(pets ? OrderRules.PETS : OrderRules.MINIONS, action);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 20 != 7) return;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            int[] now = counts(p);
            int[] before = TOLD.get(p.getUUID());
            if (before != null && before[0] == now[0] && before[1] == now[1] && before[2] == now[2]) continue;
            if (before == null && now[0] + now[1] + now[2] == 0) continue;
            TOLD.put(p.getUUID(), now);
            OrderNet.sendHave(p, now[0], now[1], now[2]);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        TOLD.remove(e.getEntity().getUUID());
        LAST.remove(e.getEntity().getUUID());
    }
}
