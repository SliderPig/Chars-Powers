package com.example.charpowers.cat;

import com.example.charpowers.cat.model.CatRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nullable;
import java.util.UUID;

// A small black cat that keeps its owner company and does nothing else: it trails behind, sits when you stop, naps if you stay a
// long while, purrs quietly now and then, and hops when you level up. It cannot be hurt, moved, ridden or fed, nothing hunts
// it, nothing is afraid of it, it is never saved with the world (it is made again each time), and it never fights. It
// is the vanilla cat, so it looks and moves like one (all-black fur, a pink collar).
public class CompanionCat extends Cat {
    public static final String TAG = "charpowers.cat";
    private int stillTicks, sittingTicks, purrIn = 200, hopsLeft, hopWait;
    private CatRules.Pose pose = CatRules.Pose.STAND;
    private BlockPos lastPlace = BlockPos.ZERO;

    public CompanionCat(EntityType<? extends Cat> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        addTag(TAG);
        setPersistenceRequired();
        setVariant(BuiltInRegistries.CAT_VARIANT.get(CatVariant.ALL_BLACK));
        setCollarColor(DyeColor.PINK);
        setTame(true);
    }

    public void bindTo(UUID owner) {
        setOwnerUUID(owner);
    }

    @Nullable
    public Player owner() {
        UUID id = getOwnerUUID();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    // ---- never part of the world's saved data, never in anyone's way

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void reassessTameGoals() {}

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {}

    @Override
    public void push(Entity entity) {}

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Nullable
    @Override
    public Cat getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean doHurtTarget(Entity victim) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 100000;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    // vanilla purrs every few ticks while a cat is lying down, and loudly: here it is soft
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, sound == SoundEvents.CAT_PURR ? volume * 0.2f : volume, pitch);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isCustomNameVisible() {
        return false;
    }

    @Override
    public Component getName() {
        return Component.literal("Cat");
    }

    // ---- what it does

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player owner = owner();
        if (owner == null || !owner.isAlive() || owner.level() != level()) {
            discard();
            return;
        }
        double distance = distanceTo(owner);
        int travel = CatRules.travel(distance);
        if (travel == 3) {
            hopOver(owner);
            return;
        }
        boolean ownerStill = CatRules.ownerStill(owner.getDeltaMovement().horizontalDistanceSqr()) && owner.blockPosition().equals(lastPlace);
        lastPlace = owner.blockPosition();
        stillTicks = ownerStill ? stillTicks + 1 : 0;
        CatRules.Pose want = CatRules.pose(distance, stillTicks, sittingTicks, random.nextDouble(), pose);
        if (travel > 0) want = CatRules.Pose.STAND;
        if (want != pose) settle(want);
        sittingTicks = pose == CatRules.Pose.STAND ? 0 : sittingTicks + 1;
        if (pose == CatRules.Pose.STAND && travel > 0 && tickCount % 10 == 0) getNavigation().moveTo(owner, travel == 2 ? 1.45 : 1.1);
        if (pose != CatRules.Pose.STAND) getNavigation().stop();
        if (hopsLeft > 0 && --hopWait <= 0 && onGround()) {
            hopsLeft--;
            hopWait = 8;
            setDeltaMovement(getDeltaMovement().x, 0.42, getDeltaMovement().z);
            hasImpulse = true;
        }
        if (--purrIn <= 0) {
            purrIn = CatRules.nextPurr(random.nextDouble());
            if (distance < 6 && pose != CatRules.Pose.STAND) playSound(SoundEvents.CAT_PURR, 0.8f, 1f);
        }
    }

    private void settle(CatRules.Pose next) {
        pose = next;
        setInSittingPose(next != CatRules.Pose.STAND);
        setLying(next == CatRules.Pose.NAP);
    }

    // Too far behind (or in another place): it hops over beside you.
    private void hopOver(Player owner) {
        for (int i = 0; i < 8; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double x = owner.getX() + Math.cos(a) * 1.8, z = owner.getZ() + Math.sin(a) * 1.8;
            BlockPos at = BlockPos.containing(x, owner.getY(), z);
            if (level().getBlockState(at).getCollisionShape(level(), at).isEmpty() && level().getBlockState(at.above()).getCollisionShape(level(), at.above()).isEmpty()
                    && !level().getBlockState(at.below()).getCollisionShape(level(), at.below()).isEmpty()) {
                moveTo(x, owner.getY(), z, owner.getYRot(), 0f);
                getNavigation().stop();
                settle(CatRules.Pose.STAND);
                return;
            }
        }
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0f);
    }

    // A meow and two little hops, for a level-up.
    public void cheer() {
        settle(CatRules.Pose.STAND);
        stillTicks = 0;
        hopsLeft = 2;
        hopWait = 0;
        level().playSound(null, blockPosition(), SoundEvents.CAT_AMBIENT, SoundSource.NEUTRAL, 0.6f, 1.25f);
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.7, getZ(), 3, 0.2, 0.2, 0.2, 0.02);
    }

    public static boolean isMine(CompanionCat cat, @Nullable ServerPlayer p) {
        return p != null && p.getUUID().equals(cat.getOwnerUUID());
    }
}
