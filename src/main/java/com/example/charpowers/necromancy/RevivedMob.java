package com.example.charpowers.necromancy;

import com.example.charpowers.cloning.CloneEntities;
import com.example.charpowers.cloning.DogPileMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

// Necromancy's awakening: a dead player raised. Their skin, name and gear, none of their powers, and the gear can't drop.
// The same body as Dog Pile's fighters, but it answers to the necromancer (see Undead), not to the player it looks like.
public class RevivedMob extends DogPileMob {
    public RevivedMob(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    static RevivedMob of(ServerLevel level, UUID player, String name, ItemStack[] gear, double health) {
        RevivedMob mob = new RevivedMob(CloneEntities.REVIVED.get(), level);
        mob.become(player, name, gear, health);
        mob.forever();
        return mob;
    }

    @Override
    protected void followHost(ServerLevel server) {}
}
