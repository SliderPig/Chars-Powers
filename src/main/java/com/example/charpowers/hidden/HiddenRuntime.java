package com.example.charpowers.hidden;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.model.Loadout;
import com.example.charpowers.hidden.model.PowerDef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

// Players' equipped hidden powers: running the always-on ones, using a key, changing slots.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class HiddenRuntime {
    // Set by Power Compat while Power Negation / dampening / Power Erasure is on someone.
    public static final String OFF_TAG = "PowerCompat.Off";

    public static boolean switchedOff(ServerPlayer p) { return p.getTags().contains(OFF_TAG); }

    // Some powers also work on the player's own PC (movement, things on screen). The client part fills this in
    // so shared code can ask "has this player got X on?" on either side. On a server it's always false.
    public static java.util.function.BiPredicate<Player, String> clientHas = (p, id) -> false;

    // Has this player got this power working right now? Works on the server, and on a PC for its own player.
    public static boolean has(Player p, String id) {
        if (p.level().isClientSide) return clientHas.test(p, id);
        return p instanceof ServerPlayer sp && HiddenEvents.has(sp, id);
    }

    // The PC also needs to know when powers are switched off (dampening etc.), for the parts that run there.
    private static final java.util.Map<java.util.UUID, Boolean> SENT_OFF = new java.util.HashMap<>();

    private static void syncOff(ServerPlayer p) {
        boolean off = switchedOff(p);
        Boolean was = SENT_OFF.put(p.getUUID(), off);
        if (was == null || was != off) com.example.charpowers.Net.sendTo(p, new com.example.charpowers.Net.Off(off));
    }

    // The powers working on this player right now (all equipped ones, unless they're switched off).
    public static List<HiddenEvents.Active> active(ServerPlayer p) {
        List<HiddenEvents.Active> out = new ArrayList<>();
        if (switchedOff(p)) return out;
        for (String id : PlayerHiddenData.get(p).equipped()) {
            if (HiddenPowers.def(id).isPresent()) out.add(new HiddenEvents.Active(id, user(p, id)));
        }
        return out;
    }

    static PowerUser user(ServerPlayer p, String id) {
        return new PowerUser(p, p, PlayerHiddenData.state(p, id));
    }

    // A key on the ability bar was pressed (slot 0-4). Returns how many ticks of cooldown are left afterwards
    // (read after the power ran, since a power can give some back), so the bar can show it. 0 = none.
    public static int useKey(ServerPlayer p, int slot) {
        if (switchedOff(p)) return 0;
        String id = PlayerHiddenData.get(p).key(slot);
        if (id == null) return 0;
        PowerDef def = HiddenPowers.def(id).orElse(null);
        if (def == null) return 0;
        PowerUser u = user(p, id);
        if (MobPowerTicker.cooledDown(u, def)) {
            try {
                com.example.charpowers.hidden.attack.AttackHelp.markPress(p);
                HiddenPowers.effect(id).onKey(u);
            } catch (RuntimeException e) {
                CharPowers.LOG.error("Hidden power {} failed for {}", id, p.getGameProfile().getName(), e);
            }
        }
        return cooldownLeft(p, slot);
    }

    // Ticks of cooldown left on the power in this key slot (0 = ready, or nothing there / switched off).
    public static int cooldownLeft(ServerPlayer p, int slot) {
        if (switchedOff(p)) return 0;
        String id = PlayerHiddenData.get(p).key(slot);
        if (id == null || HiddenPowers.def(id).isEmpty()) return 0;
        PowerUser u = user(p, id);
        long left = u.state().getLong("cd") - u.now();
        return (int) Math.max(0, Math.min(left, Integer.MAX_VALUE));
    }

    // The menu tab asked to put a power in a slot (or take it off). Never trust it: check again here.
    public static void equip(ServerPlayer p, boolean keySlot, int slot, String id) {
        Loadout l = PlayerHiddenData.get(p);
        String before = keySlot ? l.key(slot) : l.always(slot);
        if (id.isEmpty()) {
            l.unequip(keySlot, slot);
        } else {
            PowerDef def = HiddenPowers.def(id).orElse(null);
            if (def == null || !l.equip(def, keySlot, slot)) {
                PlayerHiddenData.sync(p); // put the client back in step
                return;
            }
        }
        if (before != null && !before.equals(id)) stopped(p, before);
        PlayerHiddenData.save(p, l);
    }

    // A power came off a slot: let it tidy up (e.g. send away a Ghost Kitty).
    static void stopped(ServerPlayer p, String id) {
        try {
            PowerUser u = user(p, id);
            for (String lent : HiddenPowers.effect(id).lends(u)) HiddenPowers.effect(lent).stop(u.lent(lent));
            HiddenPowers.effect(id).stop(u);
        } catch (RuntimeException e) {
            CharPowers.LOG.error("Hidden power {} failed to stop for {}", id, p.getGameProfile().getName(), e);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (p.tickCount % 20 == 0) {
            HiddenGrant.check(p);
            syncOff(p);
        }
        for (HiddenEvents.Active a : active(p)) {
            try {
                a.effect().tick(a.user());
            } catch (RuntimeException ex) {
                CharPowers.LOG.error("Hidden power {} failed for {}", a.id(), p.getGameProfile().getName(), ex);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) PlayerHiddenData.sync(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            com.example.charpowers.hidden.attack.Later.cancel(p);
            for (String id : PlayerHiddenData.get(p).equipped()) stopped(p, id);
            SENT_OFF.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        PlayerHiddenData.copy(e.getOriginal(), e.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) PlayerHiddenData.sync(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            com.example.charpowers.hidden.attack.Later.cancel(p); // half-done attack powers stay behind
            PlayerHiddenData.sync(p);
        }
    }

    private HiddenRuntime() {}
}
