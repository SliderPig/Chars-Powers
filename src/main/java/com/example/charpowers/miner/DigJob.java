package com.example.charpowers.miner;

import com.example.charpowers.hidden.attack.Wreck;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.miner.model.MinerRules;
import com.example.charpowers.own.OreScan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// One big digging job (staircase, ball, whole chunk), a bit each tick, natural blocks only. Drops are added up and
// handed over at the end instead of filling the world with thousands of items.
final class DigJob {
    final UUID player;
    final ServerLevel level;
    final boolean counts;
    private final Iterator<BlockPos> spots;
    private final int perTick;
    private final boolean rubbleDrops;
    private final Map<Item, Integer> ores = new LinkedHashMap<>(), rubble = new LinkedHashMap<>(), junk = new LinkedHashMap<>();
    private final Map<Item, Integer> rank = new java.util.HashMap<>();
    int broken;

    // rubbleDrops = false means only ores drop. counts = these blocks count for the awakening quest.
    DigJob(ServerPlayer p, Iterator<BlockPos> spots, int perTick, boolean rubbleDrops, boolean counts) {
        this.player = p.getUUID();
        this.level = p.serverLevel();
        this.spots = spots;
        this.perTick = perTick;
        this.rubbleDrops = rubbleDrops;
        this.counts = counts;
    }

    // Returns true when finished.
    boolean step(ServerPlayer p) {
        p.fallDistance = 0;
        int done = 0, looked = 0;
        ItemStack tool = p.getMainHandItem().isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                ? p.getMainHandItem() : new ItemStack(Items.NETHERITE_PICKAXE);
        while (spots.hasNext() && done < perTick && looked < perTick * 16) {
            BlockPos pos = spots.next();
            looked++;
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if ((!state.is(TheMiner.NATURAL) && !OreScan.isOre(state)) || !Wreck.mayBreak(level, pos, p)) continue;
            collect(state, pos, p, tool);
            level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
            if (done % 12 == 0) level.levelEvent(2001, pos, Block.getId(state));
            done++;
            broken++;
        }
        return !spots.hasNext();
    }

    private void collect(BlockState state, BlockPos pos, ServerPlayer p, ItemStack tool) {
        boolean ore = OreScan.isOre(state);
        if (!ore && !rubbleDrops) return;
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        int oreRank = ore && key != null ? MinerRules.oreRank(key.getPath()) : 0;
        for (ItemStack drop : Block.getDrops(state, level, pos, null, p, tool)) {
            var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(drop.getItem());
            Map<Item, Integer> into = ore ? ores : id != null && MinerRules.isJunk(id.getPath()) ? junk : rubble;
            into.merge(drop.getItem(), drop.getCount(), Integer::sum);
            if (ore) rank.merge(drop.getItem(), oreRank, Math::max);
        }
    }

    // Ores always reach you, the rarest first (so a full bag keeps the best). Plain junk (dirt, cobble...) is dropped at
    // your feet, not picked up. Both stop at 64 stacks, because thousands of stacks on the ground would lag the server.
    void handOver(ServerPlayer p) {
        List<Map.Entry<Item, Integer>> byRarity = new ArrayList<>(ores.entrySet());
        byRarity.sort((a, b) -> Integer.compare(rank.getOrDefault(b.getKey(), 0), rank.getOrDefault(a.getKey(), 0)));
        byRarity.forEach(e -> give(p, e.getKey(), e.getValue()));
        int stacks = MinerRules.RUBBLE_STACKS;
        for (Map.Entry<Item, Integer> e : rubble.entrySet()) {
            int count = Math.min(e.getValue(), stacks * e.getKey().getMaxStackSize());
            give(p, e.getKey(), count);
            stacks -= (count + e.getKey().getMaxStackSize() - 1) / e.getKey().getMaxStackSize();
            if (stacks <= 0) break;
        }
        stacks = MinerRules.RUBBLE_STACKS;
        for (Map.Entry<Item, Integer> e : junk.entrySet()) {
            int max = e.getKey().getMaxStackSize();
            int count = Math.min(e.getValue(), stacks * max);
            for (int left = count; left > 0; left -= max) {
                p.drop(new ItemStack(e.getKey(), Math.min(left, max)), false);
            }
            stacks -= (count + max - 1) / max;
            if (stacks <= 0) break;
        }
    }

    private static void give(ServerPlayer p, Item item, int count) {
        int stack = item.getMaxStackSize();
        while (count > 0) {
            int n = Math.min(count, stack);
            Fx.give(p, new ItemStack(item, n));
            count -= n;
        }
    }
}
