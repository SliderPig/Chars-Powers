package com.example.charpowers.cloning;

import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

// Cloning's awakening quest: at midnight, at mastery 100, your reflection steps out of the glass wearing your gear (or the
// best gear you've ever worn, if you took yours off to make it easier). Beat it with no clones helping and Cloning awakens.
public class ReflectionMob extends DogPileMob {
    private static final double HEALTH = 200.0, DAMAGE = 6.0, SPEED = 0.36;
    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Your reflection"), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
    private boolean spoiled;

    public ReflectionMob(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    static void stepOut(ServerPlayer host) {
        ReflectionMob mob = new ReflectionMob(CloneEntities.REFLECTION.get(), host.level());
        mob.becomeCopyOf(host, HEALTH);
        mob.forever();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (mob.getItemBySlot(slot).isEmpty()) {
                ItemStack best = CloneData.bestGear(host, slot);
                if (!best.isEmpty()) mob.setItemSlot(slot, best);
            }
        }
        mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(DAMAGE);
        mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(SPEED);
        var look = host.getLookAngle();
        mob.moveTo(host.getX() + look.x * 3, host.getY(), host.getZ() + look.z * 3, host.getYRot() + 180f, 0f);
        ServerLevel level = host.serverLevel();
        level.addFreshEntity(mob);
        mob.setTarget(host);
        mob.bar.addPlayer(host);
        level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 1, mob.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        level.playSound(null, mob.blockPosition(), PowerSounds.GLASS_SHATTER.get(), SoundSource.HOSTILE, 1.2f, 0.85f);
        host.sendSystemMessage(Component.literal("Your reflection steps out of the glass. No clones. Just you.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // It only ever wants one person.
    @Override
    @Nullable
    protected LivingEntity chooseTarget(ServerPlayer host) {
        return host;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) bar.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (!level().isClientSide && !spoiled && (attacker instanceof CloneEntity || (attacker instanceof DogPileMob && attacker != this))) {
            spoil();
            return false;
        }
        return super.hurt(source, amount);
    }

    private void spoil() {
        spoiled = true;
        for (ServerPlayer p : bar.getPlayers()) {
            p.sendSystemMessage(Component.literal("Your reflection looks at your clones, and goes back into the glass. You were meant to do this alone.").withStyle(ChatFormatting.RED));
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) return;
        for (ServerPlayer p : bar.getPlayers()) {
            if (getLastHurtByMob() != p || !p.isAlive()) continue;
            Mastery.setAwakened(p, Mastery.CLONING);
            p.sendSystemMessage(Component.literal("Cloning has awakened: up to 5 clones, and they're stronger.").withStyle(ChatFormatting.LIGHT_PURPLE));
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
            if (p.level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1, 0.6, 0.05);
        }
        bar.removeAllPlayers();
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {}
}
