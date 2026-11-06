package com.example.charpowers.house;

import com.example.charpowers.house.model.IsoThumb;
import com.example.charpowers.house.model.SchematicReader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

// One house ready to build: blocks kept bottom layer first, which is the order they go up in. Block entity data is never kept, so a chest
// in a file arrives empty. Own files also carry a little picture made from their blocks, since they have no drawn one.
public final class HousePlan {
    public record Cell(int x, int y, int z, BlockState state) {}

    public final String id, name;
    public final int w, h, d, level;
    public final boolean awakened, custom;
    public final List<Cell> cells;
    public final byte[] thumb;

    private HousePlan(String id, String name, int w, int h, int d, int level, boolean awakened, boolean custom, List<Cell> cells, byte[] thumb) {
        this.id = id;
        this.name = name;
        this.w = w;
        this.h = h;
        this.d = d;
        this.level = level;
        this.awakened = awakened;
        this.custom = custom;
        this.cells = cells;
        this.thumb = thumb;
    }

    // resolve turns "minecraft:oak_stairs[facing=east]" into a block, or null to leave it out. Null comes back if nothing is left to build.
    public static HousePlan of(String id, String name, int level, boolean awakened, boolean custom, SchematicReader.Result r, Function<String, BlockState> resolve) {
        BlockState[] palette = new BlockState[r.palette().size()];
        boolean[] looked = new boolean[palette.length];
        int[] raw = r.cells();
        List<Cell> cells = new ArrayList<>(raw.length / 4);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = -1, maxY = -1, maxZ = -1;
        for (int i = 0; i < raw.length; i += 4) {
            int p = raw[i + 3];
            if (!looked[p]) {
                looked[p] = true;
                palette[p] = resolve.apply(r.palette().get(p));
            }
            if (palette[p] == null) continue;
            cells.add(new Cell(raw[i], raw[i + 1], raw[i + 2], palette[p]));
            minX = Math.min(minX, raw[i]);
            minY = Math.min(minY, raw[i + 1]);
            minZ = Math.min(minZ, raw[i + 2]);
            maxX = Math.max(maxX, raw[i]);
            maxY = Math.max(maxY, raw[i + 1]);
            maxZ = Math.max(maxZ, raw[i + 2]);
        }
        if (cells.isEmpty()) return null;
        // own files often have empty space round them, which would make the plot bigger than the house
        int w = r.w(), h = r.h(), d = r.d();
        if (custom) {
            List<Cell> moved = new ArrayList<>(cells.size());
            for (Cell c : cells) moved.add(new Cell(c.x() - minX, c.y() - minY, c.z() - minZ, c.state()));
            cells = moved;
            w = maxX - minX + 1;
            h = maxY - minY + 1;
            d = maxZ - minZ + 1;
        }
        cells.sort(Comparator.comparingInt(Cell::y).thenComparingInt(Cell::z).thenComparingInt(Cell::x));
        return new HousePlan(id, name, w, h, d, level, awakened, custom, cells, custom ? picture(cells, w, h, d) : null);
    }

    private static byte[] picture(List<Cell> cells, int w, int h, int d) {
        int[] coloured = new int[cells.size() * 4];
        int n = 0;
        for (Cell c : cells) {
            coloured[n++] = c.x();
            coloured[n++] = c.y();
            coloured[n++] = c.z();
            coloured[n++] = colourOf(c.state());
        }
        return IsoThumb.pack(IsoThumb.render(w, h, d, coloured));
    }

    private static int colourOf(BlockState state) {
        try {
            int colour = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
            return colour == 0 ? 0x8A8A8A : colour;
        } catch (RuntimeException e) {
            return 0x8A8A8A; // some modded block wanted a real world to answer that
        }
    }
}
