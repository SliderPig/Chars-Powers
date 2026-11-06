package com.example.charpowers.mastery;

import com.example.charpowers.CharPowers;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.ResourceLocationProperty;
import net.threetag.palladiumcore.registry.DeferredRegister;
import net.threetag.palladiumcore.registry.RegistrySupplier;

// "type": "charpowers:mastery_level" in a power file: true once that power's mastery has reached the level.
// Put it under "unlocking" to lock an ability until then.
public class MasteryCondition extends Condition {
    public static final DeferredRegister<ConditionSerializer> CONDITIONS = DeferredRegister.create(CharPowers.MODID, ConditionSerializer.REGISTRY);
    public static final RegistrySupplier<ConditionSerializer> MASTERY_LEVEL = CONDITIONS.register("mastery_level", Serializer::new);

    public static final RegistrySupplier<ConditionSerializer> AWAKENED = CONDITIONS.register("awakened", AwakenedCondition.Serializer::new);

    private final ResourceLocation power;
    private final int level;

    public MasteryCondition(ResourceLocation power, int level) {
        this.power = power;
        this.level = level;
    }

    public int level() {
        return level;
    }

    @Override
    public boolean active(DataContext context) {
        LivingEntity e = context.getLivingEntity();
        return e instanceof Player p && Mastery.level(p, power) >= level;
    }

    @Override
    public ConditionSerializer getSerializer() {
        return MASTERY_LEVEL.get();
    }

    public static class Serializer extends ConditionSerializer {
        public static final PalladiumProperty<ResourceLocation> POWER = new ResourceLocationProperty("power").configurable("The power whose mastery is checked");
        public static final PalladiumProperty<Integer> LEVEL = new IntegerProperty("level").configurable("The mastery level needed (1-100)");

        public Serializer() {
            this.withProperty(POWER, Mastery.CLONING);
            this.withProperty(LEVEL, 1);
        }

        @Override
        public Condition make(JsonObject json) {
            return new MasteryCondition(this.getProperty(json, POWER), this.getProperty(json, LEVEL));
        }
    }
}
