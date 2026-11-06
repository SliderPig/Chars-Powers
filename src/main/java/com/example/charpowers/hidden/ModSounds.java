package com.example.charpowers.hidden;

import com.example.charpowers.CharPowers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Sounds Minecraft doesn't have. For now sounds.json points each one at a stand-in Minecraft sound;
// swap the files in assets/charpowers/sounds/ later (any downloaded ones get checked first).
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CharPowers.MODID);

    public static final RegistryObject<SoundEvent> KAZOO = make("kazoo");
    public static final RegistryObject<SoundEvent> SAD_TROMBONE = make("sad_trombone");
    public static final RegistryObject<SoundEvent> DRUM_ROLL = make("drum_roll");
    public static final RegistryObject<SoundEvent> BOING = make("boing");
    public static final RegistryObject<SoundEvent> MICROWAVE_DING = make("microwave_ding");
    public static final RegistryObject<SoundEvent> SNORE = make("snore");
    public static final RegistryObject<SoundEvent> YAWN = make("yawn");
    public static final RegistryObject<SoundEvent> CRICKETS = make("crickets");
    public static final RegistryObject<SoundEvent> SEAGULL = make("seagull");
    public static final RegistryObject<SoundEvent> QUACK = make("quack");
    public static final RegistryObject<SoundEvent> CRAB_CLICK = make("crab_click");
    public static final RegistryObject<SoundEvent> CLAPPING = make("clapping");
    public static final RegistryObject<SoundEvent> BURP = make("burp");
    public static final RegistryObject<SoundEvent> SNEEZE = make("sneeze");
    public static final RegistryObject<SoundEvent> GASP = make("gasp");
    public static final RegistryObject<SoundEvent> OW = make("ow");
    public static final RegistryObject<SoundEvent> GRASS_WHISTLE = make("grass_whistle");
    public static final RegistryObject<SoundEvent> ZAP = make("zap");
    public static final RegistryObject<SoundEvent> PARTY_POPPER = make("party_popper");
    public static final RegistryObject<SoundEvent> BOO = make("boo");
    public static final RegistryObject<SoundEvent> HMPH = make("hmph");
    public static final RegistryObject<SoundEvent> BUBBLE_WRAP = make("bubble_wrap");
    public static final RegistryObject<SoundEvent> SQUEAK = make("squeak");
    public static final RegistryObject<SoundEvent> HICCUP = make("hiccup");
    public static final RegistryObject<SoundEvent> STOMACH = make("stomach");

    public static final RegistryObject<SoundEvent> HOUSE_TAP = make("house_tap");
    public static final RegistryObject<SoundEvent> HOUSE_DONE = make("house_done");

    private static RegistryObject<SoundEvent> make(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CharPowers.MODID, name)));
    }

    public static void register(IEventBus modBus) { SOUNDS.register(modBus); }

    private ModSounds() {}
}
