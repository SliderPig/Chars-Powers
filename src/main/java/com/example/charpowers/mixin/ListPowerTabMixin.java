package com.example.charpowers.mixin;

import com.example.charpowers.client.power.ListPainter;
import net.minecraft.client.gui.GuiGraphics;
import net.threetag.palladium.client.screen.power.ListPowerTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// List-style powers are painted by client/power/ListPainter.
@Mixin(value = ListPowerTab.class, remap = false)
public abstract class ListPowerTabMixin {
    @Shadow private ListPowerTab.AbilityList list;

    @Inject(method = "drawContents", at = @At("HEAD"), cancellable = true)
    private void charpowers$draw(GuiGraphics g, int x, int y, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        ListPainter.draw((ListPowerTab) (Object) this, list, g, x, y, mouseX, mouseY, partial);
        ci.cancel();
    }

    @Inject(method = "drawTooltips", at = @At("HEAD"), cancellable = true)
    private void charpowers$tooltips(GuiGraphics g, int mouseX, int mouseY, int x, int y, float partial, boolean overlay, CallbackInfo ci) {
        ListPainter.tooltips((ListPowerTab) (Object) this, g, overlay);
        ci.cancel();
    }
}
