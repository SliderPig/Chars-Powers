package com.example.charpowers.futuresight;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.hidden.HiddenPowers;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.MobPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.model.PowerDef;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OreScan;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.RaidWatch;
import com.example.charpowers.futuresight.model.SightRules;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// You get told things: ores around you while you mine, what's about to hit you in a fight, and which mobs "have
// something". All of it only runs for owners who are mining or near mobs. Hush switches it off for a while.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class FutureSight extends OwnPower {
    public static final FutureSight POWER = new FutureSight();
    private static final String DODGED = "dodged", RAIDS = "raids";
    private static final int SCAN_BUDGET = 8192, GLOW_MAX = 32;

    private static final Map<UUID, Seer> SEERS = new HashMap<>();
    private static final Map<UUID, OreScan> SCANS = new HashMap<>();
    // skeleton arrows in flight -> who they've flown close past so far
    private static final Map<AbstractArrow, Set<UUID>> ARROWS = new HashMap<>();

    private static final class Seer {
        long lastMined = -1000, lastHint = -1000, lastThreat = -1000;
        final Map<UUID, Long> warned = new HashMap<>();
        final Set<UUID> sensed = new HashSet<>();
    }

    private FutureSight() {
        super(Mastery.FUTURE_SIGHT, Items.ENDER_EYE);
    }

    private static Seer seer(ServerPlayer p) {
        return SEERS.computeIfAbsent(p.getUUID(), u -> new Seer());
    }

    private boolean hushed(ServerPlayer p) {
        return AbilityUtil.isEnabled(p, id, "hush") || HiddenRuntime.switchedOff(p);
    }

    @Override
    public String unlockMessage() { return "That was the thousandth arrow you saw coming. You can see the rest now too."; }

    @Override
    public String awakenedMessage() { return "Ten raids and not a scratch. Now you can see through other eyes."; }

    @Override
    public void onKey(ServerPlayer p, String ability) {
        if (!"see_through".equals(ability)) return;
        LivingEntity target = Fx.lookTarget(p, 64);
        CompoundTag tag = new CompoundTag();
        tag.putInt("eyes", target == null ? -1 : target.getId());
        Net.toOwner(p, id.toString(), tag);
    }

    @Override
    public void everySecond(ServerPlayer p) {
        Seer s = seer(p);
        long now = p.level().getGameTime();
        if (now - s.lastMined < 200 || now - s.lastThreat < 200) Mastery.add(p, id, 1);
        if (!questReady(p)) return;
        RaidWatch.Watch raid = RaidWatch.step(p);
        if (raid == null) return;
        if (raid.started) whisper(p, "A raid. Don't let them touch you.");
        if (raid.over && raid.won()) {
            if (raid.wasHit) {
                tell(p, "You won, but you got hit. That one doesn't count.");
                return;
            }
            int raids = data(p).getInt(RAIDS) + 1;
            data(p).putInt(RAIDS, raids);
            if (raids >= SightRules.RAIDS_NEEDED) awaken(p);
            else tell(p, "A whole raid without being hit. " + raids + "/" + SightRules.RAIDS_NEEDED);
        }
    }

    // Mining: look for ores, a bit at a time, at most every 10 seconds.
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !POWER.owns(p) || !e.getState().is(BlockTags.MINEABLE_WITH_PICKAXE)) return;
        Seer s = seer(p);
        long now = p.level().getGameTime();
        s.lastMined = now;
        if (POWER.hushed(p) || SCANS.containsKey(p.getUUID()) || now - s.lastHint < Setting.SIGHT_HINT_GAP.l()) return;
        s.lastHint = now;
        SCANS.put(p.getUUID(), new OreScan(p.serverLevel(), p.blockPosition(), SightRules.oreRange(POWER.mastery(p))));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!ARROWS.isEmpty()) tickArrows(e);
        if (SCANS.isEmpty()) return;
        Iterator<Map.Entry<UUID, OreScan>> it = SCANS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, OreScan> entry = it.next();
            if (!entry.getValue().step(SCAN_BUDGET)) continue;
            it.remove();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p != null) tellOres(p, entry.getValue());
        }
    }

    private static void tellOres(ServerPlayer p, OreScan scan) {
        if (scan.bestPos == null || p.level() != scan.level) return;
        int level = POWER.mastery(p);
        Vec3 eye = p.getEyePosition();
        BlockPos at = scan.bestPos;
        String where = SightRules.direction(at.getX() + 0.5 - eye.x, at.getY() + 0.5 - eye.y, at.getZ() + 0.5 - eye.z, p.getYRot());
        whisper(p, SightRules.oreHint(SightRules.oreName(scan.bestName), where, scan.bestCount(), level));
        if (level >= Setting.SIGHT_GLOW_LEVEL.i()) {
            List<BlockPos> near = scan.nearest(GLOW_MAX);
            long[] spots = new long[near.size()];
            for (int i = 0; i < spots.length; i++) spots[i] = near.get(i).asLong();
            CompoundTag tag = new CompoundTag();
            tag.putLongArray("glow", spots);
            Net.toOwner(p, POWER.id.toString(), tag);
        }
    }

    // Fights: twice a second, look at what's near an owner.
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 10 != 0 || !POWER.owns(p)) return;
        if (HiddenRuntime.switchedOff(p)) return;
        List<Mob> mobs = p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16), m -> m.isAlive());
        if (mobs.isEmpty()) return;
        Seer s = seer(p);
        boolean quiet = POWER.hushed(p);
        long now = p.level().getGameTime();
        double window = SightRules.warnSeconds(POWER.mastery(p));
        Vec3 eye = p.getEyePosition();
        for (Mob m : mobs) {
            if (MobPowers.holds(m) && !quiet && s.sensed.add(m.getUUID())) sense(p, m);
            if (m.getTarget() != p) continue;
            s.lastThreat = now;
            if (quiet || now - s.warned.getOrDefault(m.getUUID(), -1000L) < SightRules.WARN_GAP_TICKS) continue;
            String where = SightRules.direction(m.getX() - eye.x, m.getEyeY() - eye.y, m.getZ() - eye.z, p.getYRot());
            String text = null;
            if (m instanceof AbstractSkeleton && m.isUsingItem()) {
                text = "skeleton aiming at you";
            } else if (!(m instanceof AbstractSkeleton)) {
                double speed = m.getAttributeValue(Attributes.MOVEMENT_SPEED) * 10;
                double seconds = SightRules.secondsAway(m.distanceTo(p), speed, m instanceof Creeper ? 3 : 2);
                if (seconds <= window) text = SightRules.threat(m.getName().getString().toLowerCase(), where, seconds);
            }
            if (text == null) continue;
            s.warned.put(m.getUUID(), now);
            whisper(p, text);
            Fx.soundFor(p, PowerSounds.WHISPER_SHIMMER.get(), p.position(), 0.4f, 1f);
        }
        if (s.warned.size() > 64) s.warned.clear();
        if (s.sensed.size() > 512) s.sensed.clear();
    }

    private static void sense(ServerPlayer p, Mob m) {
        String what = "something";
        if (POWER.mastery(p) >= SightRules.RANK_LEVEL) {
            PowerDef def = MobPowers.get(m).flatMap(HiddenPowers::def).orElse(null);
            if (def != null) what = "EDCBF".contains(def.rank().name()) ? "a rank " + def.rank().name() + " power" : "something big";
        }
        whisper(p, "that " + m.getName().getString().toLowerCase() + " has " + what);
        Fx.soundFor(p, PowerSounds.WHISPER_SHIMMER.get(), p.position(), 0.3f, 0.9f);
    }

    // How to get it: 1,000 skeleton arrows that fly within half a block of you and miss.
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide() && e.getEntity() instanceof AbstractArrow a && a.getOwner() instanceof AbstractSkeleton) {
            ARROWS.put(a, new HashSet<>());
        }
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent e) {
        if (ARROWS.isEmpty() || !(e.getProjectile() instanceof AbstractArrow a)) return;
        Set<UUID> near = ARROWS.remove(a);
        if (near == null) return;
        HitResult hit = e.getRayTraceResult();
        if (hit instanceof EntityHitResult eh) near.remove(eh.getEntity().getUUID());
        for (UUID id : near) {
            ServerPlayer p = a.getServer() == null ? null : a.getServer().getPlayerList().getPlayer(id);
            if (p != null) dodged(p);
        }
    }

    private static void tickArrows(TickEvent.ServerTickEvent e) {
        Iterator<Map.Entry<AbstractArrow, Set<UUID>>> it = ARROWS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<AbstractArrow, Set<UUID>> entry = it.next();
            AbstractArrow a = entry.getKey();
            if (a.isRemoved() || a.tickCount > 200) {
                it.remove();
                continue;
            }
            Vec3 now = a.position(), before = now.subtract(a.getDeltaMovement());
            AABB path = new AABB(before, now).inflate(2);
            for (var other : a.level().players()) {
                if (other instanceof ServerPlayer p && other.getBoundingBox().intersects(path)
                        && other.getBoundingBox().inflate(0.5).clip(before, now).isPresent()) {
                    entry.getValue().add(p.getUUID());
                }
            }
        }
    }

    private static void dodged(ServerPlayer p) {
        if (POWER.owns(p)) return;
        int count = POWER.data(p).getInt(DODGED) + 1;
        POWER.data(p).putInt(DODGED, count);
        if (count >= SightRules.ARROWS_NEEDED) POWER.unlock(p);
        else if (count % 50 == 0) whisper(p, "You saw that one coming. " + count + "/" + SightRules.ARROWS_NEEDED);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        SEERS.remove(e.getEntity().getUUID());
        SCANS.remove(e.getEntity().getUUID());
    }

    // So the lists can't hold on to a world after the server stops (singleplayer keeps the game running).
    @SubscribeEvent
    public static void onStopping(net.minecraftforge.event.server.ServerStoppingEvent e) {
        ARROWS.clear();
        SCANS.clear();
    }
}
