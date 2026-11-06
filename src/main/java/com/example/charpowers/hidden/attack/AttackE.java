package com.example.charpowers.hidden.attack;

import com.example.charpowers.hidden.fx.Boost;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.example.charpowers.hidden.attack.AttackF.swing;
import static com.example.charpowers.hidden.attack.AttackHelp.*;
import static com.example.charpowers.hidden.model.Rank.E;
import static com.example.charpowers.hidden.ranks.RankHelp.launch;
import static com.example.charpowers.hidden.ranks.RankHelp.refund;

// E-rank attack powers (#318-331): 1 to 3 hearts, a little more to them than F.
final class AttackE {
    static void registerAll() {
        // #318 Fast Fist: 5 punches in 1 second at what you look at (3 blocks), half a heart each. 6 s
        attack(E, 318, "fast_fist", 120, 4, () -> Items.IRON_INGOT, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            for (int i = 0; i < 5; i++) {
                Later.after(u, 1 + i * 4, () -> {
                    if (!t.isAlive() || t.distanceTo(u.entity()) > 4.5) return;
                    swing(u);
                    Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_WEAK, 1f, 1.4f + u.entity().getRandom().nextFloat() * 0.3f);
                    Fx.particles(u.level(), ParticleTypes.CRIT, t.getBoundingBox().getCenter(), 4, 0.3);
                    hit(u, t, 0.5f);
                });
            }
        });
        // #319 Rock Throw: throw a rock (20 blocks): 2 hearts. 6 s
        attack(E, 319, "rock_throw", 120, 20, () -> Items.COBBLESTONE, u -> {
            LivingEntity t = aim(u, 20);
            if (t == null) { refund(u); return; }
            swing(u);
            trail(u.level(), u.entity().getEyePosition(), t.getBoundingBox().getCenter(), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()));
            Fx.sound(u.entity(), SoundEvents.STONE_BREAK, 1f, 0.7f);
            hit(u, t, 2);
        });
        // #320 Uppercut: 2 hearts and launches it 4 blocks up (the fall hurts too). 8 s
        attack(E, 320, "uppercut", 160, 4, () -> Items.GOLDEN_SHOVEL, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            swing(u);
            Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.7f);
            if (hit(u, t, 2) && !isBoss(t)) launch(t, new Vec3(0, 0.9, 0)); // about 4 blocks up (tune in-game)
        });
        // #321 Spin Kick: everything within 3 blocks takes 1.5 hearts and is pushed away. 8 s
        attack(E, 321, "spin_kick", 160, 3, () -> Items.LEATHER_BOOTS, u -> {
            LivingEntity me = u.entity();
            Fx.sound(me, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.8f);
            Fx.particles(u.level(), ParticleTypes.SWEEP_ATTACK, me.position().add(0, 1, 0), 6, 1.2);
            for (LivingEntity t : around(u, me.position(), 3)) {
                if (hit(u, t, 1.5f)) knock(t, me.position(), 0.8, 0.2);
            }
        });
        // #322 Shoulder Charge: charge 5 blocks forward; the first thing hit takes 2 hearts and flies back. 8 s
        attack(E, 322, "shoulder_charge", 160, 5, () -> Items.SHIELD, u -> {
            LivingEntity me = u.entity();
            Vec3 dir = facing(u).multiply(1, 0, 1).normalize();
            launch(me, new Vec3(dir.x * 1.3, 0.15, dir.z * 1.3));
            Fx.sound(me, SoundEvents.RAVAGER_ATTACK, 0.8f, 1.4f);
            LivingEntity t = line(u, me.position().add(0, 1, 0), dir, 5, 1).stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(me))).orElse(null);
            if (t != null && hit(u, t, 2)) knock(t, me.position(), 1.5, 0.3);
        });
        // #323 Thorn Whip: vine lash (6 blocks): 2 hearts and pulls it to you. 8 s
        attack(E, 323, "thorn_whip", 160, 6, () -> Items.VINE, u -> {
            LivingEntity t = aim(u, 6);
            if (t == null) { refund(u); return; }
            LivingEntity me = u.entity();
            trail(u.level(), me.getEyePosition(), t.getBoundingBox().getCenter(), ParticleTypes.COMPOSTER);
            Fx.sound(me, SoundEvents.CAVE_VINES_PICK_BERRIES, 1f, 0.6f);
            if (hit(u, t, 2) && !isBoss(t)) {
                Vec3 pull = me.position().subtract(t.position()).normalize().scale(0.9);
                launch(t, new Vec3(pull.x, 0.35, pull.z));
            }
        });
        // #324 Ember Flick: small fireball (16 blocks, never sets blocks alight): 2 hearts + on fire 3 s. 6 s
        attack(E, 324, "ember_flick", 120, 16, () -> Items.FIRE_CHARGE, u -> {
            LivingEntity t = aim(u, 16);
            if (t == null) { refund(u); return; }
            swing(u);
            trail(u.level(), u.entity().getEyePosition(), t.getBoundingBox().getCenter(), ParticleTypes.FLAME);
            Fx.sound(u.entity(), SoundEvents.FIRECHARGE_USE, 0.8f, 1.3f);
            if (hit(u, t, 2)) t.setSecondsOnFire(3);
        });
        // #325 Ice Shard: ice shard (20 blocks): 1.5 hearts + Slowness II 3 s. 6 s
        attack(E, 325, "ice_shard", 120, 20, () -> Items.ICE, u -> {
            LivingEntity t = aim(u, 20);
            if (t == null) { refund(u); return; }
            swing(u);
            trail(u.level(), u.entity().getEyePosition(), t.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE);
            Fx.sound(u.entity(), SoundEvents.GLASS_BREAK, 0.7f, 1.6f);
            if (hit(u, t, 1.5f)) effect(t, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
        });
        // #326 Wind Slap: gust in a cone (8 blocks): 1 heart, big knockback. 8 s
        attack(E, 326, "wind_slap", 160, 8, () -> Items.FEATHER, u -> {
            LivingEntity me = u.entity();
            Vec3 dir = facing(u);
            for (int i = 1; i <= 8; i++) Fx.particles(u.level(), ParticleTypes.CLOUD, me.getEyePosition().add(dir.scale(i)), 3, i * 0.15);
            Fx.sound(me, SoundEvents.ENDER_DRAGON_FLAP, 1f, 1.6f);
            for (LivingEntity t : cone(u, 8, 60)) {
                if (hit(u, t, 1) && !isBoss(t)) knock(t, me.position(), 1.6, 0.4);
            }
        });
        // #327 Clap Wave: thunderous clap: everything within 4 blocks takes 1.5 hearts + Nausea 3 s. 8 s
        attack(E, 327, "clap_wave", 160, 4, () -> Items.NOTE_BLOCK, u -> {
            LivingEntity me = u.entity();
            Fx.sound(me, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.8f);
            Fx.particles(u.level(), ParticleTypes.SONIC_BOOM, me.position().add(0, 1, 0), 1, 0);
            for (LivingEntity t : around(u, me.position(), 4)) {
                if (hit(u, t, 1.5f)) effect(t, MobEffects.CONFUSION, 60, 0);
            }
        });
        // #328 Bone Toss: a thrown bone bounces between up to 3 targets, 1 heart each. 8 s
        attack(E, 328, "bone_toss", 160, 16, () -> Items.BONE, u -> {
            LivingEntity first = aim(u, 16);
            if (first == null) { refund(u); return; }
            swing(u);
            ItemParticleOption bone = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BONE));
            List<LivingEntity> hitOnes = new ArrayList<>();
            Vec3 from = u.entity().getEyePosition();
            LivingEntity t = first;
            for (int i = 0; i < 3 && t != null; i++) {
                trail(u.level(), from, t.getBoundingBox().getCenter(), bone);
                hit(u, t, 1);
                hitOnes.add(t);
                from = t.getBoundingBox().getCenter();
                Vec3 at = from;
                t = around(u, at, 6).stream().filter(e -> !hitOnes.contains(e))
                        .min(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(at))).orElse(null);
            }
            Fx.sound(u.entity(), SoundEvents.SKELETON_HURT, 1f, 1.5f);
        });
        // #329 Counter: when something hits you up close, 1 in 5 chance you hit back at once for 2 hearts
        passive(E, 329, "counter", () -> Items.IRON_SWORD, new PowerEffect() {
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) {
                // only real close-up hits: never a power's hit, so two Counters can't ping-pong forever
                if (!(e.getSource().getEntity() instanceof LivingEntity by) || !isPunch(by, e.getSource()) || by.distanceTo(u.entity()) > 4) return;
                if (u.entity().getRandom().nextInt(5) != 0) return;
                swing(u);
                Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.2f);
                hit(u, by, 2);
            }
        });
        // #330 Heavy Hand: every 4th hit in a row on the same target does 2 extra hearts
        passive(E, 330, "heavy_hand", () -> Items.ANVIL, new PowerEffect() {
            @Override public void boostHit(PowerUser u, LivingEntity target, Boost b) {
                String id = target.getUUID().toString();
                int n = id.equals(u.state().getString("hhTarget")) ? u.state().getInt("hhCount") + 1 : 1;
                u.state().putString("hhTarget", id);
                u.state().putInt("hhCount", n);
                if (n % 4 == 0) {
                    b.add(2);
                    Fx.sound(u.entity(), SoundEvents.ANVIL_LAND, 0.5f, 1.4f);
                }
            }
        });
        // #331 Pounce: leap up to 6 blocks onto your target: 2.5 hearts on landing. 8 s
        attack(E, 331, "pounce", 160, 6, () -> Items.RABBIT_FOOT, u -> {
            LivingEntity t = aim(u, 6);
            if (t == null) { refund(u); return; }
            LivingEntity me = u.entity();
            Vec3 to = t.position().subtract(me.position());
            launch(me, new Vec3(to.x * 0.22, 0.55, to.z * 0.22));
            Fx.sound(me, SoundEvents.CAT_HISS, 1f, 1.2f);
            whenLanded(u, 0, () -> {
                Fx.particles(u.level(), ParticleTypes.POOF, me.position(), 10, 0.6);
                for (LivingEntity e : around(u, me.position(), 2)) hit(u, e, 2.5f);
            });
        });
    }

    // Runs `then` once the holder is back on the ground (checks every tick, gives up after 2 seconds).
    static void whenLanded(PowerUser u, int tries, Runnable then) {
        Later.after(u, 1, () -> {
            if (tries > 3 && (u.entity().onGround() || u.entity().isInWater())) then.run();
            else if (tries < 40) whenLanded(u, tries + 1, then);
        });
    }

    private AttackE() {}
}
