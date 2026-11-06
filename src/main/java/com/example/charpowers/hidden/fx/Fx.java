package com.example.charpowers.hidden.fx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

// Small helpers every power uses, so each power's own file stays short.
public final class Fx {
    public static final int TALK_RANGE = 16;

    public static void sound(Entity at, SoundEvent s, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), s, at.getSoundSource(), volume, pitch);
    }

    public static void sound(Entity at, Supplier<SoundEvent> s, float volume, float pitch) {
        sound(at, s.get(), volume, pitch);
    }

    // Plays only for one player (e.g. a ghost "boo" only they hear), at a spot.
    public static void soundFor(ServerPlayer p, SoundEvent s, Vec3 at, float volume, float pitch) {
        p.playNotifySound(s, SoundSource.HOSTILE, volume, pitch);
    }

    // Sent "forced" to each player near enough to see them: Minecraft's own sending is skipped by players whose
    // Particles setting is Minimal (mine is), and these little puffs are the whole point of many powers.
    public static void particles(ServerLevel l, ParticleOptions p, Vec3 pos, int count, double spread) {
        for (ServerPlayer sp : l.players()) {
            if (sp.position().distanceToSqr(pos) < PARTICLE_RANGE * PARTICLE_RANGE) {
                l.sendParticles(sp, p, true, pos.x, pos.y, pos.z, count, spread, spread, spread, 0.02);
            }
        }
    }

    private static final double PARTICLE_RANGE = 48;

    public static Vec3 head(Entity e) { return e.position().add(0, e.getBbHeight() + 0.3, 0); }

    // A mob "talks": <Zombie> text, to players near it.
    public static void mobSay(Mob m, String text) {
        Component line = Component.literal("<").append(m.getType().getDescription()).append("> " + text);
        for (Player p : near(m, TALK_RANGE)) p.sendSystemMessage(line);
    }

    // A player "talks": a normal-looking chat line with their name, to everyone.
    public static void playerSay(ServerPlayer p, String text) {
        p.server.getPlayerList().broadcastSystemMessage(Component.literal("<" + p.getGameProfile().getName() + "> " + text), false);
    }

    // Lets the holder say a line whether it's a player or a mob.
    public static void say(PowerUser u, String text) {
        if (u.player() != null) playerSay(u.player(), text);
        else if (u.mob() != null) mobSay(u.mob(), text);
    }

    public static List<Player> near(Entity e, double range) {
        return e.level().getEntitiesOfClass(Player.class, e.getBoundingBox().inflate(range), p -> p != e && p.isAlive());
    }

    public static void push(LivingEntity target, Vec3 from, double strength) {
        Vec3 dir = target.position().subtract(from).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(0, 0, 1);
        dir = dir.normalize().scale(strength);
        target.push(dir.x, 0.15, dir.z);
        target.hurtMarked = true; // makes the server send the new speed to the client
    }

    // At most half a heart, ever. Works on players even with PvP off (my choice: tiny pranks).
    public static void tinyHit(LivingEntity target, @Nullable LivingEntity by, float amount) {
        float dmg = Math.min(amount, 1.0F);
        if (target instanceof Player || by == null) target.hurt(target.damageSources().generic(), dmg);
        else if (by instanceof Player p) target.hurt(target.damageSources().playerAttack(p), dmg);
        else target.hurt(target.damageSources().mobAttack(by), dmg);
    }

    public static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    // Gives the item to a player, or drops it at a mob's feet.
    public static void giveOrDrop(PowerUser u, ItemStack s) {
        if (u.player() != null) give(u.player(), s);
        else u.entity().spawnAtLocation(s);
    }

    // True once per Minecraft day (24000 ticks of overworld time) for this power on this holder.
    public static boolean daily(PowerUser u, String key) {
        long day = u.level().getServer().overworld().getGameTime() / 24000L;
        if (u.state().contains(key) && u.state().getLong(key) == day) return false;
        u.state().putLong(key, day);
        return true;
    }

    // The living thing the holder is looking at (within range), or for a mob, what it's fighting.
    @Nullable
    public static LivingEntity lookTarget(LivingEntity e, double range) {
        if (e instanceof Mob m && m.getTarget() != null && m.getTarget().distanceTo(m) <= range) return m.getTarget();
        Vec3 eye = e.getEyePosition();
        Vec3 look = e.getViewVector(1.0F);
        AABB box = e.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity other : e.level().getEntitiesOfClass(LivingEntity.class, box, o -> o != e && o.isAlive())) {
            Vec3 to = other.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 1.0E-3) continue;
            if (to.normalize().dot(look) < 0.97) continue; // within about 14 degrees of the crosshair
            if (!e.hasLineOfSight(other)) continue;
            if (dist < bestDist) { best = other; bestDist = dist; }
        }
        return best;
    }

    // The nearest player to a mob (or another player), or null.
    @Nullable
    public static Player nearestPlayer(Entity e, double range) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player p : near(e, range)) {
            double d = p.distanceToSqr(e);
            if (d < bestDist) { best = p; bestDist = d; }
        }
        return best;
    }

    private Fx() {}
}
