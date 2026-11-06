package com.example.charpowers.hidden;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.model.GrantRule;
import com.example.charpowers.hidden.model.Loadout;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.threetag.palladium.power.SuperpowerUtil;

// Giving and taking players' hidden powers, and keeping Palladium's "Hidden Powers" power in step.
public final class HiddenGrant {
    public static final ResourceLocation POWER = new ResourceLocation(CharPowers.MODID, "hidden_powers");

    public static boolean give(ServerPlayer p, String id) {
        if (HiddenPowers.def(id).isEmpty()) return false;
        Loadout l = PlayerHiddenData.get(p);
        if (l.owned().contains(id)) return false; // already had it: nothing happens
        if (!SuperpowerUtil.hasSuperpower(p, POWER)) {
            SuperpowerUtil.addSuperpower(p, POWER);
            if (!SuperpowerUtil.hasSuperpower(p, POWER)) {
                // the Hidden Powers power didn't load (broken file?): don't give anything, or check() would wipe it
                CharPowers.LOG.error("Couldn't add the Palladium power {} to {}, so no hidden power was given", POWER, p.getGameProfile().getName());
                return false;
            }
        }
        l.add(id);
        PlayerHiddenData.save(p, l);
        return true;
    }

    public static boolean take(ServerPlayer p, String id) {
        Loadout l = PlayerHiddenData.get(p);
        boolean wasEquipped = l.equipped().contains(id);
        if (!l.remove(id)) return false;
        if (wasEquipped) HiddenRuntime.stopped(p, id);
        PlayerHiddenData.save(p, l);
        if (l.owned().isEmpty()) SuperpowerUtil.removeSuperpower(p, POWER);
        return true;
    }

    // Clears everything: the whole collection, the slots and the Palladium power.
    public static void wipe(ServerPlayer p) {
        Loadout l = PlayerHiddenData.get(p);
        for (String id : l.equipped()) HiddenRuntime.stopped(p, id);
        l.clearAll();
        PlayerHiddenData.save(p, l);
        if (SuperpowerUtil.hasSuperpower(p, POWER)) SuperpowerUtil.removeSuperpower(p, POWER);
    }

    // Runs every second for each player: if something removed the Palladium power, the collection goes too.
    static void check(ServerPlayer p) {
        boolean has = SuperpowerUtil.hasSuperpower(p, POWER);
        boolean empty = PlayerHiddenData.get(p).owned().isEmpty();
        if (GrantRule.onCheck(has, empty) == GrantRule.Action.WIPE) {
            CharPowers.LOG.info("{} lost the Hidden Powers power, so their hidden powers were cleared", p.getGameProfile().getName());
            wipe(p);
        }
    }

    private HiddenGrant() {}
}
