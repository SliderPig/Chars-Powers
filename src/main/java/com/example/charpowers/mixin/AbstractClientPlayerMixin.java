package com.example.charpowers.mixin;

import com.example.charpowers.cloning.client.CloneClient;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// A clone wears its owner's skin, arm shape and cape, using what this PC already knows about the owner.
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    private PlayerInfo charpowers$owner() {
        return CloneClient.ownerOf(((AbstractClientPlayer) (Object) this).getGameProfile().getName());
    }

    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    private void charpowers$skin(CallbackInfoReturnable<ResourceLocation> cir) {
        PlayerInfo owner = charpowers$owner();
        if (owner != null) cir.setReturnValue(owner.getSkinLocation());
    }

    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
    private void charpowers$model(CallbackInfoReturnable<String> cir) {
        PlayerInfo owner = charpowers$owner();
        if (owner != null) cir.setReturnValue(owner.getModelName());
    }

    @Inject(method = "getCloakTextureLocation", at = @At("HEAD"), cancellable = true)
    private void charpowers$cape(CallbackInfoReturnable<ResourceLocation> cir) {
        PlayerInfo owner = charpowers$owner();
        if (owner != null) cir.setReturnValue(owner.getCapeLocation());
    }
}
