package com.example.charpowers.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

// Clones don't count when the game works out if enough players are asleep to skip the night.
// require = 0: if Arclight ever changes this method, clones just count again instead of the server crashing.
@Mixin(SleepStatus.class)
public abstract class SleepStatusMixin {
    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, require = 0)
    private List<ServerPlayer> charpowers$withoutClones(List<ServerPlayer> players) {
        return players.stream().filter(p -> !p.getTags().contains("charpowers.clone")).toList();
    }
}
