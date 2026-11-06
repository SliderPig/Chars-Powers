package com.example.charpowers.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.UUID;

// There's only ever one Player in a world. Once there is one, nobody else's game even looks for a gate.
final class GameOwner extends SavedData {
    @Nullable
    UUID owner;

    static GameOwner of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(GameOwner::load, GameOwner::new, "charpowers_game_owner");
    }

    private static GameOwner load(CompoundTag tag) {
        GameOwner o = new GameOwner();
        if (tag.hasUUID("owner")) o.owner = tag.getUUID("owner");
        return o;
    }

    void set(@Nullable UUID player) {
        owner = player;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (owner != null) tag.putUUID("owner", owner);
        return tag;
    }
}
