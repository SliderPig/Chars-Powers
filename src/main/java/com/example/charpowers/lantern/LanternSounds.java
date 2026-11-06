package com.example.charpowers.lantern;

import com.example.charpowers.CharPowers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

// The hard light sounds, made by tools/make_lantern_sounds.py. Registered here so no shared file has to change.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LanternSounds {
    public static final SoundEvent FORM = event("construct_form"), SWING = event("hand_swing"), THUMP = event("impact_thump"), DISSOLVE = event("construct_dissolve");

    private LanternSounds() {}

    private static SoundEvent event(String name) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(CharPowers.MODID, name));
    }

    @SubscribeEvent
    public static void register(RegisterEvent e) {
        e.register(ForgeRegistries.Keys.SOUND_EVENTS, helper -> {
            for (SoundEvent s : new SoundEvent[] {FORM, SWING, THUMP, DISSOLVE}) helper.register(s.getLocation(), s);
        });
    }

    public static void play(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        if (!level.isClientSide) level.playSound(null, BlockPos.containing(at), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
