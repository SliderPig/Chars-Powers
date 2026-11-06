package com.example.charpowers.house;

import com.example.charpowers.house.model.CastleRule;
import com.example.charpowers.house.model.HouseRules;
import com.example.charpowers.own.Setting;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Inspect Castle: walks over everything connected to the block you looked at, a few hundred blocks a tick so nobody feels it.
// Natural blocks, plants and logs are only part of it if you placed them yourself, so it doesn't wander off through the hillside.
public final class CastleScan {
    private static final int PER_TICK = 500;

    private final ServerLevel level;
    private final UUID who;
    private final HouseData.Placed placed;
    private final LongOpenHashSet seen = new LongOpenHashSet();
    private final LongArrayFIFOQueue todo = new LongArrayFIFOQueue();
    private final LongArrayList solid = new LongArrayList();
    private int yours, minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    boolean capped;

    public CastleScan(ServerLevel level, UUID who, BlockPos start) {
        this.level = level;
        this.who = who;
        this.placed = HouseData.Placed.of(level);
        seen.add(start.asLong());
        todo.enqueue(start.asLong());
    }

    // True when there's nothing left to look at.
    public boolean tick() {
        BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        for (int n = 0; n < PER_TICK && !todo.isEmpty(); n++) {
            if (seen.size() >= HouseRules.SCAN_CAP) {
                capped = true;
                while (!todo.isEmpty()) count(BlockPos.of(todo.dequeueLong()));
                return true;
            }
            BlockPos at = BlockPos.of(todo.dequeueLong());
            if (!count(at)) continue;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        near.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                        long key = near.asLong();
                        if (seen.contains(key) || !level.isLoaded(near) || !isPart(near, level.getBlockState(near))) continue;
                        seen.add(key);
                        todo.enqueue(key);
                    }
                }
            }
        }
        return todo.isEmpty();
    }

    private boolean count(BlockPos at) {
        BlockState s = level.getBlockState(at);
        if (!isPart(at, s)) return false;
        if (placed.has(who, at)) yours++;
        if (!isOpening(s)) {
            solid.add(at.asLong());
            minX = Math.min(minX, at.getX());
            maxX = Math.max(maxX, at.getX());
            minZ = Math.min(minZ, at.getZ());
            maxZ = Math.max(maxZ, at.getZ());
            minY = Math.min(minY, at.getY());
        }
        return true;
    }

    private boolean isPart(BlockPos at, BlockState s) {
        if (s.isAir() || (!s.getFluidState().isEmpty() && s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) return false;
        if (s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(HouseSite.NATURAL)) return placed.has(who, at);
        return isOpening(s) || !s.getCollisionShape(level, at).isEmpty();
    }

    private static boolean isOpening(BlockState s) {
        return s.is(BlockTags.DOORS) || s.is(BlockTags.FENCE_GATES) || s.is(BlockTags.TRAPDOORS);
    }

    public int yours() {
        return yours;
    }

    public int total() {
        return solid.size();
    }

    // Everything still missing, in plain words. Empty = it passes.
    public List<String> verdict() {
        List<String> out = new ArrayList<>();
        int need = Setting.HOUSE_CASTLE.i();
        if (yours < need) out.add(yours + " of " + need + " blocks are yours (only blocks placed since mastery " + HouseRules.REMEMBER_FROM_LEVEL + " count).");
        if (solid.isEmpty()) return out;
        long area = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        if (area > 4_000_000L) {
            out.add("That's too spread out for me to call it one castle.");
            return out;
        }
        CastleRule rule = new CastleRule(maxX - minX + 1, maxZ - minZ + 1);
        for (int i = 0; i < solid.size(); i++) {
            long key = solid.getLong(i);
            rule.solid(BlockPos.getX(key) - minX, BlockPos.getY(key) - minY, BlockPos.getZ(key) - minZ);
        }
        out.addAll(rule.missing());
        return out;
    }
}
