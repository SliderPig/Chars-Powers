package com.example.charpowers.house;

import com.example.charpowers.hidden.ModSounds;
import com.example.charpowers.house.model.HouseRules;
import com.example.charpowers.house.model.HouseTurn;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// One house going up: plants and bumps cleared top down, foundations down to the ground, then the house layer by layer from the
// ground up, a few blocks a tick. Setting a block this way doesn't make a sound or wake the neighbours, so it costs very little.
public final class BuildJob {
    private static final TicketType<ChunkPos> TICKET = TicketType.create("charpowers_house", Comparator.comparingLong(ChunkPos::toLong));
    // quiet: no neighbour updates, so doors, lanterns and fences stay exactly as the file says
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final DustParticleOptions PINK = new DustParticleOptions(new Vector3f(1f, 0.55f, 0.8f), 1.1f);
    private static final Rotation[] TURNS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};

    final String houseId;
    final ResourceKey<Level> dimension;
    final int cx, y, cz, turns;
    final UUID owner;
    final String ownerName;
    int stage; // 0 clearing, 1 foundations, 2 building
    int index;
    private final List<BlockPos> list = new ArrayList<>();
    private final List<BlockState> fills = new ArrayList<>();
    private boolean listed;
    private boolean ticketed;
    private HousePlan plan;
    private int perTick;

    BuildJob(String houseId, ResourceKey<Level> dimension, int cx, int y, int cz, int turns, UUID owner, String ownerName) {
        this.houseId = houseId;
        this.dimension = dimension;
        this.cx = cx;
        this.y = y;
        this.cz = cz;
        this.turns = turns & 3;
        this.owner = owner;
        this.ownerName = ownerName;
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("house", houseId);
        t.putString("dim", dimension.location().toString());
        t.putInt("x", cx);
        t.putInt("y", y);
        t.putInt("z", cz);
        t.putInt("turns", turns);
        t.putUUID("owner", owner);
        t.putString("name", ownerName);
        t.putInt("stage", stage);
        t.putInt("index", index);
        return t;
    }

    static BuildJob load(CompoundTag t) {
        BuildJob j = new BuildJob(t.getString("house"), ResourceKey.create(Registries.DIMENSION, new ResourceLocation(t.getString("dim"))),
                t.getInt("x"), t.getInt("y"), t.getInt("z"), t.getInt("turns"), t.getUUID("owner"), t.getString("name"));
        j.stage = t.getInt("stage");
        j.index = t.getInt("index");
        return j;
    }

    private void tickets(ServerLevel level, boolean add) {
        int[] rect = HouseTurn.rect(plan.w, plan.d, cx, cz, turns);
        for (int cxx = rect[0] >> 4; cxx <= rect[2] >> 4; cxx++) {
            for (int czz = rect[1] >> 4; czz <= rect[3] >> 4; czz++) {
                ChunkPos chunk = new ChunkPos(cxx, czz);
                if (add) level.getChunkSource().addRegionTicket(TICKET, chunk, 1, chunk);
                else level.getChunkSource().removeRegionTicket(TICKET, chunk, 1, chunk);
            }
        }
    }

    // True once it's finished (or can't go on).
    boolean tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return true;
        if (plan == null) {
            plan = HouseCatalog.get(server, houseId);
            if (plan == null) return HouseCatalog.ready(); // its file may just not have been read yet after a restart
            perTick = HouseRules.perTick(plan.cells.size(), Setting.HOUSE_SPEED.d());
        }
        if (!ticketed) {
            ticketed = true;
            tickets(level, true);
        }
        int[] rect = HouseTurn.rect(plan.w, plan.d, cx, cz, turns);
        if (!level.isLoaded(new BlockPos(rect[0], y, rect[1])) || !level.isLoaded(new BlockPos(rect[2], y, rect[3]))) return false; // its tickets are loading it
        boolean done = switch (stage) {
            case 0 -> clearing(level, rect);
            case 1 -> foundations(level);
            default -> building(level);
        };
        if (done && stage < 2) {
            stage++;
            index = 0;
            list.clear();
            fills.clear();
            listed = false;
            return false;
        }
        if (done) {
            tickets(level, false);
            finished(level, server);
            return true;
        }
        return false;
    }

    private boolean clearing(ServerLevel level, int[] rect) {
        if (!listed) {
            listed = true;
            // the plot itself, and anything left of a tall tree above where the roof ends
            int above = Math.min(y + plan.h + 32, level.getMaxBuildHeight() - 1);
            for (int yy = above; yy > y; yy--) {
                boolean treesOnly = yy > y + plan.h;
                for (int x = rect[0]; x <= rect[2]; x++) {
                    for (int z = rect[1]; z <= rect[3]; z++) {
                        BlockPos at = new BlockPos(x, yy, z);
                        BlockState s = level.getBlockState(at);
                        if (s.isAir() || !HouseSite.mayClear(level, at, s)) continue;
                        if (!treesOnly || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) list.add(at);
                    }
                }
            }
        }
        for (int n = 0; n < perTick * 3 && index < list.size(); n++, index++) {
            BlockPos at = list.get(index);
            BlockState was = level.getBlockState(at);
            if (was.isAir() || !HouseSite.stillClearable(was)) continue;
            level.setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            if (index % 6 == 0) level.sendParticles(ParticleTypes.POOF, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.01);
        }
        return index >= list.size();
    }

    private boolean foundations(ServerLevel level) {
        if (!listed) {
            listed = true;
            for (HousePlan.Cell c : plan.cells) {
                if (c.y() != 0) break;
                BlockState fill = foundationUnder(c.state());
                BlockPos at = spot(c).below();
                int depth = 0;
                while (depth <= HouseRules.MAX_DROP + 4 && !HouseSite.isGround(level.getBlockState(at))) {
                    if (level.getBlockEntity(at) == null) {
                        list.add(at);
                        fills.add(fill);
                    }
                    at = at.below();
                    depth++;
                }
            }
        }
        for (int n = 0; n < perTick * 3 && index < list.size(); n++, index++) {
            level.setBlock(list.get(index), fills.get(index), FLAGS);
        }
        return index >= list.size();
    }

    // Soil under the garden, stone under the house.
    private static BlockState foundationUnder(BlockState s) {
        if (s.is(BlockTags.DIRT) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.GRAVEL)) return Blocks.DIRT.defaultBlockState();
        if (!s.getFluidState().isEmpty()) return Blocks.STONE.defaultBlockState();
        return s.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) ? s : Blocks.COBBLESTONE.defaultBlockState();
    }

    private boolean building(ServerLevel level) {
        List<HousePlan.Cell> cells = plan.cells;
        for (int n = 0; n < perTick && index < cells.size(); n++, index++) {
            HousePlan.Cell c = cells.get(index);
            BlockPos at = spot(c);
            if (HouseSite.holdsThings(level, at)) continue; // someone put a chest there while it was going up
            BlockState state = c.state().rotate(level, at, TURNS[turns]);
            if (!state.getCollisionShape(level, at).isEmpty()) liftOut(level, at);
            level.setBlock(at, state, FLAGS);
            if (index % 5 == 0) level.sendParticles(PINK, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 2, 0.3, 0.2, 0.3, 0);
        }
        if (index < cells.size() && level.getGameTime() % 6 == 0) {
            BlockPos at = spot(cells.get(index));
            level.playSound(null, at, ModSounds.HOUSE_TAP.get(), SoundSource.BLOCKS, 0.35f, 0.9f + level.random.nextFloat() * 0.25f);
        }
        return index >= cells.size();
    }

    // Anyone standing where a block is about to go rides up on it instead of ending up inside.
    private static void liftOut(ServerLevel level, BlockPos at) {
        for (Entity e : level.getEntities((Entity) null, new AABB(at), e -> !e.isSpectator())) {
            e.teleportTo(e.getX(), at.getY() + 1.01, e.getZ());
        }
    }

    BlockPos spot(HousePlan.Cell c) {
        return new BlockPos(HouseTurn.worldX(c.x(), c.z(), plan.w, plan.d, cx, turns), y + c.y(), HouseTurn.worldZ(c.x(), c.z(), plan.w, plan.d, cz, turns));
    }

    private void finished(ServerLevel level, MinecraftServer server) {
        BlockPos middle = new BlockPos(cx, y + plan.h / 2, cz);
        level.playSound(null, middle, ModSounds.HOUSE_DONE.get(), SoundSource.BLOCKS, 0.7f, 1f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cx + 0.5, y + plan.h * 0.6, cz + 0.5, 40, plan.w * 0.35, plan.h * 0.3, plan.d * 0.35, 0.02);
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) OwnPower.tell(p, "Your " + plan.name + " is finished. Doors work, rooms are empty: the rest is yours.");
    }

    // Tests only: run to the end right now.
    void finishNow(MinecraftServer server) {
        int guard = 0;
        while (!tick(server) && guard++ < 100000) { }
    }
}
