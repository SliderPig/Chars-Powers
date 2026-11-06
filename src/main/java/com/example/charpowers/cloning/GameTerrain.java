package com.example.charpowers.cloning;

import com.example.charpowers.cloning.model.RoutePlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

// The real world as the planner sees it. Made on the server thread, used on the pathing thread: it holds the loaded chunks
// around the clone (the way Baritone reads them) and what it's carrying, and never loads a chunk or changes anything.
// What each kind of block means is only worked out once per search.
final class GameTerrain implements RoutePlanner.Terrain {
    private static final int RADIUS_CHUNKS = 8, TOO_LONG_TO_DIG = 400;
    private static final double LOW_ENOUGH_TO_WALK_THROUGH = 0.2;

    private record Info(boolean open, boolean floor, boolean water, boolean lava, boolean fill, boolean falling, double ticks) {}

    private final LevelChunk[][] chunks;
    private final int minX, minZ, minY, maxY;
    private final RouteWalker.DigRule rule;
    private final List<ItemStack> tools = new ArrayList<>();
    private final Map<BlockState, Info> infos = new IdentityHashMap<>();
    private static final Info OUTSIDE = new Info(false, false, false, false, false, false, -1);

    GameTerrain(CloneEntity c, RouteWalker.DigRule rule) {
        this.rule = rule;
        ServerLevel level = c.serverLevel();
        minX = (c.getBlockX() >> 4) - RADIUS_CHUNKS;
        minZ = (c.getBlockZ() >> 4) - RADIUS_CHUNKS;
        minY = level.getMinBuildHeight();
        maxY = level.getMaxBuildHeight();
        int size = RADIUS_CHUNKS * 2 + 1;
        chunks = new LevelChunk[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) chunks[x][z] = level.getChunkSource().getChunkNow(minX + x, minZ + z);
        }
        for (ItemStack s : c.getInventory().items) {
            if (!s.isEmpty() && s.isDamageableItem()) tools.add(s);
        }
    }

    private BlockState state(int x, int y, int z) {
        if (y < minY || y >= maxY) return null;
        int cx = (x >> 4) - minX, cz = (z >> 4) - minZ;
        if (cx < 0 || cz < 0 || cx >= chunks.length || cz >= chunks.length) return null;
        LevelChunk chunk = chunks[cx][cz];
        if (chunk == null) return null;
        LevelChunkSection section = chunk.getSection((y - minY) >> 4);
        return section.getBlockState(x & 15, y & 15, z & 15);
    }

    private Info info(int x, int y, int z) {
        BlockState state = state(x, y, z);
        if (state == null) return OUTSIDE;
        Info known = infos.get(state);
        if (known == null) {
            known = work(state);
            infos.put(state, known);
        }
        return known;
    }

    private static boolean hurts(BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.COBWEB) || state.is(Blocks.WITHER_ROSE);
    }

    private Info work(BlockState state) {
        boolean lava = state.getFluidState().is(Fluids.LAVA) || state.getFluidState().is(Fluids.FLOWING_LAVA) || state.is(Blocks.LAVA_CAULDRON);
        boolean water = !lava && !state.getFluidState().isEmpty();
        boolean hurt = hurts(state);
        VoxelShape shape;
        try {
            shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException e) {
            return new Info(false, true, false, false, false, false, -1); // a modded block that won't say: treat it as a wall
        }
        double height = shape.isEmpty() ? 0 : shape.max(net.minecraft.core.Direction.Axis.Y);
        boolean low = height <= LOW_ENOUGH_TO_WALK_THROUGH;
        boolean doorway = state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.FENCE_GATES);
        boolean open = !lava && !hurt && (low || doorway);
        boolean floor = !shape.isEmpty() && !hurt && !doorway && height <= 1.0 && !lava;
        boolean fill = !lava && !doorway && state.canBeReplaced() && !hurt;
        boolean falling = state.getBlock() instanceof FallingBlock;
        return new Info(open, floor, water, lava, fill, falling, ticksFor(state));
    }

    // How long the best tool it's carrying takes, the game's own sum. Negative if it isn't to be dug.
    private double ticksFor(BlockState state) {
        if (!RouteWalker.allowed(rule, state)) return -1;
        float hardness = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (hardness < 0) return -1;
        float speed = 1f;
        boolean right = !state.requiresCorrectToolForDrops();
        for (ItemStack s : tools) {
            speed = Math.max(speed, s.getDestroySpeed(state));
            right = right || s.isCorrectToolForDrops(state);
        }
        double ticks = Math.max(1, Math.ceil(hardness * (right ? 30f : 100f) / speed));
        return ticks > TOO_LONG_TO_DIG ? -1 : ticks;
    }

    @Override
    public boolean open(int x, int y, int z) {
        return info(x, y, z).open();
    }

    @Override
    public boolean floor(int x, int y, int z) {
        return info(x, y, z).floor();
    }

    @Override
    public boolean water(int x, int y, int z) {
        return info(x, y, z).water();
    }

    @Override
    public boolean canFill(int x, int y, int z) {
        return info(x, y, z).fill();
    }

    @Override
    public double digCost(int x, int y, int z) {
        Info here = info(x, y, z);
        if (here == OUTSIDE || here.lava()) return -1;
        if (here.open()) return 0;
        if (here.ticks() < 0) return -1;
        // opening up lava is how people die in mines. Water only matters right above you: it just pushes you about.
        if (info(x + 1, y, z).lava() || info(x - 1, y, z).lava() || info(x, y, z + 1).lava() || info(x, y, z - 1).lava() || info(x, y - 1, z).lava()) return -1;
        Info above = info(x, y + 1, z);
        if (above.lava() || above.water()) return -1;
        // sand and gravel come down when you dig under them, and have to be dug too
        double total = here.ticks();
        for (int up = 1; up <= 8 && above.falling(); up++) {
            if (above.ticks() < 0) return -1;
            total += above.ticks();
            above = info(x, y + up + 1, z);
            if (above.lava() || above.water()) return -1;
        }
        return total;
    }
}
