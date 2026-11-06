package com.example.charpowers.game;

import com.example.charpowers.CharPowers;
import com.example.charpowers.game.mob.GateBoss;
import com.example.charpowers.game.mob.GateKind;
import com.example.charpowers.game.mob.GateMobs;
import com.example.charpowers.game.model.ShadowRules;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.mobcontrol.PetMinds;
import com.example.charpowers.own.Minions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Raise Shadow: a creature you kill inside a gate leaves a dark shape for ten seconds. Press the key beside it and it stands up
// on your side, black and purple with glowing eyes, and fights for you until you leave the gate. Only gate creatures, never a
// player. Bosses mostly refuse. The shadows are ordinary mobs held by Minions (they follow you and only fight what you point at).
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class ShadowArmy {
    public static final String KIND = "shadow";
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CharPowers.MODID);
    public static final RegistryObject<SoundEvent> RISE = sound("shadow_rise");
    public static final RegistryObject<SoundEvent> FAIL = sound("shadow_fail");
    private static final int LOOK_EVERY = 10, SYNC_EVERY = 20, HUNT_RANGE = 16, RISE_TICKS = 34, RECENT = 100;

    // what is left of a creature that just died in the gate
    public record Body(EntityType<?> type, ResourceKey<Level> dim, Vec3 at, long until, boolean boss, String name) {}

    private static final class Rise {
        final ServerLevel level;
        final Vec3 at;
        int age;

        Rise(ServerLevel level, Vec3 at) {
            this.level = level;
            this.at = at;
        }
    }

    private static final Map<UUID, List<Body>> BODIES = new HashMap<>();
    private static final Map<UUID, Long> BOSS_READY = new HashMap<>();
    private static final List<Rise> RISES = new ArrayList<>();
    private static final Map<ResourceKey<Level>, Set<Integer>> SENT = new HashMap<>();

    private ShadowArmy() {}

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CharPowers.MODID, name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    // ---- bodies

    private static ServerPlayer ownerOfKill(DamageSource source, ServerLevel level) {
        Entity by = source.getEntity();
        if (by instanceof ServerPlayer p) return p;
        if (by instanceof Mob m && Minions.isKind(m, KIND)) {
            UUID id = Minions.ownerOf(m);
            return id == null ? null : level.getServer().getPlayerList().getPlayer(id);
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onKill(LivingDeathEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof Mob victim) || !(victim.level() instanceof ServerLevel level)) return;
        ServerPlayer owner = ownerOfKill(e.getSource(), level);
        if (owner == null || !GamePlayer.POWER.owns(owner)) return;
        if (Mastery.level(owner, Mastery.GAME_PLAYER) < ShadowRules.UNLOCK_LEVEL) return;
        boolean inRun = GamePlayer.POWER.runOf(owner.getUUID()) != null;
        if (!(victim instanceof Enemy) || !ShadowRules.fair(inRun, false, Minions.isMinion(victim))) return;
        boolean boss = victim instanceof GateBoss gb && gb.kind.boss;
        List<Body> list = BODIES.computeIfAbsent(owner.getUUID(), k -> new ArrayList<>());
        boolean first = list.stream().noneMatch(b -> ShadowRules.fresh(level.getGameTime(), b.until()));
        list.add(new Body(victim.getType(), level.dimension(), victim.position(), level.getGameTime() + ShadowRules.WINDOW_TICKS, boss, victim.getName().getString()));
        while (list.size() > ShadowRules.MAX_BODIES) list.remove(0);
        if (first || boss) GamePlayer.POWER.note(owner, boss ? "A great shadow lingers. Raise Shadow now, if you dare." : "A shadow lingers. Raise Shadow now.");
        level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 6, 0.25, 0.3, 0.25, 0.02);
    }

    static List<Body> bodiesOf(UUID owner) {
        return BODIES.getOrDefault(owner, List.of());
    }

    // ---- the key

    public static void raise(ServerPlayer p) {
        int level = Mastery.level(p, Mastery.GAME_PLAYER);
        long now = p.level().getGameTime();
        List<Body> list = BODIES.get(p.getUUID());
        if (list != null) list.removeIf(b -> !ShadowRules.fresh(now, b.until()));
        Body nearest = null;
        int inReach = 0;
        if (list != null) {
            double best = Double.MAX_VALUE;
            for (Body b : list) {
                if (b.dim() != p.level().dimension()) continue;
                double d = b.at().distanceToSqr(p.position());
                if (d > (double) ShadowRules.REACH * ShadowRules.REACH) continue;
                inReach++;
                if (d < best) {
                    best = d;
                    nearest = b;
                }
            }
        }
        int have = Minions.of(p.getUUID(), KIND).size();
        ShadowRules.Result r = ShadowRules.check(level, inReach, have, nearest != null && nearest.boss(), now, BOSS_READY.getOrDefault(p.getUUID(), 0L));
        GamePlayer power = GamePlayer.POWER;
        switch (r) {
            case LOCKED -> power.note(p, "Raise Shadow comes at Game Player mastery " + ShadowRules.UNLOCK_LEVEL + ".");
            case NO_BODY -> power.note(p, "There is no shadow here to raise.");
            case TOO_MANY -> power.note(p, "You can only hold " + ShadowRules.maxShadows(level) + " shadows.");
            case BOSS_WAIT -> power.note(p, "That one is too strong to try again yet.");
            case OK -> {
                list.remove(nearest);
                ServerLevel world = p.serverLevel();
                if (nearest.boss()) {
                    BOSS_READY.put(p.getUUID(), now + ShadowRules.BOSS_COOLDOWN_TICKS);
                    if (p.getRandom().nextDouble() >= ShadowRules.chance(true, level)) {
                        world.playSound(null, p.blockPosition(), FAIL.get(), SoundSource.PLAYERS, 1f, 1f);
                        world.sendParticles(ParticleTypes.SMOKE, nearest.at().x, nearest.at().y + 0.5, nearest.at().z, 20, 0.4, 0.5, 0.4, 0.02);
                        power.note(p, "The shadow crumbled before it could stand.");
                        return;
                    }
                }
                Mob mob = spawn(world, p, nearest);
                if (mob == null) {
                    power.note(p, "That shadow would not hold.");
                    return;
                }
                world.playSound(null, mob.blockPosition(), RISE.get(), SoundSource.PLAYERS, 1f, 1f);
                RISES.add(new Rise(world, mob.position()));
                power.note(p, "Shadow raised (" + (have + 1) + "/" + ShadowRules.maxShadows(level) + ").");
                Mastery.add(p, Mastery.GAME_PLAYER, 3);
            }
        }
    }

    // The creature's own kind, as a shadow. A boss comes back as the strongest ordinary gate creature, because a boss's special
    // attacks hit anyone near, friends included.
    private static Mob spawn(ServerLevel level, ServerPlayer owner, Body body) {
        EntityType<?> type = body.type();
        double scale = ShadowRules.HEALTH_SCALE;
        String name = body.name();
        if (body.boss()) {
            GateKind strongest = Arrays.stream(GateKind.values()).filter(k -> !k.boss).max(Comparator.comparingDouble(k -> k.health)).orElse(null);
            if (strongest == null) return null;
            type = GateMobs.mobType(strongest);
            scale = 1.5;
            name = "Champion";
        }
        Entity made = type.create(level);
        if (!(made instanceof Mob mob)) return null;
        mob.moveTo(body.at().x, body.at().y, body.at().z, owner.getYRot() + 180f, 0f);
        Minions.claim(mob, owner, KIND);
        mob.setCustomName(Component.literal("Shadow " + name));
        mob.setCustomNameVisible(false);
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(4.0, mob.getMaxHealth() * scale));
            mob.setHealth(mob.getMaxHealth());
        }
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, true, false, false));
        level.addFreshEntity(mob);
        return mob;
    }

    // ---- looking after them

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!RISES.isEmpty()) tickRises();
        long tick = e.getServer().getTickCount();
        if (tick % LOOK_EVERY == 3 && !Minions.noneLive()) lookAfter(e);
        if (tick % SYNC_EVERY == 5) sync(e.getServer(), tick);
    }

    private static void tickRises() {
        for (Iterator<Rise> it = RISES.iterator(); it.hasNext(); ) {
            Rise r = it.next();
            if (r.age >= RISE_TICKS) {
                it.remove();
                continue;
            }
            DustParticleOptions dark = new DustParticleOptions(new Vector3f(0.20f, 0.03f, 0.32f), 1.6f);
            DustParticleOptions violet = new DustParticleOptions(new Vector3f(0.55f, 0.20f, 0.85f), 1.0f);
            for (int k = 0; k < 3; k++) {
                double a = r.age * 0.45 + k * 2.094;
                double rad = 0.75 * (1.0 - r.age / (double) (RISE_TICKS + 8));
                double y = r.age * 0.07;
                r.level.sendParticles(k == 0 ? violet : dark, r.at.x + Math.cos(a) * rad, r.at.y + y, r.at.z + Math.sin(a) * rad, 1, 0, 0, 0, 0);
            }
            if (r.age % 4 == 0) r.level.sendParticles(ParticleTypes.SQUID_INK, r.at.x, r.at.y + 0.2, r.at.z, 3, 0.25, 0.1, 0.25, 0.04);
            r.age++;
        }
    }

    private static void lookAfter(TickEvent.ServerTickEvent e) {
        for (Mob m : Minions.all(KIND)) {
            UUID id = Minions.ownerOf(m);
            ServerPlayer owner = id == null ? null : e.getServer().getPlayerList().getPlayer(id);
            boolean stay = ShadowRules.mayStay(owner != null && owner.isAlive(), owner != null && GamePlayer.POWER.runOf(id) != null, owner != null && owner.level() == m.level());
            if (!stay) {
                vanish(m);
                continue;
            }
            if (m.distanceToSqr(owner) > 40 * 40) m.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            LivingEntity target = m.getTarget();
            if (target == null || !target.isAlive()) hunt(m, owner);
        }
    }

    private static void hunt(Mob m, ServerPlayer owner) {
        LivingEntity pick = null;
        if (owner.getLastHurtByMob() != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < RECENT) pick = owner.getLastHurtByMob();
        else if (owner.getLastHurtMob() != null && owner.tickCount - owner.getLastHurtMobTimestamp() < RECENT) pick = owner.getLastHurtMob();
        if (pick == null || !pick.isAlive() || Minions.isMinion(pick) || pick instanceof Player) {
            pick = m.level().getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(HUNT_RANGE),
                    t -> t instanceof Enemy && t.isAlive() && !Minions.isMinion(t) && !(t instanceof Player)).stream()
                    .min(Comparator.comparingDouble(t -> t.distanceToSqr(m))).orElse(null);
        }
        if (pick != null) PetMinds.aim(m, pick);
    }

    private static void vanish(Mob m) {
        if (m.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SQUID_INK, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 14, 0.3, 0.5, 0.3, 0.03);
            level.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 0.5, m.getZ(), 6, 0.3, 0.4, 0.3, 0.03);
        }
        m.discard();
    }

    // Which creatures are shadows, sent to everyone in that world only when it changes (and now and then, for people who just arrived).
    private static void sync(net.minecraft.server.MinecraftServer server, long tick) {
        Map<ResourceKey<Level>, Set<Integer>> now = new HashMap<>();
        for (Mob m : Minions.all(KIND)) {
            if (m.level() instanceof ServerLevel level) now.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(m.getId());
        }
        Set<ResourceKey<Level>> dims = new HashSet<>(now.keySet());
        dims.addAll(SENT.keySet());
        for (ResourceKey<Level> dim : dims) {
            Set<Integer> ids = now.getOrDefault(dim, Set.of());
            boolean same = ids.equals(SENT.getOrDefault(dim, Set.of()));
            if (same && (tick % 200 != 5 || ids.isEmpty())) continue;
            ServerLevel level = server.getLevel(dim);
            if (level != null) ShadowNet.send(level, ids.stream().mapToInt(Integer::intValue).toArray());
        }
        SENT.clear();
        SENT.putAll(now);
    }

    // ---- fair play

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        if (Minions.isKind(e.getEntity(), KIND) && e.getSource().getEntity() instanceof Player) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (Minions.isKind(e.getEntity(), KIND)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent e) {
        if (Minions.isKind(e.getEntity(), KIND)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        BODIES.clear();
        BOSS_READY.clear();
        RISES.clear();
        SENT.clear();
    }

    // for the tests and the real-game check
    public static int count(UUID owner) {
        return Minions.of(owner, KIND).size();
    }

    public static void addBodyForTest(ServerPlayer owner, Body body) {
        BODIES.computeIfAbsent(owner.getUUID(), k -> new ArrayList<>()).add(body);
    }

    public static void clearForTest(UUID owner) {
        BODIES.remove(owner);
        BOSS_READY.remove(owner);
    }
}
