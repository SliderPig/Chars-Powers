package com.example.charpowers.cloning.jobs;

import com.example.charpowers.cloning.CloneCombat;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

// Attacks the hostile mobs around where the order was given, plus anything that hits the player or the clone.
// Goes back to that spot when there's nothing left. Animals are left alone.
public class AttackJob implements CloneJob {
    private static final int AREA = 16;
    private static final int RECENT_TICKS = 100;
    private final BlockPos anchor;
    private LivingEntity target;

    public AttackJob(BlockPos anchor) {
        this.anchor = anchor;
    }

    @Override
    public String id() {
        return "attack";
    }

    @Override
    public String describe(CloneEntity c) {
        return target != null ? "Attacking " + target.getName().getString() : "Guarding " + anchor.toShortString();
    }

    @Override
    public void tick(CloneEntity c) {
        ServerPlayer host = CloneManager.host(c);
        if (c.mover.longStuck()) {
            target = null;
            c.mover.forgetStuck();
        }
        if (c.tickCount % 10 == 0 || !CloneCombat.isFoe(c, host, target) || target.level() != c.level()) target = pick(c, host);
        if (target != null) {
            CloneCombat.fight(c, target);
        } else if (c.blockPosition().distSqr(anchor) > 16) {
            c.mover.walkTo(anchor);
        } else {
            c.mover.stop();
        }
    }

    private LivingEntity pick(CloneEntity c, ServerPlayer host) {
        if (host != null && host.level() == c.level() && host.tickCount - host.getLastHurtByMobTimestamp() <= RECENT_TICKS
                && CloneCombat.isFoe(c, host, host.getLastHurtByMob())) return host.getLastHurtByMob();
        if (c.tickCount - c.getLastHurtByMobTimestamp() <= RECENT_TICKS && CloneCombat.isFoe(c, host, c.getLastHurtByMob())) return c.getLastHurtByMob();
        AABB area = new AABB(anchor).inflate(AREA);
        Vec3 here = c.position();
        return c.level().getEntitiesOfClass(LivingEntity.class, area, e -> e instanceof Enemy && CloneCombat.isFoe(c, host, e))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(here))).orElse(null);
    }
}
