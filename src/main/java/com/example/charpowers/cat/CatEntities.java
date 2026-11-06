package com.example.charpowers.cat;

import com.example.charpowers.CharPowers;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// The cat's entity type. It is the vanilla cat in every way that shows (model, animations, sounds), so there is nothing to draw.
public final class CatEntities {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CharPowers.MODID);
    public static final RegistryObject<EntityType<CompanionCat>> CAT = TYPES.register("companion_cat",
            () -> EntityType.Builder.<CompanionCat>of(CompanionCat::new, MobCategory.MISC).sized(0.6f, 0.7f).clientTrackingRange(8).updateInterval(3).build("companion_cat"));

    private CatEntities() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        private Attributes() {}

        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent e) {
            e.put(CAT.get(), Cat.createAttributes().build());
        }
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Renderers {
        private Renderers() {}

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(CAT.get(), CatRenderer::new);
        }
    }
}
