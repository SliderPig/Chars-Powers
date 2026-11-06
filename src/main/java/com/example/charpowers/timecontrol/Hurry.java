package com.example.charpowers.timecontrol;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.timecontrol.model.TimeRules;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Hurry up!: the machines near you run 5x as fast. The heaviest thing in this power, so: the nearest 64 machines only,
// and it finds them every 2 seconds, not every tick. Blocks that just move stuff are in the charpowers:not_hurried tag.
final class Hurry {
    static final TagKey<Block> NOT_HURRIED = BlockTags.create(new ResourceLocation(CharPowers.MODID, "not_hurried"));
    private static final int FIND_EVERY = 40;

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private static final class Job {
        final long until;
        final List<Machine<?>> machines = new ArrayList<>();

        Job(long until) {
            this.until = until;
        }
    }

    private record Machine<T extends BlockEntity>(T entity, BlockState state, BlockEntityTicker<T> ticker, int extra) {
        void run(ServerLevel level) {
            if (entity.isRemoved() || level.getBlockState(entity.getBlockPos()) != state) return;
            for (int i = 0; i < extra; i++) ticker.tick(level, entity.getBlockPos(), state, entity);
        }
    }

    private Hurry() {}

    static void start(ServerPlayer p) {
        JOBS.put(p.getUUID(), new Job(p.level().getGameTime() + TimeRules.HURRY_TICKS));
    }

    static boolean active(ServerPlayer p) {
        return JOBS.containsKey(p.getUUID());
    }

    static void clear() {
        JOBS.clear();
    }

    static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Job>> it = JOBS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Job> entry = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            Job job = entry.getValue();
            if (p == null || p.level().getGameTime() >= job.until) {
                it.remove();
                continue;
            }
            ServerLevel level = p.serverLevel();
            if (server.getTickCount() % FIND_EVERY == 0) find(p, level, job);
            for (Machine<?> machine : job.machines) machine.run(level);
        }
    }

    private static void find(ServerPlayer p, ServerLevel level, Job job) {
        job.machines.clear();
        BlockPos at = p.blockPosition();
        int r = Setting.TIME_HURRY.i();
        ChunkPos from = new ChunkPos(at.offset(-r, 0, -r)), to = new ChunkPos(at.offset(r, 0, r));
        List<BlockEntity> near = new ArrayList<>();
        for (int cx = from.x; cx <= to.x; cx++) {
            for (int cz = from.z; cz <= to.z; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be.getBlockPos().distSqr(at) <= r * r && !be.getBlockState().is(NOT_HURRIED)) near.add(be);
                }
            }
        }
        near.sort(Comparator.comparingDouble(be -> be.getBlockPos().distSqr(at)));
        for (BlockEntity be : near) {
            if (job.machines.size() >= TimeRules.HURRY_MAX_BLOCKS) break;
            Machine<?> machine = machine(level, be);
            if (machine != null) job.machines.add(machine);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> Machine<T> machine(ServerLevel level, BlockEntity be) {
        BlockState state = be.getBlockState();
        BlockEntityTicker<T> ticker = state.getTicker(level, (BlockEntityType<T>) be.getType());
        if (ticker == null) return null;
        int extra = state.is(Blocks.SPAWNER) ? TimeRules.SPAWNER_EXTRA : TimeRules.HURRY_EXTRA;
        return new Machine<>((T) be, state, ticker, extra);
    }
}
