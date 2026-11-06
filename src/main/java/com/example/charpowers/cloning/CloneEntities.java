package com.example.charpowers.cloning;

import com.example.charpowers.CharPowers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// The Dog Pile fighter's entity type.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CloneEntities {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CharPowers.MODID);
    public static final RegistryObject<EntityType<DogPileMob>> DOG_PILE = TYPES.register("dog_pile",
            () -> EntityType.Builder.<DogPileMob>of(DogPileMob::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build("charpowers:dog_pile"));

    public static final RegistryObject<EntityType<ReflectionMob>> REFLECTION = TYPES.register("reflection",
            () -> EntityType.Builder.<ReflectionMob>of(ReflectionMob::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build("charpowers:reflection"));

    public static final RegistryObject<EntityType<com.example.charpowers.necromancy.RevivedMob>> REVIVED = TYPES.register("revived",
            () -> EntityType.Builder.<com.example.charpowers.necromancy.RevivedMob>of(com.example.charpowers.necromancy.RevivedMob::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build("charpowers:revived"));

    private CloneEntities() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(DOG_PILE.get(), DogPileMob.attributes().build());
        e.put(REFLECTION.get(), DogPileMob.attributes().build());
        e.put(REVIVED.get(), DogPileMob.attributes().build());
    }
}
