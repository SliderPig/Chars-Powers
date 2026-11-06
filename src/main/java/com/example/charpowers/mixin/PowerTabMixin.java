package com.example.charpowers.mixin;

import com.example.charpowers.client.power.PowerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.threetag.palladium.client.screen.power.PowerTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Tabs live in our rail above the card now, not round all four sides of the window.
@Mixin(value = PowerTab.class, remap = false)
public abstract class PowerTabMixin {
    @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
    private void charpowers$over(int x, int y, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        PowerTab self = (PowerTab) (Object) this;
        cir.setReturnValue(PowerMenu.overTab(self.getScreen(), self, x, y, mouseX, mouseY));
    }

    @Inject(method = "drawTab", at = @At("HEAD"), cancellable = true)
    private void charpowers$tab(GuiGraphics g, int x, int y, boolean selected, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "drawIcon", at = @At("HEAD"), cancellable = true)
    private void charpowers$icon(GuiGraphics g, int x, int y, CallbackInfo ci) {
        ci.cancel();
    }
}
