package com.example.charpowers.mixin;

import com.example.charpowers.client.ClientHidden;
import com.example.charpowers.hidden.HiddenPowers;
import com.example.charpowers.hidden.HiddenSlotAbility;
import net.minecraft.network.chat.Component;
import net.threetag.palladium.client.screen.AbilityBarRenderer;
import net.threetag.palladium.power.ability.AbilityConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// The hidden-power keys on Palladium's ability bar show the name of the power in that slot ("Ground Slam")
// instead of "Hidden power 1". Palladium takes the name from the shared power file, so it's swapped here, on the
// player's own PC, from their own loadout. Other powers' names are left alone. require = 0: if a Palladium update
// moves this, the bar just shows "Hidden power N" again instead of crashing.
@Mixin(value = AbilityBarRenderer.class, remap = false)
public abstract class AbilityBarRendererMixin {
    @Redirect(method = "renderAbilitiesBackground", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/threetag/palladium/power/ability/AbilityConfiguration;getDisplayName()Lnet/minecraft/network/chat/Component;"))
    private static Component charpowers$hiddenName(AbilityConfiguration cfg) {
        if (cfg.getAbility() instanceof HiddenSlotAbility) {
            String id = ClientHidden.key(cfg.get(HiddenSlotAbility.SLOT));
            if (!id.isEmpty()) return HiddenPowers.name(id);
        }
        return cfg.getDisplayName();
    }
}
