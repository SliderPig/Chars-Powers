package com.example.charpowers.mixin;

import net.threetag.palladium.client.screen.power.TreeAbilityWidget;
import net.threetag.palladium.client.screen.power.TreePowerTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

// What it takes to paint a power's skill tree.
@Mixin(value = TreePowerTab.class, remap = false)
public interface TreePowerTabAccessor {
    @Accessor("entries")
    List<TreeAbilityWidget> charpowers$entries();

    @Accessor("connections")
    List<TreePowerTab.Connection> charpowers$connections();

    @Accessor("scrollX")
    double charpowers$scrollX();

    @Accessor("scrollX")
    void charpowers$setScrollX(double value);

    @Accessor("scrollY")
    double charpowers$scrollY();

    @Accessor("scrollY")
    void charpowers$setScrollY(double value);

    @Accessor("centered")
    boolean charpowers$centered();

    @Accessor("centered")
    void charpowers$setCentered(boolean value);
}
