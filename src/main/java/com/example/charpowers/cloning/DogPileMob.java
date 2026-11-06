package com.example.charpowers.cloning;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

// One of Dog Pile's 20 fighters (and Escaped Death's stand-in): a light mob that looks like the player.
// A real player body each would be far too heavy for the server. Never saved, so a restart can't leave any behind.
public class DogPileMob extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> HOST = SynchedEntityData.defineId(DogPileMob.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final int RECENT_TICKS = 200;
    private static final double RANGE = 20;
    private int life = 1200;

    public DogPileMob(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // A copy of the player: their name and skin, and copies of their gear that can't be dropped.
    static DogPileMob copyOf(ServerPlayer host, double maxHealth) {
        DogPileMob mob = new DogPileMob(CloneEntities.DOG_PILE.get(), host.level());
        mob.becomeCopyOf(host, maxHealth);
        return mob;
    }

    protected void becomeCopyOf(ServerPlayer host, double maxHealth) {
        ItemStack[] gear = new ItemStack[EquipmentSlot.values().length];
        for (EquipmentSlot slot : EquipmentSlot.values()) gear[slot.ordinal()] = host.getItemBySlot(slot).copy();
        become(host.getUUID(), host.getGameProfile().getName(), gear, maxHealth);
    }

    protected void become(UUID who, String name, ItemStack[] gear, double maxHealth) {
        entityData.set(HOST, Optional.of(who));
        setCustomName(Component.literal(name));
        setCustomNameVisible(true);
        setPersistenceRequired();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setItemSlot(slot, gear[slot.ordinal()]);
            setDropChance(slot, 0f);
        }
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        setHealth((float) maxHealth);
    }

    protected void forever() {
        life = Integer.MAX_VALUE;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HOST, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8f));
    }

    @Nullable
    public UUID hostId() {
        return entityData.get(HOST).orElse(null);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (--life <= 0) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 8, 0.3, 0.5, 0.3, 0.02);
            discard();
            return;
        }
        followHost(server);
    }

    // Stays with the player it's a copy of, and goes when they do.
    protected void followHost(ServerLevel server) {
        if (tickCount % 10 != 0) return;
        UUID id = hostId();
        ServerPlayer host = id == null ? null : server.getServer().getPlayerList().getPlayer(id);
        if (host == null || !host.isAlive() || host.level() != level() || !net.threetag.palladium.power.SuperpowerUtil.hasSuperpower(host, com.example.charpowers.mastery.Mastery.CLONING)) {
            discard();
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || (target != host && !isFoe(host, target))) setTarget(chooseTarget(host));
        if (getTarget() == null && distanceToSqr(host) > 64) getNavigation().moveTo(host, 1.2);
    }

    private boolean isFoe(ServerPlayer host, LivingEntity e) {
        if (e == host || e == this || !e.isAlive() || e.isSpectator() || e.isAlliedTo(host)) return false;
        if (e instanceof DogPileMob mob && host.getUUID().equals(mob.hostId())) return false;
        return !(e instanceof CloneEntity clone && clone.hostId.equals(host.getUUID()) && !clone.rogue);
    }

    @Nullable
    protected LivingEntity chooseTarget(ServerPlayer host) {
        LivingEntity attacker = host.getLastHurtByMob();
        if (attacker != null && host.tickCount - host.getLastHurtByMobTimestamp() <= RECENT_TICKS && isFoe(host, attacker)) return attacker;
        return level().getEntitiesOfClass(LivingEntity.class, host.getBoundingBox().inflate(RANGE), e -> e instanceof Enemy && isFoe(host, e))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldDropExperience() {
        return false;
    }
}
