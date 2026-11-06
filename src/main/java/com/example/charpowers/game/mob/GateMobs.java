package com.example.charpowers.game.mob;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.SuitItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

// Registers every gate creature: its entity type, its spawn egg (in the Char's Powers creative tab) and its attributes.
// Nothing here has a natural spawn, so they only ever turn up where a gate puts them (or an egg / summon command).
public final class GateMobs {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CharPowers.MODID);
    private static final DeferredRegister<Item> EGGS = DeferredRegister.create(ForgeRegistries.ITEMS, CharPowers.MODID);
    private static final Map<GateKind, RegistryObject<? extends EntityType<?>>> BY_KIND = new EnumMap<>(GateKind.class);

    static {
        for (GateKind kind : GateKind.values()) {
            RegistryObject<? extends EntityType<?>> type;
            if (kind.boss) {
                type = TYPES.register(kind.id, () -> EntityType.Builder.<GateBoss>of((t, l) -> new GateBoss(t, l, kind), MobCategory.MONSTER)
                        .sized(kind.width, kind.height).fireImmune().clientTrackingRange(10).build(CharPowers.MODID + ":" + kind.id));
            } else {
                type = TYPES.register(kind.id, () -> EntityType.Builder.<GateMob>of((t, l) -> new GateMob(t, l, kind), MobCategory.MONSTER)
                        .sized(kind.width, kind.height).clientTrackingRange(8).build(CharPowers.MODID + ":" + kind.id));
            }
            BY_KIND.put(kind, type);
            @SuppressWarnings("unchecked")
            RegistryObject<EntityType<? extends net.minecraft.world.entity.Mob>> egg = (RegistryObject<EntityType<? extends net.minecraft.world.entity.Mob>>) type;
            EGGS.register(kind.id + "_spawn_egg", () -> new ForgeSpawnEggItem(egg, kind.eggA, kind.eggB, new Item.Properties()));
        }
    }

    private GateMobs() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        EGGS.register(modBus);
        modBus.addListener(GateMobs::attributes);
        modBus.addListener(GateMobs::tab);
    }

    public static EntityType<?> mobType(GateKind kind) {
        return BY_KIND.get(kind).get();
    }

    @SuppressWarnings("unchecked")
    static EntityType<? extends GateBoss> bossType(GateKind kind) {
        return (EntityType<? extends GateBoss>) BY_KIND.get(kind).get();
    }

    @SuppressWarnings("unchecked")
    private static void attributes(EntityAttributeCreationEvent e) {
        for (GateKind kind : GateKind.values()) {
            EntityType<? extends net.minecraft.world.entity.LivingEntity> type = (EntityType<? extends net.minecraft.world.entity.LivingEntity>) BY_KIND.get(kind).get();
            e.put(type, (kind.boss ? GateBoss.attributes(kind) : GateMob.attributes(kind)).build());
        }
    }

    private static void tab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == SuitItems.TAB.getKey() || e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            for (RegistryObject<Item> egg : EGGS.getEntries()) e.accept(egg);
        }
    }
}
