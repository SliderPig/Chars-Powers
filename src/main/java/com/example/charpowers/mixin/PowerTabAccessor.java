package com.example.charpowers.mixin;

import net.threetag.palladium.client.screen.power.PowerTab;
import net.threetag.palladium.client.screen.power.PowerTabType;
import net.threetag.palladium.power.IPowerHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Lets us read which power a Palladium menu tab belongs to (those fields are protected).
@Mixin(value = PowerTab.class, remap = false)
public interface PowerTabAccessor {
    @Accessor("powerHolder")
    IPowerHolder charpowers$holder();

    @Accessor("type")
    PowerTabType charpowers$type();

    @Accessor("index")
    int charpowers$index();
}
