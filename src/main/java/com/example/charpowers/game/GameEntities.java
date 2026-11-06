package com.example.charpowers.game;

import com.example.charpowers.CharPowers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GameEntities {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CharPowers.MODID);
    public static final RegistryObject<EntityType<GameBossMob>> BOSS = TYPES.register("game_boss",
            () -> EntityType.Builder.<GameBossMob>of(GameBossMob::new, MobCategory.MONSTER).sized(0.6f, 1.95f).fireImmune().noSummon().clientTrackingRange(10).build("charpowers:game_boss"));

    private GameEntities() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener(GameEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(BOSS.get(), GameBossMob.attributes().build());
    }
}
