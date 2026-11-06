package com.example.charpowers.game.mob;

import com.example.charpowers.game.model.Elite;
import com.example.charpowers.game.model.MobMove;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

// The ordinary creatures of gates. One class: the kind decides the look, the gear and which moves it has (MobMove.of(kind.id)).
// Every creature has two or three of its own moves; each shows a tell, then lands, then waits out a cooldown. One timer decides
// when to think, and only a creature with a move ready ever looks at what is around it. They never spawn on their own: a gate puts them there.
public class GateMob extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> TELL = SynchedEntityData.defineId(GateMob.class, EntityDataSerializers.INT);
    private static final UUID BUFF = UUID.fromString("5d7c2a4e-9d0e-4a44-b3a1-1c2e6f0b7a11");
    private static final Set<String> FLYERS = Set.of("imp", "wraith", "sentry_drone");
    private static final Set<String> CLIMBERS = Set.of("alien_crawler", "giant_spider");
    private static final Set<String> KEEPS_AWAY = Set.of("goblin_shaman", "elf", "ice_archer", "lich", "cyber_soldier", "sentry_drone");
    private static final Set<String> SHIELDED = Set.of("skeleton_knight", "bulwark_mech"); // block half of what comes from the front, always
    public static final String CURSE = "charpowers.gatecurse";

    public final GateKind kind;
    private final List<MobMove> moves;
    private final long[] ready;
    private final List<Object[]> later = new ArrayList<>(); // {Integer dueTick, Runnable}
    private final List<BlockPos> webs = new ArrayList<>();
    private MobMove current;
    private int currentIndex, think = 30, windup, leapTicks, chargeLeft, burrowTicks, phaseTicks;
    private long blockUntil, skinUntil, stunUntil;
    private boolean dormant, enraged, hitByCharge, leapHit;
    private double eliteMul = 1, cooldownMul = 1;
    private Vec3 chargeDir = Vec3.ZERO;
    private Elite elite;

    public GateMob(EntityType<? extends GateMob> type, Level level, GateKind kind) {
        super(type, level);
        this.kind = kind;
        this.moves = MobMove.of(kind.id);
        this.ready = new long[moves.size()];
        this.xpReward = 5;
        if (FLYERS.contains(kind.id)) {
            this.moveControl = new FlyingMoveControl(this, 20, true);
            setNoGravity(true);
        }
        initGoals(); // not registerGoals(): that runs before the kind is known
    }

    public static AttributeSupplier.Builder attributes(GateKind kind) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, kind.health).add(Attributes.ATTACK_DAMAGE, kind.damage).add(Attributes.MOVEMENT_SPEED, kind.speed)
                .add(Attributes.FLYING_SPEED, kind.speed).add(Attributes.FOLLOW_RANGE, 40).add(Attributes.ARMOR, kind.armor).add(Attributes.KNOCKBACK_RESISTANCE, kind.knockback);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        String id = EntityType.getKey(getType()).getPath(); // the kind is not set yet while the constructor is still running
        if (FLYERS.contains(id)) {
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
            nav.setCanOpenDoors(false);
            nav.setCanFloat(true);
            nav.setCanPassDoors(true);
            return nav;
        }
        return CLIMBERS.contains(id) ? new WallClimberNavigation(this, level) : super.createNavigation(level);
    }

    @Override
    public boolean onClimbable() {
        return CLIMBERS.contains(kind.id) ? horizontalCollision : super.onClimbable();
    }

    @Override
    protected void registerGoals() {}

    private void initGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        boolean bow = kind == GateKind.ELF || kind == GateKind.ICE_ARCHER;
        if (KEEPS_AWAY.contains(kind.id)) goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, bow ? 5f : 7f, 1.0, 1.25));
        if (bow) goalSelector.addGoal(3, new RangedBowAttackGoal<>(this, 1.0, 22, 16f));
        else if (kind != GateKind.GOBLIN_SHAMAN && kind != GateKind.LICH && kind != GateKind.SENTRY_DRONE) goalSelector.addGoal(3, new MeleeAttackGoal(this, kind == GateKind.GOBLIN || kind == GateKind.DIRE_WOLF ? 1.25 : 1.1, false));
        goalSelector.addGoal(6, FLYERS.contains(kind.id) ? new WaterAvoidingRandomFlyingGoal(this, 0.8) : new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, GateMob.class).setAlertOthers()); // hit one of a pack and the pack comes
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TELL, 0);
    }

    // 0 = nothing; 1 = getting ready for something (arms go up on the client)
    public int tell() {
        return entityData.get(TELL);
    }

    public boolean dormant() {
        return dormant;
    }

    public List<MobMove> moves() {
        return moves;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data, tag);
        switch (kind) {
            case GOBLIN -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            case GOBLIN_SHAMAN -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BLAZE_ROD));
            case HOBGOBLIN -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
            case ORC -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            case ELF, ICE_ARCHER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            case DARK_ELF -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
            }
            case DEMON, LIZARDMAN -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            case DEMON_KNIGHT -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            case SKELETON_KNIGHT -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case LICH -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.END_ROD));
            case CYBER_SOLDIER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            case MINOTAUR -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            case SLIME_KNIGHT -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            default -> {}
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0f);
        if (reason == MobSpawnType.MOB_SUMMONED && (kind == GateKind.GARGOYLE || kind == GateKind.MIMIC)) { // stone (or a chest) until something comes near
            dormant = true;
            setNoAi(true);
        }
        return out;
    }

    // Stronger, with a glowing name: Swift, Armoured, Berserk or Regenerating. The moves hit a little harder and come back sooner.
    public void makeElite(Elite e) {
        elite = e;
        eliteMul = 1.25;
        cooldownMul = 0.8;
        setCustomName(Component.literal(e.title + " " + kind.title));
        setCustomNameVisible(true);
        addEffect(new MobEffectInstance(MobEffects.GLOWING, 100000, 0, false, false));
        switch (e) {
            case SWIFT -> getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier(BUFF, "elite", 0.4, AttributeModifier.Operation.MULTIPLY_BASE));
            case ARMOURED -> {
                getAttribute(Attributes.ARMOR).addPermanentModifier(new AttributeModifier(BUFF, "elite", 8, AttributeModifier.Operation.ADDITION));
                getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(BUFF, "elite", 0.3, AttributeModifier.Operation.MULTIPLY_BASE));
                setHealth(getMaxHealth());
            }
            case BERSERK -> {
                getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier(BUFF, "elite", 0.5, AttributeModifier.Operation.MULTIPLY_BASE));
                getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier(BUFF, "elite", 0.15, AttributeModifier.Operation.MULTIPLY_BASE));
            }
            case REGENERATING -> addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100000, 0, false, false));
        }
    }

    public Elite elite() {
        return elite;
    }

    // ---- the brain: one timer, a move picked when it runs out

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && dormant && tickCount % 5 == 0) {
            double r = kind == GateKind.MIMIC ? 3.5 : 7;
            if (tickCount > 1800 || level().getNearestPlayer(this, r) != null) wake();
        }
    }

    private void wake() {
        if (!dormant) return;
        dormant = false;
        setNoAi(false);
        playSound(kind == GateKind.MIMIC ? SoundEvents.CHEST_OPEN : SoundEvents.STONE_BREAK, 1.2f, 0.7f);
        GateFx.burst((ServerLevel) level(), position().add(0, 1, 0), ParticleTypes.POOF, 14, 0.5);
    }

    @Override
    public boolean isImmobile() {
        long now = level().getGameTime();
        return super.isImmobile() || dormant || stunUntil > now || burrowTicks > 0 || (windup > 0 && current != null && !current.travels());
    }

    @Override
    public boolean doHurtTarget(Entity victim) {
        if (dormant || windup > 0 || burrowTicks > 0 || stunUntil > level().getGameTime()) return false;
        boolean hit = super.doHurtTarget(victim);
        if (hit && victim instanceof LivingEntity l) {
            if (kind == GateKind.DARK_ELF || kind == GateKind.ANT_SOLDIER || kind == GateKind.SCARAB) l.addEffect(new MobEffectInstance(MobEffects.POISON, 50, 0));
            if (kind == GateKind.DIRE_WOLF || kind == GateKind.ALPHA_WOLF) l.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
        }
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel lv = (ServerLevel) level();
        long now = lv.getGameTime();
        for (Iterator<Object[]> it = later.iterator(); it.hasNext(); ) {
            Object[] job = it.next();
            if (tickCount >= (Integer) job[0]) {
                it.remove();
                ((Runnable) job[1]).run();
            }
        }
        if (kind == GateKind.ORC && !enraged && getHealth() < getMaxHealth() / 2) {
            enraged = true;
            getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(BUFF, "enrage", 0.3, AttributeModifier.Operation.MULTIPLY_BASE));
            getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(new AttributeModifier(BUFF, "enrage", 0.4, AttributeModifier.Operation.MULTIPLY_BASE));
            playSound(SoundEvents.RAVAGER_ROAR, 1.4f, 1.2f);
            GateFx.burst(lv, position().add(0, 1, 0), ParticleTypes.ANGRY_VILLAGER, 12, 0.6);
        }
        if (burrowTicks > 0) {
            if (--burrowTicks % 4 == 0) GateFx.burst(lv, position(), ParticleTypes.POOF, 4, 0.4);
            if (burrowTicks == 0) popUp(lv);
            return;
        }
        if (stunUntil > now) {
            if (tickCount % 6 == 0) GateFx.burst(lv, position().add(0, getBbHeight() + 0.2, 0), ParticleTypes.CRIT, 4, 0.3);
            return;
        }
        if (chargeLeft > 0) runCharge(lv, now);
        if (leapTicks > 0) runLeap(lv);
        if (phaseTicks > 0) runPhase(lv);
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || moves.isEmpty()) return;
        if (windup > 0) {
            if (!current.travels()) getNavigation().stop();
            getLookControl().setLookAt(target, 40f, 40f);
            windup--;
            tellFx(lv);
            if (windup == 0) {
                entityData.set(TELL, 0);
                MobMove m = current;
                current = null;
                perform(lv, m, target);
                ready[currentIndex] = now + Math.round(m.cooldown * cooldownMul);
                think = 6;
            }
            return;
        }
        if (--think > 0 || chargeLeft > 0 || leapTicks > 0 || phaseTicks > 0) return;
        think = 8 + getRandom().nextInt(5);
        double far = distanceTo(target);
        int pick = -1, seen = 0;
        for (int i = 0; i < moves.size(); i++) {
            MobMove m = moves.get(i);
            if (ready[i] > now || far < m.near || far > m.far || !fits(m, target)) continue;
            if (getRandom().nextInt(++seen) == 0) pick = i; // a fair pick among the ones that fit
        }
        if (pick < 0) return;
        currentIndex = pick;
        current = moves.get(pick);
        windup = current.tell;
        entityData.set(TELL, 1);
        playSound(tellSound(current.look), 1f, 0.9f);
        if (current.travels() || current.look == MobMove.Look.LINE) {
            Vec3 d = target.position().subtract(position()).multiply(1, 0, 1);
            chargeDir = d.lengthSqr() < 0.01 ? Vec3.ZERO : d.normalize();
        }
    }

    // Only moves that need something around them look at what is around them, and only when they are off cooldown.
    private boolean fits(MobMove m, LivingEntity target) {
        switch (m) {
            case ROCK_THROW, FIRE_BOLT, FIREBALL, ACID_SPIT, POISON_SPIT, BOULDER, SPEAR_THROW, SHADOW_BOLT, VOID_BOLT, WEB_SHOT, GRENADE, LASER, RIFLE_BURST, RAPID_VOLLEY, ICE_ARROW, LIFE_DRAIN, CURSE, TONGUE_PULL, SAND_BLAST:
                return hasLineOfSight(target);
            case SWARM, HOWL, WAR_CRY:
                return allies(8).size() >= (m == MobMove.SWARM ? 3 : 1);
            case HEAL_ALLIES:
                for (GateMob a : allies(8)) {
                    if (a.getHealth() < a.getMaxHealth() * 0.8) return true;
                }
                return false;
            case RAISE_DEAD:
                return allies(14).size() < 5;
            case DODGE_ROLL:
                return target.distanceTo(this) < 6 && getRandom().nextInt(3) == 0;
            default:
                return true;
        }
    }

    private List<GateMob> allies(double r) {
        return level().getEntitiesOfClass(GateMob.class, getBoundingBox().inflate(r), a -> a != this && a.isAlive());
    }

    private SoundEvent tellSound(MobMove.Look look) {
        return switch (look) {
            case HIT -> SoundEvents.PLAYER_ATTACK_STRONG;
            case CAST -> SoundEvents.EVOKER_PREPARE_ATTACK;
            case RING -> SoundEvents.IRON_GOLEM_ATTACK;
            case LINE -> SoundEvents.WOLF_GROWL;
            case SELF -> SoundEvents.BEACON_POWER_SELECT;
        };
    }

    private ParticleOptions trail(MobMove m) {
        return switch (m) {
            case FIRE_BOLT, FIREBALL, FLAME_SLASH, GRENADE -> ParticleTypes.FLAME;
            case ACID_SPIT, POISON_SPIT, POISON_BLADE, VENOM_BITE, SPORE_CLOUD, SPORE_SLEEP -> ParticleTypes.ITEM_SLIME;
            case LASER, RIFLE_BURST -> ParticleTypes.ELECTRIC_SPARK;
            case VOID_BOLT, SHADOW_BOLT, LIFE_DRAIN, PHASE, TELEPORT, BLINK_BEHIND -> ParticleTypes.SOUL;
            case ICE_ARROW, FROST_ROAR -> ParticleTypes.SNOWFLAKE;
            case SAND_BLAST, BURROW -> ParticleTypes.CLOUD;
            case CURSE, RAISE_DEAD -> ParticleTypes.SCULK_SOUL;
            default -> ParticleTypes.CRIT;
        };
    }

    private void tellFx(ServerLevel lv) {
        if (windup % 2 != 0) return;
        double t = 1 - windup / (double) Math.max(1, current.tell);
        Vec3 hand = position().add(getLookAngle().multiply(1, 0, 1).scale(0.6)).add(0, getBbHeight() * 0.7, 0);
        switch (current.look) {
            case HIT -> GateFx.burst(lv, hand, ParticleTypes.CRIT, 2, 0.2);
            case CAST -> GateFx.burst(lv, hand, trail(current), 3, 0.25);
            case RING -> GateFx.ring(lv, position(), 4.0 * t, trail(current), 14);
            case LINE -> {
                for (int i = 1; i <= 6; i++) lv.sendParticles(ParticleTypes.CRIT, getX() + chargeDir.x * i, getY() + 0.2, getZ() + chargeDir.z * i, 1, 0.1, 0, 0.1, 0);
            }
            case SELF -> GateFx.burst(lv, position().add(0, getBbHeight() * 0.5, 0), ParticleTypes.ENCHANT, 5, 0.4);
        }
    }

    private float dmg(double times) {
        return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * times * eliteMul);
    }

    private void in(int ticks, Runnable r) {
        later.add(new Object[]{tickCount + ticks, r});
    }

    private void slow(Player p, int ticks, int amp) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
    }

    private void hitRing(ServerLevel lv, double radius, double times, double push, double lift, ParticleOptions fx, Consumer<Player> extra) {
        lv.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.7f, 0.8f);
        GateFx.ring(lv, position(), radius, fx, 24);
        for (Player p : GateFx.playersNear(this, position(), radius)) {
            if (p.hurt(damageSources().mobAttack(this), dmg(times))) {
                Vec3 away = p.position().subtract(position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : away.normalize();
                p.setDeltaMovement(p.getDeltaMovement().add(away.x * push, lift, away.z * push));
                p.hurtMarked = true;
                if (extra != null) extra.accept(p);
            }
        }
    }

    private void hitCone(ServerLevel lv, Vec3 dir, double reach, double times, double push, ParticleOptions fx, Consumer<Player> extra) {
        Vec3 tip = position().add(dir.scale(reach * 0.6)).add(0, 1, 0);
        lv.sendParticles(fx, tip.x, tip.y, tip.z, 4, 0.5, 0.3, 0.5, 0);
        for (Player p : GateFx.playersNear(this, position(), reach)) {
            Vec3 to = p.position().subtract(position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 0.01 && to.normalize().dot(dir) < 0.35) continue;
            if (p.hurt(damageSources().mobAttack(this), dmg(times))) {
                p.setDeltaMovement(p.getDeltaMovement().add(dir.x * push, 0.25, dir.z * push));
                p.hurtMarked = true;
                if (extra != null) extra.accept(p);
            }
        }
    }

    // A thrown thing that can be dodged: aimed where the target stands now, it lands a few ticks later on whoever is still there.
    private void shot(ServerLevel lv, LivingEntity t, MobMove m, double times, double radius, ParticleOptions fx, Consumer<Player> extra) {
        Vec3 from = position().add(0, getBbHeight() * 0.7, 0), spot = t.position();
        Vec3 aim = spot.add(0, 0.9, 0);
        for (int i = 1; i <= 8; i++) {
            Vec3 at = from.lerp(aim, i / 8.0);
            lv.sendParticles(fx, at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0);
        }
        in(3 + (int) (from.distanceTo(aim) * 0.5), () -> {
            lv.sendParticles(m == MobMove.GRENADE || m == MobMove.BOULDER ? ParticleTypes.EXPLOSION : fx, spot.x, spot.y + 0.6, spot.z, m == MobMove.GRENADE ? 2 : 6, radius * 0.4, 0.3, radius * 0.4, 0.02);
            for (Player p : GateFx.playersNear(this, spot, radius)) {
                if (p.hurt(damageSources().indirectMagic(this, this), dmg(times)) && extra != null) extra.accept(p);
            }
        });
    }

    private void beam(ServerLevel lv, LivingEntity t, double times, ParticleOptions fx, Consumer<Player> extra) {
        Vec3 from = position().add(0, getBbHeight() * 0.7, 0), to = t.position().add(0, 1, 0);
        for (int i = 1; i <= 10; i++) {
            Vec3 at = from.lerp(to, i / 10.0);
            lv.sendParticles(fx, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        if (times > 0 && t instanceof Player p && hasLineOfSight(t) && p.hurt(damageSources().indirectMagic(this, this), dmg(times)) && extra != null) extra.accept(p);
    }

    private void perform(ServerLevel lv, MobMove m, LivingEntity t) {
        Vec3 flat = t.position().subtract(position()).multiply(1, 0, 1);
        Vec3 dir = flat.lengthSqr() < 0.01 ? getLookAngle().multiply(1, 0, 1) : flat.normalize();
        switch (m) {
            case STAB, SPEAR_THRUST -> hitCone(lv, dir, m == MobMove.STAB ? 3.2 : 4.6, m == MobMove.STAB ? 1.1 : 1.2, 0.3, ParticleTypes.CRIT, null);
            case BLEED_BITE -> hitCone(lv, dir, 3, 1.0, 0.2, ParticleTypes.DAMAGE_INDICATOR, p -> p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0)));
            case VENOM_BITE -> hitCone(lv, dir, 3, 1.1, 0.2, ParticleTypes.ITEM_SLIME, p -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0)));
            case POISON_BLADE -> hitCone(lv, dir, 3, 1.2, 0.2, ParticleTypes.ITEM_SLIME, p -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)));
            case FLAME_SLASH -> hitCone(lv, dir, 3.6, 1.1, 0.6, ParticleTypes.FLAME, p -> p.setSecondsOnFire(4));
            case CHOMP -> hitCone(lv, dir, 3, 1.5, 0.3, ParticleTypes.CRIT, null);
            case SHIELD_BASH -> hitCone(lv, dir, 2.8, 0.8, 1.1, ParticleTypes.POOF, p -> slow(p, 40, 2));
            case SWEEP, TAIL_SWEEP, AXE_SWEEP -> hitRing(lv, m == MobMove.AXE_SWEEP ? 3.8 : 3.4, m == MobMove.AXE_SWEEP ? 1.2 : 0.9, 0.8, 0.3, ParticleTypes.SWEEP_ATTACK, null);
            case SLAM -> hitRing(lv, 3.6, 1.0, 0.9, 0.5, ParticleTypes.POOF, null);
            case STOMP -> hitRing(lv, 4.6, 1.1, 1.0, 0.5, ParticleTypes.POOF, p -> slow(p, 50, 1));
            case FROST_ROAR -> hitRing(lv, 6.5, 0.6, 0.6, 0.3, ParticleTypes.SNOWFLAKE, p -> {
                p.setTicksFrozen(p.getTicksRequiredToFreeze() + 60);
                slow(p, 60, 1);
            });
            case SPORE_CLOUD -> hitRing(lv, 4, 0.4, 0.2, 0.1, ParticleTypes.ITEM_SLIME, p -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)));
            case SPORE_SLEEP -> hitRing(lv, 4, 0.2, 0.1, 0.1, ParticleTypes.CLOUD, p -> {
                slow(p, 80, 2);
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            });
            case FRENZY -> {
                for (int i = 0; i < 3; i++) {
                    in(i * 4, () -> {
                        LivingEntity now = getTarget();
                        if (now != null && distanceTo(now) <= 3.4) now.hurt(damageSources().mobAttack(this), dmg(0.5));
                    });
                }
            }
            case LEAP, POUNCE, DIVE, DASH -> {
                double far = Math.min(flat.length(), m == MobMove.POUNCE ? 7 : 10), div = m == MobMove.DASH ? 6 : 10;
                setDeltaMovement(dir.x * far / div, m == MobMove.POUNCE ? 0.35 : m == MobMove.DIVE ? 0.5 : 0.55, dir.z * far / div);
                hurtMarked = true;
                leapTicks = 24;
                leapHit = false;
            }
            case CHARGE, CHARGE_WALL -> {
                chargeLeft = 14;
                hitByCharge = false;
            }
            case WAR_CRY, HOWL -> {
                playSound(m == MobMove.HOWL ? SoundEvents.WOLF_HOWL : SoundEvents.RAVAGER_ROAR, 1.5f, 1f);
                GateFx.ring(lv, position(), 5, ParticleTypes.NOTE, 16);
                buff(this, 160, m == MobMove.WAR_CRY);
                for (GateMob a : allies(12)) buff(a, 160, m == MobMove.WAR_CRY);
            }
            case SWARM -> {
                buff(this, 120, true);
                for (GateMob a : allies(7)) buff(a, 120, true);
            }
            case HEAL_ALLIES -> {
                for (GateMob a : allies(8)) {
                    a.heal(6);
                    GateFx.burst(lv, a.position().add(0, 1, 0), ParticleTypes.HAPPY_VILLAGER, 5, 0.3);
                }
            }
            case SHIELD_BLOCK -> blockUntil = lv.getGameTime() + 80;
            case STONE_SKIN -> {
                skinUntil = lv.getGameTime() + 80;
                GateFx.burst(lv, position().add(0, 1, 0), ParticleTypes.POOF, 10, 0.5);
            }
            case RAISE_DEAD -> {
                for (int i = 0; i < 2; i++) {
                    if (!(GateMobs.mobType(GateKind.SKELETON_KNIGHT).create(lv) instanceof GateMob s)) continue;
                    s.moveTo(getX() + (i == 0 ? 2 : -2), getY(), getZ(), getYRot(), 0);
                    s.finalizeSpawn(lv, lv.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                    s.setPersistenceRequired();
                    s.setTarget(t);
                    lv.addFreshEntity(s);
                    GateFx.burst(lv, s.position(), ParticleTypes.SOUL, 8, 0.3);
                }
            }
            case ROCK_THROW -> shot(lv, t, m, 0.8, 1.3, ParticleTypes.CRIT, null);
            case BOULDER -> shot(lv, t, m, 1.4, 2.2, ParticleTypes.POOF, p -> slow(p, 30, 0));
            case FIRE_BOLT, FIREBALL -> shot(lv, t, m, m == MobMove.FIREBALL ? 1.1 : 0.9, 1.5, ParticleTypes.FLAME, p -> p.setSecondsOnFire(3));
            case ACID_SPIT -> shot(lv, t, m, 0.8, 1.6, ParticleTypes.ITEM_SLIME, p -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0)));
            case POISON_SPIT -> shot(lv, t, m, 0.9, 1.6, ParticleTypes.ITEM_SLIME, p -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)));
            case SPEAR_THROW -> shot(lv, t, m, 1.2, 1.2, ParticleTypes.CRIT, null);
            case SHADOW_BOLT -> shot(lv, t, m, 1.0, 1.5, ParticleTypes.SOUL, p -> p.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0)));
            case VOID_BOLT -> shot(lv, t, m, 1.1, 1.5, ParticleTypes.PORTAL, p -> p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0)));
            case GRENADE -> shot(lv, t, m, 1.3, 3.0, ParticleTypes.SMOKE, p -> slow(p, 30, 0));
            case WEB_SHOT -> shot(lv, t, m, 0.4, 1.6, ParticleTypes.CLOUD, p -> {
                slow(p, 100, 3);
                BlockPos at = p.blockPosition();
                if (lv.getBlockState(at).isAir()) {
                    lv.setBlock(at, Blocks.COBWEB.defaultBlockState(), 2 | 16);
                    webs.add(at);
                    in(160, () -> clearWeb(lv, at));
                    if (webs.size() > 5) clearWeb(lv, webs.get(0));
                }
            });
            case LASER -> beam(lv, t, 1.2, ParticleTypes.ELECTRIC_SPARK, null);
            case RIFLE_BURST -> {
                for (int i = 0; i < 3; i++) {
                    in(i * 4, () -> {
                        LivingEntity now = getTarget();
                        if (now != null) beam(lv, now, 0.45, ParticleTypes.CRIT, null);
                        playSound(SoundEvents.CROSSBOW_SHOOT, 1f, 1.4f);
                    });
                }
            }
            case LIFE_DRAIN -> {
                if (t instanceof Player p && distanceTo(p) <= 6.5 && hasLineOfSight(p)) {
                    float amount = dmg(0.9);
                    if (p.hurt(damageSources().indirectMagic(this, this), amount)) heal(amount * 0.8f);
                }
                beam(lv, t, 0, ParticleTypes.SOUL, null);
            }
            case CURSE -> {
                if (t instanceof Player p) {
                    p.getPersistentData().putLong(CURSE, lv.getGameTime() + 400); // healing is halved until then (GateEvents)
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    GateFx.burst(lv, p.position().add(0, 1, 0), ParticleTypes.SCULK_SOUL, 12, 0.5);
                    p.displayClientMessage(Component.literal("You are cursed: you heal slower."), true);
                }
            }
            case RAPID_VOLLEY, ICE_ARROW -> {
                int n = m == MobMove.ICE_ARROW ? 2 : 3;
                for (int i = 0; i < n; i++) {
                    Arrow arrow = new Arrow(lv, this);
                    arrow.setPos(getX(), getEyeY() - 0.2, getZ());
                    Vec3 aim = t.getEyePosition().subtract(arrow.position()).yRot((float) ((i - (n - 1) / 2.0) * 0.14));
                    arrow.shoot(aim.x, aim.y, aim.z, 1.5f, 1.5f);
                    arrow.setBaseDamage(dmg(0.45));
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    if (m == MobMove.ICE_ARROW) arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    lv.addFreshEntity(arrow);
                }
                playSound(SoundEvents.SKELETON_SHOOT, 1f, 1.3f);
            }
            case DODGE_ROLL -> {
                Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(getRandom().nextBoolean() ? 1 : -1);
                setDeltaMovement(side.x * 0.9, 0.25, side.z * 0.9);
                hurtMarked = true;
            }
            case RETREAT -> {
                setDeltaMovement(-dir.x * 1.0, 0.3, -dir.z * 1.0);
                hurtMarked = true;
            }
            case BLINK_BEHIND -> {
                GateFx.burst(lv, position().add(0, 1, 0), ParticleTypes.SMOKE, 12, 0.4);
                Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(1.6));
                randomTeleport(behind.x, t.getY(), behind.z, true);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1.3f);
            }
            case TELEPORT -> {
                GateFx.burst(lv, position().add(0, 1, 0), ParticleTypes.PORTAL, 16, 0.4);
                for (int tries = 0; tries < 6; tries++) {
                    double a = getRandom().nextDouble() * Math.PI * 2, r = 4 + getRandom().nextInt(4);
                    if (randomTeleport(t.getX() + Math.cos(a) * r, t.getY(), t.getZ() + Math.sin(a) * r, true)) break;
                }
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 0.8f);
            }
            case BURROW -> {
                burrowTicks = 30;
                setInvisible(true);
            }
            case PHASE -> {
                phaseTicks = 40;
                noPhysics = true;
                setNoGravity(true);
            }
            case COIL_GRAB, GRAB -> {
                if (t instanceof Player p && distanceTo(p) <= 5) {
                    Vec3 pull = position().subtract(p.position()).multiply(1, 0, 1).scale(0.25);
                    p.setDeltaMovement(pull.x, 0.1, pull.z);
                    p.hurtMarked = true;
                    p.hurt(damageSources().mobAttack(this), dmg(0.6));
                    slow(p, 80, 3);
                }
            }
            case TONGUE_PULL -> {
                if (t instanceof Player p) {
                    beam(lv, t, 0.5, ParticleTypes.CRIMSON_SPORE, null);
                    Vec3 pull = position().subtract(p.position()).multiply(1, 0, 1).scale(0.35);
                    p.setDeltaMovement(pull.x, 0.2, pull.z);
                    p.hurtMarked = true;
                }
            }
            case SAND_BLAST -> hitCone(lv, dir, 8, 0.7, 0.4, ParticleTypes.CLOUD, p -> {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
                slow(p, 60, 1);
            });
        }
    }

    private void buff(GateMob who, int ticks, boolean strength) {
        who.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1));
        if (strength) who.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0));
    }

    private void clearWeb(ServerLevel lv, BlockPos at) {
        webs.remove(at);
        if (lv.getBlockState(at).is(Blocks.COBWEB)) lv.setBlock(at, Blocks.AIR.defaultBlockState(), 2 | 16);
    }

    // ---- moves that carry on for a while

    private void runCharge(ServerLevel lv, long now) {
        chargeLeft--;
        setDeltaMovement(chargeDir.x * 1.05, getDeltaMovement().y, chargeDir.z * 1.05);
        hurtMarked = true;
        GateFx.burst(lv, position(), ParticleTypes.CLOUD, 2, 0.3);
        if (!hitByCharge) {
            for (Player p : GateFx.playersNear(this, position(), 1.8)) {
                hitByCharge = true;
                p.hurt(damageSources().mobAttack(this), dmg(1.3));
                p.setDeltaMovement(chargeDir.x * 1.4, 0.5, chargeDir.z * 1.4);
                p.hurtMarked = true;
                if (kind == GateKind.MINOTAUR) chargeLeft = 0;
            }
        }
        if (horizontalCollision && chargeLeft < 12 && kind == GateKind.MINOTAUR) { // into the wall: stunned, a chance to hit back
            chargeLeft = 0;
            stunUntil = now + 70;
            lv.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1f, 0.7f);
            GateFx.burst(lv, position().add(0, getBbHeight(), 0), ParticleTypes.CRIT, 12, 0.5);
        }
    }

    private void runLeap(ServerLevel lv) {
        leapTicks--;
        if (!leapHit) {
            for (Player p : GateFx.playersNear(this, position(), 1.7)) {
                leapHit = true;
                p.hurt(damageSources().mobAttack(this), dmg(1.2));
                p.setDeltaMovement(p.getDeltaMovement().add(getDeltaMovement().x * 0.6, 0.3, getDeltaMovement().z * 0.6));
                p.hurtMarked = true;
            }
        }
        if (leapTicks < 18 && (onGround() || leapHit)) leapTicks = 0;
    }

    private void runPhase(ServerLevel lv) {
        LivingEntity t = getTarget();
        if (t != null) {
            Vec3 d = t.position().subtract(position()).normalize();
            setDeltaMovement(d.scale(0.22));
        }
        if (tickCount % 3 == 0) GateFx.burst(lv, position().add(0, 1, 0), ParticleTypes.SOUL, 2, 0.3);
        if (--phaseTicks <= 0) {
            noPhysics = false;
            if (!level().noCollision(this) && t != null) randomTeleport(t.getX(), t.getY(), t.getZ(), true);
        }
    }

    private void popUp(ServerLevel lv) {
        setInvisible(false);
        LivingEntity t = getTarget();
        if (t != null) randomTeleport(t.getX() + 0.8, t.getY(), t.getZ(), true);
        lv.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.2f, 0.8f);
        hitRing(lv, 2.6, 1.0, 0.6, 0.5, ParticleTypes.POOF, null);
    }

    // ---- being hit

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        Entity by = source.getEntity();
        if (by instanceof GateMob || by instanceof GateBoss) return false; // creatures of a gate never hurt each other
        if (dormant) wake();
        long now = level().getGameTime();
        if (burrowTicks > 0) return false;
        float mul = 1f;
        if (fromFront(source)) mul *= blockUntil > now ? 0.15f : SHIELDED.contains(kind.id) ? 0.5f : 1f;
        if (skinUntil > now) mul *= 0.35f;
        if (stunUntil > now) mul *= 1.5f;
        return super.hurt(source, amount * mul);
    }

    private boolean fromFront(DamageSource source) {
        Vec3 from = source.getSourcePosition();
        if (from == null) return false;
        Vec3 to = from.subtract(position()).multiply(1, 0, 1);
        return to.lengthSqr() > 0.01 && to.normalize().dot(getViewVector(1f).multiply(1, 0, 1).normalize()) > 0.3;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel lv) {
            for (BlockPos at : new ArrayList<>(webs)) clearWeb(lv, at);
        }
        super.remove(reason);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), power);
        double dx = target.getX() - getX(), dz = target.getZ() - getZ(), dy = target.getY(0.3333) - arrow.getY();
        arrow.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.6f, 12f);
        arrow.setBaseDamage(getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        playSound(SoundEvents.SKELETON_SHOOT, 1f, 1f / (getRandom().nextFloat() * 0.4f + 0.8f));
        level().addFreshEntity(arrow);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return kind.ambient;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return kind.hurt;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return kind.death;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // for tests: runs a move at once, as if its tell had just finished
    public void runMoveNow(MobMove m, LivingEntity target) {
        perform((ServerLevel) level(), m, target);
    }
}
