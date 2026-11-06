package com.example.charpowers.hidden.attack;

import com.example.charpowers.hidden.fx.Boost;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static com.example.charpowers.hidden.attack.AttackE.whenLanded;
import static com.example.charpowers.hidden.attack.AttackHelp.*;
import static com.example.charpowers.hidden.model.Rank.C;
import static com.example.charpowers.hidden.ranks.RankHelp.launch;
import static com.example.charpowers.hidden.ranks.RankHelp.refund;
import static com.example.charpowers.hidden.ranks.RankHelp.teleport;

// C-rank attack powers (#346-357): 4 to 8 hearts, 15-40 s cooldowns. Big-area ones warn for half a second first.
final class AttackC {
    static void registerAll() {
        // #346 Meteor Punch ⛏: jump 10 blocks up and slam down where you look (20 blocks):
        // 6 hearts within 4 blocks, crater radius 2. 30 s
        attack(C, 346, "meteor_punch", 600, 20, () -> Items.MAGMA_BLOCK, u -> {
            LivingEntity me = u.entity();
            Vec3 aimAt = lookSpot(u, 20);
            Vec3 spot = ground(u.level(), aimAt.x, aimAt.z, aimAt.y);
            warn(u, spot, 4, 10);
            launch(me, new Vec3(0, 1.4, 0));
            Fx.sound(me, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 0.7f);
            Later.after(u, 10, () -> {
                teleport(me, highestRoom(me, spot, 6)); // never inside a ceiling
                launch(me, new Vec3(0, -2, 0));
                whenLanded(u, 0, () -> {
                    me.fallDistance = 0;
                    Vec3 at = me.position();
                    AttackHelp.sound(u, at, SoundEvents.GENERIC_EXPLODE, 1.5f, 0.8f);
                    Fx.particles(u.level(), ParticleTypes.EXPLOSION, at, 6, 1.5);
                    for (LivingEntity t : around(u, at, 4)) hit(u, t, 6);
                    Wreck.blocks(u, Wreck.bowl(BlockPos.containing(at).below(), 2), BlockPos.containing(at));
                });
            });
        });
        // #347 Chain Zap: a zap jumps between up to 5 targets (12 blocks apart), 4 hearts each. No bolt, no flash.
        attack(C, 347, "chain_zap", 500, 16, () -> Items.COPPER_INGOT, u -> {
            LivingEntity t = aim(u, 16);
            if (t == null) { refund(u); return; }
            List<LivingEntity> zapped = new ArrayList<>();
            Vec3 from = u.entity().getEyePosition();
            for (int i = 0; i < 5 && t != null; i++) {
                trail(u.level(), from, t.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK);
                hit(u, t, 4);
                zapped.add(t);
                Vec3 at = from = t.getBoundingBox().getCenter();
                t = around(u, at, 12).stream().filter(e -> !zapped.contains(e))
                        .min(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(at))).orElse(null);
            }
            Fx.sound(u.entity(), SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4f, 1.8f);
        });
        // #348 Blade Storm: 8 spinning swords circle you for 5 s; anything they touch takes 2 hearts (at most once a second each). 30 s
        attack(C, 348, "blade_storm", 600, 3, () -> Items.IRON_SWORD, u -> {
            LivingEntity me = u.entity();
            List<Display.ItemDisplay> blades = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                Display.ItemDisplay d = itemDisplay(u.level(), new ItemStack(Items.IRON_SWORD), me.position());
                if (d != null) blades.add(d);
            }
            Runnable gone = () -> blades.forEach(Display.ItemDisplay::discard);
            Later.cleanup(u, "blades", gone);
            Map<LivingEntity, Long> lastHit = new HashMap<>();
            Fx.sound(me, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.6f);
            for (int tick = 0; tick <= 100; tick += 2) {
                int at = tick;
                Later.after(u, tick + 1, () -> {
                    for (int i = 0; i < blades.size(); i++) {
                        double a = Math.PI * 2 * i / blades.size() + at * 0.25;
                        blades.get(i).setPos(me.getX() + Math.cos(a) * 2, me.getY() + 0.8, me.getZ() + Math.sin(a) * 2);
                    }
                    for (LivingEntity t : around(u, me.position().add(0, 1, 0), 2.6)) {
                        if (u.now() - lastHit.getOrDefault(t, -100L) < 20) continue;
                        lastHit.put(t, u.now());
                        hit(u, t, 2);
                    }
                    if (at == 100) {
                        gone.run();
                        Later.done(u, "blades");
                    }
                });
            }
        });
        // #349 Dragon Breath: 3 s of dragon breath, cone 8 blocks: 6 hearts total, leaves a lingering purple cloud. 30 s
        attack(C, 349, "dragon_breath", 600, 8, () -> Items.DRAGON_BREATH, u -> {
            for (int i = 0; i < 6; i++) {
                Later.after(u, 1 + i * 10, () -> {
                    LivingEntity me = u.entity();
                    Vec3 dir = facing(u);
                    for (int s = 1; s <= 8; s++) Fx.particles(u.level(), ParticleTypes.DRAGON_BREATH, me.getEyePosition().add(dir.scale(s)), 6, s * 0.15);
                    Fx.sound(me, SoundEvents.ENDER_DRAGON_SHOOT, 0.7f, 1f);
                    for (LivingEntity t : cone(u, 8, 50)) hit(u, t, 1);
                });
            }
            Vec3 cloud = u.entity().getEyePosition().add(facing(u).scale(5));
            for (int i = 0; i < 12; i++) Later.after(u, 60 + i * 5, () -> Fx.particles(u.level(), ParticleTypes.DRAGON_BREATH, cloud, 12, 1.5));
        });
        // #350 Shadow Step: teleport behind your target (24 blocks): 6 hearts + Blindness 2 s. 25 s
        attack(C, 350, "shadow_step", 500, 24, () -> Items.BLACK_DYE, u -> {
            LivingEntity t = aim(u, 24);
            if (t == null || !teleportBehind(u, t)) { refund(u); return; }
            Fx.particles(u.level(), ParticleTypes.SMOKE, u.entity().position().add(0, 1, 0), 20, 0.4);
            if (hit(u, t, 6)) effect(t, MobEffects.BLINDNESS, 40, 0);
        });
        // #351 Earthquake ⛏: half a second of rumble, then the ground within 8 blocks cracks: 5 hearts and
        // everything thrown up. Random top blocks in the area break. 40 s
        attack(C, 351, "earthquake", 800, 8, () -> Items.CRACKED_STONE_BRICKS, u -> {
            LivingEntity me = u.entity();
            Vec3 at = me.position();
            warn(u, at, 8, 10);
            Later.after(u, 10, () -> {
                AttackHelp.sound(u, at, SoundEvents.GENERIC_EXPLODE, 1.5f, 0.5f);
                for (LivingEntity t : around(u, at, 8)) if (hit(u, t, 5) && !isBoss(t)) launch(t, new Vec3(0, 0.7, 0));
                Random r = new Random(u.now());
                List<BlockPos> cracks = new ArrayList<>();
                for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                    if (x * x + z * z > 64 || r.nextInt(4) != 0) continue;
                    Vec3 top = ground(u.level(), at.x + x, at.z + z, at.y);
                    cracks.add(BlockPos.containing(top).below());
                }
                Wreck.blocks(u, cracks, BlockPos.containing(at));
            });
        });
        // #352 Ice Spear: huge ice spear (24 blocks): 7 hearts and frozen still for 3 s. 25 s
        attack(C, 352, "ice_spear", 500, 24, () -> Items.PACKED_ICE, u -> {
            LivingEntity t = aim(u, 24);
            if (t == null) { refund(u); return; }
            trail(u.level(), u.entity().getEyePosition(), t.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE);
            AttackHelp.sound(u, t.position(), SoundEvents.GLASS_BREAK, 1.2f, 0.6f);
            if (!hit(u, t, 7)) return;
            if (!isBoss(t)) hold(u, t, 60);
            t.setTicksFrozen(Math.max(t.getTicksFrozen(), 200));
        });
        // #353 Volley: 12 arrows rain down around your target (radius 3), 1 heart each. They can't be picked up.
        attack(C, 353, "volley", 500, 24, () -> Items.ARROW, u -> {
            LivingEntity t = aim(u, 24);
            Vec3 centre = t != null ? t.position() : lookSpot(u, 24);
            Random r = new Random(u.now());
            Fx.sound(u.entity(), SoundEvents.ARROW_SHOOT, 1f, 0.6f);
            for (int i = 0; i < 12; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * 3;
                Vec3 land = ground(u.level(), centre.x + Math.cos(a) * d, centre.z + Math.sin(a) * d, centre.y);
                fallingItem(u, new ItemStack(Items.ARROW), land, i * 2, 1, 1, SoundEvents.ARROW_HIT);
            }
        });
        // #354 Bone Cage: bone blocks cage your target for 3 s, then burst inward: 5 hearts. The cage is removed. 30 s
        attack(C, 354, "bone_cage", 600, 16, () -> Items.BONE_BLOCK, u -> {
            LivingEntity t = aim(u, 16);
            if (t == null) { refund(u); return; }
            if (!isBoss(t)) hold(u, t, 60);
            BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
            List<Display.BlockDisplay> cage = new ArrayList<>();
            Vec3 c = t.position();
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                for (int y = 0; y < 2; y++) {
                    Display.BlockDisplay d = blockDisplay(u.level(), bone, c.add(o[0] - 0.5, y, o[1] - 0.5));
                    if (d != null) cage.add(d);
                }
            }
            Display.BlockDisplay lid = blockDisplay(u.level(), bone, c.add(-0.5, 2, -0.5));
            if (lid != null) cage.add(lid);
            Runnable gone = () -> cage.forEach(Display.BlockDisplay::discard);
            Later.cleanup(u, "cage", gone);
            AttackHelp.sound(u, c, SoundEvents.SKELETON_HURT, 1.5f, 0.5f);
            Later.after(u, 60, () -> {
                gone.run();
                Later.done(u, "cage");
                Fx.particles(u.level(), new BlockParticleOption(ParticleTypes.BLOCK, bone), c.add(0, 1, 0), 40, 0.8);
                AttackHelp.sound(u, c, SoundEvents.BONE_BLOCK_BREAK, 1.5f, 0.6f);
                if (t.isAlive()) hit(u, t, 5);
            });
        });
        // #355 Rampage: each kill within 10 s of the last adds 1 heart to your hits (up to 5). It resets after 10 s with no kill.
        passive(C, 355, "rampage", () -> Items.RED_DYE, new PowerEffect() {
            @Override public void onKill(PowerUser u, LivingEntity victim) {
                boolean going = u.now() - u.state().getLong("lastKill") < 200;
                u.state().putInt("kills", going ? Math.min(5, u.state().getInt("kills") + 1) : 1);
                u.state().putLong("lastKill", u.now());
            }
            @Override public void boostHit(PowerUser u, LivingEntity target, Boost b) {
                if (u.now() - u.state().getLong("lastKill") < 200) b.add(u.state().getInt("kills"));
            }
        });
        // #356 Execute: hits on anything under a quarter health do double damage
        passive(C, 356, "execute", () -> Items.NETHERITE_AXE, new PowerEffect() {
            @Override public void boostHit(PowerUser u, LivingEntity target, Boost b) {
                if (target.getHealth() < target.getMaxHealth() / 4) b.times(2);
            }
        });
        // #357 Wrecking Ball ⛏: a heavy iron ball rolls 15 blocks forward, smashing through blocks in its path
        // (2 wide, 2 tall) and hitting everything for 6 hearts. 40 s
        attack(C, 357, "wrecking_ball", 800, 15, () -> Items.IRON_BLOCK, u -> {
            LivingEntity me = u.entity();
            Vec3 dir = facing(u).multiply(1, 0, 1).normalize();
            Vec3 start = me.position().add(dir.scale(1.5));
            warn(u, start, 1.5, 10);
            Display.BlockDisplay ball = blockDisplay(u.level(), Blocks.IRON_BLOCK.defaultBlockState(), start.add(-0.5, 0, -0.5));
            if (ball != null) Later.cleanup(u, "ball", ball::discard);
            Set<LivingEntity> hitOnes = new HashSet<>();
            Later.after(u, 10, () -> Wreck.blocks(u, Wreck.tunnel(start.add(0, 1, 0), dir, 15, 2), BlockPos.containing(start)));
            for (int i = 1; i <= 15; i++) {
                Vec3 at = start.add(dir.scale(i));
                boolean last = i == 15;
                Later.after(u, 10 + i, () -> {
                    if (ball != null) ball.setPos(at.x - 0.5, at.y, at.z - 0.5);
                    AttackHelp.sound(u, at, SoundEvents.ANVIL_LAND, 0.4f, 0.5f);
                    for (LivingEntity t : around(u, at.add(0, 0.5, 0), 1.6)) {
                        if (hitOnes.add(t) && hit(u, t, 6) && !isBoss(t)) knock(t, at.subtract(dir), 1.2, 0.3);
                    }
                    if (last) {
                        if (ball != null) ball.discard();
                        Later.done(u, "ball");
                    }
                });
            }
        });
    }

    // One arrow of Volley / sword of Sword Rain: falls from 10 blocks up onto `land` after `delay` ticks, then hits
    // what's within `radius` for `hearts`. (A shown item, not a real arrow, so it can never hit the holder's team.)
    static void fallingItem(PowerUser u, ItemStack stack, Vec3 land, int delay, double radius, float hearts, net.minecraft.sounds.SoundEvent sound) {
        Later.after(u, 1 + delay, () -> {
            Display.ItemDisplay d = itemDisplay(u.level(), stack, land.add(0, 10, 0));
            String key = "falling" + land.hashCode() + "_" + delay;
            if (d != null) Later.cleanup(u, key, d::discard);
            for (int i = 1; i <= 8; i++) {
                double y = 10 - 10 * i / 8.0;
                boolean last = i == 8;
                Later.after(u, i, () -> {
                    if (d != null) d.setPos(land.x, land.y + y, land.z);
                    if (!last) return;
                    if (d != null) d.discard();
                    Later.done(u, key);
                    Fx.particles(u.level(), ParticleTypes.CRIT, land, 4, 0.2);
                    AttackHelp.sound(u, land, sound, 0.8f, 1f);
                    for (LivingEntity t : around(u, land.add(0, 0.5, 0), radius)) hit(u, t, hearts);
                });
            }
        });
    }

    private AttackC() {}
}
