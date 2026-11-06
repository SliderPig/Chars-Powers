package com.example.charpowers.items;

import com.example.charpowers.CharPowers;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// A shard of ice from the Ice Staff. It hurts, slows hard, puts frost on the target and stops ordinary mobs dead for a moment (bosses barely).
// The Mod.EventBusSubscriber part lets frozen mobs go again.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public class IceShard extends Projectile {
    private static final float DAMAGE = 7f;
    private static final int LIFE = 80, FROST = 220, SLOW_TICKS = 80, STOP_TICKS = 30, BOSS_STOP_TICKS = 7;
    private static final String HELD = "charpowers.iced";
    private static final Map<Mob, Long> STOPPED = new HashMap<>();

    private int power;

    public IceShard(EntityType<? extends IceShard> type, Level level) {
        super(type, level);
    }

    public IceShard(Level level, LivingEntity owner) {
        super(CharItems.ICE_SHARD.get(), level);
        setOwner(owner);
    }

    public void setPower(int power) {
        this.power = power;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) onHit(hit);
        if (isRemoved()) return;
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        setDeltaMovement(v.scale(0.995).add(0, -0.012, 0));
        updateRotation();
        if (!level().isClientSide && tickCount > LIFE) shatter();
        if (level().isClientSide && tickCount % 2 == 0) level().addParticle(ParticleTypes.SNOWFLAKE, true, getX(), getY(), getZ(), 0, 0, 0);
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e)) return false;
        return !(getOwner() instanceof LivingEntity owner) || !(e instanceof LivingEntity l) || Foes.canHit(owner, l);
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        super.onHitEntity(r);
        if (level().isClientSide) return;
        Entity owner = getOwner();
        if (r.getEntity() instanceof LivingEntity target && target.hurt(damageSources().thrown(this, owner == null ? this : owner), DAMAGE + power)) freeze(target, owner);
    }

    // The freezing part, also used by the tests.
    public static void freeze(LivingEntity target, Entity by) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 3), by);
        if (!target.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)) target.setTicksFrozen(Math.max(target.getTicksFrozen(), FROST));
        if (target instanceof Mob mob && !(target instanceof Player)) stop(mob, isBoss(mob) ? BOSS_STOP_TICKS : STOP_TICKS);
        if (target.level() instanceof ServerLevel level) {
            Foes.particles(level, ParticleTypes.SNOWFLAKE, target.position().add(0, target.getBbHeight() * 0.5, 0), 14, 0.3, 0.05);
        }
    }

    private static boolean isBoss(Mob mob) {
        return mob instanceof WitherBoss || mob instanceof EnderDragon || mob instanceof Solgo || mob.getType().is(net.minecraftforge.common.Tags.EntityTypes.BOSSES) || mob.getMaxHealth() >= 150;
    }

    // Stopped dead: no thinking, no moving. Marked on the mob as well, in case the game shuts down before it wakes, so it can never stay frozen for ever.
    private static void stop(Mob mob, int ticks) {
        if (mob.isNoAi() && !mob.getPersistentData().getBoolean(HELD)) return; // something else switched its brain off, leave that alone
        mob.getPersistentData().putBoolean(HELD, true);
        mob.setNoAi(true);
        mob.getNavigation().stop();
        STOPPED.put(mob, mob.level().getGameTime() + ticks);
    }

    public static boolean stopped(Mob mob) {
        return STOPPED.containsKey(mob);
    }

    private static void letGo(Mob mob) {
        mob.getPersistentData().remove(HELD);
        mob.setNoAi(false);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || STOPPED.isEmpty()) return;
        Iterator<Map.Entry<Mob, Long>> it = STOPPED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Mob, Long> en = it.next();
            Mob mob = en.getKey();
            if (mob.isRemoved()) {
                letGo(mob);
                it.remove();
            } else if (mob.level().getGameTime() >= en.getValue()) {
                letGo(mob);
                it.remove();
            } else if (mob.level() instanceof ServerLevel level && mob.tickCount % 6 == 0) {
                Foes.particles(level, ParticleTypes.SNOWFLAKE, mob.position().add(0, mob.getBbHeight() * 0.6, 0), 2, 0.3, 0.01);
            }
        }
    }

    // A mob saved while frozen wakes up when it's next loaded.
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof Mob mob) || STOPPED.containsKey(mob)) return;
        if (mob.getPersistentData().getBoolean(HELD)) letGo(mob);
    }

    @Override
    protected void onHit(HitResult r) {
        super.onHit(r);
        if (!level().isClientSide) shatter();
    }

    private void shatter() {
        if (level() instanceof ServerLevel level) {
            Foes.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), position(), 18, 0.15, 0.2);
            Foes.particles(level, ParticleTypes.SNOWFLAKE, position(), 10, 0.2, 0.08);
            level.playSound(null, getX(), getY(), getZ(), CharItems.sound("ice_shatter"), SoundSource.PLAYERS, 0.9f, 0.9f + level.random.nextFloat() * 0.25f);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("power", power);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        power = tag.getInt("power");
    }
}
