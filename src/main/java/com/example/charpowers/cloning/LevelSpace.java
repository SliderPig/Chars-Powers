package com.example.charpowers.cloning;

import com.example.charpowers.cloning.model.AirSight;
import com.example.charpowers.own.OreScan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;

import java.util.IdentityHashMap;
import java.util.Map;

// The loaded world as AirSight sees it. Unloaded chunks count as rock. Nothing is loaded for it.
public final class LevelSpace implements AirSight.Space {
    private final ServerLevel level;
    private final Map<BlockState, Boolean> openKnown = new IdentityHashMap<>();
    private LevelChunk last;
    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;

    public LevelSpace(ServerLevel level) {
        this.level = level;
    }

    private BlockState state(int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return Blocks.STONE.defaultBlockState();
        int cx = x >> 4, cz = z >> 4;
        if (cx != lastX || cz != lastZ) {
            last = level.getChunkSource().getChunkNow(cx, cz);
            lastX = cx;
            lastZ = cz;
        }
        if (last == null) return Blocks.STONE.defaultBlockState();
        return last.getBlockState(new BlockPos(x, y, z));
    }

    private static boolean lava(BlockState s) {
        return s.getFluidState().is(Fluids.LAVA) || s.getFluidState().is(Fluids.FLOWING_LAVA) || s.is(Blocks.LAVA_CAULDRON);
    }

    private boolean isOpen(BlockState s) {
        return openKnown.computeIfAbsent(s, st -> {
            if (lava(st)) return false;
            try {
                return st.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
            } catch (RuntimeException e) {
                return false;
            }
        });
    }

    @Override
    public boolean open(int x, int y, int z) {
        return isOpen(state(x, y, z));
    }

    @Override
    public boolean ore(int x, int y, int z) {
        return OreScan.isOre(state(x, y, z));
    }

    @Override
    public boolean lavaNear(int x, int y, int z) {
        return lava(state(x + 1, y, z)) || lava(state(x - 1, y, z)) || lava(state(x, y + 1, z)) || lava(state(x, y - 1, z))
                || lava(state(x, y, z + 1)) || lava(state(x, y, z - 1));
    }

    @Override
    public boolean standable(int x, int y, int z) {
        return open(x, y, z) && open(x, y + 1, z) && !isOpen(state(x, y - 1, z)) && !lava(state(x, y - 1, z)) && state(x, y - 1, z).getFluidState().isEmpty();
    }

    @Override
    public boolean sky(int x, int y, int z) {
        return level.canSeeSky(new BlockPos(x, y, z));
    }
}
