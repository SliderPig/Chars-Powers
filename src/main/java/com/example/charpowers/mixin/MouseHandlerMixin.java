package com.example.charpowers.mixin;

import com.example.charpowers.orders.client.OrderWheel;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// While the order wheel is open the mouse picks a slice instead of turning your head. Everything else stays as it was.
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;
    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void charpowers$wheel(CallbackInfo ci) {
        if (!OrderWheel.holdingMouse()) return;
        OrderWheel.pull(accumulatedDX, accumulatedDY);
        accumulatedDX = 0;
        accumulatedDY = 0;
        ci.cancel();
    }
}
