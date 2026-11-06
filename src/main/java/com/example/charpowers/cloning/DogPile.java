package com.example.charpowers.cloning;

import com.example.charpowers.cloning.model.CloneRules;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

// Dog Pile: your other clones go home, then 20 copies wearing your gear fight for exactly 60 seconds.
final class DogPile {
    private static final int FIGHTERS = 20;

    private DogPile() {}

    static void start(ServerPlayer host) {
        CloneManager.dismissAll(host);
        ServerLevel level = host.serverLevel();
        double health = CloneRules.maxHealth(Mastery.level(host, Mastery.CLONING));
        for (int i = 0; i < FIGHTERS; i++) {
            DogPileMob mob = DogPileMob.copyOf(host, health);
            Vec3 at = ring(host, i, mob);
            mob.moveTo(at.x, at.y, at.z, host.getRandom().nextFloat() * 360f, 0f);
            level.addFreshEntity(mob);
        }
        level.playSound(null, host.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1f, 1.2f);
    }

    // Spots around the player, in two rings. Falls back to the player's own spot when there's no room.
    private static Vec3 ring(ServerPlayer host, int i, DogPileMob mob) {
        double radius = i < 8 ? 2.0 : 3.5;
        double angle = i * Math.PI * 2 / (i < 8 ? 8 : 12);
        for (int dy = 0; dy <= 2; dy++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                Vec3 p = host.position().add(Math.cos(angle) * radius, dy * sign, Math.sin(angle) * radius);
                if (host.level().noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(p))) return p;
            }
        }
        return host.position();
    }

    // Escaped Death: a copy appears on the spot and takes the hit that would have killed you.
    static void standIn(ServerPlayer host, DamageSource source) {
        DogPileMob mob = DogPileMob.copyOf(host, 1.0);
        mob.moveTo(host.getX(), host.getY(), host.getZ(), host.getYRot(), host.getXRot());
        host.serverLevel().addFreshEntity(mob);
        mob.hurt(source, Float.MAX_VALUE);
        if (mob.isAlive()) mob.kill();
    }
}
