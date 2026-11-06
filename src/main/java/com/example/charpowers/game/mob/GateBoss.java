package com.example.charpowers.game.mob;

import com.example.charpowers.game.model.BossRules;
import com.example.charpowers.game.model.BossRules.Move;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

// The bosses of gates. Three phases (a third of the health each), a boss bar for the whole party, and 3 or 4 special attacks that each
// show a tell first (arms go up, a ring or a line of particles, a sound) before they land, so they can be dodged.
public class GateBoss extends Monster {
    private static final EntityDataAccessor<Integer> TELL = SynchedEntityData.defineId(GateBoss.class, EntityDataSerializers.INT);
    private static final UUID RAGE = UUID.fromString("0d1c6b0e-52f6-4b0e-8a5c-93d6f1a0c001");
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.8f, 0.05f, 0.1f), 1.6f);
    private static final DustParticleOptions ACID = new DustParticleOptions(new Vector3f(0.4f, 0.9f, 0.1f), 1.6f);

    public final GateKind kind;
    private final ServerBossEvent bar;
    private final Set<UUID> party = new HashSet<>();
    private final List<Mob> minions = new ArrayList<>();
    private final List<Object[]> later = new ArrayList<>(); // {Integer dueTick, Runnable}
    private int phase = 1, windup, cooldown = 70, chargeLeft, leapTicks, swoopUp;
    private long shieldUntil;
    private Move current;
    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<UUID> chargeHit = new HashSet<>();
    private Consumer<Mob> onSummon = m -> {};
    private Runnable onDeath = () -> {};

    public GateBoss(EntityType<? extends GateBoss> type, Level level, GateKind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = 50;
        this.bar = new ServerBossEvent(Component.literal(kind.title.toUpperCase()), colour(kind), BossEvent.BossBarOverlay.NOTCHED_6);
        initGoals();
    }

    private static BossEvent.BossBarColor colour(GateKind kind) {
        return switch (kind) {
            case GOBLIN_KING -> BossEvent.BossBarColor.GREEN;
            case HOBGOBLIN_WARLORD -> BossEvent.BossBarColor.YELLOW;
            case ELF_MAGE, FROST_KING -> BossEvent.BossBarColor.BLUE;
            case ANT_QUEEN, LICH_LORD -> BossEvent.BossBarColor.PURPLE;
            case SNAKE_QUEEN, YOUNG_DRAGON, WOLF_KING -> BossEvent.BossBarColor.GREEN;
            case TOMB_KING -> BossEvent.BossBarColor.YELLOW;
            case WAR_MECH -> BossEvent.BossBarColor.WHITE;
            default -> BossEvent.BossBarColor.RED;
        };
    }

    public static AttributeSupplier.Builder attributes(GateKind kind) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, kind.health).add(Attributes.ATTACK_DAMAGE, kind.damage).add(Attributes.MOVEMENT_SPEED, kind.speed)
                .add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, kind.armor).add(Attributes.KNOCKBACK_RESISTANCE, kind.knockback);
    }

    @Override
    protected void registerGoals() {}

    private void initGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (kind == GateKind.ELF_MAGE || kind == GateKind.LICH_LORD) goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 7f, 1.0, 1.3));
        else goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TELL, 0);
    }

    // 0 = nothing, else the attack's number + 1: the client raises the arms
    public int tell() {
        return entityData.get(TELL);
    }

    // Makes a boss: health and damage by the gate's rank and how many are in the party.
    public static GateBoss spawn(ServerLevel level, GateKind kind, Vec3 at, int rank, int partySize, Consumer<Mob> onSummon, Runnable onDeath) {
        EntityType<? extends GateBoss> type = GateMobs.bossType(kind);
        GateBoss boss = new GateBoss(type, level, kind);
        boss.onSummon = onSummon;
        boss.onDeath = onDeath;
        boss.moveTo(at.x, at.y, at.z, 0f, 0f);
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BossRules.health(kind.health, rank, partySize));
        boss.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BossRules.damage(kind.damage, rank, partySize));
        boss.setHealth(boss.getMaxHealth());
        boss.setPersistenceRequired();
        boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(switch (kind) {
            case HOBGOBLIN_WARLORD -> Items.IRON_AXE;
            case ELF_MAGE, LICH_LORD -> Items.END_ROD;
            case DEMON_LORD -> Items.NETHERITE_SWORD;
            case TOMB_KING -> Items.GOLDEN_SWORD;
            case FROST_KING -> Items.DIAMOND_SWORD;
            case GOBLIN_KING -> Items.GOLDEN_SWORD;
            default -> Items.AIR;
        }));
        boss.setDropChance(EquipmentSlot.MAINHAND, 0f);
        level.addFreshEntity(boss);
        return boss;
    }

    public void addToParty(ServerPlayer p) {
        party.add(p.getUUID());
        bar.addPlayer(p);
    }

    public void removeFromParty(ServerPlayer p) {
        party.remove(p.getUUID());
        bar.removePlayer(p);
    }

    public int phase() {
        return phase;
    }

    // ---- the fight

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || (windup > 0 && current != Move.CHARGE);
    }

    @Override
    public boolean doHurtTarget(Entity victim) {
        if (windup > 0) return false;
        boolean hit = super.doHurtTarget(victim);
        if (hit && kind == GateKind.ANT_QUEEN && victim instanceof LivingEntity l) l.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.setProgress(getHealth() / getMaxHealth());
        ServerLevel level = (ServerLevel) level();
        for (Iterator<Object[]> it = later.iterator(); it.hasNext(); ) { // things set going earlier (rings, blasts) that land now
            Object[] job = it.next();
            if (tickCount >= (Integer) job[0]) {
                it.remove();
                ((Runnable) job[1]).run();
            }
        }
        minions.removeIf(m -> !m.isAlive());
        int nowPhase = BossRules.phase(getHealth() / getMaxHealth());
        if (nowPhase > phase) enterPhase(nowPhase);
        LivingEntity target = getTarget();
        if (chargeLeft > 0) {
            chargeLeft--;
            setDeltaMovement(chargeDir.x * 1.05, getDeltaMovement().y, chargeDir.z * 1.05);
            hurtMarked = true;
            for (Player p : GateFx.playersNear(this, position(), 2.0)) {
                if (chargeHit.add(p.getUUID())) {
                    p.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * (current == Move.BITE ? 1.3f : 1.2f));
                    p.setDeltaMovement(chargeDir.x * 1.4, 0.5, chargeDir.z * 1.4);
                    p.hurtMarked = true;
                    if (current == Move.BITE) p.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                }
            }
            GateFx.burst(level, position(), ParticleTypes.CLOUD, 3, 0.3);
        }
        if (swoopUp > 0) { // a flying swoop: up first, then down on the target
            setDeltaMovement(0, 0.55, 0);
            hurtMarked = true;
            if (--swoopUp == 0 && target != null) {
                Vec3 d = target.position().subtract(position()).multiply(1, 0, 1);
                double far = Math.min(d.length(), 18);
                Vec3 n = d.lengthSqr() < 0.01 ? Vec3.ZERO : d.normalize();
                setDeltaMovement(n.x * far / 12, -0.2, n.z * far / 12);
                hurtMarked = true;
                leapTicks = 40;
            }
            return;
        }
        if (leapTicks > 0 && --leapTicks < 16 && onGround()) {
            leapTicks = 0;
            slam(level);
        }
        if (target == null || !target.isAlive()) return;
        if (windup > 0) {
            getNavigation().stop();
            getLookControl().setLookAt(target, 40f, 40f);
            windup--;
            tellFx(level, target);
            if (windup == 0) {
                entityData.set(TELL, 0);
                Move done = current;
                perform(level, done, target);
                cooldown = Math.max(18, 70 - 14 * (phase - 1) + getRandom().nextInt(20));
            }
            return;
        }
        if (--cooldown > 0) return;
        Move next = BossRules.pick(kind.name(), phase, distanceTo(target), minions.size(), getRandom()::nextInt);
        if (next == null) {
            cooldown = 10;
            return;
        }
        current = next;
        windup = next.windup;
        entityData.set(TELL, next.ordinal() + 1);
        playSound(kind == GateKind.ELF_MAGE || kind == GateKind.LICH_LORD ? SoundEvents.EVOKER_PREPARE_SUMMON : SoundEvents.RAVAGER_ROAR, 1.2f, 0.8f + phase * 0.1f);
        if (next == Move.CHARGE || next == Move.BITE) {
            Vec3 d = target.position().subtract(position()).multiply(1, 0, 1);
            chargeDir = d.lengthSqr() < 0.01 ? Vec3.ZERO : d.normalize();
        }
    }

    private void enterPhase(int to) {
        phase = to;
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 1.1f);
        GateFx.burst((ServerLevel) level(), position().add(0, 1, 0), ParticleTypes.FLAME, 30, 0.8);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(RAGE);
        speed.addTransientModifier(new AttributeModifier(RAGE, "rage", 0.1 * (phase - 1), AttributeModifier.Operation.MULTIPLY_BASE));
        cooldown = Math.min(cooldown, 30);
        for (UUID id : party) {
            ServerPlayer p = getServer() == null ? null : getServer().getPlayerList().getPlayer(id);
            if (p != null) p.displayClientMessage(Component.literal(kind.title + " is enraged! (phase " + phase + ")"), true);
        }
    }

    private void tellFx(ServerLevel level, LivingEntity target) {
        double t = 1 - windup / (double) current.windup;
        switch (current) {
            case CHARGE, BITE -> {
                for (int i = 1; i <= 10; i++) {
                    Vec3 at = position().add(chargeDir.scale(i));
                    level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z, 1, 0.1, 0, 0.1, 0);
                }
            }
            case NOVA -> GateFx.ring(level, position(), 6 * t, novaFx(), 28);
            case BREATH -> {
                Vec3 d = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                for (int i = 1; i <= 7; i++) {
                    Vec3 at = position().add(d.scale(i)).add(0, 1.2, 0);
                    level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 2, i * 0.08, 0.1, i * 0.08, 0);
                }
            }
            case GUST, TAILSWEEP -> GateFx.ring(level, position(), (current == Move.GUST ? 9 : 5) * t, ParticleTypes.CLOUD, 24);
            case SANDSTORM, CURSE_ALL -> GateFx.ring(level, position(), 10 * t, current == Move.SANDSTORM ? ParticleTypes.CLOUD : ParticleTypes.SCULK_SOUL, 28);
            case SHIELD -> GateFx.burst(level, position().add(0, 1.5, 0), ParticleTypes.ELECTRIC_SPARK, 8, 0.8);
            case SWOOP -> GateFx.ring(level, target.position(), 3, ParticleTypes.CRIT, 20);
            case ROAR -> GateFx.ring(level, position(), 8 * t, ParticleTypes.SMOKE, 28);
            case CLEAVE, SLASH3, DASH -> GateFx.burst(level, position().add(getLookAngle().scale(2)).add(0, 0.8, 0), ParticleTypes.CRIT, 6, 0.6);
            case WAVE -> {
                Vec3 d = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                for (int i = 1; i <= 12; i += 2) {
                    Vec3 at = position().add(d.scale(i));
                    level.sendParticles(BLOOD, at.x, at.y + 0.2, at.z, 1, 0.1, 0, 0.1, 0);
                }
            }
            case WARCRY, SUMMON -> GateFx.ring(level, position(), 3 * t, kind == GateKind.ANT_QUEEN ? ACID : ParticleTypes.ANGRY_VILLAGER, 16);
            case LEAP -> GateFx.ring(level, target.position(), 3, ParticleTypes.CRIT, 20);
            case SPIT, METEORS -> GateFx.burst(level, position().add(0, 2.4, 0), ParticleTypes.ENCHANT, 8, 0.5);
            default -> GateFx.burst(level, position().add(0, 1.2, 0), ParticleTypes.PORTAL, 10, 0.5);
        }
    }

    private float damage(double times) {
        return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * times);
    }

    private void in(int ticks, Runnable r) {
        later.add(new Object[]{tickCount + ticks, r});
    }

    private void perform(ServerLevel level, Move atk, LivingEntity target) {
        Vec3 flat = target.position().subtract(position()).multiply(1, 0, 1);
        Vec3 dir = flat.lengthSqr() < 0.01 ? getLookAngle().multiply(1, 0, 1) : flat.normalize();
        switch (atk) {
            case LEAP -> {
                double far = Math.min(flat.length(), 16);
                setDeltaMovement(dir.x * far / 18, 0.95, dir.z * far / 18);
                hurtMarked = true;
                leapTicks = 40;
            }
            case ROAR -> {
                level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 0.7f);
                GateFx.ring(level, position(), 8, ParticleTypes.CLOUD, 40);
                for (Player p : GateFx.playersNear(this, position(), 8)) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                GateFx.hit(this, position(), 8, damageSources().mobAttack(this), damage(0.5), 1.3, 0.35);
            }
            case SUMMON, WARCRY -> {
                GateKind helper = helperFor();
                int n = 2 + phase;
                for (int i = 0; i < n; i++) summon(level, helper, target);
                if (atk == Move.WARCRY) {
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
                    for (Mob m : minions) m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                }
                level.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.5f, 0.8f);
            }
            case CHARGE -> {
                chargeLeft = 14;
                chargeHit.clear();
            }
            case BITE -> {
                chargeLeft = 9;
                chargeHit.clear();
            }
            case CLEAVE -> cone(level, dir, 5.5, damage(1.4), 1.2, ParticleTypes.SWEEP_ATTACK);
            case VOLLEY -> {
                int n = 3 + phase;
                for (int i = 0; i < n; i++) {
                    Arrow arrow = new Arrow(level, this);
                    arrow.setPos(getX(), getEyeY() - 0.2, getZ());
                    double spread = (i - (n - 1) / 2.0) * 0.16;
                    Vec3 aim = target.getEyePosition().subtract(arrow.position());
                    Vec3 turned = aim.yRot((float) spread);
                    arrow.shoot(turned.x, turned.y, turned.z, 1.5f, 1f);
                    arrow.setBaseDamage(damage(0.45));
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    arrow.setGlowingTag(true);
                    if (kind == GateKind.FROST_KING) arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    level.addFreshEntity(arrow);
                }
                playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5f, 1.4f);
            }
            case NOVA -> {
                level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2f, 0.5f);
                GateFx.ring(level, position(), 6, novaFx(), 40);
                for (Player p : GateFx.playersNear(this, position(), 6)) {
                    p.setTicksFrozen(p.getTicksRequiredToFreeze() + 80);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                }
                GateFx.hit(this, position(), 6, damageSources().mobAttack(this), damage(1.0), 1.0, 0.4);
            }
            case BLINK -> {
                GateFx.burst(level, position().add(0, 1, 0), ParticleTypes.PORTAL, 30, 0.5);
                for (int tries = 0; tries < 8; tries++) {
                    double a = getRandom().nextDouble() * Math.PI * 2, r = 8 + getRandom().nextInt(5);
                    if (randomTeleport(target.getX() + Math.cos(a) * r, target.getY(), target.getZ() + Math.sin(a) * r, true)) break;
                }
                GateFx.burst(level, position().add(0, 1, 0), ParticleTypes.PORTAL, 30, 0.5);
            }
            case METEORS -> {
                for (int i = 0; i < 4 + phase; i++) {
                    Vec3 at = target.position().add((getRandom().nextDouble() - 0.5) * 10, 0, (getRandom().nextDouble() - 0.5) * 10);
                    if (i == 0) at = target.position();
                    Vec3 where = at;
                    mark(level, where, 3, 40, kind == GateKind.FROST_KING ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, () -> {
                        GateFx.burst(level, where.add(0, 0.5, 0), ParticleTypes.EXPLOSION, 4, 1.2);
                        level.playSound(null, BlockPosBridge.of(where), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2f, 1f);
                        GateFx.hit(this, where, 3, damageSources().mobAttack(this), damage(1.3), 1.0, 0.6);
                    });
                }
            }
            case SPIT -> {
                for (int i = 0; i < phase; i++) {
                    Vec3 at = target.position().add(i == 0 ? Vec3.ZERO : new Vec3((getRandom().nextDouble() - 0.5) * 8, 0, (getRandom().nextDouble() - 0.5) * 8));
                    mark(level, at, 3, 30, ACID, () -> {
                        GateFx.burst(level, at.add(0, 0.3, 0), ParticleTypes.SPIT, 20, 1.2);
                        for (Player p : GateFx.playersNear(this, at, 3)) p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                        GateFx.hit(this, at, 3, damageSources().mobAttack(this), damage(0.9), 0.3, 0.3);
                    });
                }
                playSound(SoundEvents.LLAMA_SPIT, 1.5f, 0.6f);
            }
            case SLASH3 -> {
                for (int i = 0; i < 3; i++) {
                    in(i * 6, () -> {
                        LivingEntity t = getTarget();
                        Vec3 d = t == null ? dir : t.position().subtract(position()).multiply(1, 0, 1).normalize();
                        setDeltaMovement(d.x * 0.7, 0.1, d.z * 0.7);
                        hurtMarked = true;
                        cone(level, d, 4.2, damage(0.7), 0.5, ParticleTypes.SWEEP_ATTACK);
                        level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2f, 0.7f);
                    });
                }
            }
            case WAVE -> {
                level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1f, 1.4f);
                Set<UUID> hit = new HashSet<>();
                for (int i = 1; i <= 14; i++) {
                    int step = i;
                    in(step, () -> {
                        Vec3 at = position().add(dir.scale(step));
                        level.sendParticles(BLOOD, at.x, at.y + 0.3, at.z, 6, 0.8, 0.3, 0.8, 0);
                        for (Player p : GateFx.playersNear(this, at, 1.6)) {
                            if (hit.add(p.getUUID())) {
                                p.hurt(damageSources().mobAttack(this), damage(1.1));
                                p.setDeltaMovement(0, 0.6, 0);
                                p.hurtMarked = true;
                            }
                        }
                    });
                }
            }
            case BREATH -> {
                level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 2f, 0.8f);
                for (int i = 0; i < 10; i++) {
                    int step = i;
                    in(step, () -> {
                        for (int k = 1; k <= 8; k++) {
                            Vec3 at = position().add(dir.scale(k)).add(0, 1.2, 0);
                            level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2, k * 0.1, 0.15, k * 0.1, 0.01);
                        }
                    });
                }
                cone(level, dir, 8.5, damage(1.4), 0.6, ParticleTypes.LAVA);
                for (Player p : GateFx.playersNear(this, position(), 8.5)) {
                    Vec3 to = p.position().subtract(position()).multiply(1, 0, 1);
                    if (to.lengthSqr() > 0.01 && to.normalize().dot(dir) >= 0.2) p.setSecondsOnFire(4);
                }
            }
            case GUST -> {
                level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3f, 0.8f);
                GateFx.ring(level, position(), 9, ParticleTypes.CLOUD, 40);
                GateFx.hit(this, position(), 9, damageSources().mobAttack(this), damage(0.5), 2.0, 0.5);
            }
            case TAILSWEEP -> {
                level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5f, 0.5f);
                GateFx.ring(level, position(), 5, ParticleTypes.SWEEP_ATTACK, 20);
                GateFx.hit(this, position(), 5, damageSources().mobAttack(this), damage(1.0), 1.4, 0.4);
            }
            case SWOOP -> swoopUp = 14;
            case SANDSTORM -> {
                for (int pulse = 0; pulse < 3; pulse++) {
                    in(pulse * 10, () -> {
                        GateFx.ring(level, position(), 9, ParticleTypes.CLOUD, 40);
                        for (Player p : GateFx.playersNear(this, position(), 9)) {
                            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                            p.hurt(damageSources().mobAttack(this), damage(0.35));
                        }
                    });
                }
            }
            case CURSE_ALL -> {
                for (Player p : GateFx.playersNear(this, position(), 12)) {
                    p.getPersistentData().putLong(GateMob.CURSE, level.getGameTime() + 500);
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                    p.displayClientMessage(Component.literal("You are cursed: you heal slower."), true);
                    GateFx.burst(level, p.position().add(0, 1, 0), ParticleTypes.SCULK_SOUL, 10, 0.4);
                }
            }
            case SHIELD -> {
                shieldUntil = tickCount + 120;
                for (int i = 0; i < 2; i++) summon(level, GateKind.SENTRY_DRONE, target);
                level.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2f, 1.2f);
            }
            case DASH -> {
                GateFx.burst(level, position().add(0, 1, 0), ParticleTypes.SMOKE, 25, 0.5);
                Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(1.8));
                randomTeleport(behind.x, target.getY(), behind.z, true);
                level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 0.8f);
                Vec3 d = target.position().subtract(position()).multiply(1, 0, 1);
                cone(level, d.lengthSqr() < 0.01 ? dir : d.normalize(), 4.5, damage(1.0), 0.6, ParticleTypes.SWEEP_ATTACK);
            }
        }
    }

    // A ring on the ground that stays a while, then goes off.
    private void mark(ServerLevel level, Vec3 at, double radius, int delay, ParticleOptions look, Runnable boom) {
        for (int t = 0; t < delay; t += 6) {
            in(t, () -> GateFx.ring(level, at, radius, look, 22));
        }
        in(delay, boom);
    }

    private void cone(ServerLevel level, Vec3 dir, double reach, float amount, double push, ParticleOptions fx) {
        Vec3 tip = position().add(dir.scale(reach * 0.6)).add(0, 1, 0);
        level.sendParticles(fx, tip.x, tip.y, tip.z, 3, 0.8, 0.3, 0.8, 0);
        for (Player p : GateFx.playersNear(this, position(), reach)) {
            Vec3 to = p.position().subtract(position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 0.01 && to.normalize().dot(dir) < 0.2) continue;
            if (p.hurt(damageSources().mobAttack(this), amount)) {
                p.setDeltaMovement(p.getDeltaMovement().add(dir.x * push, 0.3, dir.z * push));
                p.hurtMarked = true;
            }
        }
    }

    private ParticleOptions novaFx() {
        return kind == GateKind.LICH_LORD ? ParticleTypes.SOUL : ParticleTypes.SNOWFLAKE;
    }

    // Who a boss calls to its side: the pack it would keep.
    private GateKind helperFor() {
        return switch (kind) {
            case GOBLIN_KING -> phase >= 3 ? GateKind.GOBLIN_SHAMAN : GateKind.GOBLIN;
            case ANT_QUEEN -> GateKind.ANT_SOLDIER;
            case WOLF_KING -> phase >= 3 ? GateKind.ALPHA_WOLF : GateKind.DIRE_WOLF;
            case DEMON_LORD -> phase >= 3 ? GateKind.DEMON_KNIGHT : getRandom().nextBoolean() ? GateKind.DEMON : GateKind.IMP;
            case LICH_LORD -> getRandom().nextBoolean() ? GateKind.SKELETON_KNIGHT : GateKind.GHOUL;
            case SNAKE_QUEEN -> getRandom().nextBoolean() ? GateKind.SNAKE_WARRIOR : GateKind.LIZARDMAN;
            case TOMB_KING -> getRandom().nextInt(3) == 0 ? GateKind.SCARAB : GateKind.MUMMY;
            case WAR_MECH -> GateKind.SENTRY_DRONE;
            default -> GateKind.ORC;
        };
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            if (source.getEntity() instanceof GateMob || source.getEntity() instanceof GateBoss) return false;
            if (shieldUntil > tickCount) { // the war mech's shield: nothing gets through, wait it out
                GateFx.burst((ServerLevel) level(), position().add(0, 1.5, 0), ParticleTypes.ELECTRIC_SPARK, 4, 0.8);
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    private void slam(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 0.7f);
        GateFx.ring(level, position(), 4, ParticleTypes.POOF, 30);
        GateFx.hit(this, position(), 4.5, damageSources().mobAttack(this), damage(1.1), 1.1, 0.5);
    }

    private void summon(ServerLevel level, GateKind helper, LivingEntity target) {
        EntityType<?> type = GateMobs.mobType(helper);
        if (!(type.create(level) instanceof GateMob mob)) return;
        double a = getRandom().nextDouble() * Math.PI * 2;
        mob.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, getRandom().nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        mob.setPersistenceRequired();
        mob.setTarget(target);
        level.addFreshEntity(mob);
        GateFx.burst(level, mob.position(), ParticleTypes.POOF, 8, 0.3);
        minions.add(mob);
        onSummon.accept(mob);
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
    protected SoundEvent getAmbientSound() {
        return kind.ambient;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) return;
        minions.forEach(Mob::discard);
        later.clear();
        onDeath.run();
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

    // Only so the code above can say "a block position" for a place in the room
    private static final class BlockPosBridge {
        static net.minecraft.core.BlockPos of(Vec3 v) {
            return net.minecraft.core.BlockPos.containing(v);
        }
    }
}
