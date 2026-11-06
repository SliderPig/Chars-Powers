package com.example.charpowers.items;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.attack.Wreck;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// The Flame Hammer's blow on the ground: a ring of flame spreading out from the spot and a line of flame shooting ahead. The fire on the ground only stays
// a moment. It obeys doFireTick and every protection, and spreads no further than normal fire would.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class FlameBurst {
    private static final int SLAM_WAIT = 30;
    private static final double RING = 4.8, LINE = 10;
    private static final float BLAST_DAMAGE = 7f, HIT_BLAST_DAMAGE = 3f;
    private static final int RING_TICKS = 7, LINE_TICKS = 9, FIRE_STAYS = 36;
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static final List<TempFire> FIRES = new ArrayList<>();

    private record TempFire(ServerLevel level, BlockPos pos, long until) {}

    private static final class Burst {
        final ServerLevel level;
        final UUID who;
        final Vec3 at, dir;
        final float damage;
        final boolean line;
        final Set<Integer> hit = new HashSet<>();
        int age;

        Burst(ServerLevel level, UUID who, Vec3 at, Vec3 dir, float damage, boolean line) {
            this.level = level;
            this.who = who;
            this.at = at;
            this.dir = dir;
            this.damage = damage;
            this.line = line;
        }
    }

    private FlameBurst() {}

    // A click on the ground reached the server. It says where it thinks the blow landed: only believed if that's within reach of you.
    public static void requested(ServerPlayer p, double x, double y, double z) {
        if (!(p.getMainHandItem().getItem() instanceof FlameHammer) || p.isSpectator()) return;
        if (p.getCooldowns().isOnCooldown(p.getMainHandItem().getItem())) return;
        boolean ready = p.getAttackStrengthScale(0.5f) >= (p.isShiftKeyDown() ? 0.7f : 0.9f);
        if (!ready || p.distanceToSqr(x, y, z) > 8 * 8) return;
        p.resetAttackStrengthTicker();
        slam(p, new Vec3(x, y, z), BLAST_DAMAGE, true);
    }

    // A fully charged hit on something: a smaller burst at its feet, no line.
    static void chargedHit(ServerPlayer p, Entity target) {
        slam(p, target.position(), HIT_BLAST_DAMAGE, false);
    }

    public static void slam(ServerPlayer p, Vec3 spot, float damage, boolean line) {
        ServerLevel level = p.serverLevel();
        BlockPos ground = Foes.groundNear(level, BlockPos.containing(spot), 2, 4);
        if (ground == null) return;
        Vec3 at = new Vec3(spot.x, ground.getY(), spot.z);
        Vec3 flat = p.getLookAngle().multiply(1, 0, 1);
        flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        BURSTS.add(new Burst(level, p.getUUID(), at, flat, damage, line));
        level.playSound(null, at.x, at.y, at.z, CharItems.sound("hammer_slam"), SoundSource.PLAYERS, 1.2f, 0.95f + level.random.nextFloat() * 0.1f);
        level.playSound(null, at.x, at.y, at.z, CharItems.sound("flame_burst"), SoundSource.PLAYERS, 1f, 1f);
        Foes.particles(level, ParticleTypes.LAVA, at, 14, 0.4, 0.4);
        Foes.particles(level, ParticleTypes.FLASH, at.add(0, 0.2, 0), 1, 0, 0);
        Foes.particles(level, ParticleTypes.LARGE_SMOKE, at, 2, 0.8, 0.02);
        if (line) {
            p.getMainHandItem().hurtAndBreak(1, p, x -> x.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            p.getCooldowns().addCooldown(p.getMainHandItem().getItem(), SLAM_WAIT);
        }
    }

    public static boolean busy() {
        return !BURSTS.isEmpty() || !FIRES.isEmpty();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Iterator<Burst> it = BURSTS.iterator();
        while (it.hasNext()) {
            Burst b = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(b.who);
            if (p == null || p.serverLevel() != b.level || ++b.age > Math.max(RING_TICKS, b.line ? LINE_TICKS : 0)) {
                it.remove();
                continue;
            }
            if (b.age <= RING_TICKS) ringStep(p, b);
            if (b.line && b.age <= LINE_TICKS) lineStep(p, b);
        }
        Iterator<TempFire> fires = FIRES.iterator();
        while (fires.hasNext()) {
            TempFire f = fires.next();
            if (f.level.getGameTime() < f.until) continue;
            if (f.level.isLoaded(f.pos) && f.level.getBlockState(f.pos).is(Blocks.FIRE)) f.level.removeBlock(f.pos, false);
            fires.remove();
        }
    }

    private static void ringStep(ServerPlayer p, Burst b) {
        ServerLevel level = b.level;
        double radius = RING * b.age / RING_TICKS * (b.line ? 1 : 0.6);
        int points = (int) (radius * 7) + 6;
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            double x = b.at.x + Math.cos(a) * radius, z = b.at.z + Math.sin(a) * radius;
            Foes.particles(level, ParticleTypes.FLAME, x, b.at.y + 0.15, z, 0, Math.cos(a) * 0.35, 0.09, Math.sin(a) * 0.35, 1);
            if (i % 3 == 0) Foes.particles(level, ParticleTypes.SMALL_FLAME, x, b.at.y + 0.3, z, 1, 0.1, 0.25, 0.1, 0.01);
            if (i % 9 == 0 && b.age % 3 == 0 && b.line) tryFire(p, level, x, b.at.y, z);
        }
        burn(p, b, new AABB(b.at.x - radius, b.at.y - 1, b.at.z - radius, b.at.x + radius, b.at.y + 3, b.at.z + radius), radius);
    }

    private static void lineStep(ServerPlayer p, Burst b) {
        ServerLevel level = b.level;
        double along = LINE * b.age / LINE_TICKS;
        Vec3 at = b.at.add(b.dir.scale(along));
        BlockPos ground = Foes.groundNear(level, BlockPos.containing(at.x, b.at.y, at.z), 2, 3);
        if (ground == null) return;
        double y = ground.getY();
        Foes.particles(level, ParticleTypes.FLAME, at.x, y + 0.1, at.z, 18, 0.3, 0.6, 0.3, 0.06);
        Foes.particles(level, ParticleTypes.LAVA, at.x, y + 0.2, at.z, 2, 0.2, 0.1, 0.2, 0.1);
        if (b.age % 3 == 0) tryFire(p, level, at.x, y, at.z);
        burn(p, b, new AABB(at.x - 1.2, y - 0.5, at.z - 1.2, at.x + 1.2, y + 3, at.z + 1.2), 99);
    }

    private static void burn(ServerPlayer p, Burst b, AABB box, double radius) {
        for (LivingEntity e : b.level.getEntitiesOfClass(LivingEntity.class, box, e -> Foes.sweepHits(p, e))) {
            if (radius < 99 && e.distanceToSqr(b.at.x, e.getY(), b.at.z) > (radius + 1) * (radius + 1)) continue;
            if (!b.hit.add(e.getId())) continue;
            e.hurt(p.damageSources().playerAttack(p), b.damage);
            e.setSecondsOnFire(6);
            Vec3 away = e.position().subtract(b.at).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-3) {
                away = away.normalize();
                e.knockback(0.4f, -away.x, -away.z);
            }
        }
    }

    // Real fire for a moment, where fire could go anyway and where you'd be allowed to light it.
    private static void tryFire(ServerPlayer p, ServerLevel level, double x, double y, double z) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) return;
        BlockPos spot = Foes.groundNear(level, BlockPos.containing(x, y, z), 1, 2);
        if (spot == null || !level.getBlockState(spot).isAir() || !level.mayInteract(p, spot) || !BaseFireBlock.canBePlacedAt(level, spot, net.minecraft.core.Direction.UP)) return;
        if (!Wreck.mayTouch(level, spot.below(), p)) return;
        BlockState fire = BaseFireBlock.getState(level, spot);
        if (level.setBlock(spot, fire, 11)) FIRES.add(new TempFire(level, spot, level.getGameTime() + FIRE_STAYS + level.random.nextInt(14)));
    }

    @SubscribeEvent
    public static void onStop(ServerStoppingEvent e) {
        BURSTS.clear();
        FIRES.clear();
    }
}
