package com.example.charpowers.cloning;

import com.example.charpowers.cloning.model.RoutePlanner;
import com.example.charpowers.cloning.model.RoutePlanner.Cell;
import com.example.charpowers.cloning.model.RoutePlanner.Kind;
import com.example.charpowers.cloning.model.RoutePlanner.Step;
import com.example.charpowers.miner.TheMiner;
import com.example.charpowers.own.OreScan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Carries out a route with the clone's real body, the way Baritone's path executor does: it never stops to line itself up,
// just walks (or sprints) through the middle of each move, digging from wherever it can reach. It carries on along the path
// it has while the next stretch is worked out, and if a move takes far longer than planned it plans again.
public final class RouteWalker {
    public enum Result { WORKING, ARRIVED, NO_ROUTE }

    // What it may dig. MINING = natural ground, ores and leaves (so builds are safe). SOFT = dirt-like and leaves.
    // CHOPPING = that and logs. NOTHING = it only walks (Farm must never dig up a field).
    public enum DigRule { MINING, SOFT, CHOPPING, NOTHING }

    private static final int MOVE_TIMEOUT_EXTRA = 100, OFF_PATH_LIMIT = 60, MAX_REPLANS = 6, PLAN_AHEAD_STEPS = 5, LOOK_AHEAD = 4;
    private static final double JUMP_WITHIN = 1.3, ON_THE_SPOT = 0.3;

    private final CloneEntity clone;
    private final DigRule rule;
    private RoutePlanner.Goal goal;
    private String goalKey;
    private PathWorker.Job pending;
    private boolean pendingIsNext, noMoreAfterThis, needPlan;
    private List<Step> path = new ArrayList<>();
    private Set<Long> favored = Set.of();
    private boolean partial;
    private int index, moveTicks, offPath, replans;

    public RouteWalker(CloneEntity clone, DigRule rule) {
        this.clone = clone;
        this.rule = rule;
    }

    // Keep going towards this goal, one tick's worth. key = which goal this is, so asking again carries on the same trip.
    public Result go(String key, RoutePlanner.Goal wanted) {
        clone.mover.stop();
        if (!key.equals(goalKey)) {
            goalKey = key;
            goal = wanted;
            replans = 0;
            favored = favoredFromPath();
            path = new ArrayList<>();
            index = 0;
            partial = false;
            noMoreAfterThis = false;
            cancelPlan();
            needPlan = true;
        }
        // a route has to start from somewhere it's really standing, not halfway through a fall
        if (needPlan) {
            if (!settled()) {
                standStill();
                return Result.WORKING;
            }
            needPlan = false;
            planFrom(feet(), false);
        }
        takePlan();
        if (index >= path.size()) {
            if (pending != null) {
                standStill();
                return Result.WORKING;
            }
            if (noMoreAfterThis) return Result.NO_ROUTE;
            if (partial) {
                needPlan = true;
                standStill();
                return Result.WORKING;
            }
            standStill();
            return Result.ARRIVED;
        }
        if (partial && pending == null && !noMoreAfterThis && path.size() - index <= PLAN_AHEAD_STEPS) {
            planFrom(path.get(path.size() - 1).to(), true);
        }
        return follow();
    }

    public void forget() {
        goalKey = null;
        goal = null;
        cancelPlan();
        path = new ArrayList<>();
        index = 0;
        partial = false;
        noMoreAfterThis = false;
        needPlan = false;
        standStill();
    }

    // Forget the route but leave its hands alone: for when the job is digging something itself.
    public void drop() {
        goalKey = null;
        goal = null;
        cancelPlan();
        path = new ArrayList<>();
        index = 0;
        partial = false;
        noMoreAfterThis = false;
        needPlan = false;
    }

    public void standStill() {
        CloneWork.stopDigging(clone);
        CloneWork.standStill(clone);
    }

    public int stepsLeft() {
        return Math.max(0, path.size() - index);
    }

    public boolean searching() {
        return pending != null;
    }

    private Cell feet() {
        BlockPos at = clone.blockPosition();
        return new Cell(at.getX(), at.getY(), at.getZ());
    }

