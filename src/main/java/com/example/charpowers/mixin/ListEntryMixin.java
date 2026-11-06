package com.example.charpowers.mixin;

import com.example.charpowers.client.power.ListPainter;
import net.minecraft.client.gui.GuiGraphics;
import net.threetag.palladium.client.screen.power.ListPowerTab;
import net.threetag.palladium.power.ability.AbilityInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// One row of a list-style power. m_6311_ = the row's render, the name it has in the real game.
@Mixin(value = ListPowerTab.ListEntry.class, remap = false)
public abstract class ListEntryMixin {
    @Shadow @Final private AbilityInstance abilityInstance;

    @Inject(method = "m_6311_", at = @At("HEAD"), cancellable = true)
    private void charpowers$draw(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partial, CallbackInfo ci) {
        ListPainter.row(abilityInstance, g, top, left, width, height, hovered);
        ci.cancel();
    }
}
