package com.example.charpowers.house.model;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

// The test files are written right here, block by block: no one else's builds in the repository.
class SchematicReaderTest {
    private static final String STAIRS = "minecraft:oak_stairs[facing=east,half=bottom]";

    @TempDir
    Path dir;

    private Path save(String name, CompoundTag root) throws IOException {
        Path file = dir.resolve(name);
        NbtIo.writeCompressed(root, file.toFile());
        return file;
    }

    private static Map<String, String> byPos(SchematicReader.Result r) {
        Map<String, String> out = new HashMap<>();
        int[] c = r.cells();
        for (int i = 0; i < c.length; i += 4) out.put(c[i] + "," + c[i + 1] + "," + c[i + 2], r.palette().get(c[i + 3]));
        return out;
    }

    // 2 wide, 2 high, 3 deep: stone at the bottom corner, stairs at the far top corner, air everywhere else.
    private static void assertTheLittleShape(SchematicReader.Result r) {
        assertEquals(2, r.w());
        assertEquals(2, r.h());
        assertEquals(3, r.d());
        Map<String, String> blocks = byPos(r);
        assertEquals(2, blocks.size(), blocks.toString());
        assertEquals("minecraft:stone", blocks.get("0,0,0"));
        assertEquals(STAIRS, blocks.get("1,1,2"));
    }

