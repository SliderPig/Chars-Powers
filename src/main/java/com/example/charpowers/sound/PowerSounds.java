package com.example.charpowers.sound;

import com.example.charpowers.CharPowers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// The main powers' own sounds. All made by tools/make_power_sounds.py, listed in sounds.json as powers/<name>.
public final class PowerSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CharPowers.MODID);

    public static final RegistryObject<SoundEvent> TIME_SLOW = make("time_slow");
    public static final RegistryObject<SoundEvent> TIME_TICK = make("time_tick");
    public static final RegistryObject<SoundEvent> TIME_STOP = make("time_stop");
    public static final RegistryObject<SoundEvent> TIME_RUSH = make("time_rush");
    public static final RegistryObject<SoundEvent> MIRROR_SHIMMER = make("mirror_shimmer");
    public static final RegistryObject<SoundEvent> GLASS_SHATTER = make("glass_shatter");
    public static final RegistryObject<SoundEvent> DARK_SUCK = make("dark_suck");
    public static final RegistryObject<SoundEvent> SOUL_PUNCH = make("soul_punch");
    public static final RegistryObject<SoundEvent> RAISE_RUMBLE = make("raise_rumble");
    public static final RegistryObject<SoundEvent> HOLO_OPEN = make("holo_open");
    public static final RegistryObject<SoundEvent> HOLO_CLOSE = make("holo_close");
    public static final RegistryObject<SoundEvent> HOLO_PING = make("holo_ping");
    public static final RegistryObject<SoundEvent> GATE_OPEN = make("gate_open");
    public static final RegistryObject<SoundEvent> LEVEL_UP = make("level_up");
    public static final RegistryObject<SoundEvent> CLAIM_CHIRP = make("claim_chirp");
    public static final RegistryObject<SoundEvent> ORDER_CHIRP = make("order_chirp");
    public static final RegistryObject<SoundEvent> COMMAND_BOOM = make("command_boom");
    public static final RegistryObject<SoundEvent> WHISPER_SHIMMER = make("whisper_shimmer");
    public static final RegistryObject<SoundEvent> WORD_THUNK = make("word_thunk");
    public static final RegistryObject<SoundEvent> ROCK_CRUNCH = make("rock_crunch");
    public static final RegistryObject<SoundEvent> ULTIMINE_SWEEP = make("ultimine_sweep");
    public static final RegistryObject<SoundEvent> PAGE_FLIP = make("page_flip");
    public static final RegistryObject<SoundEvent> INK_SCRATCH = make("ink_scratch");
    public static final RegistryObject<SoundEvent> PAGES_REWIND = make("pages_rewind");
    public static final RegistryObject<SoundEvent> CHALK_TAP = make("chalk_tap");

    private static RegistryObject<SoundEvent> make(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CharPowers.MODID, name)));
    }

    // Heard only by this player, wherever they are.
    public static void toPlayer(ServerPlayer p, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        p.playNotifySound(sound.get(), SoundSource.PLAYERS, volume, pitch);
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    private PowerSounds() {}
}
