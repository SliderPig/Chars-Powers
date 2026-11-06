package com.example.charpowers.authority;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.authority.model.AuthorityRules;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.entity.PalladiumAttributes;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The commands being obeyed right now. Nothing here runs unless a command is active. Who's in range is looked up every
// 2 seconds, not every tick. Players' keys are handled on their own PC (authority/client/ObeyClient).
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class Orders {
    private static final UUID SLOW_ID = UUID.fromString("8e7ad3d2-3a0c-4b6e-9d2e-1f0a7c5b4e11");
    private static final UUID NO_FLY_ID = UUID.fromString("c1b2e8a4-7f60-49d3-8a11-5d3e2f1c9a22");
    private static final AttributeModifier STOPPED = new AttributeModifier(SLOW_ID, "charpowers authority", -1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier CROUCHED = new AttributeModifier(SLOW_ID, "charpowers authority", -0.6, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier NO_FLY = new AttributeModifier(NO_FLY_ID, "charpowers authority", -1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final int REFRESH_TICKS = 40;
    private static final String HELD = "charpowers.held";

    private static final List<Order> ACTIVE = new ArrayList<>();
    private static final Map<UUID, Integer> OBEY = new HashMap<>();
    private static final Deque<LivingEntity> DYING = new ArrayDeque<>();

    // Only used on the player's PC, set by ObeyClient. Here so the shared events below can read it.
    public static int clientMask;
    public static long clientUntil;

    private static final class Order {
        final UUID commander;
        final String word;
        final int mask, range;
        final long ends;
        @Nullable
        final LivingEntity victim;
        long nextRefresh;
        final List<Mob> mobs = new ArrayList<>();
        boolean told;
        final List<ServerPlayer> players = new ArrayList<>();

        Order(UUID commander, String word, int mask, int range, long ends, @Nullable LivingEntity victim) {
            this.commander = commander;
            this.word = word;
            this.mask = mask;
            this.range = range;
            this.ends = ends;
            this.victim = victim;
        }
    }

    private Orders() {}

    static void start(ServerPlayer commander, String word, int mask, int range, int ticks, @Nullable LivingEntity victim) {
        ACTIVE.add(new Order(commander.getUUID(), word, mask, range, commander.getServer().getTickCount() + ticks, victim));
    }

    // Die (awakened): everyone not in your team within 100 blocks. A few each tick, so a crowd doesn't freeze the server.
    static void die(ServerPlayer p) {
        AABB box = p.getBoundingBox().inflate(Setting.AUTHORITY_DIE_RADIUS.i());
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive())) {
            if (!(e instanceof Mob) && !(e instanceof Player)) continue;
            if (p.isAlliedTo(e) || (e instanceof CloneEntity c && c.hostId.equals(p.getUUID())) || (e instanceof ServerPlayer sp && sp.isSpectator())) continue;
            if (e.distanceToSqr(p) <= Setting.AUTHORITY_DIE_RADIUS.i() * Setting.AUTHORITY_DIE_RADIUS.i()) DYING.add(e);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        for (int i = 0; i < AuthorityRules.DIE_PER_TICK && !DYING.isEmpty(); i++) {
            LivingEntity target = DYING.poll();
            if (target.isAlive()) target.hurt(target.damageSources().genericKill(), Float.MAX_VALUE);
        }
        if (ACTIVE.isEmpty()) return;
        MinecraftServer server = e.getServer();
        long now = server.getTickCount();
        boolean changed = false;
        Iterator<Order> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Order o = it.next();
            ServerPlayer commander = server.getPlayerList().getPlayer(o.commander);
            boolean victimGone = (o.mask & AuthorityRules.ATTACK) != 0 && (o.victim == null || !o.victim.isAlive());
            if (commander == null || !commander.isAlive() || now >= o.ends || victimGone || !SuperiorAuthority.POWER.owns(commander)) {
                finish(o);
                it.remove();
                changed = true;
                continue;
            }
            if (now >= o.nextRefresh) {
                o.nextRefresh = now + REFRESH_TICKS;
                refresh(o, commander);
                changed = true;
            }
            act(o, commander, now);
            if (now % 20 == 0) Mastery.add(commander, Mastery.SUPERIOR_AUTHORITY, 1);
        }
        if (changed) sendMasks(server);
    }

    private static void refresh(Order o, ServerPlayer commander) {
        List<ServerPlayer> were = new ArrayList<>(o.players);
        o.players.clear();
        double reach = (double) o.range * o.range;
        for (Player other : commander.level().players()) {
            if (other instanceof ServerPlayer p && p != commander && !p.isSpectator() && !(p instanceof CloneEntity)
                    && p.distanceToSqr(commander) <= reach && p != o.victim) {
                o.players.add(p);
                if (!were.contains(p)) OwnPower.whisper(p, commander.getGameProfile().getName() + " commands: " + o.word);
                if ((o.mask & AuthorityRules.STOP) != 0) {
                    p.stopUsingItem();
                    if (p.isSleeping()) p.stopSleepInBed(true, true);
                }
                if ((o.mask & AuthorityRules.NO_JUMP) != 0) noFlying(p, true);
            }
        }
        for (ServerPlayer p : were) {
            if (!o.players.contains(p)) noFlying(p, false);
        }
        List<Mob> mobs = commander.level().getEntitiesOfClass(Mob.class, commander.getBoundingBox().inflate(o.range), m -> m != o.victim && m.isAlive());
        if (mobs.size() > AuthorityRules.MOB_CAP) {
            mobs.sort(Comparator.comparingDouble(m -> m.distanceToSqr(commander)));
            mobs = mobs.subList(0, AuthorityRules.MOB_CAP);
        }
        for (Mob old : o.mobs) {
            if (!mobs.contains(old)) letGo(old);
        }
        o.mobs.clear();
        o.mobs.addAll(mobs);
        AttributeModifier slow = (o.mask & AuthorityRules.FREEZE) != 0 ? STOPPED : (o.mask & AuthorityRules.KNEEL) != 0 ? CROUCHED : null;
        boolean lockAi = (o.mask & (AuthorityRules.STOP | AuthorityRules.FREEZE | AuthorityRules.LOOK)) != 0;
        for (Mob m : o.mobs) {
            if (slow != null) {
                AttributeInstance speed = m.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null && speed.getModifier(SLOW_ID) == null) speed.addTransientModifier(slow);
            }
            // a mob's brain would just start walking again: it has to be stopped thinking while the command lasts
            if (lockAi && !m.isNoAi()) {
                m.setNoAi(true);
                m.getPersistentData().putBoolean(HELD, true);
            }
            if ((o.mask & AuthorityRules.KNEEL) != 0) {
                m.setPose(Pose.CROUCHING);
                m.setShiftKeyDown(true);
            }
        }
        if (!o.told) {
            o.told = true;
            OwnPower.whisper(commander, o.word + ": " + (o.mobs.size() + o.players.size()) + " obey.");
            if (commander.level() instanceof net.minecraft.server.level.ServerLevel level) {
                for (Mob m : o.mobs) level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT, m.getX(), m.getEyeY(), m.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    private static void letGo(Mob m) {
        AttributeInstance speed = m.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SLOW_ID);
        if (m.getPersistentData().getBoolean(HELD)) {
            m.getPersistentData().remove(HELD);
            m.setNoAi(false);
        }
        if (m.getPose() == Pose.CROUCHING) {
            m.setPose(Pose.STANDING);
            m.setShiftKeyDown(false);
        }
    }

    // A mob that was unloaded while held shouldn't stay stuck for ever.
    @SubscribeEvent
    public static void onJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent e) {
        if (e.getEntity() instanceof Mob m && !e.getLevel().isClientSide && m.getPersistentData().getBoolean(HELD)) {
            m.getPersistentData().remove(HELD);
            m.setNoAi(false);
        }
    }

    private static void act(Order o, ServerPlayer commander, long now) {
        int mask = o.mask;
        if (now % 5 == 0 || (mask & AuthorityRules.NO_JUMP) != 0) {
            for (Mob m : o.mobs) {
                if (!m.isAlive()) continue;
                if ((mask & (AuthorityRules.STOP | AuthorityRules.FREEZE | AuthorityRules.LOOK)) != 0) {
                    m.getNavigation().stop();
                    m.setTarget(null);
                    m.setDeltaMovement(0, m.getDeltaMovement().y, 0);
                }
                if ((mask & AuthorityRules.LOOK) != 0) {
                    m.lookAt(EntityAnchorArgument.Anchor.EYES, commander.getEyePosition());
                    m.yBodyRot = m.getYRot();
                    m.yHeadRot = m.getYRot();
                }
                if ((mask & AuthorityRules.ATTACK) != 0 && o.victim != null) {
                    m.setTarget(o.victim);
                    m.setLastHurtByMob(o.victim);
                }
                // nobody jumps or flies: anything in the air comes down
                if ((mask & AuthorityRules.NO_JUMP) != 0 && !m.onGround()) {
                    m.setJumping(false);
                    m.setDeltaMovement(m.getDeltaMovement().x, Math.min(m.getDeltaMovement().y, -0.5), m.getDeltaMovement().z);
                }
            }
        }
        if (now % 3 != 0) return;
        for (ServerPlayer p : o.players) {
            if (!p.isAlive()) continue;
            if ((mask & AuthorityRules.NO_JUMP) != 0) {
                if (p.getAbilities().flying && !p.isCreative()) {
                    p.getAbilities().flying = false;
                    p.onUpdateAbilities();
                }
                if (p.isFallFlying()) p.stopFallFlying();
            }
            if ((mask & AuthorityRules.LOOK) != 0) p.lookAt(EntityAnchorArgument.Anchor.EYES, commander.getEyePosition());
            if ((mask & AuthorityRules.ATTACK) != 0 && o.victim != null) {
                p.lookAt(EntityAnchorArgument.Anchor.EYES, o.victim.getEyePosition());
                if (p.distanceTo(o.victim) <= 3 && p.getAttackStrengthScale(0f) >= 1f) {
                    p.attack(o.victim);
                    p.swing(InteractionHand.MAIN_HAND, true);
                    p.resetAttackStrengthTicker();
                }
            }
        }
    }

    private static void finish(Order o) {
        o.mobs.forEach(Orders::letGo);
        if ((o.mask & AuthorityRules.NO_JUMP) != 0) {
            for (ServerPlayer p : o.players) noFlying(p, false);
        }
    }

    private static void noFlying(ServerPlayer p, boolean on) {
        for (Attribute a : List.of(PalladiumAttributes.FLIGHT_SPEED.get(), PalladiumAttributes.LEVITATION_SPEED.get())) {
            AttributeInstance i = p.getAttribute(a);
            if (i == null) continue;
            if (on && i.getModifier(NO_FLY_ID) == null) i.addTransientModifier(NO_FLY);
            if (!on) i.removeModifier(NO_FLY_ID);
        }
    }

    // Tells each player's PC what they must do. It stops by itself after 3 seconds if nothing else arrives.
    private static void sendMasks(MinecraftServer server) {
        Map<UUID, Integer> next = new HashMap<>();
        for (Order o : ACTIVE) {
            for (ServerPlayer p : o.players) next.merge(p.getUUID(), o.mask, (a, b) -> a | b);
        }
        for (UUID id : OBEY.keySet()) {
            if (!next.containsKey(id)) sendMask(server, id, 0);
        }
        next.forEach((id, mask) -> sendMask(server, id, mask));
        OBEY.clear();
        OBEY.putAll(next);
    }

    private static void sendMask(MinecraftServer server, UUID id, int mask) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p == null) return;
        CompoundTag tag = new CompoundTag();
        tag.putInt("mask", mask);
        Net.toOwner(p, Mastery.SUPERIOR_AUTHORITY.toString(), tag);
    }

    private static boolean stopped(Player p) {
        if (p.level().isClientSide) return (clientMask & AuthorityRules.STOP) != 0 && p.level().getGameTime() < clientUntil;
        return !OBEY.isEmpty() && (OBEY.getOrDefault(p.getUUID(), 0) & AuthorityRules.STOP) != 0;
    }

    // Stop: while it lasts you can't dig, use, click, hit or sleep.
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        if (stopped(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock e) {
        if (stopped(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (stopped(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (stopped(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract e) {
        if (stopped(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (stopped(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUse(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof Player p && stopped(p)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent e) {
        if (stopped(e.getEntity())) e.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
    }

    @SubscribeEvent
    public static void onStopping(net.minecraftforge.event.server.ServerStoppingEvent e) {
        ACTIVE.forEach(Orders::finish);
        ACTIVE.clear();
        OBEY.clear();
        DYING.clear();
    }
}
