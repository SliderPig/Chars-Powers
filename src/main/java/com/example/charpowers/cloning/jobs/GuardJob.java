package com.example.charpowers.cloning.jobs;

import com.example.charpowers.cloning.CloneCombat;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneManager;
import com.example.charpowers.cloning.model.PostRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

// Stands where it was ordered. Hostile mobs that come near that spot get attacked, and it never chases them far: when
// nothing is left it walks back to its post. (Attack goes after anything hurting you, over a wider area. Guard stays put.)
public class GuardJob implements CloneJob {
    private static final int RECENT_TICKS = 60;
    private final BlockPos post;
    private LivingEntity target;

    public GuardJob(BlockPos post) {
        this.post = post;
    }

    @Override
    public String id() {
        return "guard";
    }

    public BlockPos post() {
        return post;
    }

    @Override
    public String describe(CloneEntity c) {
        return target != null ? "Guarding, fighting " + target.getName().getString() : "Guarding " + post.toShortString();
    }

    @Override
    public void tick(CloneEntity c) {
        ServerPlayer host = CloneManager.host(c);
        if (c.mover.longStuck()) {
            target = null;
            c.mover.forgetStuck();
        }
        if (target != null && (!CloneCombat.isFoe(c, host, target) || target.level() != c.level()
                || !PostRules.worthChasing(target.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5), c.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5)))) {
            target = null;
        }
        if (target == null && c.tickCount % 10 == 0) target = pick(c, host);
        if (target != null) {
            CloneCombat.fight(c, target);
        } else if (PostRules.shouldReturn(c.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5))) {
            c.mover.walkTo(post);
        } else {
            c.mover.stop();
        }
    }

    private LivingEntity pick(CloneEntity c, ServerPlayer host) {
        double postSqr = c.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5);
        // something that has just hit it counts, if it is still near the post
        LivingEntity hitMe = c.getLastHurtByMob();
        if (hitMe != null && c.tickCount - c.getLastHurtByMobTimestamp() <= RECENT_TICKS && CloneCombat.isFoe(c, host, hitMe)
                && PostRules.worthChasing(hitMe.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5), postSqr)) return hitMe;
        AABB area = new AABB(post).inflate(PostRules.GUARD_RADIUS);
        Vec3 here = c.position();
        return c.level().getEntitiesOfClass(LivingEntity.class, area, e -> e instanceof Enemy && CloneCombat.isFoe(c, host, e))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(here))).orElse(null);
    }
}
