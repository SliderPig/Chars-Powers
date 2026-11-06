package com.example.charpowers.mixin;

import com.example.charpowers.client.ClientHidden;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// #210 Soul Sight: other players within 16 blocks get an outline through walls, only on your screen.
@Mixin(Minecraft.class)
public abstract class MinecraftGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void charpowers$soulSight(Entity e, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (e instanceof net.minecraft.world.entity.Mob && mc.player != null && e.distanceTo(mc.player) <= com.example.charpowers.armour.model.SuitRules.SCANNER_RANGE
                && com.example.charpowers.armour.InvincibleArmour.POWER.scannerOn(mc.player)) {
            cir.setReturnValue(true);
            return;
        }
        if (e instanceof Player && e != mc.player && mc.player != null && e.distanceTo(mc.player) <= 16 && ClientHidden.has("soul_sight")) {
            cir.setReturnValue(true);
        }
    }
}
