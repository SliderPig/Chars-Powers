package com.example.charpowers.game.mob;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Small helpers the gate creatures share: tells (rings of particles) and area hits that only ever land on players.
final class GateFx {
    private GateFx() {}

    static void ring(ServerLevel level, Vec3 at, double radius, ParticleOptions particle, int points) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(particle, at.x + Math.cos(a) * radius, at.y + 0.1, at.z + Math.sin(a) * radius, 1, 0, 0.02, 0, 0);
        }
    }

    static void burst(ServerLevel level, Vec3 at, ParticleOptions particle, int count, double spread) {
        level.sendParticles(particle, at.x, at.y + 0.5, at.z, count, spread, spread * 0.6, spread, 0.02);
    }

    static List<Player> playersNear(LivingEntity source, Vec3 at, double radius) {
        return source.level().getEntitiesOfClass(Player.class, new AABB(at.x - radius, at.y - 2, at.z - radius, at.x + radius, at.y + 3, at.z + radius),
                p -> p.isAlive() && !p.isSpectator() && p.position().distanceToSqr(at) <= radius * radius + 1);
    }

    // Hurts every player in reach and shoves them away from the middle. Returns how many were hit.
    static int hit(LivingEntity source, Vec3 at, double radius, DamageSource how, float amount, double push, double lift) {
        int n = 0;
        for (Player p : playersNear(source, at, radius)) {
            if (!p.hurt(how, amount)) continue;
            n++;
            Vec3 away = p.position().subtract(at).multiply(1, 0, 1);
            away = away.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : away.normalize();
            p.setDeltaMovement(p.getDeltaMovement().add(away.x * push, lift, away.z * push));
            p.hurtMarked = true;
        }
        return n;
    }
}
