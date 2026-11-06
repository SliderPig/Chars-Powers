package com.example.charpowers.cloning.jobs;

import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneGear;
import com.example.charpowers.cloning.CloneManager;
import com.example.charpowers.cloning.CloneWork;
import com.example.charpowers.cloning.RouteWalker;
import com.example.charpowers.cloning.model.RoutePlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Chops the trees around where it was told to: real trees only (natural leaves on them, so log houses are safe). It walks
// there the way Baritone would, digs what it can reach, pillars up for tall trunks (or climbs if it has no blocks), then
// picks up what fell and plants a sapling back if it has one.
public class ChopJob implements CloneJob {
    private static final int AREA = 24, SCAN_EVERY = 60, COLUMN_STEP = 1, SEARCH_DOWN = 20, MAX_LOGS = 96, GATHER_TICKS = 100, STASH_EVERY = 100;
    private static final double CLIMB_SPEED = 0.2;
    private static final int KEEP_BLOCKS = 16, LOGS_TO_AIM_AT = 4;

    private final BlockPos anchor;
    private final List<BlockPos> tree = new ArrayList<>();
    private final Set<BlockPos> notTrees = new HashSet<>();
    private CloneEntity me;
    private RouteWalker walker;
    private BlockPos base;
    private int gathering;
    private boolean saidNone, climbing;

    public ChopJob(BlockPos anchor) {
        this.anchor = anchor;
    }

    @Override
    public String id() {
        return "chop";
    }

    @Override
    public String describe(CloneEntity c) {
        if (gathering > 0) return "Picking up wood";
        return base != null ? "Chopping a tree, " + tree.size() + " logs left" : "Looking for trees";
    }

    @Override
    public boolean usesTools() {
        return true;
    }

    // Dirt it digs on the way to a tree stays on the floor, once it has enough to pillar up with.
    @Override
    public boolean refuses(ItemStack stack) {
        if (stack.is(Tags.Items.GRAVEL)) return true;
        return CloneWork.isThrowaway(stack) && me != null && CloneWork.countThrowaway(me) >= KEEP_BLOCKS;
    }

    @Override
    public void start(CloneEntity c) {
        me = c;
        walker = new RouteWalker(c, RouteWalker.DigRule.CHOPPING);
    }

    @Override
    public void stop(CloneEntity c) {
        walker.forget();
        c.mover.stop();
    }

    @Override
    public void tick(CloneEntity c) {
        ServerPlayer host = CloneManager.host(c);
        if (CloneWork.fightBack(c, host)) {
            walker.forget();
            return;
        }
        if (c.isUsingItem()) return;
        if (c.tickCount % STASH_EVERY == 0 && !CloneWork.stashIfFull(c, host)) {
            CloneWork.say(c, host, "is full and has nowhere to put the wood. Mark it an emptier chest.");
            c.setJob(new FollowJob());
            return;
        }
        if (c.tickCount % 100 == 0) {
            com.example.charpowers.CharPowers.LOG.info("[Cloning] {} chop: at {} ground={} base={} logs={} first={} gathering={} searching={} stepsLeft={} digging={} seeing={} climbing={}",
                    c.getGameProfile().getName(), c.blockPosition().toShortString(), c.onGround(), base, tree.size(), tree.isEmpty() ? "-" : tree.get(0).toShortString(), gathering, walker.searching(), walker.stepsLeft(), c.digging, CloneWork.lookingAt(c), climbing);
        }
        if (gathering > 0) {
            gather(c);
        } else if (base == null) {
            if (c.tickCount % SCAN_EVERY == 0) findTree(c, host);
            if (base == null && c.blockPosition().distSqr(anchor) > 25 && walker.go("back", RoutePlanner.near(anchor.getX(), anchor.getY(), anchor.getZ(), 3, 4)) != RouteWalker.Result.WORKING) walker.forget();
        } else {
            chop(c);
        }
    }

