package com.example.charpowers.cloning;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.HiddenSlotAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladiumcore.registry.RegistrySupplier;

// Cloning's Palladium abilities. "passive" does nothing itself: it's for the abilities that are just a line on the
// power screen until their mastery level unlocks them (the code checks mastery directly).
public final class Cloning {
    // on every clone before it joins the server, so anything that loops over players can skip it
    public static final String TAG = "charpowers.clone";

    public static final RegistrySupplier<Ability> PASSIVE = HiddenSlotAbility.ABILITIES.register("passive", Ability::new);
    public static final RegistrySupplier<Ability> CLONE = HiddenSlotAbility.ABILITIES.register("clone", CloneAbility::new);
    public static final RegistrySupplier<Ability> DOG_PILE = HiddenSlotAbility.ABILITIES.register("dog_pile", DogPileAbility::new);

    public static final ResourceLocation POWER = new ResourceLocation(CharPowers.MODID, "cloning");

    private Cloning() {}

    // Called from CharPowers so the fields above are registered before Palladium's registry opens.
    public static void init() {}

    public static class CloneAbility extends Ability {
        @Override
        public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
            if (enabled && entity instanceof ServerPlayer p && !(p instanceof CloneEntity)) CloneManager.pressSummon(p);
        }
    }

    public static class DogPileAbility extends Ability {
        @Override
        public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
            if (enabled && entity instanceof ServerPlayer p && !(p instanceof CloneEntity)) DogPile.start(p);
        }
    }
}