    private static byte[] varints(int[] values) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int v : values) {
            while ((v & ~0x7F) != 0) {
                out.write((v & 0x7F) | 0x80);
                v >>>= 7;
            }
            out.write(v);
        }
        return out.toByteArray();
    }

    private static CompoundTag spongeBody(int w, int h, int d) {
        CompoundTag s = new CompoundTag();
        s.putShort("Width", (short) w);
        s.putShort("Height", (short) h);
        s.putShort("Length", (short) d);
        s.putInt("DataVersion", 3465);
        return s;
    }

    private static int[] shapeValues() {
        int[] values = new int[12]; // y * w * d + z * w + x, all air (0)
        values[0] = 1;
        values[1 * 2 * 3 + 2 * 2 + 1] = 2;
        return values;
    }

    private static CompoundTag shapePalette() {
        CompoundTag palette = new CompoundTag();
        palette.putInt("minecraft:air", 0);
        palette.putInt("minecraft:stone", 1);
        palette.putInt(STAIRS, 2);
        return palette;
    }

    @Test
    void spongeVersion2() throws IOException {
        CompoundTag s = spongeBody(2, 2, 3);
        s.putInt("Version", 2);
        s.put("Palette", shapePalette());
        s.putByteArray("BlockData", varints(shapeValues()));
        assertTheLittleShape(SchematicReader.read(save("a.schem", s), k -> null));
    }

    @Test
    void spongeVersion3WrappedInASchematicTag() throws IOException {
        CompoundTag s = spongeBody(2, 2, 3);
        s.putInt("Version", 3);
        CompoundTag blocks = new CompoundTag();
        blocks.put("Palette", shapePalette());
        blocks.putByteArray("Data", varints(shapeValues()));
        s.put("Blocks", blocks);
        CompoundTag root = new CompoundTag();
        root.put("Schematic", s);
        assertTheLittleShape(SchematicReader.read(save("a.schem", root), k -> null));
    }

    @Test
    void spongeWithVarintsOverOneByte() throws IOException {
        CompoundTag s = spongeBody(200, 1, 1);
        s.putInt("Version", 2);
        CompoundTag palette = new CompoundTag();
        int[] values = new int[200];
        for (int i = 0; i < 200; i++) {
            palette.putInt("mod:block_" + i, i);
            values[i] = i;
        }
        s.put("Palette", palette);
        s.putByteArray("BlockData", varints(values));
        SchematicReader.Result r = SchematicReader.read(save("big.schem", s), k -> null);
        assertEquals(200, r.blocks());
        assertEquals("mod:block_150", byPos(r).get("150,0,0"));
    }

    private static CompoundTag paletteEntry(String name, String... props) {
        CompoundTag e = new CompoundTag();
        e.putString("Name", name);
        if (props.length > 0) {
            CompoundTag p = new CompoundTag();
            for (int i = 0; i < props.length; i += 2) p.putString(props[i], props[i + 1]);
            e.put("Properties", p);
        }
        return e;
    }

    private static long[] packLitematic(int[] values, int bits) {
        long[] out = new long[(int) Math.ceil(values.length * bits / 64.0)];
        for (int i = 0; i < values.length; i++) {
            long bit = (long) i * bits;
            int word = (int) (bit >>> 6), shift = (int) (bit & 63);
            out[word] |= (long) values[i] << shift;
            if (shift + bits > 64) out[word + 1] |= (long) values[i] >>> (64 - shift);
        }
        return out;
    }

    private static CompoundTag xyz(int x, int y, int z) {
        CompoundTag t = new CompoundTag();
        t.putInt("x", x);
        t.putInt("y", y);
        t.putInt("z", z);
        return t;
    }

    private static CompoundTag region(CompoundTag pos, CompoundTag size, ListTag palette, long[] states) {
        CompoundTag r = new CompoundTag();
        r.put("Position", pos);
        r.put("Size", size);
        r.put("BlockStatePalette", palette);
        r.put("BlockStates", new LongArrayTag(states));
        return r;
    }

    private static ListTag shapeList() {
        ListTag palette = new ListTag();
        palette.add(paletteEntry("minecraft:air"));
        palette.add(paletteEntry("minecraft:stone"));
        palette.add(paletteEntry("minecraft:oak_stairs", "half", "bottom", "facing", "east"));
        return palette;
    }

    @Test
    void litematicOneRegion() throws IOException {
        CompoundTag regions = new CompoundTag();
        regions.put("main", region(xyz(0, 0, 0), xyz(2, 2, 3), shapeList(), packLitematic(shapeValues(), 2)));
        CompoundTag root = new CompoundTag();
        root.put("Regions", regions);
        root.putInt("MinecraftDataVersion", 3465);
        assertTheLittleShape(SchematicReader.read(save("a.litematic", root), k -> null));
    }

    @Test
    void litematicNegativeSizeAndTwoRegionsShareOneBox() throws IOException {
        CompoundTag regions = new CompoundTag();
        // a negative size means the region runs the other way from its position: this covers x 3..4
        int[] one = new int[2];
        one[1] = 1;
        regions.put("left", region(xyz(4, 0, 0), xyz(-2, 1, 1), shapeList(), packLitematic(one, 2)));
        int[] two = new int[1];
        two[0] = 2;
        regions.put("right", region(xyz(10, 5, 7), xyz(1, 1, 1), shapeList(), packLitematic(two, 2)));
        CompoundTag root = new CompoundTag();
        root.put("Regions", regions);
        SchematicReader.Result r = SchematicReader.read(save("b.litematic", root), k -> null);
        assertEquals(8, r.w());
        assertEquals(6, r.h());
        assertEquals(8, r.d());
        Map<String, String> blocks = byPos(r);
        assertEquals("minecraft:stone", blocks.get("1,0,0"));
        assertEquals(STAIRS, blocks.get("7,5,7"));
    }

    @Test
    void litematicEntriesCrossingLongBoundaries() throws IOException {
        // 9 kinds of block = 4 bits; 5 bits with 17 kinds, so entries straddle the 64 bit words
        ListTag palette = new ListTag();
        for (int i = 0; i < 17; i++) palette.add(paletteEntry(i == 0 ? "minecraft:air" : "mod:b" + i));
        int[] values = new int[40];
        for (int i = 0; i < 40; i++) values[i] = i % 17;
        CompoundTag regions = new CompoundTag();
        regions.put("r", region(xyz(0, 0, 0), xyz(40, 1, 1), palette, packLitematic(values, 5)));
        CompoundTag root = new CompoundTag();
        root.put("Regions", regions);
        Map<String, String> blocks = byPos(SchematicReader.read(save("c.litematic", root), k -> null));
        for (int i = 0; i < 40; i++) {
            if (i % 17 != 0) assertEquals("mod:b" + i % 17, blocks.get(i + ",0,0"), "at " + i);
        }
    }

    @Test
    void oldSchematicMapsNumbersAndCountsWhatItCannot() throws IOException {
        CompoundTag s = spongeBody(2, 1, 2);
        s.putString("Materials", "Alpha");
        // index = (y * d + z) * w + x
        byte[] ids = {1, 5, 0, (byte) 200};
        byte[] data = {0, 2, 0, 0};
        s.put("Blocks", new ByteArrayTag(ids));
        s.put("Data", new ByteArrayTag(data));
        Map<Integer, String> names = Map.of(1 << 4, "minecraft:stone", 5 << 4, "minecraft:oak_planks", 5 << 4 | 2, "minecraft:birch_planks");
        SchematicReader.Result r = SchematicReader.read(save("old.schematic", s), names::get);
        Map<String, String> blocks = byPos(r);
        assertEquals("minecraft:stone", blocks.get("0,0,0"));
        assertEquals("minecraft:birch_planks", blocks.get("1,0,0"));
        assertEquals(2, blocks.size());
        assertEquals(1, r.unmapped());
    }

    @Test
    void oldSchematicAddBlocksReachHighIds() throws IOException {
        CompoundTag s = spongeBody(2, 1, 1);
        s.putString("Materials", "Alpha");
        s.put("Blocks", new ByteArrayTag(new byte[] {1, 1}));
        s.put("Data", new ByteArrayTag(new byte[] {0, 0}));
        s.put("AddBlocks", new ByteArrayTag(new byte[] {0x21})); // first block + 0x100, second + 0x200
        SchematicReader.Result r = SchematicReader.read(save("add.schematic", s), k -> "mod:id" + (k >> 4));
        Map<String, String> blocks = byPos(r);
        assertEquals("mod:id257", blocks.get("0,0,0"));
        assertEquals("mod:id513", blocks.get("1,0,0"));
    }

    @Test
    void vanillaStructure() throws IOException {
        CompoundTag root = new CompoundTag();
        ListTag size = new ListTag();
        size.add(IntTag.valueOf(2));
        size.add(IntTag.valueOf(2));
        size.add(IntTag.valueOf(3));
        root.put("size", size);
        root.put("palette", shapeList());
        ListTag blocks = new ListTag();
        for (int[] b : new int[][] {{0, 0, 0, 1}, {1, 1, 2, 2}, {0, 1, 0, 0}}) {
            CompoundTag e = new CompoundTag();
            ListTag pos = new ListTag();
            pos.add(IntTag.valueOf(b[0]));
            pos.add(IntTag.valueOf(b[1]));
            pos.add(IntTag.valueOf(b[2]));
            e.put("pos", pos);
            e.putInt("state", b[3]);
            blocks.add(e);
        }
        root.put("blocks", blocks);
        assertTheLittleShape(SchematicReader.read(save("a.nbt", root), k -> null));
    }

    // ---- files that want to hurt us

    private static void assertRefused(Path file, String word) {
        SchematicReader.Bad bad = assertThrows(SchematicReader.Bad.class, () -> SchematicReader.read(file, k -> null));
        assertTrue(bad.getMessage().contains(word), bad.getMessage());
    }

    @Test
    void tooBigIsRefusedBeforeAnythingIsAllocated() throws IOException {
        CompoundTag s = spongeBody(257, 1, 1);
        s.put("Palette", shapePalette());
        assertRefused(save("wide.schem", s), "limit");
        s = spongeBody(10, 129, 10);
        s.put("Palette", shapePalette());
        assertRefused(save("tall.schem", s), "limit");
        CompoundTag regions = new CompoundTag();
        regions.put("a", region(xyz(0, 0, 0), xyz(1, 1, 1), shapeList(), new long[1]));
        regions.put("b", region(xyz(5000, 0, 0), xyz(1, 1, 1), shapeList(), new long[1]));
        CompoundTag root = new CompoundTag();
        root.put("Regions", regions);
        assertRefused(save("far.litematic", root), "limit");
    }

    @Test
    void tooManyBlocksIsRefused() throws IOException {
        CompoundTag s = spongeBody(256, 4, 256); // 262,144 blocks of stone
        s.putInt("Version", 2);
        CompoundTag palette = new CompoundTag();
        palette.putInt("minecraft:stone", 0);
        s.put("Palette", palette);
        s.putByteArray("BlockData", new byte[256 * 4 * 256]);
        assertRefused(save("lots.schem", s), "more than");
    }

    @Test
    void shortDataIsRefused() throws IOException {
        CompoundTag s = spongeBody(2, 2, 3);
        s.putInt("Version", 2);
        s.put("Palette", shapePalette());
        s.putByteArray("BlockData", new byte[] {1, 0});
        assertRefused(save("short.schem", s), "early");
        CompoundTag regions = new CompoundTag();
        regions.put("r", region(xyz(0, 0, 0), xyz(2, 2, 3), shapeList(), new long[0]));
        CompoundTag root = new CompoundTag();
        root.put("Regions", regions);
        assertRefused(save("short.litematic", root), "early");
    }

    @Test
    void valuesOutsideThePaletteAreRefused() throws IOException {
        CompoundTag s = spongeBody(1, 1, 1);
        s.putInt("Version", 2);
        s.put("Palette", shapePalette());
        s.putByteArray("BlockData", varints(new int[] {99}));
        assertRefused(save("bad.schem", s), "broken");
    }

    @Test
    void garbageIsRefusedNotThrownAround() throws IOException {
        Path file = dir.resolve("junk.schem");
        Files.write(file, new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        assertThrows(IOException.class, () -> SchematicReader.read(file, k -> null));
        Path gz = dir.resolve("junk2.litematic");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(new byte[] {10, 0, 0, 7, 0, 1, 65, 127, 127, 127, 127});
        }
        Files.write(gz, bytes.toByteArray());
        assertThrows(IOException.class, () -> SchematicReader.read(gz, k -> null));
    }

    @Test
    void aZipBombStopsAtTheCap() throws IOException {
        Path file = dir.resolve("bomb.schem");
        try (GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {
            byte[] zeros = new byte[1024 * 1024];
            for (int i = 0; i < 300; i++) out.write(zeros);
        }
        assertThrows(IOException.class, () -> SchematicReader.read(file, k -> null));
    }

    @Test
    void onlyKnownFileTypes() {
        assertTrue(SchematicReader.canRead("My House.SCHEM"));
        assertTrue(SchematicReader.canRead("a.litematic"));
        assertTrue(SchematicReader.canRead("a.schematic"));
        assertTrue(SchematicReader.canRead("a.nbt"));
        assertFalse(SchematicReader.canRead("a.txt"));
    }

    // ---- the little picture

    @Test
    void thumbnailShowsTheBlocksAndSurvivesThePost() {
        int[] cells = new int[4 * 27];
        int n = 0;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    cells[n++] = x;
                    cells[n++] = y;
                    cells[n++] = z;
                    cells[n++] = 0xFF00FF;
                }
            }
        }
        int[] pixels = IsoThumb.render(3, 3, 3, cells);
        assertEquals(IsoThumb.W * IsoThumb.H, pixels.length);
        long painted = java.util.Arrays.stream(pixels).filter(p -> p != 0).count();
        assertTrue(painted > pixels.length / 8, "only " + painted + " pixels painted");
        byte[] packed = IsoThumb.pack(pixels);
        assertTrue(packed.length < pixels.length, "packing should shrink it");
        assertArrayEquals(pixels, IsoThumb.unpack(packed));
        assertNull(IsoThumb.unpack(new byte[] {1, 2, 3}));
        assertArrayEquals(new int[IsoThumb.W * IsoThumb.H], IsoThumb.render(3, 3, 3, new int[0]));
    }

    @Test
    void sidecarSaysLevelAndName() {
        SidecarText s = SidecarText.parse(List.of("# my house", "level = 25", "name=Big Barn", "rubbish"));
        assertEquals(25, s.level());
        assertEquals("Big Barn", s.name());
        assertEquals(1, SidecarText.parse(List.of()).level());
        assertNull(SidecarText.parse(List.of()).name());
        assertEquals(100, SidecarText.parse(List.of("level=5000")).level());
        assertEquals(1, SidecarText.parse(List.of("level=-2")).level());
        assertEquals(1, SidecarText.parse(List.of("level=lots")).level());
    }
}
