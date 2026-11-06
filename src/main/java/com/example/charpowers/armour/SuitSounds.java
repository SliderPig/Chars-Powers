package com.example.charpowers.armour;

import com.example.charpowers.CharPowers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// The suit's own sounds (made by tools/make_armour_sounds.py).
public final class SuitSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CharPowers.MODID);
    public static final RegistryObject<SoundEvent> STEP = make("suit_step");
    public static final RegistryObject<SoundEvent> LAND = make("suit_land");
    public static final RegistryObject<SoundEvent> SERVO = make("suit_servo");
    public static final RegistryObject<SoundEvent> CLANK = make("suit_clank");
    public static final RegistryObject<SoundEvent> THRUSTER = make("suit_thruster");

    private SuitSounds() {}

    private static RegistryObject<SoundEvent> make(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CharPowers.MODID, name)));
    }

    static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
