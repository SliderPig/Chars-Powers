package com.example.charpowers.cloning;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

// Players have no pathfinding, so a clone borrows the game's own: a zombie that is never added to the world
// is asked for a route (at most once a second), and the clone walks it like a person would.
public final class CloneMover {
    private final CloneEntity clone;
    private Zombie probe;
    private BlockPos goal;
    private Path path;
    private int lastAsked = -100;
    private Vec3 lastPos = Vec3.ZERO;
    private int stuckChecks;

    CloneMover(CloneEntity clone) {
        this.clone = clone;
    }

    public void walkTo(BlockPos where) {
        goal = where;
    }

    public void stop() {
        if (goal != null) CloneActions.stop(clone);
        goal = null;
        path = null;
        stuckChecks = 0;
    }

    // 30 seconds without getting anywhere.
    public boolean longStuck() {
        return stuckChecks >= 6;
    }

    public void forgetStuck() {
        stuckChecks = 0;
    }

    void tick() {
        if (goal == null) return;
        if (clone.tickCount - lastAsked >= 20) {
            lastAsked = clone.tickCount;
            path = ask(goal);
        }
        if (clone.tickCount % 100 == 0) {
            stuckChecks = clone.position().distanceToSqr(lastPos) < 1.0 ? stuckChecks + 1 : 0;
            lastPos = clone.position();
        }
        Vec3 aim = nextPoint();
        if (aim == null) {
            CloneActions.stop(clone);
            return;
        }
        CloneActions.lookAt(clone, new Vec3(aim.x, clone.isInWater() ? aim.y : clone.getEyeY(), aim.z));
        double far = clone.position().distanceToSqr(Vec3.atCenterOf(goal));
        CloneActions.walk(clone, 1f, far > 16 && !clone.isInWater());
        if (aim.y > clone.getY() + 0.4 || clone.horizontalCollision || clone.isInWater()) CloneActions.jump(clone);
    }

    private Vec3 nextPoint() {
        while (path != null && !path.isDone()) {
            Node n = path.getNextNode();
            Vec3 p = new Vec3(n.x + 0.5, n.y, n.z + 0.5);
            double dx = p.x - clone.getX();
            double dz = p.z - clone.getZ();
            if (dx * dx + dz * dz < 0.4 && Math.abs(p.y - clone.getY()) < 1.2) {
                path.advance();
                continue;
            }
            return p;
        }
        return null;
    }

    private Path ask(BlockPos where) {
        if (probe == null || probe.level() != clone.level()) {
            probe = new Zombie(clone.level());
            probe.getNavigation().setCanFloat(true);
        }
        probe.setPos(clone.position());
        probe.setOnGround(true);
        return probe.getNavigation().createPath(where, 1);
    }
}
