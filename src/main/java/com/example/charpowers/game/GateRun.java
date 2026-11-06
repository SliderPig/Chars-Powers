package com.example.charpowers.game;

import com.example.charpowers.game.mob.GateBoss;
import com.example.charpowers.game.mob.GateKind;
import com.example.charpowers.game.model.GatePlan;
import com.example.charpowers.game.model.GatePlan.Kind;
import com.example.charpowers.game.model.GatePlan.Room;
import com.example.charpowers.game.model.GatePlan.Theme;
import com.example.charpowers.game.model.Elite;
import com.example.charpowers.game.model.GateRules;
import com.example.charpowers.own.Pocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

// One trip through a gate: a chain of rooms in the pocket dimension, built only for you and the friends you bring. Each room has a theme, a
// size, a shape and a goal (clear it, survive waves, kill the marked leader, cross a trap hall, find a key). The way on stays shut until it
// is done, and the last room holds a boss. It is all gone when the run ends.
final class GateRun {
    private static final BlockState BARS = Blocks.IRON_BARS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final int FLAGS = 2 | 16, PER_TICK = 1800, WALK_IN = 3;
    static final String KEY_TAG = "GateKey";

    final ServerLevel level;
    final int rank;
    final boolean sealed;
    final Theme theme;
    final UUID host;
    final List<Room> rooms;
    private final int[] cx;
    private final BlockPos first;
    private final Random rng = new Random();
    private final Set<UUID> members = new LinkedHashSet<>();
    private final List<Mob> mobs = new ArrayList<>();
    private final ArrayDeque<int[]> slices = new ArrayDeque<>();
    private final boolean[] built, open, trapLive;
    private int room = -1, leaveIn = -1, wave, waveWait, slitClock;
    private boolean ambushed, keyDropped, lootOpen, cleared;
    private int hold, jarsLeft, spawnClock;
    private final java.util.Map<BlockPos, int[]> jars = new java.util.HashMap<>(); // every block of a soul jar -> its hits left
    private Mob leader, bearer;
    private Vec3 bearerAt;
    private GateBoss boss;
    private Palette pal;

