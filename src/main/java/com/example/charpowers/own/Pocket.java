package com.example.charpowers.own;

import com.example.charpowers.CharPowers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.UUID;

// A dimension of nothing, with small rooms built in it when someone needs one (data/charpowers/dimension/pocket.json).
// Everyone has their own far-apart spot. It remembers where you came from, even across logging off.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class Pocket {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(CharPowers.MODID, "pocket"));
    private static final String BACK = "charpowers.pocket.back";
    private static final int ROOM_Y = 64, SPACING = 256;

    private Pocket() {}

    public static boolean in(Player p) {
        return p.level().dimension() == LEVEL;
    }

    private static CompoundTag root(Player p) {
        CompoundTag all = p.getPersistentData();
        if (!all.contains(Player.PERSISTED_NBT_TAG)) all.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return all.getCompound(Player.PERSISTED_NBT_TAG);
    }

    // The middle of this player's own room floor.
    public static BlockPos roomOf(UUID player) {
        int spot = player.hashCode() & 0x3FFF;
        return new BlockPos((spot % 128 - 64) * SPACING, ROOM_Y, (spot / 128 - 64) * SPACING);
    }

    // A hollow box, inside size x size x size, around the floor middle. Skipped if it's already there.
    public static void buildBox(ServerLevel level, BlockPos middle, int size, BlockState wall, BlockState floor) {
        int half = size / 2;
        if (!level.getBlockState(middle.below()).isAir()) return;
        for (int x = -half - 1; x <= half; x++) {
            for (int z = -half - 1; z <= half; z++) {
                for (int y = -1; y <= size; y++) {
                    boolean shell = x == -half - 1 || x == half || z == -half - 1 || z == half || y == -1 || y == size;
                    if (shell) level.setBlock(middle.offset(x, y, z), y == -1 ? floor : wall, 2);
                }
            }
        }
    }

    // A hollow room: half blocks each way from the middle of the floor, and this tall. Skipped if it's already there.
    public static void buildRoom(ServerLevel level, BlockPos middle, int half, int height, BlockState wall, BlockState floor) {
        if (!level.getBlockState(middle.below()).isAir()) return;
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                level.setBlock(middle.offset(x, -1, z), floor, 2);
                level.setBlock(middle.offset(x, height, z), wall, 2);
                if (x == -half || x == half || z == -half || z == half) {
                    for (int y = 0; y < height; y++) level.setBlock(middle.offset(x, y, z), wall, 2);
                }
            }
        }
    }

    @Nullable
    public static ServerLevel level(ServerPlayer p) {
        return p.getServer() == null ? null : p.getServer().getLevel(LEVEL);
    }

    // Goes to the middle of the room, remembering where you were. False if the dimension didn't load.
    public static boolean enter(ServerPlayer p, ServerLevel level, BlockPos middle) {
        if (!in(p)) {
            CompoundTag back = new CompoundTag();
            back.putString("dim", p.level().dimension().location().toString());
            back.putDouble("x", p.getX());
            back.putDouble("y", p.getY());
            back.putDouble("z", p.getZ());
            back.putFloat("yaw", p.getYRot());
            back.putFloat("pitch", p.getXRot());
            root(p).put(BACK, back);
        }
        p.teleportTo(level, middle.getX() + 0.5, middle.getY(), middle.getZ() + 0.5, p.getYRot(), 0f);
        return true;
    }

    // Back to the same spot you left from. If that world's gone, to spawn.
    public static void leave(ServerPlayer p) {
        CompoundTag root = root(p);
        CompoundTag back = root.getCompound(BACK);
        root.remove(BACK);
        ResourceLocation dim = ResourceLocation.tryParse(back.getString("dim"));
        ServerLevel to = dim == null ? null : p.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        if (to == null) {
            to = p.getServer().overworld();
            BlockPos spawn = to.getSharedSpawnPos();
            p.teleportTo(to, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, p.getYRot(), 0f);
            return;
        }
        p.teleportTo(to, back.getDouble("x"), back.getDouble("y"), back.getDouble("z"), back.getFloat("yaw"), back.getFloat("pitch"));
    }

    // Rooms can't be taken apart from inside, not even in creative.
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof Level l && l.dimension() == LEVEL) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getLevel() instanceof Level l && l.dimension() == LEVEL) e.setCanceled(true);
    }

    // There's nothing out here to land on: fall out of a room and you're simply back in the real world.
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !in(p) || p.getY() >= ROOM_Y - 12) return;
        p.fallDistance = 0;
        leave(p);
        p.fallDistance = 0;
    }

    public static BlockState wall() {
        return Blocks.BLACK_CONCRETE.defaultBlockState();
    }
}
