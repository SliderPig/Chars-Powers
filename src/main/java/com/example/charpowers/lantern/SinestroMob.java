package com.example.charpowers.lantern;

import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.lantern.model.LanternRules;
import com.example.charpowers.own.OwnPower;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

// Green Lantern's quest: your mortal enemy, with yellow versions of your own constructs. Only constructs hurt him. Beat him and
// he kneels, and you have a minute to say your oath over him. If your battery hits 0 he laughs and comes back another night.
public class SinestroMob extends Monster {
    private static final DustParticleOptions YELLOW = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.15f), 1f);
    private static final int KNEEL_TICKS = 1200, GUN_TICKS = 40;

    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Sinestro"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private UUID enemyId;
    private ConstructEntity gun;
    private int cooldown = 60, gunLeft, kneelLeft, next;
    private boolean beaten;

    public SinestroMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, LanternRules.SINESTRO_HEALTH).add(Attributes.FOLLOW_RANGE, 64).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    static SinestroMob comeFor(ServerPlayer p) {
        SinestroMob s = new SinestroMob(LanternEntities.SINESTRO.get(), p.level());
        s.enemyId = p.getUUID();
        Vec3 from = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(16)).add(0, 8, 0);
        s.moveTo(from.x, from.y, from.z, p.getYRot() + 180f, 0f);
        s.setHealth(s.getMaxHealth());
        p.level().addFreshEntity(s);
        s.bar.addPlayer(p);
        p.level().playSound(null, s.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 1.4f);
        OwnPower.tell(p, "A yellow light comes down out of the night. Sinestro has come for you. Only your constructs can hurt him.");
        return s;
    }

    public boolean isBeaten() {
        return beaten;
    }

    private ServerPlayer enemy() {
        return getServer() == null ? null : getServer().getPlayerList().getPlayer(enemyId);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerPlayer enemy = enemy();
        if (enemy == null || !enemy.isAlive() || enemy.level() != level() || distanceTo(enemy) > 96) {
            leave("Sinestro loses interest in you, for tonight.");
            return;
        }
        bar.setProgress(getHealth() / getMaxHealth());
        getLookControl().setLookAt(enemy);
        if (beaten) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (--kneelLeft <= 0) leave("Sinestro gets back up. You never said your oath. He'll be back.");
            return;
        }
        hover(enemy);
        if (gun != null) fireGun(enemy);
        else if (--cooldown <= 0) attack(enemy);
    }

    // He hangs in the air a little way off and circles.
    private void hover(ServerPlayer enemy) {
        double angle = tickCount * 0.02;
        Vec3 spot = enemy.position().add(Math.cos(angle) * 10, 5, Math.sin(angle) * 10);
        Vec3 way = spot.subtract(position());
        double far = way.length();
        setDeltaMovement(far < 0.3 ? Vec3.ZERO : way.normalize().scale(Math.min(0.35, far * 0.1)));
        Vec3 to = enemy.getEyePosition().subtract(getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    private ConstructEntity make(String kind, String animation, Vec3 at, float yaw, int life) {
        ConstructEntity c = ConstructEntity.make(level(), kind, animation, at.x, at.y, at.z, yaw, life);
        c.yellow();
        return c;
    }

    private void hit(LivingEntity target, float damage, Vec3 push) {
        if (!target.isAlive() || !isAlive() || beaten) return;
        target.hurt(damageSources().mobAttack(this), damage);
        target.setDeltaMovement(push);
        target.hurtMarked = true;
    }

    // One after another, so you learn them: the same things you can do, in yellow.
    private void attack(ServerPlayer enemy) {
        cooldown = LanternRules.SINESTRO_GAP;
        Vec3 spot = enemy.position();
        Vec3 side = enemy.getLookAngle().cross(new Vec3(0, 1, 0)).normalize();
        switch (next++ % 5) {
            case 0 -> {
                Fx.particles((ServerLevel) level(), YELLOW, spot.add(0, 0.1, 0), 20, 1.2);
                make("fist", "Emerge", spot, getYRot(), LanternRules.FIST_LIFE).at(LanternRules.FIST_HIT, () -> {
                    if (enemy.position().distanceToSqr(spot) < 9) hit(enemy, LanternRules.SINESTRO_DAMAGE, new Vec3(0, 1.2, 0));
                });
            }
            case 1 -> make("slam", "slam", spot.add(0, 3, 0), getYRot(), LanternRules.SLAM_LIFE).at(LanternRules.SLAM_HIT, () -> {
                if (enemy.position().distanceToSqr(spot) < 9) hit(enemy, LanternRules.SINESTRO_DAMAGE, new Vec3(0, -1, 0));
            });
            case 2 -> make("slap", "slap", spot.add(side.scale(-2)), getYRot(), LanternRules.SLAP_LIFE).at(LanternRules.SLAP_HIT, () -> {
                if (enemy.position().distanceToSqr(spot) < 9) hit(enemy, LanternRules.SINESTRO_DAMAGE, side.scale(2).add(0, 0.4, 0));
            });
            case 3 -> {
                for (int i = 0; i < 6; i++) {
                    double[] fan = LanternRules.fan(i, 6);
                    ConstructEntity dagger = make("dagger", "form", position().add(fan[0], fan[1], 0), getYRot(), 0);
                    dagger.at(10 + i * 2, () -> dagger.launch(enemy.getEyePosition().subtract(dagger.position()).normalize().scale(1.4), this, t -> hit(t, LanternRules.SINESTRO_DAMAGE / 3, Vec3.ZERO)));
                }
            }
            default -> {
                gun = make("minigun", "Summon", gunSpot(), getYRot(), 0);
                gunLeft = GUN_TICKS;
                gun.at(6, () -> {
                    if (gun != null) gun.play("Shooting");
                });
            }
        }
    }

    private Vec3 gunSpot() {
        return getEyePosition().add(getLookAngle().scale(1.4)).add(0, -1.45, 0);
    }

    private void fireGun(ServerPlayer enemy) {
        Vec3 at = gunSpot();
        gun.setPos(at.x, at.y, at.z);
        gun.setYRot(getYRot());
        if (gunLeft-- % 2 == 0 && gunLeft < GUN_TICKS - 6) {
            Vec3 from = getEyePosition(), to = enemy.getEyePosition();
            if (level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                enemy.invulnerableTime = 0;
                enemy.hurt(damageSources().mobAttack(this), 1f);
                Vec3 way = to.subtract(from).normalize();
                double length = to.distanceTo(from);
                for (double t = 2; t < length; t += 4) Fx.particles((ServerLevel) level(), YELLOW, from.add(way.scale(t)), 1, 0);
            }
        }
        if (gunLeft <= 0) {
            gun.play("Unsummon");
            gun.endIn(4);
            gun = null;
        }
    }

    // Only a construct hurts him. When he should die he kneels instead, waiting for your oath.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (beaten) return false;
        if (!GreenLantern.fromConstruct()) {
            if (source.getEntity() instanceof ServerPlayer p) OwnPower.whisper(p, "That goes straight through him. Only your constructs can hurt him.");
            return false;
        }
        if (amount >= getHealth()) {
            setHealth(1f);
            kneel();
            return true;
        }
        return super.hurt(source, amount);
    }

    private void kneel() {
        beaten = true;
        kneelLeft = KNEEL_TICKS;
        setNoGravity(false);
        setPose(Pose.CROUCHING);
        if (gun != null) {
            gun.discard();
            gun = null;
        }
        level().playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1f, 0.5f);
        ServerPlayer enemy = enemy();
        if (enemy != null) OwnPower.tell(enemy, "Sinestro falls to his knees. Say your oath over him.");
    }

    void batteryGone() {
        if (!beaten) leave("Your ring goes dark. Sinestro laughs, and leaves you there. He'll be back another night.");
    }

    void leave(String message) {
        ServerPlayer enemy = enemy();
        if (enemy != null) OwnPower.tell(enemy, message);
        if (level() instanceof ServerLevel server) Fx.particles(server, YELLOW, position().add(0, 1, 0), 40, 0.8);
        GreenLantern.fightOver(enemyId);
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        if (gun != null) gun.discard();
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }
}
