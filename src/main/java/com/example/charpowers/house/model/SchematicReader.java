package com.example.charpowers.house.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.zip.GZIPInputStream;

// Reads building files into plain block lists: vanilla structures (.nbt), Sponge schematics v1-3 (.schem), Litematica (.litematic, every
// region) and old MCEdit schematics (.schematic). A file is never trusted: sizes are checked before anything is allocated, the unpacked
// data is capped, and block entity data is not read at all, so a chest in a file comes out empty. Nothing here needs the game's registries,
// so block names stay strings like "minecraft:oak_stairs[facing=east,half=bottom]" until the game side looks them up.
public final class SchematicReader {
    public static final int MAX_X = 256, MAX_Y = 128, MAX_Z = 256, MAX_BLOCKS = 200_000, MAX_PALETTE = 65_536, MAX_REGIONS = 64;
    public static final long MAX_FILE_BYTES = 64L * 1024 * 1024, MAX_UNPACKED_BYTES = 256L * 1024 * 1024;

    // A file that can't be used, with a reason a person can read.
    public static final class Bad extends IOException {
        public Bad(String why) {
            super(why);
        }
    }

    // cells = x, y, z, palette index, four ints per block, air already left out.
    public record Result(int w, int h, int d, int dataVersion, List<String> palette, int[] cells, int unmapped) {
        public int blocks() {
            return cells.length / 4;
        }
    }

    private SchematicReader() {}

    public static boolean canRead(String fileName) {
        String n = fileName.toLowerCase(Locale.ROOT);
        return n.endsWith(".nbt") || n.endsWith(".schem") || n.endsWith(".litematic") || n.endsWith(".schematic");
    }

