package com.example.charpowers.armour.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.SuitFlight;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SuitFlightSetup {
    private SuitFlightSetup() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(SuitFlight.TYPE.get(), SuitFlightRenderer::new);
    }
}
