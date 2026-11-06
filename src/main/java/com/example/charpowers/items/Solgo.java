package com.example.charpowers.items;

import com.example.charpowers.items.model.ComboRules;
import com.example.charpowers.items.model.SolgoRules;
import com.example.charpowers.items.model.SolgoRules.Move;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Solgo: ten blocks tall if he ever stood up, which he doesn't. He hunches and leans, walks like something wrong, and carries a long thin katana that
// grows out of his hand. He doesn't path-find like a mob: his legs step over anything low and he passes straight through trees. If stone walls him off he flickers.
// Every attack has a clear tell. He gets faster when you don't look at him and freezes for a moment when you first do. He never breaks a block.
public class Solgo extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(Solgo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOVE_COUNT = SynchedEntityData.defineId(Solgo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PHASE_TWO = SynchedEntityData.defineId(Solgo.class, EntityDataSerializers.BOOLEAN);
    private static final int DEATH_TICKS = 70, BAR_RANGE = 48;
    private static final Map<Move, RawAnimation> ANIMS = new EnumMap<>(Move.class);

    static {
        ANIMS.put(Move.IDLE, RawAnimation.begin().thenLoop("idle"));
        ANIMS.put(Move.WALK, RawAnimation.begin().thenLoop("walk"));
        ANIMS.put(Move.RUN, RawAnimation.begin().thenLoop("run"));
        ANIMS.put(Move.WINDUP, RawAnimation.begin().thenPlayAndHold("windup"));
        ANIMS.put(Move.SLASH, RawAnimation.begin().thenPlayAndHold("slash"));
        ANIMS.put(Move.CROUCH, RawAnimation.begin().thenPlayAndHold("crouch"));
        ANIMS.put(Move.LUNGE, RawAnimation.begin().thenPlayAndHold("lunge"));
        ANIMS.put(Move.VANISH, RawAnimation.begin().thenPlayAndHold("crouch"));
        ANIMS.put(Move.ARRIVE, RawAnimation.begin().thenPlayAndHold("arrive"));
        ANIMS.put(Move.SCREAM, RawAnimation.begin().thenPlayAndHold("scream"));
        ANIMS.put(Move.STARED, RawAnimation.begin().thenPlayAndHold("stared"));
        ANIMS.put(Move.DIE, RawAnimation.begin().thenPlayAndHold("death"));
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_6);
    private int seenMoveCount = -1;
    private int moveTicks, slashCd, lungeCd = 40, screamCd = 120, flickerCd = 160, stareCd, unseen, blocked, lost, recover;
    private boolean looked, leaves;
    private Vec3 lungeDir = Vec3.ZERO;

    public Solgo(EntityType<? extends Solgo> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        xpReward = 250;
        setPersistenceRequired();
        bar.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 420).add(Attributes.ATTACK_DAMAGE, 13).add(Attributes.ARMOR, 8).add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    // Made by a natural spawn: walks away when there's nobody to hunt or the sun's up. Summoned or from an egg he stays until killed.
    public void setLeaves(boolean leaves) {
        this.leaves = leaves;
    }

    // A gate's own scale.
    public void scaleForParty(int rankAboveA, int party) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(SolgoRules.scaledHealth(420, rankAboveA, party));
        setHealth(getMaxHealth());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MOVE, Move.IDLE.ordinal());
        entityData.define(MOVE_COUNT, 0);
        entityData.define(PHASE_TWO, false);
    }

    public Move move() {
        return Move.values()[entityData.get(MOVE)];
    }

    public int moveCount() {
        return entityData.get(MOVE_COUNT);
    }

    public boolean phaseTwo() {
        return entityData.get(PHASE_TWO);
    }

    public int moveTicks() {
        return moveTicks;
    }

    // Starts one of his moves. Public so the tests can make him show each one.
    public void begin(Move m) {
        entityData.set(MOVE, m.ordinal());
        entityData.set(MOVE_COUNT, moveCount() + 1);
        moveTicks = 0;
    }

    private void stroll() {
        if (move() != Move.WALK && move() != Move.RUN) begin(phaseTwo() ? Move.RUN : Move.WALK);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity e) {}

    @Override
    public boolean canBeLeashed(Player p) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource s) {
        return s.is(DamageTypes.IN_WALL) || s.is(DamageTypes.DROWN) || s.is(DamageTypes.FALL) || s.is(DamageTypes.CRAMMING) || super.isInvulnerableTo(s);
    }

    @Override
    public boolean hurt(DamageSource s, float amount) {
        if (move() == Move.VANISH && moveTicks > 5) return false;
        return super.hurt(s, amount);
    }

    @Override
    public void travel(Vec3 input) {
        // his own legs do all of this, in walkTowards
    }

    @Override
    public void knockback(double strength, double x, double z) {}

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4, 2, 4);
    }

    @Override
    protected void registerGoals() {}

    // ---- what he thinks, every tick on the server

    @Override
    protected void customServerAiStep() {
        setDeltaMovement(Vec3.ZERO);
        moveTicks++;
        slashCd = Math.max(0, slashCd - 1);
        lungeCd = Math.max(0, lungeCd - 1);
        screamCd = Math.max(0, screamCd - 1);
        flickerCd = Math.max(0, flickerCd - 1);
        stareCd = Math.max(0, stareCd - 1);
        ServerLevel level = (ServerLevel) level();
        bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 20 == 0) {
            pickTarget(level);
            updateBar(level);
        }
        boolean phase = SolgoRules.phaseTwo(getHealth(), getMaxHealth());
        if (phase && !phaseTwo()) {
            entityData.set(PHASE_TWO, true);
            screamCd = 0;
        }
        if (move() != Move.LUNGE) settle();
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.level() != level) {
            setTarget(null);
            noTarget(level);
            return;
        }
        lost = 0;
        recover = Math.max(0, recover - 1);
        double dist = Math.sqrt(distanceToSqr(target.getX(), getY(), target.getZ()));
        checkLooks(level, target);
        switch (move()) {
            case WINDUP -> {
                turnTo(target, 3f);
                if (moveTicks == 1) level.playSound(null, getX(), getEyeY(), getZ(), CharItems.sound("solgo_windup"), SoundSource.HOSTILE, 1.2f, phaseTwo() ? 1.2f : 1f);
                if (moveTicks % 3 == 0) Foes.particles(level, ParticleTypes.SOUL_FIRE_FLAME, bladeSpot(), 1, 0.2, 0.01);
                if (moveTicks >= SolgoRules.windup(phaseTwo())) begin(Move.SLASH);
            }
            case SLASH -> {
                if (moveTicks == 3) slash(level);
                if (moveTicks >= 16) {
                    slashCd = SolgoRules.SLASH_COOLDOWN;
                    stroll();
                }
            }
            case CROUCH -> {
                turnTo(target, 6f);
                if (moveTicks == 1) level.playSound(null, getX(), getEyeY(), getZ(), CharItems.sound("solgo_whisper"), SoundSource.HOSTILE, 1f, 0.7f);
                if (moveTicks >= SolgoRules.crouch(phaseTwo())) {
                    lungeDir = target.position().subtract(position()).multiply(1, 0, 1);
                    lungeDir = lungeDir.lengthSqr() < 1e-3 ? getLookAngle().multiply(1, 0, 1).normalize() : lungeDir.normalize();
                    level.playSound(null, getX(), getEyeY(), getZ(), CharItems.sound("solgo_lunge"), SoundSource.HOSTILE, 1.3f, 1f);
                    begin(Move.LUNGE);
                }
            }
            case LUNGE -> lungeStep(level, target);
            case VANISH -> {
                if (moveTicks == 1) level.playSound(null, getX(), getEyeY(), getZ(), CharItems.sound("solgo_flicker"), SoundSource.HOSTILE, 1f, 1f);
                if (moveTicks == 9) flickerBehind(level, target);
            }
            case ARRIVE -> {
                turnTo(target, 8f);
                if (moveTicks >= 16) stroll();
            }
            case SCREAM -> {
                turnTo(target, 4f);
                if (moveTicks == 12) scream(level);
                if (moveTicks >= 36) {
                    screamCd = SolgoRules.SCREAM_COOLDOWN;
                    stroll();
                }
            }
            case STARED -> {
                turnTo(target, 12f);
                if (moveTicks >= SolgoRules.STARE_FREEZE) {
                    stareCd = SolgoRules.STARE_COOLDOWN;
                    stroll();
                }
            }
            default -> freeToAct(level, target, dist);
        }
    }

    private void freeToAct(ServerLevel level, LivingEntity target, double dist) {
        turnTo(target, phaseTwo() ? 8f : 5f);
        if (recover > 0) {
            if (move() != Move.IDLE) begin(Move.IDLE);
            return;
        }
        Move next = SolgoRules.choose(dist, phaseTwo(), looked, unseen, slashCd, lungeCd, screamCd, flickerCd, random.nextDouble());
        if (next != null) {
            if (next == Move.VANISH) {
                flickerCd = SolgoRules.FLICKER_COOLDOWN;
                unseen = 0;
            } else if (next == Move.CROUCH) {
                lungeCd = SolgoRules.LUNGE_COOLDOWN;
            }
            begin(next);
            return;
        }
        if (dist > 4) {
            stroll();
            if (!walkTowards(target.position(), SolgoRules.speed(phaseTwo(), looked))) {
                if (++blocked > 50 && flickerCd <= 20) {
                    blocked = 0;
                    flickerCd = SolgoRules.FLICKER_COOLDOWN;
                    begin(Move.VANISH);
                }
            } else {
                blocked = 0;
            }
        } else if (move() != Move.IDLE) {
            begin(Move.IDLE);
        }
    }

    // Who he's after: the nearest player he could reasonably reach, or whoever last hurt him.
    private void pickTarget(ServerLevel level) {
        LivingEntity t = getTarget();
        if (t != null && t.isAlive() && t.level() == level && distanceToSqr(t) < 72 * 72 && !(t instanceof Player p && (p.isCreative() || p.isSpectator()))) return;
        Player nearest = level.getNearestPlayer(getX(), getY(), getZ(), 64, e -> e instanceof Player p && !p.isCreative() && !p.isSpectator() && e.isAlive());
        if (nearest != null) {
            setTarget(nearest);
            return;
        }
        LivingEntity hurtBy = getLastHurtByMob();
        setTarget(hurtBy != null && hurtBy.isAlive() && hurtBy.level() == level ? hurtBy : null);
    }

    private void noTarget(ServerLevel level) {
        if (move() != Move.IDLE) begin(Move.IDLE);
        if (!leaves) return;
        // nobody left to hunt, or the night is over: gone, without a sound
        boolean sunUp = !SolgoRules.nightTime(level.getDayTime()) && level.dimensionType().hasSkyLight();
        if (++lost > (sunUp ? 60 : 300)) {
            Foes.particles(level, ParticleTypes.LARGE_SMOKE, position().add(0, 3, 0), 30, 1.2, 0.02);
            discard();
        }
    }

    private void checkLooks(ServerLevel level, LivingEntity target) {
        if (tickCount % 2 != 0) return;
        boolean now = false;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(this) > 64 * 64) continue;
            Vec3 to = position().add(0, 4, 0).subtract(p.getEyePosition());
            Vec3 view = p.getViewVector(1f);
            if (SolgoRules.lookingAt(view.x, view.y, view.z, to.x, to.y, to.z) && p.hasLineOfSight(this)) {
                now = true;
                break;
            }
        }
        unseen = now ? 0 : unseen + 2;
        // the first look stops him where he is, for a moment
        if (now && !looked && stareCd <= 0 && (move() == Move.WALK || move() == Move.RUN || move() == Move.IDLE)) {
            begin(Move.STARED);
            if (random.nextInt(3) == 0) level.playSound(null, getX(), getEyeY(), getZ(), CharItems.sound("solgo_whisper"), SoundSource.HOSTILE, 0.9f, 1f);
        }
        looked = now;
    }

    public boolean looked() {
        return looked;
    }

    private void turnTo(LivingEntity target, float maxDegrees) {
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        float yaw = Mth.approachDegrees(getYRot(), want, maxDegrees);
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = Mth.approachDegrees(yHeadRot, want, maxDegrees * 3);
        lookControl.setLookAt(target);
        double dy = target.getEyeY() - getEyeY();
        setXRot((float) (-Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG));
    }

    // Steps towards a spot. False when something is in the way.
    private boolean walkTowards(Vec3 to, double speed) {
        Vec3 d = to.subtract(position()).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-3) return true;
        d = d.normalize().scale(speed);
        return step(d.x, d.z);
    }

    private boolean step(double dx, double dz) {
        double x = getX() + dx, z = getZ() + dz;
        int ground = groundUnder(x, z, Mth.floor(getY()));
        if (ground == Integer.MIN_VALUE) return false;
        // his long legs take a step up easily, but he glides down rather than dropping
        double y = getY() + Mth.clamp(ground - getY(), -0.6, 0.8);
        setPos(x, y, z);
        return true;
    }

    // Never floating, wherever he was put.
    private void settle() {
        int ground = groundUnder(getX(), getZ(), Mth.floor(getY()));
        if (ground != Integer.MIN_VALUE && Math.abs(ground - getY()) > 0.01) setPos(getX(), getY() + Mth.clamp(ground - getY(), -0.6, 0.8), getZ());
    }

    // The height he can stand at near here: trees don't count, he goes through them. Up to 5 blocks up or 12 down. MIN_VALUE when it's blocked.
    private int groundUnder(double x, double z, int from) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(x), bz = Mth.floor(z);
        for (int y = from + 5; y >= from - 12; y--) {
            p.set(bx, y, bz);
            if (!passable(p)) continue;
            p.set(bx, y - 1, bz);
            if (!passable(p)) return y;
        }
        return Integer.MIN_VALUE;
    }

    private boolean passable(BlockPos p) {
        if (!level().isLoaded(p)) return true;
        BlockState s = level().getBlockState(p);
        return s.getCollisionShape(level(), p).isEmpty() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS);
    }

    private Vec3 bladeSpot() {
        double yaw = Math.toRadians(yBodyRot);
        return position().add(-Math.sin(yaw) * 3.5 - Math.cos(yaw) * 1.5, 4.5, Math.cos(yaw) * 3.5 - Math.sin(yaw) * 1.5);
    }

    // ---- his moves

    private void slash(ServerLevel level) {
        level.playSound(null, getX(), getEyeY(), getZ(), CharItems.sound("solgo_slash"), SoundSource.HOSTILE, 1.5f, phaseTwo() ? 1.15f : 1f);
        double yaw = Math.toRadians(yBodyRot);
        double lx = -Math.sin(yaw), lz = Math.cos(yaw);
        // what you can see of it as well as the blade itself: a fan of light where it passed
        for (int i = -5; i <= 5; i++) {
            double a = yaw + i * 0.2;
            for (double r = 4; r <= SolgoRules.SLASH_RANGE; r += 2) {
                Foes.particles(level, ParticleTypes.SWEEP_ATTACK, getX() - Math.sin(a) * r, getY() + 1.4, getZ() + Math.cos(a) * r, 1, 0, 0, 0, 0);
            }
        }
        for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SolgoRules.SLASH_RANGE + 1, 2, SolgoRules.SLASH_RANGE + 1), this::fairGame)) {
            Vec3 d = v.position().subtract(position());
            if (ComboRules.inArc(d.x, d.z, lx, lz, SolgoRules.SLASH_RANGE + v.getBbWidth() * 0.5, 62) && hasLineOfSight(v)) {
                hitHard(v, SolgoRules.slashDamage(phaseTwo()), 0.5);
            }
        }
    }

    private void lungeStep(ServerLevel level, LivingEntity target) {
        Vec3 d = lungeDir.scale(phaseTwo() ? 1.3 : 1.05);
        boolean moved = moveTicks <= 10 && step(d.x, d.z);
        if (moveTicks <= 10) Foes.particles(level, ParticleTypes.SQUID_INK, position().add(0, 2, 0), 3, 0.5, 0.02);
        if (moveTicks <= 12) {
            Vec3 tip = position().add(lungeDir.scale(4.5));
            for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, new AABB(tip, position()).inflate(1.4, 3, 1.4), this::fairGame)) {
                if (v.invulnerableTime <= 10) hitHard(v, SolgoRules.lungeDamage(phaseTwo()), 0.9);
            }
        }
        if (moveTicks > 16 || !moved && moveTicks <= 10) {
            recover = phaseTwo() ? 12 : 22;
            begin(Move.IDLE);
        }
    }

    private void flickerBehind(ServerLevel level, LivingEntity target) {
        Foes.particles(level, ParticleTypes.LARGE_SMOKE, position().add(0, 3, 0), 20, 0.8, 0.01);
        Vec3 view = target.getViewVector(1f).multiply(1, 0, 1);
        view = view.lengthSqr() < 1e-3 ? new Vec3(0, 0, 1) : view.normalize();
        Vec3 behind = null;
        for (double back = 7; back >= 3 && behind == null; back -= 1) {
            for (int side = -2; side <= 2 && behind == null; side += 2) {
                double x = target.getX() - view.x * back + view.z * side, z = target.getZ() - view.z * back - view.x * side;
                int ground = groundUnder(x, z, Mth.floor(target.getY()));
                if (ground != Integer.MIN_VALUE && Math.abs(ground - target.getY()) < 6) behind = new Vec3(x, ground, z);
            }
        }
        if (behind != null) {
            setPos(behind.x, behind.y, behind.z);
            level.playSound(null, target.getX(), target.getEyeY(), target.getZ(), CharItems.sound("solgo_whisper"), SoundSource.HOSTILE, 1.1f, 0.85f);
            Foes.particles(level, ParticleTypes.LARGE_SMOKE, position().add(0, 3, 0), 16, 0.6, 0.01);
        }
        turnTo(target, 180f);
        slashCd = Math.min(slashCd, 12);
        begin(Move.ARRIVE);
    }

    private void scream(ServerLevel level) {
        level.playSound(null, getX(), getEyeY(), getZ(), CharItems.sound("solgo_scream"), SoundSource.HOSTILE, 1.5f, 1f);
        Foes.particles(level, ParticleTypes.SCULK_SOUL, getEyePosition(), 25, 0.5, 0.15);
        for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SolgoRules.SCREAM_RANGE), this::fairGame)) {
            v.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 55, 0), this);
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 1), this);
        }
    }

    private boolean fairGame(LivingEntity v) {
        if (v == this || !v.isAlive()) return false;
        if (v instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return v == getTarget();
    }

    private void hitHard(LivingEntity v, float damage, double push) {
        if (!v.hurt(damageSources().mobAttack(this), damage)) return;
        setLastHurtMob(v);
        Vec3 away = v.position().subtract(position()).multiply(1, 0, 1);
        if (away.lengthSqr() > 1e-3) {
            away = away.normalize();
            v.knockback(push, -away.x, -away.z);
        }
    }

    // Bar for everyone close enough that it's their problem.
    private void updateBar(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            boolean near = p.distanceToSqr(this) < BAR_RANGE * BAR_RANGE;
            if (near && !bar.getPlayers().contains(p)) bar.addPlayer(p);
            else if (!near && bar.getPlayers().contains(p)) bar.removePlayer(p);
        }
        for (ServerPlayer p : List.copyOf(bar.getPlayers())) {
            if (p.level() != level || p.isRemoved()) bar.removePlayer(p);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bar.removePlayer(p);
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        super.remove(reason);
    }

    // ---- dying takes a while: he folds up and comes apart

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && move() != Move.DIE) begin(Move.DIE);
        bar.setProgress(0);
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel level) {
            if (deathTime % 3 == 0) Foes.particles(level, ParticleTypes.SOUL, position().add(0, 1.5 + deathTime * 0.03, 0), 6, 1.0, 0.04);
            if (deathTime >= DEATH_TICKS && !isRemoved()) {
                Foes.particles(level, ParticleTypes.SCULK_SOUL, position().add(0, 1, 0), 60, 1.6, 0.05);
                bar.removeAllPlayers();
                remove(RemovalReason.KILLED);
            }
        }
    }

    // ---- sounds, sparingly

    @Override
    protected SoundEvent getAmbientSound() {
        return getTarget() != null && random.nextInt(3) == 0 ? CharItems.sound("solgo_whisper") : CharItems.sound("solgo_breath");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return CharItems.sound("solgo_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return CharItems.sound("solgo_death");
    }

    @Override
    protected float getSoundVolume() {
        return 1.3f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Leaves", leaves);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        leaves = tag.getBoolean("Leaves");
        noPhysics = true;
        setNoGravity(true);
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    // ---- animations

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "body", 3, this::animate));
    }

    private PlayState animate(AnimationState<Solgo> state) {
        int count = moveCount();
        if (count != seenMoveCount) {
            seenMoveCount = count;
            state.getController().forceAnimationReset();
        }
        Move m = isDeadOrDying() ? Move.DIE : move();
        return state.setAndContinue(ANIMS.get(m));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Nullable
    public static Solgo spawn(ServerLevel level, double x, double y, double z, boolean leaves) {
        Solgo s = CharItems.SOLGO.get().create(level);
        if (s == null) return null;
        s.moveTo(x, y, z, level.random.nextFloat() * 360f, 0f);
        s.setLeaves(leaves);
        level.addFreshEntity(s);
        return s;
    }
}
