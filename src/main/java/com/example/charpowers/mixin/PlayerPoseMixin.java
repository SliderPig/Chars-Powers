package com.example.charpowers.mixin;

import com.example.charpowers.hidden.Lying;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Minecraft picks a player's pose every tick (standing, crouching, swimming...). While Play Dead or Flop
// is on, it would keep standing them back up, so the body jerked up and down. Now it leaves them lying flat.
// Runs on the server and on every player's PC, so everyone sees the same thing.
@Mixin(Player.class)
public abstract class PlayerPoseMixin {
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void charpowers$stayLyingDown(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (Lying.isLying(self)) {
            self.setPose(Pose.SWIMMING);
            ci.cancel();
        }
    }
}
