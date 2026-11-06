package com.example.charpowers.hidden.ranks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

// The Z rank (Everything): only ONE mob in the whole world may have it at a time (my rule).
// Which mob holds it is saved with the world (data/charpowers_z.dat), so a restart doesn't make room for a second.
// The place frees up when that mob dies or is removed for good (RankEvents).
public final class ZSlot extends SavedData {
    private UUID holder;

    private static ZSlot get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ZSlot::load, ZSlot::new, "charpowers_z");
    }

    private static ZSlot load(CompoundTag t) {
        ZSlot z = new ZSlot();
        if (t.hasUUID("holder")) z.holder = t.getUUID("holder");
        return z;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        if (holder != null) t.putUUID("holder", holder);
        return t;
    }

    // True if this mob may have Everything (nobody else has it, or it's already this mob's).
    public static boolean claim(Mob m) {
        if (m.getServer() == null) return false;
        ZSlot z = get(m.getServer());
        if (z.holder != null && !z.holder.equals(m.getUUID())) return false;
        z.holder = m.getUUID();
        z.setDirty();
        return true;
    }

    public static void free(Mob m) {
        if (m.getServer() == null) return;
        ZSlot z = get(m.getServer());
        if (m.getUUID().equals(z.holder)) {
            z.holder = null;
            z.setDirty();
        }
    }
}
