package com.example.charpowers.lantern;

import com.example.charpowers.CharPowers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class LanternEntities {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CharPowers.MODID);
    public static final RegistryObject<EntityType<ConstructEntity>> CONSTRUCT = TYPES.register("construct",
            () -> EntityType.Builder.<ConstructEntity>of(ConstructEntity::new, MobCategory.MISC).sized(0.5f, 0.5f).fireImmune().noSummon().clientTrackingRange(10).updateInterval(1).build("charpowers:construct"));
    public static final RegistryObject<EntityType<BubbleEntity>> BUBBLE = TYPES.register("bubble",
            () -> EntityType.Builder.<BubbleEntity>of(BubbleEntity::new, MobCategory.MISC).sized(0.5f, 0.5f).fireImmune().noSummon().clientTrackingRange(16).updateInterval(20).build("charpowers:bubble"));

    public static final RegistryObject<EntityType<SinestroMob>> SINESTRO = TYPES.register("sinestro",
            () -> EntityType.Builder.<SinestroMob>of(SinestroMob::new, MobCategory.MONSTER).sized(0.6f, 1.95f).fireImmune().noSummon().clientTrackingRange(16).build("charpowers:sinestro"));

    private LanternEntities() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener(LanternEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(SINESTRO.get(), SinestroMob.attributes().build());
    }
}
