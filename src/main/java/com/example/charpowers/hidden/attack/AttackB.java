package com.example.charpowers.hidden.attack;

import com.example.charpowers.hidden.fx.Boost;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static com.example.charpowers.hidden.attack.AttackC.fallingItem;
import static com.example.charpowers.hidden.attack.AttackE.whenLanded;
import static com.example.charpowers.hidden.attack.AttackF.fightingMe;
import static com.example.charpowers.hidden.attack.AttackF.swing;
import static com.example.charpowers.hidden.attack.AttackHelp.*;
import static com.example.charpowers.hidden.f.FHelp.running;
import static com.example.charpowers.hidden.f.FHelp.startFor;
import static com.example.charpowers.hidden.model.Rank.B;
import static com.example.charpowers.hidden.ranks.RankHelp.launch;
import static com.example.charpowers.hidden.ranks.RankHelp.refund;

// B-rank attack powers (#358-367): 6 to 12 hearts, 30-90 s cooldowns. Area and thrown ones warn for 1 s first.
final class AttackB {
    static void registerAll() {
        // #358 Behind You: teleport behind your target (32 blocks): 10 hearts. Double if it wasn't fighting you. 60 s
        attack(B, 358, "behind_you", 1200, 24, () -> Items.ENDER_EYE, u -> {
            LivingEntity t = aim(u, 32);
            if (t == null) { refund(u); return; }
            boolean surprise = !fightingMe(u.entity(), t);
            if (!teleportBehind(u, t)) { refund(u); return; }
            swing(u);
            Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.7f);
            Fx.particles(u.level(), ParticleTypes.CRIT, t.getBoundingBox().getCenter(), 15, 0.4);
            hit(u, t, surprise ? 20 : 10);
        });
        // #359 Thousand Fists: 20 punches in 2 s at your target (3 blocks), half a heart each. It's held in place. 60 s
        attack(B, 359, "thousand_fists", 1200, 4, () -> Items.GOLD_BLOCK, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            if (!isBoss(t)) hold(u, t, 40);
            for (int i = 0; i < 20; i++) {
                Later.after(u, 1 + i * 2, () -> {
                    if (!t.isAlive() || t.distanceTo(u.entity()) > 5) return;
                    swing(u);
                    Fx.particles(u.level(), ParticleTypes.CRIT, t.getBoundingBox().getCenter(), 3, 0.4);
                    Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_WEAK, 0.8f, 1.2f + u.entity().getRandom().nextFloat() * 0.6f);
                    hit(u, t, 0.5f);
                });
            }
        });
        // #360 Crater Leap ⛏: leap up to 20 blocks to where you look. Landing: 8 hearts within 5 blocks, crater radius 4. 60 s
        attack(B, 360, "crater_leap", 1200, 20, () -> Items.TNT, u -> {
            LivingEntity me = u.entity();
            Vec3 aimAt = lookSpot(u, 20);
            Vec3 spot = ground(u.level(), aimAt.x, aimAt.z, aimAt.y);
            warn(u, spot, 5, 20);
            Later.after(u, 20, () -> {
                Vec3 d = spot.subtract(me.position());
                launch(me, new Vec3(d.x * 0.09, 1.1, d.z * 0.09)); // high arc toward the spot (tune in-game)
                Fx.sound(me, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 0.5f);
                whenLanded(u, 0, () -> {
                    me.fallDistance = 0;
                    Vec3 at = me.position();
                    AttackHelp.sound(u, at, SoundEvents.GENERIC_EXPLODE, 2f, 0.7f);
                    Fx.particles(u.level(), ParticleTypes.EXPLOSION_EMITTER, at, 1, 0);
                    for (LivingEntity t : around(u, at, 5)) hit(u, t, 8);
                    Wreck.blocks(u, Wreck.bowl(BlockPos.containing(at).below(), 4), BlockPos.containing(at));
                });
            });
        });
        // #361 Power Beam ⛏: 1 s warning glow, then a 2 s beam (30 blocks): 3 hearts every half second (12 total).
        // Bores a 1-wide tunnel. 75 s
        attack(B, 361, "power_beam", 1500, 24, () -> Items.BEACON, u -> {
            LivingEntity me = u.entity();
            effect(me, MobEffects.GLOWING, 60, 0);
            Fx.sound(me, SoundEvents.BEACON_POWER_SELECT, 1.5f, 1.2f);
            for (int i = 0; i < 4; i++) Later.after(u, i * 5 + 1, () -> Fx.particles(u.level(), ParticleTypes.END_ROD, me.getEyePosition(), 6, 0.4));
            for (int i = 0; i < 4; i++) {
                boolean first = i == 0;
                Later.after(u, 20 + i * 10, () -> {
                    Vec3 from = me.getEyePosition();
                    Vec3 dir = facing(u);
                    trail(u.level(), from, from.add(dir.scale(30)), ParticleTypes.END_ROD);
                    Fx.sound(me, SoundEvents.BEACON_AMBIENT, 2f, 1.8f);
                    if (first) Wreck.blocks(u, Wreck.tunnel(from.add(dir), dir, 30, 1), BlockPos.containing(from));
                    for (LivingEntity t : line(u, from, dir, 30, 1)) hit(u, t, 3);
                });
            }
        });
        // #362 Black Hole Fist: a tiny black hole opens at your target (16 blocks) for 3 s, pulls in everything within
        // 6 blocks, 2 hearts every half second to whatever is close to it (up to 12). 60 s
        attack(B, 362, "black_hole_fist", 1200, 16, () -> Items.ENDER_EYE, u -> {
            LivingEntity t = aim(u, 16);
            if (t == null) { refund(u); return; }
            Vec3 c = t.getBoundingBox().getCenter();
            AttackHelp.sound(u, c, SoundEvents.PORTAL_TRIGGER, 1f, 1.8f);
            for (int tick = 0; tick < 60; tick += 2) {
                boolean bite = tick % 10 == 0;
                Later.after(u, tick + 1, () -> {
                    Fx.particles(u.level(), ParticleTypes.REVERSE_PORTAL, c, 12, 0.6);
                    Fx.particles(u.level(), ParticleTypes.SQUID_INK, c, 2, 0.15);
                    for (LivingEntity e : around(u, c, 6)) {
                        if (!isBoss(e)) launch(e, c.subtract(e.getBoundingBox().getCenter()).normalize().scale(0.25));
                        if (bite && e.getBoundingBox().getCenter().distanceTo(c) <= 2.5) hit(u, e, 2);
                    }
                });
            }
        });
        // #363 Thunder Hammer: 1 s wind-up, then slam: 8 hearts within 4 blocks, stunned 2 s (can't move or attack).
        // Thunder sound, no flash. 60 s
        attack(B, 363, "thunder_hammer", 1200, 4, () -> Items.ANVIL, u -> {
            LivingEntity me = u.entity();
            warn(u, me.position(), 4, 20);
            Later.after(u, 20, () -> {
                Vec3 at = me.position();
                swing(u);
                AttackHelp.sound(u, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 0.8f);
                AttackHelp.sound(u, at, SoundEvents.ANVIL_LAND, 1.5f, 0.5f);
                Fx.particles(u.level(), ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.5, 0), 40, 2);
                for (LivingEntity t : around(u, at, 4)) {
                    if (!hit(u, t, 8) || isBoss(t)) continue;
                    hold(u, t, 40);
                    effect(t, MobEffects.WEAKNESS, 40, 9); // hits do nothing
                }
            });
        });
        // #364 Tidal Wave: a wave 10 wide rolls 20 blocks forward: 6 hearts and sweeps everything along.
        // The water is gone after (it's all splashes). 60 s
        attack(B, 364, "tidal_wave", 1200, 16, () -> Items.WATER_BUCKET, u -> {
            LivingEntity me = u.entity();
            Vec3 dir = facing(u).multiply(1, 0, 1).normalize();
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            Vec3 start = me.position();
            warn(u, start.add(dir.scale(3)), 3, 20);
            Set<LivingEntity> hitOnes = new HashSet<>();
            for (int i = 1; i <= 20; i++) {
                Vec3 front = start.add(dir.scale(i));
                Later.after(u, 20 + i, () -> {
                    for (int s = -5; s <= 5; s++) {
                        Vec3 p = front.add(side.scale(s));
                        Fx.particles(u.level(), ParticleTypes.SPLASH, p.add(0, 1, 0), 6, 0.4);
                        Fx.particles(u.level(), ParticleTypes.FALLING_WATER, p.add(0, 2.5, 0), 3, 0.4);
                    }
                    AttackHelp.sound(u, front, SoundEvents.GENERIC_SPLASH, 1f, 0.7f);
                    for (LivingEntity t : around(u, front.add(0, 1, 0), 5.5)) {
                        if (Math.abs(t.position().subtract(front).dot(dir)) > 1.5) continue; // only the wave's front
                        if (hitOnes.add(t)) hit(u, t, 6);
                        if (!isBoss(t)) launch(t, new Vec3(dir.x * 0.8, 0.2, dir.z * 0.8));
                    }
                });
            }
        });
        // #365 Blood Pact: your hits do 50% more damage, but each one costs you a quarter heart (it can't kill you)
        passive(B, 365, "blood_pact", () -> Items.CRIMSON_FUNGUS, new PowerEffect() {
            @Override public void boostHit(PowerUser u, LivingEntity target, Boost b) {
                b.times(1.5f);
                if (u.state().getLong("paid") == b.press && u.state().contains("paid")) return; // once per key press
                u.state().putLong("paid", b.press);
                LivingEntity me = u.entity();
                if (me.getHealth() > 0.5f) me.setHealth(Math.max(0.5f, me.getHealth() - 0.5f));
            }
        });
        // #366 Explosive Fist ⛏: your next punch within 10 s explodes: 10 hearts to it, 4 to things within 3 blocks,
        // breaks blocks like TNT. 60 s
        attack(B, 366, "explosive_fist", 1200, () -> Items.GUNPOWDER, new Move(4) {
            @Override public void onKey(PowerUser u) {
                startFor(u, "boom", 200);
                Fx.sound(u.entity(), SoundEvents.TNT_PRIMED, 1f, 1.2f);
                for (int i = 0; i < 10; i++) Later.after(u, i * 20 + 1, () -> {
                    if (running(u, "boom")) Fx.particles(u.level(), ParticleTypes.SMOKE, u.entity().getEyePosition().add(u.entity().getViewVector(1f).scale(0.6)), 3, 0.1);
                });
                if (u.mob() != null && u.mob().getTarget() != null) explode(u, u.mob().getTarget()); // mobs punch right away
            }
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                if (!isPunch(u.entity(), e.getSource()) || !running(u, "boom")) return; // a real punch only (no arrows, thorns or powers)
                explode(u, target);
            }
        });
        // #367 Sword Rain: 1 s warning, then 20 swords fall over 3 s on the area around your target (radius 5): 2 hearts each hit. 75 s
        attack(B, 367, "sword_rain", 1500, 24, () -> Items.DIAMOND_SWORD, u -> {
            LivingEntity t = aim(u, 24);
            Vec3 centre = t != null ? t.position() : lookSpot(u, 24);
            warn(u, centre, 5, 20);
            Random r = new Random(u.now());
            for (int i = 0; i < 20; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * 5;
                Vec3 land = ground(u.level(), centre.x + Math.cos(a) * d, centre.z + Math.sin(a) * d, centre.y);
                fallingItem(u, new ItemStack(Items.IRON_SWORD), land, 20 + i * 3, 1.2, 2, SoundEvents.PLAYER_ATTACK_STRONG);
            }
        });
    }

    // Explosive Fist going off on `t` (next tick, so it never lands inside another hit).
    private static void explode(PowerUser u, LivingEntity t) {
        u.state().putLong("boom", 0);
        Later.after(u, 1, () -> {
            Vec3 at = t.getBoundingBox().getCenter();
            AttackHelp.sound(u, at, SoundEvents.GENERIC_EXPLODE, 1.5f, 1f);
            Fx.particles(u.level(), ParticleTypes.EXPLOSION_EMITTER, at, 1, 0);
            if (t.isAlive()) hit(u, t, 10);
            for (LivingEntity e : around(u, at, 3)) if (e != t) hit(u, e, 4);
            Wreck.blocks(u, Wreck.sphere(BlockPos.containing(at), 3), BlockPos.containing(at));
        });
    }

    private AttackB() {}
}
