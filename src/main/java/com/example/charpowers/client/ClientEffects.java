package com.example.charpowers.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.model.Loadout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

// Hidden powers that only change what YOU see on your own screen.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEffects {

    static boolean equipped(String id) {
        for (int i = 0; i < Loadout.ALWAYS_SLOTS; i++) if (id.equals(ClientHidden.always(i))) return true;
        return false;
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        // lets shared code ask "has my own player got this power on?" on this PC (movement powers, Quick Draw...)
        com.example.charpowers.hidden.HiddenRuntime.clientHas = (p, id) -> p == Minecraft.getInstance().player && ClientHidden.has(id);
        e.enqueueWork(() -> {
            // #30 Backwards Compass: compasses in YOUR hands point exactly the wrong way
            ResourceLocation angle = new ResourceLocation("angle");
            ItemPropertyFunction normal = ItemProperties.getProperty(Items.COMPASS, angle);
            if (normal == null) return;
            ClampedItemPropertyFunction flipped = (stack, level, entity, seed) -> {
                float a = normal.call(stack, level, entity, seed);
                // no entity = drawn in a menu (your inventory); a framed compass on a wall isn't yours
                boolean mine = entity == null ? !stack.isFramed() : entity == Minecraft.getInstance().player;
                return mine && equipped("backwards_compass") ? (a + 0.5f) % 1.0f : a;
            };
            ItemProperties.register(Items.COMPASS, angle, flipped);
        });
    }

    private ClientEffects() {}
}
