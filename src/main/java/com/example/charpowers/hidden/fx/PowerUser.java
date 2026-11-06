package com.example.charpowers.hidden.fx;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;

// Who is using a power right now, plus a little saved scratch space (`state`) just for this power on this holder,
// e.g. timers. `state` is saved with the player or mob, so it survives relogs and chunk reloads.
public record PowerUser(LivingEntity entity, @Nullable ServerPlayer player, CompoundTag state) {

    public boolean isPlayer() { return player != null; }

    public ServerLevel level() { return (ServerLevel) entity.level(); }

    @Nullable
    public Mob mob() { return entity instanceof Mob m ? m : null; }

    public long now() { return entity.level().getGameTime(); }

    // True once every `ticks` ticks (counting from when it first asks), handy for "every 10 seconds" effects.
    public boolean every(String key, int ticks) {
        long next = state.getLong(key);
        if (now() < next) return false;
        state.putLong(key, now() + ticks);
        return next != 0 || ticks <= 1;
    }

    // The same holder using a power that another power lends them (Copycat, Everything), with its own saved space.
    public PowerUser lent(String id) {
        String key = "lent:" + id;
        if (!state.contains(key)) state.put(key, new CompoundTag());
        return new PowerUser(entity, player, state.getCompound(key));
    }
}
