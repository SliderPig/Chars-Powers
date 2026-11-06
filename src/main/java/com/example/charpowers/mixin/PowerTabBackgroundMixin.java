package com.example.charpowers.mixin;

import net.threetag.palladium.client.screen.power.ListPowerTab;
import net.threetag.palladium.client.screen.power.TreePowerTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// A power with no background of its own gets our plum tile instead of Palladium's red wool.
@Mixin(value = {ListPowerTab.class, TreePowerTab.class}, remap = false)
public abstract class PowerTabBackgroundMixin {
    @ModifyConstant(method = "drawContents", constant = @Constant(stringValue = "textures/block/red_wool.png"), require = 0)
    private String charpowers$plumBackground(String original) {
        return "charpowers:textures/gui/powers/background.png";
    }
}
