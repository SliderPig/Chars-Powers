package com.example.charpowers.necromancy;

import com.example.charpowers.CharPowers;
import com.example.charpowers.necromancy.model.UndeadRules;
import com.example.charpowers.own.Minions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

// Everyone's summons, on top of own/Minions (which stops them hunting and has them follow). This adds what's special about
// the undead: the light, no drops, crumbling, and fast arrows. Looked after a couple of times a second, only while there are any.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class Undead {
    static final String KIND = "undead";
    private static final String LEVEL = "charpowers.undead.level", UNTIL = "charpowers.undead.until", REVIVED = "charpowers.undead.revived";
    private static final UUID SPEED_ID = UUID.fromString("0b6f7c1e-94d2-4a68-b1de-3f5a7c2e9d10");
    private static final AttributeModifier SUN_SLOW = new AttributeModifier(SPEED_ID, "charpowers sunlight", -0.3, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier DARK_FAST = new AttributeModifier(SPEED_ID, "charpowers dark", 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final int LET_GO_FROM = 48;

    private static final Deque<Mob> EXPLODING = new ArrayDeque<>();

    private Undead() {}

    static boolean isUndead(Entity e) {
        return Minions.isKind(e, KIND);
    }

    static UUID ownerOf(Mob m) {
        return Minions.ownerOf(m);
    }

    // Turn a new mob into a summon. Do this before it joins the world.
    static void claim(Mob m, ServerPlayer owner, int level, long until, boolean revived) {
        Minions.claim(m, owner, KIND);
        CompoundTag d = m.getPersistentData();
        d.putInt(LEVEL, level);
        if (until > 0) d.putLong(UNTIL, until);
        if (revived) d.putBoolean(REVIVED, true);
        AttributeInstance health = m.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(UndeadRules.health(level));
            m.setHealth((float) UndeadRules.health(level));
        }
    }

    static List<Mob> of(UUID owner) {
        return Minions.of(owner, KIND);
    }

    static int ordinaryCount(UUID owner) {
        int n = 0;
        for (Mob m : of(owner)) {
            if (!m.getPersistentData().contains(UNTIL) && !m.getPersistentData().getBoolean(REVIVED)) n++;
        }
        return n;
    }

    static List<Mob> revived(UUID owner) {
        List<Mob> out = new ArrayList<>();
        for (Mob m : of(owner)) {
            if (m.getPersistentData().getBoolean(REVIVED)) out.add(m);
        }
        return out;
    }

    static void pointAt(UUID owner, LivingEntity target) {
        Minions.pointAt(owner, KIND, target);
    }

    static void explodeAll(UUID owner) {
        EXPLODING.addAll(of(owner));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        for (int i = 0; i < UndeadRules.EXPLODE_PER_TICK && !EXPLODING.isEmpty(); i++) {
            Mob m = EXPLODING.poll();
            if (!m.isAlive()) continue;
            m.level().explode(m, m.getX(), m.getY() + 0.5, m.getZ(), UndeadRules.EXPLOSION, Level.ExplosionInteraction.NONE);
            m.discard();
        }
        int tick = e.getServer().getTickCount();
        if (Minions.noneLive() || tick % 10 != 0) return;
        for (Mob m : Minions.all(KIND)) lookAfter(m, tick);
    }

    private static void lookAfter(Mob m, int tick) {
        if (!(m.level() instanceof ServerLevel level)) return;
        CompoundTag d = m.getPersistentData();
        if (d.contains(UNTIL) && level.getGameTime() >= d.getLong(UNTIL)) {
            level.sendParticles(ParticleTypes.ASH, m.getX(), m.getY() + 1, m.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
            m.discard();
            return;
        }
        if (m.isOnFire() && !m.isInLava()) m.clearFire();
        LivingEntity target = m.getTarget();
        if (target != null && (!target.isAlive() || target.distanceToSqr(m) > LET_GO_FROM * LET_GO_FROM)) {
            m.setTarget(null);
            target = null;
        }
        // skeletons shoot 2 arrows a second from mastery 25
        if (m instanceof AbstractSkeleton s && target != null && d.getInt(LEVEL) >= UndeadRules.FAST_ARROWS_LEVEL
                && s.distanceToSqr(target) < 225 && s.hasLineOfSight(target)) {
            s.performRangedAttack(target, 1f);
        }
        if (tick % 40 == 0) light(m, level);
    }

    private static boolean sunlit(Mob m, ServerLevel level) {
        return level.isDay() && level.canSeeSky(m.blockPosition());
    }

    private static boolean dark(Mob m, ServerLevel level) {
        return level.isNight() || level.getBrightness(LightLayer.BLOCK, m.blockPosition()) + level.getBrightness(LightLayer.SKY, m.blockPosition()) <= 7;
    }

    private static void light(Mob m, ServerLevel level) {
        AttributeInstance speed = m.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(SPEED_ID);
        if (sunlit(m, level)) speed.addTransientModifier(SUN_SLOW);
        else if (dark(m, level)) speed.addTransientModifier(DARK_FAST);
    }

    // They don't burn in the sun, and your own explosions don't hurt you.
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent e) {
        if (Minions.noneLive()) return;
        LivingEntity hurt = e.getEntity();
        if (isUndead(hurt) && e.getSource().is(DamageTypeTags.IS_FIRE) && !hurt.isInLava()) {
            e.setCanceled(true);
            hurt.clearFire();
            return;
        }
        if (e.getSource().is(DamageTypeTags.IS_EXPLOSION) && e.getSource().getEntity() instanceof Mob m && isUndead(m)) {
            UUID owner = ownerOf(m);
            if (owner != null && (owner.equals(hurt.getUUID()) || isUndead(hurt))) e.setCanceled(true);
        }
    }

    // Their hits scale with mastery and with the light. Works for arrows as well as fists.
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (Minions.noneLive() || !(e.getSource().getEntity() instanceof Mob m) || !isUndead(m) || !(m.level() instanceof ServerLevel level)) return;
        int mastery = m.getPersistentData().getInt(LEVEL);
        e.setAmount((float) (e.getAmount() * UndeadRules.damageScale(mastery) * UndeadRules.lightScale(sunlit(m, level), dark(m, level), mastery)));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (isUndead(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent e) {
        if (isUndead(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        EXPLODING.clear();
    }
}
