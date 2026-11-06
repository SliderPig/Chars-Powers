package com.example.charpowers.mixin;

import net.threetag.palladium.client.screen.power.TreeAbilityWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Palladium draws a skill 3 units to the right of where it checks the mouse. This lines the two up.
@Mixin(value = TreeAbilityWidget.class, remap = false)
public abstract class TreeAbilityWidgetMixin {
    @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
    private void charpowers$over(int scrollX, int scrollY, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        TreeAbilityWidget self = (TreeAbilityWidget) (Object) this;
        int x0 = scrollX + self.getX() + 3, y0 = scrollY + self.getY();
        cir.setReturnValue(mouseX >= x0 && mouseX <= x0 + 26 && mouseY >= y0 && mouseY <= y0 + 26);
    }
}
