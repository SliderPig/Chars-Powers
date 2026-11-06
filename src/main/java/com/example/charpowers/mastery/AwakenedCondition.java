package com.example.charpowers.mastery;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.ResourceLocationProperty;

// "charpowers:awakened" in a power file: this ability stays locked until the power's quest is done.
public class AwakenedCondition extends Condition {
    private final ResourceLocation power;

    public AwakenedCondition(ResourceLocation power) {
        this.power = power;
    }

    @Override
    public boolean active(DataContext context) {
        LivingEntity e = context.getLivingEntity();
        return e instanceof Player p && Mastery.isAwakened(p, power);
    }

    @Override
    public ConditionSerializer getSerializer() {
        return MasteryCondition.AWAKENED.get();
    }

    public static class Serializer extends ConditionSerializer {
        public static final PalladiumProperty<ResourceLocation> POWER = new ResourceLocationProperty("power").configurable("The power whose quest must be done");

        public Serializer() {
            this.withProperty(POWER, Mastery.CLONING);
        }

        @Override
        public Condition make(JsonObject json) {
            return new AwakenedCondition(this.getProperty(json, POWER));
        }
    }
}
