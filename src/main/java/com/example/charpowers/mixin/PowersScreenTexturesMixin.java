package com.example.charpowers.mixin;

import net.threetag.palladium.client.screen.power.PowersScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// Palladium's power menu keeps its layout, but reads its window, tabs and widgets pictures from our folder
// (assets/charpowers/textures/gui/powers, painted by tools/make_power_menu.py).
// remap = false because Palladium's jar is already SRG-named.
@Mixin(value = PowersScreen.class, remap = false)
public abstract class PowersScreenTexturesMixin {
    @ModifyConstant(method = "<clinit>", constant = @Constant(stringValue = "palladium"), require = 0)
    private static String charpowers$ourSheets(String original) {
        return "charpowers";
    }
}