    // legacy maps an old numeric block (id << 4 | data) to a block string, or null if there's no such thing.
    public static Result read(Path file, IntFunction<String> legacy) throws IOException {
        long size = Files.size(file);
        if (size > MAX_FILE_BYTES) throw new Bad("the file is " + size / (1024 * 1024) + " MB, the limit is " + MAX_FILE_BYTES / (1024 * 1024) + " MB");
        CompoundTag root;
        try (InputStream in = Files.newInputStream(file)) {
            root = readTag(in);
        } catch (RuntimeException | StackOverflowError e) {
            // an NBT tag past the limit, or nonsense nested too deep
            throw new Bad("it isn't a readable NBT file (" + e.getMessage() + ")");
        }
        String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (n.endsWith(".litematic")) return litematic(root);
            if (n.endsWith(".schem")) return sponge(root);
            if (n.endsWith(".schematic")) return mcedit(root, legacy);
            return vanilla(root);
        } catch (RuntimeException e) {
            throw new Bad("its contents make no sense (" + e + ")");
        }
    }

    // The NBT itself, gzipped or not, and however it unpacks it can't grow past the cap.
    public static CompoundTag readTag(InputStream raw) throws IOException {
        BufferedInputStream in = new BufferedInputStream(raw);
        in.mark(2);
        int a = in.read(), b = in.read();
        in.reset();
        InputStream unpacked = a == 0x1f && b == 0x8b ? new GZIPInputStream(in) : in;
        try (DataInputStream data = new DataInputStream(new BufferedInputStream(new Capped(unpacked, MAX_UNPACKED_BYTES)))) {
            return NbtIo.read(data, new NbtAccounter(MAX_UNPACKED_BYTES * 2));
        }
    }

    private static final class Capped extends FilterInputStream {
        private long left;

        Capped(InputStream in, long cap) {
            super(in);
            left = cap;
        }

        private void used(long n) throws IOException {
            left -= Math.max(0, n);
            if (left < 0) throw new Bad("it unpacks to more than " + MAX_UNPACKED_BYTES / (1024 * 1024) + " MB");
        }

        @Override
        public int read() throws IOException {
            int v = super.read();
            if (v >= 0) used(1);
            return v;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            used(n);
            return n;
        }
    }

    private static void checkSize(int w, int h, int d) throws Bad {
        if (w < 1 || h < 1 || d < 1) throw new Bad("it says it's " + w + " x " + h + " x " + d + " blocks");
        if (w > MAX_X || h > MAX_Y || d > MAX_Z) {
            throw new Bad("it's " + w + " x " + d + " wide and " + h + " high, the limit is " + MAX_X + " x " + MAX_Z + " wide and " + MAX_Y + " high");
        }
    }

    private static boolean isAir(String state) {
        String name = state;
        int bracket = name.indexOf('[');
        if (bracket >= 0) name = name.substring(0, bracket);
        if (name.startsWith("minecraft:")) name = name.substring(10);
        return name.equals("air") || name.equals("cave_air") || name.equals("void_air") || name.equals("structure_void");
    }

    // Grows as needed, refuses to pass the block limit.
    private static final class Cells {
        int[] data = new int[4096];
        int n;

        void add(int x, int y, int z, int state) throws Bad {
            if (n / 4 >= MAX_BLOCKS) throw new Bad("it has more than " + MAX_BLOCKS + " blocks");
            if (n == data.length) data = java.util.Arrays.copyOf(data, data.length * 2);
            data[n++] = x;
            data[n++] = y;
            data[n++] = z;
            data[n++] = state;
        }

        int[] done() {
            return java.util.Arrays.copyOf(data, n);
        }
    }

    private static String stateOf(CompoundTag entry) {
        String name = entry.getString("Name");
        if (!entry.contains("Properties", Tag.TAG_COMPOUND)) return name;
        CompoundTag props = entry.getCompound("Properties");
        if (props.isEmpty()) return name;
        TreeMap<String, String> sorted = new TreeMap<>();
        for (String key : props.getAllKeys()) sorted.put(key, props.getString(key));
        StringBuilder out = new StringBuilder(name).append('[');
        boolean first = true;
        for (var e : sorted.entrySet()) {
            if (!first) out.append(',');
            first = false;
            out.append(e.getKey()).append('=').append(e.getValue());
        }
        return out.append(']').toString();
    }

    // ---- vanilla structure files

    public static Result vanilla(CompoundTag root) throws Bad {
        ListTag size = root.getList("size", Tag.TAG_INT);
        if (size.size() != 3) throw new Bad("it has no size, so it isn't a structure file");
        int w = size.getInt(0), h = size.getInt(1), d = size.getInt(2);
        checkSize(w, h, d);
        ListTag paletteTag = root.contains("palette", Tag.TAG_LIST) ? root.getList("palette", Tag.TAG_COMPOUND)
                : root.getList("palettes", Tag.TAG_LIST).isEmpty() ? new ListTag() : root.getList("palettes", Tag.TAG_LIST).getList(0);
        if (paletteTag.size() > MAX_PALETTE) throw new Bad("it has too many kinds of block");
        List<String> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) palette.add(stateOf(paletteTag.getCompound(i)));
        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        Cells cells = new Cells();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag e = blocks.getCompound(i);
            ListTag pos = e.getList("pos", Tag.TAG_INT);
            int state = e.getInt("state");
            if (pos.size() != 3 || state < 0 || state >= palette.size() || isAir(palette.get(state))) continue;
            int x = pos.getInt(0), y = pos.getInt(1), z = pos.getInt(2);
            if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d) continue;
            cells.add(x, y, z, state);
        }
        return new Result(w, h, d, root.getInt("DataVersion"), palette, cells.done(), 0);
    }

    // ---- Sponge schematics, versions 1 to 3

    private static Result sponge(CompoundTag root) throws Bad {
        CompoundTag s = root.contains("Schematic", Tag.TAG_COMPOUND) ? root.getCompound("Schematic") : root;
        int w = s.getShort("Width") & 0xFFFF, h = s.getShort("Height") & 0xFFFF, d = s.getShort("Length") & 0xFFFF;
        checkSize(w, h, d);
        CompoundTag paletteTag;
        byte[] data;
        if (s.contains("Blocks", Tag.TAG_COMPOUND)) { // version 3
            CompoundTag blocks = s.getCompound("Blocks");
            paletteTag = blocks.getCompound("Palette");
            data = blocks.getByteArray("Data");
        } else {
            paletteTag = s.getCompound("Palette");
            data = s.getByteArray("BlockData");
        }
        if (paletteTag.size() > MAX_PALETTE) throw new Bad("it has too many kinds of block");
        int top = -1;
        for (String key : paletteTag.getAllKeys()) top = Math.max(top, paletteTag.getInt(key));
        if (top < 0) throw new Bad("it has no block palette");
        if (top >= MAX_PALETTE) throw new Bad("its block palette is broken");
        String[] states = new String[top + 1];
        for (String key : paletteTag.getAllKeys()) {
            int i = paletteTag.getInt(key);
            if (i >= 0) states[i] = key;
        }
        List<String> palette = new ArrayList<>(states.length);
        for (String state : states) palette.add(state == null ? "minecraft:air" : state);
        boolean[] air = new boolean[palette.size()];
        for (int i = 0; i < air.length; i++) air[i] = isAir(palette.get(i));
        Cells cells = new Cells();
        long volume = (long) w * h * d;
        int at = 0;
        for (long i = 0; i < volume; i++) {
            int value = 0, shift = 0, b;
            do {
                if (at >= data.length) throw new Bad("its block data stops early");
                b = data[at++];
                if (shift > 28) throw new Bad("its block data is broken");
                value |= (b & 0x7F) << shift;
                shift += 7;
            } while ((b & 0x80) != 0);
            if (value < 0 || value >= air.length) throw new Bad("its block data is broken");
            if (air[value]) continue;
            int layer = w * d;
            cells.add((int) (i % w), (int) (i / layer), (int) (i / w % d), value);
        }
        return new Result(w, h, d, s.getInt("DataVersion"), palette, cells.done(), 0);
    }

    // ---- Litematica: every region, in one shared box

    private static Result litematic(CompoundTag root) throws Bad {
        CompoundTag regions = root.getCompound("Regions");
        if (regions.isEmpty()) throw new Bad("it has no regions");
        if (regions.size() > MAX_REGIONS) throw new Bad("it has more than " + MAX_REGIONS + " regions");
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (String name : regions.getAllKeys()) {
            CompoundTag region = regions.getCompound(name);
            CompoundTag pos = region.getCompound("Position"), size = region.getCompound("Size");
            int sx = size.getInt("x"), sy = size.getInt("y"), sz = size.getInt("z");
            checkSize(Math.abs(sx), Math.abs(sy), Math.abs(sz));
            int x0 = low(pos.getInt("x"), sx), y0 = low(pos.getInt("y"), sy), z0 = low(pos.getInt("z"), sz);
            minX = Math.min(minX, x0);
            minY = Math.min(minY, y0);
            minZ = Math.min(minZ, z0);
            maxX = Math.max(maxX, x0 + Math.abs(sx) - 1);
            maxY = Math.max(maxY, y0 + Math.abs(sy) - 1);
            maxZ = Math.max(maxZ, z0 + Math.abs(sz) - 1);
        }
        long boxW = (long) maxX - minX + 1, boxH = (long) maxY - minY + 1, boxD = (long) maxZ - minZ + 1;
        if (boxW > MAX_X || boxH > MAX_Y || boxD > MAX_Z) {
            throw new Bad("it's " + boxW + " x " + boxD + " wide and " + boxH + " high, the limit is " + MAX_X + " x " + MAX_Z + " wide and " + MAX_Y + " high");
        }
        List<String> palette = new ArrayList<>();
        Cells cells = new Cells();
        for (String name : regions.getAllKeys()) {
            CompoundTag region = regions.getCompound(name);
            CompoundTag pos = region.getCompound("Position"), size = region.getCompound("Size");
            int rawX = size.getInt("x"), rawY = size.getInt("y"), rawZ = size.getInt("z");
            int sx = Math.abs(rawX), sy = Math.abs(rawY), sz = Math.abs(rawZ);
            int x0 = low(pos.getInt("x"), rawX) - minX, y0 = low(pos.getInt("y"), rawY) - minY, z0 = low(pos.getInt("z"), rawZ) - minZ;
            ListTag paletteTag = region.getList("BlockStatePalette", Tag.TAG_COMPOUND);
            if (paletteTag.isEmpty()) continue;
            if (palette.size() + paletteTag.size() > MAX_PALETTE) throw new Bad("it has too many kinds of block");
            int offset = palette.size();
            boolean[] air = new boolean[paletteTag.size()];
            for (int i = 0; i < paletteTag.size(); i++) {
                String state = stateOf(paletteTag.getCompound(i));
                palette.add(state);
                air[i] = isAir(state);
            }
            int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteTag.size() - 1));
            long mask = (1L << bits) - 1;
            long[] packed = region.getLongArray("BlockStates");
            long volume = (long) sx * sy * sz;
            if (packed.length * 64L < volume * bits) throw new Bad("the block data of region " + name + " stops early");
            long index = 0;
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    for (int x = 0; x < sx; x++, index++) {
                        long bit = index * bits;
                        int word = (int) (bit >>> 6), shift = (int) (bit & 63);
                        long value = packed[word] >>> shift;
                        if (shift + bits > 64) value |= packed[word + 1] << (64 - shift);
                        int state = (int) (value & mask);
                        if (state >= air.length) throw new Bad("the block data of region " + name + " is broken");
                        if (!air[state]) cells.add(x0 + x, y0 + y, z0 + z, offset + state);
                    }
                }
            }
        }
        return new Result((int) boxW, (int) boxH, (int) boxD, root.getInt("MinecraftDataVersion"), palette, cells.done(), 0);
    }

    private static int low(int position, int size) {
        return size < 0 ? position + size + 1 : position;
    }

    // ---- old MCEdit schematics: numbers instead of names

    private static Result mcedit(CompoundTag root, IntFunction<String> legacy) throws Bad {
        CompoundTag s = root.contains("Schematic", Tag.TAG_COMPOUND) ? root.getCompound("Schematic") : root;
        int w = s.getShort("Width") & 0xFFFF, h = s.getShort("Height") & 0xFFFF, d = s.getShort("Length") & 0xFFFF;
        checkSize(w, h, d);
        if (s.contains("Materials", Tag.TAG_STRING) && !"Alpha".equals(s.getString("Materials"))) {
            throw new Bad("its blocks are from before Minecraft had this format (" + s.getString("Materials") + ")");
        }
        byte[] ids = s.getByteArray("Blocks"), metas = s.getByteArray("Data"), add = s.getByteArray("AddBlocks");
        int volume = w * h * d;
        if (ids.length < volume) throw new Bad("its block data stops early");
        java.util.HashMap<Integer, Integer> seen = new java.util.HashMap<>();
        List<String> palette = new ArrayList<>();
        Cells cells = new Cells();
        int unmapped = 0;
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < d; z++) {
                for (int x = 0; x < w; x++) {
                    int i = (y * d + z) * w + x;
                    int id = ids[i] & 0xFF;
                    if (i >> 1 < add.length) id |= ((i & 1) == 0 ? add[i >> 1] & 0x0F : (add[i >> 1] & 0xF0) >> 4) << 8;
                    if (id == 0) continue;
                    int meta = i < metas.length ? metas[i] & 0x0F : 0;
                    int key = id << 4 | meta;
                    Integer state = seen.get(key);
                    if (state == null) {
                        String name = legacy.apply(key);
                        if (name == null) name = legacy.apply(id << 4); // same block, unknown variant
                        if (name == null || isAir(name)) {
                            state = -1;
                        } else {
                            state = palette.size();
                            palette.add(name);
                        }
                        seen.put(key, state);
                    }
                    if (state < 0) unmapped++;
                    else cells.add(x, y, z, state);
                }
            }
        }
        return new Result(w, h, d, 1519, palette, cells.done(), unmapped); // what the numbers map to is the 1.13 list of names
    }
}
