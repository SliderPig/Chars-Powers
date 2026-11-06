package com.example.charpowers.mixin;

import com.example.charpowers.hidden.HiddenRuntime;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// #108 Heavy Boots: water currents can't push you. Minecraft asks this every tick, on the server and on your PC.
@Mixin(Player.class)
public abstract class PlayerFluidMixin {
    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void charpowers$heavyBoots(CallbackInfoReturnable<Boolean> cir) {
        if (HiddenRuntime.has((Player) (Object) this, "heavy_boots")) cir.setReturnValue(false);
    }
}
