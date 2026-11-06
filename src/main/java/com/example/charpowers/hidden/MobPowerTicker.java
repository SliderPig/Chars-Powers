package com.example.charpowers.hidden;

import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.model.MobRoll;
import com.example.charpowers.hidden.model.MobRule;
import com.example.charpowers.hidden.model.PowerDef;
import com.example.charpowers.hidden.model.SpamLimiter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

// Every tick: lets each mob that has a hidden power use it, following its mob rule.
public final class MobPowerTicker {
    private static SpamLimiter limiter;

    public static void tick(ServerLevel level) {
        for (Mob m : MobPowers.holdersIn(level)) {
            // usually just the mob's own power; Everything also lends the one it has right now
            for (HiddenEvents.Active a : HiddenEvents.active(m)) use(m, a.id(), a.user());
        }
    }

    private static void use(Mob m, String id, PowerUser u) {
        PowerDef def = HiddenPowers.def(id).orElse(null);
        if (def == null) return;
        PowerEffect fx = HiddenPowers.effect(id);
        try {
            if (def.mobRule() == MobRule.CARRY) {
                MobBrain.tick(m, u, id);
                return;
            }
            fx.tick(u);
            switch (def.mobRule()) {
                case RANDOM -> randomUse(m, u, () -> fx.onKey(u));
                case ATTACK -> {
                    LivingEntity t = m.getTarget();
                    if (t != null && t.isAlive() && t.distanceTo(m) <= fx.mobRange() && m.hasLineOfSight(t) && cooledDown(u, def)) {
                        com.example.charpowers.hidden.attack.AttackHelp.markPress(m);
                        fx.onKey(u);
                    }
                }
                case ESCAPE -> {
                    if (m.getHealth() < m.getMaxHealth() / 2 && cooledDown(u, def)) fx.onKey(u);
                }
                default -> { }
            }
        } catch (RuntimeException e) {
            // one broken power must never crash the server
            com.example.charpowers.CharPowers.LOG.error("Hidden power {} failed on {}", id, m, e);
        }
    }

    static void randomUse(Mob m, PowerUser u, Runnable what) {
        long now = u.now();
        long next = u.state().getLong("rnd");
        int min = HiddenConfig.MOB_RANDOM_MIN_SEC.get();
        int max = HiddenConfig.MOB_RANDOM_MAX_SEC.get();
        if (next == 0) {
            u.state().putLong("rnd", now + MobRoll.nextRandomUse(new MobPowers.RandomAdapter(m.getRandom()), min, max));
            return;
        }
        if (now < next) return;
        u.state().putLong("rnd", now + MobRoll.nextRandomUse(new MobPowers.RandomAdapter(m.getRandom()), min, max));
        Player p = Fx.nearestPlayer(m, HiddenConfig.MOB_RANDOM_RANGE.get());
        if (p == null) return;
        if (!limiter().allow(p.getUUID(), now)) return; // that player has heard enough mob powers this minute
        what.run();
    }

    // Shared cooldown check for key powers (players and mobs). Starts the cooldown when it says yes.
    public static boolean cooledDown(PowerUser u, PowerDef def) {
        if (u.now() < u.state().getLong("cd")) return false;
        u.state().putLong("cd", u.now() + def.cooldownTicks());
        return true;
    }

    private static SpamLimiter limiter() {
        if (limiter == null) limiter = new SpamLimiter(HiddenConfig.PLAYER_HEARS_PER_MINUTE.get());
        return limiter;
    }

    private MobPowerTicker() {}
}