    GateRun(ServerPlayer host, ServerLevel level, int rank, boolean sealed, Theme chosen) {
        this.level = level;
        this.rank = GateRules.clampRank(rank);
        this.sealed = sealed;
        this.host = host.getUUID();
        this.first = Pocket.roomOf(host.getUUID()).offset(-100, 0, -100);
        this.theme = chosen != null ? chosen : GatePlan.themeFor(this.rank, rng, sealed);
        this.rooms = GatePlan.plan(this.rank, theme, rng);
        this.cx = GatePlan.centres(rooms);
        this.built = new boolean[rooms.size()];
        this.open = new boolean[rooms.size()];
        this.trapLive = new boolean[rooms.size()];
        members.add(host.getUUID());
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            for (int x = -r.halfX(); x <= r.halfX(); x++) slices.add(new int[]{i, x});
        }
        buildUntil(0); // the first room is ready when you step in; the others are built a little at a time while you fight
    }

    Set<UUID> members() {
        return members;
    }

    boolean has(UUID id) {
        return members.contains(id);
    }

    String themeName() {
        return GatePlan.title(theme);
    }

    // ---- building

    private BlockPos mid(int i) {
        return first.offset(cx[i], 0, 0);
    }

    private void set(BlockPos pos, BlockState state) {
        level.setBlock(pos, state, FLAGS);
    }

    private void buildUntil(int roomIndex) {
        while (!built[roomIndex] && !slices.isEmpty()) buildSome(Integer.MAX_VALUE);
    }

    private void buildSome(int budget) {
        int done = 0;
        while (!slices.isEmpty() && done < budget) {
            int[] s = slices.poll();
            done += slice(s[0], s[1]);
            if (slices.isEmpty() || slices.peek()[0] != s[0]) finishRoom(s[0]);
        }
    }

    private record Palette(BlockState[] wall, BlockState[] floor, BlockState ceiling, BlockState light, BlockState[] cover, BlockState accent) {}

    private Palette palette() {
        BlockState oakLeaves = Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState spruceLeaves = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState mangroveLeaves = Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState shroom = Blocks.SHROOMLIGHT.defaultBlockState();
        return switch (theme) {
            case GOBLIN_CAVE -> new Palette(st(Blocks.TUFF, Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.MOSSY_COBBLESTONE), st(Blocks.COARSE_DIRT, Blocks.PACKED_MUD, Blocks.GRAVEL, Blocks.MOSSY_COBBLESTONE),
                    Blocks.TUFF.defaultBlockState(), shroom, st(Blocks.MOSSY_COBBLESTONE, Blocks.HAY_BLOCK, Blocks.BONE_BLOCK), Blocks.BONE_BLOCK.defaultBlockState());
            case ELF_RUIN -> new Palette(st(Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS), st(Blocks.MOSS_BLOCK, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.GRASS_BLOCK),
                    oakLeaves, shroom, st(Blocks.CHISELED_STONE_BRICKS, Blocks.STRIPPED_OAK_LOG, Blocks.MOSSY_STONE_BRICKS), Blocks.GOLD_BLOCK.defaultBlockState());
            case ICE_KEEP -> new Palette(st(Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.PACKED_ICE, Blocks.SNOW_BLOCK, Blocks.STONE_BRICKS), st(Blocks.PACKED_ICE, Blocks.SNOW_BLOCK, Blocks.STONE_BRICKS),
                    Blocks.BLUE_ICE.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(), st(Blocks.BLUE_ICE, Blocks.PACKED_ICE, Blocks.STONE_BRICKS), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState());
            case ANT_NEST -> new Palette(st(Blocks.PACKED_MUD, Blocks.MUD_BRICKS, Blocks.BROWN_TERRACOTTA, Blocks.PACKED_MUD), st(Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.PACKED_MUD),
                    Blocks.PACKED_MUD.defaultBlockState(), Blocks.GLOWSTONE.defaultBlockState(), st(Blocks.HONEYCOMB_BLOCK, Blocks.MUD_BRICKS), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
            case DRAGON_KEEP -> new Palette(st(Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE, Blocks.CRACKED_DEEPSLATE_BRICKS), st(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.GILDED_BLACKSTONE),
                    Blocks.BLACKSTONE.defaultBlockState(), shroom, st(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.GOLD_BLOCK), Blocks.GOLD_BLOCK.defaultBlockState());
            case WOLF_FOREST -> new Palette(st(Blocks.MOSSY_COBBLESTONE, Blocks.DARK_OAK_LOG, Blocks.MOSS_BLOCK, Blocks.STRIPPED_SPRUCE_LOG), st(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MOSS_BLOCK, Blocks.COARSE_DIRT),
                    spruceLeaves, shroom, st(Blocks.STRIPPED_SPRUCE_LOG, Blocks.MOSSY_COBBLESTONE, Blocks.HAY_BLOCK), Blocks.MOSS_BLOCK.defaultBlockState());
            case DEMON_CASTLE -> new Palette(st(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS), st(Blocks.BLACKSTONE, Blocks.NETHER_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.MAGMA_BLOCK),
                    Blocks.BLACKSTONE.defaultBlockState(), shroom, st(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.NETHER_BRICKS), Blocks.GILDED_BLACKSTONE.defaultBlockState());
            case CRYPT -> new Palette(st(Blocks.DEEPSLATE_BRICKS, Blocks.COBBLED_DEEPSLATE, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES), st(Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.SOUL_SOIL),
                    Blocks.DEEPSLATE.defaultBlockState(), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), st(Blocks.CHISELED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.BONE_BLOCK), Blocks.CRYING_OBSIDIAN.defaultBlockState());
            case MARSH -> new Palette(st(Blocks.MUD_BRICKS, Blocks.MANGROVE_PLANKS, Blocks.MOSS_BLOCK, Blocks.PACKED_MUD), st(Blocks.MUD, Blocks.PACKED_MUD, Blocks.MOSS_BLOCK, Blocks.MUD),
                    mangroveLeaves, shroom, st(Blocks.MANGROVE_LOG, Blocks.MUD_BRICKS, Blocks.MOSS_BLOCK), Blocks.MOSS_BLOCK.defaultBlockState());
            case DESERT_TOMB -> new Palette(st(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.CHISELED_SANDSTONE), st(Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE, Blocks.SAND),
                    Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.GLOWSTONE.defaultBlockState(), st(Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE, Blocks.GOLD_BLOCK), Blocks.GOLD_BLOCK.defaultBlockState());
            case STATION -> new Palette(st(Blocks.IRON_BLOCK, Blocks.SMOOTH_QUARTZ, Blocks.LIGHT_GRAY_CONCRETE, Blocks.QUARTZ_BLOCK), st(Blocks.LIGHT_GRAY_CONCRETE, Blocks.SMOOTH_QUARTZ, Blocks.IRON_BLOCK),
                    Blocks.IRON_BLOCK.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(), st(Blocks.IRON_BLOCK, Blocks.QUARTZ_PILLAR, Blocks.LIGHT_BLUE_STAINED_GLASS), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState());
        };
    }

    private static BlockState[] st(Block... blocks) {
        BlockState[] out = new BlockState[blocks.length];
        for (int i = 0; i < blocks.length; i++) out[i] = blocks[i].defaultBlockState();
        return out;
    }

    private BlockState pick(BlockState[] from) {
        return from[rng.nextInt(from.length)];
    }

    // One x-column of a room, floor to ceiling. Everything in the room's box that isn't walkable is solid, which is what makes round and cross rooms.
    private int slice(int i, int x) {
        if (pal == null) pal = palette();
        Room r = rooms.get(i);
        BlockPos m = mid(i);
        boolean lastRoom = i == rooms.size() - 1;
        int count = 0;
        for (int z = -r.halfZ(); z <= r.halfZ(); z++) {
            boolean walk = GatePlan.walkable(r, x, z);
            boolean doorX = Math.abs(x) == r.halfX();
            if (doorX && ((x < 0 && i == 0) || (x > 0 && lastRoom))) walk = false; // no door out of the first room's back or the boss room's far end
            for (int y = -1; y <= r.height(); y++) {
                BlockState state;
                if (y == -1) state = walk ? pick(pal.floor) : pick(pal.wall);
                else if (y == r.height()) state = pal.ceiling;
                else if (walk && (!doorX || y <= 2)) state = AIR;
                else state = pick(pal.wall);
                set(m.offset(x, y, z), state);
                count++;
            }
        }
        return count;
    }

    // Once every column of a room is in: cover, lights, the corridor on, the door, and what the room's goal needs.
    private void finishRoom(int i) {
        Room r = rooms.get(i);
        BlockPos m = mid(i);
        boolean lastRoom = i == rooms.size() - 1;
        Random lay = new Random(rng.nextLong());
        for (int[] c : GatePlan.cover(r, lay)) {
            BlockState stuff = pick(pal.cover);
            int tall = c[2] == 1 ? 2 : r.height() - 1;
            for (int dx = 0; dx < 2; dx++) {
                for (int dz = 0; dz < 2; dz++) {
                    for (int y = 0; y < tall; y++) set(m.offset(c[0] + dx, y, c[1] + dz), stuff);
                }
            }
        }
        for (int x = -r.halfX() + 3; x < r.halfX() - 2; x += 6) { // lights in the ceiling
            for (int z = -r.halfZ() + 3; z < r.halfZ() - 2; z += 6) {
                if (!GatePlan.walkable(r, x, z)) continue;
                if (pal.light.getBlock() instanceof LanternBlock) set(m.offset(x, r.height() - 1, z), pal.light);
                else set(m.offset(x, r.height(), z), theme == Theme.STATION && ((x + z) / 6) % 2 == 0 ? Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true) : pal.light);
            }
        }
        decorate(i, r, m, lay);
        if (r.kind() == Kind.BOSS) { // a marked ring for the fight
            for (int a = 0; a < 48; a++) {
                double t = Math.PI * 2 * a / 48;
                set(m.offset((int) Math.round(Math.cos(t) * 6), -1, (int) Math.round(Math.sin(t) * 6)), pal.accent);
            }
        }
        if (r.kind() == Kind.TRAP) {
            for (int x = -r.halfX() + 4; x <= r.halfX() - 4; x += 4) {
                set(m.offset(x, 1, -r.halfZ()), Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
                set(m.offset(x + 2, 1, r.halfZ()), Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.NORTH));
            }
        }
        if (r.loot()) buildAlcove(r, m);
        buildSpecial(i, r, m);
        if (!lastRoom) {
            BlockState wall = pick(pal.wall);
            for (int x = r.halfX() + 1; x <= r.halfX() + GatePlan.CORRIDOR; x++) { // the corridor to the next room
                for (int z = -2; z <= 2; z++) {
                    for (int y = -1; y <= 3; y++) {
                        boolean shell = Math.abs(z) == 2 || y == -1 || y == 3;
                        set(m.offset(x, y, z), shell ? wall : AIR);
                    }
                }
            }
            shutDoor(i); // last, so the corridor's air doesn't wipe the barrier
        }
        built[i] = true;
    }

    // Little touches that make each theme look like itself.
    private void decorate(int i, Room r, BlockPos m, Random lay) {
        switch (theme) {
            case DESERT_TOMB -> {
                for (int x = -r.halfX() + 3; x < r.halfX() - 2; x += 4) { // torches along the walls
                    for (int side = -1; side <= 1; side += 2) {
                        int z = side * (r.halfZ() - 1);
                        if (GatePlan.walkable(r, x, z)) set(m.offset(x, 0, z), Blocks.TORCH.defaultBlockState());
                    }
                }
            }
            case DRAGON_KEEP -> {
                for (int x = -r.halfX() + 1; x < r.halfX(); x++) {
                    for (int z = -1; z <= 1; z++) set(m.offset(x, 0, z), Blocks.BLACK_CARPET.defaultBlockState());
                }
            }
            case DEMON_CASTLE -> {
                for (int k = 0; k < 3; k++) { // little lava pools, boxed in below so they stay put
                    int x = lay.nextInt(2 * r.halfX() - 8) - r.halfX() + 4, z = (lay.nextBoolean() ? 1 : -1) * (3 + lay.nextInt(Math.max(1, r.halfZ() - 5)));
                    if (!GatePlan.walkable(r, x, z) || !GatePlan.walkable(r, x + 1, z + 1) || r.kind() == Kind.TRAP) continue;
                    for (int dx = 0; dx < 2; dx++) {
                        for (int dz = 0; dz < 2; dz++) {
                            set(m.offset(x + dx, -2, z + dz), Blocks.BLACKSTONE.defaultBlockState());
                            set(m.offset(x + dx, -1, z + dz), Blocks.LAVA.defaultBlockState());
                        }
                    }
                }
            }
            case CRYPT -> {
                for (int k = 0; k < 5; k++) { // soul fire on soul soil, and a few skulls
                    int x = lay.nextInt(2 * r.halfX() - 6) - r.halfX() + 3, z = (lay.nextBoolean() ? 1 : -1) * (3 + lay.nextInt(Math.max(1, r.halfZ() - 4)));
                    if (!GatePlan.walkable(r, x, z)) continue;
                    if (k < 3) {
                        set(m.offset(x, -1, z), Blocks.SOUL_SOIL.defaultBlockState());
                        set(m.offset(x, 0, z), Blocks.SOUL_FIRE.defaultBlockState());
                    } else {
                        set(m.offset(x, 0, z), Blocks.SKELETON_SKULL.defaultBlockState());
                    }
                }
            }
            case STATION -> {
                for (int x = -r.halfX() + 2; x < r.halfX() - 1; x += 4) { // a blue glow along the lane
                    for (int side = -1; side <= 1; side += 2) set(m.offset(x, -1, side * 2), Blocks.SEA_LANTERN.defaultBlockState());
                }
            }
            case MARSH -> {
                for (int k = 0; k < 8; k++) {
                    int x = lay.nextInt(2 * r.halfX() - 4) - r.halfX() + 2, z = lay.nextInt(2 * r.halfZ() - 3) - r.halfZ() + 2;
                    if (GatePlan.walkable(r, x, z)) set(m.offset(x, 0, z), Blocks.MOSS_CARPET.defaultBlockState());
                }
            }
            case WOLF_FOREST -> {
                for (int k = 0; k < 10; k++) {
                    int x = lay.nextInt(2 * r.halfX() - 4) - r.halfX() + 2, z = lay.nextInt(2 * r.halfZ() - 3) - r.halfZ() + 2;
                    if (GatePlan.walkable(r, x, z)) set(m.offset(x, 0, z), (lay.nextBoolean() ? Blocks.FERN : Blocks.GRASS).defaultBlockState());
                }
            }
            case ICE_KEEP -> {
                for (int k = 0; k < 8; k++) {
                    int x = lay.nextInt(2 * r.halfX() - 4) - r.halfX() + 2, z = lay.nextInt(2 * r.halfZ() - 3) - r.halfZ() + 2;
                    if (GatePlan.walkable(r, x, z)) set(m.offset(x, 0, z), Blocks.SNOW.defaultBlockState());
                }
            }
            default -> {}
        }
    }

    // What the special rooms are made of: the maze walls, the terminal, the soul jars.
    private void buildSpecial(int i, Room r, BlockPos m) {
        switch (r.kind()) {
            case MAZE -> {
                for (int[] w : GatePlan.mazeWalls(r)) {
                    for (int z = w[1]; z <= w[2]; z++) {
                        for (int y = 0; y < 3; y++) set(m.offset(w[0], y, z), pick(pal.cover));
                    }
                }
            }
            case TERMINAL -> {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) set(m.offset(dx, 0, dz), Blocks.IRON_BLOCK.defaultBlockState());
                }
                set(m.offset(0, 1, 0), Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
                set(m.offset(0, 2, 0), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            }
            case PHYLACTERY -> {
                jars.clear();
                for (int k = 0; k < r.waves(); k++) {
                    double a = Math.PI * 2 * k / r.waves() + 0.5;
                    int x = (int) Math.round(Math.cos(a) * 5), z = (int) Math.round(Math.sin(a) * 5);
                    BlockPos base = m.offset(x, 0, z);
                    int[] hp = {4 + rank / 2};
                    set(base, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                    set(base.above(), Blocks.CRYING_OBSIDIAN.defaultBlockState());
                    set(base.above(2), Blocks.SOUL_LANTERN.defaultBlockState());
                    jars.put(base, hp);
                    jars.put(base.above(), hp);
                    jars.put(base.above(2), hp);
                }
            }
            default -> {}
        }
    }

    // Someone hit a block in the room: if it belongs to a soul jar, the jar takes the hit. True when it was a jar.
    boolean jarHit(BlockPos pos, ServerPlayer p) {
        int[] hp = jars.get(pos);
        if (hp == null) return false;
        hp[0]--;
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.5f, 0.6f);
        level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
        if (hp[0] > 0) return true;
        for (java.util.Iterator<java.util.Map.Entry<BlockPos, int[]>> it = jars.entrySet().iterator(); it.hasNext(); ) {
            java.util.Map.Entry<BlockPos, int[]> e = it.next();
            if (e.getValue() == hp) {
                set(e.getKey(), AIR);
                it.remove();
            }
        }
        jarsLeft--;
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2f, 0.7f);
        tell(jarsLeft > 0 ? "A soul jar shatters. " + jarsLeft + " left." : "The last soul jar shatters.");
        return true;
    }

    private void buildAlcove(Room r, BlockPos m) {
        int zWall = -r.halfZ();
        BlockState wall = pick(pal.wall);
        for (int x = -2; x <= 2; x++) {
            for (int z = zWall - 4; z <= zWall; z++) {
                for (int y = -1; y <= 3; y++) {
                    boolean shell = Math.abs(x) == 2 || z == zWall - 4 || y == -1 || y == 3;
                    if (z == zWall && Math.abs(x) <= 1 && y >= 0 && y <= 2) continue; // the barred opening, set below
                    set(m.offset(x, y, z), shell ? wall : AIR);
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 3; y++) {
                set(m.offset(x, y, zWall), BARS.setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true));
                set(m.offset(x, y, zWall - 1), Blocks.BARRIER.defaultBlockState());
            }
        }
        BlockPos chest = m.offset(0, 0, zWall - 3);
        set(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        BlockEntity be = level.getBlockEntity(chest);
        if (be instanceof ChestBlockEntity box) {
            List<GatePlan.Item> loot = GatePlan.chestLoot(rank + (sealed ? 1 : 0), rng, 6 + rank);
            for (int s = 0; s < loot.size() && s < 27; s++) {
                GatePlan.Item l = loot.get(s);
                Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(l.id()));
                if (it != null) box.setItem(s * 2 % 27, new ItemStack(it, l.count()));
            }
        }
    }

    private BlockPos alcoveDoor(int i) {
        return mid(i).offset(0, 0, -rooms.get(i).halfZ() + 1);
    }

    private void openAlcove(int i) {
        BlockPos m = mid(i);
        int zWall = -rooms.get(i).halfZ();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 3; y++) {
                set(m.offset(x, y, zWall), AIR);
                set(m.offset(x, y, zWall - 1), AIR);
            }
        }
        level.playSound(null, m.offset(0, 1, zWall), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2f, 0.8f);
    }

    private void shutDoor(int i) {
        BlockPos m = mid(i);
        int x = rooms.get(i).halfX();
        for (int z = -1; z <= 1; z++) {
            for (int y = 0; y < 3; y++) {
                // set without neighbour updates, so the bars are told by hand which way they connect: left alone they stand as lone posts you can slip between
                set(m.offset(x, y, z), BARS.setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true));
                set(m.offset(x + 1, y, z), Blocks.BARRIER.defaultBlockState()); // and nothing, not even a pearl, gets past this
            }
        }
    }

    private void openDoor(int i) {
        BlockPos m = mid(i);
        int x = rooms.get(i).halfX();
        for (int z = -1; z <= 1; z++) {
            for (int y = 0; y < 3; y++) {
                set(m.offset(x, y, z), AIR);
                set(m.offset(x + 1, y, z), AIR);
            }
        }
        open[i] = true;
        level.playSound(null, m.offset(x, 1, 0), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2f, 0.6f);
        level.playSound(null, m.offset(x, 1, 0), SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.5f, 1.8f);
    }

    // ---- people

    Vec3 entrance() {
        return Vec3.atBottomCenterOf(mid(0).offset(-rooms.get(0).halfX() + WALK_IN, 0, 0));
    }

    void buildAll() {
        while (!slices.isEmpty()) buildSome(Integer.MAX_VALUE);
    }

    // The start of room i (tests and joins use it).
    Vec3 startOf(int i) {
        return Vec3.atBottomCenterOf(mid(i).offset(-rooms.get(i).halfX() + WALK_IN, 0, 0));
    }

    private BlockPos exit() {
        return mid(0).offset(-rooms.get(0).halfX() + 1, 0, 0);
    }

    // Where someone who arrives now stands: the start of the room being fought in.
    private Vec3 meetingPoint() {
        int i = Math.max(0, room);
        return Vec3.atBottomCenterOf(mid(i).offset(-rooms.get(i).halfX() + WALK_IN, 0, 0));
    }

    // A friend arrives: at the start together with the host, or through the entry of the room in progress.
    void join(ServerPlayer p) {
        members.add(p.getUUID());
        Vec3 at = room <= 0 ? entrance() : meetingPoint();
        Pocket.enter(p, level, BlockPos.containing(at));
        p.setYRot(-90f);
        p.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.6f);
        if (boss != null) boss.addToParty(p);
        buildUntil(Math.max(0, room));
    }

    // Takes someone out of the run. home = also send them back where they came from.
    void leave(UUID id, boolean home) {
        members.remove(id);
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
        if (p == null) return;
        if (boss != null) boss.removeFromParty(p);
        if (home && Pocket.in(p)) Pocket.leave(p);
    }

    List<ServerPlayer> players() {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : members) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null && p.level() == level && p.isAlive()) out.add(p);
        }
        return out;
    }

    int partySize() {
        return Math.max(1, members.size());
    }

    private void tell(String text) {
        for (ServerPlayer p : players()) p.displayClientMessage(Component.literal(text), true);
    }

    // ---- every tick: building, and the arrow slits

    void tick() {
        if (!slices.isEmpty()) buildSome(PER_TICK);
        if (room >= 0 && rooms.get(room).kind() == Kind.TRAP && trapLive[room] && !open[room]) slits(room);
        // a shut door really is shut: anyone who got past the barrier (a laggy pearl, a push) is put back
        if (room >= 0 && room < rooms.size() - 1 && !open[room]) {
            double limit = mid(room).getX() + rooms.get(room).halfX() + 0.4;
            for (ServerPlayer p : players()) {
                if (p.getX() > limit) p.teleportTo(level, limit - 2.4, p.getY(), p.getZ(), p.getYRot(), p.getXRot());
            }
        }
    }

    private void slits(int i) {
        Room r = rooms.get(i);
        BlockPos m = mid(i);
        slitClock++;
        int cycle = slitClock % 40;
        boolean north = (slitClock / 40) % 2 == 0; // this round the slits in the north wall, next the south one
        if (cycle != 28 && cycle != 38) return;
        boolean someoneIn = false;
        for (ServerPlayer p : players()) someoneIn |= Math.abs(p.getX() - m.getX()) <= r.halfX();
        if (!someoneIn) return;
        for (int x = -r.halfX() + 4; x <= r.halfX() - 4; x += 4) {
            BlockPos slit = north ? m.offset(x, 1, -r.halfZ()) : m.offset(x + 2, 1, r.halfZ());
            Vec3 out = Vec3.atCenterOf(slit).add(0, 0, north ? 1 : -1);
            if (cycle == 28) { // the tell: a puff and a click ten ticks before
                level.sendParticles(ParticleTypes.SMOKE, out.x, out.y, out.z, 6, 0.1, 0.1, 0.1, 0.01);
                level.playSound(null, slit, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1f, 1.4f);
            } else {
                Arrow arrow = new Arrow(level, out.x, out.y, out.z);
                arrow.shoot(0, 0.02, north ? 1 : -1, 1.3f, 0f);
                arrow.setBaseDamage(2 + rank * 0.8);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                level.addFreshEntity(arrow);
                level.playSound(null, slit, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1f, 1f);
            }
        }
    }

    // ---- every second. True when it is over and everyone has gone home.

    boolean second() {
        // anyone who left by other means (a command, a death) is no longer in the run
        for (UUID id : new ArrayList<>(members)) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null && (p.level() != level || !p.isAlive())) members.remove(id);
        }
        if (members.isEmpty()) {
            end();
            return true;
        }
        if (leaveIn >= 0) {
            if (leaveIn-- == 0) {
                for (UUID id : new ArrayList<>(members)) leave(id, true);
                end();
                return true;
            }
            return false;
        }
        List<ServerPlayer> here = players();
        if (room < 0) {
            room = 0;
            activate(0);
            return false;
        }
        mobs.removeIf(m -> !m.isAlive() || m.isRemoved());
        for (Mob m : mobs) {
            ServerPlayer near = nearest(m);
            if (near != null && (m.getTarget() == null || !m.getTarget().isAlive() || m.getTarget().level() != level)) m.setTarget(near);
        }
        objective(here);
        advance(here);
        if (room == 0 && !sealed) {
            // behind you at the start, a way out if it's too much. It costs you the gate, so it takes a crouch: a Ravager's shove won't do it.
            for (ServerPlayer p : here) {
                GamePlayer.POWER.drawRing(p, Vec3.atBottomCenterOf(exit()), 1f, 16);
                if (p.isShiftKeyDown() && p.position().distanceToSqr(Vec3.atBottomCenterOf(exit())) <= 2.2) {
                    GamePlayer.POWER.noteAll(p, "You ran. The gate closes behind you.");
                    leave(p.getUUID(), true);
                }
            }
        }
        if (members.isEmpty()) {
            end();
            return true;
        }
        return false;
    }

    private ServerPlayer nearest(Mob m) {
        ServerPlayer best = null;
        for (ServerPlayer p : players()) {
            if (best == null || p.distanceToSqr(m) < best.distanceToSqr(m)) best = p;
        }
        return best;
    }

    // Walk into the next room once the door is open.
    private void advance(List<ServerPlayer> here) {
        if (room >= rooms.size() - 1 || !open[room] || !built[room + 1]) return;
        int next = room + 1;
        for (ServerPlayer p : here) {
            if (p.getX() > mid(next).getX() - rooms.get(next).halfX() + 0.5) {
                room = next;
                ambushed = false;
                wave = 0;
                leader = null;
                bearer = null;
                activate(next);
                return;
            }
        }
    }

    private ServerPlayer anyone() {
        List<ServerPlayer> ps = players();
        return ps.isEmpty() ? null : ps.get(0);
    }

    // ---- what a room asks of you

    private void activate(int i) {
        Room r = rooms.get(i);
        switch (r.kind()) {
            case ARENA -> spawn(i, GatePlan.count(rank, i, partySize(), r.kind()));
            case WAVES -> {
                wave = 1;
                spawn(i, GatePlan.count(rank, i, partySize(), r.kind()));
                tell("Wave 1 of " + r.waves() + ".");
            }
            case SHAMAN -> {
                spawn(i, GatePlan.count(rank, i, partySize(), r.kind()));
                leader = spawnOne(i, GatePlan.leader(theme), true, 1.6);
                tell("Kill the glowing leader. The rest will lose heart.");
            }
            case KEY -> {
                spawn(i, GatePlan.count(rank, i, partySize(), r.kind()));
                bearer = spawnOne(i, rank >= 3 ? "charpowers:orc" : "charpowers:hobgoblin", true, 2.5);
                if (bearer != null) bearer.setCustomName(Component.literal("Key Bearer"));
                keyDropped = false;
                lootOpen = false;
                tell("The glowing Key Bearer carries the key to the barred alcove.");
            }
            case TERMINAL -> {
                hold = 0;
                spawn(i, 3 + rank / 2);
                tell("Hold the terminal: stay close to it while it works.");
            }
            case PHYLACTERY -> {
                jarsLeft = r.waves();
                spawnClock = 0;
                spawn(i, 3);
                tell("Smash the soul jars. The dead keep rising until they are gone.");
            }
            case MAZE -> {
                spawnOne(i, "charpowers:minotaur", true, 1.4);
                spawn(i, GatePlan.count(rank, i, partySize(), r.kind()));
                tell("A maze. Find your way to the beast at the end.");
            }
            case TRAP -> {
                trapLive[i] = true;
                tell("Cross the hall. Mind the slits in the walls.");
            }
            case BOSS -> {
                if (boss == null) spawnBoss(i);
            }
        }
    }

    private void spawnBoss(int i) {
        Room r = rooms.get(i);
        String id = GatePlan.boss(theme, rank, rng);
        GateKind kind = GateKind.GOBLIN_KING;
        for (GateKind k : GateKind.values()) {
            if (("charpowers:" + k.id).equals(id)) kind = k;
        }
        Vec3 at = Vec3.atBottomCenterOf(mid(i).offset(r.halfX() - 4, 0, 0));
        boss = GateBoss.spawn(level, kind, at, rank, partySize(), mobs::add, this::bossKilled);
        for (ServerPlayer p : players()) boss.addToParty(p);
        tell("Rank " + GateRules.RANKS[rank] + " boss: " + kind.title + ".");
        level.playSound(null, BlockPos.containing(at), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.5f, 0.8f);
    }

    private void bossKilled() {
        if (cleared) return;
        cleared = true;
        com.example.charpowers.items.SolgoSpawning.afterGateBoss(level, boss, rank, partySize());
        GamePlayer.POWER.gateCleared(this);
        leaveIn = 8;
        mobs.forEach(Mob::discard);
        mobs.clear();
        for (ServerPlayer p : players()) GamePlayer.POWER.noteAll(p, "The gate closes in a few seconds.");
    }

    private void objective(List<ServerPlayer> here) {
        if (room < 0 || here.isEmpty()) return;
        Room r = rooms.get(room);
        BlockPos m = mid(room);
        switch (r.kind()) {
            case BOSS -> {
                if (boss == null && here.stream().anyMatch(p -> p.getX() > m.getX() - r.halfX() + 3)) spawnBoss(room);
            }
            case ARENA, MAZE -> finishWhenClear();
            case TERMINAL -> {
                if (open[room]) return;
                BlockPos term = m;
                boolean near = here.stream().anyMatch(p -> p.blockPosition().distSqr(term) <= 49);
                if (near) hold++;
                tell("Terminal " + Math.min(100, hold * 100 / Math.max(1, r.waves())) + "%" + (near ? "" : " (paused: get close)"));
                if (near && hold % 6 == 0 && mobs.size() < 8) spawn(room, 2 + rank / 2);
                if (hold >= r.waves()) {
                    mobs.forEach(mob -> mob.discard());
                    mobs.clear();
                    tell("Terminal done. The room unlocks.");
                    done();
                }
            }
            case PHYLACTERY -> {
                if (open[room]) return;
                if (jarsLeft > 0 && mobs.size() < 7 && ++spawnClock % 6 == 0) spawn(room, 2);
                if (jarsLeft <= 0) {
                    mobs.forEach(mob -> {
                        level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
                        mob.discard();
                    });
                    mobs.clear();
                    done();
                }
            }
            case WAVES -> {
                if (!mobs.isEmpty() || open[room]) return;
                if (wave >= r.waves()) {
                    done();
                } else if (++waveWait >= 3) {
                    waveWait = 0;
                    wave++;
                    spawn(room, GatePlan.count(rank, room, partySize(), r.kind()));
                    tell("Wave " + wave + " of " + r.waves() + ".");
                }
            }
            case SHAMAN -> {
                if (leader != null && !leader.isAlive() && !open[room]) {
                    mobs.forEach(mob -> {
                        level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 1, mob.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
                        mob.discard();
                    });
                    mobs.clear();
                    tell("The leader fell. The pack flees.");
                    done();
                } else if (leader == null) {
                    finishWhenClear();
                }
            }
            case KEY -> {
                if (bearer != null) {
                    if (bearer.isAlive()) {
                        bearerAt = bearer.position();
                    } else if (!keyDropped) {
                        keyDropped = true;
                        ItemStack key = new ItemStack(Items.TRIPWIRE_HOOK);
                        key.getOrCreateTag().putBoolean(KEY_TAG, true);
                        key.setHoverName(Component.literal("Gate Key"));
                        Vec3 at = bearerAt == null ? Vec3.atBottomCenterOf(m) : bearerAt;
                        level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.5, at.z, key));
                        tell("The Key Bearer dropped the key. Take it to the barred alcove.");
                    }
                }
                if (!lootOpen) useKey(here);
                finishWhenClear();
            }
            case TRAP -> {
                if (open[room]) return;
                if (!ambushed && here.stream().anyMatch(p -> p.getX() > m.getX())) {
                    ambushed = true;
                    int n = GatePlan.count(rank, room, partySize(), Kind.ARENA) / 2 + 2;
                    for (int k = 0; k < n; k++) {
                        BlockPos at = m.offset(k % 2 == 0 ? -r.halfX() + 2 : r.halfX() - 2, 0, (k % 3) - 1);
                        spawnAt(at, randomMob(), false, 1.0);
                    }
                    tell("An ambush!");
                    level.playSound(null, m, SoundEvents.WARDEN_NEARBY_CLOSER, SoundSource.HOSTILE, 1.5f, 1f);
                }
                if (ambushed && mobs.isEmpty() && here.stream().anyMatch(p -> p.getX() > m.getX() + r.halfX() - 4)) done();
            }
        }
    }

    private void useKey(List<ServerPlayer> here) {
        for (ServerPlayer p : here) {
            if (p.blockPosition().distSqr(alcoveDoor(room)) > 25) continue;
            for (ItemStack s : p.getInventory().items) {
                if (s.is(Items.TRIPWIRE_HOOK) && s.hasTag() && s.getTag().getBoolean(KEY_TAG)) {
                    s.shrink(1);
                    lootOpen = true;
                    openAlcove(room);
                    tell("The alcove is open.");
                    return;
                }
            }
        }
    }

    private void finishWhenClear() {
        if (mobs.isEmpty() && !open[room]) done();
    }

    private void done() {
        if (room >= rooms.size() - 1 || open[room]) return;
        if (!built[room + 1]) return; // built a little at a time; this is a second or two away at most
        openDoor(room);
        tell("Room " + (room + 1) + " of " + rooms.size() + " clear. The door is open.");
    }

    // ---- creatures

    private String randomMob() {
        List<String> kinds = rng.nextInt(100) < 10 ? GatePlan.general(rank) : GatePlan.mobs(theme, rank); // now and then something from outside the theme
        return kinds.get(rng.nextInt(kinds.size()));
    }

    private void spawn(int i, int count) {
        for (int n = 0; n < count; n++) spawnOne(i, randomMob(), false, 1.0);
    }

    private Mob spawnOne(int i, String id, boolean marked, double health) {
        Room r = rooms.get(i);
        BlockPos at = mid(i).offset(r.halfX() - 3, 0, 0);
        for (int tries = 0; tries < 30; tries++) {
            int x = -r.halfX() + 5 + rng.nextInt(Math.max(1, 2 * r.halfX() - 8)), z = rng.nextInt(2 * r.halfZ() - 3) - r.halfZ() + 2;
            if (!GatePlan.walkable(r, x, z)) continue;
            BlockPos p = mid(i).offset(x, 0, z);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getBlockState(p.below()).canOcclude()) {
                at = p;
                break;
            }
        }
        return spawnAt(at, id, marked, health);
    }

    private Mob spawnAt(BlockPos at, String id, boolean marked, double healthTimes) {
        Optional<EntityType<?>> type = EntityType.byString(id);
        if (type.isEmpty() || !(type.get().create(level) instanceof Mob mob)) return null;
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rng.nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.MOB_SUMMONED, null, null);
        mob.setPersistenceRequired();
        if (mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin) piglin.setImmuneToZombification(true);
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * GateRules.mobHealth(rank) * GatePlan.partyMobHealth(partySize()) * healthTimes);
            mob.setHealth(mob.getMaxHealth());
        }
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(damage.getBaseValue() * GateRules.mobDamage(rank));
        if (marked) mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100000, 0, false, false));
        Elite elite = marked ? null : Elite.roll(rank, rng);
        if (elite != null && mob instanceof com.example.charpowers.game.mob.GateMob gm) gm.makeElite(elite);
        ServerPlayer target = anyone();
        if (target != null) mob.setTarget(target);
        level.addFreshEntity(mob);
        mobs.add(mob);
        return mob;
    }

    // ---- over

    void end() {
        mobs.forEach(Mob::discard);
        mobs.clear();
        if (boss != null) boss.discard();
    }
}
