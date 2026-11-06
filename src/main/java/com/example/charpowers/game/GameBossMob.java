package com.example.charpowers.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

// Game Player's last quest: a boss only you can see. Nobody else's PC is ever told it exists. It blinks behind you now and then.
public class GameBossMob extends Monster {
    private static final int BLINK_EVERY = 120;
    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("FINAL BOSS"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private UUID ownerId;
    private boolean gateBoss;

    public GameBossMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 300).add(Attributes.ATTACK_DAMAGE, 11).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FOLLOW_RANGE, 64).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    static GameBossMob spawn(ServerPlayer owner, ServerLevel level, Vec3 at) {
        GameBossMob boss = new GameBossMob(GameEntities.BOSS.get(), level);
        boss.ownerId = owner.getUUID();
        boss.moveTo(at.x, at.y, at.z, 0f, 0f);
        boss.setHealth(boss.getMaxHealth());
        level.addFreshEntity(boss);
        boss.bar.addPlayer(owner);
        return boss;
    }

    // A gate's boss: stronger the higher the rank.
    static GameBossMob spawnForGate(ServerPlayer owner, ServerLevel level, Vec3 at, int rank) {
        GameBossMob boss = spawn(owner, level, at);
        boss.gateBoss = true;
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(com.example.charpowers.game.model.GateRules.bossHealth(rank));
        boss.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(com.example.charpowers.game.model.GateRules.bossDamage(rank));
        boss.setHealth(boss.getMaxHealth());
        boss.bar.setName(Component.literal("RANK " + com.example.charpowers.game.model.GateRules.RANKS[rank] + " BOSS"));
        return boss;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
    }

    private ServerPlayer owner() {
        return getServer() == null || ownerId == null ? null : getServer().getPlayerList().getPlayer(ownerId);
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer other) {
        return other.getUUID().equals(ownerId);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerPlayer owner = owner();
        if (owner == null || !owner.isAlive() || owner.level() != level() || distanceTo(owner) > 64) {
            discard();
            return;
        }
        setTarget(owner);
        bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % BLINK_EVERY == 0) {
            Vec3 back = owner.position().subtract(owner.getLookAngle().multiply(1, 0, 1).normalize().scale(3));
            randomTeleport(back.x, owner.getY(), back.z, false);
        }
    }

    // Only the one who can see it can hurt it.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && ownerId != null && source.getEntity() != null && !ownerId.equals(source.getEntity().getUUID())) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        ServerPlayer owner = owner();
        if (level().isClientSide || owner == null) return;
        if (gateBoss) GamePlayer.POWER.gateCleared(owner);
        else GamePlayer.POWER.bossBeaten(owner);
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
