package com.example.charpowers.hidden.attack;

import com.example.charpowers.hidden.fx.Boost;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.example.charpowers.hidden.attack.AttackF.swing;
import static com.example.charpowers.hidden.attack.AttackHelp.*;
import static com.example.charpowers.hidden.model.Rank.D;
import static com.example.charpowers.hidden.ranks.RankHelp.launch;
import static com.example.charpowers.hidden.ranks.RankHelp.refund;
import static com.example.charpowers.hidden.ranks.RankHelp.teleport;

// D-rank attack powers (#332-345): 2 to 5 hearts, 8-20 s cooldowns.
final class AttackD {
    static void registerAll() {
        // #332 Blink Strike: teleport behind what you look at (16 blocks) and hit it: 4 hearts. 15 s
        attack(D, 332, "blink_strike", 300, 16, () -> Items.ENDER_PEARL, u -> {
            LivingEntity t = aim(u, 16);
            if (t == null || !teleportBehind(u, t)) { refund(u); return; }
            swing(u);
            Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1f);
            hit(u, t, 4);
        });
        // #333 Boulder Toss ⛏: tear up a 2x2x2 chunk of ground beside you and throw it (20 blocks):
        // 4 hearts where it lands (radius 2). The hole stays. 15 s
        attack(D, 333, "boulder_toss", 300, 20, () -> Items.MOSSY_COBBLESTONE, u -> {
            LivingEntity me = u.entity();
            LivingEntity t = aim(u, 20);
            Vec3 land = t != null ? t.position() : lookSpot(u, 20);
            Vec3 dir = facing(u).multiply(1, 0, 1).normalize();
            BlockPos front = BlockPos.containing(me.position().add(dir.scale(2)));
            BlockState top = me.level().getBlockState(front.below());
            if (top.isAir()) top = Blocks.STONE.defaultBlockState();
            Wreck.blocks(u, Wreck.box(front.below(2), front.below().offset(1, 0, 1)), front);
            throwBlock(u, top, Vec3.atCenterOf(front), land, 10, at -> {
                AttackHelp.sound(u, at, SoundEvents.STONE_BREAK, 1.5f, 0.6f);
                Fx.particles(u.level(), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), at, 30, 1);
                for (LivingEntity e : around(u, at, 2)) hit(u, e, 4);
            });
        });
        // #334 Flame Breath: 3 s of fire breath, cone 5 blocks: half a heart every half second + on fire. 15 s
        attack(D, 334, "flame_breath", 300, 5, () -> Items.BLAZE_POWDER, u -> {
            for (int i = 0; i < 6; i++) {
                Later.after(u, 1 + i * 10, () -> {
                    LivingEntity me = u.entity();
                    Vec3 dir = facing(u);
                    for (int s = 1; s <= 5; s++) Fx.particles(u.level(), ParticleTypes.FLAME, me.getEyePosition().add(dir.scale(s)), 4, s * 0.15);
                    Fx.sound(me, SoundEvents.BLAZE_SHOOT, 0.6f, 0.8f);
                    for (LivingEntity t : cone(u, 5, 50)) if (hit(u, t, 0.5f)) t.setSecondsOnFire(3);
                });
            }
        });
        // #335 Shock Palm: palm strike (3 blocks): 4 hearts, then a zap jumps to 2 things nearby for 2 each. No flash.
        attack(D, 335, "shock_palm", 300, 4, () -> Items.LIGHTNING_ROD, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            swing(u);
            AttackHelp.sound(u, t.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.6f);
            hit(u, t, 4);
            Vec3 from = t.getBoundingBox().getCenter();
            around(u, from, 6).stream().filter(e -> e != t)
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(t))).limit(2)
                    .forEach(e -> {
                        trail(u.level(), from, e.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK);
                        hit(u, e, 2);
                    });
        });
        // #336 Earth Spikes: dripstone spikes burst up under your target (16 blocks): 4 hearts. They crumble after 3 s.
        attack(D, 336, "earth_spikes", 300, 16, () -> Items.POINTED_DRIPSTONE, u -> {
            LivingEntity t = aim(u, 16);
            if (t == null) { refund(u); return; }
            Vec3 at = ground(u.level(), t.getX(), t.getZ(), t.getY());
            BlockState spike = Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP);
            List<Display.BlockDisplay> spikes = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2 * i;
                Display.BlockDisplay d = blockDisplay(u.level(), spike, at.add(Math.cos(a) * 0.6 - 0.5, -0.6, Math.sin(a) * 0.6 - 0.5));
                if (d != null) spikes.add(d);
            }
            Runnable gone = () -> spikes.forEach(d -> {
                Fx.particles(u.level(), new BlockParticleOption(ParticleTypes.BLOCK, spike), d.position(), 6, 0.3);
                d.discard();
            });
            Later.cleanup(u, "spikes", gone);
            Later.after(u, 2, () -> {
                spikes.forEach(d -> d.setPos(d.getX(), d.getY() + 0.9, d.getZ()));
                AttackHelp.sound(u, at, SoundEvents.POINTED_DRIPSTONE_LAND, 1.5f, 0.7f);
                for (LivingEntity e : around(u, at.add(0, 0.5, 0), 1.6)) if (hit(u, e, 4) && !isBoss(e)) launch(e, new Vec3(0, 0.45, 0));
            });
            Later.after(u, 60, () -> {
                gone.run();
                Later.done(u, "spikes");
            });
        });
        // #337 Whirlwind: spin for 3 s, hitting everything within 3 blocks for 1 heart every half second. 15 s
        attack(D, 337, "whirlwind", 300, 3, () -> Items.IRON_AXE, u -> {
            for (int i = 0; i < 6; i++) {
                Later.after(u, 1 + i * 10, () -> {
                    LivingEntity me = u.entity();
                    me.setYRot(me.getYRot() + 120);
                    swing(u);
                    Fx.particles(u.level(), ParticleTypes.SWEEP_ATTACK, me.position().add(0, 1, 0), 4, 1.3);
                    Fx.sound(me, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f);
                    for (LivingEntity t : around(u, me.position(), 3)) hit(u, t, 1);
                });
            }
        });
        // #338 Shockwave Stomp: everything within 5 blocks takes 3 hearts and is knocked up. 15 s
        attack(D, 338, "shockwave_stomp", 300, 5, () -> Items.NETHERITE_BOOTS, u -> {
            LivingEntity me = u.entity();
            Fx.sound(me, SoundEvents.GENERIC_EXPLODE, 0.8f, 0.7f);
            AttackHelp.ring(u.level(), me.position(), 2.5);
            AttackHelp.ring(u.level(), me.position(), 5);
            for (LivingEntity t : around(u, me.position(), 5)) if (hit(u, t, 3) && !isBoss(t)) launch(t, new Vec3(0, 0.6, 0));
        });
        // #339 Piercing Bolt: a bolt goes through everything in a 30-block line: 3 hearts each. 12 s
        attack(D, 339, "piercing_bolt", 240, 24, () -> Items.SPECTRAL_ARROW, u -> {
            LivingEntity me = u.entity();
            Vec3 dir = facing(u);
            Vec3 from = me.getEyePosition();
            trail(u.level(), from, from.add(dir.scale(30)), ParticleTypes.END_ROD);
            Fx.sound(me, SoundEvents.TRIDENT_THROW, 1f, 1.5f);
            for (LivingEntity t : line(u, from, dir, 30, 1)) hit(u, t, 3);
        });
        // #340 Iron Fist: your hits do 1.5 extra hearts and knock shields down like an axe does
        passive(D, 340, "iron_fist", () -> Items.IRON_BLOCK, new PowerEffect() {
            @Override public void boostHit(PowerUser u, LivingEntity target, Boost b) { b.add(1.5f); }
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                if (!POWER_HIT && target instanceof Player p && p.isBlocking()) p.disableShield(true);
            }
        });
        // #341 Bleed: your hits make them bleed: half a heart a second for 3 s (up to 3 bleeds at once)
        passive(D, 341, "bleed", () -> Items.REDSTONE, new PowerEffect() {
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                if (BLEEDING) return; // a bleed tick never starts another bleed
                int n = BLEEDS.getOrDefault(target, 0);
                if (n >= 3) return;
                BLEEDS.put(target, n + 1);
                // if the bleeder dies / logs out mid-bleed, the count still goes back down
                String key = "bleed" + target.getUUID() + "_" + u.now() + "_" + n;
                Later.cleanup(u, key, () -> BLEEDS.merge(target, -1, (a, b) -> a + b <= 0 ? null : a + b));
                for (int s = 1; s <= 3; s++) {
                    boolean last = s == 3;
                    Later.after(u, s * 20, () -> {
                        if (target.isAlive()) {
                            Fx.particles(u.level(), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), target.getBoundingBox().getCenter(), 6, 0.3);
                            BLEEDING = true;
                            try { hit(u, target, 0.5f); } finally { BLEEDING = false; }
                        }
                        if (last) {
                            BLEEDS.merge(target, -1, (a, b) -> a + b <= 0 ? null : a + b);
                            Later.done(u, key);
                        }
                    });
                }
            }
        });
        // #342 Grab and Throw: grab what you look at (3 blocks) and hurl it 10 blocks: 3 hearts + the fall/wall. Not bosses.
        attack(D, 342, "grab_and_throw", 300, 4, () -> Items.LEAD, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            swing(u);
            Fx.sound(u.entity(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.6f);
            if (!hit(u, t, 3) || isBoss(t)) return;
            Vec3 dir = facing(u).multiply(1, 0, 1).normalize();
            launch(t, new Vec3(dir.x * 1.8, 0.7, dir.z * 1.8)); // about 10 blocks (tune in-game)
        });
        // #343 Venom Spit: spit venom (15 blocks): 1 heart + Poison II 5 s. 12 s
        attack(D, 343, "venom_spit", 240, 15, () -> Items.SPIDER_EYE, u -> {
            LivingEntity t = aim(u, 15);
            if (t == null) { refund(u); return; }
            trail(u.level(), u.entity().getEyePosition(), t.getEyePosition(), ParticleTypes.ITEM_SLIME);
            Fx.sound(u.entity(), SoundEvents.LLAMA_SPIT, 1f, 0.7f);
            if (hit(u, t, 1)) effect(t, MobEffects.POISON, 100, 1);
        });
        // #344 Lunge: dash 8 blocks forward; everything you pass through takes 3 hearts. 12 s
        attack(D, 344, "lunge", 240, 8, () -> Items.TRIDENT, u -> {
            LivingEntity me = u.entity();
            Vec3 dir = facing(u).multiply(1, 0, 1).normalize();
            Vec3 from = me.position().add(0, 0.5, 0);
            Vec3 head = me.position().add(0, me.getBbHeight() - 0.1, 0);
            double free = Math.min(reach(me, from, dir), reach(me, head, dir)) - 0.6; // a little short of any wall
            Vec3 stop = me.position();
            for (double d = free; d >= 0.5; d -= 0.25) {
                Vec3 at = me.position().add(dir.scale(d));
                if (roomFor(me, at)) { stop = at; break; }
            }
            if (stop == me.position()) { refund(u); return; } // a wall right in front: nowhere to go
            Vec3 end = from.add(stop.subtract(me.position()));
            List<LivingEntity> passed = line(u, from, dir, from.distanceTo(end), 1.2);
            trail(u.level(), from, stop.add(0, 0.5, 0), ParticleTypes.CLOUD);
            teleport(me, stop);
            Fx.sound(me, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.5f);
            for (LivingEntity t : passed) hit(u, t, 3);
        });
        // #345 Crushing Grip: hold your target (3 blocks) still for 2 s: 2 hearts a second (4 total). 20 s
        attack(D, 345, "crushing_grip", 400, 4, () -> Items.PISTON, u -> {
            LivingEntity t = aim(u, 3);
            if (t == null) { refund(u); return; }
            if (!isBoss(t)) hold(u, t, 40);
            Fx.sound(u.entity(), SoundEvents.IRON_GOLEM_ATTACK, 1f, 0.8f);
            for (int i = 1; i <= 4; i++) {
                Later.after(u, i * 10, () -> {
                    if (!t.isAlive()) return;
                    Fx.particles(u.level(), ParticleTypes.CRIT, t.getBoundingBox().getCenter(), 6, 0.3);
                    hit(u, t, 1);
                });
            }
        });
    }

    // bleeds running on each target (not saved: if the bleeder dies, its bleeds just stop and never count again)
    private static final java.util.Map<LivingEntity, Integer> BLEEDS = new java.util.WeakHashMap<>();
    private static boolean BLEEDING = false;

    // How far you can go from `from` along `dir` (up to 8 blocks) before a block.
    private static double reach(LivingEntity me, Vec3 from, Vec3 dir) {
        return me.level().clip(new ClipContext(from, from.add(dir.scale(8)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, me)).getLocation().distanceTo(from);
    }

    // Throws a block (shown as a floating block) from `from` to `to` over `ticks`, then runs `landed` there.
    // Removed (no landing) if the holder's plans are cancelled.
    static void throwBlock(PowerUser u, BlockState state, Vec3 from, Vec3 to, int ticks, java.util.function.Consumer<Vec3> landed) {
        Display.BlockDisplay d = blockDisplay(u.level(), state, from.add(-0.5, 0, -0.5));
        String key = "thrown" + u.now() + "_" + from.hashCode();
        if (d != null) Later.cleanup(u, key, d::discard);
        for (int i = 1; i <= ticks; i++) {
            double f = i / (double) ticks;
            Vec3 at = from.lerp(to, f).add(0, Math.sin(f * Math.PI) * 3, 0); // a little arc
            boolean last = i == ticks;
            Later.after(u, i, () -> {
                if (d != null) d.setPos(at.x - 0.5, at.y, at.z - 0.5);
                if (!last) return;
                if (d != null) d.discard();
                Later.done(u, key);
                landed.accept(to);
            });
        }
    }

    private AttackD() {}
}
