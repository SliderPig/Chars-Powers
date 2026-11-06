package com.example.charpowers.hidden.ranks;

import com.example.charpowers.hidden.fx.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

// Arrows with a trick, watched every tick while they fly:
// HOMING curves a little toward mobs, RICOCHET bounces off a wall once at the nearest mob,
// SECOND turns round once when it's about to miss, BOOMERANG comes back to the shooter after it hits (RankEvents).
public final class Arrows {
    public static final String HOMING = "charpowers.homing";
    public static final String RICOCHET = "charpowers.ricochet";
    public static final String SECOND = "charpowers.second";
    public static final String BOOMERANG = "charpowers.boomerang";
    public static final String RETURNED = "charpowers.returned"; // a boomerang arrow that's already been given back

    private static final Set<AbstractArrow> FLYING = Collections.newSetFromMap(new WeakHashMap<>());

    static void track(AbstractArrow a, String trick) {
        a.addTag(trick);
        FLYING.add(a);
    }

    // It hit something for real (RankEvents): its tricks are over. (A stuck arrow can't be told apart by speed alone.)
    static void landed(AbstractArrow a) { FLYING.remove(a); }

    // Called at the end of every world tick (RankEvents).
    static void tick(ServerLevel level) {
        if (FLYING.isEmpty()) return;
        for (AbstractArrow a : FLYING.toArray(new AbstractArrow[0])) {
            if (a.level() != level) continue;
            if (a.isRemoved()) { FLYING.remove(a); continue; }
            if (a.getTags().contains(RETURNED)) { a.discard(); FLYING.remove(a); continue; }
            Vec3 v = a.getDeltaMovement();
            if (v.lengthSqr() < 0.01) { FLYING.remove(a); continue; } // stuck in the ground: tricks are over
            if (a.getTags().contains(HOMING)) steer(a, v);
            boolean bounce = a.getTags().contains(RICOCHET), again = a.getTags().contains(SECOND);
            if (bounce || again) aboutToHitBlock(a, v, bounce);
        }
    }

    private static void steer(AbstractArrow a, Vec3 v) {
        LivingEntity t = nearestTarget(a, a.position(), 8, v);
        if (t == null) return;
        Vec3 to = t.getBoundingBox().getCenter().subtract(a.position()).normalize();
        double speed = v.length();
        a.setDeltaMovement(v.normalize().scale(0.85).add(to.scale(0.15)).normalize().scale(speed));
    }

    // If the arrow would hit a block during its next move, turn it now (the arrow hasn't stuck in anything yet).
    private static void aboutToHitBlock(AbstractArrow a, Vec3 v, boolean ricochet) {
        Vec3 from = a.position(), to = from.add(v);
        BlockHitResult hit = a.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, a));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        Vec3 at = hit.getLocation();
        LivingEntity t = nearestTarget(a, at, 16, null);
        if (t == null && !ricochet) return; // Second Chance only turns round if there's something to try again at
        double speed = Math.max(v.length() * 0.8, 1.0);
        Vec3 dir;
        if (t != null) {
            dir = t.getBoundingBox().getCenter().subtract(at).normalize();
        } else {
            Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            dir = v.subtract(n.scale(2 * v.dot(n))).normalize(); // a plain bounce off the wall
        }
        Vec3 off = Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.2);
        a.setPos(at.x + off.x, at.y + off.y, at.z + off.z);
        a.setDeltaMovement(dir.scale(speed));
        a.hurtMarked = true;
        a.removeTag(ricochet ? RICOCHET : SECOND); // once only
        if (ricochet) a.removeTag(SECOND);
        Fx.particles((ServerLevel) a.level(), ParticleTypes.CRIT, at, 6, 0.1);
        a.level().playSound(null, at.x, at.y, at.z, SoundEvents.ARROW_HIT, a.getSoundSource(), 0.6f, 1.6f);
    }

    // The closest mob worth aiming at (not the shooter, not their pets or other players), optionally only ones ahead.
    @Nullable
    private static LivingEntity nearestTarget(AbstractArrow a, Vec3 from, double range, @Nullable Vec3 ahead) {
        Entity owner = a.getOwner();
        LivingEntity best = null;
        double bestD = range * range;
        for (LivingEntity e : a.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(range))) {
            if (e == owner || !e.isAlive() || !(e instanceof Mob)) continue;
            if (owner instanceof Player && e instanceof TamableAnimal t && t.isTame()) continue;
            if (owner instanceof Mob && !(e instanceof Enemy) && !(owner instanceof Mob m && m.getTarget() == e)) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(from);
            if (ahead != null && to.normalize().dot(ahead.normalize()) < 0.6) continue;
            double d = to.lengthSqr();
            if (d < bestD && RankHelp.clearPath(a, from, e.getBoundingBox().getCenter())) { bestD = d; best = e; }
        }
        // a mob's arrows aim for the player it's fighting
        if (best == null && owner instanceof Mob m && m.getTarget() != null && m.getTarget().distanceToSqr(from) < range * range) best = m.getTarget();
        return best;
    }

    private Arrows() {}
}
