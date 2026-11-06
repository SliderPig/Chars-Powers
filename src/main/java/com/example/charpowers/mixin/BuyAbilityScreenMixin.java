package com.example.charpowers.mixin;

import com.example.charpowers.client.power.BuyPainter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.threetag.palladium.client.screen.power.BuyAbilityScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The buy box is painted by client/power/BuyPainter. m_88315_ = Screen.render, the name it has in the real game.
@Mixin(value = BuyAbilityScreen.class, remap = false)
public abstract class BuyAbilityScreenMixin {
    @Shadow private Button confirmButton;
    @Shadow @Final private Component text;

    @Inject(method = "m_88315_", at = @At("HEAD"), cancellable = true)
    private void charpowers$draw(GuiGraphics g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        BuyPainter.draw((BuyAbilityScreen) (Object) this, confirmButton, text, g, mouseX, mouseY);
        ci.cancel();
    }
}
