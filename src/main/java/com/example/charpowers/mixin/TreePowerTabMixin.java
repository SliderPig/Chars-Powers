package com.example.charpowers.mixin;

import com.example.charpowers.client.power.TreePainter;
import net.minecraft.client.gui.GuiGraphics;
import net.threetag.palladium.client.screen.power.TreePowerTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Skill trees are painted by client/power/TreePainter.
@Mixin(value = TreePowerTab.class, remap = false)
public abstract class TreePowerTabMixin {
    @Inject(method = "drawContents", at = @At("HEAD"), cancellable = true)
    private void charpowers$draw(GuiGraphics g, int x, int y, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        TreePainter.draw((TreePowerTab) (Object) this, g, x, y, mouseX, mouseY, partial);
        ci.cancel();
    }

    @Inject(method = "drawTooltips", at = @At("HEAD"), cancellable = true)
    private void charpowers$tooltips(GuiGraphics g, int mouseX, int mouseY, int x, int y, float partial, boolean overlay, CallbackInfo ci) {
        TreePainter.tooltips((TreePowerTab) (Object) this, g, mouseX, mouseY, x, y, overlay);
        ci.cancel();
    }
}
