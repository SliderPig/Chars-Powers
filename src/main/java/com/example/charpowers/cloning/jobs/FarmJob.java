package com.example.charpowers.cloning.jobs;

import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneGear;
import com.example.charpowers.cloning.CloneManager;
import com.example.charpowers.cloning.CloneWork;
import com.example.charpowers.cloning.RouteWalker;
import com.example.charpowers.cloning.model.PostRules;
import com.example.charpowers.cloning.model.RoutePlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

// Harvests the ripe crops round the spot it was ordered to (wheat, carrots, potatoes, beetroot, nether wart), plants the
// same crop back in the empty soil, and sends the harvest to its marked chest. It walks like the other jobs do and never
// digs: it only breaks crops, never the field.
public class FarmJob implements CloneJob {
    private static final int SCAN_EVERY = 30, GATHER_TICKS = 40, DROPS_LAND = 10, STASH_EVERY = 100, PLANT_TRIES = 3;
    private final BlockPos post;
    private final Set<BlockPos> unreachable = new HashSet<>();
    private RouteWalker walker;
    private BlockPos crop;
    private Item seed;
    private BlockPos soil;
    private int gathering, plantTries, harvested;
    private boolean saidNone, saidNoSeeds;

    public FarmJob(BlockPos post) {
        this.post = post;
    }

    @Override
    public String id() {
        return "farm";
    }

    public BlockPos post() {
        return post;
    }

    public int harvested() {
        return harvested;
    }

    @Override
    public String describe(CloneEntity c) {
        if (crop != null) return "Harvesting at " + crop.toShortString();
        if (soil != null || gathering > 0) return "Planting and picking up";
        return "Farming near " + post.toShortString() + (harvested > 0 ? " (" + harvested + " picked)" : "");
    }

    @Override
    public boolean usesTools() {
        return true;
    }

    @Override
    public void start(CloneEntity c) {
        walker = new RouteWalker(c, RouteWalker.DigRule.NOTHING);
    }

    @Override
    public void stop(CloneEntity c) {
        if (walker != null) walker.forget();
        c.mover.stop();
    }

    // true = fully grown. Anything else (a seedling, a melon) is left alone.
    public static boolean isRipe(BlockState state) {
        if (state.getBlock() instanceof CropBlock cb) return PostRules.ripe(cb.getAge(state), cb.getMaxAge());
        if (state.getBlock() instanceof NetherWartBlock) return PostRules.ripe(state.getValue(NetherWartBlock.AGE), 3);
        return false;
    }

    @Nullable
    static Item seedOf(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof NetherWartBlock) return Items.NETHER_WART;
        return state.getBlock().getCloneItemStack(level, pos, state).getItem();
    }

    private double reachSqr() {
        return (CloneWork.REACH - 0.8) * (CloneWork.REACH - 0.8);
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
            CloneWork.say(c, host, "is full and has nowhere to put the harvest. Mark it an emptier chest.");
            c.setJob(new FollowJob());
            return;
        }
        if (gathering > 0) {
            gather(c);
        } else if (soil != null) {
            replant(c);
        } else if (crop == null) {
            if (c.tickCount % SCAN_EVERY == 0) find(c, host);
            if (crop == null && PostRules.shouldReturn(c.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5))
                    && walker.go("post", RoutePlanner.near(post.getX(), post.getY(), post.getZ(), 3, 3)) != RouteWalker.Result.WORKING) walker.forget();
        } else {
            harvest(c);
        }
    }

    private void find(CloneEntity c, ServerPlayer host) {
        ServerLevel level = c.serverLevel();
        BlockPos best = null;
        double bestSqr = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -PostRules.FARM_AREA; x <= PostRules.FARM_AREA; x++) {
            for (int z = -PostRules.FARM_AREA; z <= PostRules.FARM_AREA; z++) {
                for (int y = -PostRules.FARM_HEIGHT; y <= PostRules.FARM_HEIGHT; y++) {
                    p.set(post.getX() + x, post.getY() + y, post.getZ() + z);
                    if (!level.hasChunkAt(p) || unreachable.contains(p)) continue;
                    if (!isRipe(level.getBlockState(p))) continue;
                    double d = p.distSqr(c.blockPosition());
                    if (d < bestSqr) {
                        bestSqr = d;
                        best = p.immutable();
                    }
                }
            }
        }
        crop = best;
        if (best != null) {
            saidNone = false;
            seed = seedOf(level, best, level.getBlockState(best));
        } else if (!saidNone && harvested > 0) {
            saidNone = true;
            CloneWork.say(c, host, "has picked everything that was ripe.");
        }
    }

    private void harvest(CloneEntity c) {
        ServerLevel level = c.serverLevel();
        BlockState state = level.getBlockState(crop);
        if (!isRipe(state)) {
            // broken (by it) or not ripe any more: plant again and pick up what fell
            if (state.isAir()) {
                soil = crop.below();
                plantTries = 0;
                harvested++;
            }
            crop = null;
            gathering = GATHER_TICKS;
            CloneWork.stopDigging(c);
            return;
        }
        if (crop.distToCenterSqr(c.getEyePosition()) <= reachSqr() && CloneWork.canSee(c, crop)) {
            walker.drop();
            CloneWork.dig(c, crop, state);
            return;
        }
        RouteWalker.Result r = walker.go("crop " + crop.toShortString(), RoutePlanner.near(crop.getX(), crop.getY(), crop.getZ(), 2, 2));
        if (r == RouteWalker.Result.NO_ROUTE || (r == RouteWalker.Result.ARRIVED && !CloneWork.canSee(c, crop))) {
            unreachable.add(crop);
            crop = null;
            walker.forget();
        }
    }

    private void replant(CloneEntity c) {
        ServerLevel level = c.serverLevel();
        if (seed == null || !level.getBlockState(soil.above()).isAir() || plantTries >= PLANT_TRIES) {
            soil = null;
            return;
        }
        if (soil.distToCenterSqr(c.getEyePosition()) > reachSqr()) {
            RouteWalker.Result r = walker.go("soil " + soil.toShortString(), RoutePlanner.near(soil.getX(), soil.getY(), soil.getZ(), 2, 2));
            if (r == RouteWalker.Result.NO_ROUTE) soil = null;
            return;
        }
        walker.drop();
        c.mover.stop();
        if (c.tickCount % 4 != 0) return;
        if (CloneWork.plantOn(c, soil, seed)) {
            soil = null;
        } else if (++plantTries >= PLANT_TRIES) {
            if (!saidNoSeeds && CloneGear.findIn(c, s -> s.is(seed)) == null) {
                saidNoSeeds = true;
                CloneWork.say(c, CloneManager.host(c), "has no seeds left to plant again.");
            }
            soil = null;
        }
    }

    private void gather(CloneEntity c) {
        gathering--;
        if (soil != null) {
            // wait for the drops to land, then plant while it picks them up
            if (gathering <= GATHER_TICKS - DROPS_LAND) replant(c);
            return;
        }
        var items = c.level().getEntitiesOfClass(ItemEntity.class, new AABB(c.blockPosition()).inflate(8), e -> e.isAlive() && !e.hasPickUpDelay());
        if (!items.isEmpty()) {
            ItemEntity nearest = items.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).get();
            c.mover.walkTo(nearest.blockPosition());
        }
        if (items.isEmpty() || gathering <= 0) {
            gathering = 0;
            c.mover.stop();
        }
    }
}
