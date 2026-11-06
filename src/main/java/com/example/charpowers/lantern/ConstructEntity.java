package com.example.charpowers.lantern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToDoubleBiFunction;

// One thing made of hard light: a hand, a sword, a gun. It only shows a model and plays my animations. What it does
// is run by GreenLantern, at the moments the animations line up with. Never saved, never touches a block.
public class ConstructEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> BASE = SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> OVER = SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> RESTART = SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_PITCH = SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> YELLOW = SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> HOLDER = SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.INT);

    // How fast an animation plays: 1 unless the tuner (/constructs) on this PC says otherwise.
    public static ToDoubleBiFunction<String, String> speed = (kind, animation) -> 1;
    // What the PC does each tick for a construct somebody's holding: see client/anim/PlayerAnims.
    public static Consumer<ConstructEntity> whileHeld = gun -> {};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<Timed> timed = new ArrayList<>();
    private int age, life, seenRestart;
    private Vec3 velocity;
    private Entity launcher;
    private Consumer<LivingEntity> onHit;

    private record Timed(int at, Runnable what) {}

    public ConstructEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public static ConstructEntity make(Level level, String kind, String animation, double x, double y, double z, float yaw, int life) {
        ConstructEntity c = new ConstructEntity(LanternEntities.CONSTRUCT.get(), level);
        c.entityData.set(KIND, kind);
        c.entityData.set(BASE, animation);
        c.life = life;
        c.moveTo(x, y, z, yaw, 0f);
        level.addFreshEntity(c);
        // the air whoosh of it being made (the many little swords stay quiet)
        if (!"dagger".equals(kind)) LanternSounds.play(level, c.position(), LanternSounds.FORM, 0.7f, "turret".equals(kind) || "minigun".equals(kind) ? 0.8f : 1f);
        return c;
    }

    // Only for the tuner: lives on this PC alone, the server never hears of it.
    public static ConstructEntity preview(Level level, String kind, String animation, double x, double y, double z, float yaw) {
        ConstructEntity c = new ConstructEntity(LanternEntities.CONSTRUCT.get(), level);
        c.entityData.set(KIND, kind);
        c.entityData.set(BASE, animation);
        c.moveTo(x, y, z, yaw, 0f);
        return c;
    }

    // Do this when the entity is this many ticks old, the moment a hand connects.
    public void at(int tick, Runnable what) {
        timed.add(new Timed(tick, what));
    }

    public int age() {
        return age;
    }

    public String kind() {
        return entityData.get(KIND);
    }

    // Whose hands this is in: the gun is drawn there on every PC, and they pose to hold it.
    public void holdBy(Entity holder) {
        entityData.set(HOLDER, holder.getId());
    }

    public int holder() {
        return entityData.get(HOLDER);
    }

    public String animation() {
        return entityData.get(BASE);
    }

    public void play(String animation) {
        entityData.set(BASE, animation);
    }

    // A second animation at the same time: the sword's swing over its idle. Restarts even if it's the same one again.
    public void playOver(String animation) {
        entityData.set(OVER, animation);
        entityData.set(RESTART, entityData.get(RESTART) + 1);
    }

    // Sinestro's are yellow.
    public void yellow() {
        entityData.set(YELLOW, true);
    }

    public boolean isYellow() {
        return entityData.get(YELLOW);
    }

    // Off it goes in a straight line until it hits something or goes too far.
    public void launch(Vec3 velocity, Entity launcher, Consumer<LivingEntity> onHit) {
        this.velocity = velocity;
        this.launcher = launcher;
        this.onHit = onHit;
        setYRot((float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)));
        setXRot((float) Math.toDegrees(-Math.asin(velocity.normalize().y)));
        play("fly");
    }

    private void fly() {
        Vec3 from = position(), to = from.add(velocity);
        BlockHitResult wall = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        EntityHitResult struck = ProjectileUtil.getEntityHitResult(level(), this, from, to, new AABB(from, to).inflate(1), x -> x instanceof LivingEntity && x != launcher && x.isAlive());
        setPos(to.x, to.y, to.z);
        if (struck != null) onHit.accept((LivingEntity) struck.getEntity());
        if (struck != null || wall.getType() != HitResult.Type.MISS || age > 100) {
            velocity = null;
            play("vanish");
            endIn(4);
        }
    }

    public void aim(float yaw, float pitch) {
        entityData.set(AIM_YAW, yaw);
        entityData.set(AIM_PITCH, pitch);
    }

    public float aimYaw() {
        return entityData.get(AIM_YAW);
    }

    public float aimPitch() {
        return entityData.get(AIM_PITCH);
    }

    public void endIn(int ticks) {
        life = ticks;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, "fist");
        entityData.define(BASE, "");
        entityData.define(OVER, "");
        entityData.define(RESTART, 0);
        entityData.define(AIM_YAW, 0f);
        entityData.define(AIM_PITCH, 0f);
        entityData.define(YELLOW, false);
        entityData.define(HOLDER, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (holder() >= 0) whileHeld.accept(this);
            return;
        }
        age++;
        if (velocity != null) fly();
        for (Timed t : new ArrayList<>(timed)) {
            if (t.at() == age) t.what().run();
        }
        if (life > 0 && --life == 0) {
            if (!"dagger".equals(kind())) LanternSounds.play(level(), position(), LanternSounds.DISSOLVE, 0.5f, 1f);
            discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 0, this::base));
        controllers.add(new AnimationController<>(this, "over", 0, this::over));
    }

    private static RawAnimation raw(String name) {
        return RawAnimation.begin().then(name, Animation.LoopType.DEFAULT);
    }

    private PlayState base(AnimationState<ConstructEntity> state) {
        String name = entityData.get(BASE);
        if (name.isEmpty()) return PlayState.STOP;
        state.getController().setAnimationSpeed(speed.applyAsDouble(kind(), name));
        return state.setAndContinue(raw(name));
    }

    private PlayState over(AnimationState<ConstructEntity> state) {
        String name = entityData.get(OVER);
        if (name.isEmpty()) return PlayState.STOP;
        state.getController().setAnimationSpeed(speed.applyAsDouble(kind(), name));
        int restart = entityData.get(RESTART);
        if (restart != seenRestart) {
            seenRestart = restart;
            state.getController().forceAnimationReset();
        }
        return state.setAndContinue(raw(name));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
