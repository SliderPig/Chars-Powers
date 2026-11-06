package com.example.charpowers.life;

import com.example.charpowers.life.model.LifeRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// One key sorts every chest and barrel near you: everything is pulled together, stacked, put in name order, then laid
// back out starting with the nearest chest, so like things end up side by side.
final class ChestSorter {
    private ChestSorter() {}

    // How many chests it sorted.
    static int sort(ServerPlayer p, int range) {
        ServerLevel level = p.serverLevel();
        List<BaseContainerBlockEntity> chests = find(p, level, range);
        if (chests.isEmpty()) return 0;
        List<ItemStack> all = new ArrayList<>();
        for (BaseContainerBlockEntity c : chests) {
            if (c instanceof RandomizableContainerBlockEntity r) r.unpackLootTable(p);
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack stack = c.removeItemNoUpdate(i);
                if (!stack.isEmpty()) merge(all, stack);
            }
        }
        all.sort(Comparator.comparing(ChestSorter::name));
        int next = 0;
        for (BaseContainerBlockEntity c : chests) {
            for (int i = 0; i < c.getContainerSize() && next < all.size(); i++) c.setItem(i, all.get(next++));
            c.setChanged();
        }
        return chests.size();
    }

    private static String name(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    private static void merge(List<ItemStack> all, ItemStack stack) {
        for (ItemStack have : all) {
            if (stack.isEmpty()) return;
            if (have.getCount() < have.getMaxStackSize() && ItemStack.isSameItemSameTags(have, stack)) {
                int move = Math.min(stack.getCount(), have.getMaxStackSize() - have.getCount());
                have.grow(move);
                stack.shrink(move);
            }
        }
        if (!stack.isEmpty()) all.add(stack);
    }

    private static List<BaseContainerBlockEntity> find(ServerPlayer p, ServerLevel level, int range) {
        List<BaseContainerBlockEntity> out = new ArrayList<>();
        ChunkPos from = new ChunkPos(p.blockPosition().offset(-range, 0, -range)), to = new ChunkPos(p.blockPosition().offset(range, 0, range));
        double reach = (double) range * range;
        for (int cx = from.x; cx <= to.x; cx++) {
            for (int cz = from.z; cz <= to.z; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if ((be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) && be instanceof BaseContainerBlockEntity c && c.canOpen(p)
                            && be.getBlockPos().distSqr(p.blockPosition()) <= reach) out.add(c);
                }
            }
        }
        out.sort(Comparator.comparingDouble(c -> c.getBlockPos().distSqr(p.blockPosition())));
        return out.size() > LifeRules.SORT_MAX_CHESTS ? new ArrayList<>(out.subList(0, LifeRules.SORT_MAX_CHESTS)) : out;
    }
}
