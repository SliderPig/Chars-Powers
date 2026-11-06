package com.example.charpowers.mixin;

import com.example.charpowers.client.anim.PlayerAnims;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// While a pose shows your real arms in first person, the stand-in hand and what it holds sink out of view the way they do when you
// change items.
@Mixin(ItemInHandRenderer.class)
public abstract class HandsAwayMixin {
    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float charpowers$sinkHands(float equip) {
        return Math.max(equip, PlayerAnims.handsAway(Minecraft.getInstance().getFrameTime()));
    }
}
