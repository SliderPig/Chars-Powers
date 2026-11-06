package com.example.charpowers.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

// Where a slot is drawn is fixed when it's made. The hologram inventory moves them round on the player's own PC only: they're the same real slots.
@Mixin(Slot.class)
public interface SlotAccessor {
    @Mutable
    @Accessor("x")
    void charpowers$setX(int x);

    @Mutable
    @Accessor("y")
    void charpowers$setY(int y);
}
