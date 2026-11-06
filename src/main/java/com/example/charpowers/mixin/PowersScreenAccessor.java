package com.example.charpowers.mixin;

import net.threetag.palladium.client.screen.power.PowerTab;
import net.threetag.palladium.client.screen.power.PowersScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

// The power menu's list of tabs.
@Mixin(value = PowersScreen.class, remap = false)
public interface PowersScreenAccessor {
    @Accessor("tabs")
    List<PowerTab> charpowers$tabs();
}
