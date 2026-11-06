package com.example.charpowers.hidden.attack;

import com.example.charpowers.hidden.fx.Boost;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Comparator;

import static com.example.charpowers.hidden.attack.AttackHelp.*;
import static com.example.charpowers.hidden.f.FHelp.running;
import static com.example.charpowers.hidden.f.FHelp.startFor;
import static com.example.charpowers.hidden.model.Rank.F;
import static com.example.charpowers.hidden.ranks.RankHelp.launch;
import static com.example.charpowers.hidden.ranks.RankHelp.refund;

// F-rank attack powers (#302-317): weak, about half a heart to a heart and a half, short cooldowns.
final class AttackF {
    static void registerAll() {
        // #302 Flick: flick the thing in front of you (3 blocks): 1 heart and a tiny push. 3 s
        attack(F, 302, "flick", 60, 4, () -> Items.STICK, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            swing(u);
            if (hit(u, t, 1)) knock(t, u.entity().position(), 0.3, 0.1);
            Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_WEAK, 1f, 1.6f);
        });
        // #303 Pebble Toss: throw a pebble (no item needed), 16 blocks: 1 heart. 3 s
        attack(F, 303, "pebble_toss", 60, 16, () -> Items.FLINT, u -> {
            LivingEntity t = aim(u, 16);
            if (t == null) { refund(u); return; }
            swing(u);
            trail(u.level(), u.entity().getEyePosition(), t.getBoundingBox().getCenter(), new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBBLESTONE)));
            Fx.sound(u.entity(), SoundEvents.SNOWBALL_THROW, 1f, 0.6f);
            hit(u, t, 1);
        });
        // #304 Spit: spit at it (8 blocks): half a heart + Nausea 2 s. 5 s
        attack(F, 304, "spit", 100, 8, () -> Items.SLIME_BALL, u -> {
            LivingEntity t = aim(u, 8);
            if (t == null) { refund(u); return; }
            trail(u.level(), u.entity().getEyePosition(), t.getEyePosition(), ParticleTypes.SPIT);
            Fx.sound(u.entity(), SoundEvents.LLAMA_SPIT, 1f, 1.2f);
            if (hit(u, t, 0.5f)) effect(t, MobEffects.CONFUSION, 40, 0);
        });
        // #305 Headbutt: lunge 2 blocks head first: 1.5 hearts to the first thing you hit. 5 s
        attack(F, 305, "headbutt", 100, 4, () -> Items.IRON_HELMET, u -> {
            LivingEntity me = u.entity();
            Vec3 dir = facing(u);
            LivingEntity t = line(u, me.getEyePosition(), dir, 3, 0.8).stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(me))).orElse(null);
            launch(me, me.getDeltaMovement().add(dir.x * 0.6, 0.1, dir.z * 0.6));
            if (t == null) { refund(u); return; }
            if (hit(u, t, 1.5f)) knock(t, me.position(), 0.5, 0.1);
            Fx.sound(me, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.8f);
        });
        // #306 Sucker Punch: your first hit on something that isn't fighting you does +1 heart
        passive(F, 306, "sucker_punch", () -> Items.LEATHER, new PowerEffect() {
            @Override public void boostHit(PowerUser u, LivingEntity target, Boost b) {
                if (!fightingMe(u.entity(), target)) b.add(1);
            }
        });
        // #307 Stomp: stomp: everything within 2 blocks takes half a heart. 4 s
        attack(F, 307, "stomp", 80, 2, () -> Items.LEATHER_BOOTS, u -> {
            LivingEntity me = u.entity();
            Fx.sound(me, SoundEvents.RAVAGER_STEP, 1f, 1.4f);
            Fx.particles(u.level(), ParticleTypes.POOF, me.position(), 10, 0.8);
            for (LivingEntity t : around(u, me.position(), 2)) hit(u, t, 0.5f);
        });
        // #308 Back Elbow: hit whatever is right behind you (2 blocks): 1 heart. 4 s
        attack(F, 308, "back_elbow", 80, 2, () -> Items.BONE, u -> {
            LivingEntity me = u.entity();
            Vec3 look = me.getViewVector(1f);
            LivingEntity t = around(u, me.position(), 2.5).stream()
                    .filter(e -> e.position().subtract(me.position()).dot(look) < 0)
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(me))).orElse(null);
            if (t == null) { refund(u); return; }
            swing(u);
            hit(u, t, 1);
            Fx.sound(me, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1.2f);
        });
        // #309 Iceball: throw a hard-packed snowball: 1 heart (blazes 3). 3 s
        attack(F, 309, "iceball", 60, 16, () -> Items.SNOWBALL, u -> {
            LivingEntity t = aim(u, 16);
            if (t == null) { refund(u); return; }
            swing(u);
            trail(u.level(), u.entity().getEyePosition(), t.getBoundingBox().getCenter(), ParticleTypes.ITEM_SNOWBALL);
            Fx.sound(u.entity(), SoundEvents.SNOWBALL_THROW, 1f, 0.8f);
            hit(u, t, t instanceof Blaze ? 3 : 1);
        });
        // #310 Nettle Sting: sting (3 blocks): half a heart + Poison 2 s. 5 s
        attack(F, 310, "nettle_sting", 100, 4, () -> Items.SWEET_BERRIES, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            swing(u);
            Fx.sound(u.entity(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1f, 1.5f);
            if (hit(u, t, 0.5f)) effect(t, MobEffects.POISON, 40, 0);
        });
        // #311 Double Tap: 1 in 10 of your hits lands a second small hit (half a heart)
        passive(F, 311, "double_tap", () -> Items.FEATHER, new PowerEffect() {
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                // only real hits: its own second hit is a power hit, so it can never tap again (no loops)
                if (!isPunch(u.entity(), e.getSource()) || u.entity().getRandom().nextInt(10) != 0) return;
                Later.after(u, 4, () -> {
                    if (target.isAlive()) hit(u, target, 0.5f);
                });
            }
        });
        // #312 Knuckle Crack: your next hit within 5 s does +1.5 hearts. 8 s
        attack(F, 312, "knuckle_crack", 160, () -> Items.IRON_NUGGET, new Move(4) {
            @Override public void onKey(PowerUser u) {
                startFor(u, "crack", 100);
                Fx.sound(u.entity(), SoundEvents.BONE_BLOCK_BREAK, 1f, 1.8f);
            }
            @Override public void boostHit(PowerUser u, LivingEntity target, Boost b) {
                if (!running(u, "crack")) return;
                b.add(1.5f);
                u.state().putLong("crack", 0);
            }
        });
        // #313 Kick: kick (3 blocks): 1 heart, knocks it back 3 blocks. 4 s
        attack(F, 313, "kick", 80, 4, () -> Items.CHAINMAIL_BOOTS, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            if (hit(u, t, 1)) knock(t, u.entity().position(), 1.0, 0.2);
            Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 1f);
        });
        // #314 Hot Hands: touch (3 blocks): half a heart + on fire 2 s. 5 s
        attack(F, 314, "hot_hands", 100, 4, () -> Items.BLAZE_POWDER, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            swing(u);
            Fx.particles(u.level(), ParticleTypes.FLAME, t.getBoundingBox().getCenter(), 8, 0.3);
            Fx.sound(u.entity(), SoundEvents.FIRECHARGE_USE, 0.6f, 1.6f);
            if (hit(u, t, 0.5f)) t.setSecondsOnFire(2);
        });
        // #315 Pinch: pinch (3 blocks): 1 heart that ignores armour. 6 s
        attack(F, 315, "pinch", 120, 4, () -> Items.SHEARS, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            swing(u);
            Fx.sound(u.entity(), SoundEvents.SHEEP_SHEAR, 1f, 1.8f);
            hit(u, t, 1, true);
        });
        // #316 Bite: bite (2 blocks): 1 heart, and you heal half a heart. 6 s
        attack(F, 316, "bite", 120, 3, () -> Items.ROTTEN_FLESH, u -> {
            LivingEntity t = aim(u, 2);
            if (t == null) { refund(u); return; }
            Fx.sound(u.entity(), SoundEvents.FOX_BITE, 1f, 1f);
            if (hit(u, t, 1)) u.entity().heal(1);
        });
        // #317 Leg Sweep: sweep its legs (3 blocks): half a heart + Slowness 2 s. 6 s
        attack(F, 317, "leg_sweep", 120, 4, () -> Items.LEAD, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.9f);
            if (hit(u, t, 0.5f)) effect(t, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
        });
    }

    static void swing(PowerUser u) { u.entity().swing(InteractionHand.MAIN_HAND, true); }

    // Is `t` already fighting `me`? (a mob after me, or someone I hurt in the last 10 s)
    static boolean fightingMe(LivingEntity me, LivingEntity t) {
        if (t instanceof Mob m && m.getTarget() == me) return true;
        return t.getLastHurtByMob() == me && t.tickCount - t.getLastHurtByMobTimestamp() < 200;
    }

    private AttackF() {}
}
