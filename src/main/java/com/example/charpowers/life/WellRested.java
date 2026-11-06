package com.example.charpowers.life;

import com.example.charpowers.CharPowers;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// The buff from sleeping: a bit quicker on your feet and a slow heal.
public final class WellRested extends MobEffect {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, CharPowers.MODID);
    public static final RegistryObject<MobEffect> EFFECT = EFFECTS.register("well_rested", WellRested::new);

    private WellRested() {
        super(MobEffectCategory.BENEFICIAL, 0xFF88C8);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "d9f6ab11-52c7-4f33-8d0a-9e2c1b7a6f33", 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 100 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() < entity.getMaxHealth()) entity.heal(1f);
    }
}
