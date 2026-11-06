package com.example.charpowers.cloning.jobs;

import com.example.charpowers.cloning.CloneEntity;
import net.minecraft.world.item.ItemStack;

// What a clone is doing right now. Each job is its own class, so Mine, Chop and Cook can be filled in
// one at a time without touching the fighting code.
public interface CloneJob {
    String id();

    default void start(CloneEntity c) {}

    void tick(CloneEntity c);

    default void stop(CloneEntity c) {
        c.mover.stop();
    }

    // true = it's working with tools, so don't swap to the sword every few seconds
    default boolean usesTools() {
        return false;
    }

    // true = don't pick this item up
    default boolean refuses(ItemStack stack) {
        return false;
    }

    // Shown in the clones menu.
    default String describe(CloneEntity c) {
        return id().substring(0, 1).toUpperCase() + id().substring(1);
    }

    // true = the only kind of job a rogue clone will take
    default boolean lockedIn() {
        return false;
    }
}
