package com.example.charpowers.timecontrol;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.Pocket;
import com.example.charpowers.timecontrol.model.TimeRules;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Slow things down, speed things up, and step outside of time. Hurry Up lives in Hurry (the only heavy part, and capped).
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class TimeControl extends OwnPower {
    public static final TimeControl POWER = new TimeControl();

    private static final UUID SLOW_ID = UUID.fromString("5f1e2d3c-7b6a-4c59-8e41-2a3b4c5d6e77");
    private static final UUID FAST_ID = UUID.fromString("a7c6e5d4-3b2a-4190-9f8e-7d6c5b4a3921");
    private static final AttributeModifier SLOW = new AttributeModifier(SLOW_ID, "charpowers slow down", TimeRules.SLOW_AMOUNT, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier STOPPED = new AttributeModifier(SLOW_ID, "charpowers still moment", -1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final String ENTERED = "enteredEnd", SPEED = "speed", CHUNKS = "chunks", IN_BLOCK = "inBlock";

    private static final Map<UUID, Slowing> SLOWING = new HashMap<>();
    private static final Map<UUID, WitherFight> FIGHTS = new HashMap<>();
    private static final List<Barrage> BARRAGES = new ArrayList<>();
    private static final List<Moment> MOMENTS = new ArrayList<>();

    private static final class Slowing {
        long until;
        final Set<LivingEntity> slowed = new HashSet<>();
    }

    private static final class WitherFight {
        final int wither;
        boolean clean = true;

        WitherFight(int wither) {
            this.wither = wither;
        }
    }

    private static final class Barrage {
        final UUID player;
        final LivingEntity target;
        int hits = TimeRules.BARRAGE_HITS;

        Barrage(UUID player, LivingEntity target) {
            this.player = player;
            this.target = target;
        }
    }

    private record Moment(UUID player, long ends, List<LivingEntity> held) {}

    private TimeControl() {
        super(Mastery.TIME_CONTROL, Items.CLOCK);
    }

    @Override
    public boolean watches() { return true; }

    @Override
    public String unlockMessage() { return "A hundred days of nothing out here. Time has stopped meaning anything to you. You can use that."; }

    @Override
    public String awakenedMessage() { return "You outran a Wither without it ever touching you. Time can hit things for you now."; }

    // How to get it: 100 days straight in the End. The clock is the world's, so it runs while you're offline too.
    @Override
    public void watchSecond(ServerPlayer p) {
        if (data(p).getBoolean(IN_BLOCK)) {
            data(p).remove(IN_BLOCK);
            if (Pocket.in(p)) Pocket.leave(p); // lost the power inside the room
        }
        if (p.level().dimension() != Level.END) {
            if (data(p).contains(ENTERED)) data(p).remove(ENTERED);
            return;
        }
        CompoundTag d = data(p);
        long now = p.level().getGameTime();
        if (!d.contains(ENTERED)) d.putLong(ENTERED, now);
        else if (now - d.getLong(ENTERED) >= TimeRules.END_TICKS_NEEDED) unlock(p);
    }

    @Override
    public void everySecond(ServerPlayer p) {
        boolean using = false;
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (AbilityUtil.isEnabled(p, id, "gotta_move")) {
            using = true;
            double amount = speedOf(p) / 100.0;
            AttributeModifier now = speed == null ? null : speed.getModifier(FAST_ID);
            if (speed != null && now == null) Fx.sound(p, PowerSounds.TIME_RUSH, 0.7f, 1f);
            if (speed != null && (now == null || now.getAmount() != amount)) {
                speed.removeModifier(FAST_ID);
                speed.addTransientModifier(new AttributeModifier(FAST_ID, "charpowers gotta move", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        } else if (speed != null && speed.getModifier(FAST_ID) != null) {
            speed.removeModifier(FAST_ID);
        }
        if (Pocket.in(p)) {
            using = true;
            p.heal(2f);
        }
        if (SLOWING.containsKey(p.getUUID()) || Hurry.active(p)) using = true;
        if (using) Mastery.add(p, id, 1);
        WitherFight fight = FIGHTS.get(p.getUUID());
        if (fight != null && fight.clean && !isSlowed(p, fight.wither)) {
            fight.clean = false;
            tell(p, "Your Slow Down let go of the Wither. This fight doesn't count.");
        }
    }

    private int speedOf(ServerPlayer p) {
        int saved = data(p).getInt(SPEED);
        return Math.max(TimeRules.SPEED_MIN, Math.min(TimeRules.topSpeed(mastery(p)), saved));
    }

    @Override
    public void onClientAction(ServerPlayer p, String action) {
        if (!owns(p) || !action.startsWith("speed")) return;
        int next = TimeRules.nudgeSpeed(speedOf(p), action.endsWith("+"), mastery(p));
        data(p).putInt(SPEED, next);
        whisper(p, "Gotta move: " + next + "%");
    }

    @Override
    public void onKey(ServerPlayer p, String ability) {
        switch (ability) {
            case "slow_down" -> slowDown(p);
            case "hurry_up" -> {
                Hurry.start(p);
                Fx.sound(p, PowerSounds.TIME_TICK, 0.8f, 1.25f);
                whisper(p, "Hurry up!");
            }
            case "let_time_flow" -> letTimeFlow(p);
            case "time_block" -> timeBlock(p);
            case "barrage" -> {
                LivingEntity target = Fx.lookTarget(p, 6);
                if (target == null) whisper(p, "Nothing close enough to hit.");
                else BARRAGES.add(new Barrage(p.getUUID(), target));
            }
            case "still_moment" -> stillMoment(p);
            default -> {}
        }
    }

    private void slowDown(ServerPlayer p) {
        Slowing s = SLOWING.computeIfAbsent(p.getUUID(), u -> new Slowing());
        s.until = p.level().getGameTime() + TimeRules.SLOW_TICKS;
        Fx.sound(p, PowerSounds.TIME_SLOW, 1f, 1f);
        whisper(p, "Slow down.");
    }

    // A forced chunk stays loaded by the game itself, across restarts. Press in a chunk of yours to let it go.
    private void letTimeFlow(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ChunkPos c = p.chunkPosition();
        String key = level.dimension().location() + "|" + c.x + "|" + c.z;
        ListTag chunks = data(p).getList(CHUNKS, Tag.TAG_STRING);
        for (int i = 0; i < chunks.size(); i++) {
            if (chunks.getString(i).equals(key)) {
                chunks.remove(i);
                level.setChunkForced(c.x, c.z, false);
                data(p).put(CHUNKS, chunks);
                whisper(p, "This chunk can rest now.");
                return;
            }
        }
        if (chunks.size() >= TimeRules.chunkLimit(mastery(p))) {
            whisper(p, "You're holding as many chunks as you can. See /char_chunks.");
            return;
        }
        chunks.add(StringTag.valueOf(key));
        data(p).put(CHUNKS, chunks);
        level.setChunkForced(c.x, c.z, true);
        Fx.sound(p, PowerSounds.TIME_TICK, 0.8f, 0.8f);
        whisper(p, "Time will flow here, always.");
    }

    public List<String> chunks(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        ListTag chunks = data(p).getList(CHUNKS, Tag.TAG_STRING);
        for (int i = 0; i < chunks.size(); i++) out.add(chunks.getString(i));
        return out;
    }

    public boolean removeChunk(ServerPlayer p, int index) {
        ListTag chunks = data(p).getList(CHUNKS, Tag.TAG_STRING);
        if (index < 0 || index >= chunks.size()) return false;
        String[] parts = chunks.getString(index).split("\\|");
        chunks.remove(index);
        data(p).put(CHUNKS, chunks);
        ResourceLocation dim = ResourceLocation.tryParse(parts[0]);
        for (ServerLevel level : p.getServer().getAllLevels()) {
            if (level.dimension().location().equals(dim)) level.setChunkForced(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), false);
        }
        return true;
    }

    // Time Block: your own room outside of time. The cooldown starts when you come back.
    private void timeBlock(ServerPlayer p) {
        if (Pocket.in(p)) {
            data(p).remove(IN_BLOCK);
            Pocket.leave(p);
            PowerSounds.toPlayer(p, PowerSounds.TIME_TICK, 0.8f, 0.9f);
            AbilityInstance key = AbilityUtil.getInstance(p, id, "time_block");
            if (key != null) key.startCooldown(p, Setting.TIME_BLOCK_COOLDOWN.i());
            return;
        }
        ServerLevel level = Pocket.level(p);
        if (level == null) {
            whisper(p, "Time won't open up here.");
            return;
        }
        BlockPos middle = Pocket.roomOf(p.getUUID());
        Pocket.buildBox(level, middle, TimeRules.BLOCK_SIZE, Pocket.wall(), Blocks.SEA_LANTERN.defaultBlockState());
        data(p).putBoolean(IN_BLOCK, true);
        Pocket.enter(p, level, middle);
        PowerSounds.toPlayer(p, PowerSounds.TIME_STOP, 0.8f, 1f);
    }

    private void stillMoment(ServerPlayer p) {
        List<LivingEntity> held = new ArrayList<>();
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(TimeRules.MOMENT_RADIUS), e -> e != p && e.isAlive())) {
            if (!(e instanceof Mob) && !(e instanceof Player) || p.isAlliedTo(e)) continue;
            hold(e, STOPPED);
            held.add(e);
        }
        MOMENTS.add(new Moment(p.getUUID(), p.level().getGameTime() + TimeRules.MOMENT_TICKS, held));
        Fx.sound(p, PowerSounds.TIME_STOP, 1f, 1f);
        whisper(p, "Everything holds still...");
    }

    private static void hold(LivingEntity e, AttributeModifier how) {
        AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SLOW_ID) == null) speed.addTransientModifier(how);
    }

    private static void release(LivingEntity e) {
        AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SLOW_ID);
    }

    private static boolean isSlowed(ServerPlayer p, int entityId) {
        var e = p.level().getEntity(entityId);
        return e instanceof LivingEntity l && SLOWING.containsKey(p.getUUID()) && SLOWING.get(p.getUUID()).slowed.contains(l);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        long tick = e.getServer().getTickCount();
        Hurry.tick(e.getServer());
        if (!SLOWING.isEmpty() && tick % 20 == 0) tickSlowing(e);
        if (!BARRAGES.isEmpty() && tick % 2 == 0) tickBarrages(e);
        if (!MOMENTS.isEmpty()) tickMoments(e);
    }

    private static void tickSlowing(TickEvent.ServerTickEvent e) {
        Iterator<Map.Entry<UUID, Slowing>> it = SLOWING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Slowing> entry = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(entry.getKey());
            Slowing s = entry.getValue();
            if (p == null || p.level().getGameTime() >= s.until) {
                s.slowed.forEach(TimeControl::release);
                it.remove();
                continue;
            }
            Set<LivingEntity> now = new HashSet<>();
            double reach = (double) Setting.TIME_SLOW.i() * Setting.TIME_SLOW.i();
            for (LivingEntity t : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(Setting.TIME_SLOW.i()), t -> t != p && t.isAlive())) {
                if ((t instanceof Mob || t instanceof Player) && !p.isAlliedTo(t) && t.distanceToSqr(p) <= reach) {
                    now.add(t);
                    hold(t, SLOW);
                    // not only walking: digging, swinging and falling drag too, and you can see it on them
                    t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 3, true, false));
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false));
                    t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false));
                    Fx.particles(p.serverLevel(), ParticleTypes.ENCHANT, t.position().add(0, t.getBbHeight() / 2, 0), 4, 0.4);
                }
            }
            for (LivingEntity old : s.slowed) {
                if (!now.contains(old)) release(old);
            }
            if (s.slowed.isEmpty()) whisper(p, "Slow down: " + now.size() + (now.size() == 1 ? " thing" : " things") + " caught in it.");
            for (var shot : p.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, p.getBoundingBox().inflate(Setting.TIME_SLOW.i()), x -> x.getOwner() != p)) {
                shot.setDeltaMovement(shot.getDeltaMovement().scale(0.5));
                shot.hurtMarked = true;
            }
            s.slowed.clear();
            s.slowed.addAll(now);
        }
    }

    private static void tickBarrages(TickEvent.ServerTickEvent e) {
        Iterator<Barrage> it = BARRAGES.iterator();
        while (it.hasNext()) {
            Barrage b = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(b.player);
            if (p == null || !b.target.isAlive() || b.hits-- <= 0) {
                it.remove();
                continue;
            }
            b.target.invulnerableTime = 0;
            b.target.hurt(p.damageSources().playerAttack(p), (float) Math.max(2, p.getAttributeValue(Attributes.ATTACK_DAMAGE) / 2));
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            p.level().playSound(null, b.target.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }

    private static void tickMoments(TickEvent.ServerTickEvent e) {
        Iterator<Moment> it = MOMENTS.iterator();
        while (it.hasNext()) {
            Moment m = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(m.player());
            if (p != null && p.level().getGameTime() < m.ends()) continue;
            it.remove();
            for (LivingEntity held : m.held()) {
                release(held);
                if (p != null && held.isAlive()) held.hurt(p.damageSources().playerAttack(p), TimeRules.MOMENT_DAMAGE);
            }
        }
    }

    // The quest: beat a Wither with Slow Down on it the whole fight, without taking a hit.
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof WitherBoss wither && e.getSource().getEntity() instanceof ServerPlayer p && POWER.owns(p) && POWER.questReady(p)) {
            FIGHTS.computeIfAbsent(p.getUUID(), u -> {
                whisper(p, "Keep it slow, and don't let it touch you.");
                WitherFight fight = new WitherFight(wither.getId());
                fight.clean = isSlowed(p, wither.getId());
                return fight;
            });
        } else if (e.getEntity() instanceof ServerPlayer p && !FIGHTS.isEmpty()) {
            WitherFight fight = FIGHTS.get(p.getUUID());
            if (fight != null && fight.clean) {
                fight.clean = false;
                tell(p, "You took a hit. This fight doesn't count.");
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (FIGHTS.isEmpty()) return;
        if (e.getEntity() instanceof WitherBoss wither) {
            for (ServerPlayer p : new ArrayList<>(wither.getServer().getPlayerList().getPlayers())) {
                WitherFight fight = FIGHTS.get(p.getUUID());
                if (fight != null && fight.wither == wither.getId()) {
                    FIGHTS.remove(p.getUUID());
                    if (fight.clean) POWER.awaken(p);
                }
            }
        } else if (e.getEntity() instanceof ServerPlayer p) {
            FIGHTS.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        FIGHTS.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        SLOWING.values().forEach(s -> s.slowed.forEach(TimeControl::release));
        SLOWING.clear();
        FIGHTS.clear();
        BARRAGES.clear();
        MOMENTS.clear();
        Hurry.clear();
    }
}
