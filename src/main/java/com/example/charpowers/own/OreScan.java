package com.example.charpowers.own;

import com.example.charpowers.cloning.model.OreRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Looks for ores around a spot, a bit each tick. Whole chunk sections with no ore in them are skipped at once, and
// unloaded chunks are left alone. Used by Future Sight and The Miner.
public final class OreScan {
    private static final int MAX_FOUND = 4096;
    private static final Map<Block, Boolean> KNOWN = new ConcurrentHashMap<>();

    public final ServerLevel level;
    public final BlockPos center;
    private final int range, minCx, maxCx, minCz, maxCz, minSection, maxSection;
    private int cx, cz, section;
    private final List<BlockPos> found = new ArrayList<>();
    public String bestName;
    public BlockPos bestPos;
    private final Map<String, Integer> counts = new HashMap<>();
    private int bestRank = -1;
    private double bestDistance;

    public OreScan(ServerLevel level, BlockPos center, int range) {
        this.level = level;
        this.center = center;
        this.range = range;
        minCx = (center.getX() - range) >> 4;
        maxCx = (center.getX() + range) >> 4;
        minCz = (center.getZ() - range) >> 4;
        maxCz = (center.getZ() + range) >> 4;
        minSection = Math.max(0, level.getSectionIndex(center.getY() - range));
        maxSection = Math.min(level.getSectionsCount() - 1, level.getSectionIndex(center.getY() + range));
        cx = minCx;
        cz = minCz;
        section = minSection;
    }

    // The forge:ores tag, or going by the name. Remembered per block, as this gets asked a lot.
    public static boolean isOre(BlockState state) {
        return KNOWN.computeIfAbsent(state.getBlock(), block -> {
            var key = ForgeRegistries.BLOCKS.getKey(block);
            return state.is(Tags.Blocks.ORES) || (key != null && OreRules.looksLikeOre(key.getPath()));
        });
    }

    // Returns true once it has looked everywhere.
    public boolean step(int budget) {
        while (budget > 0) {
            if (cx > maxCx) return true;
            budget -= 64;
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk != null) {
                LevelChunkSection s = chunk.getSection(section);
                if (!s.hasOnlyAir() && s.maybeHas(OreScan::isOre)) {
                    look(s, cx, level.getSectionYFromSectionIndex(section), cz);
                    budget -= 4096;
                }
            }
            if (++section > maxSection) {
                section = minSection;
                if (++cz > maxCz) {
                    cz = minCz;
                    cx++;
                }
            }
        }
        return cx > maxCx;
    }

    private void look(LevelChunkSection s, int chunkX, int sectionY, int chunkZ) {
        for (int y = 0; y < 16; y++) {
            int wy = (sectionY << 4) + y;
            if (Math.abs(wy - center.getY()) > range) continue;
            for (int z = 0; z < 16; z++) {
                int wz = (chunkZ << 4) + z;
                if (Math.abs(wz - center.getZ()) > range) continue;
                for (int x = 0; x < 16; x++) {
                    int wx = (chunkX << 4) + x;
                    if (Math.abs(wx - center.getX()) > range) continue;
                    BlockState state = s.getBlockState(x, y, z);
                    if (isOre(state)) note(state, new BlockPos(wx, wy, wz));
                }
            }
        }
    }

    private void note(BlockState state, BlockPos pos) {
        if (found.size() < MAX_FOUND) found.add(pos);
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String name = key == null ? "ore" : key.getPath();
        int rank = OreRules.rank(name);
        double distance = pos.distSqr(center);
        counts.merge(OreRules.family(name), 1, Integer::sum);
        if (rank > bestRank || (rank == bestRank && distance < bestDistance)) {
            bestRank = rank;
            bestName = name;
            bestPos = pos;
            bestDistance = distance;
        }
    }

    // How many of the ore it's going to talk about, counted as one kind (deepslate or not).
    public int bestCount() {
        return bestName == null ? 0 : counts.getOrDefault(OreRules.family(bestName), 0);
    }

    public List<BlockPos> nearest(int count) {
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return new ArrayList<>(found.subList(0, Math.min(count, found.size())));
    }
}
