package com.example.charpowers.house;

import com.example.charpowers.CharPowers;
import com.example.charpowers.house.model.HouseRules;
import com.example.charpowers.house.model.HouseTurn;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

// Can a house go here? The same check runs on the PC (to colour the ghost) and on the server (which decides).
// Plants, wild trees and small bumps of terrain get cleared; anything that looks built, and anything holding things, is left
// alone and the house is refused. Claims are asked about separately on the server (HouseBuilder), before the cooldown starts.
public final class HouseSite {
    public static final TagKey<Block> NATURAL = BlockTags.create(new ResourceLocation(CharPowers.MODID, "natural"));

    public record Result(boolean ok, String why, int y) {
        static Result no(String why, int y) {
            return new Result(false, why, y);
        }
    }

    private enum Kind { CLEAR, BUILT, HOLDER, FLUID }

    private HouseSite() {}

    // (cx, cz) = the middle of the house, around = roughly the height of the ground there. The floor height comes back in the result.
    public static Result check(Level level, int cx, int cz, int around, int w, int h, int d, int turns) {
        return check(level, cx, cz, around, w, h, d, turns, false);
    }

    // rough = look at one column in nine on a very big plot, for the ghost on the PC.
    public static Result check(Level level, int cx, int cz, int around, int w, int h, int d, int turns, boolean rough) {
        int step = rough && (long) w * d * h > 300_000 ? 3 : 1;
        int[] rect = HouseTurn.rect(w, d, cx, cz, turns);
        if (!level.hasChunksAt(new BlockPos(rect[0], around, rect[1]), new BlockPos(rect[2], around, rect[3]))) {
            return Result.no("That's too far away to see the ground", around);
        }
        int columns = ((rect[2] - rect[0]) / step + 1) * ((rect[3] - rect[1]) / step + 1);
        int[] grounds = new int[columns];
        int n = 0;
        int wet = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = rect[0]; x <= rect[2]; x += step) {
            for (int z = rect[1]; z <= rect[3]; z += step) {
                int top = around + 24;
                int low = Math.max(level.getMinBuildHeight(), around - 32);
                int y = Math.min(top, level.getMaxBuildHeight() - 1);
                boolean water = false;
                while (y > low) {
                    BlockState s = level.getBlockState(at.set(x, y, z));
                    if (!s.getFluidState().isEmpty()) water = true;
                    if (isGround(s)) break;
                    y--;
                }
                if (water) wet++;
                grounds[n++] = y;
            }
        }
        int[] sorted = grounds.clone();
        Arrays.sort(sorted);
        int floor = sorted[sorted.length / 2];
        if (floor + h >= level.getMaxBuildHeight()) return Result.no("There isn't room for it under the sky", floor);
        if (wet > columns * HouseRules.MAX_WET_SHARE) return Result.no("Too much of the ground here is under water", floor);
        for (int ground : grounds) {
            if (ground - floor > HouseRules.MAX_CUT) return Result.no("A hill is in the way", floor);
            if (floor - ground > HouseRules.MAX_DROP) return Result.no("The ground drops away too far here", floor);
        }
        for (int x = rect[0]; x <= rect[2]; x += step) {
            for (int z = rect[1]; z <= rect[3]; z += step) {
                for (int y = floor; y < floor + h; y++) {
                    at.set(x, y, z);
                    BlockState s = level.getBlockState(at);
                    if (s.isAir()) continue;
                    Kind kind = kindOf(level, at, s);
                    if (y == floor && kind != Kind.HOLDER) {
                        if (s.getFluidState().is(FluidTags.LAVA)) return Result.no("There's lava in the way", floor);
                        // the ground layer itself gets replaced, but never somebody's floor
                        if (kind == Kind.BUILT) return Result.no(what(s) + " is in the way at " + spot(at), floor);
                        continue;
                    }
                    switch (kind) {
                        case HOLDER -> {
                            return Result.no(what(s) + " at " + spot(at) + " is in the way, I won't touch anything that holds things", floor);
                        }
                        case BUILT -> {
                            return Result.no(what(s) + " is in the way at " + spot(at), floor);
                        }
                        case FLUID -> {
                            return Result.no(s.getFluidState().is(FluidTags.LAVA) ? "There's lava in the way" : "There's water in the way", floor);
                        }
                        default -> {
                        }
                    }
                }
            }
        }
        return new Result(true, null, floor);
    }

    // Solid ground to stand a foundation on.
    public static boolean isGround(BlockState s) {
        return !s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced() && !isPlant(s) && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS);
    }

    // When the job lists what to clear, trees still have their leaves.
    public static boolean mayClear(BlockGetter level, BlockPos at, BlockState s) {
        return kindOf(level, at, s) == Kind.CLEAR || (!s.getFluidState().isEmpty() && !s.hasBlockEntity());
    }

    // While it clears, top down, the leaves have gone: this only stops it touching something placed since the list was made.
    public static boolean stillClearable(BlockState s) {
        if (s.hasBlockEntity()) return false;
        if (!s.getFluidState().isEmpty() || s.canBeReplaced() || isPlant(s) || s.is(BlockTags.LOGS)) return true;
        if (s.is(BlockTags.LEAVES)) return s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT);
        return s.is(NATURAL);
    }

    private static Kind kindOf(BlockGetter level, BlockPos at, BlockState s) {
        if (s.hasBlockEntity()) return Kind.HOLDER;
        if (!s.getFluidState().isEmpty()) return Kind.FLUID;
        if (s.canBeReplaced() || isPlant(s)) return Kind.CLEAR;
        if (s.is(BlockTags.LEAVES)) return s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT) ? Kind.CLEAR : Kind.BUILT;
        if (s.is(BlockTags.LOGS)) return isWildTree(level, at) ? Kind.CLEAR : Kind.BUILT;
        return s.is(NATURAL) ? Kind.CLEAR : Kind.BUILT;
    }

    private static boolean isPlant(BlockState s) {
        Block b = s.getBlock();
        if (b instanceof CropBlock || b instanceof StemBlock) return false;
        return b instanceof BushBlock || b instanceof VineBlock || b instanceof BambooStalkBlock || b instanceof CactusBlock || b instanceof SugarCaneBlock || b instanceof HugeMushroomBlock
                || b instanceof CocoaBlock || s.is(Blocks.MOSS_CARPET);
    }

    // A log is part of a wild tree if wild leaves grow near it. Nobody's log cabin has those.
    private static boolean isWildTree(BlockGetter level, BlockPos at) {
        BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 12; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockState s = level.getBlockState(near.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz));
                    if (s.is(BlockTags.LEAVES) && s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT)) return true;
                }
            }
        }
        return false;
    }

    private static String what(BlockState s) {
        return s.getBlock().getName().getString();
    }

    private static String spot(BlockPos at) {
        return at.getX() + " " + at.getY() + " " + at.getZ();
    }

    // For the build job: does this spot hold something it must never overwrite?
    public static boolean holdsThings(Level level, BlockPos at) {
        BlockEntity be = level.getBlockEntity(at);
        return be != null && (be instanceof Container || !level.getBlockState(at).is(BlockTags.ALL_SIGNS));
    }
}
