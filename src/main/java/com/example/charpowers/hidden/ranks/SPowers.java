package com.example.charpowers.hidden.ranks;

import com.example.charpowers.hidden.HiddenEvents;
import com.example.charpowers.hidden.HiddenPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.model.PowerDef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;

import java.util.ArrayList;
import java.util.List;

import static com.example.charpowers.hidden.f.FHelp.*;
import static com.example.charpowers.hidden.model.MobRule.*;
import static com.example.charpowers.hidden.model.Rank.S;
import static com.example.charpowers.hidden.model.Rank.Z;
import static com.example.charpowers.hidden.ranks.RankHelp.*;

// The S-rank hidden powers (legendary, 296-300) and the one Z rank (beyond legendary, 301).
final class SPowers {
    static void registerAll() {
        // #296 Nine Lives: 9 lives. Dying uses one up: you drop nothing and get straight back up on the spot with full
        // health. One grows back every Minecraft day. A mob with this has to be killed 9 times.
        always(S, 296, "nine_lives", PASSIVE, () -> Items.CAT_SPAWN_EGG, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (!u.state().getBoolean("init")) {
                    u.state().putBoolean("init", true);
                    u.state().putInt("lives", 9);
                    Fx.daily(u, "grow"); // the first one grows back tomorrow
                }
                rememberSafeSpot(u);
                if (u.now() % 100 == 0 && u.state().getInt("lives") < 9 && Fx.daily(u, "grow")) {
                    u.state().putInt("lives", u.state().getInt("lives") + 1);
                    tell(u, "A life grew back. Lives: " + u.state().getInt("lives"));
                }
                if (u.now() % 20 == 0) hud(u, "lives", "Lives: " + u.state().getInt("lives") + " / 9");
            }
            @Override public boolean preventsDeath(PowerUser u, LivingDeathEvent e) {
                int lives = u.state().getInt("lives");
                if (lives <= 0 || e.getSource().is(DamageTypes.GENERIC_KILL)) return false; // /kill still kills
                u.state().putInt("lives", lives - 1);
                LivingEntity me = u.entity();
                me.setHealth(me.getMaxHealth());
                me.clearFire();
                me.setAirSupply(me.getMaxAirSupply());
                me.fallDistance = 0;
                removeBadEffects(me);
                if (e.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) backToSafeSpot(u);
                Fx.sound(me, SoundEvents.CAT_AMBIENT, 1.5f, 0.8f);
                Fx.particles(u.level(), ParticleTypes.HEART, Fx.head(me), 8, 0.5);
                tell(u, "You lost a life. Lives left: " + (lives - 1));
                return true;
            }
            @Override public void stop(PowerUser u) { hud(u, "lives", ""); }
        });
        // #297 Main Character: 1 in 4 hits that would hurt you just miss, you land critical hits more often,
        // and rare drops are about twice as likely (crits and drops: RankEvents)
        always(S, 297, "main_character", PASSIVE, () -> Items.NETHER_STAR, new PowerEffect() {
            @Override public void onAttacked(PowerUser u, LivingAttackEvent e) {
                if (e.getSource().getEntity() == null || e.getSource().is(DamageTypes.GENERIC_KILL)) return;
                if (u.entity().getRandom().nextInt(4) != 0) return;
                e.setCanceled(true);
                Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_NODAMAGE, 1f, 1.2f);
            }
        });
        // #298 Soul Jar: kill a mob and you trap its soul (up to 3). Press the key to let one out as a loyal ghost
        // version of it that fights for you until it dies.
        key(S, 298, "soul_jar", ATTACK, 100, () -> Items.SOUL_LANTERN, new PowerEffect() {
            @Override public void onKill(PowerUser u, LivingEntity victim) {
                // no bosses, and nothing that could wreck builds as your ghost (creepers and ghasts blow up blocks,
                // endermen pick them up)
                if (!(victim instanceof Mob) || victim instanceof EnderDragon || victim instanceof WitherBoss || victim instanceof Warden
                        || victim instanceof net.minecraft.world.entity.monster.Creeper || victim instanceof net.minecraft.world.entity.monster.Ghast
                        || victim instanceof net.minecraft.world.entity.monster.EnderMan || victim.getTags().contains(HiddenEvents.TEMP_TAG)) return;
                ListTag souls = u.state().getList("souls", Tag.TAG_STRING);
                if (souls.size() >= 3) return;
                souls.add(StringTag.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString()));
                u.state().put("souls", souls);
                Fx.particles(u.level(), ParticleTypes.SOUL, victim.position().add(0, 1, 0), 10, 0.3);
                tell(u, "Soul trapped (" + souls.size() + "/3)");
            }
            @Override public void onKey(PowerUser u) {
                ListTag souls = u.state().getList("souls", Tag.TAG_STRING);
                if (souls.isEmpty()) { tell(u, "Your soul jar is empty. Defeat a mob first."); refund(u); return; }
                String id = souls.getString(souls.size() - 1);
                souls.remove(souls.size() - 1);
                u.state().put("souls", souls);
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(id));
                if (!(type.create(u.level()) instanceof Mob ghost)) return;
                LivingEntity me = u.entity();
                Vec3 at = me.position().add(me.getViewVector(1).multiply(1, 0, 1).normalize().scale(1.5));
                ghost.moveTo(at.x, me.getY(), at.z, me.getYRot(), 0);
                BPowers.ghostly(ghost);
                ghost.setCanPickUpLoot(false);
                for (EquipmentSlot s : EquipmentSlot.values()) ghost.setDropChance(s, 0f);
                makeMine(u, ghost);
                u.level().addFreshEntity(ghost);
                remember(u, "ghosts", ghost, Long.MAX_VALUE);
                Fx.sound(ghost, SoundEvents.SOUL_ESCAPE, 1.5f, 1f);
            }
            @Override public void tick(PowerUser u) {
                if (u.now() % 10 != 0) return;
                List<LivingEntity> foes = hostiles(u, 16);
                forEachRemembered(u, "ghosts", g -> {
                    if (!(g instanceof Mob m)) return;
                    LivingEntity t = m.getTarget();
                    if ((t == null || !t.isAlive() || !isFoe(u, t)) && !foes.isEmpty()) m.setTarget(foes.get(0));
                    else if (t == null && m.distanceTo(u.entity()) > 8) m.getNavigation().moveTo(u.entity(), 1.2);
                }, g -> {});
                if (u.now() % 20 == 0) {
                    int n = u.state().getList("souls", Tag.TAG_STRING).size();
                    hud(u, "souls", n > 0 ? "Souls: " + n + " / 3" : "");
                }
            }
            @Override public void stop(PowerUser u) {
                forEachRemembered(u, "ghosts", Entity::discard, g -> {});
                u.state().remove("ghosts");
                hud(u, "souls", "");
            }
        });
        // #299 Storm Heart: get hit and it rains around you; under half health, a "lightning" zap hits whatever hurt you
        // (every 10 s). No lightning bolt is ever made, so nothing flashes: thunder, sparks and a burn instead.
        always(S, 299, "storm_heart", PASSIVE, () -> Items.LIGHTNING_ROD, new PowerEffect() {
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) {
                startFor(u, "rain", 200);
                LivingEntity me = u.entity();
                if (me.getHealth() - e.getAmount() >= me.getMaxHealth() / 2 || running(u, "zapCd")) return;
                if (!(e.getSource().getEntity() instanceof LivingEntity by) || by == me) return;
                startFor(u, "zapCd", 200);
                by.hurt(me.damageSources().lightningBolt(), 5.0f);
                by.setSecondsOnFire(4);
                Fx.sound(by, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
                for (int i = 0; i < 8; i++) Fx.particles(u.level(), ParticleTypes.ELECTRIC_SPARK, by.position().add(0, i * 0.5, 0), 4, 0.15);
            }
            @Override public void tick(PowerUser u) {
                if (!running(u, "rain") || u.now() % 2 != 0) return;
                LivingEntity me = u.entity();
                var r = me.getRandom();
                Vec3 top = me.position().add(0, me.getBbHeight() + 2.5, 0);
                Fx.particles(u.level(), ParticleTypes.CLOUD, top, 3, 1.2);
                for (int i = 0; i < 4; i++) {
                    Vec3 drop = top.add(r.nextGaussian() * 1.5, -0.3, r.nextGaussian() * 1.5);
                    Fx.particles(u.level(), ParticleTypes.FALLING_WATER, drop, 1, 0);
                }
            }
        });
        // #300 Untouchable: fire, lava and explosions can't hurt you, and falling into the void throws you back up
        always(S, 300, "untouchable", PASSIVE, () -> Items.TOTEM_OF_UNDYING, new PowerEffect() {
            @Override public void onAttacked(PowerUser u, LivingAttackEvent e) {
                if (e.getSource().is(DamageTypeTags.IS_FIRE) || e.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                    e.setCanceled(true);
                    u.entity().clearFire();
                } else if (e.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
                    e.setCanceled(true);
                    backToSafeSpot(u);
                }
            }
            @Override public void tick(PowerUser u) {
                rememberSafeSpot(u);
                LivingEntity me = u.entity();
                if (me.isOnFire() && u.now() % 5 == 0) me.clearFire();
                if (me.getY() < u.level().getMinBuildHeight() - 8) backToSafeSpot(u);
            }
        });

        // #301 Everything (Z): you have ALL the hidden powers at once... but only one works at a time, and it changes to
        // a random one every 60 s (shown on your screen). Put it on a key: when the power of the minute is a key power,
        // that key uses it. Only one mob in the whole world can have it at a time (MobPowers / ZSlot).
        key(Z, 301, "everything", PASSIVE, 20, () -> Items.NETHER_STAR, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                String now = u.state().getString("now");
                if (!now.isEmpty()) APowers.useLentKey(u, now);
            }
            @Override public List<String> lends(PowerUser u) {
                String now = u.state().getString("now");
                return now.isEmpty() ? List.of() : List.of(now);
            }
            @Override public void tick(PowerUser u) {
                String now = u.state().getString("now");
                if (now.isEmpty() || !running(u, "next")) {
                    if (!now.isEmpty()) APowers.endLent(u, now);
                    List<PowerDef> pool = new ArrayList<>();
                    for (PowerDef d : HiddenPowers.all()) if (!d.id().equals("everything") && !d.id().equals("copycat")) pool.add(d);
                    String pick = pool.get(u.entity().getRandom().nextInt(pool.size())).id();
                    u.state().putString("now", pick);
                    startFor(u, "next", 1200);
                    if (u.player() != null) tell(u, "Everything: now you're " + HiddenPowers.name(pick).getString());
                }
                if (u.now() % 20 == 0) {
                    long left = Math.max(0, (u.state().getLong("next") - u.now()) / 20);
                    hud(u, "everything", "Everything: " + HiddenPowers.name(u.state().getString("now")).getString() + " (" + left + "s)");
                }
            }
            @Override public void stop(PowerUser u) {
                String now = u.state().getString("now");
                if (!now.isEmpty()) APowers.endLent(u, now);
                u.state().putString("now", "");
                hud(u, "everything", "");
            }
        });
    }

    // --- building blocks ---

    static void removeBadEffects(LivingEntity e) {
        List<MobEffect> bad = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) if (i.getEffect().getCategory() == MobEffectCategory.HARMFUL) bad.add(i.getEffect());
        bad.forEach(e::removeEffect);
    }

    // Where the holder last stood on solid ground (so the void can throw them back there).
    static void rememberSafeSpot(PowerUser u) {
        LivingEntity e = u.entity();
        if (u.now() % 10 != 0 || !e.onGround() || e.getY() < u.level().getMinBuildHeight()) return;
        u.state().putDouble("sx", e.getX());
        u.state().putDouble("sy", e.getY());
        u.state().putDouble("sz", e.getZ());
        u.state().putString("sdim", u.level().dimension().location().toString());
    }

    static void backToSafeSpot(PowerUser u) {
        LivingEntity e = u.entity();
        Vec3 to;
        if (u.state().getString("sdim").equals(u.level().dimension().location().toString())) {
            to = new Vec3(u.state().getDouble("sx"), u.state().getDouble("sy") + 0.5, u.state().getDouble("sz"));
        } else {
            BlockPos spawn = u.level().getSharedSpawnPos();
            to = new Vec3(spawn.getX() + 0.5, u.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ()) + 1, spawn.getZ() + 0.5);
        }
        e.setDeltaMovement(Vec3.ZERO);
        teleport(e, to);
        Fx.sound(e, SoundEvents.ENDERMAN_TELEPORT, 1f, 0.8f);
    }

    private SPowers() {}
}
