package com.example.charpowers.hidden.attack;

import com.example.charpowers.hidden.fx.Boost;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.ranks.RankHelp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.example.charpowers.hidden.attack.AttackF.swing;
import static com.example.charpowers.hidden.attack.AttackHelp.*;
import static com.example.charpowers.hidden.f.FHelp.running;
import static com.example.charpowers.hidden.f.FHelp.startFor;
import static com.example.charpowers.hidden.model.Rank.A;
import static com.example.charpowers.hidden.ranks.RankHelp.launch;
import static com.example.charpowers.hidden.ranks.RankHelp.refund;

// A-rank attack powers (#368-375): 12 to 25 hearts, 2-3 minute cooldowns, 1.5-3 s warnings.
final class AttackA {
    static void registerAll() {
        // #368 Sky Splitter ⛏: 1.5 s warning line on the ground, then a giant blade falls along a 20-block line:
        // 15 hearts, and it cuts a trench 1 wide and 3 deep. 2 min
        attack(A, 368, "sky_splitter", 2400, 20, () -> Items.NETHERITE_SWORD, u -> {
            LivingEntity me = u.entity();
            Vec3 dir = facing(u).multiply(1, 0, 1).normalize();
            Vec3 start = me.position().add(dir.scale(1.5));
            List<Vec3> path = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                Vec3 p = start.add(dir.scale(i));
                path.add(ground(u.level(), p.x, p.z, p.y));
            }
            for (int i = 0; i < 20; i += 3) warn(u, path.get(i), 1, 30);
            Later.after(u, 30, () -> {
                AttackHelp.sound(u, path.get(10), SoundEvents.TRIDENT_THUNDER, 1.5f, 0.6f);
                List<BlockPos> trench = new ArrayList<>();
                for (Vec3 p : path) {
                    Fx.particles(u.level(), ParticleTypes.SWEEP_ATTACK, p.add(0, 1, 0), 1, 0);
                    Fx.particles(u.level(), ParticleTypes.END_ROD, p.add(0, 2, 0), 4, 0.6);
                    for (int d = 1; d <= 3; d++) trench.add(BlockPos.containing(p).below(d));
                }
                for (LivingEntity t : line(u, start.add(0, 1, 0), dir, 20, 1.5)) hit(u, t, 15);
                Wreck.blocks(u, trench, BlockPos.containing(start));
            });
        });
        // #369 Meteor ⛏: call a meteor where you look (40 blocks): 3 s falling shadow, then 18 hearts within 4 blocks
        // and a crater radius 5. 3 min
        attack(A, 369, "meteor", 3600, 24, () -> Items.FIRE_CHARGE, u -> {
            LivingEntity t = aim(u, 40);
            Vec3 aimAt = t != null ? t.position() : lookSpot(u, 40);
            Vec3 spot = ground(u.level(), aimAt.x, aimAt.z, aimAt.y);
            meteor(u, spot, 60, 4, 18, 5, Blocks.MAGMA_BLOCK.defaultBlockState());
        });
        // #370 Vanish Strike: teleport behind up to 5 targets in a row (within 20 blocks), 8 hearts each. 2 min
        attack(A, 370, "vanish_strike", 2400, 20, () -> Items.CHORUS_FRUIT, u -> {
            LivingEntity me = u.entity();
            List<LivingEntity> targets = around(u, me.position(), 20).stream().filter(me::hasLineOfSight)
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(me))).limit(5).toList();
            if (targets.isEmpty()) { refund(u); return; }
            for (int i = 0; i < targets.size(); i++) {
                LivingEntity t = targets.get(i);
                Later.after(u, 1 + i * 6, () -> {
                    if (!t.isAlive() || !teleportBehind(u, t)) return;
                    swing(u);
                    Fx.particles(u.level(), ParticleTypes.SMOKE, t.getBoundingBox().getCenter(), 10, 0.4);
                    hit(u, t, 8);
                });
            }
        });
        // #371 Titan Grip: grab your target (8 blocks), lift it 15 blocks up and slam it down: 15 hearts + the fall.
        // Bosses: 10, no lifting. 2 min
        attack(A, 371, "titan_grip", 2400, 8, () -> Items.NETHERITE_INGOT, u -> {
            LivingEntity t = aim(u, 8);
            if (t == null) { refund(u); return; }
            Fx.sound(u.entity(), SoundEvents.IRON_GOLEM_ATTACK, 1.5f, 0.5f);
            if (isBoss(t)) { hit(u, t, 10); return; }
            launch(t, new Vec3(0, 1.6, 0)); // about 15 blocks (tune in-game)
            Later.after(u, 15, () -> {
                if (!t.isAlive()) return;
                launch(t, new Vec3(0, -3, 0));
                Fx.particles(u.level(), ParticleTypes.CLOUD, t.position(), 10, 0.5);
                hit(u, t, 15);
            });
        });
        // #372 Shockwave Roar ⛏: roar in a 15-block cone: 12 hearts, knocked back 10 blocks, and glass, leaves and
        // plants in the cone shatter. 2 min
        attack(A, 372, "shockwave_roar", 2400, 15, () -> Items.GOAT_HORN, u -> {
            LivingEntity me = u.entity();
            warn(u, me.position().add(facing(u).multiply(1, 0, 1).normalize().scale(4)), 3, 30);
            Fx.sound(me, SoundEvents.RAVAGER_ROAR, 1.5f, 0.6f);
            Later.after(u, 30, () -> {
                Vec3 eye = me.getEyePosition();
                Vec3 dir = facing(u);
                Fx.sound(me, SoundEvents.WARDEN_SONIC_BOOM, 2f, 0.8f);
                for (int s = 2; s <= 15; s += 2) Fx.particles(u.level(), ParticleTypes.SONIC_BOOM, eye.add(dir.scale(s)), 1, 0);
                for (LivingEntity t : cone(u, 15, 60)) if (hit(u, t, 12) && !isBoss(t)) knock(t, me.position(), 2.5, 0.5);
                Wreck.blocks(u, fragileInCone(u.level(), eye, dir, 15, 60), BlockPos.containing(eye));
            });
        });
        // #373 Overcharge Beam ⛏: 2 s charge (you glow), then a 50-block beam 3 wide: 20 hearts, blasts a 3x3 tunnel. 3 min
        attack(A, 373, "overcharge_beam", 3600, 24, () -> Items.END_CRYSTAL, u -> {
            LivingEntity me = u.entity();
            effect(me, MobEffects.GLOWING, 60, 0);
            Fx.sound(me, SoundEvents.BEACON_ACTIVATE, 2f, 0.6f);
            for (int i = 0; i < 8; i++) Later.after(u, 1 + i * 5, () -> Fx.particles(u.level(), ParticleTypes.END_ROD, me.getEyePosition(), 10, 0.8));
            Later.after(u, 40, () -> {
                Vec3 from = me.getEyePosition();
                Vec3 dir = facing(u);
                Vec3 side = new Vec3(-dir.z, 0, dir.x).normalize();
                for (int s = -1; s <= 1; s++) trail(u.level(), from.add(side.scale(s)), from.add(side.scale(s)).add(dir.scale(50)), ParticleTypes.END_ROD);
                Fx.sound(me, SoundEvents.WARDEN_SONIC_BOOM, 2f, 1.2f);
                for (LivingEntity t : line(u, from, dir, 50, 3)) hit(u, t, 20);
                Wreck.blocks(u, Wreck.tunnel(from.add(dir), dir, 50, 3), BlockPos.containing(from));
            });
        });
        // #374 Death Mark: mark your target (24 blocks). If you're still within 50 blocks 5 s later, it takes 20 hearts. 3 min
        attack(A, 374, "death_mark", 3600, 24, () -> Items.WITHER_SKELETON_SKULL, u -> {
            LivingEntity t = aim(u, 24);
            if (t == null) { refund(u); return; }
            effect(t, MobEffects.GLOWING, 100, 0);
            AttackHelp.sound(u, t.position(), SoundEvents.WITHER_SPAWN, 0.5f, 1.6f);
            for (int i = 0; i < 5; i++) Later.after(u, 1 + i * 20, () -> Fx.particles(u.level(), ParticleTypes.SOUL, Fx.head(t), 4, 0.2));
            Later.after(u, 100, () -> {
                if (!t.isAlive() || t.level() != u.level() || t.distanceTo(u.entity()) > 50) return;
                AttackHelp.sound(u, t.position(), SoundEvents.WITHER_BREAK_BLOCK, 1f, 0.6f);
                Fx.particles(u.level(), ParticleTypes.SOUL_FIRE_FLAME, t.getBoundingBox().getCenter(), 30, 0.5);
                hit(u, t, 20);
            });
        });
        // #375 Berserker: for 15 s every hit does 5 extra hearts, you can't be knocked back and you swing faster.
        // Then Weakness for 10 s. 3 min
        attack(A, 375, "berserker", 3600, () -> Items.CRIMSON_STEM, new Move(4) {
            @Override public void onKey(PowerUser u) {
                startFor(u, "rage", 300);
                u.state().putBoolean("raging", true);
                RankHelp.boost(u.entity(), Attributes.ATTACK_SPEED, "berserker", 0.5);
                Fx.sound(u.entity(), SoundEvents.RAVAGER_ROAR, 1f, 1.3f);
            }
            @Override public void tick(PowerUser u) {
                if (!u.state().getBoolean("raging")) return;
                if (running(u, "rage")) {
                    if (u.now() % 10 == 0) Fx.particles(u.level(), ParticleTypes.ANGRY_VILLAGER, Fx.head(u.entity()), 1, 0.3);
                    return;
                }
                u.state().putBoolean("raging", false);
                RankHelp.boost(u.entity(), Attributes.ATTACK_SPEED, "berserker", 0);
                effect(u.entity(), MobEffects.WEAKNESS, 200, 0);
            }
            @Override public void boostHit(PowerUser u, LivingEntity target, Boost b) {
                if (running(u, "rage")) b.add(5);
            }
            @Override public void onKnockback(PowerUser u, LivingKnockBackEvent e) {
                if (running(u, "rage")) e.setStrength(0);
            }
            @Override public void stop(PowerUser u) {
                RankHelp.boost(u.entity(), Attributes.ATTACK_SPEED, "berserker", 0);
                u.state().putBoolean("raging", false);
            }
        });
    }

    // A meteor (a falling block of `look`) lands on `spot` after `ticks` with a warning ring of `radius`:
    // `hearts` within `radius` and a crater of `crater` (0 = none).
    static void meteor(PowerUser u, Vec3 spot, int ticks, double radius, float hearts, int crater, BlockState look) {
        warn(u, spot, radius, ticks);
        Display.BlockDisplay rock = blockDisplay(u.level(), look, spot.add(-0.5, 30, -0.5));
        String key = "meteor" + spot.hashCode() + "_" + u.now();
        if (rock != null) Later.cleanup(u, key, rock::discard);
        for (int i = 1; i <= ticks; i++) {
            double y = 30 - 30.0 * i / ticks;
            boolean last = i == ticks;
            Later.after(u, i, () -> {
                if (rock != null) {
                    rock.setPos(spot.x - 0.5, spot.y + y, spot.z - 0.5);
                    Fx.particles(u.level(), ParticleTypes.LARGE_SMOKE, rock.position().add(0.5, 1, 0.5), 2, 0.3);
                }
                if (!last) return;
                if (rock != null) rock.discard();
                Later.done(u, key);
                AttackHelp.sound(u, spot, SoundEvents.GENERIC_EXPLODE, 2f, 0.6f);
                Fx.particles(u.level(), ParticleTypes.EXPLOSION_EMITTER, spot, 1, 0);
                Fx.particles(u.level(), ParticleTypes.LAVA, spot, 20, radius / 2);
                for (LivingEntity t : around(u, spot, radius)) hit(u, t, hearts);
                if (crater > 0) Wreck.blocks(u, Wreck.bowl(BlockPos.containing(spot).below(), crater), BlockPos.containing(spot));
            });
        }
    }

    // Glass, leaves and plants inside a cone (for Shockwave Roar).
    private static List<BlockPos> fragileInCone(ServerLevel l, Vec3 eye, Vec3 dir, int range, double degrees) {
        double minDot = Math.cos(Math.toRadians(degrees / 2));
        List<BlockPos> out = new ArrayList<>();
        BlockPos c = BlockPos.containing(eye);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-range, -range, -range), c.offset(range, range, range))) {
            Vec3 to = Vec3.atCenterOf(p).subtract(eye);
            if (to.lengthSqr() > range * range || to.normalize().dot(dir) < minDot || !l.isLoaded(p)) continue;
            BlockState s = l.getBlockState(p);
            boolean fragile = s.is(BlockTags.LEAVES) || s.is(Tags.Blocks.GLASS) || s.is(Tags.Blocks.GLASS_PANES)
                    || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || (s.canBeReplaced() && s.getFluidState().isEmpty() && !s.isAir());
            if (fragile) out.add(p.immutable());
        }
        return out;
    }

    private AttackA() {}
}