    private Set<Long> favoredFromPath() {
        Set<Long> keep = new HashSet<>();
        for (int i = index; i < path.size(); i++) {
            Cell c = path.get(i).to();
            keep.add(RoutePlanner.key(c.x(), c.y(), c.z()));
        }
        return keep;
    }

    private void cancelPlan() {
        if (pending != null) pending.cancelled = true;
        pending = null;
    }

    private void planFrom(Cell start, boolean next) {
        cancelPlan();
        pendingIsNext = next;
        noMoreAfterThis = false;
        pending = PathWorker.plan(start, goal, new GameTerrain(clone, rule), CloneWork.hasThrowaway(clone), favored);
    }

    private void takePlan() {
        if (pending == null || !pending.done) return;
        PathWorker.Job job = pending;
        pending = null;
        if (job.noRoute) {
            noMoreAfterThis = true;
            partial = false;
            return;
        }
        if (pendingIsNext) {
            path.addAll(job.path);
        } else {
            path = new ArrayList<>(job.path);
            index = 0;
            moveTicks = 0;
        }
        partial = job.partial;
    }

    private Result replan() {
        if (++replans > MAX_REPLANS) return Result.NO_ROUTE;
        favored = favoredFromPath();
        standStill();
        path = new ArrayList<>();
        index = 0;
        partial = false;
        cancelPlan();
        needPlan = true;
        return Result.WORKING;
    }

    private boolean settled() {
        return clone.onGround() || clone.isInWater();
    }

    // Where are we on the path? Falling or being pushed can put it further along than the move it thinks it's on.
    private void findPlace() {
        Cell at = feet();
        for (int i = Math.min(path.size() - 1, index + LOOK_AHEAD); i >= index; i--) {
            Step s = path.get(i);
            if (s.to().equals(at) && (settled() || s.kind() == Kind.WALK || s.kind() == Kind.DIAGONAL)) {
                index = i + 1;
                moveTicks = 0;
                offPath = 0;
                replans = 0;
                return;
            }
        }
        if (index < path.size() && at.equals(path.get(index).from())) {
            offPath = 0;
        } else {
            offPath++;
        }
    }

    private Result follow() {
        findPlace();
        if (index >= path.size()) return Result.WORKING;
        if (offPath > OFF_PATH_LIMIT) return replan();
        Step step = path.get(index);
        if (++moveTicks > step.cost() + MOVE_TIMEOUT_EXTRA) return replan();

        if (settled()) {
            Boolean dug = digFor(step);
            if (dug == null) return replan();
            if (dug) return Result.WORKING;
        }
        CloneWork.stopDigging(clone);
        openDoors(step);
        if (step.place() != null && step.kind() == Kind.WALK && !placeBridge(step)) return replan();
        move(step);
        return Result.WORKING;
    }

    // True = busy digging this tick. False = nothing left to dig. Null = something it mustn't dig is in the way.
    private Boolean digFor(Step step) {
        for (Cell cell : step.dig()) {
            BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
            BlockState state = clone.level().getBlockState(pos);
            if (state.getCollisionShape(clone.level(), pos).isEmpty()) continue;
            if (!allowed(rule, state)) return null;
            if (Math.sqrt(pos.distToCenterSqr(clone.getEyePosition())) > CloneWork.REACH) return false;
            if (!CloneWork.dig(clone, pos, state)) return null;
            BlockPos seen = CloneWork.lookingAt(clone);
            if (seen != null && !seen.equals(pos)) {
                BlockState inTheWay = clone.level().getBlockState(seen);
                // grass or a flower in the way just gets cut. Anything solid it isn't allowed to dig means another route.
                if (!inTheWay.getCollisionShape(clone.level(), seen).isEmpty() && !allowed(rule, inTheWay)) {
                    CloneWork.stopDigging(clone);
                    return null;
                }
            }
            return true;
        }
        return false;
    }