    private void findTree(CloneEntity c, ServerPlayer host) {
        ServerLevel level = c.serverLevel();
        BlockPos found = null;
        double nearest = Double.MAX_VALUE;
        for (int x = anchor.getX() - AREA; x <= anchor.getX() + AREA; x += COLUMN_STEP) {
            for (int z = anchor.getZ() - AREA; z <= anchor.getZ() + AREA; z += COLUMN_STEP) {
                if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = top; y > top - SEARCH_DOWN; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(BlockTags.LOGS)) continue;
                    if (notTrees.contains(pos)) break;
                    double far = pos.distSqr(c.blockPosition());
                    if (far < nearest) {
                        nearest = far;
                        found = pos;
                    }
                    break;
                }
            }
        }
        if (found != null && loadTree(c, found)) {
            saidNone = false;
            return;
        }
        if (found == null && !saidNone) {
            saidNone = true;
            CloneWork.say(c, host, "can't see any more trees around here.");
        }
    }

    // All the logs joined to this one, and only if it has natural leaves on it: a log cabin doesn't.
    private boolean loadTree(CloneEntity c, BlockPos start) {
        ServerLevel level = c.serverLevel();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        boolean leafy = false;
        while (!queue.isEmpty() && seen.size() < MAX_LOGS) {
            BlockPos pos = queue.poll();
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                BlockState state = level.getBlockState(near);
                if (state.is(BlockTags.LOGS)) {
                    BlockPos fixed = near.immutable();
                    if (seen.add(fixed)) queue.add(fixed);
                } else if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT)) {
                    leafy = true;
                }
            }
        }
        if (!leafy) {
            notTrees.addAll(seen);
            return false;
        }
        tree.clear();
        tree.addAll(seen);
        tree.sort(Comparator.comparingInt(BlockPos::getY));
        base = tree.get(0);
        return true;
    }

    private void chop(CloneEntity c) {
        tree.removeIf(pos -> !c.level().getBlockState(pos).is(BlockTags.LOGS));
        if (tree.isEmpty()) {
            CloneWork.stopDigging(c);
            gathering = GATHER_TICKS;
            return;
        }
        BlockPos log = tree.get(0);
        double far = Math.sqrt(log.distToCenterSqr(c.getEyePosition()));
        if (far <= CloneWork.REACH - 0.5 && (c.onGround() || climbing)) {
            walker.drop();
            c.fallDistance = 0;
            CloneWork.dig(c, log, c.level().getBlockState(log));
            if (climbing) c.setDeltaMovement(c.getDeltaMovement().x, 0, c.getDeltaMovement().z);
            return;
        }
        boolean underIt = Math.abs(log.getX() + 0.5 - c.getX()) < 3 && Math.abs(log.getZ() + 0.5 - c.getZ()) < 3 && log.getY() > c.getY();
        if (underIt && !CloneWork.hasThrowaway(c)) {
            // tall tree and nothing to pillar with: up the trunk it goes
            climbing = true;
            walker.drop();
            CloneWork.stopDigging(c);
            c.mover.stop();
            CloneWork.standStill(c);
            c.fallDistance = 0;
            c.setDeltaMovement(c.getDeltaMovement().x, CLIMB_SPEED, c.getDeltaMovement().z);
            c.hurtMarked = true;
            return;
        }
        climbing = false;
        List<RoutePlanner.Target> lowest = new ArrayList<>();
        for (int i = 0; i < Math.min(LOGS_TO_AIM_AT, tree.size()); i++) {
            BlockPos pos = tree.get(i);
            lowest.add(new RoutePlanner.Target(pos.getX(), pos.getY(), pos.getZ(), 0));
        }
        RouteWalker.Result result = walker.go("tree " + base.toShortString() + " " + tree.size(), RoutePlanner.anyOf(lowest));
        if (result == RouteWalker.Result.NO_ROUTE) {
            // can't get there from here: leave this tree and find another
            notTrees.addAll(tree);
            tree.clear();
            base = null;
            walker.forget();
        }
    }

    private void gather(CloneEntity c) {
        c.fallDistance = 0;
        if (gathering == GATHER_TICKS && base != null) replant(c);
        gathering--;
        List<ItemEntity> items = c.level().getEntitiesOfClass(ItemEntity.class, new AABB(c.blockPosition()).inflate(10),
                e -> e.isAlive() && !e.hasPickUpDelay());
        if (!items.isEmpty()) {
            ItemEntity nearest = items.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).get();
            c.mover.walkTo(nearest.blockPosition());
        }
        if (items.isEmpty() || gathering <= 0) {
            gathering = 0;
            c.mover.stop();
            base = null;
        }
    }

    private void replant(CloneEntity c) {
        ServerLevel level = c.serverLevel();
        BlockPos spot = base;
        if (!level.getBlockState(spot).isAir() || !level.getBlockState(spot.below()).is(BlockTags.DIRT)) return;
        ItemStack sapling = CloneGear.findIn(c, s -> s.is(ItemTags.SAPLINGS) && s.getItem() instanceof BlockItem);
        if (sapling == null) return;
        if (level.setBlock(spot, ((BlockItem) sapling.getItem()).getBlock().defaultBlockState(), 3)) sapling.shrink(1);
    }
}
