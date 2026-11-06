package com.example.charpowers.smoke;

import com.example.charpowers.CharPowers;
import com.example.charpowers.house.CastleScan;
import com.example.charpowers.house.HouseBuilder;
import com.example.charpowers.house.HouseCatalog;
import com.example.charpowers.house.HouseData;
import com.example.charpowers.house.HousePlan;
import com.example.charpowers.house.model.HouseTurn;
import com.example.charpowers.mastery.Mastery;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// House Builder on a real headless server. Every test works on its own stone platform far away from the others.
@GameTestHolder(CharPowers.MODID)
@PrefixGameTestTemplate(false)
public class HouseSmokeTests {
    private static final int Y = 150;
    private static final Rotation[] TURNS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};

    private static ServerPlayer loggedIn(GameTestHelper h, String name, BlockPos at) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name));
        level.getServer().getPlayerList().placeNewPlayer(new dev.dubhe.curtain.features.player.patches.FakeClientConnection(PacketFlow.SERVERBOUND), p);
        p.teleportTo(level, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 0f, 0f);
        p.setNoGravity(true);
        return p;
    }

    // A flat stone platform, kept loaded, around (x, z).
    private static BlockPos platform(ServerLevel level, int x, int z, int radius) {
        for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
            for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) level.setChunkForced(cx, cz, true);
        }
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(x - radius, Y, z - radius, x + radius, Y, z + radius)) level.setBlock(pos, stone, 2);
        return new BlockPos(x, Y, z);
    }

    private static void release(ServerLevel level, int x, int z, int radius) {
        for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
            for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) level.setChunkForced(cx, cz, false);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    private static void noOtherJobs(GameTestHelper h) {
        HouseBuilder.POWER.finishJobs(h.getLevel().getServer());
    }

    // Everything the file says is really there, block for block, turned the right way, for all four turns.
    @GameTest(template = "empty", timeoutTicks = 400, batch = "house_a")
    public static void aHouseGoesUpCorrectlyFacingEveryWay(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        noOtherJobs(h);
        int z = 20000;
        BlockPos mid = platform(level, 20000, z, 110);
        ServerPlayer p = loggedIn(h, "builder1", mid);
        HouseBuilder.POWER.give(p);
        check(HouseBuilder.POWER.owns(p), "no power");
        HousePlan plan = HouseCatalog.get(level.getServer(), "cosy_cottage");
        check(plan != null, "no cottage");
        int[][] middles = {{20000 - 50, z - 50}, {20000 + 50, z - 50}, {20000 - 50, z + 50}, {20000 + 50, z + 50}};
        for (int turns = 0; turns < 4; turns++) {
            p.teleportTo(level, middles[turns][0], Y + 30, middles[turns][1], 0f, 0f);
            HouseBuilder.POWER.resetCooldown(p);
            String refused = HouseBuilder.POWER.tryBuild(p, "cosy_cottage", middles[turns][0], Y, middles[turns][1], turns);
            check(refused == null, "refused: " + refused);
            HouseBuilder.POWER.finishJobs(level.getServer());
            int wrong = 0;
            String first = null;
            for (HousePlan.Cell c : plan.cells) {
                BlockPos at = new BlockPos(HouseTurn.worldX(c.x(), c.z(), plan.w, plan.d, middles[turns][0], turns), Y + c.y(),
                        HouseTurn.worldZ(c.x(), c.z(), plan.w, plan.d, middles[turns][1], turns));
                BlockState want = c.state().rotate(TURNS[turns]);
                if (level.getBlockState(at) != want) {
                    wrong++;
                    if (first == null) first = at + " has " + level.getBlockState(at) + " not " + want;
                }
            }
            check(wrong == 0, wrong + " blocks wrong at turn " + turns + ": " + first);
        }
        p.discard();
        release(level, 20000, z, 110);
        h.succeed();
    }

    // The cooldown is one for the whole power, it blocks a second house, and it can be reset.
    @GameTest(template = "empty", timeoutTicks = 200, batch = "house_b")
    public static void theCooldownIsSharedAndResets(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        noOtherJobs(h);
        BlockPos mid = platform(level, 21000, 20000, 60);
        ServerPlayer p = loggedIn(h, "builder2", mid);
        HouseBuilder.POWER.give(p);
        check(HouseBuilder.POWER.cooldownLeft(p) == 0, "starts on cooldown");
        check(HouseBuilder.POWER.tryBuild(p, "cosy_cottage", 21000 - 25, Y, 20000, 0) == null, "first house refused");
        check(HouseBuilder.POWER.cooldownLeft(p) > 11000, "cooldown didn't start");
        String second = HouseBuilder.POWER.tryBuild(p, "cosy_cottage", 21000 + 25, Y, 20000, 0);
        check(second != null && second.contains("next house"), "second house wasn't stopped: " + second);
        HouseBuilder.POWER.finishJobs(level.getServer());
        HouseBuilder.POWER.trigger(p);
        check(HouseBuilder.POWER.cooldownLeft(p) == 0, "reset didn't work");
        p.discard();
        release(level, 21000, 20000, 60);
        h.succeed();
    }

    // It waits for a real tick-by-tick build too: layer by layer, nothing placed yet on the first tick.
    @GameTest(template = "empty", timeoutTicks = 800, batch = "house_c")
    public static void aHouseBuildsOverTime(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        noOtherJobs(h);
        BlockPos mid = platform(level, 22000, 20000, 40);
        ServerPlayer p = loggedIn(h, "builder3", mid);
        HouseBuilder.POWER.give(p);
        HousePlan plan = HouseCatalog.get(level.getServer(), "cosy_cottage");
        check(HouseBuilder.POWER.tryBuild(p, "cosy_cottage", 22000, Y, 20000, 0) == null, "refused");
        HousePlan.Cell top = plan.cells.get(plan.cells.size() - 1);
        BlockPos topAt = new BlockPos(HouseTurn.worldX(top.x(), top.z(), plan.w, plan.d, 22000, 0), Y + top.y(), HouseTurn.worldZ(top.x(), top.z(), plan.w, plan.d, 20000, 0));
        h.runAfterDelay(40, () -> check(level.getBlockState(topAt).isAir(), "the top went up before the bottom"));
        h.runAfterDelay(30 * 20 + 40, () -> {
            check(!level.getBlockState(topAt).isAir(), "not finished after 30 s");
            p.discard();
            release(level, 22000, 20000, 40);
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "house_d")
    public static void itRefusesOverAChestOrABuiltWall(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        noOtherJobs(h);
        BlockPos mid = platform(level, 23000, 20000, 40);
        ServerPlayer p = loggedIn(h, "builder4", mid);
        HouseBuilder.POWER.give(p);
        BlockPos chest = mid.offset(2, 1, 2);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        String refused = HouseBuilder.POWER.tryBuild(p, "cosy_cottage", 23000, Y, 20000, 0);
        check(refused != null && refused.contains("holds things"), "chest not refused: " + refused);
        check(HouseBuilder.POWER.cooldownLeft(p) == 0, "a refusal started the cooldown");
        check(level.getBlockState(chest).is(Blocks.CHEST), "chest touched");
        level.setBlock(chest, Blocks.BRICKS.defaultBlockState(), 2);
        refused = HouseBuilder.POWER.tryBuild(p, "cosy_cottage", 23000, Y, 20000, 0);
        check(refused != null && refused.contains("in the way"), "wall not refused: " + refused);
        level.setBlock(chest, Blocks.AIR.defaultBlockState(), 2);
        // a flower and a wild bump are fine, and get cleared
        level.setBlock(chest, Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(chest.above(), Blocks.POPPY.defaultBlockState(), 2);
        refused = HouseBuilder.POWER.tryBuild(p, "cosy_cottage", 23000, Y, 20000, 0);
        check(refused == null, "plants and a bump were refused: " + refused);
        HouseBuilder.POWER.finishJobs(level.getServer());
        HouseBuilder.POWER.resetCooldown(p);
        // a locked house says what it needs
        String locked = HouseBuilder.POWER.tryBuild(p, "tudor_manor", 23000, Y, 20000, 0);
        check(locked != null && locked.contains("Mastery 100"), "locked house: " + locked);
        String awakened = HouseBuilder.POWER.tryBuild(p, "pagoda_house", 23000, Y, 20000, 0);
        check(awakened != null && awakened.contains("Awakening"), "awakened house: " + awakened);
        p.discard();
        release(level, 23000, 20000, 40);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "house_e")
    public static void tenThousandBlocksUnlocksAndBlocksGiveMastery(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = platform(level, 24000, 20000, 4);
        ServerPlayer p = loggedIn(h, "builder5", mid);
        for (int i = 0; i < 9999; i++) HouseBuilder.POWER.countForTest(p, level, mid);
        check(!HouseBuilder.POWER.owns(p), "unlocked too early");
        HouseBuilder.POWER.countForTest(p, level, mid);
        check(HouseBuilder.POWER.owns(p), "not unlocked at 10,000");
        check(HouseBuilder.POWER.mastery(p) == 1, "the 10,000 gave mastery");
        for (int i = 0; i < 6100; i++) HouseBuilder.POWER.countForTest(p, level, mid);
        int at6100 = HouseBuilder.POWER.mastery(p);
        check(at6100 >= 25, "6,100 blocks is only level " + at6100);
        for (int i = 6100; i < 100000; i++) HouseBuilder.POWER.countForTest(p, level, mid);
        check(HouseBuilder.POWER.mastery(p) == 100, "100,000 blocks is only level " + HouseBuilder.POWER.mastery(p));
        p.discard();
        release(level, 24000, 20000, 4);
        h.succeed();
    }

    // A real structure file check: the game knows every block in every house, in all ten.
    @GameTest(template = "empty", timeoutTicks = 100, batch = "house_f")
    public static void everyHouseFileLoadsAndEveryBlockIsReal(GameTestHelper h) throws Exception {
        var server = h.getLevel().getServer();
        List<String> bad = new ArrayList<>();
        int count = 0;
        for (HousePlan plan : HouseCatalog.all(server)) {
            if (plan.custom) continue;
            count++;
            ResourceLocation at = new ResourceLocation(CharPowers.MODID, "structures/houses/" + plan.id + ".nbt");
            try (InputStream in = server.getResourceManager().getResourceOrThrow(at).open()) {
                CompoundTag root = NbtIo.readCompressed(in);
                ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
                for (int i = 0; i < palette.size(); i++) {
                    String name = palette.getCompound(i).getString("Name");
                    if (!BuiltInRegistries.BLOCK.containsKey(new ResourceLocation(name))) bad.add(plan.id + ": " + name);
                    // every property written has to be a real one
                    var block = BuiltInRegistries.BLOCK.get(new ResourceLocation(name));
                    CompoundTag props = palette.getCompound(i).getCompound("Properties");
                    for (String key : props.getAllKeys()) {
                        var prop = block.getStateDefinition().getProperty(key);
                        if (prop == null || prop.getValue(props.getString(key)).isEmpty()) bad.add(plan.id + ": " + name + " " + key + "=" + props.getString(key));
                    }
                }
                int entries = root.getList("blocks", Tag.TAG_COMPOUND).size();
                if (entries != plan.cells.size()) bad.add(plan.id + ": " + entries + " in the file, " + plan.cells.size() + " loaded");
            }
        }
        check(count == 10, "only " + count + " houses");
        check(bad.isEmpty(), bad.toString());
        h.succeed();
    }

    // Walls, a courtyard floor, towers at two corners, an arch in the south wall.
    private static List<BlockPos> castle(ServerLevel level, BlockPos base, int side, boolean towers, boolean door) {
        List<BlockPos> out = new ArrayList<>();
        int mid = side / 2;
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                boolean edge = x < 2 || z < 2 || x >= side - 2 || z >= side - 2;
                int high = edge ? 8 : 1;
                boolean tower = towers && (x < 4 || x >= side - 4) && z < 4;
                if (tower) high = 14;
                for (int y = 1; y <= high; y++) {
                    if (door && Math.abs(x - mid) == 0 && z >= side - 2 && y >= 2 && y <= 4) continue;
                    BlockPos at = base.offset(x, y, z);
                    level.setBlock(at, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    out.add(at);
                }
            }
        }
        return out;
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "house_g")
    public static void theCastleQuestFailsThenPasses(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = platform(level, 25000, 20000, 60);
        ServerPlayer p = loggedIn(h, "builder6", mid);
        HouseBuilder.POWER.give(p);
        Mastery.setLevel(p, HouseBuilder.POWER.id, 100);
        HouseData.Placed placed = HouseData.Placed.of(level);
        // no towers, no door, and none of it placed by them
        BlockPos base = mid.offset(-20, 0, -20);
        List<BlockPos> plain = castle(level, base, 36, false, false);
        CastleScan scan = new CastleScan(level, p.getUUID(), plain.get(0));
        while (!scan.tick()) { }
        List<String> missing = scan.verdict();
        CharPowers.LOG.info("[Smoke] plain walls: {} {}", scan.total(), missing);
        check(missing.stream().anyMatch(s -> s.contains("0 of 3200")), "should say none are yours: " + missing);
        check(missing.stream().anyMatch(s -> s.contains("2 more towers")), "should ask for towers: " + missing);
        check(missing.stream().anyMatch(s -> s.contains("doorway")), "should ask for a door: " + missing);
        for (BlockPos at : plain) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
        // the same with towers and a door: too few of their own
        List<BlockPos> real = castle(level, base, 36, true, true);
        check(real.size() > 3200, "the test castle is too small: " + real.size());
        for (int i = 0; i < 1000; i++) placed.add(p.getUUID(), real.get(i));
        CastleScan few = new CastleScan(level, p.getUUID(), real.get(0));
        while (!few.tick()) { }
        List<String> fewMissing = few.verdict();
        CharPowers.LOG.info("[Smoke] 1000 of theirs: {}", fewMissing);
        check(fewMissing.size() == 1 && fewMissing.get(0).contains("1000 of 3200"), "should only say the count: " + fewMissing);
        HouseBuilder.POWER.startScan(p, real.get(0));
        h.runAfterDelay(60, () -> {
            check(!HouseBuilder.POWER.awakened(p), "awakened with too few blocks of their own");
            for (BlockPos at : real) placed.add(p.getUUID(), at);
            HouseBuilder.POWER.startScan(p, real.get(0));
            h.runAfterDelay(60, () -> {
                check(HouseBuilder.POWER.awakened(p), "a real castle didn't awaken it");
                check(!placed.has(p.getUUID(), real.get(0)), "memory should be let go once awakened");
                String refused = HouseBuilder.POWER.tryBuild(p, "pagoda_house", 25000 + 500, Y, 20000, 0);
                check(refused == null || !refused.contains("Awakening"), "awakened house still locked");
                p.discard();
                release(level, 25000, 20000, 60);
                h.succeed();
            });
        });
    }

    // Blocks placed only count towards the castle from mastery 90 on.
    @GameTest(template = "empty", timeoutTicks = 100, batch = "house_h")
    public static void placedBlocksAreOnlyRememberedNearTheEnd(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = platform(level, 26000, 20000, 4);
        ServerPlayer p = loggedIn(h, "builder7", mid);
        HouseBuilder.POWER.give(p);
        Mastery.setLevel(p, HouseBuilder.POWER.id, 50);
        BlockPos spot = mid.above();
        HouseBuilder.POWER.countForTest(p, level, spot);
        check(!HouseBuilder.isRemembered(level, p.getUUID(), spot), "remembered too early");
        Mastery.setLevel(p, HouseBuilder.POWER.id, 90);
        HouseBuilder.POWER.countForTest(p, level, spot);
        check(HouseBuilder.isRemembered(level, p.getUUID(), spot), "not remembered at 90");
        check(!HouseBuilder.isRemembered(level, UUID.randomUUID(), spot), "remembered for someone else");
        p.discard();
        release(level, 26000, 20000, 4);
        h.succeed();
    }

    // Every one of the ten, each turned a different way, block for block. One house every second so no single tick gets silly.
    @GameTest(template = "empty", timeoutTicks = 1200, batch = "house_i")
    public static void allTenHousesBuildCorrectlyTurned(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        noOtherJobs(h);
        BlockPos mid = platform(level, 40000, 20000, 4);
        ServerPlayer p = loggedIn(h, "builder8", mid);
        p.getAbilities().flying = true;
        HouseBuilder.POWER.give(p);
        Mastery.setLevel(p, HouseBuilder.POWER.id, 100);
        Mastery.setAwakened(p, HouseBuilder.POWER.id);
        var list = com.example.charpowers.house.model.HouseList.BUILT_IN;
        for (int i = 0; i < list.size(); i++) {
            int index = i;
            h.runAfterDelay(1 + i * 20L, () -> {
                String id = list.get(index).id();
                int x = 40000 + 150 * (index + 1), z = 20000, turns = index & 3;
                platform(level, x, z, 40);
                p.teleportTo(level, x + 0.5, Y + 60, z + 0.5, 0f, 0f);
                HouseBuilder.POWER.resetCooldown(p);
                String refused = HouseBuilder.POWER.tryBuild(p, id, x, Y, z, turns);
                check(refused == null, id + " refused: " + refused);
                HouseBuilder.POWER.finishJobs(level.getServer());
                HousePlan plan = HouseCatalog.get(level.getServer(), id);
                int wrong = 0;
                String first = null;
                for (HousePlan.Cell c : plan.cells) {
                    BlockPos at = new BlockPos(HouseTurn.worldX(c.x(), c.z(), plan.w, plan.d, x, turns), Y + c.y(), HouseTurn.worldZ(c.x(), c.z(), plan.w, plan.d, z, turns));
                    if (level.getBlockState(at) != c.state().rotate(TURNS[turns])) {
                        wrong++;
                        if (first == null) first = at + " has " + level.getBlockState(at);
                    }
                }
                check(wrong == 0, id + ": " + wrong + " blocks wrong at turn " + turns + ", " + first);
                release(level, x, z, 40);
                if (index == list.size() - 1) {
                    p.discard();
                    release(level, 40000, 20000, 4);
                    h.succeed();
                }
            });
        }
    }

    // ---- files dropped in config/charpowers/houses. These test files are written right here, block by block.

    private static byte[] varints(int[] values) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int v : values) {
            while ((v & ~0x7F) != 0) {
                out.write((v & 0x7F) | 0x80);
                v >>>= 7;
            }
            out.write(v);
        }
        return out.toByteArray();
    }

    // A small hut as a Sponge schematic, with a block from a mod that isn't here and a command block it must leave out.
    public static void writeHut(Path file) throws IOException {
        int w = 9, h = 7, d = 9;
        String[] palette = {"minecraft:air", "minecraft:stone_bricks", "minecraft:oak_planks", "minecraft:oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
                "minecraft:oak_door[facing=south,half=lower,hinge=left,open=false,powered=false]", "minecraft:oak_door[facing=south,half=upper,hinge=left,open=false,powered=false]",
                "minecraft:chest[facing=north,type=single,waterlogged=false]", "minecraft:command_block[conditional=false,facing=up]", "somemod:thing", "minecraft:glass"};
        int[] values = new int[w * h * d];
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < d; z++) {
                for (int x = 0; x < w; x++) {
                    int v = 0;
                    boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                    if (y == 0) v = 1;
                    else if (y < 4 && edge) v = y == 2 && x == 2 ? 9 : 2;
                    else if (y == 4) v = 2;
                    else if (y == 5 && x >= 2 && x <= 6 && z >= 2 && z <= 6) v = 3;
                    values[x + z * w + y * w * d] = v;
                }
            }
        }
        values[4 + 8 * w + w * d] = 4;
        values[4 + 8 * w + 2 * w * d] = 5;
        values[3 + 3 * w + w * d] = 6;
        values[5 + 3 * w + w * d] = 7;
        values[5 + 5 * w + w * d] = 8;
        CompoundTag s = new CompoundTag();
        s.putInt("Version", 2);
        s.putInt("DataVersion", 3465);
        s.putShort("Width", (short) w);
        s.putShort("Height", (short) h);
        s.putShort("Length", (short) d);
        CompoundTag p = new CompoundTag();
        for (int i = 0; i < palette.length; i++) p.putInt(palette[i], i);
        s.put("Palette", p);
        s.putByteArray("BlockData", varints(values));
        NbtIo.writeCompressed(s, file.toFile());
    }

    private static void writeLitematic(Path file) throws IOException {
        // 2 x 1 x 1: planks at x 0, air at x 1, two bits each
        ListTag palette = new ListTag();
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        CompoundTag planks = new CompoundTag();
        planks.putString("Name", "minecraft:birch_planks");
        palette.add(air);
        palette.add(planks);
        CompoundTag size = new CompoundTag(), pos = new CompoundTag();
        size.putInt("x", 2);
        size.putInt("y", 1);
        size.putInt("z", 1);
        pos.putInt("x", 0);
        pos.putInt("y", 0);
        pos.putInt("z", 0);
        CompoundTag region = new CompoundTag();
        region.put("Size", size);
        region.put("Position", pos);
        region.put("BlockStatePalette", palette);
        region.putLongArray("BlockStates", new long[] {1L});
        CompoundTag regions = new CompoundTag();
        regions.put("main", region);
        CompoundTag root = new CompoundTag();
        root.put("Regions", regions);
        root.putInt("MinecraftDataVersion", 3465);
        NbtIo.writeCompressed(root, file.toFile());
    }

    private static void writeOldSchematic(Path file) throws IOException {
        CompoundTag s = new CompoundTag();
        s.putShort("Width", (short) 2);
        s.putShort("Height", (short) 1);
        s.putShort("Length", (short) 1);
        s.putString("Materials", "Alpha");
        s.putByteArray("Blocks", new byte[] {5, 53});
        s.putByteArray("Data", new byte[] {2, 0});
        NbtIo.writeCompressed(s, file.toFile());
    }

    private static void writeTooBig(Path file) throws IOException {
        CompoundTag s = new CompoundTag();
        s.putShort("Width", (short) 1000);
        s.putShort("Height", (short) 10);
        s.putShort("Length", (short) 10);
        NbtIo.writeCompressed(s, file.toFile());
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "house_j")
    public static void droppedInFilesBecomeHouses(GameTestHelper h) throws IOException {
        ServerLevel level = h.getLevel();
        noOtherJobs(h);
        Path folder = FMLPaths.CONFIGDIR.get().resolve("charpowers/houses");
        Files.createDirectories(folder);
        Path hut = folder.resolve("smoke_hut.schem"), side = folder.resolve("smoke_hut.txt"), lit = folder.resolve("smoke_lit.litematic"),
                old = folder.resolve("smoke_old.schematic"), big = folder.resolve("smoke_big.schem");
        writeHut(hut);
        Files.writeString(side, "level=25\nname=Test Hut\n");
        writeLitematic(lit);
        writeOldSchematic(old);
        writeTooBig(big);
        String[] said = {null};
        // a scan from another test or from the server starting may be under way: keep asking until this one is the one that ran
        h.runAtTickTime(h.getTick() + 5, () -> HouseCatalog.scan(level.getServer(), true, text -> said[0] = text));
        h.runAfterDelay(60, () -> {
            if (said[0] == null || said[0].startsWith("Still")) HouseCatalog.scan(level.getServer(), true, text -> said[0] = text);
        });
        h.runAfterDelay(160, () -> {
            CharPowers.LOG.info("[Smoke] reload said: {}", said[0]);
            HousePlan plan = HouseCatalog.get(level.getServer(), "custom_smoke_hut_schem");
            check(plan != null, "the hut wasn't read: " + said[0]);
            check(plan.name.equals("Test Hut") && plan.level == 25, "sidecar ignored: " + plan.name + " " + plan.level);
            check(plan.thumb != null && plan.thumb.length > 50, "no picture");
            check(com.example.charpowers.house.model.IsoThumb.unpack(plan.thumb) != null, "picture doesn't unpack");
            check(plan.cells.stream().noneMatch(c -> c.state().is(Blocks.COMMAND_BLOCK)), "a command block got through");
            check(plan.cells.stream().anyMatch(c -> c.state().is(Blocks.CHEST)), "the chest went missing");
            check(HouseCatalog.problems().stream().anyMatch(t -> t.contains("smoke_big")), "the too-big file wasn't reported: " + HouseCatalog.problems());
            HousePlan one = HouseCatalog.get(level.getServer(), "custom_smoke_lit_litematic");
            check(one != null && one.cells.size() == 1 && one.cells.get(0).state().is(Blocks.BIRCH_PLANKS), "litematic wrong");
            HousePlan two = HouseCatalog.get(level.getServer(), "custom_smoke_old_schematic");
            check(two != null && two.cells.size() == 2 && two.cells.get(0).state().is(Blocks.BIRCH_PLANKS) && two.cells.get(1).state().is(Blocks.OAK_STAIRS), "old schematic wrong: " + (two == null ? "missing" : two.cells));
            BlockPos mid = platform(level, 27000, 20000, 30);
            ServerPlayer p = loggedIn(h, "builder9", mid);
            HouseBuilder.POWER.give(p);
            String locked = HouseBuilder.POWER.tryBuild(p, plan.id, 27000, Y, 20000, 1);
            check(locked != null && locked.contains("Mastery 25"), "should be locked: " + locked);
            Mastery.setLevel(p, HouseBuilder.POWER.id, 25);
            String refused = HouseBuilder.POWER.tryBuild(p, plan.id, 27000, Y, 20000, 1);
            check(refused == null, "refused: " + refused);
            HouseBuilder.POWER.finishJobs(level.getServer());
            for (HousePlan.Cell c : plan.cells) {
                BlockPos at = new BlockPos(HouseTurn.worldX(c.x(), c.z(), plan.w, plan.d, 27000, 1), Y + c.y(), HouseTurn.worldZ(c.x(), c.z(), plan.w, plan.d, 20000, 1));
                check(level.getBlockState(at) == c.state().rotate(TURNS[1]), "wrong block at " + at);
            }
            check(HouseBuilder.POWER.cooldownLeft(p) > 0, "no cooldown after a file house");
            // over the same spot now: it refuses, like any other
            HouseBuilder.POWER.resetCooldown(p);
            String again = HouseBuilder.POWER.tryBuild(p, plan.id, 27000, Y, 20000, 1);
            check(again != null, "built over itself");
            HouseBuilder.POWER.sendPicker(p);
            p.discard();
            release(level, 27000, 20000, 30);
            try {
                for (Path f : List.of(hut, side, lit, old, big)) Files.deleteIfExists(f);
            } catch (IOException e) {
                throw new GameTestAssertException("couldn't tidy up: " + e);
            }
            HouseCatalog.scan(level.getServer(), true, null);
            h.succeed();
        });
    }
}
