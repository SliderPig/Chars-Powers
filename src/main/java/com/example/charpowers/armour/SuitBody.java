package com.example.charpowers.armour;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.model.SuitMotion;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// What wearing the suit does to your body: it is heavy (slowness, easing with mastery) and it is loud (clunking steps, a servo whirr).
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class SuitBody {
    private static final Map<UUID, SuitMotion.Walk> WALKS = new HashMap<>();
    private static final Map<UUID, double[]> LAST = new HashMap<>();
    private static final Map<UUID, Integer> SERVO = new HashMap<>();
    private static final Set<UUID> SLOWED = new HashSet<>(), SWINGING = new HashSet<>();

    private SuitBody() {}

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        UUID id = p.getUUID();
        if (!SuitItems.fullSuit(p)) {
            if (SLOWED.remove(id)) p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            forget(id);
            return;
        }
        slow(p, id);
        servo(p, id);
        steps(p, id);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) InvincibleArmour.POWER.sendStation(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        SLOWED.remove(e.getEntity().getUUID());
        forget(e.getEntity().getUUID());
    }

    private static void forget(UUID id) {
        WALKS.remove(id);
        LAST.remove(id);
        SERVO.remove(id);
        SWINGING.remove(id);
    }

    // Slowness V at mastery 1 down to I at 100. Topped up before it runs out and with no particles or icon, so it never flickers.
    private static void slow(ServerPlayer p, UUID id) {
        MobEffectInstance now = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (SLOWED.contains(id) && now != null && now.getDuration() > 20 && p.tickCount % 10 != 0) return;
        int amplifier = SuitMotion.slownessAmplifier(Mastery.level(p, Mastery.INVINCIBLE_ARMOUR));
        if (now == null || now.getAmplifier() != amplifier || now.getDuration() <= 20 || !SLOWED.contains(id)) {
            if (now != null && SLOWED.contains(id) && now.getAmplifier() != amplifier) p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, amplifier, false, false, false));
        }
        SLOWED.add(id);
    }

    private static void steps(ServerPlayer p, UUID id) {
        double[] last = LAST.get(id);
        double moved = last == null ? 0 : Math.hypot(p.getX() - last[0], p.getZ() - last[1]);
        LAST.put(id, new double[] {p.getX(), p.getZ()});
        SuitMotion.Walk walk = WALKS.computeIfAbsent(id, k -> new SuitMotion.Walk());
        if (p.isInWater() || p.isPassenger() || p.getAbilities().flying || p.isFallFlying() || p.isSleeping() || p.isSpectator()) {
            walk.reset();
            return;
        }
        SuitMotion.Step step = walk.update(moved, p.onGround(), p.getY());
        if (step == SuitMotion.Step.STEP) {
            play(p, SuitSounds.STEP.get(), p.isCrouching() ? 0.3f : 0.8f, 0.85f + p.getRandom().nextFloat() * 0.3f);
        } else if (step == SuitMotion.Step.LAND) {
            float volume = (float) Math.max(0.5, Math.min(1.4, 0.5 + walk.drop * 0.15));
            play(p, SuitSounds.LAND.get(), volume, 0.8f + p.getRandom().nextFloat() * 0.2f);
        }
    }

    // A soft whirr each time you swing an arm.
    private static void servo(ServerPlayer p, UUID id) {
        boolean swinging = p.swinging;
        boolean started = swinging && !SWINGING.contains(id);
        if (swinging) SWINGING.add(id);
        else SWINGING.remove(id);
        if (started && p.tickCount - SERVO.getOrDefault(id, -100) >= 6) {
            SERVO.put(id, p.tickCount);
            play(p, SuitSounds.SERVO.get(), 0.5f, 0.9f + p.getRandom().nextFloat() * 0.3f);
        }
    }

    private static void play(ServerPlayer p, SoundEvent sound, float volume, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
