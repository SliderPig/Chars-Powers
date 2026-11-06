package com.example.charpowers.hidden.attack;

import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.*;

import static com.example.charpowers.hidden.attack.AttackF.swing;
import static com.example.charpowers.hidden.attack.AttackHelp.*;
import static com.example.charpowers.hidden.f.FHelp.running;
import static com.example.charpowers.hidden.f.FHelp.startFor;
import static com.example.charpowers.hidden.model.Rank.S;
import static com.example.charpowers.hidden.ranks.RankHelp.launch;
import static com.example.charpowers.hidden.ranks.RankHelp.refund;

// S-rank attack powers (#376-381): 30 to 50 hearts, 5-10 minute cooldowns, 2-3 s warnings.
final class AttackS {
    static final int SLAM_COOLDOWN = 12000; // 10 minutes, counted from the first press

    static void registerAll() {
        // #376 Ground Slam ⛏ (my idea): press once and a 10x10, 3-deep hunk of ground under and around you
        // tears up and floats overhead (the hole stays). Press again within 10 s (or it throws itself at 10 s): it
        // flies at what you look at (up to 40 blocks) and lands where the target WAS (2 s shadow, so it can be
        // dodged). Under the 10x10: 50 hearts. Within 5 blocks of it: 10 and knocked flat. The hunk shatters and
        // leaves a crater. 10 min
        attack(S, 376, "ground_slam", SLAM_COOLDOWN, () -> Items.GRASS_BLOCK, new Move(24) {
            @Override public void onKey(PowerUser u) {
                // (after a crash the saved "up" can outlive the floating hunk: then just lift a new one)
                if (u.state().getBoolean("up") && SLABS.containsKey(u.entity().getUUID())) throwSlab(u);
                else liftSlab(u);
            }
            @Override public void stop(PowerUser u) { shatter(u); }
        });
        // #377 Vanishing Blow: teleport behind your target (64 blocks). Everything else within 10 blocks freezes for
        // 1 s, then the blow lands: 40 hearts. 5 min
        attack(S, 377, "vanishing_blow", 6000, 24, () -> Items.CLOCK, u -> {
            LivingEntity t = aim(u, 64);
            if (t == null || !teleportBehind(u, t)) { refund(u); return; }
            LivingEntity me = u.entity();
            for (LivingEntity e : around(u, t.position(), 10)) {
                if (isBoss(e)) continue;
                hold(u, e, 20);
                effect(e, net.minecraft.world.effect.MobEffects.WEAKNESS, 20, 9); // its hits do nothing for that second
            }
            Fx.sound(me, SoundEvents.BELL_RESONATE, 1.5f, 0.5f);
            Fx.particles(u.level(), ParticleTypes.REVERSE_PORTAL, t.getBoundingBox().getCenter(), 40, 3);
            Later.after(u, 20, () -> {
                if (!t.isAlive()) return;
                swing(u);
                AttackHelp.sound(u, t.position(), SoundEvents.PLAYER_ATTACK_CRIT, 2f, 0.5f);
                Fx.particles(u.level(), ParticleTypes.SWEEP_ATTACK, t.getBoundingBox().getCenter(), 3, 0.5);
                hit(u, t, 40);
            });
        });
        // #378 Planet Cracker ⛏: 2 s wind-up, then punch the ground: crater radius 8, 35 hearts within 8 blocks,
        // 10 within 16. 10 min
        attack(S, 378, "planet_cracker", 12000, 8, () -> Items.BEDROCK, u -> {
            LivingEntity me = u.entity();
            Vec3 at = me.position();
            warn(u, at, 8, 40);
            warn(u, at, 16, 40);
            Later.after(u, 40, () -> {
                swing(u);
                AttackHelp.sound(u, at, SoundEvents.GENERIC_EXPLODE, 3f, 0.4f);
                AttackHelp.sound(u, at, SoundEvents.WARDEN_SONIC_BOOM, 3f, 0.5f);
                Fx.particles(u.level(), ParticleTypes.EXPLOSION_EMITTER, at, 4, 4);
                for (LivingEntity t : around(u, at, 16)) hit(u, t, t.position().distanceTo(at) <= 8 + t.getBbWidth() ? 35 : 10);
                Wreck.blocks(u, Wreck.bowl(BlockPos.containing(at).below(), 8), BlockPos.containing(at));
            });
        });
        // #379 Starfall ⛏: 10 meteors fall over 5 s around your target (radius 15), each with a 2 s shadow:
        // 12 hearts within 3 blocks and a small crater each. 10 min
        attack(S, 379, "starfall", 12000, 24, () -> Items.NETHER_STAR, u -> {
            LivingEntity t = aim(u, 40);
            Vec3 centre = t != null ? t.position() : lookSpot(u, 40);
            Random r = new Random(u.now());
            Fx.sound(u.entity(), SoundEvents.WITHER_SPAWN, 1f, 1.4f);
            for (int i = 0; i < 10; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = i == 0 ? 0 : Math.sqrt(r.nextDouble()) * 15; // the first lands on the target
                Vec3 spot = ground(u.level(), centre.x + Math.cos(a) * d, centre.z + Math.sin(a) * d, centre.y);
                Later.after(u, 1 + i * 10, () -> AttackA.meteor(u, spot, 40, 3, 12, 2, Blocks.MAGMA_BLOCK.defaultBlockState()));
            }
        });
        // #380 Void Rend ⛏: tear a rift at your target (24 blocks): everything within 6 blocks is pulled in for 3 s,
        // then it snaps shut: 40 hearts, and every block within 3 blocks of the rift is erased. 10 min
        attack(S, 380, "void_rend", 12000, 24, () -> Items.END_PORTAL_FRAME, u -> {
            LivingEntity t = aim(u, 24);
            if (t == null) { refund(u); return; }
            Vec3 c = t.getBoundingBox().getCenter();
            warn(u, c.subtract(0, t.getBbHeight() / 2, 0), 6, 60);
            AttackHelp.sound(u, c, SoundEvents.END_PORTAL_SPAWN, 1f, 1.6f);
            for (int tick = 0; tick < 60; tick += 2) {
                Later.after(u, tick + 1, () -> {
                    Fx.particles(u.level(), ParticleTypes.REVERSE_PORTAL, c, 20, 1);
                    Fx.particles(u.level(), ParticleTypes.SQUID_INK, c, 4, 0.3);
                    for (LivingEntity e : around(u, c, 6)) {
                        if (!isBoss(e)) launch(e, c.subtract(e.getBoundingBox().getCenter()).normalize().scale(0.3));
                    }
                });
            }
            Later.after(u, 60, () -> {
                AttackHelp.sound(u, c, SoundEvents.WARDEN_SONIC_BOOM, 2f, 0.3f);
                Fx.particles(u.level(), ParticleTypes.SONIC_BOOM, c, 1, 0);
                for (LivingEntity e : around(u, c, 6)) hit(u, e, 40);
                Wreck.blocks(u, Wreck.sphere(BlockPos.containing(c), 3), BlockPos.containing(c));
            });
        });
        // #381 One Punch: your next punch within 10 s does 50 hearts and sends it flying 30 blocks.
        // If you miss, it's wasted. 10 min
        attack(S, 381, "one_punch", 12000, () -> Items.GOLDEN_APPLE, new Move(4) {
            @Override public void onKey(PowerUser u) {
                startFor(u, "punch", 200);
                Fx.sound(u.entity(), SoundEvents.BEACON_POWER_SELECT, 1f, 0.5f);
                Fx.particles(u.level(), ParticleTypes.ENCHANTED_HIT, u.entity().getEyePosition(), 15, 0.4);
                if (u.mob() != null && u.mob().getTarget() != null) punch(u, u.mob().getTarget()); // mobs punch right away
            }
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                if (!isPunch(u.entity(), e.getSource()) || !running(u, "punch")) return; // a real punch only (no arrows, thorns or powers)
                e.setAmount(0); // the power's own 50-heart hit replaces it (so all the boosts stack on that)
                punch(u, target);
            }
        });
    }

    // One Punch landing (next tick, so it never lands inside another hit).
    private static void punch(PowerUser u, LivingEntity t) {
        u.state().putLong("punch", 0);
        Later.after(u, 1, () -> {
            if (!t.isAlive()) return;
            AttackHelp.sound(u, t.position(), SoundEvents.GENERIC_EXPLODE, 1.5f, 1.4f);
            Fx.particles(u.level(), ParticleTypes.EXPLOSION, t.getBoundingBox().getCenter(), 3, 0.3);
            if (hit(u, t, 50) && !isBoss(t)) knock(t, u.entity().position(), 3.5, 0.6); // about 30 blocks (tune in-game)
        });
    }

    // --- Ground Slam ---

    private record Piece(Display.BlockDisplay d, BlockState state, int dx, int dz) {}

    private static final Map<UUID, List<Piece>> SLABS = new HashMap<>();
    private static final int FLOAT = 6;

    private static void liftSlab(PowerUser u) {
        LivingEntity me = u.entity();
        BlockPos feet = BlockPos.containing(me.position());
        List<Piece> pieces = new ArrayList<>();
        for (int dx = -5; dx <= 4; dx++) for (int dz = -5; dz <= 4; dz++) {
            BlockState s = u.level().getBlockState(feet.offset(dx, -1, dz));
            if (s.isAir() || s.hasBlockEntity() || !s.getFluidState().isEmpty()) continue;
            Display.BlockDisplay d = blockDisplay(u.level(), s, Vec3.atLowerCornerOf(feet.offset(dx, FLOAT, dz)));
            if (d != null) pieces.add(new Piece(d, s, dx, dz));
        }
        if (pieces.isEmpty()) { refund(u); return; } // nothing solid underfoot (air, water)
        SLABS.put(me.getUUID(), pieces);
        Wreck.blocks(u, Wreck.box(feet.offset(-5, -3, -5), feet.offset(4, -1, 4)), feet);
        u.state().putBoolean("up", true);
        u.state().putLong("first", u.now());
        u.state().putLong("cd", u.now() + (u.mob() != null ? 40 : 10)); // the second press is the throw
        Later.cleanup(u, "slab", () -> shatter(u));
        AttackHelp.sound(u, me.position(), SoundEvents.GENERIC_EXPLODE, 1.5f, 0.5f);
        Fx.particles(u.level(), ParticleTypes.EXPLOSION, me.position(), 8, 4);
        follow(u, 0);
    }

    // The hunk floats over the holder; at 10 s it throws itself.
    private static void follow(PowerUser u, int ticks) {
        Later.after(u, 2, () -> {
            if (!u.state().getBoolean("up")) return;
            if (ticks + 2 >= 200) { throwSlab(u); return; }
            BlockPos feet = BlockPos.containing(u.entity().position());
            for (Piece p : SLABS.getOrDefault(u.entity().getUUID(), List.of())) {
                Vec3 at = Vec3.atLowerCornerOf(feet.offset(p.dx(), FLOAT, p.dz()));
                p.d().setPos(at.x, at.y, at.z);
            }
            follow(u, ticks + 2);
        });
    }

    private static void throwSlab(PowerUser u) {
        u.state().putBoolean("up", false);
        u.state().putLong("cd", u.state().getLong("first") + SLAM_COOLDOWN);
        LivingEntity t = aim(u, 40);
        Vec3 aimAt = t != null ? t.position() : lookSpot(u, 40);
        Vec3 spot = ground(u.level(), aimAt.x, aimAt.z, aimAt.y); // where the target IS now: it can still run
        warn(u, spot, 5, 40);
        List<Piece> pieces = SLABS.getOrDefault(u.entity().getUUID(), List.of());
        Map<Piece, Vec3> from = new HashMap<>();
        for (Piece p : pieces) from.put(p, p.d().position());
        Fx.sound(u.entity(), SoundEvents.ENDER_DRAGON_FLAP, 2f, 0.4f);
        for (int i = 1; i <= 40; i++) {
            double f = i / 40.0;
            boolean last = i == 40;
            Later.after(u, i, () -> {
                for (Piece p : pieces) {
                    Vec3 to = spot.add(p.dx(), 0, p.dz());
                    Vec3 at = from.get(p).lerp(to, f).add(0, Math.sin(f * Math.PI) * 4, 0);
                    p.d().setPos(at.x, at.y, at.z);
                }
                if (last) land(u, spot);
            });
        }
    }

    private static void land(PowerUser u, Vec3 spot) {
        Later.done(u, "slab");
        shatter(u);
        AttackHelp.sound(u, spot, SoundEvents.GENERIC_EXPLODE, 3f, 0.5f);
        AttackHelp.sound(u, spot, SoundEvents.ANVIL_LAND, 3f, 0.4f);
        Fx.particles(u.level(), ParticleTypes.EXPLOSION_EMITTER, spot, 4, 3);
        AABB under = new AABB(spot.x - 5, spot.y - 1, spot.z - 5, spot.x + 5, spot.y + 4, spot.z + 5);
        for (LivingEntity t : around(u, spot, 11)) {
            if (t.getBoundingBox().intersects(under)) hit(u, t, 50);
            else if (hit(u, t, 10) && !isBoss(t)) {
                hold(u, t, 20);
                launch(t, new Vec3(0, -1, 0));
            }
        }
        Wreck.blocks(u, Wreck.bowl(BlockPos.containing(spot).below(), 3), BlockPos.containing(spot));
    }

    // The floating hunk breaks apart (thrown and landed, or the holder died / logged off / took the power off).
    private static void shatter(PowerUser u) {
        if (u.state().getBoolean("up")) {
            u.state().putBoolean("up", false);
            u.state().putLong("cd", u.state().getLong("first") + SLAM_COOLDOWN); // the cooldown still counts
        }
        List<Piece> pieces = SLABS.remove(u.entity().getUUID());
        if (pieces == null) return;
        for (Piece p : pieces) {
            if (!p.d().isRemoved()) {
                Fx.particles(u.level(), new BlockParticleOption(ParticleTypes.BLOCK, p.state()), p.d().position().add(0.5, 0.5, 0.5), 4, 0.4);
                p.d().discard();
            }
        }
    }

    static void clear() { SLABS.clear(); }

    private AttackS() {}
}
