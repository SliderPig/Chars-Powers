package com.example.charpowers.house;

import com.example.charpowers.CharPowers;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// What the power remembers in the world's save: houses still going up (so a restart doesn't leave half a house), and per dimension
// which blocks each nearly-mastered builder placed, packed per chunk, for the castle quest.
public final class HouseData {
    public static final int MAX_REMEMBERED = 200_000;

    private HouseData() {}

    public static final class Jobs extends SavedData {
        final List<BuildJob> jobs = new ArrayList<>();

        static Jobs of(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(Jobs::load, Jobs::new, CharPowers.MODID + "_house_jobs");
        }

        private static Jobs load(CompoundTag tag) {
            Jobs out = new Jobs();
            ListTag list = tag.getList("jobs", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                try {
                    out.jobs.add(BuildJob.load(list.getCompound(i)));
                } catch (RuntimeException e) {
                    CharPowers.LOG.warn("Dropped a saved house job I can't read", e);
                }
            }
            return out;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (BuildJob job : jobs) list.add(job.save());
            tag.put("jobs", list);
            return tag;
        }
    }

    public static final class Placed extends SavedData {
        private final Map<UUID, Long2ObjectOpenHashMap<IntOpenHashSet>> byPlayer = new HashMap<>();
        private final Map<UUID, Integer> counts = new HashMap<>();
        private int minY;

        public static Placed of(ServerLevel level) {
            Placed p = level.getDataStorage().computeIfAbsent(Placed::load, Placed::new, CharPowers.MODID + "_house_placed");
            p.minY = level.getMinBuildHeight();
            return p;
        }

        private int local(BlockPos pos) {
            return (pos.getX() & 15) | (pos.getZ() & 15) << 4 | (pos.getY() - minY) << 8;
        }

        public void add(UUID who, BlockPos pos) {
            if (counts.getOrDefault(who, 0) >= MAX_REMEMBERED) return;
            if (byPlayer.computeIfAbsent(who, k -> new Long2ObjectOpenHashMap<>())
                    .computeIfAbsent(ChunkPos.asLong(pos), k -> new IntOpenHashSet()).add(local(pos))) {
                counts.merge(who, 1, Integer::sum);
                setDirty();
            }
        }

        public boolean has(UUID who, BlockPos pos) {
            Long2ObjectOpenHashMap<IntOpenHashSet> chunks = byPlayer.get(who);
            if (chunks == null) return false;
            IntOpenHashSet set = chunks.get(ChunkPos.asLong(pos));
            return set != null && set.contains(local(pos));
        }

        public void forget(UUID who) {
            if (byPlayer.remove(who) != null) {
                counts.remove(who);
                setDirty();
            }
        }

        private static Placed load(CompoundTag tag) {
            Placed out = new Placed();
            for (String key : tag.getAllKeys()) {
                try {
                    UUID who = UUID.fromString(key);
                    Long2ObjectOpenHashMap<IntOpenHashSet> chunks = new Long2ObjectOpenHashMap<>();
                    ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
                    int count = 0;
                    for (int i = 0; i < list.size(); i++) {
                        CompoundTag c = list.getCompound(i);
                        int[] cells = c.getIntArray("v");
                        IntOpenHashSet set = new IntOpenHashSet(cells);
                        chunks.put(c.getLong("k"), set);
                        count += cells.length;
                    }
                    out.byPlayer.put(who, chunks);
                    out.counts.put(who, count);
                } catch (RuntimeException e) {
                    CharPowers.LOG.warn("Dropped placed blocks I can't read for {}", key, e);
                }
            }
            return out;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            byPlayer.forEach((who, chunks) -> {
                ListTag list = new ListTag();
                chunks.long2ObjectEntrySet().forEach(e -> {
                    CompoundTag c = new CompoundTag();
                    c.putLong("k", e.getLongKey());
                    c.putIntArray("v", e.getValue().toIntArray());
                    list.add(c);
                });
                tag.put(who.toString(), list);
            });
            return tag;
        }
    }
}
