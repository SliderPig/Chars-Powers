package com.example.charpowers.cloning;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

// Hitting things like a player does: only at full swing strength, in reach, with something it can see,
// through the player's own attack (so durability, enchantments and XP all work).
public final class CloneCombat {
    private static final double RETREAT_BELOW = 0.25;

    private CloneCombat() {}

    public static boolean isFoe(CloneEntity c, @Nullable ServerPlayer host, @Nullable LivingEntity e) {
        if (e == null || e == c || e == host || !e.isAlive() || e.isSpectator()) return false;
        if (e instanceof CloneEntity other && other.hostId.equals(c.hostId)) return false;
        if (e instanceof DogPileMob mob && c.hostId.equals(mob.hostId())) return false;
        return host == null || !e.isAlliedTo(host);
    }

    public static void fight(CloneEntity c, LivingEntity target) {
        c.brain.fight(target);
        if (!c.brain.handsBusy()) CloneActions.lookAt(c, target.getEyePosition());
        // nearly dead and not rogue: back away, and let the brain heal or block
        if (!c.rogue && c.getHealth() / c.getMaxHealth() < RETREAT_BELOW && target.distanceToSqr(c) < 64) {
            Vec3 away = c.position().subtract(target.position()).normalize().scale(10);
            c.mover.walkTo(BlockPos.containing(c.position().add(away)));
            return;
        }
        boolean inReach = target.getBoundingBox().distanceToSqr(c.getEyePosition()) <= 9.0;
        if (inReach) c.mover.stop();
        else c.mover.walkTo(target.blockPosition());
        if (inReach && !c.isUsingItem() && !c.brain.holdSwing() && c.hasLineOfSight(target) && c.getAttackStrengthScale(0.5f) >= 0.9f) {
            CloneGear.holdWeapon(c);
            c.attack(target);
            c.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    public static void teleportToHost(CloneEntity c, ServerPlayer host) {
        c.mover.stop();
        var spot = CloneManager.spotNear(host);
        c.teleportTo(host.serverLevel(), spot.x, spot.y, spot.z, host.getYRot(), host.getXRot());
        c.fixBody();
    }
}