    private void openDoors(Step step) {
        BlockPos to = new BlockPos(step.to().x(), step.to().y(), step.to().z());
        for (BlockPos pos : List.of(to, to.above())) {
            BlockState state = clone.level().getBlockState(pos);
            if ((state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.FENCE_GATES)) && !state.getValue(BlockStateProperties.OPEN)) {
                CloneActions.clickBlock(clone, pos);
                return;
            }
        }
    }

    private boolean placeBridge(Step step) {
        BlockPos where = new BlockPos(step.place().x(), step.place().y(), step.place().z());
        if (!clone.level().getBlockState(where).canBeReplaced()) return true; // already there
        BlockPos against = new BlockPos(step.from().x(), step.from().y() - 1, step.from().z());
        Direction face = Direction.getNearest(step.to().x() - step.from().x(), 0, step.to().z() - step.from().z());
        return CloneWork.placeThrowaway(clone, against, face);
    }

    private void move(Step step) {
        BlockPos to = new BlockPos(step.to().x(), step.to().y(), step.to().z());
        Vec3 aim = Vec3.atBottomCenterOf(to);
        double dx = aim.x - clone.getX(), dz = aim.z - clone.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        switch (step.kind()) {
            case WALK, DIAGONAL -> {
                walkTowards(aim, 1f, sprintable());
                if (clone.isInWater()) CloneActions.jump(clone);
                else if (clone.horizontalCollision && clone.onGround()) CloneActions.jump(clone);
            }
            case UP -> {
                walkTowards(aim, 1f, false);
                if (flat < JUMP_WITHIN && settled()) CloneActions.jump(clone);
            }
            case DOWN, FALL -> {
                if (flat < ON_THE_SPOT) CloneActions.stop(clone);
                else walkTowards(aim, clone.onGround() ? 1f : 0.5f, false);
            }
            case DIG_DOWN -> {
                if (flat > ON_THE_SPOT) walkTowards(aim, 0.3f, false);
                else CloneActions.stop(clone);
            }
            case PILLAR -> pillar(step, aim, flat);
            case SWIM_UP -> {
                CloneActions.stop(clone);
                CloneActions.jump(clone);
            }
        }
    }

    private void walkTowards(Vec3 aim, float speed, boolean sprint) {
        CloneActions.lookAt(clone, new Vec3(aim.x, clone.getEyeY(), aim.z));
        CloneActions.walk(clone, speed, sprint);
    }

    // Nothing to dig on this move or the next, heading straight on, and not hungry.
    private boolean sprintable() {
        if (clone.getFoodData().getFoodLevel() <= 6 || clone.isInWater() || index + 1 >= path.size()) return false;
        Step here = path.get(index), next = path.get(index + 1);
        return here.dig().isEmpty() && next.dig().isEmpty() && here.place() == null && next.place() == null
                && next.to().x() - here.to().x() == here.to().x() - here.from().x() && next.to().z() - here.to().z() == here.to().z() - here.from().z();
    }

    // Jump, and at the top of the jump put a block where your feet were.
    private void pillar(Step step, Vec3 aim, double flat) {
        if (flat > ON_THE_SPOT) {
            walkTowards(aim, 0.3f, false);
            return;
        }
        CloneActions.stop(clone);
        BlockPos was = new BlockPos(step.from().x(), step.from().y(), step.from().z());
        CloneActions.lookAt(clone, Vec3.atCenterOf(was));
        if (clone.getY() >= step.from().y() + 1 && clone.level().getBlockState(was).canBeReplaced()) {
            CloneWork.placeThrowaway(clone, was.below(), Direction.UP);
        } else if (clone.onGround()) {
            CloneActions.jump(clone);
        }
    }

    static boolean allowed(DigRule rule, BlockState state) {
        if (state.hasBlockEntity() || state.is(BlockTags.BEDS)) return false;
        return switch (rule) {
            case MINING -> state.is(TheMiner.NATURAL) || state.is(BlockTags.LEAVES) || OreScan.isOre(state);
            case SOFT -> soft(state);
            case CHOPPING -> soft(state) || state.is(BlockTags.LOGS);
            case NOTHING -> false;
        };
    }

    private static boolean soft(BlockState state) {
        return state.is(BlockTags.LEAVES) || (state.is(BlockTags.MINEABLE_WITH_SHOVEL) && !state.is(BlockTags.WOOL));
    }
}
