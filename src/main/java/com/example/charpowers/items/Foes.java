package com.example.charpowers.items;

import com.example.charpowers.CharPowers;
import com.example.charpowers.own.TeamBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

// Who the weapons' sweeping moves may hurt, the Anima Katana's damage type, and sending particles everyone sees.
public final class Foes {
    public static final ResourceKey<DamageType> SOUL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(CharPowers.MODID, "soul"));

    private Foes() {}

    // May this ever be hurt by that? Same rules as a sword: friends and your own pets are safe.
    public static boolean canHit(LivingEntity attacker, LivingEntity target) {
        if (target == attacker || !target.isAlive() || !target.isAttackable() || target.isInvulnerable()) return false;
        if (attacker.isAlliedTo(target)) return false;
        if (attacker instanceof Player a) {
            if (target instanceof Player b && (!a.canHarmPlayer(b) || TeamBridge.sameTeam(a.getUUID(), b.getUUID()) || b.isSpectator())) return false;
            if (target instanceof TamableAnimal pet && pet.getOwnerUUID() != null
                    && (a.getUUID().equals(pet.getOwnerUUID()) || TeamBridge.sameTeam(a.getUUID(), pet.getOwnerUUID()))) return false;
        }
        return true;
    }

    // A move that sweeps an area only catches things that are fighting: not your cows, villagers or armour stands. What you aim right at is always fair.
    public static boolean sweepHits(LivingEntity attacker, LivingEntity target) {
        if (!canHit(attacker, target) || target instanceof ArmorStand) return false;
        if (target instanceof Enemy || target instanceof Player) return true;
        if (target instanceof Mob m && m.getTarget() == attacker) return true;
        return attacker.getLastHurtMob() == target || target.getLastHurtByMob() == attacker;
    }

    public static DamageSource soul(Entity attacker) {
        return new DamageSource(attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOUL), attacker);
    }

    // Sent forced, so a player with Particles set to Minimal still sees the weapon doing its thing.
    public static void particles(ServerLevel level, ParticleOptions p, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayer sp : level.players()) {
            if (sp.distanceToSqr(x, y, z) < 64 * 64) level.sendParticles(sp, p, true, x, y, z, count, dx, dy, dz, speed);
        }
    }

    public static void particles(ServerLevel level, ParticleOptions p, Vec3 at, int count, double spread, double speed) {
        particles(level, p, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    // The ground under a spot, within a few blocks up or down. Null when there's none.
    public static BlockPos groundNear(ServerLevel level, BlockPos from, int up, int down) {
        BlockPos.MutableBlockPos p = from.mutable().move(0, up, 0);
        for (int i = 0; i <= up + down; i++) {
            BlockPos below = p.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP) && !level.getBlockState(p).isFaceSturdy(level, p, net.minecraft.core.Direction.UP)) return p.immutable();
            p.move(0, -1, 0);
        }
        return null;
    }
}
