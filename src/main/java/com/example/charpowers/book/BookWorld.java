package com.example.charpowers.book;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.SuitItems;
import com.example.charpowers.book.model.BookRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

// Where the one book in the world is, and which marked items no longer exist. The room is built the first time that chunk
// loads, so it works on old worlds too, and nothing shows above ground.
public final class BookWorld extends SavedData {
    private int x, z;
    private boolean chosen, built;
    final Set<String> deadMarks = new HashSet<>();

    static BookWorld of(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(BookWorld::load, BookWorld::new, "charpowers_book_world");
    }

    private static BookWorld load(CompoundTag tag) {
        BookWorld w = new BookWorld();
        w.x = tag.getInt("x");
        w.z = tag.getInt("z");
        w.chosen = tag.getBoolean("chosen");
        w.built = tag.getBoolean("built");
        tag.getList("dead", Tag.TAG_STRING).forEach(t -> w.deadMarks.add(t.getAsString()));
        return w;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("x", x);
        tag.putInt("z", z);
        tag.putBoolean("chosen", chosen);
        tag.putBoolean("built", built);
        ListTag dead = new ListTag();
        for (String mark : deadMarks) dead.add(StringTag.valueOf(mark));
        tag.put("dead", dead);
        return tag;
    }

    void choose(ServerLevel level) {
        if (chosen) return;
        chosen = true;
        RandomSource r = level.getRandom();
        int[] spot = BookRules.spotInRing(r.nextDouble(), r.nextDouble());
        x = spot[0];
        z = spot[1];
        setDirty();
        CharPowers.LOG.info("[Book Keeping] The book is hidden underground near {} {}", x, z);
    }

    String where() {
        return chosen ? "The book is near " + x + " " + z : "The book's spot isn't picked yet.";
    }

    boolean isTheChunk(int chunkX, int chunkZ) {
        return chosen && !built && chunkX == x >> 4 && chunkZ == z >> 4;
    }

    // A run-down little room well under the ground, the book in the only chest.
    void build(ServerLevel level) {
        if (built) return;
        built = true;
        setDirty();
        RandomSource r = level.getRandom();
        int cx = (x & ~15) + 8, cz = (z & ~15) + 8;
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz);
        int floor = Math.max(level.getMinBuildHeight() + 6, Math.min(surface - 30, 10));
        BlockState[] walls = {Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState()};
        for (BlockPos pos : BlockPos.betweenClosed(cx - 5, floor, cz - 5, cx + 5, floor + 4, cz + 5)) {
            boolean shell = Math.abs(pos.getX() - cx) == 5 || Math.abs(pos.getZ() - cz) == 5 || pos.getY() == floor || pos.getY() == floor + 4;
            BlockState state = shell ? walls[r.nextInt(walls.length)] : Blocks.AIR.defaultBlockState();
            if (!shell && r.nextInt(25) == 0) state = Blocks.COBWEB.defaultBlockState();
            level.setBlock(pos, state, 2);
        }
        for (int i = 0; i < 14; i++) { // it's fallen apart a bit
            level.setBlock(new BlockPos(cx - 4 + r.nextInt(9), floor + 1, cz - 4 + r.nextInt(9)), walls[r.nextInt(walls.length)], 2);
        }
        BlockPos chest = new BlockPos(cx, floor + 1, cz);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        level.setBlock(chest.above(), Blocks.AIR.defaultBlockState(), 2);
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity box) {
            box.setItem(13, new ItemStack(SuitItems.THE_BOOK.get()));
            box.setItem(3, new ItemStack(Items.BONE, 2));
            box.setItem(21, new ItemStack(Items.PAPER, 5));
        }
        CharPowers.LOG.info("[Book Keeping] Built the book's room at {}", chest.toShortString());
    }
}
