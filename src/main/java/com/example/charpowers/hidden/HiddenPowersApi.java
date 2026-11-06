package com.example.charpowers.hidden;

import com.example.charpowers.hidden.model.Loadout;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;

// The way other code talks to hidden powers: Power Compat's KubeJS scripts
// (Java.loadClass('com.example.charpowers.hidden.HiddenPowersApi')) and, later, Death Steal and Future Sight.
public final class HiddenPowersApi {
    // The hidden power a mob has, if any (players: use owned/equipped).
    public static Optional<String> mobPower(LivingEntity e) {
        return e instanceof Mob m ? MobPowers.get(m) : Optional.empty();
    }

    // Adds one to a player's collection. False if they already had it or it doesn't exist.
    public static boolean give(ServerPlayer p, String id) { return HiddenGrant.give(p, id); }

    public static boolean take(ServerPlayer p, String id) { return HiddenGrant.take(p, id); }

    public static List<String> owned(ServerPlayer p) { return new ArrayList<>(PlayerHiddenData.get(p).owned()); }

    public static List<String> equipped(ServerPlayer p) { return PlayerHiddenData.get(p).equipped(); }

    // Takes every EQUIPPED one away from this player and returns them (Absorption / Transferal).
    public static List<String> takeEquipped(ServerPlayer from) {
        List<String> eq = equipped(from);
        for (String id : eq) HiddenGrant.take(from, id);
        return eq;
    }

    // Takes ONE random equipped one away (crouching with the DNA Normalizer / Absorption).
    public static Optional<String> takeRandomEquipped(ServerPlayer p) {
        Loadout l = PlayerHiddenData.get(p);
        List<String> eq = l.equipped();
        if (eq.isEmpty()) return Optional.empty();
        String id = eq.get(new SplittableRandom().nextInt(eq.size()));
        HiddenGrant.take(p, id);
        return Optional.of(id);
    }

    // Clears absolutely everything (collection + slots).
    public static void wipe(ServerPlayer p) { HiddenGrant.wipe(p); }

    // The power's display name in English-ish form for chat lines made by scripts.
    public static String name(String id) { return HiddenPowers.name(id).getString(); }

    private HiddenPowersApi() {}
}
